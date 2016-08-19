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

  var createdIdXml: Int = 0
  var createdIdJson: Int = 0

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

  test("create authorized entity xml") {
    if (Environment.isLocal) {
      val authorizedEntity = AuthorizedEntity(1, "test", None)
      val sifRequest = new SifRequest(TestValues.sifProvider, PrsResource.AuthorizedEntities.toString)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(authorizedEntity.toXml.toXmlString)
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      createdIdXml = (response.getBodyXml.get \ "creates" \ "create" \ "@id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("create authorized entity json") {
    if (Environment.isLocal) {
      val authorizedEntity = AuthorizedEntity(2, "test", None)
      val sifRequest = new SifRequest(TestValues.sifProvider, PrsResource.AuthorizedEntities.toString)
      sifRequest.accept = Some(SifContentType.Json)
      sifRequest.contentType = Some(SifContentType.Json)
      sifRequest.generatorId = Some(TestValues.generatorId)
      sifRequest.body = Some(authorizedEntity.toXml.toJsonString)
      val response = new SifConsumer().create(sifRequest)
      printlnResponse(response)
      createdIdJson = (response.getBodyXml.get \ "creates" \ "id").text.toInt
      assert(response.statusCode.equals(SifHttpStatusCode.Created))
    }
  }

  test("create authorized entity empty body") {
    if (Environment.isLocal) {
      val sifRequest = new SifRequest(TestValues.sifProvider, PrsResource.AuthorizedEntities.toString)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        SifConsumer().create(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("update authorized entity empty body") {
    if (Environment.isLocal) {
      val sifRequest = new SifRequest(TestValues.sifProvider, PrsResource.AuthorizedEntities.toString)
      sifRequest.body = Some("")
      val thrown = intercept[ArgumentInvalidException] {
        SifConsumer().update(sifRequest)
      }
      assert(thrown.getMessage.equals(ExceptionMessage.IsInvalid.format("request body")))
    }
  }

  test("query authorized entity by id") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString + "/" + createdIdXml.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("query authorized entity all") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      val response = new SifConsumer().query(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete authorized entity xml") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString + "/" + createdIdXml.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      val response = new SifConsumer().delete(sifRequest)
      printlnResponse(response)
      assert(response.statusCode.equals(SifHttpStatusCode.Ok))
    }
  }

  test("delete authorized entity json") {
    if (Environment.isLocal) {
      val resource = PrsResource.AuthorizedEntities.toString + "/" + createdIdJson.toString
      val sifRequest = new SifRequest(TestValues.sifProvider, resource)
      sifRequest.generatorId = Some(TestValues.generatorId)
      val response = new SifConsumer().delete(sifRequest)
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
    for (header <- response.getHeaders) {
      println("%s=%s".format(header._1, header._2))
    }
    println(response.body.getOrElse(""))
  }

}
