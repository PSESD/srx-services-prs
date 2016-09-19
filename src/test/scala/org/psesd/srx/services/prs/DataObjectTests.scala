package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class DataObjectTests extends FunSuite {

  var createdId: Int = 0

  test("constructor") {
    val id = 123
    val dataSetId = 456
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
    val dataSetId = 456
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
    val dataSetId = 456
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
    val dataSetId = 456
    val name = "test name"
    val filterType = "test filter"
    val includeStatement = "test include"
    val dataObject = DataObject(0, dataSetId, name, filterType, includeStatement)
    val result = DataObject.create(dataObject, List[SifRequestParameter]()).asInstanceOf[DataObjectResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update id parameter") {
    val dataSetId = 654
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
    val result = DataObject.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("654"))
    assert(resultBody.contains("test name UPDATED"))
    assert(resultBody.contains("test filter UPDATED"))
    assert(resultBody.contains("test include UPDATED"))
  }

  test("query all") {
    val result = DataObject.query(List[SifRequestParameter](SifRequestParameter("dataSetId", "654")))
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
