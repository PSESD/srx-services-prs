package org.psesd.srx.services.prs

import org.json4s.JValue
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter, SifRequestParameterCollection}
import org.psesd.srx.shared.data.{Datasource, DatasourceResult}

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/** Represents a PRS Data Set Object.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DistrictServicePersonnel(
                  val id: Int,
                  val districtServiceId: Int,
                  val personnelId: Int,
                  val role: String
                ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <districtServicePersonnel>
      <id>{id.toString}</id>
      <districtServiceId>{districtServiceId.toString}</districtServiceId>
      <personnelId>{personnelId.toString}</personnelId>
      <role>{role}</role>
    </districtServicePersonnel>
  }

}

/** Represents a PRS Data Set method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DistrictServicePersonnelResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  DistrictServicePersonnel.getDistrictServicePersonnelFromResult,
  <districtServicePersonnels/>,
  responseFormat
) {
}

/** PRS Data Set Object methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object DistrictServicePersonnel extends PrsEntityService {
  def apply(id: Int, districtServiceId: Int, personnelId: Int, role: String): DistrictServicePersonnel = new DistrictServicePersonnel(id, districtServiceId, personnelId, role)

  def apply(requestBody: SrxRequestBody, parameters: Option[List[SifRequestParameter]]): DistrictServicePersonnel = {
    if (requestBody == null) {
      throw new ArgumentNullException("requestBody parameter")
    }
    apply(requestBody.getXml.orNull, parameters)
  }

  def apply(districtServicePersonnelXml: Node, parameters: Option[List[SifRequestParameter]]): DistrictServicePersonnel = {
    if (districtServicePersonnelXml == null) {
      throw new ArgumentNullException("districtServicePersonnelXml parameter")
    }
    val rootElementName = districtServicePersonnelXml.label
    if (rootElementName != "districtServicePersonnel" && rootElementName != "root") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (districtServicePersonnelXml \ "id").textOption.getOrElse("0").toInt
    val districtServiceId = (districtServicePersonnelXml \ "districtServiceId").textOption.getOrElse("0").toInt
    val personnelId = (districtServicePersonnelXml \ "personnelId").textOption.getOrElse("0").toInt
    val role = (districtServicePersonnelXml \ "role").textRequired("districtServicePersonnel.role")
    new DistrictServicePersonnel(
      id,
      districtServiceId,
      personnelId,
      role
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val districtServicePersonnel = resource.asInstanceOf[DistrictServicePersonnel]

      val districtServiceIdParam = parameters.find(p => p.key.toLowerCase == "districtserviceid")
      if (districtServicePersonnel.districtServiceId < 1 && (districtServiceIdParam == null || districtServiceIdParam.isEmpty)) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("districtServiceId parameter"))
      } else {

        val districtServiceId = if (districtServicePersonnel.districtServiceId > 0) districtServicePersonnel.districtServiceId else districtServiceIdParam.get.value.toInt

        val datasource = new Datasource(datasourceConfig)

        val result: DatasourceResult = datasource.create(
          "insert into srx_services_prs.district_service_personnel (" +
            "id, district_service_id, personnel_id, role) values (" +
            "DEFAULT, ?, ?, ?) " +
            "RETURNING id;",
          "id",
          districtServiceId,
          districtServicePersonnel.personnelId,
          districtServicePersonnel.role
        )

        datasource.close()

        if (result.success) {
          PrsServer.logSuccessMessage(
            PrsResource.DistrictServicePersonnel.toString,
            SifRequestAction.Create.toString,
            result.id,
            SifRequestParameterCollection(parameters),
            Some(districtServicePersonnel.toXml.toXmlString)
          )
          val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
          if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(result.id.get.toInt), None)
            new DistrictServicePersonnelResult(
              SifRequestAction.Create,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
              queryResult,
              responseFormat
            )
          } else {
            new DistrictServicePersonnelResult(
              SifRequestAction.Create,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
              result,
              responseFormat
            )
          }
        } else {
          throw result.exceptions.head
        }
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
          "delete from srx_services_prs.district_service_personnel where id = ?;",
          id.get
        )

        datasource.close()

        if (result.success) {
          PrsServer.logSuccessMessage(
            PrsResource.DistrictServicePersonnel.toString,
            SifRequestAction.Delete.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            None
          )
          val dsResult = new DistrictServicePersonnelResult(
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
      val districtServiceIdParam = parameters.find(p => p.key.toLowerCase == "districtserviceid")
      if (id.isEmpty && (districtServiceIdParam == null || districtServiceIdParam.isEmpty)) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("districtServiceId parameter"))
      } else {
        try {
          val result = executeQuery(id, districtServiceIdParam)
          if (result.success) {
            val resourceId = if (id.isEmpty) Some("all") else Some(id.get.toString)
            if (id.isDefined && result.rows.isEmpty) {
              PrsServer.logNotFoundMessage(
                PrsResource.DistrictServicePersonnel.toString,
                SifRequestAction.Query.toString,
                resourceId,
                SifRequestParameterCollection(parameters),
                None
              )
              SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.Personnel.toString))
            } else {
              PrsServer.logSuccessMessage(
                PrsResource.DistrictServicePersonnel.toString,
                SifRequestAction.Query.toString,
                resourceId,
                SifRequestParameterCollection(parameters),
                None
              )
              new DistrictServicePersonnelResult(
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

  private def executeQuery(id: Option[Int], districtServiceIdParam: Option[SifRequestParameter]): DatasourceResult = {
    val selectFrom = "select * from srx_services_prs.district_service_personnel"
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        datasource.get(selectFrom + " where district_service_id = ? order by id;", districtServiceIdParam.get.value.toInt)
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

      val districtServicePersonnel = resource.asInstanceOf[DistrictServicePersonnel]
      if ((id.isEmpty || id.get == 0) && districtServicePersonnel.id > 0) {
        id = Some(districtServicePersonnel.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val districtServiceIdParam = parameters.find(p => p.key.toLowerCase == "districtserviceid")
        if (districtServicePersonnel.districtServiceId < 1 && (districtServiceIdParam == null || districtServiceIdParam.isEmpty)) {
          SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("districtServiceId parameter"))
        } else {

          val districtServiceId = if (districtServicePersonnel.districtServiceId > 0) districtServicePersonnel.districtServiceId else districtServiceIdParam.get.value.toInt

          val datasource = new Datasource(datasourceConfig)

          val result: DatasourceResult = datasource.execute(
            "update srx_services_prs.district_service_personnel set " +
              "district_service_id = ?, " +
              "personnel_id = ?, " +
              "role = ? " +
              "where id = ?;",
            districtServiceId,
            districtServicePersonnel.personnelId,
            districtServicePersonnel.role,
            id.get
          )

          datasource.close()

          if (result.success) {
            PrsServer.logSuccessMessage(
              PrsResource.DistrictServicePersonnel.toString,
              SifRequestAction.Update.toString,
              Some(id.get.toString),
              SifRequestParameterCollection(parameters),
              Some(districtServicePersonnel.toXml.toXmlString)
            )
            val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
            var dspResult: DistrictServicePersonnelResult = null
            if(responseFormat.equals(SrxResponseFormat.Object)) {
              val queryResult = executeQuery(Some(id.get), None)
              dspResult = new DistrictServicePersonnelResult(
                SifRequestAction.Update,
                SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
                queryResult,
                responseFormat
              )
            } else {
              dspResult = new DistrictServicePersonnelResult(
                SifRequestAction.Update,
                SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
                result,
                responseFormat
              )
            }
            dspResult.setId(id.get)
            dspResult
          } else {
            throw result.exceptions.head
          }
        }
      }
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def getDistrictServicePersonnelFromResult(result: DatasourceResult): List[DistrictServicePersonnel] = {
    val districtServicePersonnels = ArrayBuffer[DistrictServicePersonnel]()
    for (row <- result.rows) {
      val districtServicePersonnel = DistrictServicePersonnel(
        row.getString("id").getOrElse("").toInt,
        row.getString("district_service_id").getOrElse("").toInt,
        row.getString("personnel_id").getOrElse("").toInt,
        row.getString("role").getOrElse("")
      )
      districtServicePersonnels += districtServicePersonnel
    }
    districtServicePersonnels.toList
  }

}
