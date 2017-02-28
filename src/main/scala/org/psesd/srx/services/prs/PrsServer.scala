package org.psesd.srx.services.prs

import org.http4s._
import org.http4s.dsl._
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.sif._
import org.psesd.srx.shared.data.DatasourceConfig

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext

/** SRX Privacy Rules Service server.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  **/
object PrsServer extends SrxServer {

  private final val ServerUrlKey = "SERVER_URL"
  private final val ServerName = "SERVER_NAME"

  private final val DatasourceClassNameKey = "DATASOURCE_CLASS_NAME"
  private final val DatasourceMaxConnectionsKey = "DATASOURCE_MAX_CONNECTIONS"
  private final val DatasourceTimeoutKey = "DATASOURCE_TIMEOUT"
  private final val DatasourceUrlKey = "DATASOURCE_URL"
  private final val MongoDbUri = "MONGODB_URI"

  private final val MongoDbHost = "MONGODB_HOST"
  private final val MongoDbPort = "MONGODB_PORT"
  private final val MongoDbName = "MONGODB_NAME"
  private final val MongoDbPassword = "MONGODB_PASSWORD"

  private final val AuthorizedEntityIdParam = "authorizedEntityId"
  private final val DataSetIdParam = "dataSetId"
  private final val DistrictIdParam = "districtId"
  private final val DistrictServiceIdParam = "districtServiceId"

  private lazy val datasourceConfig = new DatasourceConfig(
    Environment.getProperty(DatasourceUrlKey),
    Environment.getProperty(DatasourceClassNameKey),
    Environment.getProperty(DatasourceMaxConnectionsKey).toInt,
    Environment.getProperty(DatasourceTimeoutKey).toLong
  )

  lazy val mongoDbUri = Environment.getProperty(MongoDbUri)

  lazy val serverName = Environment.getProperty(ServerName)
  lazy val mongoDbHost = Environment.getProperty(MongoDbHost)
  lazy val mongoDbPort = Environment.getProperty(MongoDbPort)
  lazy val mongoDbName = Environment.getProperty(MongoDbName)
  lazy val mongoDbPassword = Environment.getProperty(MongoDbPassword)

  val sifProvider: SifProvider = new SifProvider(
    SifProviderUrl(Environment.getProperty(ServerUrlKey)),
    SifProviderSessionToken(Environment.getProperty(Environment.SrxSessionTokenKey)),
    SifProviderSharedSecret(Environment.getProperty(Environment.SrxSharedSecretKey)),
    SifAuthenticationMethod.SifHmacSha256
  )

  val srxService: SrxService = new SrxService(
    new SrxServiceComponent(Build.name, Build.version + "." + Build.buildNumber),
    List[SrxServiceComponent](
      new SrxServiceComponent("java", Build.javaVersion),
      new SrxServiceComponent("scala", Build.scalaVersion),
      new SrxServiceComponent("sbt", Build.sbtVersion)
    )
  )

  private val authorizedEntitiesResource = PrsResource.AuthorizedEntities.toString
  private val dataObjectsResource = PrsResource.DataObjects.toString
  private val dataSetsResource = PrsResource.DataSets.toString
  private val districtsResource = PrsResource.Districts.toString
  private val externalServicesResource = PrsResource.ExternalServices.toString
  private val filtersResource = PrsResource.Filters.toString
  private val personnelResource = PrsResource.Personnel.toString
  private val studentsResource = PrsResource.Students.toString

  override def serviceRouter(implicit executionContext: ExecutionContext) = HttpService {

    case req@GET -> Root =>
      Ok()

    case _ -> Root =>
      NotImplemented()

    case req@GET -> Root / _ if services(req, SrxResourceType.Ping.toString) =>
      Ok(true.toString)

    case req@GET -> Root / _ if services(req, SrxResourceType.Info.toString) =>
      respondWithInfo(getDefaultSrxResponse(req))


    /* AUTHORIZED ENTITY */
    case req@GET -> Root / _ if services(req, authorizedEntitiesResource) =>
      executeRequest(req, None, authorizedEntitiesResource, AuthorizedEntity)

    case req@GET -> Root / `authorizedEntitiesResource` / _ =>
      executeRequest(req, None, authorizedEntitiesResource, AuthorizedEntity)

    case req@POST -> Root / _ if services(req, authorizedEntitiesResource) =>
      executeRequest(req, None, authorizedEntitiesResource, AuthorizedEntity, AuthorizedEntity.apply)

    case req@PUT -> Root / _ if services(req, authorizedEntitiesResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `authorizedEntitiesResource` / _ =>
      executeRequest(req, None, authorizedEntitiesResource, AuthorizedEntity, AuthorizedEntity.apply)

    case req@DELETE -> Root / _ if services(req, authorizedEntitiesResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `authorizedEntitiesResource` / _ =>
      executeRequest(req, None, authorizedEntitiesResource, AuthorizedEntity)


    /* DATA OBJECT */
    case req@GET -> Root / `dataSetsResource` / IntVar(dataSetId) / _ if services(req, dataSetsResource, dataSetId.toString, dataObjectsResource) =>
      executeRequest(req, addRouteParams(DataSetIdParam, dataSetId.toString), dataObjectsResource, DataObject)

    case req@GET -> Root / `dataSetsResource` / IntVar(dataSetId) / `dataObjectsResource` / _ =>
      executeRequest(req, addRouteParams(DataSetIdParam, dataSetId.toString), dataObjectsResource, DataObject)

    case req@POST -> Root / `dataSetsResource` / IntVar(dataSetId) / _ if services(req, dataSetsResource, dataSetId.toString, dataObjectsResource) =>
      executeRequest(req, addRouteParams(DataSetIdParam, dataSetId.toString), dataObjectsResource, DataObject, DataObject.apply)

    case req@PUT -> Root / `dataSetsResource` / IntVar(dataSetId) / _ if services(req, dataSetsResource, dataSetId.toString, dataObjectsResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `dataSetsResource` / IntVar(dataSetId) / `dataObjectsResource` / _ =>
      executeRequest(req, addRouteParams(DataSetIdParam, dataSetId.toString), dataObjectsResource, DataObject, DataObject.apply)

    case req@DELETE -> Root / `dataSetsResource` / IntVar(dataSetId) / _ if services(req, dataSetsResource, dataSetId.toString, dataObjectsResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `dataSetsResource` / IntVar(dataSetId) / `dataObjectsResource` / _ =>
      executeRequest(req, addRouteParams(DataSetIdParam, dataSetId.toString), dataObjectsResource, DataObject)


    /* DATA SET */
    case req@GET -> Root / _ if services(req, dataSetsResource) =>
      executeRequest(req, None, dataSetsResource, DataSet)

    case req@GET -> Root / `dataSetsResource` / _ =>
      executeRequest(req, None, dataSetsResource, DataSet)

    case req@POST -> Root / _ if services(req, dataSetsResource) =>
      executeRequest(req, None, dataSetsResource, DataSet, DataSet.apply)

    case req@PUT -> Root / _ if services(req, dataSetsResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `dataSetsResource` / _ =>
      executeRequest(req, None, dataSetsResource, DataSet, DataSet.apply)

    case req@DELETE -> Root / _ if services(req, dataSetsResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `dataSetsResource` / _ =>
      executeRequest(req, None, dataSetsResource, DataSet)


    /* DISTRICT */
    case req@GET -> Root / _ if services(req, districtsResource) =>
      executeRequest(req, None, districtsResource, District)

    case req@GET -> Root / `districtsResource` / _ =>
      executeRequest(req, None, districtsResource, District)

    case req@POST -> Root / _ if services(req, districtsResource) =>
      executeRequest(req, None, districtsResource, District, District.apply)

    case req@PUT -> Root / _ if services(req, districtsResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `districtsResource` / _ =>
      executeRequest(req, None, districtsResource, District, District.apply)

    case req@DELETE -> Root / _ if services(req, districtsResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `districtsResource` / _ =>
      executeRequest(req, None, districtsResource, District)


    /* DISTRICT SERVICE */
    case req@GET -> Root / `districtsResource` / IntVar(districtId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource) =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString), externalServicesResource, DistrictService)

    case req@GET -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString), externalServicesResource, DistrictService)

    case req@POST -> Root / `districtsResource` / IntVar(districtId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource) =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString), externalServicesResource, DistrictService, DistrictService.apply)

    case req@PUT -> Root / `districtsResource` / IntVar(districtId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString), externalServicesResource, DistrictService, DistrictService.apply)

    case req@DELETE -> Root / `districtsResource` / IntVar(districtId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString), externalServicesResource, DistrictService)


    /* DISTRICT SERVICE PERSONNEL */
    case req@GET -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource, districtServiceId.toString, personnelResource) =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), personnelResource, DistrictServicePersonnel)

    case req@GET -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / `personnelResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), personnelResource, DistrictServicePersonnel)

    case req@POST -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource, districtServiceId.toString, personnelResource) =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), personnelResource, DistrictServicePersonnel, DistrictServicePersonnel.apply)

    case req@PUT -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource, districtServiceId.toString, personnelResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / `personnelResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), personnelResource, DistrictServicePersonnel, DistrictServicePersonnel.apply)

    case req@DELETE -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource, districtServiceId.toString, personnelResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / `personnelResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), personnelResource, DistrictServicePersonnel)


    /* DISTRICT SERVICE STUDENT */
    case req@GET -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource, districtServiceId.toString, studentsResource) =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), studentsResource, Student)

    case req@GET -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / `studentsResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), studentsResource, Student)

    case req@POST -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource, districtServiceId.toString, studentsResource) =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), studentsResource, Student, Student.apply)

    case req@PUT -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource, districtServiceId.toString, studentsResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / `studentsResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), studentsResource, Student, Student.apply)

    case req@DELETE -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / _ if services(req, districtsResource, districtId.toString, externalServicesResource, districtServiceId.toString, studentsResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `districtsResource` / IntVar(districtId) / `externalServicesResource` / IntVar(districtServiceId) / `studentsResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString, DistrictServiceIdParam, districtServiceId.toString), studentsResource, Student)


    /* DISTRICT STUDENT */
    case req@GET -> Root / `districtsResource` / IntVar(districtId) / _ if services(req, districtsResource, districtId.toString, studentsResource) =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString), studentsResource, Student)

    case req@GET -> Root / `districtsResource` / IntVar(districtId) / `studentsResource` / _ =>
      executeRequest(req, addRouteParams(DistrictIdParam, districtId.toString), studentsResource, Student)

    case req@POST -> Root / `districtsResource` / IntVar(districtId) / _ if services(req, districtsResource, districtId.toString, studentsResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `districtsResource` / IntVar(districtId) / _ if services(req, districtsResource, districtId.toString, studentsResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `districtsResource` / IntVar(districtId) / `studentsResource` / _ =>
      MethodNotAllowed()

    case req@DELETE -> Root / `districtsResource` / IntVar(districtId) / _ if services(req, districtsResource, districtId.toString, studentsResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `districtsResource` / IntVar(districtId) / `studentsResource` / _ =>
      MethodNotAllowed()


    /* EXTERNAL SERVICE */
    case req@GET -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / _ if services(req, authorizedEntitiesResource, authorizedEntityId.toString, externalServicesResource) =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), externalServicesResource, ExternalService)

    case req@GET -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / `externalServicesResource` / _ =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), externalServicesResource, ExternalService)

    case req@POST -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / _ if services(req, authorizedEntitiesResource, authorizedEntityId.toString, externalServicesResource) =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), externalServicesResource, ExternalService, ExternalService.apply)

    case req@PUT -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / _ if services(req, authorizedEntitiesResource, authorizedEntityId.toString, externalServicesResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / `externalServicesResource` / _ =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), externalServicesResource, ExternalService, ExternalService.apply)

    case req@DELETE -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / _ if services(req, authorizedEntitiesResource, authorizedEntityId.toString, externalServicesResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / `externalServicesResource` / _ =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), externalServicesResource, ExternalService)


    /* FILTERS */
    case req@GET -> Root / _ if services(req, filtersResource) =>
      executeRequest(req, addPrsFilterParameters(req), filtersResource, PrsFilter)

    case req@GET -> Root / `filtersResource` / _ =>
      executeRequest(req, addPrsFilterParameters(req), filtersResource, PrsFilter)

    case req@POST -> Root / _ if services(req, filtersResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / _ if services(req, filtersResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `filtersResource` / _ =>
      MethodNotAllowed()

    case req@DELETE -> Root / _ if services(req, filtersResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `filtersResource` / _ =>
      MethodNotAllowed()


    /* PERSONNEL */
    case req@GET -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / _ if services(req, authorizedEntitiesResource, authorizedEntityId.toString, personnelResource) =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), personnelResource, Personnel)

    case req@GET -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / `personnelResource` / _ =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), personnelResource, Personnel)

    case req@POST -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / _ if services(req, authorizedEntitiesResource, authorizedEntityId.toString, personnelResource) =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), personnelResource, Personnel, Personnel.apply)

    case req@PUT -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / _ if services(req, authorizedEntitiesResource, authorizedEntityId.toString, personnelResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / `personnelResource` / _ =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), personnelResource, Personnel, Personnel.apply)

    case req@DELETE -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / _ if services(req, authorizedEntitiesResource, authorizedEntityId.toString, personnelResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `authorizedEntitiesResource` / IntVar(authorizedEntityId) / `personnelResource` / _ =>
      executeRequest(req, addRouteParams(AuthorizedEntityIdParam, authorizedEntityId.toString), personnelResource, Personnel)


    case _ =>
      NotFound()

  }

  def addPrsFilterParameters(req: Request): Option[List[SifRequestParameter]] = {
    val params = ArrayBuffer[SifRequestParameter]()
    for (h <- req.headers) {
      val headerName = h.name.value.toLowerCase
      if(headerName == PrsFilter.AuthorizedEntityIdParameter.toLowerCase) params += SifRequestParameter(PrsFilter.AuthorizedEntityIdParameter, h.value)
      if(headerName == PrsFilter.DistrictStudentIdParameter.toLowerCase) params += SifRequestParameter(PrsFilter.DistrictStudentIdParameter, h.value)
      if(headerName == PrsFilter.ExternalServiceIdParameter.toLowerCase) params += SifRequestParameter(PrsFilter.ExternalServiceIdParameter, h.value)
      if(headerName == PrsFilter.ObjectTypeParameter.toLowerCase) params += SifRequestParameter(PrsFilter.ObjectTypeParameter, h.value)
      if(headerName == PrsFilter.PersonnelIdParameter.toLowerCase) params += SifRequestParameter(PrsFilter.PersonnelIdParameter, h.value)
      if(headerName == SifHeader.Accept.toString.toLowerCase) params += SifRequestParameter(SifHeader.Accept.toString, h.value)
    }
    Some(params.toList)
  }

}
