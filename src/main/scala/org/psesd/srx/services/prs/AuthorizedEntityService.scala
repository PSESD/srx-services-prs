package org.psesd.srx.services.prs

import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxRequestActionNotAllowedException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif._
import org.psesd.srx.shared.data._

import scala.collection.mutable.ArrayBuffer

/** PRS Authorized Entity service.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  * */
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

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val authorizedEntity = resource.asInstanceOf[AuthorizedEntity]

      val datasource = new Datasource(datasourceConfig)

      val result = datasource.create(
        "insert into srx_services_prs.authorized_entity (" +
          "id, name, main_contact_id) values (" +
          "DEFAULT, ?, ?) " +
          "RETURNING id;",
        "id",
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
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def delete(parameters: List[SifRequestParameter]): SrxResourceResult = {
    val id = getAuthorizedEntityIdFromRequestParameters(parameters)
    if(id.isEmpty || id.get == -1) {
      SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
    } else {
      try {
        val datasource = new Datasource(datasourceConfig)

        val result = datasource.execute(
          "delete from srx_services_prs.authorized_entity " +
            "where id = ?",
          id.get
        )

        datasource.close()

        if (result.success) {
          val aeResult = AuthorizedEntityResult(SifRequestAction.Delete, SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete), result)
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
    val id = getAuthorizedEntityIdFromRequestParameters(parameters)
    if(id.isDefined && id.get == -1) {
      SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
    } else {
      try {
        val datasource = new Datasource(datasourceConfig)
        val result = {
          if (id.isEmpty) {
            datasource.get("select * from srx_services_prs.authorized_entity order by id")
          } else {
            datasource.get("select * from srx_services_prs.authorized_entity where id = ?", id.get)
          }
        }
        datasource.close()
        if (result.success) {
          if (result.rows.isEmpty) {
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.AuthorizedEntities.toString))
          } else {
            AuthorizedEntityResult(SifRequestAction.Query, SifHttpStatusCode.Ok, result)
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
      var id = getAuthorizedEntityIdFromRequestParameters(parameters)

      val authorizedEntity = resource.asInstanceOf[AuthorizedEntity]
      if ((id.isEmpty || id.get == 0) && authorizedEntity.id > 0) {
        id = Some(authorizedEntity.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        val result = datasource.execute(
          "update srx_services_prs.authorized_entity set " +
            "name = ?, main_contact_id = ? " +
            "where id = ?",
          authorizedEntity.name,
          authorizedEntity.mainContactId.orNull,
          id.get
        )

        datasource.close()

        if (result.success) {
          val aeResult = AuthorizedEntityResult(SifRequestAction.Update, SifRequestAction.getSuccessStatusCode(SifRequestAction.Update), result)
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

  private def getAuthorizedEntityIdFromRequestParameters(parameters: List[SifRequestParameter]): Option[Int] = {
    var result: Option[Int] = None
    try {
      val id = getIdFromRequestParameters(parameters)
      if (id.isDefined) {
        result = Some(id.get.toInt)
      }
    } catch {
      case e: Exception =>
        result = Some(-1)
    }
    result
  }

}
