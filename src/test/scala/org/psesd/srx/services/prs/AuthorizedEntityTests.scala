package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.SrxResourceErrorResult
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter, SifResponse}
import org.scalatest.FunSuite

class AuthorizedEntityTests extends FunSuite {

  var createdId: Int = 0
  var createdId2: Int = 0

  test("constructor") {
    val id = 123
    val name = "test"
    val contactName = "jon"
    val contactPhone = "555-1212"
    val mainContact = Some(new Contact(456, Some(contactName), None, None, Some(contactPhone), None, None))
    val authorizedEntity = new AuthorizedEntity(id, name, mainContact, None)
    assert(authorizedEntity.id.equals(id))
    assert(authorizedEntity.name.equals(name))
    assert(authorizedEntity.mainContact.get.name.get.equals(contactName))
  }

  test("factory") {
    val id = 123
    val name = "test"
    val contactName = "jon"
    val contactPhone = "555-1212"
    val mainContact = Some(new Contact(456, Some(contactName), None, None, Some(contactPhone), None, None))
    val authorizedEntity = AuthorizedEntity(id, name, mainContact)
    assert(authorizedEntity.id.equals(id))
    assert(authorizedEntity.name.equals(name))
    assert(authorizedEntity.mainContact.get.name.get.equals(contactName))
  }

  test("node") {
    val id = 123
    val name = "test"
    val contactName = "jon"
    val contactPhone = "555-1212"
    val mainContact = Some(new Contact(456, Some(contactName), None, None, Some(contactPhone), None, None))
    val authorizedEntity = AuthorizedEntity(
      <authorizedEntity>
        <id>{id}</id>
        <name>{name}</name>
        <mainContact>
          <name>{contactName}</name>
          <phone>{contactPhone}</phone>
        </mainContact>
      </authorizedEntity>,
      None
    )
    assert(authorizedEntity.id.equals(id))
    assert(authorizedEntity.name.equals(name))
    assert(authorizedEntity.mainContact.get.name.get.equals(contactName))
  }

  test("node no contact") {
    val id = 123
    val name = "test"
    val authorizedEntity = AuthorizedEntity(
      <authorizedEntity>
        <id>{id}</id>
        <name>{name}</name>
      </authorizedEntity>,
      None
    )
    assert(authorizedEntity.id.equals(id))
    assert(authorizedEntity.name.equals(name))
  }

  test("create") {
    val contact = new Contact(0, Some("jon"), Some("director"), Some("jon@doe.com"), Some("555-1212"), Some("123 Spring St"), Some("jon.com"))
    val authorizedEntity = AuthorizedEntity(0, "authorized entity test", Some(contact))
    val result = AuthorizedEntity.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[AuthorizedEntityResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("create with no contact") {
    val authorizedEntity = AuthorizedEntity(0, "test no contact", None)
    val result = AuthorizedEntity.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[AuthorizedEntityResult]
    createdId2 = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId2.toString)))
  }

   test("create duplicate") {
    val authorizedEntity = AuthorizedEntity(0, "authorized entity test", None)
    val result = AuthorizedEntity.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[SrxResourceErrorResult]
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("update id parameter") {
    val contact = new Contact(0, Some("jonny"), Some("director"), Some("jon@doe.com"), Some("666-1234"), Some("123 Spring St"), Some("jon.com"))
    val authorizedEntity = AuthorizedEntity(0, "test UPDATED 1", Some(contact))
    val result = AuthorizedEntity.update(authorizedEntity, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update id parameter no contact") {
    val authorizedEntity = AuthorizedEntity(0, "test UPDATED 2", None)
    val result = AuthorizedEntity.update(authorizedEntity, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update duplicate") {
    val authorizedEntity = AuthorizedEntity(0, "test UPDATED 1", None)
    val result = AuthorizedEntity.update(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[SrxResourceErrorResult]
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query bad request") {
    val result = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val result = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("test UPDATED 2"))
    assert(resultBody.contains("666-1234"))
  }

  test("query all") {
    val result = AuthorizedEntity.query(null)
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = AuthorizedEntity.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

  test("delete no contact") {
    val result = AuthorizedEntity.delete(List[SifRequestParameter](SifRequestParameter("id", createdId2.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }
}
