package org.psesd.srx.services.prs

import org.mongodb.scala.Document
import org.mongodb.scala.bson.BsonValue
import org.psesd.srx.services.prs.SslUser.{bsonTimeStamp, getAuthorizedEntity}

/** Student Success Link User Model
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */

class SslUser (val authorizedEntityId: String, val organizationId: BsonValue, val createdAt: BsonValue = null) {
  val authorizedEntityXml : scala.xml.Node = getAuthorizedEntity(authorizedEntityId)
  val mainContact : String = (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "name").text
  val contact : Array[String] = contactName(mainContact)
  val timestamp = bsonTimeStamp
  val email : String = (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text

  var userPermissions: List[org.mongodb.scala.bson.collection.mutable.Document] = List[org.mongodb.scala.bson.collection.mutable.Document](new UserPermission(organizationId).toDocument)

  def toDocument : Document = {
    var adminUser = Document(
      "email" -> email,
      "first_name" -> contact(0),
      "middle_name" -> contact(1),
      "last_name" -> contact(2),
      "salt" -> PrsServer.mongoUserSalt,
      "hashedPassword" -> PrsServer.mongoUserHashedPassword,
      "last_updated" -> timestamp,
      "updated_at" -> timestamp,
      "permissions" -> userPermissions
    )

    val createdDate = if (createdAt != null) createdAt else timestamp

    for (p <- userPermissions) {
      p += ("activateDate" -> createdDate)
    }
    adminUser ++= Document("created_at" -> timestamp)
    adminUser ++= Document("created" -> timestamp)

    adminUser
  }

  def addOrganization(organizationId: BsonValue) : Unit = {
    userPermissions ::= new UserPermission(organizationId).toDocument
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

  private class UserPermission(val organizationId: BsonValue) {
    def toDocument : org.mongodb.scala.bson.collection.mutable.Document = {
      org.mongodb.scala.bson.collection.mutable.Document("organization" -> organizationId,
        "activateStatus" -> "Active",
        "activate" -> "true",
        "role" -> "admin")
    }
  }

}

object SslUser extends SslEntity {

  def apply(authorizedEntityId: String, organizationId: BsonValue, createdAt: BsonValue = null): Document = {
    new SslUser(authorizedEntityId, organizationId, createdAt).toDocument
  }
}
