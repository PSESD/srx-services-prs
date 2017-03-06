package org.psesd.srx.services.prs

import org.bson.BsonValue
import org.mongodb.scala.bson.{BsonInt32, BsonValue}
import org.mongodb.scala.{Completed, Document, MongoClient, MongoCollection, MongoDatabase, Observable, Observer, Subscription}
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.result.{DeleteResult, UpdateResult}
import org.psesd.srx.shared.core.sif.{SifRequestParameter}

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

  private def deleteAdminUser(authorizedEntityId: String): Unit = {
    val collection = retrieveCollection("users")

    val authorizedEntityResult = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", authorizedEntityId)))
    val authorizedEntityXml = authorizedEntityResult.toXml.get

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
    val collection = retrieveCollection("organizations")
    val observable: Observable[DeleteResult] = collection.deleteOne(equal("authorizedEntityId", BsonInt32(authorizedEntityId.toInt)))

    observable.subscribe(new Observer[DeleteResult] {
      override def onSubscribe(subscription: Subscription): Unit = subscription.request(1)
      override def onNext(result: DeleteResult): Unit = deleteAdminUser(authorizedEntityId)
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
    val organization = SslOrganization(authorizedEntityId, BsonInt32(externalServiceId.toInt))
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
