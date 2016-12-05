package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class DistrictTests extends FunSuite {

  var createdId: Int = 0
  var createdId2: Int = 0

  test("constructor") {
    val id = 123
    val name = "test"
    val ncesleaCode = "test code"
    val zoneId = "test zone"
    val contactName = "jon"
    val contactPhone = "555-1212"
    val mainContact = Some(new Contact(456, Some(contactName), None, None, Some(contactPhone), None, None))
    val district = new District(id, name, Some(ncesleaCode), Some(zoneId), mainContact, None)
    assert(district.id.equals(id))
    assert(district.name.equals(name))
    assert(district.ncesleaCode.get.equals(ncesleaCode))
    assert(district.zoneId.get.equals(zoneId))
    assert(district.mainContact.get.name.get.equals(contactName))
  }

  test("factory") {
    val id = 123
    val name = "test"
    val ncesleaCode = "test code"
    val zoneId = "test zone"
    val contactName = "jon"
    val contactPhone = "555-1212"
    val mainContact = Some(new Contact(456, Some(contactName), None, None, Some(contactPhone), None, None))
    val district = District(id, name, Some(ncesleaCode), Some(zoneId), mainContact)
    assert(district.id.equals(id))
    assert(district.name.equals(name))
    assert(district.ncesleaCode.get.equals(ncesleaCode))
    assert(district.zoneId.get.equals(zoneId))
    assert(district.mainContact.get.name.get.equals(contactName))
  }

  test("node") {
    val id = 123
    val name = "test"
    val ncesleaCode = "test code"
    val zoneId = "test zone"
    val contactName = "jon"
    val contactPhone = "555-1212"
    val mainContact = Some(new Contact(456, Some(contactName), None, None, Some(contactPhone), None, None))
    val district = District(
      <district>
        <id>{id}</id>
        <name>{name}</name>
        <ncesleaCode>{ncesleaCode}</ncesleaCode>
        <zoneId>{zoneId}</zoneId>
        <mainContact>
          <name>{contactName}</name>
          <phone>{contactPhone}</phone>
        </mainContact>
      </district>,
      None
    )
    assert(district.id.equals(id))
    assert(district.name.equals(name))
    assert(district.ncesleaCode.get.equals(ncesleaCode))
    assert(district.zoneId.get.equals(zoneId))
    assert(district.mainContact.get.name.get.equals(contactName))
  }

  test("node no contact") {
    val id = 123
    val name = "test"
    val district = District(
      <district>
        <id>{id}</id>
        <name>{name}</name>
      </district>,
      None
    )
    assert(district.id.equals(id))
    assert(district.name.equals(name))
  }

  test("create") {
    val contact = new Contact(0, Some("jon"), Some("director"), Some("jon@doe.com"), Some("555-1212"), Some("123 Spring St"), Some("jon.com"))
    val district = District(0, "test", None, None, Some(contact))
    val result = District.create(district, List[SifRequestParameter]()).asInstanceOf[DistrictResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("create with no contact") {
    val district = District(0, "test", None, None, None)
    val result = District.create(district, List[SifRequestParameter]()).asInstanceOf[DistrictResult]
    createdId2 = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId2.toString)))
  }

  test("update id parameter") {
    val contact = new Contact(0, Some("jon"), Some("director"), Some("jon@doe.com"), Some("666-1234"), Some("123 Spring St"), Some("jon.com"))
    val district = District(0, "test UPDATED 1", Some("test code updated"), Some("test zone UPDATED"), Some(contact))
    val result = District.update(district, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update id parameter no contact") {
    val district = District(0, "test UPDATED 2", Some("test code updated"), Some("test zone UPDATED"), None)
    val result = District.update(district, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("query bad request") {
    val result = District.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = District.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val result = District.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("test UPDATED 2"))
    assert(resultBody.contains("666-1234"))
  }

  test("query all") {
    val result = District.query(null)
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = District.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

  test("delete no contact") {
    val result = District.delete(List[SifRequestParameter](SifRequestParameter("id", createdId2.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

}
