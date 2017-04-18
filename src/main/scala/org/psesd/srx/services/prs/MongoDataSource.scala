package org.psesd.srx.services.prs

import java.util.concurrent.TimeUnit

import scala.concurrent.ExecutionContext.Implicits.global
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.bson.{BsonInt32, BsonValue}
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase, Observable, Observer, Subscription}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.{FindOneAndUpdateOptions, UpdateOptions, Updates}
import org.mongodb.scala.result.{DeleteResult, UpdateResult}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.xml.Node

/** MongoDB Connection for Student Success Link Data
  *
  * @version 1.0
  * @since 1.0
  * @author Margarett Ly (iTrellis, LLC)
  */
class MongoDataSource {

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

  def deleteOrganization(authorizedEntityXml: Node): Unit = {
    val authorizedEntityId = (authorizedEntityXml \ "authorizedEntity" \ "id").text
    val collection = retrieveCollection("organizations")





    val observable: Observable[DeleteResult] = collection.deleteOne(equal("authorizedEntityId", BsonInt32(authorizedEntityId.toInt)))

    observable.subscribe(new Observer[DeleteResult] {
      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
      override def onNext(result: DeleteResult): Unit = deleteAdminUser(authorizedEntityXml)
      override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
      override def onComplete(): Unit = println("Deleted Organization")
    })
  }

  def insertAdminUser(authorizedEntityId: String, organizationId: BsonValue): Unit = {
    val adminUser = new SslUser(authorizedEntityId, organizationId)
    val collection = retrieveCollection("users")

    val filter: Bson = equal("email", adminUser.email)

    val userQuery = collection.find(filter).first()

    val user = Await.result(userQuery.toFuture(), Duration(10, TimeUnit.SECONDS))

    val onCompleteInsert = new Observer[Completed] {
      override def onNext(result: Completed): Unit = println("Inserted Admin User. Result: " + result.toString())
      override def onError(e: Throwable): Unit = println("ERROR: " + e.toString)
      override def onComplete(): Unit = {
        close
      }
    }

    val onCompleteUpdate = new Observer[UpdateResult] {
      override def onNext(result: UpdateResult): Unit = println("Updated Admin User. Result: " + result.toString)
      override def onError(e: Throwable): Unit = println("ERROR: " + e.toString)
      override def onComplete(): Unit = {
        close
      }
    }
    if (user.isEmpty) {
      collection.insertOne(adminUser.toDocument).subscribe(onCompleteInsert)
    } else {
      collection.updateOne(filter, Updates.addToSet("permissions", adminUser.userPermissions.head)).subscribe(onCompleteUpdate)
    }
  }

  def insertOrganization(authorizedEntityId: String, externalServiceId: String): Unit = {
    val organization = SslOrganization(authorizedEntityId, BsonInt32(externalServiceId.toInt))
    val collection = retrieveCollection("organizations")

    val observable: Observable[Completed] = collection.insertOne(organization)

    observable.subscribe(new Observer[Completed] {
      override def onNext(result: Completed): Unit = println("Inserted Organization")
      override def onError(e: Throwable): Unit = println(e.toString)
      override def onComplete(): Unit = queryOrganization(collection, authorizedEntityId)
    })
  }

  def queryOrganization(collection: MongoCollection[Document], authorizedEntityId: String): Unit = {
    val observable: Observable[Document] = collection.find(equal("authorizedEntityId", authorizedEntityId.toInt))

    observable.subscribe(new Observer[Document] {
      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
      override def onNext(result: Document): Unit = {
        val organizationId = result.get("_id").get
        insertAdminUser(authorizedEntityId, organizationId)
      }
      override def onError(e: Throwable): Unit = println(e.toString)
      override def onComplete(): Unit = println("Queried Organization")
    })
  }

  private def updateAdminUser(authorizedEntityId: String, organizationId: BsonValue, email: String) = {
    val collection = retrieveCollection("users")
    val query = equal("email", email)

    val queryObservable: Observable[Document] = collection.find(query)

    queryObservable.subscribe(new Observer[Document] {
      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
      override def onNext(result: Document): Unit = {
        val createdAt = result.get("created_at").get
        val adminUser = SslUser(authorizedEntityId, organizationId, createdAt)

        val updateObservable: Observable[UpdateResult] = collection.replaceOne(query, adminUser)

        updateObservable.subscribe(new Observer[UpdateResult] {
          override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
          override def onNext(updatedResult: UpdateResult): Unit = println("Updating User")
          override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
          override def onComplete(): Unit = close
        })
      }
      override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
      override def onComplete(): Unit = println("Queried User")
    })
  }

  def updateOrganization(authorizedEntityId: String, email: String): Unit = {
    val collection = retrieveCollection("organizations")
    val query = equal("authorizedEntityId", BsonInt32(authorizedEntityId.toInt))

    val queryObservable: Observable[Document] = collection.find(query)

    queryObservable.subscribe(new Observer[Document] {
      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
      override def onNext(result: Document): Unit = {
        val organizationId = result.get("_id").get
        val externalServiceId = result.get("externalServiceId").get
        val createdAt = result.get("created_at").get
        val organization = SslOrganization(authorizedEntityId, externalServiceId, createdAt)

        val updateObservable: Observable[UpdateResult] = collection.replaceOne(query, organization)

        updateObservable.subscribe(new Observer[UpdateResult] {
          override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
          override def onNext(updatedResult: UpdateResult): Unit = println("Updating Organization")
          override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
          override def onComplete(): Unit = updateAdminUser(authorizedEntityId, organizationId, email)
        })
      }
      override def onError(e: Throwable): Unit = println("Failed" + e.getMessage)
      override def onComplete(): Unit = println("Queried Organization")
    })
  }
}
