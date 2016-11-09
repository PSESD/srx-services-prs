package org.psesd.srx.services.prs

import org.json4s.JValue
import org.psesd.srx.shared.core.{SrxResource, SrxResourceErrorResult, SrxResourceResult}
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter}
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
                        val mainContact: Option[Contact]
                      ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <district>
      <id>{id.toString}</id>
      <name>{name}</name>
      {optional(ncesleaCode.orNull, <ncesleaCode>{ncesleaCode.orNull}</ncesleaCode>)}
      {optional(zoneId.orNull, <zoneID>{zoneId.orNull}</zoneID>)}
      {if(mainContact.isDefined && !mainContact.get.isEmpty) mainContact.get.toXml}
    </district>
  }

}

/** Represents a District method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class DistrictResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  District.getDistrictsFromResult,
    <districts/>
) {
}

/** PRS District methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object District extends PrsEntityService {
  def apply(id: Int, name: String, ncesleaCode: Option[String], zoneId: Option[String], mainContact: Option[Contact]): District = new District(id, name, ncesleaCode, zoneId, mainContact)

  def apply(districtXml: Node, parameters: Option[List[SifRequestParameter]]): District = {
    if (districtXml == null) {
      throw new ArgumentNullException("districtXml parameter")
    }
    val rootElementName = districtXml.label
    if (rootElementName != "district") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (districtXml \ "id").textOption.getOrElse("0").toInt
    val name = (districtXml \ "name").textRequired("district.name")
    val ncesleaCode = (districtXml \ "ncesleaCode").textOption
    val zoneId = (districtXml \ "zoneID").textOption
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
      }
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
        new DistrictResult(SifRequestAction.Create, SifRequestAction.getSuccessStatusCode(SifRequestAction.Create), result)
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
            "delete from srx_services_prs.contact where id = (select main_contact_id from srx_services_prs.district where id = ?);" +
            "delete from srx_services_prs.district where id = ?;" +
            "commit;",
          id.get,
          id.get
        )

        datasource.close()

        if (result.success) {
          val aeResult = new DistrictResult(SifRequestAction.Delete, SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete), result)
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
        val selectFrom = "select srx_services_prs.district.*, " +
          "srx_services_prs.contact.name main_contact_name, " +
          "srx_services_prs.contact.title main_contact_title, " +
          "srx_services_prs.contact.email main_contact_email, " +
          "srx_services_prs.contact.phone main_contact_phone, " +
          "srx_services_prs.contact.mailing_address main_contact_mailing_address, " +
          "srx_services_prs.contact.web_address main_contact_web_address " +
          "from srx_services_prs.district " +
          "join srx_services_prs.contact on srx_services_prs.contact.id = srx_services_prs.district.main_contact_id "
        val datasource = new Datasource(datasourceConfig)
        val result = {
          if (id.isEmpty) {
            datasource.get(selectFrom + "order by srx_services_prs.district.id;")
          } else {
            datasource.get(selectFrom + "where srx_services_prs.district.id = ?;", id.get)
          }
        }
        datasource.close()
        if (result.success) {
          if (id.isDefined && result.rows.isEmpty) {
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.Districts.toString))
          } else {
            new DistrictResult(SifRequestAction.Query, SifHttpStatusCode.Ok, result)
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
          val aeResult = new DistrictResult(SifRequestAction.Update, SifRequestAction.getSuccessStatusCode(SifRequestAction.Update), result)
          aeResult.setId(id.get)
          aeResult
        } else {
          throw result.exceptions.head
        }
      }
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def getDistrictsFromResult(result: DatasourceResult): List[District] = {
    val districts = ArrayBuffer[District]()
    for (row <- result.rows) {
      val mainContactId = row.getString("main_contact_id")
      val district = District(
        row.getString("id").getOrElse("").toInt,
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
        }
      )
      districts += district
    }
    districts.toList
  }

}
