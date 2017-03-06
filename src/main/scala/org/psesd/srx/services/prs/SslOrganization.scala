package org.psesd.srx.services.prs

import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.Document
import org.mongodb.scala.bson.BsonDateTime
import org.psesd.srx.shared.core.sif.SifRequestParameter

import scala.xml.Node

/** Student Success Link Data Organization Model
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
object SslOrganization extends SslEntity{

  def apply(authorizedEntityId: String, externalServiceId: String = null): Document = {
    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)
    val timestamp = bsonTimeStamp

    var organization = Document("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text,
      "website" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
      "url" -> PrsServer.serverName,
      "authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt,
      "created_at" -> timestamp,
      "updated_at" -> timestamp)

    if (externalServiceId != null) organization ++= Document("externalServiceId" -> externalServiceId.toInt)

    organization
  }

}
