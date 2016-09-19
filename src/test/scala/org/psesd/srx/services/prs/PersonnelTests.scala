package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class PersonnelTests extends FunSuite {

  var createdId: Int = 0

  test("constructor") {
    val id = 123
    val authorizedEntityId = 456
    val firstName = "jon"
    val lastName = "snow"
    val personnel = new Personnel(id, authorizedEntityId, Some(firstName), Some(lastName))
    assert(personnel.id.equals(id))
    assert(personnel.authorizedEntityId.equals(authorizedEntityId))
    assert(personnel.firstName.get.equals(firstName))
    assert(personnel.lastName.get.equals(lastName))
  }

  test("factory") {
    val id = 123
    val authorizedEntityId = 456
    val firstName = "jon"
    val lastName = "snow"
    val personnel = Personnel(id, authorizedEntityId, Some(firstName), Some(lastName))
    assert(personnel.id.equals(id))
    assert(personnel.authorizedEntityId.equals(authorizedEntityId))
    assert(personnel.firstName.get.equals(firstName))
    assert(personnel.lastName.get.equals(lastName))
  }

  test("node") {
    val id = 123
    val authorizedEntityId = 456
    val firstName = "jon"
    val lastName = "snow"
    val personnel = Personnel(
      <personnel>
        <id>{id}</id>
        <authorizedEntityId>{authorizedEntityId}</authorizedEntityId>
        <firstName>{firstName}</firstName>
        <lastName>{lastName}</lastName>
      </personnel>,
      Some(List(SifRequestParameter("authorizedEntityId", authorizedEntityId.toString)))
    )
    assert(personnel.id.equals(id))
    assert(personnel.authorizedEntityId.equals(authorizedEntityId))
    assert(personnel.firstName.get.equals(firstName))
    assert(personnel.lastName.get.equals(lastName))
  }

  test("create") {
    val personnel = Personnel(0, 0, Some("jon"), Some("doe"))
    val result = Personnel.create(personnel, List[SifRequestParameter]()).asInstanceOf[PersonnelResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update id parameter") {
    val personnel = Personnel(0, 0, Some("jonathan"), Some("doe"))
    val result = Personnel.update(personnel, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("query bad request") {
    val result = Personnel.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = Personnel.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val result = Personnel.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("jonathan"))
  }

  test("query all") {
    val result = Personnel.query(List[SifRequestParameter](SifRequestParameter("authorizedEntityId", "0")))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = Personnel.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

}
