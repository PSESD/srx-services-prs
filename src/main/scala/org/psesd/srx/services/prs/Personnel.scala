package org.psesd.srx.services.prs

import org.json4s.JValue
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core.{SrxResource, SrxResourceErrorResult, SrxResourceResult, SrxResponseFormat}
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter}
import org.psesd.srx.shared.data.{Datasource, DatasourceResult}

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/** Represents Authorized Entity Personnel.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class Personnel(
                        val id: Int,
                        val authorizedEntityId: Int,
                        val firstName: Option[String],
                        val lastName: Option[String]
                      ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <personnel>
      <id>{id.toString}</id>
      <authorizedEntityId>{authorizedEntityId.toString}</authorizedEntityId>
      {optional(firstName.orNull, <firstName>{firstName.orNull}</firstName>)}
      {optional(lastName.orNull, <lastName>{lastName.orNull}</lastName>)}
    </personnel>
  }

}

/** Represents Authorized Entity Personnel method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class PersonnelResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  Personnel.getPersonnelFromResult,
  <personnels/>,
  responseFormat
) {
}

/** PRS Authorized Entity Personnel methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  * */
object Personnel extends PrsEntityService {
  def apply(
    id: Int,
    authorizedEntityId: Int,
    firstName: Option[String],
    lastName: Option[String]
  ): Personnel = new Personnel(id, authorizedEntityId, firstName, lastName)

  def apply(personnelXml: Node, parameters: Option[List[SifRequestParameter]]): Personnel = {
    if (personnelXml == null) {
      throw new ArgumentNullException("personnelXml parameter")
    }
    val authorizedEntityIdParam = {if (parameters.isDefined) parameters.get.find(p => p.key.toLowerCase == "authorizedentityid") else None}
    if (authorizedEntityIdParam.isEmpty) {
      throw new ArgumentNullException("authorizedEntityId parameter")
    }

    val rootElementName = personnelXml.label
    if (rootElementName != "personnel" && rootElementName != "root") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (personnelXml \ "id").textOption.getOrElse("0").toInt
    val authorizedEntityId = authorizedEntityIdParam.get.value.toInt
    val firstName = (personnelXml \ "firstName").textOption
    val lastName = (personnelXml \ "lastName").textOption
    new Personnel(
      id,
      authorizedEntityId,
      firstName,
      lastName
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val personnel = resource.asInstanceOf[Personnel]

      val datasource = new Datasource(datasourceConfig)

      val result = datasource.create(
        "insert into srx_services_prs.personnel (" +
          "id, authorized_entity_id, first_name, last_name) values (" +
          "DEFAULT, ?, ?, ?) " +
          "RETURNING id;",
        "id",
        personnel.authorizedEntityId,
        personnel.firstName.orNull,
        personnel.lastName.orNull
      )

      datasource.close()

      if (result.success) {
        val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
        if(responseFormat.equals(SrxResponseFormat.Object)) {
          val queryResult = executeQuery(Some(result.id.get.toInt), None)
          new PersonnelResult(
            SifRequestAction.Create,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
            queryResult,
            responseFormat
          )
        } else {
          new PersonnelResult(
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
          "delete from srx_services_prs.personnel where id = ?;",
          id.get
        )

        datasource.close()

        if (result.success) {
          val pResult = new PersonnelResult(
            SifRequestAction.Delete,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete),
            result,
            SrxResponseFormat.getResponseFormat(parameters)
          )
          pResult.setId(id.get)
          pResult
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
      val authorizedEntityIdParam = parameters.find(p => p.key.toLowerCase == "authorizedentityid")
      if (id.isEmpty && (authorizedEntityIdParam == null || authorizedEntityIdParam.isEmpty)) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("authorizedEntityId parameter"))
      } else {
        try {
          val result = executeQuery(id, authorizedEntityIdParam)
          if (result.success) {
            if (id.isDefined && result.rows.isEmpty) {
              SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.Personnel.toString))
            } else {
              new PersonnelResult(
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
  }

  private def executeQuery(id: Option[Int], authorizedEntityIdParam: Option[SifRequestParameter]): DatasourceResult = {
    val selectFrom = "select srx_services_prs.personnel.* from srx_services_prs.personnel"
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        datasource.get(selectFrom + " where srx_services_prs.personnel.authorized_entity_id = ? order by srx_services_prs.personnel.id;", authorizedEntityIdParam.get.value.toInt)
      } else {
        datasource.get(selectFrom + " where srx_services_prs.personnel.id = ?;", id.get)
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

      val personnel = resource.asInstanceOf[Personnel]
      if ((id.isEmpty || id.get == 0) && personnel.id > 0) {
        id = Some(personnel.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        val result = datasource.execute(
          "update srx_services_prs.personnel set " +
            "authorized_entity_id = ?, " +
            "first_name = ?, " +
            "last_name = ? " +
            "where id = ?;",
          personnel.authorizedEntityId,
          personnel.firstName.orNull,
          personnel.lastName.orNull,
          id.get
        )

        datasource.close()

        if (result.success) {
          val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
          var pResult: PersonnelResult = null
          if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(id.get), None)
            pResult = new PersonnelResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              queryResult,
              responseFormat
            )
          } else {
            pResult = new PersonnelResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              result,
              responseFormat
            )
          }
          pResult.setId(id.get)
          pResult
        } else {
          throw result.exceptions.head
        }
      }
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def getPersonnelFromResult(result: DatasourceResult): List[Personnel] = {
    val personnelResult = ArrayBuffer[Personnel]()
    for (row <- result.rows) {
      val personnel = Personnel(
        row.getString("id").getOrElse("").toInt,
        row.getString("authorized_entity_id").getOrElse("").toInt,
        row.getString("first_name"),
        row.getString("last_name")
      )
      personnelResult += personnel
    }
    personnelResult.toList
  }

}
