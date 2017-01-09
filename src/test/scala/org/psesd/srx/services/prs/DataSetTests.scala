package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.SrxResourceErrorResult
import org.psesd.srx.shared.core.exceptions.{ArgumentNullOrEmptyOrWhitespaceException, ExceptionMessage}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

import scala.collection.mutable.ArrayBuffer

class DataSetTests extends FunSuite {

  var createdId: Int = 0

  test("constructor") {
    val id = 123
    val name = "sre"
    val description = "firstName"
    val dataSet = new DataSet(id, name, Some(description), None)
    assert(dataSet.id.equals(id))
    assert(dataSet.name.equals(name))
    assert(dataSet.description.get.equals(description))
  }

  test("constructor null name") {
    val id = 123
    val description = "firstName"
    val thrown = intercept[ArgumentNullOrEmptyOrWhitespaceException] {
      new DataSet(id, null, Some(description), None)
    }
    assert(thrown.getMessage.equals(ExceptionMessage.NotNullOrEmptyOrWhitespace.format("name")))
  }

  test("factory") {
    val id = 123
    val name = "sre"
    val description = "firstName"
    val dataSet = DataSet(id, name, Some(description), None)
    assert(dataSet.id.equals(id))
    assert(dataSet.name.equals(name))
    assert(dataSet.description.get.equals(description))
  }

  test("node") {
    val id = 123
    val name = "sre"
    val description = "firstName"
    val dataSet = DataSet(
      <dataSet>
        <id>{id}</id>
        <name>{name}</name>
        <description>{description}</description>
        <dataObjects>
          <dataObject>
            <id>456</id>
            <dataSetId>{id}</dataSetId>
            <filterType>test filter</filterType>
            <sifObjectName>test name</sifObjectName>
            <includeStatement>test include</includeStatement>
          </dataObject>
        </dataObjects>
      </dataSet>,
      None
    )
    assert(dataSet.id.equals(id))
    assert(dataSet.name.equals(name))
    assert(dataSet.description.get.equals(description))
  }

  test("node null name") {
    val id = 123
    val description = "firstName"
    val thrown = intercept[ArgumentNullOrEmptyOrWhitespaceException] {
      DataSet(
        <dataSet>
          <id>{id}</id>
          <description>{description}</description>
        </dataSet>,
        None
      )
    }
    assert(thrown.getMessage.equals(ExceptionMessage.NotNullOrEmptyOrWhitespace.format("name")))
  }

  test("node without ids") {
    val name = "sre"
    val description = "firstName"
    val dataSet = DataSet(
      <dataSet>
        <name>{name}</name>
        <description>{description}</description>
        <dataObjects>
          <dataObject>
            <filterType>test filter</filterType>
            <sifObjectName>test name</sifObjectName>
            <includeStatement>test include</includeStatement>
          </dataObject>
        </dataObjects>
      </dataSet>,
      None
    )
    assert(dataSet.id.equals(0))
    assert(dataSet.name.equals(name))
    assert(dataSet.description.get.equals(description))
  }

  test("create") {
    val dataobjects = ArrayBuffer[DataObject](DataObject(0, 0, "test object", "test type", "test include"))
    val dataSet = DataSet(0, "sre", Some("firstName"), Some(dataobjects))
    val result = DataSet.create(dataSet, List[SifRequestParameter]()).asInstanceOf[DataSetResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("create duplicate") {
    val dataSet = DataSet(0, "sre", Some("firstName"), None)
    val result = DataSet.create(dataSet, List[SifRequestParameter]()).asInstanceOf[SrxResourceErrorResult]

    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("update id parameter") {
    val dataSet = DataSet(0, "sre UPDATED", Some("firstName UPDATED"), None)
    val result = DataSet.update(dataSet, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update duplicate") {
    val newDataSet = DataSet(0, "sre", Some("firstName"), None)
    val newDataSetResult = DataSet.create(newDataSet, List[SifRequestParameter]()).asInstanceOf[DataSetResult]

    val updatedDataSet = DataSet(0, "sre UPDATED", Some("firstName UPDATED"), None)
    val result = DataSet.update(updatedDataSet, List[SifRequestParameter](SifRequestParameter("id", newDataSetResult.getId.toString))).asInstanceOf[SrxResourceErrorResult]

    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)

    DataSet.delete(List[SifRequestParameter](SifRequestParameter("id", newDataSetResult.getId.toString)))
  }

  test("query bad request") {
    val result = DataSet.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = DataSet.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val result = DataSet.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("sre UPDATED"))
    assert(resultBody.contains("firstName UPDATED"))
  }

  test("query all") {
    val result = DataSet.query(null)
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = DataSet.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

}
