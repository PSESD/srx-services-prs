package org.psesd.srx.services.prs

import com.mongodb.ServerAddress
import com.mongodb.casbah.{MongoClient, MongoCollection, MongoCredential}
import com.mongodb.casbah.commons.{MongoDBList, MongoDBObject}
import com.mongodb.casbah.Imports._
import org.joda.time.{DateTime, DateTimeZone}
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
    val usersTable = mongoDb("users")

    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)

    action match {
      case "delete"  => deleteOrganization(organizationsTable, authorizedEntityXml, usersTable)
      case "insert"  => insertOrganization(organizationsTable, authorizedEntityXml, externalServiceId, usersTable)
      case "update"  => updateOrganization(organizationsTable, authorizedEntityXml, usersTable)
    }

    mongoClient.close()
  }

  def connectMongoClient: MongoClient = {
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

  private def deleteAdminUser(authorizedEntityXml: Node, usersTable: MongoCollection): Unit = {
    val adminUserQuery = MongoDBObject("email" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text)
    usersTable.findAndRemove(adminUserQuery)
  }

  private def deleteOrganization(organizationsTable: MongoCollection, authorizedEntityXml: Node, usersTable: MongoCollection): Unit = {
    val authorizedEntityQuery = MongoDBObject("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text)
    organizationsTable.findAndRemove(authorizedEntityQuery)

    deleteAdminUser(authorizedEntityXml, usersTable)
  }

  private def insertAdminUser(usersTable: MongoCollection, authorizedEntityXml: Node, organizationId: String): Unit = {
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

    val userPermission = $push("organization" -> organizationId,
                                        "activateStatus" -> "Active",
                                        "activateDate" -> DateTime.now(DateTimeZone.UTC),
                                        "activate" -> "true",
                                        "role" -> "admin" )


    val adminUser = MongoDBObject("email" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text,
                                  "first_name" -> firstName,
                                  "middle_name" -> middleName,
                                  "last_name" -> lastName,
                                  "salt" -> PrsServer.mongoUserSalt,
                                  "hashedPassword" -> PrsServer.mongoUserHashedPassword)
//                                  "permissions" -> userPermission)

    usersTable.save(adminUser)
  }

  private def insertOrganization(organizationsTable: MongoCollection, authorizedEntityXml: Node, externalServiceId: String, usersTable: MongoCollection): Unit = {
    val organization = MongoDBObject( "name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text,
                                      "website" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
                                      "url" -> PrsServer.serverName,
                                      "authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt,
                                      "externalServiceId" -> externalServiceId.toInt  )

    organizationsTable.save(organization)

    val query = MongoDBObject("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text)
    val savedOrganization = organizationsTable.findOne(query)
    val organizationId = savedOrganization.get.get("_id").toString

    insertAdminUser(usersTable, authorizedEntityXml, organizationId)
  }

  private def getAuthorizedEntity(authorizedEntityId: String): Node = {
    val authorizedEntityResult = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityId)))
    authorizedEntityResult.toXml.get
  }

  private def updateAdminUser(usersTable: MongoCollection, authorizedEntityXml: Node) = {
    val query = MongoDBObject("email" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text)

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

    val updatedAdminUser = MongoDBObject("email" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text,
                                         "first_name" -> firstName,
                                         "middle_name" -> middleName,
                                         "last_name" -> lastName)

    usersTable.update(query, updatedAdminUser, true)
  }

  private def updateOrganization(organizationsTable: MongoCollection, authorizedEntityXml: Node, usersTable: MongoCollection): Unit = {
    val query = MongoDBObject("authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text)

    val updatedOrganization = MongoDBObject("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text,
                                            "website" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
                                            "url" -> PrsServer.serverName,
                                            "authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "id").text)

    organizationsTable.update(query, updatedOrganization, true)
    updateAdminUser(usersTable, authorizedEntityXml)
  }
}
