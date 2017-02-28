package org.psesd.srx.services.prs

import com.mongodb.ServerAddress
import com.mongodb.casbah.{MongoClient, MongoCollection, MongoCredential}
import com.mongodb.casbah.commons.MongoDBObject
import org.psesd.srx.shared.core.sif.SifRequestParameter

import scala.xml.Node

/** MongoDB Connection for Student Success Link Data
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
class MongoDataSource {

  def action(action: String, authorizedEntityId: String, externalServiceId: String = null): Unit = {
    val mongoClient = connectMongoClient
    val mongoDb = mongoClient(PrsServer.mongoDbName)

    val organizationsTable = mongoDb("organizations")
    lazy val usersTable = mongoDb("users")

    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)

    action match {
      case "delete"  => delete(organizationsTable, authorizedEntityXml, usersTable)
      case "insert"  => insert(organizationsTable, authorizedEntityXml, externalServiceId, usersTable)
      case "update"  => update(organizationsTable, authorizedEntityXml)
    }

    mongoClient.close()
  }

  private def delete(organizationsTable: MongoCollection, authorizedEntityXml: Node, usersTable: MongoCollection): Unit = {
    val authorizedEntityQuery = MongoDBObject("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text)
    organizationsTable.findAndRemove(authorizedEntityQuery)

    deleteAdminUser(authorizedEntityXml, usersTable)
  }

  private def deleteAdminUser(authorizedEntityXml: Node, usersTable: MongoCollection): Unit = {
    val adminUserEmail = (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text

    val adminUserQuery = MongoDBObject("email" -> adminUserEmail)
    usersTable.findAndRemove(adminUserQuery)
  }

  private def insert(organizationsTable: MongoCollection, authorizedEntityXml: Node, externalServiceId: String, usersTable: MongoCollection): Unit = {
    val organization = MongoDBObject( "name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text,
                                      "website" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
                                      "url" -> PrsServer.serverName,
                                      "authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt,
                                      "externalServiceId" -> externalServiceId.toInt  )

    organizationsTable.save(organization)

    insertAdminUser(usersTable, authorizedEntityXml)
  }

  private def insertAdminUser(usersTable: MongoCollection, authorizedEntityXml: Node): Unit = {
    val mainContactName = (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "name").text
    val mainContactNameArr = mainContactName.split(" ")

    val firstName = mainContactNameArr(0)
    var middleName = ""
    var lastName = ""

    if (mainContactNameArr.length == 2) {
      lastName = mainContactNameArr(1)
    } else if (mainContactNameArr.length >= 3) {
      middleName = mainContactNameArr(1)
      lastName = mainContactNameArr(mainContactNameArr.length - 1)
    }

    val adminUser = MongoDBObject("email" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text,
                                  "first_name" -> firstName,
                                  "middle_name" -> middleName,
                                  "last_name" -> lastName,
                                  "salt" -> PrsServer.mongoUserSalt,
                                  "hashedPassword" -> PrsServer.mongoUserHashedPassword)

    usersTable.save(adminUser)
  }

  private def getAuthorizedEntity(authorizedEntityId: String): Node = {
    val authorizedEntityResult = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityId)))
    authorizedEntityResult.toXml.get
  }

  private def update(organizationsTable: MongoCollection, authorizedEntityXml: Node): Unit = {
    val query = MongoDBObject("authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text)

    val updatedOrganization = MongoDBObject("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text,
                                            "website" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
                                            "url" -> PrsServer.serverName,
                                            "authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt)

    organizationsTable.update(query, updatedOrganization)
  }

  private def connectMongoClient: MongoClient = {
    var mongoClient: MongoClient = null.asInstanceOf[MongoClient]

    if (PrsServer.mongoDbHost == "localhost") {
      mongoClient = MongoClient(PrsServer.mongoDbHost, PrsServer.mongoDbPort.toInt)
    } else {
      val server = new ServerAddress(PrsServer.mongoDbHost, PrsServer.mongoDbPort.toInt)
      val credentials = MongoCredential.createCredential(PrsServer.mongoDbName, PrsServer.mongoDbName, PrsServer.mongoDbPassword.toString.toArray)
      mongoClient = MongoClient(server, List(credentials))
    }

    mongoClient
  }
}
