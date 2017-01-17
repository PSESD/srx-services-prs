package org.psesd.srx.services.prs

import org.json4s._
import org.json4s.JsonDSL._
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter, SifRequestParameterCollection}
import org.psesd.srx.shared.data.exceptions.DatasourceDuplicateViolationException
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
                        val mainContact: Option[Contact],
                        val services: Option[ArrayBuffer[ExternalService]]
                      ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    if(services.isDefined) {
      if(mainContact.isDefined) {
        ("id" -> id.toString) ~
          ("name" -> name) ~
          ("mainContact" -> mainContact.get.toJson) ~
          ("services" -> services.get.map { d => d.toJson })
      } else {
        ("id" -> id.toString) ~
          ("name" -> name) ~
          ("services" -> services.get.map { d => d.toJson })
      }
    } else {
      if(mainContact.isDefined) {
        ("id" -> id.toString) ~
          ("name" -> name) ~
          ("mainContact" -> mainContact.get.toJson)
      } else {
        ("id" -> id.toString) ~
          ("name" -> name)
      }
    }
  }

  def toXml: Node = {
    <authorizedEntity>
      <id>{id.toString}</id>
      <name>{name}</name>
      {if(mainContact.isDefined && !mainContact.get.isEmpty) mainContact.get.toXml}
      {optional({if(services.isDefined) "true" else null}, <services>{if(services.isDefined) services.get.map(d => d.toXml)}</services>)}
    </authorizedEntity>
  }

}

/** Represents an Authorized Entity method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class AuthorizedEntityResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  AuthorizedEntity.getAuthorizedEntitiesFromResult,
  <authorizedEntities/>,
  responseFormat
) {
}

/** PRS Authorized Entity methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object AuthorizedEntity extends PrsEntityService {
  def apply(id: Int, name: String, mainContact: Option[Contact]): AuthorizedEntity = new AuthorizedEntity(id, name, mainContact, None)

  def apply(requestBody: SrxRequestBody, parameters: Option[List[SifRequestParameter]]): AuthorizedEntity = {
    if (requestBody == null) {
      throw new ArgumentNullException("requestBody parameter")
    }
    apply(requestBody.getXml.orNull, parameters)
  }

  def apply(authorizedEntityXml: Node, parameters: Option[List[SifRequestParameter]]): AuthorizedEntity = {
    if (authorizedEntityXml == null) {
      throw new ArgumentNullException("authorizedEntityXml parameter")
    }
    val rootElementName = authorizedEntityXml.label
    if (rootElementName != "authorizedEntity" && rootElementName != "root") {
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
      },
      None
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
        PrsServer.logPrsSuccessMessage(
          PrsResource.AuthorizedEntities.toString,
          SifRequestAction.Create.toString,
          result.id,
          SifRequestParameterCollection(parameters),
          Some(authorizedEntity.toXml.toXmlString)
        )
        val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
        if(responseFormat.equals(SrxResponseFormat.Object)) {
          val queryResult = executeQuery(Some(result.id.get.toInt))
          new AuthorizedEntityResult(
            SifRequestAction.Create,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
            queryResult,
            responseFormat
          )
        } else {
          new AuthorizedEntityResult(
            SifRequestAction.Create,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
            result,
            responseFormat
          )
        }
      } else {
        throw result.exceptions.head
      }
    } catch {
      case dv: DatasourceDuplicateViolationException =>
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, dv)
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
          PrsServer.logPrsSuccessMessage(
            PrsResource.AuthorizedEntities.toString,
            SifRequestAction.Delete.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            None
          )
          val aeResult = new AuthorizedEntityResult(
            SifRequestAction.Delete,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete),
            result,
            SrxResponseFormat.getResponseFormat(parameters)
          )
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
        val result = executeQuery(id)
        if (result.success) {
          val resourceId = if (id.isEmpty) Some("all") else Some(id.get.toString)
          if (id.isDefined && result.rows.isEmpty) {
            PrsServer.logPrsNotFoundMessage(
              PrsResource.AuthorizedEntities.toString,
              SifRequestAction.Query.toString,
              resourceId,
              SifRequestParameterCollection(parameters),
              None
            )
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.AuthorizedEntities.toString))
          } else {
            PrsServer.logPrsSuccessMessage(
              PrsResource.AuthorizedEntities.toString,
              SifRequestAction.Query.toString,
              resourceId,
              SifRequestParameterCollection(parameters),
              None
            )
            new AuthorizedEntityResult(
              SifRequestAction.Query,
              SifHttpStatusCode.Ok,
              result,
              SrxResponseFormat.getResponseFormat(parameters)
            )
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

  def executeQuery(id: Option[Int]): DatasourceResult = {
    val selectFrom = "select srx_services_prs.authorized_entity.id, " +
      "srx_services_prs.authorized_entity.name, " +
      "srx_services_prs.contact.id main_contact_id, " +
      "srx_services_prs.contact.name main_contact_name, " +
      "srx_services_prs.contact.title main_contact_title, " +
      "srx_services_prs.contact.email main_contact_email, " +
      "srx_services_prs.contact.phone main_contact_phone, " +
      "srx_services_prs.contact.mailing_address main_contact_mailing_address, " +
      "srx_services_prs.contact.web_address main_contact_web_address, " +
      "srx_services_prs.external_service.id external_service_id, " +
      "srx_services_prs.external_service.name external_service_name, " +
      "srx_services_prs.external_service.description external_service_description " +
      "from srx_services_prs.authorized_entity " +
      "left join srx_services_prs.contact on srx_services_prs.contact.id = srx_services_prs.authorized_entity.main_contact_id " +
      "left join srx_services_prs.external_service on srx_services_prs.external_service.authorized_entity_id = srx_services_prs.authorized_entity.id "
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        datasource.get(selectFrom + "order by srx_services_prs.authorized_entity.id;")
      } else {
        val test = selectFrom + "where srx_services_prs.authorized_entity.id = ?;"
        datasource.get(selectFrom + "where srx_services_prs.authorized_entity.id = ?;", id.get)
      }
    }
    datasource.close()
    result
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
          PrsServer.logPrsSuccessMessage(
            PrsResource.AuthorizedEntities.toString,
            SifRequestAction.Update.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            Some(authorizedEntity.toXml.toXmlString)
          )
          val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
          var aeResult: AuthorizedEntityResult = null
          if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(id.get))
            aeResult = new AuthorizedEntityResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              queryResult,
              responseFormat
            )
          } else {
            aeResult = new AuthorizedEntityResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              result,
              responseFormat
            )
          }
          aeResult.setId(id.get)
          aeResult
        } else {
          throw result.exceptions.head
        }
      }
    } catch {
      case dv: DatasourceDuplicateViolationException =>
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, dv)
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def getAuthorizedEntitiesFromResult(result: DatasourceResult): List[AuthorizedEntity] = {
    val authorizedEntities = ArrayBuffer[AuthorizedEntity]()
    for (row <- result.rows) {
      val id = row.getString("id").getOrElse("").toInt
      val mainContactId = row.getString("main_contact_id")
      val externalServiceId = row.getString("external_service_id")
      val authorizedEntity = authorizedEntities.find(ae => ae.id.equals(id))
      if (authorizedEntity.isDefined) {
        if (externalServiceId.isDefined) {
          authorizedEntity.get.services.get += ExternalService(
            externalServiceId.get.toInt,
            id,
            row.getString("external_service_name"),
            row.getString("external_service_description")
          )
        }
      } else {
        authorizedEntities += new AuthorizedEntity(
          id,
          row.getString("name").getOrElse(""),
          {
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
          },
          {
            if(externalServiceId.isDefined)
              Some(ArrayBuffer[ExternalService](ExternalService(
                externalServiceId.get.toInt,
                id,
                row.getString("external_service_name"),
                row.getString("external_service_description")
              )))
            else
              Some(ArrayBuffer[ExternalService]())
          }
        )
      }
    }
    authorizedEntities.toList
  }

}
