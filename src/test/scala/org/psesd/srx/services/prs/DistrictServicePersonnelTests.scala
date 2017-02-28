package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.{BeforeAndAfterAll, FunSuite}

class DistrictServicePersonnelTests extends FunSuite with BeforeAndAfterAll {

  var createdId: Int = 0

  val district = District(0, "district join table test", None, None, None)
  var districtResult: DistrictResult = _

  val authorizedEntity = AuthorizedEntity(0, "authorized entity join table test", None)
  var authorizedEntityResult: AuthorizedEntityResult = _

  var externalServiceResult: ExternalServiceResult = _

  var districtServiceResult: DistrictServiceResult = _

  var personnelResult: PersonnelResult = _

  override def beforeAll: Unit = {
    districtResult = District.create(district, List[SifRequestParameter]()).asInstanceOf[DistrictResult]
    authorizedEntityResult = AuthorizedEntity.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[AuthorizedEntityResult]

    val externalService = ExternalService(0, authorizedEntityResult.getId, Some("join table test"), Some("test service description"))
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

    val personnel = Personnel(0, authorizedEntityResult.getId, Some("jon"), Some("doe"))
    personnelResult = Personnel.create(personnel, List[SifRequestParameter]()).asInstanceOf[PersonnelResult]
  }

  test("constructor") {
    val id = 123
    val districtServiceId = 456
    val personnelId = 789
    val role = "test role"
    val districtServicePersonnel = new DistrictServicePersonnel(id, districtServiceId, personnelId, role)
    assert(districtServicePersonnel.id.equals(id))
    assert(districtServicePersonnel.districtServiceId.equals(districtServiceId))
    assert(districtServicePersonnel.personnelId.equals(personnelId))
    assert(districtServicePersonnel.role.equals(role))
  }

  test("factory") {
    val id = 123
    val districtServiceId = 456
    val personnelId = 789
    val role = "test role"
    val districtServicePersonnel = DistrictServicePersonnel(id, districtServiceId, personnelId, role)
    assert(districtServicePersonnel.id.equals(id))
    assert(districtServicePersonnel.districtServiceId.equals(districtServiceId))
    assert(districtServicePersonnel.personnelId.equals(personnelId))
    assert(districtServicePersonnel.role.equals(role))
  }

  test("node") {
    val id = 123
    val districtServiceId = 456
    val personnelId = 789
    val role = "test role"
    val districtServicePersonnel = DistrictServicePersonnel(
      <districtServicePersonnel>
        <id>{id}</id>
        <districtServiceId>{districtServiceId.toString}</districtServiceId>
        <personnelId>{personnelId.toString}</personnelId>
        <role>{role}</role>
      </districtServicePersonnel>,
      None
    )
    assert(districtServicePersonnel.id.equals(id))
    assert(districtServicePersonnel.districtServiceId.equals(districtServiceId))
    assert(districtServicePersonnel.personnelId.equals(personnelId))
    assert(districtServicePersonnel.role.equals(role))
  }

  test("create") {
    val districtServiceId = districtServiceResult.getId
    val personnelId = personnelResult.getId
    val role = "test role"
    val districtServicePersonnel = DistrictServicePersonnel(
      <districtServicePersonnel>
        <districtServiceId>{districtServiceId.toString}</districtServiceId>
        <personnelId>{personnelId.toString}</personnelId>
        <role>{role}</role>
      </districtServicePersonnel>,
      None
    )
    val result = DistrictServicePersonnel.create(districtServicePersonnel, List[SifRequestParameter]()).asInstanceOf[DistrictServicePersonnelResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update id parameter") {
    val districtServiceId = districtServiceResult.getId
    val personnelId = personnelResult.getId
    val role = "test role UPDATED"
    val districtServicePersonnel = DistrictServicePersonnel(
      <districtServicePersonnel>
        <districtServiceId>{districtServiceId.toString}</districtServiceId>
        <personnelId>{personnelId.toString}</personnelId>
        <role>{role}</role>
      </districtServicePersonnel>,
      None
    )
    val result = DistrictServicePersonnel.update(districtServicePersonnel, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("query bad request") {
    val result = DistrictServicePersonnel.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = DistrictServicePersonnel.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val districtServiceId = districtServiceResult.getId
    val personnelId = personnelResult.getId
    val role = "test role"
    val districtServicePersonnel = DistrictServicePersonnel(
      <districtServicePersonnel>
        <districtServiceId>{districtServiceId.toString}</districtServiceId>
        <personnelId>{personnelId.toString}</personnelId>
        <role>{role}</role>
      </districtServicePersonnel>,
      None
    )
    val districtServicePersonnelResult = DistrictServicePersonnel.create(districtServicePersonnel, List[SifRequestParameter]()).asInstanceOf[DistrictServicePersonnelResult]
    createdId = districtServicePersonnelResult.getId

    val result = DistrictServicePersonnel.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains(districtServiceId.toString))
    assert(resultBody.contains(personnelId.toString))
    assert(resultBody.contains("test role"))
  }

  test("query all") {
    val result = DistrictServicePersonnel.query(List[SifRequestParameter](SifRequestParameter("districtServiceId", "654")))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = DistrictServicePersonnel.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

  override def afterAll: Unit = {
    District.delete(List[SifRequestParameter](SifRequestParameter("id", districtResult.getId.toString)))
    AuthorizedEntity.delete(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityResult.getId.toString)))
    Personnel.delete(List[SifRequestParameter](SifRequestParameter("id", personnelResult.getId.toString)))
  }

}
