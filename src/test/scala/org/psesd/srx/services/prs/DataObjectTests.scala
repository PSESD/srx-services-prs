package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

import scala.collection.mutable.ArrayBuffer

class DataObjectTests extends FunSuite {

  var createdId: Int = 0

  val dataobjects = ArrayBuffer[DataObject](DataObject(0, 0, "test object", "test type", "test include"))
  val dataSet = DataSet(0, "sre", Some("firstName"), Some(dataobjects))
  val dataSetResult = DataSet.create(dataSet, List[SifRequestParameter]()).asInstanceOf[DataSetResult]

  test("constructor") {
    val id = 123
    val dataSetId = dataSetResult.getId
    val name = "test name"
    val filterType = "test filter"
    val includeStatement = "test include"
    val dataObject = new DataObject(id, dataSetId, name, filterType, includeStatement)
    assert(dataObject.id.equals(id))
    assert(dataObject.dataSetId.equals(dataSetId))
    assert(dataObject.name.equals(name))
    assert(dataObject.filterType.equals(filterType))
    assert(dataObject.includeStatement.equals(includeStatement))
  }

  test("factory") {
    val id = 123
    val dataSetId = dataSetResult.getId
    val name = "test name"
    val filterType = "test filter"
    val includeStatement = "test include"
    val dataObject = DataObject(id, dataSetId, name, filterType, includeStatement)
    assert(dataObject.id.equals(id))
    assert(dataObject.dataSetId.equals(dataSetId))
    assert(dataObject.name.equals(name))
    assert(dataObject.filterType.equals(filterType))
    assert(dataObject.includeStatement.equals(includeStatement))
  }

  test("node") {
    val id = 123
    val dataSetId = dataSetResult.getId
    val name = "test name"
    val filterType = "test filter"
    val includeStatement = "test include"
    val dataObject = DataObject(
      <dataObject>
        <id>{id}</id>
        <dataSetId>{dataSetId}</dataSetId>
        <filterType>{filterType}</filterType>
        <sifObjectName>{name}</sifObjectName>
        <includeStatement>{includeStatement}</includeStatement>
      </dataObject>,
      None
    )
    assert(dataObject.id.equals(id))
    assert(dataObject.dataSetId.equals(dataSetId))
    assert(dataObject.name.equals(name))
    assert(dataObject.filterType.equals(filterType))
    assert(dataObject.includeStatement.equals(includeStatement))
  }

  test("create") {
    val dataSetId = dataSetResult.getId
    val name = "test name"
    val filterType = "test filter"
    val includeStatement = "test include"
    val dataObject = DataObject(0, dataSetId, name, filterType, includeStatement)
    val result = DataObject.create(dataObject, List[SifRequestParameter]()).asInstanceOf[DataObjectResult]
    assert(result.success)
    assert(result.exceptions.isEmpty)
  }

  test("update id parameter") {
    val dataSetId = dataSetResult.getId
    val name = "test name UPDATED"
    val filterType = "test filter UPDATED"
    val includeStatement = "test include UPDATED"
    val dataObject = DataObject(createdId, dataSetId, name, filterType, includeStatement)
    val result = DataObject.update(dataObject, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("query bad request") {
    val result = DataObject.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = DataObject.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val dataSetId = dataSetResult.getId
    val name = "test name"
    val filterType = "test filter"
    val includeStatement = "test include"
    val dataObject = DataObject(0, dataSetId, name, filterType, includeStatement)
    val dataObjectResult = DataObject.create(dataObject, List[SifRequestParameter]()).asInstanceOf[DataObjectResult]

    val result = DataObject.query(List[SifRequestParameter](SifRequestParameter("id", dataObjectResult.getId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains(dataSetResult.getId.toString))
    assert(resultBody.contains("test name"))
    assert(resultBody.contains("test filter"))
    assert(resultBody.contains("test include"))
  }

  test("query all") {
    val result = DataObject.query(List[SifRequestParameter](SifRequestParameter("dataSetId", dataSetResult.getId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = DataObject.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

}
