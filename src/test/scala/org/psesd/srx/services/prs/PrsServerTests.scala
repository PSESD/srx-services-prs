package org.psesd.srx.services.prs

import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.apache.http.util.EntityUtils
import org.http4s.dsl._
import org.http4s.{Method, Request}
import org.psesd.srx.shared.core.CoreResource
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ExceptionMessage}
import org.psesd.srx.shared.core.extensions.HttpTypeExtensions._
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif._
import org.scalatest.FunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Success
import scala.xml.Node

class PrsServerTests extends FunSuite {

  private final val ServerDuration = 8000
  private lazy val tempServer = Future {
    delayedInterrupt(ServerDuration)
    intercept[InterruptedException] {
      startServer()
    }
  }
  private val pendingInterrupts = new ThreadLocal[List[Thread]] {
    override def initialValue = Nil
  }

  test("service") {
    assert(PrsServer.srxService.service.name.equals(Build.name))
    assert(PrsServer.srxService.service.version.equals(Build.version + "." + Build.buildNumber))
    assert(PrsServer.srxService.buildComponents(0).name.equals("java"))
    assert(PrsServer.srxService.buildComponents(0).version.equals(Build.javaVersion))
    assert(PrsServer.srxService.buildComponents(1).name.equals("scala"))
    assert(PrsServer.srxService.buildComponents(1).version.equals(Build.scalaVersion))
    assert(PrsServer.srxService.buildComponents(2).name.equals("sbt"))
    assert(PrsServer.srxService.buildComponents(2).version.equals(Build.sbtVersion))
  }

  test("ping (localhost)") {
    if (Environment.isLocal) {
      val expected = "true"
      var actual = ""
      tempServer onComplete {
        case Success(x) =>
          assert(actual.equals(expected))
        case _ =>
      }

      // wait for server to init
      Thread.sleep(2000)

      // ping server and collect response
      val httpclient: CloseableHttpClient = HttpClients.custom().disableCookieManagement().build()
      val httpGet = new HttpGet("http://localhost:%s/ping".format(Environment.getPropertyOrElse("SERVER_PORT", "80")))
      val response = httpclient.execute(httpGet)
      actual = EntityUtils.toString(response.getEntity)
    }
  }

  test("root") {
    val getRoot = Request(Method.GET, uri("/"))
    val task = PrsServer.service.run(getRoot)
    val response = task.run
    assert(response.status.code.equals(SifHttpStatusCode.Ok))
  }

  test("ping") {
    if (Environment.isLocal) {
      val getPing = Request(Method.GET, uri("/ping"))
      val task = PrsServer.service.run(getPing)
      val response = task.run
      val body = response.body.value
      assert(response.status.code.equals(SifHttpStatusCode.Ok))
      assert(body.equals(true.toString))
    }
  }

  test("info (localhost)") {
    if (Environment.isLocal) {
      val sifRequest = new SifRequest(TestValues.sifProvider, CoreResource.Info.toString)
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val responseBody = response.body.getOrElse("")
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(response.contentType.get.equals(SifContentType.Xml))
      assert(responseBody.contains("<service>"))
    }
  }


  /* AUTHORIZED ENTITY ROUTES */

  var authorizedEntityIdXml: Int = 0
  var authorizedEntityIdJson: Int = 0

  test("create authorized entity xml") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString
      val authorizedEntity = AuthorizedEntity(1, "test", None)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(authorizedEntity.toXml.toXmlString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      authorizedEntityIdXml = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("create authorized entity json") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString
      val authorizedEntity = AuthorizedEntity(2, "test", None)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.accept = Some(SifContentType.Json)
      sifRequest.contentType = Some(SifContentType.Json)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(authorizedEntity.toXml.toJsonString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      authorizedEntityIdJson = (response.getBodyXml.get \ "creates" \ "id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("create authorized entity empty body") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        println("CREATE RESOURCE: %s".format(resource))
        SifConsumer().create(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("create authorized entity invalid root element") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(<notAnEntity/>.toString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.BadRequest))
      assert((response.getBodyXml.get \ "description").text == "The root element 'notAnEntity' is invalid.")
    }
  }

  test("create authorized entity missing name") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(<authorizedEntity><invalidField/></authorizedEntity>.toString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.BadRequest))
      assert((response.getBodyXml.get \ "description").text == "The authorizedEntity.name cannot be null, empty, or whitespace.")
    }
  }

  test("update authorized entity empty body") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        println("UPDATE RESOURCE: %s".format(resource))
        SifConsumer().update(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("query authorized entity by id") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString + "/" + authorizedEntityIdXml.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val authorizedEntity = AuthorizedEntity((response.getBodyXml.get \ "authorizedEntity").head , None)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(authorizedEntity.id.equals(authorizedEntityIdXml))
      assert(authorizedEntity.name.equals("test"))
    }
  }

  test("update authorized entity") {
    if (Environment.isLocal) {
      val resource = "%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityIdXml.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(AuthorizedEntity(authorizedEntityIdXml, "test UPDATED", None).toXml.toXmlString)
      println("UPDATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query authorized entity all") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete authorized entity xml") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString + "/" + authorizedEntityIdXml.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete authorized entity json") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString + "/" + authorizedEntityIdJson.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  /* PERSONNEL ROUTES */

  var personnelId: Int = 0

  test("create personnel") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.Personnel.toString)
      val personnel = Personnel(0, authorizedEntityId, Some("jon"), Some("doe"))
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(personnel.toXml.toXmlString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      personnelId = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("query personnel by id") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.Personnel.toString, personnelId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val personnel = Personnel((response.getBodyXml.get \ "personnel").head , Some(List(SifRequestParameter("authorizedEntityId", authorizedEntityId.toString))))
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(personnel.id.equals(personnelId))
      assert(personnel.authorizedEntityId.equals(authorizedEntityId))
      assert(personnel.firstName.get.equals("jon"))
      assert(personnel.lastName.get.equals("doe"))
    }
  }

  test("update personnel") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.Personnel.toString, personnelId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(Personnel(personnelId, authorizedEntityId, Some("jon UPDATED"), Some("jon UPDATED")).toXml.toXmlString)
      println("UPDATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query personnel all") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.Personnel.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete personnel") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.Personnel.toString, personnelId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  /* EXTERNAL SERVICE ROUTES */

  var externalServiceId: Int = 0

  test("create external service") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.ExternalServices.toString)
      val externalService = ExternalService(0, authorizedEntityId, Some("test service"), Some("test service description"))
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(externalService.toXml.toXmlString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      externalServiceId = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("query external service by id") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.ExternalServices.toString, externalServiceId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val externalService = ExternalService((response.getBodyXml.get \ "externalService").head , Some(List(SifRequestParameter("authorizedEntityId", authorizedEntityId.toString))))
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(externalService.id.equals(externalServiceId))
      assert(externalService.authorizedEntityId.equals(authorizedEntityId))
      assert(externalService.name.get.equals("test service"))
      assert(externalService.description.get.equals("test service description"))
    }
  }

  test("update external service") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.ExternalServices.toString, externalServiceId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(ExternalService(externalServiceId, authorizedEntityId, Some("test service UPDATED"), Some("test service description UPDATED")).toXml.toXmlString)
      println("UPDATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query external service all") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.ExternalServices.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete external service") {
    if (Environment.isLocal) {
      val authorizedEntityId: Int = 0
      val resource = "%s/%s/%s/%s".format(PrsResource.AuthorizedEntities.toString, authorizedEntityId.toString, PrsResource.ExternalServices.toString, externalServiceId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  /* DISTRICT ROUTES */

  var districtId: Int = 0

  test("create district") {
    if (Environment.isLocal) {
      val resource = PrsResource.Districts.toString
      val district = District(0, "test", None, None, None)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(district.toXml.toXmlString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      districtId = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("create district empty body") {
    if (Environment.isLocal) {
      val resource = PrsResource.Districts.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        println("CREATE RESOURCE: %s".format(resource))
        SifConsumer().create(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("create district invalid root element") {
    if (Environment.isLocal) {
      val resource = PrsResource.Districts.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(<notAnEntity/>.toString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.BadRequest))
      assert((response.getBodyXml.get \ "description").text == "The root element 'notAnEntity' is invalid.")
    }
  }

  test("create district missing name") {
    if (Environment.isLocal) {
      val resource = PrsResource.Districts.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(<district><invalidField/></district>.toString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.BadRequest))
      assert((response.getBodyXml.get \ "description").text == "The district.districtName cannot be null, empty, or whitespace.")
    }
  }

  test("update district empty body") {
    if (Environment.isLocal) {
      val resource = PrsResource.Districts.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        println("UPDATE RESOURCE: %s".format(resource))
        SifConsumer().update(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("query district by id") {
    if (Environment.isLocal) {
      val resource = PrsResource.Districts.toString + "/" + districtId.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val district = District((response.getBodyXml.get \ "district").head , None)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(district.id.equals(districtId))
      assert(district.name.equals("test"))
    }
  }

  test("update district") {
    if (Environment.isLocal) {
      val resource = "%s/%s".format(PrsResource.Districts.toString, districtId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(District(districtId, "test UPDATED", None, None, None).toXml.toXmlString)
      println("UPDATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query district all") {
    if (Environment.isLocal) {
      val resource = PrsResource.Districts.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete district") {
    if (Environment.isLocal) {
      val resource = PrsResource.Districts.toString + "/" + districtId.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  /* DISTRICT SERVICE ROUTES */

  var districtServiceId: Int = 0

  test("create district service") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString)
      val districtService = DistrictService(
        <districtService>
          <externalServiceId>456</externalServiceId>
          <initiationDate>2016-01-01</initiationDate>
          <expirationDate>2017-01-01</expirationDate>
          <requiresPersonnel>true</requiresPersonnel>
        </districtService>,
        Some(List[SifRequestParameter](SifRequestParameter("districtId", districtId.toString)))
      )
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(districtService.toXml.toXmlString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      districtServiceId = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("query district service by id") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val districtService = DistrictService((response.getBodyXml.get \ "districtService").head , Some(List(SifRequestParameter("districtId", districtId.toString))))
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(districtService.id.equals(districtServiceId))
      assert(districtService.districtId.equals(districtId))
      assert(districtService.initiationDate.equals("2016-01-01"))
      assert(districtService.expirationDate.equals("2017-01-01"))
      assert(districtService.requiresPersonnel)
    }
  }

  test("update district service") {
    if (Environment.isLocal) {
      val districtService = DistrictService(
        <districtService>
          <externalServiceId>654</externalServiceId>
          <initiationDate>2015-01-01</initiationDate>
          <expirationDate>2018-01-01</expirationDate>
          <requiresPersonnel>false</requiresPersonnel>
        </districtService>,
        Some(List[SifRequestParameter](SifRequestParameter("districtId", districtId.toString)))
      )
      val resource = "%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(districtService.toXml.toXmlString)
      println("UPDATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query district service all") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  /* DISTRICT SERVICE PERSONNEL ROUTES */

  var districtServicePersonnelId: Int = 0

  test("create district service personnel") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Personnel.toString)
      val districtServicePersonnel = DistrictServicePersonnel(
        <districtServicePersonnel>
          <districtServiceId>{districtServiceId.toString}</districtServiceId>
          <personnelId>{personnelId.toString}</personnelId>
          <role>individual provider</role>
        </districtServicePersonnel>,
        None
      )
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(districtServicePersonnel.toXml.toXmlString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      districtServicePersonnelId = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("query district service personnel by id") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Personnel.toString, districtServicePersonnelId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val districtServicePersonnel = DistrictServicePersonnel((response.getBodyXml.get \ "districtServicePersonnel").head , None)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(districtServicePersonnel.id.equals(districtServicePersonnelId))
      assert(districtServicePersonnel.districtServiceId.equals(districtServiceId))
      assert(districtServicePersonnel.personnelId.equals(personnelId))
      assert(districtServicePersonnel.role.equals("individual provider"))
    }
  }

  test("update district service personnel") {
    if (Environment.isLocal) {
      val districtServicePersonnel = DistrictServicePersonnel(
        <districtServicePersonnel>
          <districtServiceId>{districtServiceId.toString}</districtServiceId>
          <personnelId>{personnelId.toString}</personnelId>
          <role>individual provider UPDATED</role>
        </districtServicePersonnel>,
        None
      )
      val resource = "%s/%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Personnel.toString, districtServicePersonnelId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(districtServicePersonnel.toXml.toXmlString)
      println("UPDATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query district service personnel all") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Personnel.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete district service personnel") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Personnel.toString, districtServicePersonnelId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  /* DISTRICT SERVICE STUDENT ROUTES */

  var districtServiceStudentId: Int = 0

  test("create district service student") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Students.toString)
      val student = Student(
        <student>
          <districtServiceId>{districtServiceId}</districtServiceId>
          <districtStudentId>12345</districtStudentId>
          <consent>
            <consentType>test type</consentType>
            <startDate>2016-01-01</startDate>
            <endDate>2017-01-01</endDate>
          </consent>
        </student>,
        None
      )
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(student.toXml.toXmlString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      districtServiceStudentId = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("query district service student by id") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Students.toString, districtServiceStudentId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val student = Student((response.getBodyXml.get \ "student").head , None)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(student.id.equals(districtServiceStudentId))
      assert(student.districtServiceId.equals(districtServiceId))
      assert(student.districtStudentId.equals("12345"))
      assert(student.consent.get.consentType.equals("test type"))
      assert(student.consent.get.startDate.get.equals("2016-01-01"))
      assert(student.consent.get.endDate.get.equals("2017-01-01"))
    }
  }

  test("update district service student") {
    if (Environment.isLocal) {
      val student = Student(
        <student>
          <districtServiceId>{districtServiceId}</districtServiceId>
          <districtStudentId>54321</districtStudentId>
          <consent>
            <consentType>test type UPDATED</consentType>
            <startDate>2018-01-01</startDate>
            <endDate>2019-01-01</endDate>
          </consent>
        </student>,
        None
      )
      val resource = "%s/%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Students.toString, districtServiceStudentId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(student.toXml.toXmlString)
      println("UPDATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query district service student all") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Students.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query district student all") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.Students.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete district service student") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId, PrsResource.Students.toString, districtServiceStudentId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete district service") {
    if (Environment.isLocal) {
      val resource = "%s/%s/%s/%s".format(PrsResource.Districts.toString, districtId.toString, PrsResource.ExternalServices.toString, districtServiceId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  /* DATA SET ROUTES */

  var dataSetId: Int = 0

  test("create dataSet") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString
      val dataSet = DataSet(
        <dataSet>
          <name>sre</name>
          <description>firstName</description>
          <dataObjects>
            <dataObject>
              <filterType>test filter</filterType>
              <sifObjectName>test name</sifObjectName>
              <includeStatement>test include</includeStatement>
            </dataObject>
          </dataObjects>
        </dataSet>,
        None
      )
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(dataSet.toXml.toXmlString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      dataSetId = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("create dataSet empty body") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        println("CREATE RESOURCE: %s".format(resource))
        SifConsumer().create(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("create dataSet invalid root element") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(<notAnEntity/>.toString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.BadRequest))
      assert((response.getBodyXml.get \ "description").text == "The root element 'notAnEntity' is invalid.")
    }
  }

  test("update dataSet empty body") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        println("UPDATE RESOURCE: %s".format(resource))
        SifConsumer().update(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("query dataSet by id") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val dataSet = DataSet((response.getBodyXml.get \ "dataSet").head , None)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(dataSet.id.equals(dataSetId))
      assert(dataSet.name.get.equals("sre"))
    }
  }

  test("update dataSet") {
    if (Environment.isLocal) {
      val resource = "%s/%s".format(PrsResource.DataSets.toString, dataSetId.toString)
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(DataSet(dataSetId, Some("sre UPDATED"), Some("firstName UPDATED"), None).toXml.toXmlString)
      println("UPDATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query dataSet all") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  /* DATA OBJECT ROUTES */

  var dataObjectId: Int = 0

  test("create dataObject") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString + "/" + PrsResource.DataObjects.toString
      val dataObject = DataObject(
        <dataObject>
          <filterType>test filter</filterType>
          <sifObjectName>test name</sifObjectName>
          <includeStatement>test include</includeStatement>
        </dataObject>,
        Some(List[SifRequestParameter](SifRequestParameter("dataSetId", dataSetId.toString)))
      )
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(dataObject.toXml.toXmlString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      dataObjectId = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("create dataObject empty body") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString + "/" + PrsResource.DataObjects.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        println("CREATE RESOURCE: %s".format(resource))
        SifConsumer().create(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("create dataObject invalid root element") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString + "/" + PrsResource.DataObjects.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(<notAnEntity/>.toString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.BadRequest))
      assert((response.getBodyXml.get \ "description").text == "The root element 'notAnEntity' is invalid.")
    }
  }

  test("update dataObject empty body") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString + "/" + PrsResource.DataObjects.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        println("UPDATE RESOURCE: %s".format(resource))
        SifConsumer().update(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("query dataObject by id") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString + "/" + PrsResource.DataObjects.toString + "/" + dataObjectId.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      val dataObject = DataObject((response.getBodyXml.get \ "dataObject").head , None)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
      assert(dataObject.id.equals(dataObjectId))
      assert(dataObject.name.equals("test name"))
      assert(dataObject.filterType.equals("test filter"))
      assert(dataObject.includeStatement.equals("test include"))
    }
  }

  test("update dataObject") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString + "/" + PrsResource.DataObjects.toString + "/" + dataObjectId.toString
      val dataObject = DataObject(
        <dataObject>
          <filterType>test filter UPDATED</filterType>
          <sifObjectName>test name UPDATED</sifObjectName>
          <includeStatement>test include UPDATED</includeStatement>
        </dataObject>,
        Some(List[SifRequestParameter](SifRequestParameter("dataSetId", dataSetId.toString)))
      )
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(dataObject.toXml.toXmlString)
      println("UPDATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query dataObject all") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString + "/" + PrsResource.DataObjects.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.accept = Some(SifContentType.Json)
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete dataObject") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString + "/" + PrsResource.DataObjects.toString + "/" + dataObjectId.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete dataSet") {
    if (Environment.isLocal) {
      val resource = PrsResource.DataSets.toString + "/" + dataSetId.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("DELETE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  /* PRS FILTER ROUTES */

  test("create filters") {
    if (Environment.isLocal) {
      val resource = PrsResource.Filters.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("highline"), SifContext("default"))
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(<filter/>.toString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.MethodNotAllowed))
    }
  }

  test("delete filters") {
    if (Environment.isLocal) {
      val resource = PrsResource.Filters.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("highline"), SifContext("default"))
      sifRequest.generatorId = Some(TestValues.generatorId)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.MethodNotAllowed))
    }
  }

  test("update filters") {
    if (Environment.isLocal) {
      val resource = PrsResource.Filters.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("highline"), SifContext("default"))
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(<filter/>.toString)
      println("CREATE RESOURCE: %s".format(resource))
      val response = new SifConsumer().update(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.MethodNotAllowed))
    }
  }

  test("query filters") {
    if (Environment.isLocal) {
      val resource = PrsResource.Filters.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource, SifZone("highline"), SifContext("default"))
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.addHeader("authorizedEntityId", "1")
      sifRequest.addHeader("districtStudentId", "9999999999")
      sifRequest.addHeader("externalServiceId", "2")
      sifRequest.addHeader("objectType", "sre")
      sifRequest.addHeader("personnelId", "3")
      println("QUERY RESOURCE: %s".format(resource))
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }


  private def delayedInterrupt(delay: Long) {
    delayedInterrupt(Thread.currentThread, delay)
  }

  private def delayedInterrupt(target: Thread, delay: Long) {
    val t = new Thread {
      override def run() {
        Thread.sleep(delay)
        target.interrupt()
      }
    }
    pendingInterrupts.set(t :: pendingInterrupts.get)
    t.start()
  }

  private def startServer(): Unit = {
    if (Environment.isLocal) {
      PrsServer.main(Array[String]())
    }
  }

  private def printlnResponse(response: SifResponse): Unit = {
    println("STATUS CODE: " + response.statusCode.toString)
    for (header <- response.getHeaders) {
      println("%s=%s".format(header._1, header._2))
    }
    println(response.body.getOrElse(""))
  }

}
