package org.psesd.srx.services.prs

import java.math.BigInteger
import java.util.concurrent.TimeUnit

import org.json4s.JValue
import org.mongodb.scala.model.Filters
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter, SifRequestParameterCollection, SifZone}
import org.psesd.srx.shared.data.exceptions.DatasourceDuplicateViolationException
import org.psesd.srx.shared.data.{DataRow, Datasource, DatasourceResult}
import org.mongodb.scala.{Completed, Document, MongoCollection, Observer, Subscription, bson}

import scala.concurrent.duration.Duration
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.xml.Node

/** Represents a Student.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class Student(
                val id: Int,
                val districtServiceId: Int,
                val districtStudentId: String,
                val consent: Option[Consent]
              ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <student>
      <id>{id.toString}</id>
      <districtServiceId>{districtServiceId.toString}</districtServiceId>
      <districtStudentId>{districtStudentId}</districtStudentId>
      {if(consent.isDefined) consent.get.toXml}
    </student>
  }

}

/** Represents a Student method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class StudentResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  Student.getStudentsFromResult,
  <students/>,
  responseFormat
) {
}

/** PRS Student methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object Student extends PrsEntityService {
  def apply(id: Int, districtServiceId: Int, districtStudentId: String, consent: Option[Consent]): Student = new Student(id, districtServiceId, districtStudentId, consent)

  def apply(requestBody: SrxRequestBody, parameters: Option[List[SifRequestParameter]]): Student = {
    if (requestBody == null) {
      throw new ArgumentNullException("requestBody parameter")
    }
    apply(requestBody.getXml.orNull, parameters)
  }

  def apply(studentXml: Node, parameters: Option[List[SifRequestParameter]]): Student = {
    if (studentXml == null) {
      throw new ArgumentNullException("studentXml parameter")
    }
    val rootElementName = studentXml.label
    if (rootElementName != "student" && rootElementName != "root") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (studentXml \ "id").textOption.getOrElse("0").toInt
    val districtServiceId = (studentXml \ "districtServiceId").textOption.getOrElse("0").toInt
    val districtStudentId = (studentXml \ "districtStudentId").textRequired("student.districtStudentId")
    val consent = studentXml \ "consent"
    new Student(
      id,
      districtServiceId,
      districtStudentId,
      {
        if (consent != null && consent.length > 0) {
          Some(Consent(consent.head, Some(List[SifRequestParameter](SifRequestParameter("districtServiceId", districtServiceId.toString)))))
        } else {
          None
        }
      }
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val student = resource.asInstanceOf[Student]

      val datasource = new Datasource(datasourceConfig)

      var result: DatasourceResult = null

      if (student.consent.isDefined) {

        result = datasource.create(
          "with new_consent as (" +
            "insert into srx_services_prs.consent (" +
            "id, district_service_id, consent_type, start_date, end_date) values (" +
            "DEFAULT, ?, ?, ?, ?) " +
            "RETURNING id) " +
            "insert into srx_services_prs.student (" +
            "id, district_service_id, district_student_id, consent_id) values (" +
            "DEFAULT, ?, ?, (select id from new_consent)) " +
            "RETURNING id;",
          "id",
          student.districtServiceId,
          student.consent.get.consentType,
          student.consent.get.getStartDate,
          student.consent.get.getEndDate,
          student.districtServiceId,
          student.districtStudentId
        )

      } else {
        result = datasource.create(
          "with new_consent as (" +
            "insert into srx_services_prs.consent (" +
            "id) values (" +
            "DEFAULT) " +
            "RETURNING id) " +
            "insert into srx_services_prs.student (" +
            "id, district_service_id, district_student_id, consent_id) values (" +
            "DEFAULT, ?, ?, (select id from new_consent)) " +
            "RETURNING id;",
          "id",
          student.districtServiceId,
          student.districtStudentId
        )
      }

      datasource.close()

      if (result.success) {
        val requestParams = SifRequestParameterCollection(parameters)

        PrsServer.logSuccessMessage(
          PrsResource.DistrictServiceStudents.toString,
          SifRequestAction.Create.toString,
          result.id,
          requestParams,
          Some(student.toXml.toXmlString)
        )

        val zone = determineBestZoneInfo(requestParams)

        val refreshResult = XsreRefreshRequestService.sendRequest(SifZone(zone), student.districtStudentId, requestParams("generatorId").get)
        PrsServer.logMessage(
          "XsreRefresh",
          SifRequestAction.Create.toString,
          Some(SifZone(requestParams("zoneId").get)),
          Some(student.districtStudentId),
          requestParams,
          None,
          refreshResult.statusCode.toString,
          "Xsre refresh request submitted for student " + student.districtStudentId)


        insertStudentIntoSSLDatabase(student, requestParams, result.id.get, zone)

        val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
        if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(result.id.get.toInt), None, None)
            new StudentResult(
              SifRequestAction.Create,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
              queryResult,
              responseFormat
            )
          } else {
            new StudentResult(
              SifRequestAction.Create,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
              result,
              responseFormat
            )
          }

      } else {  //result.fail
        throw result.exceptions.head
      }
    } catch {
      case dv: DatasourceDuplicateViolationException =>
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, dv)
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  private def determineBestZoneInfo(requestParams: SifRequestParameterCollection) : String = {
    requestParams("zoneId").getOrElse("") match {
      case "seattle" | "renton" | "highline" => requestParams("zoneId").get
      case _ =>
        val districtNode : Node = District.query(List[SifRequestParameter](SifRequestParameter("id", requestParams("districtId").getOrElse("")))).toXml.get
        (districtNode \ "district" \ "zoneId").text
    }

  }

  private def insertStudentIntoSSLDatabase(student: Student, requestParams: SifRequestParameterCollection, studentDbId : String, zone: String) : Unit = {
    val sslDataSource = new MongoDataSource
    val students = sslDataSource.retrieveCollection("students")
    val organizationsCollection : MongoCollection[Document] = sslDataSource.retrieveCollection("organizations")

    //retrieve organization info necessary for saving student into SSL db
    val organization = DistrictService.query(List[SifRequestParameter] {SifRequestParameter("id", student.districtServiceId.toString)})

    //query SSL db for organization id
    val aeId : Int = (organization.toXml.get \\ "authorizedEntityId").text.toInt
    val esId : Int = (organization.toXml.get \\"externalServiceId").text.toInt

    val org = organizationsCollection
        .find(Filters.and(Filters.equal("authorizedEntityId", aeId), Filters.equal("externalServiceId", esId)))
        .first()

    val orgResponse = Await.result(org.toFuture, Duration(10, TimeUnit.SECONDS))

    if (orgResponse != null && orgResponse.nonEmpty && orgResponse.head.nonEmpty) {
      val orgId = orgResponse.head.get("_id").get.asObjectId()

      //create student to insert
      val doc: Document = Document(
        "district_student_id" -> student.districtStudentId,
        "organization" -> orgId,
        "school_district" -> zone
      )

      //perform insert
      students.insertOne(doc).subscribe(new Observer[Completed] {
        override def onNext(result: Completed): Unit = {
          PrsServer.logSuccessMessage(
            "SSLStudentInsert",
            SifRequestAction.Create.toString,
            Some(student.districtStudentId),
            requestParams,
            Some(student.toXml.toXmlString)
          )
        }

        override def onError(e: Throwable) = {
          PrsServer.logMessage("SSLStudentInsert",
            SifRequestAction.Create.toString,
            Some(SifZone(requestParams("zoneId").get)),
            Some(student.districtStudentId),
            requestParams,
            None,
            "",
            e.getMessage)
          sslDataSource.close
        }

        override def onComplete(): Unit = {
          sslDataSource.close
        }
      })
    }

  }


  def delete(parameters: List[SifRequestParameter]): SrxResourceResult = {
    val id = getKeyIdFromRequestParameters(parameters)
    if (id.isEmpty || id.get == -1) {
      SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
    } else {
      try {
        val datasource = new Datasource(datasourceConfig)

        val result = datasource.execute(
          "begin;" +
            "delete from srx_services_prs.student where id = ?;" +
            "commit;",
          id.get
        )

        datasource.close()

        if (result.success) {
          PrsServer.logSuccessMessage(
            PrsResource.DistrictServiceStudents.toString,
            SifRequestAction.Delete.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            None
          )
          val sResult = new StudentResult(
            SifRequestAction.Delete,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete),
            result,
            SrxResponseFormat.getResponseFormat(parameters)
          )
          sResult.setId(id.get)
          sResult
        } else {
          throw result.exceptions.head
        }
      } catch {
        case e: Exception =>
          SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
      }
    }
  }

  def query(parameters: List[SifRequestParameter]): SrxResourceResult = {
    val id = getKeyIdFromRequestParameters(parameters)
    if (id.isDefined && id.get == -1) {
      SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
    } else {
      val districtIdParam = parameters.find(p => p.key.toLowerCase == "districtid")
      val districtServiceIdParam = parameters.find(p => p.key.toLowerCase == "districtserviceid")
      if (id.isEmpty && (districtIdParam == null || districtIdParam.isEmpty) && (districtServiceIdParam == null || districtServiceIdParam.isEmpty)) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("districtId or districtServiceId parameter"))
      } else {
        try {
          val result = executeQuery(id, districtIdParam, districtServiceIdParam)
          if (result.success) {
            var resource = ""
            var resourceId = Some("")
            if (id.isDefined) {
              resource = PrsResource.DistrictServiceStudents.toString
              resourceId = Some(id.get.toString)
            } else {
              resource = PrsResource.DistrictStudents.toString
              resourceId = Some("all")
            }

            if (id.isDefined && result.rows.isEmpty) {
              PrsServer.logNotFoundMessage(
                resource,
                SifRequestAction.Query.toString,
                resourceId,
                SifRequestParameterCollection(parameters),
                None
              )
              SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.Students.toString))
            } else {
              PrsServer.logSuccessMessage(
                resource,
                SifRequestAction.Query.toString,
                resourceId,
                SifRequestParameterCollection(parameters),
                None
              )
              new StudentResult(
                SifRequestAction.Query,
                SifHttpStatusCode.Ok,
                result,
                SrxResponseFormat.getResponseFormat(parameters)
              )
            }
          } else {
            throw result.exceptions.head
          }
        } catch {
          case e: Exception =>
            SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
        }
      }
    }
  }

  private def executeQuery(id: Option[Int], districtIdParam: Option[SifRequestParameter], districtServiceIdParam: Option[SifRequestParameter]): DatasourceResult = {
    val selectFrom = "select student.id, " +
      "student.district_service_id student_district_service_id, " +
      "student.district_student_id, " +
      "consent.district_service_id consent_district_service_id, " +
      "consent.id consent_consent_id, " +
      "consent.consent_type, " +
      "consent.start_date, " +
      "consent.end_date, " +
      "district_service.district_id district_service_district_id " +
      "from srx_services_prs.student " +
      "join srx_services_prs.consent on consent.id = student.consent_id " +
      "left join srx_services_prs.district_service on district_service.id = student.district_service_id " +
      "where district_service.expiration_date <= (select current_date) "
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        if(districtServiceIdParam.isDefined) {
          datasource.get(selectFrom + "and student.district_service_id = ? order by student.id;", districtServiceIdParam.get.value.toInt)
        } else {
          datasource.get(selectFrom + "and district_service.district_id = ? order by student.id;", districtIdParam.get.value.toInt)
        }
      } else {
        datasource.get(selectFrom + "and student.id = ?;", id.get)
      }
    }
    datasource.close()
    result
  }

  def update(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    if (resource == null) {
      throw new ArgumentNullException("resource parameter")
    }
    try {
      var id = getKeyIdFromRequestParameters(parameters)

      val student = resource.asInstanceOf[Student]
      if ((id.isEmpty || id.get == 0) && student.id > 0) {
        id = Some(student.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        var result: DatasourceResult = null

        if (student.consent.isDefined) {

          result = datasource.execute(
            "begin;" +
              "update srx_services_prs.consent set " +
              "district_service_id = ?, consent_type = ?, start_date = ?, end_date = ? " +
              "where id = (select consent_id from srx_services_prs.student where id = ?); " +
              "update srx_services_prs.student set " +
              "district_service_id = ?, " +
              "district_student_id = ? " +
              "where id = ?;" +
              "commit;",
            student.districtServiceId,
            student.consent.get.consentType,
            student.consent.get.getStartDate,
            student.consent.get.getEndDate,
            id.get,
            student.districtServiceId,
            student.districtStudentId,
            id.get
          )

        } else {
          result = datasource.execute(
            "update srx_services_prs.student set " +
              "district_service_id = ?, " +
              "district_student_id = ? " +
              "where id = ?;",
            student.districtServiceId,
            student.districtStudentId,
            id.get
          )
        }

        datasource.close()

        if (result.success) {
          PrsServer.logSuccessMessage(
            PrsResource.DistrictServiceStudents.toString,
            SifRequestAction.Update.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            Some(student.toXml.toXmlString)
          )
          val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
          var sResult: StudentResult = null
          if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(id.get), None, None)
            sResult = new StudentResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              queryResult,
              responseFormat
            )
          } else {
            sResult = new StudentResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              result,
              responseFormat
            )
          }
          sResult.setId(id.get)
          sResult
        } else {
          throw result.exceptions.head
        }
      }
    } catch {
      case dv: DatasourceDuplicateViolationException =>
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, dv)
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def getStudentsFromResult(result: DatasourceResult): List[Student] = {
    val students = ArrayBuffer[Student]()
    for (row <- result.rows) {
      val districtServiceId = row.getString("student_district_service_id").getOrElse("").toInt
      val consentId = row.getString("consent_consent_id")
      val student = Student(
        row.getString("id").getOrElse("").toInt,
        districtServiceId,
        row.getString("district_student_id").getOrElse(""),
        {
          if (consentId.isDefined) {
            Some(Consent(
              consentId.get.toInt,
              districtServiceId,
              row.getString("consent_type").getOrElse(""),
              row.getString("start_date"),
              row.getString("end_date")
            ))
          } else {
            None
          }
        }
      )
      students += student
    }
    students.toList
  }

}
