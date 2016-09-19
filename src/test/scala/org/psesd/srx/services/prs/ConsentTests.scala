package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestParameter}
import org.scalatest.FunSuite

class ConsentTests extends FunSuite {

  var createdId: Int = 0

  test("constructor") {
    val id = 123
    val districtServiceId = 456
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val consent = new Consent(id, districtServiceId, consentType, Some(startDate), Some(endDate))
    assert(consent.id.equals(id))
    assert(consent.districtServiceId.equals(districtServiceId))
    assert(consent.consentType.equals(consentType))
    assert(consent.startDate.get.equals(startDate))
    assert(consent.endDate.get.equals(endDate))
  }

  test("factory") {
    val id = 123
    val districtServiceId = 456
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val consent = Consent(id, districtServiceId, consentType, Some(startDate), Some(endDate))
    assert(consent.id.equals(id))
    assert(consent.districtServiceId.equals(districtServiceId))
    assert(consent.consentType.equals(consentType))
    assert(consent.startDate.get.equals(startDate))
    assert(consent.endDate.get.equals(endDate))
  }

  test("node") {
    val id = 123
    val districtServiceId = 456
    val consentType = "test type"
    val startDate = "2016-01-01"
    val endDate = "2017-01-01"
    val consent = Consent(
      <consent>
        <id>{id}</id>
        <consentType>{consentType}</consentType>
        <startDate>{startDate}</startDate>
        <endDate>{endDate}</endDate>
      </consent>,
      Some(List(SifRequestParameter("districtServiceId", districtServiceId.toString)))
    )
    assert(consent.id.equals(id))
    assert(consent.districtServiceId.equals(districtServiceId))
    assert(consent.consentType.equals(consentType))
    assert(consent.startDate.get.equals(startDate))
    assert(consent.endDate.get.equals(endDate))
  }

  test("create") {
    val districtServiceId = 456
    val consent = Consent(
      <consent>
        <consentType>test type</consentType>
        <startDate>2016-01-01</startDate>
        <endDate>2017-01-01</endDate>
      </consent>,
      Some(List(SifRequestParameter("districtServiceId", districtServiceId.toString)))
    )
    val result = Consent.create(consent, List[SifRequestParameter]()).asInstanceOf[ConsentResult]
    createdId = result.getId
    assert(result.success)
    assert(result.exceptions.isEmpty)
    assert(result.toXml.get.toXmlString.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("update id parameter") {
    val districtServiceId = 456
    val consent = Consent(
      <consent>
        <consentType>test type UPDATED</consentType>
        <startDate>2018-01-01</startDate>
        <endDate>2019-01-01</endDate>
      </consent>,
      Some(List(SifRequestParameter("districtServiceId", districtServiceId.toString)))
    )
    val result = Consent.update(consent, List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.exceptions.isEmpty)
    val resultBody = result.toXml.get.toXmlString
    assert(resultBody.contains("id=\"%s\"".format(createdId.toString)))
  }

  test("query bad request") {
    val result = Consent.query(List[SifRequestParameter](SifRequestParameter("id", "abc")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.BadRequest)
    assert(result.toXml.isEmpty)
  }

  test("query not found") {
    val result = Consent.query(List[SifRequestParameter](SifRequestParameter("id", "9999999")))
    assert(!result.success)
    assert(result.statusCode == SifHttpStatusCode.NotFound)
    assert(result.toXml.isEmpty)
  }

  test("query by id") {
    val result = Consent.query(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    val resultBody = result.toJson.get.toJsonString
    assert(resultBody.contains("test type UPDATED"))
    assert(resultBody.contains("2018-01-01"))
    assert(resultBody.contains("2019-01-01"))
  }

  test("query all") {
    val result = Consent.query(null)
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
    assert(result.toXml.nonEmpty)
  }

  test("delete") {
    val result = Consent.delete(List[SifRequestParameter](SifRequestParameter("id", createdId.toString)))
    assert(result.success)
    assert(result.statusCode == SifHttpStatusCode.Ok)
  }

}
