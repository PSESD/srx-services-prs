package org.psesd.srx.services.prs

import org.psesd.srx.shared.core._
import org.psesd.srx.shared.core.config.Environment
import org.psesd.srx.shared.core.sif._
import org.psesd.srx.shared.data._

/** PRS entity service.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  * */
trait PrsEntityService extends SrxResourceService {

  private final val DatasourceClassNameKey = "DATASOURCE_CLASS_NAME"
  private final val DatasourceMaxConnectionsKey = "DATASOURCE_MAX_CONNECTIONS"
  private final val DatasourceTimeoutKey = "DATASOURCE_TIMEOUT"
  private final val DatasourceUrlKey = "DATASOURCE_URL"

  protected lazy val datasourceConfig = new DatasourceConfig(
    Environment.getProperty(DatasourceUrlKey),
    Environment.getProperty(DatasourceClassNameKey),
    Environment.getProperty(DatasourceMaxConnectionsKey).toInt,
    Environment.getProperty(DatasourceTimeoutKey).toLong
  )

  protected def getKeyIdFromRequestParameters(parameters: List[SifRequestParameter]): Option[Int] = {
    var result: Option[Int] = None
    try {
      val id = getIdFromRequestParameters(parameters)
      if (id.isDefined) {
        result = Some(id.get.toInt)
      }
    } catch {
      case e: Exception =>
        result = Some(-1)
    }
    result
  }

}
