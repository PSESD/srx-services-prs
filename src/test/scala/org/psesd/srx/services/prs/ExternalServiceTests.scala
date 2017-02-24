package org.psesd.srx.services.prs

import com.mongodb.casbah.MongoConnection
import com.mongodb.casbah.commons.MongoDBObject
import org.psesd.srx.shared.core.SrxResourceErrorResult
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfterAll

class ExternalServiceTests extends FunSuite with BeforeAndAfterAll {

  var createdId: Int = 0

  val authorizedEntity: AuthorizedEntity = AuthorizedEntity(0, "external service test", None)
  var authorizedEntityResult: AuthorizedEntityResult = _

  override def beforeAll: Unit = {
    authorizedEntityResult = AuthorizedEntity.create(authorizedEntity, List[SifRequestParameter]()).asInstanceOf[AuthorizedEntityResult]
  }

  test("constructor") {
    val id = 123
    val name = "test service"
    val description = "test service description"
    val externalService = new ExternalService(id, authorizedEntityResult.getId, Some(name), Some(description))
    assert(externalService.id.equals(id))
    assert(externalService.authorizedEntityId.equals(authorizedEntityResult.getId))
    assert(externalService.name.get.equals(name))
    assert(externalService.description.get.equals(description))
  }

  test("factory") {
    val id = 123
    val name = "test service"
    val description = "test service description"
    val externalService = ExternalService(id, authorizedEntityResult.getId, Some(name), Some(description))
    assert(externalService.id.equals(id))
    assert(externalService.authorizedEntityId.equals(authorizedEntityResult.getId))
    assert(externalService.name.get.equals(name))
    assert(externalService.description.get.equals(description))
  }

  test("node") {
    val id = 123
    val name = "test service"
    val description = "test service description"
    val externalService = ExternalService(
      <externalService>
        <id>{id}</id>
        <authorizedEntityId>{authorizedEntityResult.getId}</authorizedEntityId>
        <externalServiceName>{name}</externalServiceName>
        <externalServiceDescription>{description}</externalServiceDescription>
      </externalService>,
      Some(List(SifRequestParameter("authorizedEntityId", authorizedEntityResult.getId.toString)))
    )
    assert(externalService.id.equals(id))
    assert(externalService.authorizedEntityId.equals(authorizedEntityResult.getId))
    assert(externalService.name.get.equals(name))
    assert(externalService.description.get.equals(description))
  }

  test("create") {
    val externalService = ExternalService(0, authorizedEntityResult.getId, Some("test"), Some("test service description"))
    val result = ExternalService.create(externalService, List[SifRequestParameter]()).asInstanceOf[ExternalServiceResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("create duplicate") {
    val externalService = ExternalService(0, authorizedEntityResult.getId, Some("test"), Some("test service description"))
    val result = ExternalService.create(externalService, List[SifRequestParameter]()).asInstanceOf[SrxResourceErrorResult]
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("update id parameter") {
    val externalService = ExternalService(0, authorizedEntityResult.getId, Some("test UPDATED"), Some("test service description UPDATED"))
    val result = ExternalService.update(externalService, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update duplicate") {
    val newExternalService = ExternalService(0, authorizedEntityResult.getId, Some("new test"), Some("test service description"))
    val newExternalServiceResult = ExternalService.create(newExternalService, List[SifRequestParameter]()).asInstanceOf[ExternalServiceResult]

    val externalService = ExternalService(0, authorizedEntityResult.getId, Some("test UPDATED"), Some("test service description"))
    val result = ExternalService.update(externalService, List[SifRequestParameter]()).asInstanceOf[SrxResourceErrorResult]

    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)

    ExternalService.delete(List[SifRequestParameter](SifRequestParameter("id", newExternalServiceResult.getId.toString)))
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

  override def afterAll: Unit = {
    AuthorizedEntity.delete(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityResult.getId.toString)))

//    val mongoConn = MongoConnection(PrsServer.mongoUri)
//    val mongoDB = mongoConn.getDB(mongoConn.getDatabaseNames().head)
//    val organizationsTable = mongoDB("organizations")
//    val query = MongoDBObject("name" -> "external service test")
//    organizationsTable.findAndRemove(query)
//    mongoConn.close()
  }
}
