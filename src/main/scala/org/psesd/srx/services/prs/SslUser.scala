package org.psesd.srx.services.prs

import org.mongodb.scala.Document
import org.mongodb.scala.bson.BsonValue

/** Student Success Link User Model
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
object SslUser extends SslEntity {

  def apply(authorizedEntityId: String, organizationId: BsonValue): Document = {
    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)
    val mainContact = (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "name").text
    val contact = contactName(mainContact)
    val timestamp = bsonTimeStamp

    val userPermission = Document("organization" -> organizationId,
      "activateStatus" -> "Active",
      "activateDate" -> timestamp,
      "activate" -> "true",
      "role" -> "admin")

    Document( "email" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text,
      "first_name" -> contact(0),
      "middle_name" -> contact(1),
      "last_name" -> contact(2),
      "salt" -> PrsServer.mongoUserSalt,
      "hashedPassword" -> PrsServer.mongoUserHashedPassword,
      "last_updated" -> timestamp,
      "created" -> timestamp,
      "created_at" -> timestamp,
      "updated_at" -> timestamp,
      "permissions" -> List(userPermission))
  }

  private def contactName(mainContactName: String): Array[String] = {
    var contactNameArr = mainContactName.split(" ")

    if (contactNameArr.length == 1) {
      contactNameArr ++= Array("", "")
    } else if (contactNameArr.length == 2) {
      contactNameArr ++= Array("")
    }

    contactNameArr
  }

}
