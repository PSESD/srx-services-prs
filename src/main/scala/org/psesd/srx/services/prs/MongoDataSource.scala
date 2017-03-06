package org.psesd.srx.services.prs

import com.mongodb.ServerAddress
import org.bson.BsonValue
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.{BsonDateTime, BsonValue}
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase, Observable, Observer, Subscription, result}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.DeleteResult
import org.psesd.srx.shared.core.sif.{SifRequestParameter, SifTimestamp}
import org.mongodb.scala.model.Updates._

import scala.xml.Node
import scalaz.std.set

/** MongoDB Connection for Student Success Link Data
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
class MongoDataSource extends SslEntity {

  private var mongoClient: MongoClient = _
  private var mongoDb: MongoDatabase = _

  def close: Unit = {
    mongoClient.close()
  }

  private def connectToDatabase: MongoDatabase = {
    if (mongoClient == null) mongoClient = MongoClient(PrsServer.mongoUrl)
    if (mongoDb == null) mongoDb = mongoClient.getDatabase(PrsServer.mongoDbName)

    mongoDb
  }

  def retrieveCollection(name: String): MongoCollection[Document] = {
    connectToDatabase
    mongoDb.getCollection(name)
  }

  private def deleteAdminUser(authorizedEntityXml: Node): Unit = {
    val collection = retrieveCollection("users")

    val email = (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text
    val observable: Observable[DeleteResult] = collection.deleteOne(equal("email", email))

    observable.subscribe(new Observer[DeleteResult] {
      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
      override def onNext(result: DeleteResult): Unit = println("Deleted Admin User")
      override def onError(e: Throwable): Unit = println(e.toString)
      override def onComplete(): Unit = close
    })
  }

  def deleteOrganization(authorizedEntityId: String): Unit = {
    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)

    val collection = retrieveCollection("organizations")
    val observable: Observable[DeleteResult] = collection.deleteOne(equal("name", (authorizedEntityXml \ "authorizedEntity" \ "name").text))

    observable.subscribe(new Observer[DeleteResult] {
      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
      override def onNext(result: DeleteResult): Unit = deleteAdminUser(authorizedEntityXml)
      override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
      override def onComplete(): Unit = println("Deleted Organization")
    })
  }

  def insertAdminUser(authorizedEntityId: String, organizationId: BsonValue): Unit = {
    val adminUser = SslUser(authorizedEntityId, organizationId)
    val collection = retrieveCollection("users")

    val observable: Observable[Completed] = collection.insertOne(adminUser)

    observable.subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = println("Inserted Admin User")
      override def onError(e: Throwable): Unit = println(e.toString)
      override def onComplete(): Unit = close
    })
  }

  def insertOrganization(authorizedEntityId: String, externalServiceId: String): Unit = {
    val organization = SslOrganization(authorizedEntityId, externalServiceId)
    val collection = retrieveCollection("organizations")

    val observable: Observable[Completed] = collection.insertOne(organization)

    observable.subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = println("Inserted Organization")
      override def onError(e: Throwable): Unit = println(e.toString)
      override def onComplete(): Unit = {
        val authorizedEntityName = organization.get("name").get
        queryOrganization(collection, authorizedEntityId, authorizedEntityName)
      }
    })
  }

  def queryOrganization(collection: MongoCollection[Document], authorizedEntityId: String, authorizedEntityName: BsonValue): Unit = {
    val observable: Observable[Document] = collection.find(equal("name", authorizedEntityName))

    observable.subscribe(new Observer[Document] {
      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
      override def onNext(result: Document): Unit = {
        val organizationId = result.get("_id").get
        insertAdminUser(authorizedEntityId, organizationId)
      }
      override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
      override def onComplete(): Unit = println("Queried")
    })
  }

//
//  private def updateAdminUser(usersTable: MongoCollection, authorizedEntityXml: Node) = {
//    val query = MongoDBObject("email" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text)
//
//    val mainContactName = (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "name").text
//    val mainContactNameArr = mainContactName.split(" ")
//
//    val firstName = mainContactNameArr(0)
//    var middleName = ""
//    var lastName = ""
//
//    if (mainContactNameArr.length == 2) {
//      lastName = mainContactNameArr(1)
//    } else if (mainContactNameArr.length >= 3) {
//      middleName = mainContactNameArr(1)
//      lastName = mainContactNameArr(mainContactNameArr.length - 1)
//    }
//
//    val updatedAdminUser = MongoDBObject("email" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "email").text,
//                                         "first_name" -> firstName,
//                                         "middle_name" -> middleName,
//                                         "last_name" -> lastName)
//
//    usersTable.update(query, updatedAdminUser, true)
//  }

//  def updateOrganization(authorizedEntityId: String): Unit = {
//    val authorizedEntityXml = getAuthorizedEntity(authorizedEntityId)
//    val organization = SslOrganization(authorizedEntityId)
//
//    val collection = retrieveCollection("organizations")
//    val query = equal("name", (authorizedEntityXml \ "authorizedEntity" \ "name").text)
//    val timestamp = bsonTimeStamp
//
//
////    val observable: Observable[Document] = collection.find(equal("name", (authorizedEntityXml \ "authorizedEntity" \ "name").text))
////
////    observable.subscribe(new Observer[Document] {
////      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
////      override def onNext(result: Document): Unit = {
////        val organizationId = result.get("_id").get
////        val r = result
////        val t = ""
////      }
////      override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
////      override def onComplete(): Unit = println("Queried")
////    })
//
//
//    val observable: Observable[result.UpdateResult] = collection.replaceOne(query, organization)
//
//    observable.subscribe(new Observer[result.UpdateResult] {
//      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
//      override def onNext(updatedResult: result.UpdateResult): Unit = {
//        val organizationId = updatedResult
//        val test =""
//      }
//      override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
//      override def onComplete(): Unit = println("Queried")
//    })
//


//    combine(set("quantity", 11),
//      set("total", 30.40),

//  }
}
