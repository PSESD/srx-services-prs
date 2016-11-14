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

/** Represents a PRS Data Set Object.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DataObject(
               val id: Int,
               val dataSetId: Int,
               val name: String,
               val filterType: String,
               val includeStatement: String
             ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <dataObject>
      <id>{id.toString}</id>
      <dataSetId>{dataSetId.toString}</dataSetId>
      <filterType>{filterType}</filterType>
      <sifObjectName>{name}</sifObjectName>
      <includeStatement>{includeStatement}</includeStatement>
    </dataObject>
  }

}

/** Represents a PRS Data Set method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DataObjectResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  DataObject.getDataObjectsFromResult,
  <dataObjects/>,
  responseFormat
) {
}

/** PRS Data Set Object methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object DataObject extends PrsEntityService {
  def apply(id: Int, dataSetId: Int, name: String, filterType: String, includeStatement: String): DataObject = new DataObject(id, dataSetId, name, filterType, includeStatement)

  def apply(dataObjectXml: Node, parameters: Option[List[SifRequestParameter]]): DataObject = {
    if (dataObjectXml == null) {
      throw new ArgumentNullException("dataObjectXml parameter")
    }
    val rootElementName = dataObjectXml.label
    if (rootElementName != "dataObject" && rootElementName != "root") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    var dataSetId = (dataObjectXml \ "dataSetId").textOption.getOrElse("0").toInt
    if(dataSetId < 1) {
      val dataSetIdParam = {if (parameters.isDefined) parameters.get.find(p => p.key.toLowerCase == "datasetid") else None}
      if (dataSetIdParam.isDefined) {
        dataSetId = dataSetIdParam.get.value.toInt
      }
    }
    val id = (dataObjectXml \ "id").textOption.getOrElse("0").toInt
    val name = (dataObjectXml \ "sifObjectName").textRequired("dataObject.sifObjectName")
    val filterType = (dataObjectXml \ "filterType").textRequired("dataObject.filterType")
    val includeStatement = (dataObjectXml \ "includeStatement").textRequired("dataObject.includeStatement")
    new DataObject(
      id,
      dataSetId,
      name,
      filterType,
      includeStatement
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val dataObject = resource.asInstanceOf[DataObject]

      val dataSetIdParam = parameters.find(p => p.key.toLowerCase == "datasetid")
      if (dataObject.dataSetId < 1 && (dataSetIdParam == null || dataSetIdParam.isEmpty)) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("dataSetId parameter"))
      } else {

        val dataSetId = if (dataObject.dataSetId > 0) dataObject.dataSetId else dataSetIdParam.get.value.toInt

        val datasource = new Datasource(datasourceConfig)

        val result: DatasourceResult = datasource.create(
          "insert into srx_services_prs.data_object (" +
            "id, data_set_id, name, filter_type, include_statement) values (" +
            "DEFAULT, ?, ?, ?, ?) " +
            "RETURNING id;",
          "id",
          dataSetId,
          dataObject.name,
          dataObject.filterType,
          dataObject.includeStatement
        )

        datasource.close()

        if (result.success) {
          val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
          if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(result.id.get.toInt), None)
            new DataObjectResult(
              SifRequestAction.Create,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
              queryResult,
              responseFormat
            )
          } else {
            new DataObjectResult(
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
          "delete from srx_services_prs.data_object where id = ?;",
          id.get
        )

        datasource.close()

        if (result.success) {
          val dsResult = new DataObjectResult(
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
      val dataSetIdParam = parameters.find(p => p.key.toLowerCase == "datasetid")
      if (id.isEmpty && (dataSetIdParam == null || dataSetIdParam.isEmpty)) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("dataSetId parameter"))
      } else {
        try {
          val result = executeQuery(id, dataSetIdParam)
          if (result.success) {
            if (id.isDefined && result.rows.isEmpty) {
              SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.DataObjects.toString))
            } else {
              new DataObjectResult(
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

  private def executeQuery(id: Option[Int], dataSetIdParam: Option[SifRequestParameter]): DatasourceResult = {
    val selectFrom = "select srx_services_prs.data_object.* from srx_services_prs.data_object"
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        datasource.get(selectFrom + " where srx_services_prs.data_object.data_set_id = ? order by srx_services_prs.data_object.id;", dataSetIdParam.get.value.toInt)
      } else {
        datasource.get(selectFrom + " where srx_services_prs.data_object.id = ?;", id.get)
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

      val dataObject = resource.asInstanceOf[DataObject]
      if ((id.isEmpty || id.get == 0) && dataObject.id > 0) {
        id = Some(dataObject.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val dataSetIdParam = parameters.find(p => p.key.toLowerCase == "datasetid")
        if (dataObject.dataSetId < 1 && (dataSetIdParam == null || dataSetIdParam.isEmpty)) {
          SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("dataSetId parameter"))
        } else {

          val dataSetId = if (dataObject.dataSetId > 0) dataObject.dataSetId else dataSetIdParam.get.value.toInt

          val datasource = new Datasource(datasourceConfig)

          val result: DatasourceResult = datasource.execute(
            "update srx_services_prs.data_object set " +
              "data_set_id = ?, " +
              "name = ?, " +
              "filter_type = ?, " +
              "include_statement = ? " +
              "where id = ?;",
            dataSetId,
            dataObject.name,
            dataObject.filterType,
            dataObject.includeStatement,
            id.get
          )

          datasource.close()

          if (result.success) {
            val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
            var dResult: DataObjectResult = null
            if(responseFormat.equals(SrxResponseFormat.Object)) {
              val queryResult = executeQuery(Some(id.get), None)
              dResult = new DataObjectResult(
                SifRequestAction.Update,
                SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
                queryResult,
                responseFormat
              )
            } else {
              dResult = new DataObjectResult(
                SifRequestAction.Update,
                SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
                result,
                responseFormat
              )
            }
            dResult.setId(id.get)
            dResult
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

  def getDataObjectsFromResult(result: DatasourceResult): List[DataObject] = {
    val dataObjects = ArrayBuffer[DataObject]()
    for (row <- result.rows) {
      val dataObject = DataObject(
        row.getString("id").getOrElse("").toInt,
        row.getString("data_set_id").getOrElse("").toInt,
        row.getString("name").getOrElse(""),
        row.getString("filter_type").getOrElse(""),
        row.getString("include_statement").getOrElse("")
      )
      dataObjects += dataObject
    }
    dataObjects.toList
  }

}
