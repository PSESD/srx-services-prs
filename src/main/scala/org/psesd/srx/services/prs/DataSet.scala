package org.psesd.srx.services.prs

import org.json4s._
import org.json4s.JsonDSL._
import org.psesd.srx.shared.core.{SrxResource, SrxResourceErrorResult, SrxResourceResult}
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter}
import org.psesd.srx.shared.data.{Datasource, DatasourceResult}

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/** Represents a PRS Data Set.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DataSet(
                        val id: Int,
                        val name: Option[String],
                        val description: Option[String],
                        val dataObjects: Option[ArrayBuffer[DataObject]]
                      ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    if(dataObjects.isDefined) {
      ("id" -> id.toString) ~
        ("name" -> name.orNull) ~
        ("description" -> description.orNull) ~
        ("dataObjects" -> dataObjects.get.map { d => d.toJson })
    } else {
      ("id" -> id.toString) ~
        ("name" -> name.orNull) ~
        ("description" -> description.orNull)
    }
  }

  def toXml: Node = {
    <dataSet>
      <id>{id.toString}</id>
      {optional(name.orNull, <name>{name.orNull}</name>)}
      {optional(description.orNull, <description>{description.orNull}</description>)}
      {optional({if(dataObjects.isDefined) "true" else null}, <dataObjects>{if(dataObjects.isDefined) dataObjects.get.map(d => d.toXml)}</dataObjects>)}
    </dataSet>
  }
}

/** Represents a PRS Data Set method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DataSetResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  DataSet.getDataSetsFromResult,
    <dataSets/>
) {
}

/** PRS Data Set methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object DataSet extends PrsEntityService {
  def apply(id: Int, name: Option[String], description: Option[String], dataObjects: Option[ArrayBuffer[DataObject]]): DataSet = new DataSet(id, name, description, dataObjects)

  def apply(dataSetXml: Node, parameters: Option[List[SifRequestParameter]]): DataSet = {
    if (dataSetXml == null) {
      throw new ArgumentNullException("dataSetXml parameter")
    }
    val rootElementName = dataSetXml.label
    if (rootElementName != "dataSet") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (dataSetXml \ "id").textOption.getOrElse("0").toInt
    val name = (dataSetXml \ "name").textOption
    val description = (dataSetXml \ "description").textOption
    val dataObjects = ArrayBuffer[DataObject]()
    for(d <- dataSetXml \ "dataObjects" \ "dataObject") {
      dataObjects += DataObject(d, None)
    }
    new DataSet(
      id,
      name,
      description,
      if(dataObjects.isEmpty) None else Some(dataObjects)
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val dataSet = resource.asInstanceOf[DataSet]

      val datasource = new Datasource(datasourceConfig)

      val result: DatasourceResult = datasource.create(
        "insert into srx_services_prs.data_set (" +
          "id, name, description) values (" +
          "DEFAULT, ?, ?) " +
          "RETURNING id;",
        "id",
        dataSet.name.orNull,
        dataSet.description.orNull
      )
      if(result.success && dataSet.dataObjects.isDefined) {
        val dataSetId = result.id.get.toInt
        val dParams = List[SifRequestParameter](SifRequestParameter("dataSetId", dataSetId.toString))
        for (d <- dataSet.dataObjects.get) {
          val dResult = DataObject.create(d, dParams)
          if (!dResult.success) {
            throw dResult.exceptions.head
          }
        }
      }

      datasource.close()

      if (result.success) {
        new DataSetResult(SifRequestAction.Create, SifRequestAction.getSuccessStatusCode(SifRequestAction.Create), result)
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

        datasource.execute(
          "delete from srx_services_prs.data_object where data_set_id = ?;",
          id.get
        )

        val result = datasource.execute(
          "delete from srx_services_prs.data_set where id = ?;",
          id.get
        )

        datasource.close()

        if (result.success) {
          val dsResult = new DataSetResult(SifRequestAction.Delete, SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete), result)
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
      try {
        val selectFrom = "select data_set.id," +
          " data_set.name," +
          " data_set.description," +
          " data_object.id data_object_id," +
          " data_object.name data_object_name," +
          " data_object.filter_type data_object_filter_type," +
          " data_object.include_statement data_object_include_statement" +
          " from srx_services_prs.data_set " +
          " left join srx_services_prs.data_object on srx_services_prs.data_object.data_set_id = srx_services_prs.data_set.id"
        val datasource = new Datasource(datasourceConfig)
        val result = {
          if (id.isEmpty) {
            datasource.get(selectFrom + " order by data_set.id, data_object_id;")
          } else {
            datasource.get(selectFrom + " where data_set.id = ?;", id.get)
          }
        }
        datasource.close()
        if (result.success) {
          if (id.isDefined && result.rows.isEmpty) {
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.DataSets.toString))
          } else {
            new DataSetResult(SifRequestAction.Query, SifHttpStatusCode.Ok, result)
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

      val dataSet = resource.asInstanceOf[DataSet]
      if ((id.isEmpty || id.get == 0) && dataSet.id > 0) {
        id = Some(dataSet.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        val result: DatasourceResult = datasource.execute(
          "update srx_services_prs.data_set set " +
            "name = ?, " +
            "description = ? " +
            "where id = ?;",
          dataSet.name.orNull,
          dataSet.description.orNull,
          id.get
        )

        if(result.success && dataSet.dataObjects.isDefined) {
          val dataSetId = id.get
          val deleteSql = new StringBuilder("delete from srx_services_prs.data_object where data_set_id = ? and id not in (")
          var sep = ""
          for (dataObject <- dataSet.dataObjects.get) {
            deleteSql.append(sep + dataObject.id.toString)
            sep = ", "
            val dsResult: DatasourceResult = datasource.executeNoReturn(
              "select srx_services_prs.update_data_set_data_object(?, ?, ?, ?, ?);",
              dataSetId,
              dataObject.id,
              dataObject.name,
              dataObject.filterType,
              dataObject.includeStatement
            )
            if (!dsResult.success) {
              throw dsResult.exceptions.head
            }
          }
          if(sep != "") {
            deleteSql.append(");")
            val deleteResult: DatasourceResult = datasource.execute(
              deleteSql.toString,
              dataSetId
            )
            if (!deleteResult.success) {
              throw deleteResult.exceptions.head
            }
          }
        }

        datasource.close()

        if (result.success) {
          val aeResult = new DataSetResult(SifRequestAction.Update, SifRequestAction.getSuccessStatusCode(SifRequestAction.Update), result)
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

  def getDataSetsFromResult(result: DatasourceResult): List[DataSet] = {
    val dataSets = ArrayBuffer[DataSet]()
    for (row <- result.rows) {
      val id = row.getString("id").getOrElse("").toInt
      val dataObjectId = row.getString("data_object_id")
      val dataSet = dataSets.find(ds => ds.id.equals(id))
      if (dataSet.isDefined) {
        if(dataObjectId.isDefined) {
          dataSet.get.dataObjects.get += DataObject(
            dataObjectId.get.toInt,
            id,
            row.getString("data_object_name").orNull,
            row.getString("data_object_filter_type").orNull,
            row.getString("data_object_include_statement").orNull
          )
        }
      } else {
        dataSets += DataSet(
          id,
          row.getString("name"),
          row.getString("description"),
          {
            if(dataObjectId.isDefined)
              Some(ArrayBuffer[DataObject](DataObject(
                dataObjectId.get.toInt,
                id,
                row.getString("data_object_name").orNull,
                row.getString("data_object_filter_type").orNull,
                row.getString("data_object_include_statement").orNull
              )))
            else
              Some(ArrayBuffer[DataObject]())
          }
        )
      }
    }
    dataSets.toList
  }

}
