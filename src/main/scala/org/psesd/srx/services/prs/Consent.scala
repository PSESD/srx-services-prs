package org.psesd.srx.services.prs

import java.sql.Date

import org.json4s.JValue
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter}
import org.psesd.srx.shared.core.{SrxResource, SrxResourceErrorResult, SrxResourceResult, SrxResponseFormat}
import org.psesd.srx.shared.data.{Datasource, DatasourceResult}

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/** Represents Consent.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class Consent(
               val id: Int,
               val districtServiceId: Int,
               val consentType: String,
               val startDate: Option[String],
               val endDate: Option[String]
             ) extends SrxResource with PrsEntity {

  def getEndDate: Date = {
    var result: Date = null
    if(endDate.isDefined) {
      try {
        result = Date.valueOf(endDate.get)
      } catch {
        case e: Exception =>
          throw new ArgumentInvalidException("endDate")
      }
    }
    result
  }

  def getStartDate: Date = {
    var result: Date = null
    if(startDate.isDefined) {
      try {
        result = Date.valueOf(startDate.get)
      } catch {
        case e: Exception =>
          throw new ArgumentInvalidException("startDate")
      }
    }
    result
  }

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <consent>
      <id>{id.toString}</id>
      <districtServiceId>{districtServiceId.toString}</districtServiceId>
      <consentType>{consentType.toString}</consentType>
      {optional(startDate.orNull, <startDate>{startDate.orNull}</startDate>)}
      {optional(endDate.orNull, <endDate>{endDate.orNull}</endDate>)}
    </consent>
  }

}

/** Represents Consent method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class ConsentResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  Consent.getConsentFromResult,
  <consents/>,
  responseFormat
) {
}

/** PRS Consent methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  * */
object Consent extends PrsEntityService {
  def apply(
  id: Int,
  districtServiceId: Int,
  consentType: String,
  startDate: Option[String],
  endDate: Option[String]
           ): Consent = new Consent(id, districtServiceId, consentType, startDate, endDate)

  def apply(consentXml: Node, parameters: Option[List[SifRequestParameter]]): Consent = {
    if (consentXml == null) {
      throw new ArgumentNullException("consentXml parameter")
    }
    val districtServiceIdParam = {if (parameters.isDefined) parameters.get.find(p => p.key.toLowerCase == "districtserviceid") else None}
    if (districtServiceIdParam.isEmpty) {
      throw new ArgumentNullException("districtServiceId parameter")
    }

    val rootElementName = consentXml.label
    if (rootElementName != "consent") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (consentXml \ "id").textOption.getOrElse("0").toInt
    val districtServiceId = districtServiceIdParam.get.value.toInt
    val consentType = (consentXml \ "consentType").textRequired("consent.consentType")
    val startDate = (consentXml \ "startDate").textOption
    val endDate = (consentXml \ "endDate").textOption
    new Consent(
      id,
      districtServiceId,
      consentType,
      startDate,
      endDate
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val consent = resource.asInstanceOf[Consent]

      val datasource = new Datasource(datasourceConfig)

      val result = datasource.create(
        "insert into srx_services_prs.consent (" +
          "id, district_service_id, consent_type, start_date, end_date) values (" +
          "DEFAULT, ?, ?, ?, ?) " +
          "RETURNING id;",
        "id",
        consent.districtServiceId,
        consent.consentType,
        consent.getStartDate,
        consent.getEndDate
      )

      datasource.close()

      if (result.success) {
        val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
        if(responseFormat.equals(SrxResponseFormat.Object)) {
          val queryResult = executeQuery(Some(result.id.get.toInt))
          new ConsentResult(
            SifRequestAction.Create,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
            queryResult,
            responseFormat
          )
        } else {
          new ConsentResult(
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
          "delete from srx_services_prs.consent where id = ?;",
          id.get
        )

        datasource.close()

        if (result.success) {
          val consentResult = new ConsentResult(
            SifRequestAction.Delete,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete),
            result,
            SrxResponseFormat.getResponseFormat(parameters)
          )
          consentResult.setId(id.get)
          consentResult
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
          if (id.isDefined && result.rows.isEmpty) {
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.Consents.toString))
          } else {
            new ConsentResult(
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

  private def executeQuery(id: Option[Int]): DatasourceResult = {
    val selectFrom = "select * from srx_services_prs.consent"
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        datasource.get(selectFrom + " order by id;")
      } else {
        datasource.get(selectFrom + " where id = ?;", id.get)
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

      val consent = resource.asInstanceOf[Consent]
      if ((id.isEmpty || id.get == 0) && consent.id > 0) {
        id = Some(consent.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        val result = datasource.execute(
          "update srx_services_prs.consent set " +
            "district_service_id = ?, " +
            "consent_type = ?, " +
            "start_date = ?, " +
            "end_date = ? " +
            "where id = ?;",
          consent.districtServiceId,
          consent.consentType,
          consent.getStartDate,
          consent.getEndDate,
          id.get
        )

        datasource.close()

        if (result.success) {
          val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
          var cResult: ConsentResult = null
          if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(id.get))
            cResult = new ConsentResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              queryResult,
              responseFormat
            )
          } else {
            cResult = new ConsentResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              result,
              responseFormat
            )
          }
          cResult.setId(id.get)
          cResult
        } else {
          throw result.exceptions.head
        }
      }
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def getConsentFromResult(result: DatasourceResult): List[Consent] = {
    val consentResult = ArrayBuffer[Consent]()
    for (row <- result.rows) {
      val consent = Consent(
        row.getString("id").getOrElse("").toInt,
        row.getString("district_service_id").getOrElse("").toInt,
        row.getString("consent_type").getOrElse(""),
        row.getString("start_date"),
        row.getString("end_date")
      )
      consentResult += consent
    }
    consentResult.toList
  }

}
