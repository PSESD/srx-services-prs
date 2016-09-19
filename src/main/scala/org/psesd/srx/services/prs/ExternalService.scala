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

/** Represents External Service.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class ExternalService(
                 val id: Int,
                 val authorizedEntityId: Int,
                 val name: Option[String],
                 val description: Option[String]
               ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <externalService>
      <id>{id.toString}</id>
      <authorizedEntityId>{authorizedEntityId.toString}</authorizedEntityId>
      {optional(name.orNull, <externalServiceName>{name.orNull}</externalServiceName>)}
      {optional(description.orNull, <externalServiceDescription>{description.orNull}</externalServiceDescription>)}
    </externalService>
  }

}

/** Represents External Service method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class ExternalServiceResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  ExternalService.getExternalServiceFromResult,
    <externalService/>
) {
}

/** PRS External Service methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  * */
object ExternalService extends PrsEntityService {
  def apply(
             id: Int,
             authorizedEntityId: Int,
             name: Option[String],
             description: Option[String]
           ): ExternalService = new ExternalService(id, authorizedEntityId, name, description)

  def apply(externalServiceXml: Node, parameters: Option[List[SifRequestParameter]]): ExternalService = {
    if (externalServiceXml == null) {
      throw new ArgumentNullException("externalServiceXml parameter")
    }
    val authorizedEntityParam = {if (parameters.isDefined) parameters.get.find(p => p.key.toLowerCase == "authorizedentityid") else None}
    if (authorizedEntityParam.isEmpty) {
      throw new ArgumentNullException("authorizedEntityId parameter")
    }

    val rootElementName = externalServiceXml.label
    if (rootElementName != "externalService") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (externalServiceXml \ "id").textOption.getOrElse("0").toInt
    val authorizedEntityId = authorizedEntityParam.get.value.toInt
    val name = (externalServiceXml \ "externalServiceName").textOption
    val description = (externalServiceXml \ "externalServiceDescription").textOption
    new ExternalService(
      id,
      authorizedEntityId,
      name,
      description
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val externalService = resource.asInstanceOf[ExternalService]

      val datasource = new Datasource(datasourceConfig)

      val result = datasource.create(
        "insert into srx_services_prs.external_service (" +
          "id, authorized_entity_id, name, description) values (" +
          "DEFAULT, ?, ?, ?) " +
          "RETURNING id;",
        "id",
        externalService.authorizedEntityId,
        externalService.name.orNull,
        externalService.description.orNull
      )

      datasource.close()

      if (result.success) {
        new ExternalServiceResult(SifRequestAction.Create, SifRequestAction.getSuccessStatusCode(SifRequestAction.Create), result)
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
          "delete from srx_services_prs.external_service where id = ?;",
          id.get
        )

        datasource.close()

        if (result.success) {
          val esResult = new ExternalServiceResult(SifRequestAction.Delete, SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete), result)
          esResult.setId(id.get)
          esResult
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
        val selectFrom = "select srx_services_prs.external_service.* from srx_services_prs.external_service "
        val datasource = new Datasource(datasourceConfig)
        val result = {
          if (id.isEmpty) {
            datasource.get(selectFrom + "order by srx_services_prs.external_service.id;")
          } else {
            datasource.get(selectFrom + "where srx_services_prs.external_service.id = ?;", id.get)
          }
        }
        datasource.close()
        if (result.success) {
          if (result.rows.isEmpty) {
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.ExternalServices.toString))
          } else {
            new ExternalServiceResult(SifRequestAction.Query, SifHttpStatusCode.Ok, result)
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

      val externalService = resource.asInstanceOf[ExternalService]
      if ((id.isEmpty || id.get == 0) && externalService.id > 0) {
        id = Some(externalService.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        val result = datasource.execute(
          "update srx_services_prs.external_service set " +
            "authorized_entity_id = ?, " +
            "name = ?, " +
            "description = ? " +
            "where id = ?;",
          externalService.authorizedEntityId,
          externalService.name.orNull,
          externalService.description.orNull,
          id.get
        )

        datasource.close()

        if (result.success) {
          val pResult = new ExternalServiceResult(SifRequestAction.Update, SifRequestAction.getSuccessStatusCode(SifRequestAction.Update), result)
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

  def getExternalServiceFromResult(result: DatasourceResult): List[ExternalService] = {
    val externalServiceResult = ArrayBuffer[ExternalService]()
    for (row <- result.rows) {
      val externalService = ExternalService(
        row.getString("id").getOrElse("").toInt,
        row.getString("authorized_entity_id").getOrElse("").toInt,
        row.getString("name"),
        row.getString("description")
      )
      externalServiceResult += externalService
    }
    externalServiceResult.toList
  }

}
