package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class PrsFilterTests extends FunSuite {

  test("constructor") {
    val filter = new PrsFilter(<node/>)
    assert(filter.toXml.toXmlString.equals("<node/>"))
  }

  test("factory") {
    val filter = PrsFilter(<node/>)
    assert(filter.toXml.toXmlString.equals("<node/>"))
  }

  test("query null parameters") {
    val result = PrsFilter.query(null)
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    val message = result.exceptions.head.getMessage
    assert(message.equals("The parameters collection cannot be null."))
  }

  test("query missing authorizedEntityId") {
    val result = PrsFilter.query(List[SifRequestParameter]())
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    val message = result.exceptions.head.getMessage
    assert(message.equals("The authorizedEntityId parameter is invalid."))
  }

  test("query missing districtStudentId") {
    val result = PrsFilter.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    val message = result.exceptions.head.getMessage
    assert(message.equals("The districtStudentId parameter is invalid."))
  }

  test("query missing externalServiceId") {
    val result = PrsFilter.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("districtStudentId", "9999999999")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    val message = result.exceptions.head.getMessage
    assert(message.equals("The externalServiceId parameter is invalid."))
  }

  test("query missing objectType") {
    val result = PrsFilter.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("districtStudentId", "9999999999"),
      SifRequestParameter("externalServiceId", "2")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    val message = result.exceptions.head.getMessage
    assert(message.equals("The objectType parameter is invalid."))
  }

  test("query missing zoneId") {
    val result = PrsFilter.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("districtStudentId", "9999999999"),
      SifRequestParameter("externalServiceId", "2"),
      SifRequestParameter("objectType", "sre")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    val message = result.exceptions.head.getMessage
    assert(message.equals("The zoneId parameter is invalid."))
  }

  test("query accept header is json") {
    val result = PrsFilter.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("districtStudentId", "1111111111"),
      SifRequestParameter("externalServiceId", "1"),
      SifRequestParameter("objectType", "sre"),
      SifRequestParameter("personnelId", "1"),
      SifRequestParameter("zoneId", "federalway"),
      SifRequestParameter("accept", "json")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    val message = result.exceptions.head.getMessage
    assert(message.equals("SIF header 'accept' contains invalid value 'json'."))
  }

  test("query not found") {
    val result = PrsFilter.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("districtStudentId", "notfound"),
      SifRequestParameter("externalServiceId", "2"),
      SifRequestParameter("objectType", "sre"),
      SifRequestParameter("personnelId", "3"),
      SifRequestParameter("zoneId", "highline")
    ))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
  }

  test("query valid") {
    val result = PrsFilter.query(List[SifRequestParameter](
      SifRequestParameter("authorizedEntityId", "1"),
      SifRequestParameter("districtStudentId", "9999999999"),
      SifRequestParameter("externalServiceId", "2"),
      SifRequestParameter("objectType", "sre"),
      SifRequestParameter("personnelId", "3"),
      SifRequestParameter("zoneId", "highline")
    ))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("<xsl:stylesheet version=\"2.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">"))
  }

}
