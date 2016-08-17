package org.psesd.srx.services.prs

import org.http4s._
import org.http4s.dsl._
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.sif._
import org.psesd.srx.shared.core._
import org.psesd.srx.shared.data.DatasourceConfig

import scala.concurrent.ExecutionContext

/** SRX Privacy Rules Service server.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  * */
object PrsServer extends SrxServer {

  private final val ServerUrlKey = "SERVER_URL"

  private final val DatasourceClassNameKey = "DATASOURCE_CLASS_NAME"
  private final val DatasourceMaxConnectionsKey = "DATASOURCE_MAX_CONNECTIONS"
  private final val DatasourceTimeoutKey = "DATASOURCE_TIMEOUT"
  private final val DatasourceUrlKey = "DATASOURCE_URL"

  private val authorizedEntitiesResource = PrsResource.AuthorizedEntities.toString

  private lazy val datasourceConfig = new DatasourceConfig(
    Environment.getProperty(DatasourceUrlKey),
    Environment.getProperty(DatasourceClassNameKey),
    Environment.getProperty(DatasourceMaxConnectionsKey).toInt,
    Environment.getProperty(DatasourceTimeoutKey).toLong
  )

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

  override def serviceRouter(implicit executionContext: ExecutionContext) = HttpService {

    case req@GET -> Root =>
      Ok()

    case _ -> Root =>
      NotImplemented()

    case req@GET -> Root / _ if services(req, CoreResource.Ping.toString) =>
      Ok(true.toString)

    case req@GET -> Root / _ if services(req, CoreResource.Info.toString) =>
      respondWithInfo(getDefaultSrxResponse(req))

    case req@GET -> Root / _ if services(req, authorizedEntitiesResource) =>
      MethodNotAllowed()

    case req@GET -> Root / `authorizedEntitiesResource` / _ =>
      executeRequest(req, authorizedEntitiesResource, AuthorizedEntityService)

    case req@POST -> Root / _ if services(req, authorizedEntitiesResource) =>
      executeRequest(req, authorizedEntitiesResource, AuthorizedEntityService, AuthorizedEntity.apply)

    case req@PUT -> Root / _ if services(req, authorizedEntitiesResource) =>
      MethodNotAllowed()

    case req@PUT -> Root / `authorizedEntitiesResource` / _ =>
      MethodNotAllowed()

    case req@DELETE -> Root / _ if services(req, authorizedEntitiesResource) =>
      MethodNotAllowed()

    case req@DELETE -> Root / `authorizedEntitiesResource` / _ =>
      MethodNotAllowed()

    case _ =>
      NotFound()

  }

}
