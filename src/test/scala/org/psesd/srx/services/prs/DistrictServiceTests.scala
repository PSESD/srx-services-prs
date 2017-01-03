package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ExceptionMessage}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

import scala.collection.mutable.ArrayBuffer

class DistrictServiceTests extends FunSuite {

  var createdId: Int = 0

  val districtContact = new Contact(0, Some("jon"), Some("director"), Some("jon@doe.com"), Some("555-1212"), Some("123 Spring St"), Some("jon.com"))
  val district = District(0, "test", None, None, Some(districtContact))
  val districtResult = District.create(district, List[SifRequestParameter]()).asInstanceOf[DistrictResult]

  val authorizedEntityContact = new Contact(0, Some("jon"), Some("director"), Some("jon@doe.com"), Some("555-1212"), Some("123 Spring St"), Some("jon.com"))
  val authorizedEntity = AuthorizedEntity(0, "test", Some(authorizedEntityContact))
  val authorizedEntityResult = AuthorizedEntity.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[AuthorizedEntityResult]

  val externalService = ExternalService(0, authorizedEntityResult.getId, Some("test"), Some("test service description"))
  val externalServiceResult = ExternalService.create(externalService, List[SifRequestParameter]()).asInstanceOf[ExternalServiceResult]

  val dataobjects1 = ArrayBuffer[DataObject](DataObject(0, 0, "test object", "test type", "test include"))
  val dataSet1 = DataSet(0, "sre", Some("firstName"), Some(dataobjects1))
  val dataSetResult1 = DataSet.create(dataSet1, List[SifRequestParameter]()).asInstanceOf[DataSetResult]

  val dataobjects2 = ArrayBuffer[DataObject](DataObject(0, 0, "test object", "test type", "test include"))
  val dataSet2 = DataSet(0, "sre", Some("firstName"), Some(dataobjects2))
  val dataSetResult2 = DataSet.create(dataSet2, List[SifRequestParameter]()).asInstanceOf[DataSetResult]

  test("constructor") {
    val id = 123
    val districtId = districtResult.getId
    val externalServiceId = externalServiceResult.getId
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(authorizedEntityResult.getId)
    val authorizedEntityName = Some("test entity")
    val initiationDate = "2016-01-01"
    val expirationDate = "2017-01-01"
    val requiresPersonnel = true
    val districtService = new DistrictService(
      id,
      districtId,
      externalServiceId,
      externalServiceName,
      authorizedEntityId,
      authorizedEntityName,
      initiationDate,
      expirationDate,
      requiresPersonnel,
      None
    )
    assert(districtService.id.equals(id))
    assert(districtService.districtId.equals(districtId))
    assert(districtService.externalServiceId.equals(externalServiceResult.getId))
    assert(districtService.externalServiceName.get.equals(externalServiceName.get))
    assert(districtService.authorizedEntityId.get.equals(authorizedEntityResult.getId))
    assert(districtService.authorizedEntityName.get.equals(authorizedEntityName.get))
    assert(districtService.initiationDate.equals(initiationDate))
    assert(districtService.expirationDate.equals(expirationDate))
    assert(districtService.requiresPersonnel.equals(requiresPersonnel))
  }

  test("factory") {
    val id = 123
    val districtId = districtResult.getId
    val externalServiceId = externalServiceResult.getId
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(authorizedEntityResult.getId)
    val authorizedEntityName = Some("test entity")
    val initiationDate = "2016-01-01"
    val expirationDate = "2017-01-01"
    val requiresPersonnel = true
    val districtService = DistrictService(
      id,
      districtId,
      externalServiceId,
      externalServiceName,
      authorizedEntityId,
      authorizedEntityName,
      initiationDate,
      expirationDate,
      requiresPersonnel,
      None
    )
    assert(districtService.id.equals(id))
    assert(districtService.districtId.equals(districtId))
    assert(districtService.externalServiceId.equals(externalServiceId))
    assert(districtService.externalServiceName.get.equals(externalServiceName.get))
    assert(districtService.authorizedEntityId.get.equals(authorizedEntityId.get))
    assert(districtService.authorizedEntityName.get.equals(authorizedEntityName.get))
    assert(districtService.initiationDate.equals(initiationDate))
    assert(districtService.expirationDate.equals(expirationDate))
    assert(districtService.requiresPersonnel.equals(requiresPersonnel))
  }

  test("node") {
    val id = 123
    val districtId = districtResult.getId
    val externalServiceId = externalServiceResult.getId
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(authorizedEntityResult.getId)
    val authorizedEntityName = Some("test entity")
    val initiationDate = "2016-01-01"
    val expirationDate = "2017-01-01"
    val requiresPersonnel = true
    val districtService = DistrictService(
      <districtService>
        <id>{id}</id>
        <externalServiceId>{externalServiceId}</externalServiceId>
        <externalServiceName>{externalServiceName.get}</externalServiceName>
        <authorizedEntityId>{authorizedEntityId.get.toString}</authorizedEntityId>
        <authorizedEntityName>{authorizedEntityName.get}</authorizedEntityName>
        <initiationDate>{initiationDate}</initiationDate>
        <expirationDate>{expirationDate}</expirationDate>
        <requiresPersonnel>{requiresPersonnel.toString}</requiresPersonnel>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", districtId.toString)))
    )
    assert(districtService.id.equals(id))
    assert(districtService.districtId.equals(districtId))
    assert(districtService.externalServiceId.equals(externalServiceId))
    assert(districtService.externalServiceName.get.equals(externalServiceName.get))
    assert(districtService.authorizedEntityId.get.equals(authorizedEntityId.get))
    assert(districtService.authorizedEntityName.get.equals(authorizedEntityName.get))
    assert(districtService.initiationDate.equals(initiationDate))
    assert(districtService.expirationDate.equals(expirationDate))
    assert(districtService.requiresPersonnel.equals(requiresPersonnel))
  }

  test("invalid initiationDate") {
    val id = 123
    val districtId = districtResult.getId
    val externalServiceId = externalServiceResult.getId
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(authorizedEntityResult.getId)
    val authorizedEntityName = Some("test entity")
    val initiationDate = "not_a_date"
    val expirationDate = "2017-01-01"
    val requiresPersonnel = true
    val thrown = intercept[ArgumentInvalidException] {
      val districtService = DistrictService(
        id,
        districtId,
        externalServiceId,
        externalServiceName,
        authorizedEntityId,
        authorizedEntityName,
        initiationDate,
        expirationDate,
        requiresPersonnel,
        None
      )
    }
    assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("initiationDate")))
  }

  test("invalid expirationDate") {
    val id = 123
    val districtId = districtResult.getId
    val externalServiceId = externalServiceResult.getId
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(authorizedEntityResult.getId)
    val authorizedEntityName = Some("test entity")
    val initiationDate = "2016-01-01"
    val expirationDate = "not_a_date"
    val requiresPersonnel = true
    val thrown = intercept[ArgumentInvalidException] {
      val districtService = DistrictService(
        id,
        districtId,
        externalServiceId,
        externalServiceName,
        authorizedEntityId,
        authorizedEntityName,
        initiationDate,
        expirationDate,
        requiresPersonnel,
        None
      )
    }
    assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("expirationDate")))
  }

  test("create") {
    val districtService = DistrictService(
      <districtService>
        <externalServiceId>{externalServiceResult.getId}</externalServiceId>
        <initiationDate>2016-01-01</initiationDate>
        <expirationDate>2017-01-01</expirationDate>
        <requiresPersonnel>true</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>{dataSetResult1.getId}</id>
            <name>sre</name>
          </dataSet>
          <dataSet>
            <id>{dataSetResult2.getId}</id>
            <name>sre</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", {districtResult.getId.toString})))
    )
    val result = DistrictService.create(districtService, List[SifRequestParameter]()).asInstanceOf[DistrictServiceResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update") {
    val districtService = DistrictService(
      <districtService>
        <externalServiceId>{externalServiceResult.getId}</externalServiceId>
        <initiationDate>2016-01-01</initiationDate>
        <expirationDate>2017-01-01</expirationDate>
        <requiresPersonnel>true</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>{dataSetResult1.getId}</id>
            <name>sre</name>
          </dataSet>
          <dataSet>
            <id>{dataSetResult2.getId}</id>
            <name>sre</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", {districtResult.getId.toString})))
    )
    val districtServiceResult = DistrictService.create(districtService, List[SifRequestParameter]()).asInstanceOf[DistrictServiceResult]

    val districtServiceUpdated = DistrictService(
      <districtService>
        <externalServiceId>{externalServiceResult.getId}</externalServiceId>
        <initiationDate>2015-01-01</initiationDate>
        <expirationDate>2018-01-01</expirationDate>
        <requiresPersonnel>false</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>{dataSetResult1.getId}</id>
            <name>sre</name>
          </dataSet>
          <dataSet>
            <id>{dataSetResult2.getId}</id>
            <name>sre</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", {districtResult.getId.toString})))
    )

    val result = DistrictService.update(districtServiceUpdated, List[SifRequestParameter](SifRequestParameter("id", districtServiceResult.getId.toString)))

    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(districtServiceResult.getId.toString)))
  }

  test("query bad request") {
    val result = DistrictService.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = DistrictService.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val districtService = DistrictService(
      <districtService>
        <externalServiceId>{externalServiceResult.getId}</externalServiceId>
        <initiationDate>2016-01-01</initiationDate>
        <expirationDate>2017-01-01</expirationDate>
        <requiresPersonnel>true</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>{dataSetResult1.getId}</id>
            <name>sre</name>
          </dataSet>
          <dataSet>
            <id>{dataSetResult2.getId}</id>
            <name>sre</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", {districtResult.getId.toString})))
    )
    val districtServiceResult = DistrictService.create(districtService, List[SifRequestParameter]()).asInstanceOf[DistrictServiceResult]

    val result = DistrictService.query(List[SifRequestParameter](SifRequestParameter("id", districtServiceResult.getId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("<districtId>" + districtResult.getId.toString + "</districtId>"))
    assert(resultBody.contains("<externalServiceId>" + externalServiceResult.getId.toString + "</externalServiceId>"))
    assert(resultBody.contains("<initiationDate>2016-01-01</initiationDate>"))
    assert(resultBody.contains("<expirationDate>2017-01-01</expirationDate>"))
    assert(resultBody.contains("<requiresPersonnel>true</requiresPersonnel>"))
  }

  test("query all") {
    val result = DistrictService.query(List[SifRequestParameter](SifRequestParameter("districtId", "321")))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = DistrictService.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

}
