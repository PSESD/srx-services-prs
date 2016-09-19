package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class DistrictServicePersonnelTests extends FunSuite {

  var createdId: Int = 0

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
    val districtServiceId = 456
    val personnelId = 789
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
    val districtServiceId = 654
    val personnelId = 987
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
    val result = DistrictServicePersonnel.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("654"))
    assert(resultBody.contains("987"))
    assert(resultBody.contains("test role UPDATED"))
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

}
