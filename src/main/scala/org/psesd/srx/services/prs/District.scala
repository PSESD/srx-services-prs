package org.psesd.srx.services.prs

import org.json4s._
import org.json4s.JsonDSL._
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter, SifRequestParameterCollection}
import org.psesd.srx.shared.data.exceptions.DatasourceDuplicateViolationException
import org.psesd.srx.shared.data.{Datasource, DatasourceResult}

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/** Represents a District.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class District(
                val id: Int,
                val name: String,
                val ncesleaCode: Option[String],
                val zoneId: Option[String],
                val mainContact: Option[Contact],
                val services: Option[ArrayBuffer[DistrictService]]
              ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    if(services.isDefined) {
      if(mainContact.isDefined) {
        ("id" -> id.toString) ~
          ("name" -> name) ~
          ("ncesleaCode" -> ncesleaCode.orNull) ~
          ("zoneId" -> zoneId.orNull) ~
          ("mainContact" -> mainContact.get.toJson) ~
          ("services" -> services.get.map { d => d.toJson })
      } else {
        ("id" -> id.toString) ~
          ("name" -> name) ~
          ("ncesleaCode" -> ncesleaCode.orNull) ~
          ("zoneId" -> zoneId.orNull) ~
          ("services" -> services.get.map { d => d.toJson })
      }
    } else {
      if(mainContact.isDefined) {
        ("id" -> id.toString) ~
          ("name" -> name) ~
          ("ncesleaCode" -> ncesleaCode.orNull) ~
          ("zoneId" -> zoneId.orNull) ~
          ("mainContact" -> mainContact.get.toJson)
      } else {
        ("id" -> id.toString) ~
          ("name" -> name) ~
          ("ncesleaCode" -> ncesleaCode.orNull) ~
          ("zoneId" -> zoneId.orNull)
      }
    }
  }

  def toXml: Node = {
    <district>
      <id>{id.toString}</id>
      <name>{name}</name>
      {optional(ncesleaCode.orNull, <ncesleaCode>{ncesleaCode.orNull}</ncesleaCode>)}
      {optional(zoneId.orNull, <zoneId>{zoneId.orNull}</zoneId>)}
      {if(mainContact.isDefined && !mainContact.get.isEmpty) mainContact.get.toXml}
      {optional({if(services.isDefined) "true" else null}, <services>{if(services.isDefined) services.get.map(d => d.toXml)}</services>)}
    </district>
  }

}

/** Represents a District method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DistrictResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  District.getDistrictsFromResult,
    <districts/>,
  responseFormat
) {
}

/** PRS District methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object District extends PrsEntityService {
  def apply(id: Int, name: String, ncesleaCode: Option[String], zoneId: Option[String], mainContact: Option[Contact]): District = new District(id, name, ncesleaCode, zoneId, mainContact, None)

  def apply(requestBody: SrxRequestBody, parameters: Option[List[SifRequestParameter]]): District = {
    if (requestBody == null) {
      throw new ArgumentNullException("requestBody parameter")
    }
    apply(requestBody.getXml.orNull, parameters)
  }

  def apply(districtXml: Node, parameters: Option[List[SifRequestParameter]]): District = {
    if (districtXml == null) {
      throw new ArgumentNullException("districtXml parameter")
    }
    val rootElementName = districtXml.label
    if (rootElementName != "district" && rootElementName != "root") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (districtXml \ "id").textOption.getOrElse("0").toInt
    val name = (districtXml \ "name").textRequired("district.name")
    val ncesleaCode = (districtXml \ "ncesleaCode").textOption
    val zoneId = (districtXml \ "zoneId").textOption
    val mainContact = districtXml \ "mainContact"
    new District(
      id,
      name,
      ncesleaCode,
      zoneId,
      {
        if (mainContact != null && mainContact.length > 0) {
          Some(Contact(mainContact.head))
        } else {
          None
        }
      },
      None
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val district = resource.asInstanceOf[District]

      val datasource = new Datasource(datasourceConfig)

      var result: DatasourceResult = null

      if (district.mainContact.isDefined) {

        result = datasource.create(
          "with new_contact as (" +
            "insert into srx_services_prs.contact (" +
            "id, name, title, email, phone, mailing_address, web_address) values (" +
            "DEFAULT, ?, ?, ?, ?, ?, ?) " +
            "RETURNING id) " +
            "insert into srx_services_prs.district (" +
            "id, name, nces_lea_code, zone_id, main_contact_id) values (" +
            "DEFAULT, ?, ?, ?, (select id from new_contact)) " +
            "RETURNING id;",
          "id",
          district.mainContact.get.name.orNull,
          district.mainContact.get.title.orNull,
          district.mainContact.get.email.orNull,
          district.mainContact.get.phone.orNull,
          district.mainContact.get.mailingAddress.orNull,
          district.mainContact.get.webAddress.orNull,
          district.name,
          district.ncesleaCode.orNull,
          district.zoneId.orNull
        )

      } else {
        result = datasource.create(
          "with new_contact as (" +
            "insert into srx_services_prs.contact (" +
            "id) values (" +
            "DEFAULT) " +
            "RETURNING id) " +
            "insert into srx_services_prs.district (" +
            "id, name, nces_lea_code, zone_id, main_contact_id) values (" +
            "DEFAULT, ?, ?, ?, (select id from new_contact)) " +
            "RETURNING id;",
          "id",
          district.name,
          district.ncesleaCode.orNull,
          district.zoneId.orNull
        )
      }

      datasource.close()

      if (result.success) {
        PrsServer.logSuccessMessage(
          PrsResource.Districts.toString,
          SifRequestAction.Create.toString,
          result.id,
          SifRequestParameterCollection(parameters),
          Some(district.toXml.toXmlString)
        )
        val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
        if(responseFormat.equals(SrxResponseFormat.Object)) {
          val queryResult = executeQuery(Some(result.id.get.toInt))
          new DistrictResult(
            SifRequestAction.Create,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
            queryResult,
            responseFormat
          )
        } else {
          new DistrictResult(
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
      case dv: DatasourceDuplicateViolationException =>
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, dv)
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
            "delete from srx_services_prs.contact where id = (select main_contact_id from srx_services_prs.district where id = ?);" +
            "delete from srx_services_prs.district where id = ?;" +
            "commit;",
          id.get,
          id.get
        )

        datasource.close()

        if (result.success) {
          PrsServer.logSuccessMessage(
            PrsResource.Districts.toString,
            SifRequestAction.Delete.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            None
          )
          val aeResult = new DistrictResult(
            SifRequestAction.Delete,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete),
            result,
            SrxResponseFormat.getResponseFormat(parameters)
          )
          aeResult.setId(id.get)
          aeResult
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
      try {
        val result = executeQuery(id)
        if (result.success) {
          val resourceId = if (id.isEmpty) Some("all") else Some(id.get.toString)
          if (id.isDefined && result.rows.isEmpty) {
            PrsServer.logNotFoundMessage(
              PrsResource.Districts.toString,
              SifRequestAction.Query.toString,
              resourceId,
              SifRequestParameterCollection(parameters),
              None
            )
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.Districts.toString))
          } else {
            PrsServer.logSuccessMessage(
              PrsResource.Districts.toString,
              SifRequestAction.Query.toString,
              resourceId,
              SifRequestParameterCollection(parameters),
              None
            )
            new DistrictResult(
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

  private def executeQuery(id: Option[Int]): DatasourceResult = {
    val selectFrom = "select district.*, " +
      "contact.name main_contact_name, " +
      "contact.title main_contact_title, " +
      "contact.email main_contact_email, " +
      "contact.phone main_contact_phone, " +
      "contact.mailing_address main_contact_mailing_address, " +
      "contact.web_address main_contact_web_address, " +
      "district_service.id district_service_id, " +
      "district_service.external_service_id, " +
      "external_service.name external_service_name, " +
      "external_service.authorized_entity_id, " +
      "authorized_entity.name authorized_entity_name, " +
      "district_service.initiation_date, " +
      "district_service.expiration_date, " +
      "district_service.requires_personnel " +
      "from srx_services_prs.district " +
      "left join srx_services_prs.contact on contact.id = district.main_contact_id " +
      "left join srx_services_prs.district_service on district_service.district_id = district.id " +
      "left join srx_services_prs.external_service on external_service.id = district_service.external_service_id " +
      "left join srx_services_prs.authorized_entity on authorized_entity.id = external_service.authorized_entity_id "
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        datasource.get(selectFrom + "order by district.id;")
      } else {
        datasource.get(selectFrom + "where district.id = ?;", id.get)
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

      val district = resource.asInstanceOf[District]
      if ((id.isEmpty || id.get == 0) && district.id > 0) {
        id = Some(district.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        var result: DatasourceResult = null

        if (district.mainContact.isDefined) {

          result = datasource.execute(
            "begin;" +
              "update srx_services_prs.contact set " +
              "name = ?, title = ?, email = ?, phone = ?, mailing_address = ?, web_address = ? " +
              "where id = (select main_contact_id from srx_services_prs.district where id = ?); " +
              "update srx_services_prs.district set " +
              "name = ?, " +
              "nces_lea_code = ?, " +
              "zone_id = ? " +
              "where id = ?;" +
              "commit;",
            district.mainContact.get.name.orNull,
            district.mainContact.get.title.orNull,
            district.mainContact.get.email.orNull,
            district.mainContact.get.phone.orNull,
            district.mainContact.get.mailingAddress.orNull,
            district.mainContact.get.webAddress.orNull,
            id.get,
            district.name,
            district.ncesleaCode.orNull,
            district.zoneId.orNull,
            id.get
          )

        } else {
          result = datasource.execute(
            "update srx_services_prs.district set " +
              "name = ?, " +
              "nces_lea_code = ?, " +
              "zone_id = ? " +
              "where id = ?;",
            district.name,
            district.ncesleaCode.orNull,
            district.zoneId.orNull,
            id.get
          )
        }

        datasource.close()

        if (result.success) {
          PrsServer.logSuccessMessage(
            PrsResource.Districts.toString,
            SifRequestAction.Update.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            Some(district.toXml.toXmlString)
          )
          val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
          var dResult: DistrictResult = null
          if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(id.get))
            dResult = new DistrictResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              queryResult,
              responseFormat
            )
          } else {
            dResult = new DistrictResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              result,
              responseFormat
            )
          }
          dResult.setId(id.get)
          dResult
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

  def getDistrictsFromResult(result: DatasourceResult): List[District] = {
    val districts = ArrayBuffer[District]()
    for (row <- result.rows) {
      val id = row.getString("id").getOrElse("").toInt
      val mainContactId = row.getString("main_contact_id")
      val districtServiceId = row.getString("district_service_id")
      val district = districts.find(d => d.id.equals(id))
      if (district.isDefined) {
        if (districtServiceId.isDefined) {
          district.get.services.get += DistrictService(
            districtServiceId.get.toInt,
            id,
            row.getString("external_service_id").get.toInt,
            row.getString("external_service_name"),
            Some(row.getString("authorized_entity_id").get.toInt),
            row.getString("authorized_entity_name"),
            row.getString("initiation_date").get,
            row.getString("expiration_date").get,
            row.getBoolean("requires_personnel").get,
            None
          )
        }
      } else {
        districts += new District(
          id,
          row.getString("name").getOrElse(""),
          row.getString("nces_lea_code"),
          row.getString("zone_id"),
          {
            if (mainContactId.isDefined) {
              Some(Contact(
                mainContactId.get.toInt,
                row.getString("main_contact_name"),
                row.getString("main_contact_title"),
                row.getString("main_contact_email"),
                row.getString("main_contact_phone"),
                row.getString("main_contact_mailing_address"),
                row.getString("main_contact_web_address")
              ))
            } else {
              None
            }
          },
          {
            if(districtServiceId.isDefined)
              Some(ArrayBuffer[DistrictService](DistrictService(
                districtServiceId.get.toInt,
                id,
                row.getString("external_service_id").get.toInt,
                row.getString("external_service_name"),
                Some(row.getString("authorized_entity_id").get.toInt),
                row.getString("authorized_entity_name"),
                row.getString("initiation_date").get,
                row.getString("expiration_date").get,
                row.getBoolean("requires_personnel").get,
                None
              )))
            else
              None
          }
        )
      }
    }
    districts.toList
  }

}
