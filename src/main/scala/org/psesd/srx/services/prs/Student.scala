package org.psesd.srx.services.prs

import org.json4s.JValue
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core.{SrxResource, SrxResourceErrorResult, SrxResourceResult, SrxResponseFormat}
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter}
import org.psesd.srx.shared.data.{Datasource, DatasourceResult}

import scala.collection.mutable.ArrayBuffer
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
      } else {
        throw result.exceptions.head
      }
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
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
            "delete from srx_services_prs.consent where id = (select consent_id from srx_services_prs.student where id = ?);" +
            "delete from srx_services_prs.student where id = ?;" +
            "commit;",
          id.get,
          id.get
        )

        datasource.close()

        if (result.success) {
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
            if (id.isDefined && result.rows.isEmpty) {
              SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.Students.toString))
            } else {
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
      "left join srx_services_prs.district_service on district_service.id = student.district_service_id "
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        if(districtServiceIdParam.isDefined) {
          datasource.get(selectFrom + "where student.district_service_id = ? order by student.id;", districtServiceIdParam.get.value.toInt)
        } else {
          datasource.get(selectFrom + "where district_service.district_id = ? order by student.id;", districtIdParam.get.value.toInt)
        }
      } else {
        datasource.get(selectFrom + "where student.id = ?;", id.get)
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
