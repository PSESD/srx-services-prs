package org.psesd.srx.services.prs

import java.sql.Date

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

/** Represents a PRS Data Set Object.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DistrictService(
                  val id: Int,
                  val districtId: Int,
                  val externalServiceId: Int,
                  val externalServiceName: Option[String],
                  val authorizedEntityId: Option[Int],
                  val authorizedEntityName: Option[String],
                  val initiationDate: String,
                  val expirationDate: String,
                  val requiresPersonnel: Boolean,
                  val dataSets: Option[ArrayBuffer[DataSet]]
                ) extends SrxResource with PrsEntity {

  if(initiationDate == null || initiationDate.isEmpty || getInitiationDate == null) {
    throw new ArgumentInvalidException("initiationDate")
  }
  if(expirationDate == null || expirationDate.isEmpty || getExpirationDate == null) {
    throw new ArgumentInvalidException("expirationDate")
  }

  def getInitiationDate: Date = {
    var result: Date = null
    try{
      result = Date.valueOf(initiationDate)
    } catch {
      case e: Exception =>
        throw new ArgumentInvalidException("initiationDate")
    }
    result
  }

  def getExpirationDate: Date = {
    var result: Date = null
    try{
      result = Date.valueOf(expirationDate)
    } catch {
      case e: Exception =>
        throw new ArgumentInvalidException("expirationDate")
    }
    result
  }

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
    if(dataSets.isDefined) {
      ("id" -> id.toString) ~
        ("districtId" -> districtId.toString) ~
        ("externalServiceId" -> externalServiceId.toString) ~
        ("externalServiceName" -> externalServiceName.getOrElse("")) ~
        ("authorizedEntityId" -> {if(authorizedEntityId.isDefined) authorizedEntityId.get.toString else null}) ~
        ("initiationDate" -> initiationDate) ~
        ("expirationDate" -> expirationDate) ~
        ("requiresPersonnel" -> requiresPersonnel.toString) ~
        ("dataSets" -> dataSets.get.map { d => d.toJson })
    } else {
      ("id" -> id.toString) ~
        ("districtId" -> districtId.toString) ~
        ("externalServiceId" -> externalServiceId.toString) ~
        ("externalServiceName" -> externalServiceName.getOrElse("")) ~
        ("authorizedEntityId" -> {if(authorizedEntityId.isDefined) authorizedEntityId.get.toString else null}) ~
        ("initiationDate" -> initiationDate) ~
        ("expirationDate" -> expirationDate) ~
        ("requiresPersonnel" -> requiresPersonnel.toString)
    }
  }

  def toXml: Node = {
    <districtService>
      <id>{id.toString}</id>
      <districtId>{districtId.toString}</districtId>
      <externalServiceId>{externalServiceId.toString}</externalServiceId>
      {optional(externalServiceName.orNull, <externalServiceName>{externalServiceName.orNull}</externalServiceName>)}
      {optional({if (authorizedEntityId.isDefined) authorizedEntityId.get.toString else null}, <authorizedEntityId>{authorizedEntityId.orNull}</authorizedEntityId>)}
      {optional(authorizedEntityName.orNull, <authorizedEntityName>{authorizedEntityName.orNull}</authorizedEntityName>)}
      <initiationDate>{initiationDate}</initiationDate>
      <expirationDate>{expirationDate}</expirationDate>
      <requiresPersonnel>{requiresPersonnel.toString}</requiresPersonnel>
      {optional({if(dataSets.isDefined) "true" else null}, <dataSets>{if(dataSets.isDefined) dataSets.get.map(ds => ds.toXml)}</dataSets>)}
    </districtService>
  }

}

/** Represents a PRS Data Set method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DistrictServiceResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  DistrictService.getDistrictServicesFromResult,
  <districtServices/>,
  responseFormat
) {
}

/** PRS Data Set Object methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object DistrictService extends PrsEntityService {
  def apply(
      id: Int,
      districtId: Int,
      externalServiceId: Int,
      externalServiceName: Option[String],
      authorizedEntityId: Option[Int],
      authorizedEntityName: Option[String],
      initiationDate: String,
      expirationDate: String,
      requiresPersonnel: Boolean,
      dataSets: Option[ArrayBuffer[DataSet]]
  ): DistrictService = new DistrictService(
    id,
    districtId,
    externalServiceId,
    externalServiceName,
    authorizedEntityId,
    authorizedEntityName,
    initiationDate,
    expirationDate,
    requiresPersonnel,
    dataSets
  )

  def apply(requestBody: SrxRequestBody, parameters: Option[List[SifRequestParameter]]): DistrictService = {
    if (requestBody == null) {
      throw new ArgumentNullException("requestBody parameter")
    }
    apply(requestBody.getXml.orNull, parameters)
  }

  def apply(districtServiceXml: Node, parameters: Option[List[SifRequestParameter]]): DistrictService = {
    if (districtServiceXml == null) {
      throw new ArgumentNullException("districtServiceXml parameter")
    }
    val districtIdParam = {if (parameters.isDefined) parameters.get.find(p => p.key.toLowerCase == "districtid") else None}
    if (districtIdParam.isEmpty) {
      throw new ArgumentNullException("districtId parameter")
    }
    val rootElementName = districtServiceXml.label
    if (rootElementName != "districtService" && rootElementName != "root") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (districtServiceXml \ "id").textOption.getOrElse("0").toInt
    val districtId = districtIdParam.get.value.toInt
    val externalServiceId = (districtServiceXml \ "externalServiceId").textRequired("districtService.externalServiceId").toInt
    val externalServiceName = (districtServiceXml \ "externalServiceName").textOption
    val authorizedEntityId = (districtServiceXml \ "authorizedEntityId").textOption
    val authorizedEntityName = (districtServiceXml \ "authorizedEntityName").textOption
    val initiationDate = (districtServiceXml \ "initiationDate").textRequired("districtService.initiationDate")
    val expirationDate = (districtServiceXml \ "expirationDate").textRequired("districtService.expirationDate")
    val requiresPersonnel = (districtServiceXml \ "requiresPersonnel").textOption
    val dataSets = ArrayBuffer[DataSet]()
    for(ds <- districtServiceXml \ "dataSets" \ "dataSet") {
      dataSets += DataSet(ds, None)
    }
    new DistrictService(
      id,
      districtId,
      externalServiceId,
      externalServiceName,
      {
        if(authorizedEntityId.isDefined) Some(authorizedEntityId.get.toInt) else None
      },
      authorizedEntityName,
      initiationDate,
      expirationDate,
      requiresPersonnel.isDefined && (requiresPersonnel.get.toLowerCase == "true"),
      if(dataSets.isEmpty) None else Some(dataSets)
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val districtService = resource.asInstanceOf[DistrictService]

      val datasource = new Datasource(datasourceConfig)

      val result: DatasourceResult = datasource.create(
        "insert into srx_services_prs.district_service (" +
          "id, district_id, external_service_id, requires_personnel, initiation_date, expiration_date) values (" +
          "DEFAULT, ?, ?, ?, ?, ?) " +
          "RETURNING id;",
        "id",
        districtService.districtId,
        districtService.externalServiceId,
        districtService.requiresPersonnel,
        districtService.getInitiationDate,
        districtService.getExpirationDate
      )

      if(result.success && districtService.dataSets.isDefined) {
        val districtServiceId = result.id.get.toInt
        for (ds <- districtService.dataSets.get) {
          val dsResult: DatasourceResult = datasource.create(
            "insert into srx_services_prs.district_service_data_set (" +
              "id, district_service_id, data_set_id) values (" +
              "DEFAULT, ?, ?) " +
              "RETURNING id;",
            "id",
            districtServiceId,
            ds.id
          )

          val dsi = districtServiceId
          val dsid = ds.id

          if (!dsResult.success) {
            throw dsResult.exceptions.head
          }
        }
      }

      datasource.close()

      if (result.success) {
        PrsServer.logPrsMessage(
          PrsResource.DistrictServices.toString,
          SifRequestAction.Create.toString,
          result.id,
          SifRequestParameterCollection(parameters),
          Some(districtService.toXml.toXmlString)
        )
        val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
        if(responseFormat.equals(SrxResponseFormat.Object)) {
          val queryResult = executeQuery(Some(result.id.get.toInt), None)
          new DistrictServiceResult(
            SifRequestAction.Create,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
            queryResult,
            responseFormat
          )
        } else {
          new DistrictServiceResult(
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

        datasource.execute(
          "delete from srx_services_prs.district_service_data_set where district_service_id = ?;",
          id.get
        )

        val result = datasource.execute(
          "delete from srx_services_prs.district_service where id = ?;",
          id.get
        )

        datasource.close()

        if (result.success) {
          PrsServer.logPrsMessage(
            PrsResource.DistrictServices.toString,
            SifRequestAction.Delete.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            None
          )
          val dsResult = new DistrictServiceResult(
            SifRequestAction.Delete,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete),
            result,
            SrxResponseFormat.getResponseFormat(parameters)
          )
          dsResult.setId(id.get)
          dsResult
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
      val districtIdParam = parameters.find(p => p.key.toLowerCase == "districtid")
      if (id.isEmpty && (districtIdParam == null || districtIdParam.isEmpty)) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("districtId parameter"))
      } else {
        try {
          val result = executeQuery(id, districtIdParam)
          if (result.success) {
            val resourceId = if (id.isEmpty) Some("all") else Some(id.get.toString)
            PrsServer.logPrsMessage(
              PrsResource.DistrictServices.toString,
              SifRequestAction.Query.toString,
              resourceId,
              SifRequestParameterCollection(parameters),
              None
            )
            if (id.isDefined && result.rows.isEmpty) {
              SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.ExternalServices.toString))
            } else {
              new DistrictServiceResult(
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

  private def executeQuery(id: Option[Int], districtIdParam: Option[SifRequestParameter]): DatasourceResult = {
    val selectFrom = "select district_service.id," +
      " district_service.district_id," +
      " district_service.external_service_id," +
      " external_service.name external_service_name," +
      " external_service.authorized_entity_id," +
      " authorized_entity.name authorized_entity_name," +
      " district_service.initiation_date," +
      " district_service.expiration_date," +
      " district_service.requires_personnel," +
      " data_set.id data_set_id," +
      " data_set.name data_set_name," +
      " data_set.description data_set_description" +
      " from srx_services_prs.district_service" +
      " left join srx_services_prs.district_service_data_set on srx_services_prs.district_service_data_set.district_service_id = srx_services_prs.district_service.id" +
      " left join srx_services_prs.data_set on srx_services_prs.data_set.id = srx_services_prs.district_service_data_set.data_set_id" +
      " left join srx_services_prs.district on srx_services_prs.district.id = srx_services_prs.district_service.district_id" +
      " left join srx_services_prs.external_service on srx_services_prs.external_service.id = srx_services_prs.district_service.external_service_id" +
      " left join srx_services_prs.authorized_entity on srx_services_prs.authorized_entity.id = srx_services_prs.external_service.authorized_entity_id"
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        datasource.get(selectFrom + " where srx_services_prs.district_service.district_id = ? order by srx_services_prs.district_service.id;", districtIdParam.get.value.toInt)
      } else {
        datasource.get(selectFrom + " where srx_services_prs.district_service.id = ?;", id.get)
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

      val districtService = resource.asInstanceOf[DistrictService]
      if ((id.isEmpty || id.get == 0) && districtService.id > 0) {
        id = Some(districtService.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        val result: DatasourceResult = datasource.execute(
          "update srx_services_prs.district_service set " +
            "district_id = ?, " +
            "external_service_id = ?, " +
            "requires_personnel = ?, " +
            "initiation_date = ?, " +
            "expiration_date = ? " +
            "where id = ?;",
          districtService.districtId,
          districtService.externalServiceId,
          districtService.requiresPersonnel,
          districtService.getInitiationDate,
          districtService.getExpirationDate,
          id.get
        )

        if(result.success && districtService.dataSets.isDefined) {
          val districtServiceId = id.get
          val deleteSql = new StringBuilder("delete from srx_services_prs.district_service_data_set where district_service_id = ? and data_set_id not in (")
          var sep = ""
          for (dataSet <- districtService.dataSets.get) {
            deleteSql.append(sep + dataSet.id.toString)
            sep = ", "
            val dsResult: DatasourceResult = datasource.executeNoReturn(
              "select srx_services_prs.update_district_service_data_set(?, ?);",
              districtServiceId,
              dataSet.id
            )

            if (!dsResult.success) {
              throw dsResult.exceptions.head
            }
          }
          if(sep != "") {
            deleteSql.append(");")
            val deleteResult: DatasourceResult = datasource.execute(
              deleteSql.toString,
              districtServiceId
            )
            if (!deleteResult.success) {
              throw deleteResult.exceptions.head
            }
          }
        }

        datasource.close()
val res = result
        if (result.success) {
          PrsServer.logPrsMessage(
            PrsResource.DistrictServices.toString,
            SifRequestAction.Update.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            Some(districtService.toXml.toXmlString)
          )
          val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
          var dsResult: DistrictServiceResult = null
          if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(id.get), None)
            dsResult = new DistrictServiceResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              queryResult,
              responseFormat
            )
          } else {
            dsResult = new DistrictServiceResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              result,
              responseFormat
            )
          }
          dsResult.setId(id.get)
          dsResult
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

  def getDistrictServicesFromResult(result: DatasourceResult): List[DistrictService] = {
    val districtServices = ArrayBuffer[DistrictService]()
    for (row <- result.rows) {
      val id = row.getString("id").getOrElse("").toInt
      val dataSetId = row.getString("data_set_id")
      val districtService = districtServices.find(ds => ds.id.equals(id))
      if (districtService.isDefined) {
        if(dataSetId.isDefined && !dataSetId.get.isNullOrEmpty) {
          districtService.get.dataSets.get += DataSet(
            dataSetId.get.toInt,
            row.getString("data_set_name").orNull,
            row.getString("data_set_description"),
            None
          )
        }
      } else {
        districtServices += DistrictService(
          id,
          row.getString("district_id").getOrElse("").toInt,
          row.getString("external_service_id").getOrElse("").toInt,
          row.getString("external_service_name"), {
            val authorizedEntityId = row.getString("authorized_entity_id")
            if (authorizedEntityId.isDefined && !authorizedEntityId.get.isNullOrEmpty) {
              Some(authorizedEntityId.get.toInt)
            } else {
              None
            }
          },
          row.getString("authorized_entity_name"),
          row.getString("initiation_date").getOrElse(""),
          row.getString("expiration_date").getOrElse(""),
          row.getBoolean("requires_personnel").getOrElse(false),
          {
            if(dataSetId.isDefined && !dataSetId.get.isNullOrEmpty) Some(ArrayBuffer[DataSet](DataSet(
              dataSetId.get.toInt,
              row.getString("data_set_name").orNull,
              row.getString("data_set_description"),
              None
            ))) else Some(ArrayBuffer[DataSet]())
          }
        )
      }
    }
    districtServices.toList
  }

}
