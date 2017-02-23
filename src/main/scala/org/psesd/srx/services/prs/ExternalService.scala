package org.psesd.srx.services.prs

//import com.mongodb.ServerAddress
//import reactivemongo.bson.BSONDocument
//import com.mongodb.casbah.{MongoClient, MongoClientURI, MongoConnection}
//import com.mongodb.casbah.commons.MongoDBObject
//import akka.actor.ActorSystem
import org.json4s.JValue
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException, SrxResourceNotFoundException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter, SifRequestParameterCollection}
import org.psesd.srx.shared.data.exceptions.DatasourceDuplicateViolationException
import org.psesd.srx.shared.data.{Datasource, DatasourceResult}
//import com.github.jeroenr.bson.BsonDsl._
//import com.github.jeroenr.tepkin.MongoClient
//import akka.util.Timeout
//import com.github.jeroenr.tepkin.protocol.WriteConcern
//import com.github.jeroenr.bson.BsonDocument
//import com.github.jeroenr.bson.Implicits._
//import akka.stream.scaladsl.Source
//import akka.stream.ActorMaterializer

//import scala.collection.mutable.ListBuffer
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.duration._
//import scala.concurrent.{ExecutionContext, Future}
//import scala.runtime.Nothing$
//
//import reactivemongo.api.{DefaultDB, MongoConnection, MongoDriver}
//import reactivemongo.bson.{BSONDocumentWriter, BSONDocumentReader, Macros, document}

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/** Represents External Service.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class ExternalService(
                 val id: Int,
                 val authorizedEntityId: Int,
                 val name: Option[String],
                 val description: Option[String]
               ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <externalService>
      <id>{id.toString}</id>
      <authorizedEntityId>{authorizedEntityId.toString}</authorizedEntityId>
      {optional(name.orNull, <externalServiceName>{name.orNull}</externalServiceName>)}
      {optional(description.orNull, <externalServiceDescription>{description.orNull}</externalServiceDescription>)}
    </externalService>
  }

}

/** Represents External Service method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class ExternalServiceResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  ExternalService.getExternalServiceFromResult,
  <externalServices/>,
  responseFormat
) {
}

/** PRS External Service methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  * */
object ExternalService extends PrsEntityService {
  def apply(
             id: Int,
             authorizedEntityId: Int,
             name: Option[String],
             description: Option[String]
           ): ExternalService = new ExternalService(id, authorizedEntityId, name, description)

  def apply(requestBody: SrxRequestBody, parameters: Option[List[SifRequestParameter]]): ExternalService = {
    if (requestBody == null) {
      throw new ArgumentNullException("requestBody parameter")
    }
    apply(requestBody.getXml.orNull, parameters)
  }

  def apply(externalServiceXml: Node, parameters: Option[List[SifRequestParameter]]): ExternalService = {
    if (externalServiceXml == null) {
      throw new ArgumentNullException("externalServiceXml parameter")
    }
    val authorizedEntityParam = {if (parameters.isDefined) parameters.get.find(p => p.key.toLowerCase == "authorizedentityid") else None}
    if (authorizedEntityParam.isEmpty) {
      throw new ArgumentNullException("authorizedEntityId parameter")
    }

    val rootElementName = externalServiceXml.label
    if (rootElementName != "externalService" && rootElementName != "root") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    val id = (externalServiceXml \ "id").textOption.getOrElse("0").toInt
    val authorizedEntityId = authorizedEntityParam.get.value.toInt
    val name = (externalServiceXml \ "externalServiceName").textOption
    val description = (externalServiceXml \ "externalServiceDescription").textOption
    new ExternalService(
      id,
      authorizedEntityId,
      name,
      description
    )
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }

      val externalService = resource.asInstanceOf[ExternalService]

      val datasource = new Datasource(datasourceConfig)

      val result = datasource.create(
        "insert into srx_services_prs.external_service (" +
          "id, authorized_entity_id, name, description) values (" +
          "DEFAULT, ?, ?, ?) " +
          "RETURNING id;",
        "id",
        externalService.authorizedEntityId,
        externalService.name.orNull,
        externalService.description.orNull
      )

      datasource.close()

      if (result.success) {
//        mongoDbInsert(externalService, result)
//        mongoDBInsert(externalService, result)

        PrsServer.logSuccessMessage(
          PrsResource.ExternalServices.toString,
          SifRequestAction.Create.toString,
          result.id,
          SifRequestParameterCollection(parameters),
          Some(externalService.toXml.toXmlString)
        )
        val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
        if(responseFormat.equals(SrxResponseFormat.Object)) {
          val queryResult = executeQuery(Some(result.id.get.toInt))
          new ExternalServiceResult(
            SifRequestAction.Create,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
            queryResult,
            responseFormat
          )
        } else {
          new ExternalServiceResult(
            SifRequestAction.Create,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Create),
            result,
            responseFormat
          )
        }
      } else {
        throw result.exceptions.head
      }
    } catch {
      case dv: DatasourceDuplicateViolationException =>
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, dv)
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def delete(parameters: List[SifRequestParameter]): SrxResourceResult = {
    val id = getKeyIdFromRequestParameters(parameters)
    if (id.isEmpty || id.get == -1) {
      SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
    } else {
      try {
        val datasource = new Datasource(datasourceConfig)

        val result = datasource.execute(
          "delete from srx_services_prs.external_service where id = ?;",
          id.get
        )

        datasource.close()

        if (result.success) {
          PrsServer.logSuccessMessage(
            PrsResource.ExternalServices.toString,
            SifRequestAction.Delete.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            None
          )
          val esResult = new ExternalServiceResult(
            SifRequestAction.Delete,
            SifRequestAction.getSuccessStatusCode(SifRequestAction.Delete),
            result,
            SrxResponseFormat.getResponseFormat(parameters)
          )
          esResult.setId(id.get)
          esResult
        } else {
          throw result.exceptions.head
        }
      } catch {
        case e: Exception =>
          SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
      }
    }
  }

  def query(parameters: List[SifRequestParameter]): SrxResourceResult = {
    val id = getKeyIdFromRequestParameters(parameters)
    if (id.isDefined && id.get == -1) {
      SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
    } else {
      try {
        val result = executeQuery(id)
        if (result.success) {
          val resourceId = if (id.isEmpty) Some("all") else Some(id.get.toString)
          if (id.isDefined && result.rows.isEmpty) {
            PrsServer.logNotFoundMessage(
              PrsResource.ExternalServices.toString,
              SifRequestAction.Query.toString,
              resourceId,
              SifRequestParameterCollection(parameters),
              None
            )
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new SrxResourceNotFoundException(PrsResource.ExternalServices.toString))
          } else {
            PrsServer.logSuccessMessage(
              PrsResource.ExternalServices.toString,
              SifRequestAction.Query.toString,
              resourceId,
              SifRequestParameterCollection(parameters),
              None
            )
            new ExternalServiceResult(
              SifRequestAction.Query,
              SifHttpStatusCode.Ok,
              result,
              SrxResponseFormat.getResponseFormat(parameters)
            )
          }
        } else {
          throw result.exceptions.head
        }
      } catch {
        case e: Exception =>
          SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
      }
    }
  }

  private def executeQuery(id: Option[Int]): DatasourceResult = {
    val selectFrom = "select srx_services_prs.external_service.* from srx_services_prs.external_service "
    val datasource = new Datasource(datasourceConfig)
    val result = {
      if (id.isEmpty) {
        datasource.get(selectFrom + "order by srx_services_prs.external_service.id;")
      } else {
        datasource.get(selectFrom + "where srx_services_prs.external_service.id = ?;", id.get)
      }
    }
    datasource.close()
    result
  }

  def update(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    if (resource == null) {
      throw new ArgumentNullException("resource parameter")
    }
    try {
      var id = getKeyIdFromRequestParameters(parameters)

      val externalService = resource.asInstanceOf[ExternalService]
      if ((id.isEmpty || id.get == 0) && externalService.id > 0) {
        id = Some(externalService.id)
      }
      if (id.isEmpty || id.get == -1) {
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, new ArgumentInvalidException("id parameter"))
      } else {
        val datasource = new Datasource(datasourceConfig)

        val result = datasource.execute(
          "update srx_services_prs.external_service set " +
            "authorized_entity_id = ?, " +
            "name = ?, " +
            "description = ? " +
            "where id = ?;",
          externalService.authorizedEntityId,
          externalService.name.orNull,
          externalService.description.orNull,
          id.get
        )

        datasource.close()

        if (result.success) {
          PrsServer.logSuccessMessage(
            PrsResource.ExternalServices.toString,
            SifRequestAction.Update.toString,
            Some(id.get.toString),
            SifRequestParameterCollection(parameters),
            Some(externalService.toXml.toXmlString)
          )
          val responseFormat = SrxResponseFormat.getResponseFormat(parameters)
          var esResult: ExternalServiceResult = null
          if(responseFormat.equals(SrxResponseFormat.Object)) {
            val queryResult = executeQuery(Some(id.get))
            esResult = new ExternalServiceResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              queryResult,
              responseFormat
            )
          } else {
            esResult = new ExternalServiceResult(
              SifRequestAction.Update,
              SifRequestAction.getSuccessStatusCode(SifRequestAction.Update),
              result,
              responseFormat
            )
          }
          esResult.setId(id.get)
          esResult
        } else {
          throw result.exceptions.head
        }
      }
    } catch {
      case dv: DatasourceDuplicateViolationException =>
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, dv)
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def getExternalServiceFromResult(result: DatasourceResult): List[ExternalService] = {
    val externalServiceResult = ArrayBuffer[ExternalService]()
    for (row <- result.rows) {
      val externalService = ExternalService(
        row.getString("id").getOrElse("").toInt,
        row.getString("authorized_entity_id").getOrElse("").toInt,
        row.getString("name"),
        row.getString("description")
      )
      externalServiceResult += externalService
    }
    externalServiceResult.toList
  }

//  TEPKIN DRIVER
//    private def mongoDbInsert(externalService: ExternalService, dataSourceResult: DatasourceResult): Unit = {
//      val authorizedEntityResult = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", externalService.authorizedEntityId.toString)))
//      val authorizedEntityXml = authorizedEntityResult.toXml.get
//
//      val organization = Iterable($document("name" := (authorizedEntityXml \ "authorizedEntity" \ "name").text,
//                                   "website" := (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
//                                   "url" := (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
//                                   "authorizedEntityId" := (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt,
//                                   "externalServiceId" := dataSourceResult.id.get.toInt))
//
//      val client = MongoClient(PrsServer.mongoUri)
//      val db = client(PrsServer.mongoDbName)
//      val collection = db("organizations")
//
//      implicit val timeout: Timeout = 5.seconds
////      val executionContext = ActorSystem("tepkin-system").dispatcher
////      import client.ec
//      collection.insertFromSource(Source{List(organization)})
//
//      client.shutdown()
//    }



//  REACTIVEMONGO
//  private def mongoDBInsert(externalService: ExternalService, dataSourceResult: DatasourceResult): Unit = {
//    import ExecutionContext.Implicits.global
//
//    val driver = MongoDriver()
//    val parsedUri = MongoConnection.parseURI(PrsServer.mongoUri)
//    val connection = parsedUri.map(driver.connection(_))
//
//    val futureConnection = Future.fromTry(connection)
//    def database: Future[DefaultDB] = futureConnection.flatMap(_.database(PrsServer.mongoDbName))
//    def organizationsCollection = database.map(_.collection("organizations"))
//
////    implicit def organizationWriter: BSONDocumentWriter[Person] = Macros.writer[Person]
////    // or provide a custom one
//
//    val authorizedEntityResult = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", externalService.authorizedEntityId.toString)))
//    val authorizedEntityXml = authorizedEntityResult.toXml.get
//
//    val organization = BSONDocument("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text,
//                                    "website" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
//                                    "url" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
//                                    "authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt,
//                                    "externalServiceId" -> dataSourceResult.id.get.toInt)
//
//
//
//    def createOrganization(organization: BSONDocument): Unit = {
//      organizationsCollection(organization)
//    }
//
//
//
//    case class Organization(firstName: String, lastName: String, age: Int)
//
//
//
//    driver.close()
//  }


// CASBAH DRIVER
//  private def mongoDBInsert(externalService: ExternalService, dataSourceResult: DatasourceResult): Unit = {
//    val mongoClientURI = MongoClientURI(PrsServer.mongoUri)
//    val mongoClient = MongoClient(mongoClientURI)
//
//    val mongoDb = mongoClient(PrsServer.mongoDbName)
//    val organizationsTable = mongoDb("organizations")
//
//    val authorizedEntityResult = AuthorizedEntity.query(List[SifRequestParameter](SifRequestParameter("id", externalService.authorizedEntityId.toString)))
//    val authorizedEntityXml = authorizedEntityResult.toXml.get
//
//    val organization = MongoDBObject("name" -> (authorizedEntityXml \ "authorizedEntity" \ "name").text,
//                                     "website" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
//                                     "url" -> (authorizedEntityXml \ "authorizedEntity" \ "mainContact" \ "webAddress").text,
//                                     "authorizedEntityId" -> (authorizedEntityXml \ "authorizedEntity" \ "id").text.toInt,
//                                     "externalServiceId" -> dataSourceResult.id.get.toInt)
//
////    try {
//      val res = organizationsTable.save(organization)
////    } catch {
////      case e => Exception
////      throw new e
////    }
//
////    mongoClient.requestStart() // lock the connection in
////    coll.insert(obj) // attempt the insert
////    getLastError.throwOnError() // This tells the getLastError command to throw an exception in case of an error
////    mongo.requestDone()
//
//    mongoClient.close()
//  }
}
