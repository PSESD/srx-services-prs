package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class ConsentTests extends FunSuite with BeforeAndAfterAll {

  var createdId: Int = 0

  val district = District(0, "district consent test", None, None, None)
  var districtResult: DistrictResult = _

  val authorizedEntity = AuthorizedEntity(0, "authorized entity consent test", None)
  var authorizedEntityResult: AuthorizedEntityResult = _

  var externalServiceResult: ExternalServiceResult = _

  var districtServiceResult: DistrictServiceResult = _

  override def beforeAll: Unit = {
    districtResult = District.create(district, List[SifRequestParameter]()).asInstanceOf[DistrictResult]
    authorizedEntityResult = AuthorizedEntity.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[AuthorizedEntityResult]

    val externalService = ExternalService(0, authorizedEntityResult.getId, Some("external service consent test"), Some("test service description"))
    externalServiceResult = ExternalService.create(externalService, List[SifRequestParameter]()).asInstanceOf[ExternalServiceResult]

    val districtService = DistrictService(
      <districtService>
        <externalServiceId>{externalServiceResult.getId}</externalServiceId>
        <initiationDate>2016-01-01</initiationDate>
        <expirationDate>2017-01-01</expirationDate>
        <requiresPersonnel>true</requiresPersonnel>
      </districtService>,
      Some(List[SifRequestParameter](SifRequestParameter("districtId", {districtResult.getId.toString})))
    )
    districtServiceResult = DistrictService.create(districtService, List[SifRequestParameter]()).asInstanceOf[DistrictServiceResult]
  }

  test("constructor") {
    val id = 123
    val districtServiceId = districtServiceResult.getId
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val consent = new Consent(id, districtServiceId, consentType, Some(startDate), Some(endDate))
    assert(consent.id.equals(id))
    assert(consent.districtServiceId.equals(districtServiceId))
    assert(consent.consentType.equals(consentType))
    assert(consent.startDate.get.equals(startDate))
    assert(consent.endDate.get.equals(endDate))
  }

  test("factory") {
    val id = 123
    val districtServiceId = districtServiceResult.getId
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val consent = Consent(id, districtServiceId, consentType, Some(startDate), Some(endDate))
    assert(consent.id.equals(id))
    assert(consent.districtServiceId.equals(districtServiceId))
    assert(consent.consentType.equals(consentType))
    assert(consent.startDate.get.equals(startDate))
    assert(consent.endDate.get.equals(endDate))
  }

  test("node") {
    val id = 123
    val districtServiceId = districtServiceResult.getId
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val consent = Consent(
      <consent>
        <id>{id}</id>
        <consentType>{consentType}</consentType>
        <startDate>{startDate}</startDate>
        <endDate>{endDate}</endDate>
      </consent>,
      Some(List(SifRequestParameter("districtServiceId", districtServiceId.toString)))
    )
    assert(consent.id.equals(id))
    assert(consent.districtServiceId.equals(districtServiceId))
    assert(consent.consentType.equals(consentType))
    assert(consent.startDate.get.equals(startDate))
    assert(consent.endDate.get.equals(endDate))
  }

  test("create") {
    val districtServiceId = districtServiceResult.getId
    val consent = Consent(
      <consent>
        <consentType>test type</consentType>
        <startDate>2016-01-01</startDate>
        <endDate>2017-01-01</endDate>
      </consent>,
      Some(List(SifRequestParameter("districtServiceId", districtServiceId.toString)))
    )
    val result = Consent.create(consent, List[SifRequestParameter]()).asInstanceOf[ConsentResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update id parameter") {
    val districtServiceId = districtServiceResult.getId
    val consent = Consent(
      <consent>
        <consentType>test type UPDATED</consentType>
        <startDate>2018-01-01</startDate>
        <endDate>2019-01-01</endDate>
      </consent>,
      Some(List(SifRequestParameter("districtServiceId", districtServiceId.toString)))
    )
    val result = Consent.update(consent, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("query bad request") {
    val result = Consent.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = Consent.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val result = Consent.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("test type UPDATED"))
    assert(resultBody.contains("2018-01-01"))
    assert(resultBody.contains("2019-01-01"))
  }

  test("query all") {
    val result = Consent.query(null)
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = Consent.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

  override def afterAll: Unit = {
    District.delete(List[SifRequestParameter](SifRequestParameter("id", districtResult.getId.toString)))
    AuthorizedEntity.delete(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityResult.getId.toString)))
  }

}
