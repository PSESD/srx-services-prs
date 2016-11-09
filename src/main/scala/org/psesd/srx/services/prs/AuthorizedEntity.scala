package org.psesd.srx.services.prs

import org.json4s.JValue
import org.psesd.srx.shared.core.{SrxResource, SrxResourceErrorResult, SrxResourceResult}
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter}
import org.psesd.srx.shared.data.{Datasource, DatasourceResult}

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/** Represents an Authorized Entity.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class AuthorizedEntity(
                        val id: Int,
                        val name: String,
                        val mainContact: Option[Contact]
                      ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <authorizedEntity>
      <id>{id.toString}</id>
      <name>{name}</name>
      {if(mainContact.isDefined && !mainContact.get.isEmpty) mainContact.get.toXml}
    </authorizedEntity>
  }

}

/** Represents an Authorized Entity method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class AuthorizedEntityResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  AuthorizedEntity.getAuthorizedEntitiesFromResult,
    <authorizedEntities/>
) {
}

/** PRS Authorized Entity methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object AuthorizedEntity extends PrsEntityService {
  def apply(id: Int, name: String, mainContact: Option[Contact]): AuthorizedEntity = new AuthorizedEntity(id, name, mainContact)

  def apply(authorizedEntityXml: Node, parameters: Option[List[SifRequestParameter]]): AuthorizedEntity = {
    if (authorizedEntityXml == null) {
      throw new ArgumentNullException("authorizedEntityXml parameter")
    }
    val rootElementName = authorizedEntityXml.label
    if (rootElementName != "authorizedEntity") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (authorizedEntityXml \ "id").textOption.getOrElse("0").toInt
    val name = (authorizedEntityXml \ "name").textRequired("authorizedEntity.name")
    val mainContact = authorizedEntityXml \ "mainContact"
    new AuthorizedEntity(
      id,
      name, {
        if (mainContact != null && mainContact.length > 0) {
          Some(Contact(mainContact.head))
        } else {
          None
        }
      }
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val authorizedEntity = resource.asInstanceOf[AuthorizedEntity]

      val datasource = new Datasource(datasourceConfig)

      var result: DatasourceResult = null

      if (authorizedEntity.mainContact.isDefined) {

        result = datasource.create(
          "with new_contact as (" +
            "insert into srx_services_prs.contact (" +
            "id, name, title, email, phone, mailing_address, web_address) values (" +
            "DEFAULT, ?, ?, ?, ?, ?, ?) " +
            "RETURNING id) " +
            "insert into srx_services_prs.authorized_entity (" +
            "id, name, main_contact_id) values (" +
            "DEFAULT, ?, (select id from new_contact)) " +
            "RETURNING id;",
          "id",
          authorizedEntity.mainContact.get.name.orNull,
          authorizedEntity.mainContact.get.title.orNull,
          authorizedEntity.mainContact.get.email.orNull,
          authorizedEntity.mainContact.get.phone.orNull,
          authorizedEntity.mainContact.get.mailingAddress.orNull,
          authorizedEntity.mainContact.get.webAddress.orNull,
          authorizedEntity.name
        )

      } else {
        result = datasource.create(
          "with new_contact as (" +
            "insert into srx_services_prs.contact (" +
            "id) values (" +
            "DEFAULT) " +
            "RETURNING id) " +
            "insert into srx_services_prs.authorized_entity (" +
            "id, name, main_contact_id) values (" +
            "DEFAULT, ?, (select id from new_contact)) " +
            "RETURNING id;",
          "id",
          authorizedEntity.name
        )
      }

      datasource.close()

      if (result.success) {
        new AuthorizedEntityResult(SifRequestAction.Create, SifRequestAction.getSuccessStatusCode(SifRequestAction.Create), result)
      } else {
        throw result.exceptions.head
      }
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def delete(parameters: List[SifRequestParameter]): SrxResourceResult = {
    val id = getKeyIdFromRequestParameters(parameters)
    if (id.isEmpty || id.get == -1) {
      SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
    } else {
      try {
        val datasource = new Datasource(datasourceConfig)

        val result = datasource.execute(
          "begin;" +
            "delete from srx_services_prs.contact where id = (select main_contact_id from srx_services_prs.authorized_entity where id = ?);" +
            "delete from srx_services_prs.authorized_entity where id = ?;" +
            "commit;",
          id.get,
          id.get
        )

        datasource.close()

        if (result.success) {
          val aeResult = new AuthorizedEntityResult(SifRequestAction.Delete, SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete), result)
          aeResult.setId(id.get)
          aeResult
        } else {
          throw result.exceptions.head
        }
      } catch {
        case e: Exception =>
          SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
      }
    }
  }

  def query(parameters: List[SifRequestParameter]): SrxResourceResult = {
    val id = getKeyIdFromRequestParameters(parameters)
    if (id.isDefined && id.get == -1) {
      SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
    } else {
      try {
        val selectFrom = "select srx_services_prs.authorized_entity.*, " +
          "srx_services_prs.contact.name main_contact_name, " +
          "srx_services_prs.contact.title main_contact_title, " +
          "srx_services_prs.contact.email main_contact_email, " +
          "srx_services_prs.contact.phone main_contact_phone, " +
          "srx_services_prs.contact.mailing_address main_contact_mailing_address, " +
          "srx_services_prs.contact.web_address main_contact_web_address " +
          "from srx_services_prs.authorized_entity " +
          "join srx_services_prs.contact on srx_services_prs.contact.id = srx_services_prs.authorized_entity.main_contact_id "
        val datasource = new Datasource(datasourceConfig)
        val result = {
          if (id.isEmpty) {
            datasource.get(selectFrom + "order by srx_services_prs.authorized_entity.id;")
          } else {
            datasource.get(selectFrom + "where srx_services_prs.authorized_entity.id = ?;", id.get)
          }
        }
        datasource.close()
        if (result.success) {
          if (id.isDefined && result.rows.isEmpty) {
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.AuthorizedEntities.toString))
          } else {
            new AuthorizedEntityResult(SifRequestAction.Query, SifHttpStatusCode.Ok, result)
          }
        } else {
          throw result.exceptions.head
        }
      } catch {
        case e: Exception =>
          SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
      }
    }
  }

  def update(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    if (resource == null) {
      throw new ArgumentNullException("resource parameter")
    }
    try {
      var id = getKeyIdFromRequestParameters(parameters)

      val authorizedEntity = resource.asInstanceOf[AuthorizedEntity]
      if ((id.isEmpty || id.get == 0) && authorizedEntity.id > 0) {
        id = Some(authorizedEntity.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        var result: DatasourceResult = null

        if (authorizedEntity.mainContact.isDefined) {

          result = datasource.execute(
            "begin;" +
              "update srx_services_prs.contact set " +
              "name = ?, title = ?, email = ?, phone = ?, mailing_address = ?, web_address = ? " +
              "where id = (select main_contact_id from srx_services_prs.authorized_entity where id = ?); " +
              "update srx_services_prs.authorized_entity set " +
              "name = ? " +
              "where id = ?;" +
              "commit;",
            authorizedEntity.mainContact.get.name.orNull,
            authorizedEntity.mainContact.get.title.orNull,
            authorizedEntity.mainContact.get.email.orNull,
            authorizedEntity.mainContact.get.phone.orNull,
            authorizedEntity.mainContact.get.mailingAddress.orNull,
            authorizedEntity.mainContact.get.webAddress.orNull,
            id.get,
            authorizedEntity.name,
            id.get
          )

        } else {
          result = datasource.execute(
            "update srx_services_prs.authorized_entity set " +
              "name = ? " +
              "where id = ?;",
            authorizedEntity.name,
            id.get
          )
        }

        datasource.close()

        if (result.success) {
          val aeResult = new AuthorizedEntityResult(SifRequestAction.Update, SifRequestAction.getSuccessStatusCode(SifRequestAction.Update), result)
          aeResult.setId(id.get)
          aeResult
        } else {
          throw result.exceptions.head
        }
      }
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def getAuthorizedEntitiesFromResult(result: DatasourceResult): List[AuthorizedEntity] = {
    val authorizedEntities = ArrayBuffer[AuthorizedEntity]()
    for (row <- result.rows) {
      val mainContactId = row.getString("main_contact_id")
      val authorizedEntity = AuthorizedEntity(
        row.getString("id").getOrElse("").toInt,
        row.getString("name").getOrElse(""), {
          if (mainContactId.isDefined) {
            Some(Contact(
              mainContactId.get.toInt,
              row.getString("main_contact_name"),
              row.getString("main_contact_title"),
              row.getString("main_contact_email"),
              row.getString("main_contact_phone"),
              row.getString("main_contact_mailing_address"),
              row.getString("main_contact_web_address")
            ))
          } else {
            None
          }
        }
      )
      authorizedEntities += authorizedEntity
    }
    authorizedEntities.toList
  }

}
