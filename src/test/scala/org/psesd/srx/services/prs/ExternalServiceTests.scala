package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class ExternalServiceTests extends FunSuite {

  var createdId: Int = 0

  test("constructor") {
    val id = 123
    val authorizedEntityId = 456
    val name = "test service"
    val description = "test service description"
    val externalService = new ExternalService(id, authorizedEntityId, Some(name), Some(description))
    assert(externalService.id.equals(id))
    assert(externalService.authorizedEntityId.equals(authorizedEntityId))
    assert(externalService.name.get.equals(name))
    assert(externalService.description.get.equals(description))
  }

  test("factory") {
    val id = 123
    val authorizedEntityId = 456
    val name = "test service"
    val description = "test service description"
    val externalService = ExternalService(id, authorizedEntityId, Some(name), Some(description))
    assert(externalService.id.equals(id))
    assert(externalService.authorizedEntityId.equals(authorizedEntityId))
    assert(externalService.name.get.equals(name))
    assert(externalService.description.get.equals(description))
  }

  test("node") {
    val id = 123
    val authorizedEntityId = 456
    val name = "test service"
    val description = "test service description"
    val externalService = ExternalService(
      <externalService>
        <id>{id}</id>
        <authorizedEntityId>{authorizedEntityId}</authorizedEntityId>
        <externalServiceName>{name}</externalServiceName>
        <externalServiceDescription>{description}</externalServiceDescription>
      </externalService>,
      Some(List(SifRequestParameter("authorizedEntityId", authorizedEntityId.toString)))
    )
    assert(externalService.id.equals(id))
    assert(externalService.authorizedEntityId.equals(authorizedEntityId))
    assert(externalService.name.get.equals(name))
    assert(externalService.description.get.equals(description))
  }

  test("create") {
    val externalService = ExternalService(0, 0, Some("test"), Some("test service description"))
    val result = ExternalService.create(externalService, List[SifRequestParameter]()).asInstanceOf[ExternalServiceResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update id parameter") {
    val externalService = ExternalService(0, 0, Some("test UPDATED"), Some("test service description UPDATED"))
    val result = ExternalService.update(externalService, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("query bad request") {
    val result = ExternalService.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = ExternalService.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val result = ExternalService.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("test service description UPDATED"))
  }

  test("query all") {
    val result = ExternalService.query(null)
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = ExternalService.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

}