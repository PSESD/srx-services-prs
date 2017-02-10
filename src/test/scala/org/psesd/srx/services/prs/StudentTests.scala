package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.SrxResourceErrorResult
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class StudentTests extends FunSuite with BeforeAndAfterAll {

  var createdId: Int = 0
  var consentId: Int = _

  val district = District(0, "district", None, None, None)
  var districtResult: DistrictResult = _

  val authorizedEntity = AuthorizedEntity(0, "authorized entity", None)
  var authorizedEntityResult: AuthorizedEntityResult = _

  var externalServiceResult: ExternalServiceResult = _
  var districtServiceResult: DistrictServiceResult = _
  var consentResult: ConsentResult = _

  override def beforeAll: Unit = {
    districtResult = District.create(district, List[SifRequestParameter]()).asInstanceOf[DistrictResult]
    authorizedEntityResult = AuthorizedEntity.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[AuthorizedEntityResult]

    val externalService = ExternalService(0, authorizedEntityResult.getId, Some("external service"), Some("test service description"))
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

    val consent = Consent(
      <consent>
        <consentType>test type</consentType>
        <startDate>2016-01-01</startDate>
        <endDate>2017-01-01</endDate>
      </consent>,
      Some(List(SifRequestParameter("districtServiceId", districtServiceResult.getId.toString)))
    )
    consentResult = Consent.create(consent, List[SifRequestParameter]()).asInstanceOf[ConsentResult]
    consentId = consentResult.getId
  }

  test("constructor") {
    val id = 123
    val districtServiceId = 456
    val districtStudentId = "1234"
    val student = new Student(id, districtServiceId, districtStudentId, None)
    assert(student.id.equals(id))
    assert(student.districtServiceId.equals(districtServiceId))
    assert(student.districtStudentId.equals(districtStudentId))
  }

  test("factory") {
    val id = 123
    val districtServiceId = 456
    val districtStudentId = "1234"
    val student = Student(id, districtServiceId, districtStudentId, None)
    assert(student.id.equals(id))
    assert(student.districtServiceId.equals(districtServiceId))
    assert(student.districtStudentId.equals(districtStudentId))
  }

  test("node") {
    val id = 123
    val districtServiceId = 456
    val districtStudentId = "1234"
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val student = Student(
      <student>
        <id>{id}</id>
        <districtServiceId>{districtServiceId}</districtServiceId>
        <districtStudentId>{districtStudentId}</districtStudentId>
        <consent>
          <consentType>{consentType}</consentType>
          <startDate>{startDate}</startDate>
          <endDate>{endDate}</endDate>
        </consent>
      </student>,
      None
    )
    assert(student.id.equals(id))
    assert(student.districtServiceId.equals(districtServiceId))
    assert(student.districtStudentId.equals(districtStudentId))
    assert(student.consent.get.districtServiceId.equals(districtServiceId))
    assert(student.consent.get.consentType.equals(consentType))
    assert(student.consent.get.startDate.get.equals(startDate))
    assert(student.consent.get.endDate.get.equals(endDate))
  }

  test("create") {
    val districtServiceId = districtServiceResult.getId
    val districtStudentId = "1234"
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val student = Student(
      <student>
        <districtServiceId>{districtServiceId}</districtServiceId>
        <districtStudentId>{districtStudentId}</districtStudentId>
        <consent>
          <consentType>{consentType}</consentType>
          <startDate>{startDate}</startDate>
          <endDate>{endDate}</endDate>
        </consent>
      </student>,
      None
    )
    val result = Student.create(student, List[SifRequestParameter]()).asInstanceOf[StudentResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("create duplicate") {
    val districtServiceId = districtServiceResult.getId
    val districtStudentId = "1234"
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val student = Student(
      <student>
        <districtServiceId>{districtServiceId}</districtServiceId>
        <districtStudentId>{districtStudentId}</districtStudentId>
        <consent>
          <consentType>{consentType}</consentType>
          <startDate>{startDate}</startDate>
          <endDate>{endDate}</endDate>
        </consent>
      </student>,
      None
    )
    val result = Student.create(student, List[SifRequestParameter]()).asInstanceOf[SrxResourceErrorResult]

    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("update") {
    val districtServiceId = districtServiceResult.getId
    val districtStudentId = "6789 UPDATED"
    val consentType = "test type UPDATED"
    val startDate = "2018-01-01"
    val endDate = "2019-01-01"
    val student = Student(
      <student>
        <districtServiceId>{districtServiceId}</districtServiceId>
        <districtStudentId>{districtStudentId}</districtStudentId>
        <consent>
          <consentType>{consentType}</consentType>
          <startDate>{startDate}</startDate>
          <endDate>{endDate}</endDate>
        </consent>
      </student>,
      None
    )
    val result = Student.update(student, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update duplicate") {
    val districtServiceId = districtServiceResult.getId
    val districtStudentId = "12345"
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val student = Student(
      <student>
        <districtServiceId>{districtServiceId}</districtServiceId>
        <districtStudentId>{districtStudentId}</districtStudentId>
        <consent>
          <consentType>{consentType}</consentType>
          <startDate>{startDate}</startDate>
          <endDate>{endDate}</endDate>
        </consent>
      </student>,
      None
    )
    val createdStudentResult = Student.create(student, List[SifRequestParameter]()).asInstanceOf[StudentResult]

    val updatedDistrictStudentId = "6789 UPDATED"
    val updatedStudent = Student(
      <student>
        <districtServiceId>{districtServiceId}</districtServiceId>
        <districtStudentId>{updatedDistrictStudentId}</districtStudentId>
        <consent>
          <consentType>{consentType}</consentType>
          <startDate>{startDate}</startDate>
          <endDate>{endDate}</endDate>
        </consent>
      </student>,
      None
    )
    val result = Student.update(updatedStudent, List[SifRequestParameter](SifRequestParameter("id", createdStudentResult.getId.toString))).asInstanceOf[SrxResourceErrorResult]

    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)

    Student.delete(List[SifRequestParameter](SifRequestParameter("id", createdStudentResult.getId.toString)))
  }

  test("query bad request") {
    val result = Student.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = Student.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val result = Student.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("6789 UPDATED"))
    assert(resultBody.contains("test type UPDATED"))
    assert(resultBody.contains("2018-01-01"))
    assert(resultBody.contains("2019-01-01"))
  }

  test("query by district service") {
    val result = Student.query(List[SifRequestParameter](SifRequestParameter("districtServiceId", "456")))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = Student.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

  override def afterAll: Unit = {
    District.delete(List[SifRequestParameter](SifRequestParameter("id", districtResult.getId.toString)))
    AuthorizedEntity.delete(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityResult.getId.toString)))
    ExternalService.delete(List[SifRequestParameter](SifRequestParameter("id", externalServiceResult.getId.toString)))
    DistrictService.delete(List[SifRequestParameter](SifRequestParameter("id", districtServiceResult.getId.toString)))
    Consent.delete(List[SifRequestParameter](SifRequestParameter("id", consentResult.getId.toString)))
  }
}
