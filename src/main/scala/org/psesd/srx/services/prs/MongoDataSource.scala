package org.psesd.srx.services.prs

import com.mongodb.casbah.{MongoClient, MongoClientURI, MongoCollection}
import com.mongodb.casbah.commons.MongoDBObject
import org.psesd.srx.shared.core.sif.SifRequestParameter
import org.psesd.srx.shared.data.DatasourceResult

import scala.xml.Node

/** MongoDB Connection for Student Success Link Data
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
class MongoDataSource {

  def action(action: String, authorizedEntityId: String, dataSourceResult: DatasourceResult = null): Unit = {
    val mongoClientURI = MongoClientURI(PrsServer.mongoUri)
    val mongoClient = MongoClient(mongoClientURI)

    val mongoDb = mongoClient(PrsServer.mongoDbName)
    val organizationsTable = mongoDb("organizations")

    val authorizedEntityResult = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityId)))
    val authorizedEntityXml = authorizedEntityResult.toXml.get

    action match {
      case "insert"  => insert(organizationsTable, authorizedEntityXml, dataSourceResult)
      case "delete"  => delete(organizationsTable, authorizedEntityXml)
    }

    mongoClient.close()
  }

  private def insert(organizationsTable: MongoCollection, authorizedEntityXml: Node, dataSourceResult: DatasourceResult): Unit = {
    val organization = MongoDBObject( "name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text,
      "website" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
      "url" -> PrsServer.serverName,
      "authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt,
      "externalServiceId" -> dataSourceResult.id.get.toInt  )

    organizationsTable.save(organization)
  }

  private def delete(organizationsTable: MongoCollection, authorizedEntityXml: Node): Unit = {
    val authorizedEntityQuery = MongoDBObject("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text)
    organizationsTable.findAndRemove(authorizedEntityQuery)
  }
}
