package org.psesd.srx.services.prs

import org.json4s._
import org.psesd.srx.shared.core.SrxResource
import org.psesd.srx.shared.core.extensions.TypeExtensions._

import scala.xml.Node

/** Represents a Contact.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class Contact(
               val id: Int,
               val name: Option[String],
               val title: Option[String],
               val email: Option[String],
               val phone: Option[String],
               val mailingAddress: Option[String],
               val webAddress: Option[String]
             ) extends SrxResource {
  override def isEmpty = {
    name.isEmpty &&
    title.isEmpty &&
    email.isEmpty &&
    phone.isEmpty &&
    mailingAddress.isEmpty &&
    webAddress.isEmpty
  }

  def toJson: JValue = {
    if(isEmpty) {
      "{}".toJson
    } else {
      toXml.toJsonStringNoRoot.toJson
    }
  }

  def toXml: Node = {
    <mainContact>
      {optional(name.orNull, <name>{name.orNull}</name>)}
      {optional(title.orNull, <title>{title.orNull}</title>)}
      {optional(email.orNull, <email>{email.orNull}</email>)}
      {optional(phone.orNull, <phone>{phone.orNull}</phone>)}
      {optional(mailingAddress.orNull, <mailingAddress>{mailingAddress.orNull}</mailingAddress>)}
      {optional(webAddress.orNull, <webAddress>{webAddress.orNull}</webAddress>)}
    </mainContact>
  }
}

object Contact {
  def apply(
             id: Int,
             name: Option[String],
             title: Option[String],
             email: Option[String],
             phone: Option[String],
             mailingAddress: Option[String],
             webAddress: Option[String]
           ): Contact = {
    new Contact(
      id,
      name,
      title,
      email,
      phone,
      mailingAddress,
      webAddress
    )
  }

  def apply(contactXml: Node): Contact = {
    new Contact(
      (contactXml \ "id").textOption.getOrElse("0").toInt,
      (contactXml \ "name").textOption,
      (contactXml \ "title").textOption,
      (contactXml \ "email").textOption,
      (contactXml \ "phone").textOption,
      (contactXml \ "mailingAddress").textOption,
      (contactXml \ "webAddress").textOption
    )
  }
}
