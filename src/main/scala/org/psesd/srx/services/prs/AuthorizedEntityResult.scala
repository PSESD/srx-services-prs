package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.SrxResourceResult
import org.psesd.srx.shared.core.exceptions.ArgumentNullException
import org.psesd.srx.shared.core.sif.SifRequestAction.SifRequestAction
import org.psesd.srx.shared.core.sif._
import org.psesd.srx.shared.data.DatasourceResult

import scala.xml.Node

/** PRS authorized entity operation response.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  * */
class AuthorizedEntityResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult) extends SrxResourceResult {
  if (requestAction == null) {
    throw new ArgumentNullException("request action")
  }

  statusCode = httpStatusCode

  if (result != null) {
    exceptions ++= result.exceptions
  }

  var id = 0

  def getId: Int = {
    if(result == null) {
      id
    } else {
      result.id.getOrElse(id.toString).toInt
    }
  }

  def setId(authorizedEntityId: Int) = {
    id = authorizedEntityId
  }

  def toXml: Option[Node] = {

    requestAction match {

      case SifRequestAction.Create =>
        Option(SifCreateResponse().addResult(getId.toString, statusCode).toXml)

      case SifRequestAction.Delete =>
        Option(SifDeleteResponse().addResult(getId.toString, statusCode).toXml)

      case SifRequestAction.Query =>
        if (statusCode == SifHttpStatusCode.Ok) {
          Option(<authorizedEntities>{AuthorizedEntityService.getAuthorizedEntitiesFromResult(result).map(ae => ae.toXml)}</authorizedEntities>)
        } else {
          None
        }

      case SifRequestAction.Update =>
        Option(SifUpdateResponse().addResult(getId.toString, statusCode).toXml)

      case _ =>
        None
    }
  }
}

object AuthorizedEntityResult {
  def apply(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult): AuthorizedEntityResult = new AuthorizedEntityResult(requestAction, httpStatusCode, result)
}
