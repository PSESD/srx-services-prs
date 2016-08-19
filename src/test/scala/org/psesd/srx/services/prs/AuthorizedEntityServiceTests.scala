package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif._
import org.scalatest.FunSuite

class AuthorizedEntityServiceTests extends FunSuite {

  var createdId: Int = 0

  test("create") {
    val authorizedEntity = AuthorizedEntity(0, "test", None)
    val result = AuthorizedEntityService.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[AuthorizedEntityResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update id parameter") {
    val authorizedEntity = AuthorizedEntity(0, "test UPDATED", None)
    val result = AuthorizedEntityService.update(authorizedEntity, List[SifRequestParameter](SifRequestParameter("id", createdId.toString))).asInstanceOf[AuthorizedEntityResult]
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("query bad request") {
    val result = AuthorizedEntityService.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = AuthorizedEntityService.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val result = AuthorizedEntityService.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("test UPDATED"))
  }

  test("query all") {
    val result = AuthorizedEntityService.query(null)
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = AuthorizedEntityService.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

}
