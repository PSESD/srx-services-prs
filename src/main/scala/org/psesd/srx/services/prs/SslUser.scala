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

  def apply(authorizedEntityId: String, organizationId: BsonValue, createdAt: BsonValue = null): Document = {
    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)
    val mainContact = (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "name").text
    val contact = contactName(mainContact)
    val timestamp = bsonTimeStamp

    var userPermission = Document("organization" -> organizationId,
      "activateStatus" -> "Active",
      "activate" -> "true",
      "role" -> "admin")

    var adminUser = Document( "email" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text,
      "first_name" -> contact(0),
      "middle_name" -> contact(1),
      "last_name" -> contact(2),
      "salt" -> PrsServer.mongoUserSalt,
      "hashedPassword" -> PrsServer.mongoUserHashedPassword,
      "last_updated" -> timestamp,
      "updated_at" -> timestamp,
      "permissions" -> List(userPermission))


    if (createdAt != null) {
      userPermission ++= Document("activateDate" -> createdAt)
      adminUser ++= Document("created_at" -> createdAt)
      adminUser ++= Document("created" -> createdAt)
    } else {
      userPermission ++= Document("activateDate" -> timestamp)
      adminUser ++= Document("created_at" -> timestamp)
      adminUser ++= Document("created" -> timestamp)
    }

    adminUser
  }

  private def contactName(mainContactName: String): Array[String] = {
    var contactNameArr = mainContactName.split(" ")

    if (contactNameArr.length == 1) {
      contactNameArr ++= Array("", "")
    } else if (contactNameArr.length == 2) {
      contactNameArr = Array(contactNameArr(0)) ++ Array("") ++ Array(contactNameArr(1))
    }

    contactNameArr
  }

}
