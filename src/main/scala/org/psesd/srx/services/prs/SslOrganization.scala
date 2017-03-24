package org.psesd.srx.services.prs

import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.{BsonDateTime, BsonDocument, BsonValue}
import org.psesd.srx.shared.core.sif.SifRequestParameter

import scala.xml.Node

/** Student Success Link Data Organization Model
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
object SslOrganization extends SslEntity{

  def apply(authorizedEntityId: String, externalServiceId: BsonValue, createdAt: BsonValue = null): Document = {
    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)
    val timestamp = bsonTimeStamp

    var organization = Document("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text,
      "website" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
      "url" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
      "authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt,
      "externalServiceId" -> externalServiceId,
      "updated_at" -> timestamp)

    if (createdAt != null) {
      organization ++= Document("created_at" -> createdAt)
    } else {
      organization ++= Document("created_at" -> timestamp)
    }

    organization
  }

  def name(authorizedEntityId: String): String = {
    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)
    (authorizedEntityXml \ "authorizedEntity" \ "name").text
  }

//  def update(authorizedEntityId: String): BsonDocument = {
//    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)
//    val timestamp = bsonTimeStamp
//
//    new BsonDocument("name", (authorizedEntityXml \ "authorizedEntity" \ "name").text,
//      "website", (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
//      "url", PrsServer.serverName,
//      "authorizedEntityId", (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt,
//      "updated_at", timestamp)
//
//
//
//
//
////    new BsonDocument("_id", new BsonInt32(2)).append
//  }

}
