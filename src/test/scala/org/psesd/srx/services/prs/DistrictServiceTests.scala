package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ExceptionMessage}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class DistrictServiceTests extends FunSuite {

  var createdId: Int = 0

  test("constructor") {
    val id = 123
    val districtId = 456
    val externalServiceId = 789
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(321)
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
    assert(districtService.externalServiceId.equals(externalServiceId))
    assert(districtService.externalServiceName.get.equals(externalServiceName.get))
    assert(districtService.authorizedEntityId.get.equals(authorizedEntityId.get))
    assert(districtService.authorizedEntityName.get.equals(authorizedEntityName.get))
    assert(districtService.initiationDate.equals(initiationDate))
    assert(districtService.expirationDate.equals(expirationDate))
    assert(districtService.requiresPersonnel.equals(requiresPersonnel))
  }

  test("factory") {
    val id = 123
    val districtId = 456
    val externalServiceId = 789
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(321)
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
    val districtId = 456
    val externalServiceId = 789
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(321)
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
    val districtId = 456
    val externalServiceId = 789
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(321)
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
    val districtId = 456
    val externalServiceId = 789
    val externalServiceName = Some("test service")
    val authorizedEntityId = Some(321)
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
        <externalServiceId>456</externalServiceId>
        <initiationDate>2016-01-01</initiationDate>
        <expirationDate>2017-01-01</expirationDate>
        <requiresPersonnel>true</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>123</id>
            <name>data set 123</name>
          </dataSet>
          <dataSet>
            <id>456</id>
            <name>data set 456</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", "123")))
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
        <externalServiceId>654</externalServiceId>
        <initiationDate>2015-01-01</initiationDate>
        <expirationDate>2018-01-01</expirationDate>
        <requiresPersonnel>false</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>789</id>
            <name>data set 789</name>
          </dataSet>
          <dataSet>
            <id>456</id>
            <name>data set 456</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", "321")))
    )
    val result = DistrictService.update(districtService, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
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
    val result = DistrictService.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("<districtId>321</districtId>"))
    assert(resultBody.contains("<externalServiceId>654</externalServiceId>"))
    assert(resultBody.contains("<initiationDate>2015-01-01</initiationDate>"))
    assert(resultBody.contains("<expirationDate>2018-01-01</expirationDate>"))
    assert(resultBody.contains("<requiresPersonnel>false</requiresPersonnel>"))
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
