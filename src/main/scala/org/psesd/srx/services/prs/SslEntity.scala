package org.psesd.srx.services.prs

import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.BsonDateTime
import org.psesd.srx.shared.core.sif.SifRequestParameter

import scala.xml.Node

/** Student Success Link User Model
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */

trait SslEntity {

  def bsonTimeStamp: BsonDateTime = {
    BsonDateTime(DateTime.now(DateTimeZone.UTC).getMillis)
  }

  def getAuthorizedEntity(authorizedEntityId: String): Node = {
    val authorizedEntityResult = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityId)))
    val xml = authorizedEntityResult.toXml
    authorizedEntityResult.toXml.get
  }

}
