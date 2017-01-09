package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.SrxResourceErrorResult
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ExceptionMessage}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll

import scala.collection.mutable.ArrayBuffer

class DistrictServiceTests extends FunSuite with BeforeAndAfterAll {

  var createdId: Int = 0

  val district: District = District(0, "test", None, None, None)
  var districtResult: DistrictResult = _

  val authorizedEntity: AuthorizedEntity = AuthorizedEntity(0, "test", None)
  var authorizedEntityResult: AuthorizedEntityResult = _

  var externalServiceResult: ExternalServiceResult = _

  val dataSet: DataSet = DataSet(0, "sre", Some("firstName"), None)
  var dataSetResult: DataSetResult = _

  override def beforeAll {
    districtResult = District.create(district, List[SifRequestParameter]()).asInstanceOf[DistrictResult]
    authorizedEntityResult = AuthorizedEntity.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[AuthorizedEntityResult]

    val externalService: ExternalService = ExternalService(0, authorizedEntityResult.getId, Some("test"), Some("test service description"))
    externalServiceResult = ExternalService.create(externalService, List[SifRequestParameter]()).asInstanceOf[ExternalServiceResult]

    dataSetResult = DataSet.create(dataSet, List[SifRequestParameter]()).asInstanceOf[DataSetResult]
  }

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
            <id>{dataSetResult.getId}</id>
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

  test("create duplicate") {
    val districtService = DistrictService(
      <districtService>
        <externalServiceId>{externalServiceResult.getId}</externalServiceId>
        <initiationDate>2016-01-01</initiationDate>
        <expirationDate>2017-01-01</expirationDate>
        <requiresPersonnel>true</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>{dataSetResult.getId}</id>
            <name>sre</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", {districtResult.getId.toString})))
    )
    val result = DistrictService.create(districtService, List[SifRequestParameter]()).asInstanceOf[SrxResourceErrorResult]

    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("update") {
    val districtServiceUpdated = DistrictService(
      <districtService>
        <externalServiceId>{externalServiceResult.getId}</externalServiceId>
        <initiationDate>2015-01-01</initiationDate>
        <expirationDate>2018-01-01</expirationDate>
        <requiresPersonnel>false</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>{dataSetResult.getId}</id>
            <name>sre</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", {districtResult.getId.toString})))
    )

    val result = DistrictService.update(districtServiceUpdated, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))

    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update duplicate") {
    val newDistrict: District = District(0, "new test", None, None, None)
    val newDistrictResult = District.create(newDistrict, List[SifRequestParameter]()).asInstanceOf[DistrictResult]

    val districtService = DistrictService(
      <districtService>
        <externalServiceId>{externalServiceResult.getId}</externalServiceId>
        <initiationDate>2016-01-01</initiationDate>
        <expirationDate>2017-01-01</expirationDate>
        <requiresPersonnel>true</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>{dataSetResult.getId}</id>
            <name>sre</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", {newDistrictResult.getId.toString})))
    )
    val createdDistrictServiceResult = DistrictService.create(districtService, List[SifRequestParameter]()).asInstanceOf[DistrictServiceResult]
    val newCreatedId = createdDistrictServiceResult.getId


    val districtServiceUpdated = DistrictService(
      <districtService>
        <externalServiceId>{externalServiceResult.getId}</externalServiceId>
        <initiationDate>2015-01-01</initiationDate>
        <expirationDate>2018-01-01</expirationDate>
        <requiresPersonnel>false</requiresPersonnel>
        <dataSets>
          <dataSet>
            <id>{dataSetResult.getId}</id>
            <name>sre</name>
          </dataSet>
        </dataSets>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", {districtResult.getId.toString})))
    )

    val result = DistrictService.update(districtServiceUpdated, List[SifRequestParameter](SifRequestParameter("id", newCreatedId.toString)))

    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)

    District.delete(List[SifRequestParameter](SifRequestParameter("id", newDistrictResult.getId.toString)))
    DistrictService.delete(List[SifRequestParameter](SifRequestParameter("id", newCreatedId.toString)))
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
    assert(resultBody.contains("<districtId>" + districtResult.getId.toString + "</districtId>"))
    assert(resultBody.contains("<externalServiceId>" + externalServiceResult.getId.toString + "</externalServiceId>"))
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

  override def afterAll {
    District.delete(List[SifRequestParameter](SifRequestParameter("id", districtResult.getId.toString)))
    AuthorizedEntity.delete(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityResult.getId.toString)))
    ExternalService.delete(List[SifRequestParameter](SifRequestParameter("id", externalServiceResult.getId.toString)))
    DataSet.delete(List[SifRequestParameter](SifRequestParameter("id", dataSetResult.getId.toString)))
  }
}
