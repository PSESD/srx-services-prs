package org.psesd.srx.services.prs

import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException}
import org.psesd.srx.shared.core.sif._
import org.psesd.srx.shared.data._

import scala.collection.mutable.ArrayBuffer

/** PRS Authorized Entity service.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  **/
object AuthorizedEntityService extends SrxResourceService {

  private final val DatasourceClassNameKey = "DATASOURCE_CLASS_NAME"
  private final val DatasourceMaxConnectionsKey = "DATASOURCE_MAX_CONNECTIONS"
  private final val DatasourceTimeoutKey = "DATASOURCE_TIMEOUT"
  private final val DatasourceUrlKey = "DATASOURCE_URL"

  private lazy val datasourceConfig = new DatasourceConfig(
    Environment.getProperty(DatasourceUrlKey),
    Environment.getProperty(DatasourceClassNameKey),
    Environment.getProperty(DatasourceMaxConnectionsKey).toInt,
    Environment.getProperty(DatasourceTimeoutKey).toLong
  )

  def delete(parameters: List[SifRequestParameter]): SrxResourceResult = {
    SrxResourceErrorResult(SifHttpStatusCode.MethodNotAllowed, new UnsupportedOperationException(PrsResource.AuthorizedEntities.toString + " DELETE method not allowed."))
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val authorizedEntity = resource.asInstanceOf[AuthorizedEntity]

      val datasource = new Datasource(datasourceConfig)

      val result = datasource.execute(
        "insert into srx_services_prs.authorized_entity (" +
          "id, name, main_contact_id) values (" +
          "DEFAULT, ?, ?) " +
          "RETURNING id;",
        authorizedEntity.name,
        authorizedEntity.mainContactId.orNull
      )

      datasource.close()

      if (result.success) {
        AuthorizedEntityResult(SifRequestAction.Create, SifRequestAction.getSuccessStatusCode(SifRequestAction.Create), result)
      } else {
        throw result.exceptions.head
      }
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, new Exception(""))
    }
  }

  def query(parameters: List[SifRequestParameter]): SrxResourceResult = {
    val id = getIdFromRequestParameters(parameters)
    if (id.isEmpty) {
      SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
    } else {
      try {
        val datasource = new Datasource(datasourceConfig)
        val result = datasource.get("select * from srx_services_prs.authorized_entity where id = ?", id.get)
        datasource.close()
        if (result.success) {
          if (result.rows.isEmpty) {
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new Exception("%s not found.".format(PrsResource.AuthorizedEntities.toString)))
          } else {
            AuthorizedEntityResult(SifRequestAction.Query, SifHttpStatusCode.Ok, result)
          }
        } else {
          throw result.exceptions.head
        }
      } catch {
        case e: Exception =>
          SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, new Exception(""))
      }
    }
  }

  private def getIdFromRequestParameters(parameters: List[SifRequestParameter]): Option[Int] = {
    var id: Option[Int] = None
    try {
      if (parameters != null && parameters.nonEmpty) {
        val idParameter = parameters.find(p => p.key == "id").orNull
        if (idParameter != null) {
          id = Some(idParameter.value.toInt)
        }
      }
    } catch {
      case e: Exception =>
    }
    id
  }

  def update(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    SrxResourceErrorResult(SifHttpStatusCode.MethodNotAllowed, new UnsupportedOperationException(PrsResource.AuthorizedEntities.toString + " UPDATE method not allowed."))
  }

  def getAuthorizedEntitiesFromResult(result: DatasourceResult): List[AuthorizedEntity] = {
    val authorizedEntities = ArrayBuffer[AuthorizedEntity]()
    for (row <- result.rows) {
      val mainContactId = row.getString("main_contact_id")
      val authorizedEntity = AuthorizedEntity(
        row.getString("id").getOrElse("").toInt,
        row.getString("name").getOrElse(""), {
          if (mainContactId.isDefined) {
            Some(mainContactId.getOrElse("").toInt)
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
