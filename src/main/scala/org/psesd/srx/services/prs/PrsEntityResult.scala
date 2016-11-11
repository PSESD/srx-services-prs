package org.psesd.srx.services.prs

import org.json4s._
import org.psesd.srx.shared.core.{SrxResourceResult, SrxResponseFormat}
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core.exceptions.ArgumentNullException
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction.SifRequestAction
import org.psesd.srx.shared.core.sif._
import org.psesd.srx.shared.data.DatasourceResult

import scala.xml.{Elem, Node}

/** Interface for Privacy Rules Service entity operation result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class PrsEntityResult(
                       requestAction: SifRequestAction,
                       httpStatusCode: Int,
                       result: DatasourceResult,
                       queryService: (DatasourceResult) => List[PrsEntity],
                       queryRootNode: Node,
                       responseFormat: SrxResponseFormat
                     ) extends SrxResourceResult {
  if (requestAction == null) {
    throw new ArgumentNullException("request action")
  }

  statusCode = httpStatusCode

  if (result != null) {
    exceptions ++= result.exceptions
  }

  var id = 0

  def toJson: Option[JValue] = {
    requestAction match {

      case SifRequestAction.Create =>
        if(responseFormat.equals(SrxResponseFormat.Object) && statusCode.equals(SifHttpStatusCode.Created)) {
          Some(queryService(result).head.toJson)
        } else {
          Option(SifCreateResponse().addResult(getId.toString, statusCode).toXml.toJsonString.toJson)
        }

      case SifRequestAction.Delete =>
        Option(SifDeleteResponse().addResult(getId.toString, statusCode).toXml.toJsonString.toJson)

      case SifRequestAction.Query =>
        if (statusCode == SifHttpStatusCode.Ok) {
          val sb = new StringBuilder("[")
          var prefix = ""
          for (ae <- queryService(result)) {
            sb.append(prefix + ae.toJson.toJsonString)
            prefix = ","
          }
          sb.append("]")
          Some(sb.toString.toJson)
        } else {
          None
        }

      case SifRequestAction.Update =>
        if(responseFormat.equals(SrxResponseFormat.Object) && statusCode.equals(SifHttpStatusCode.Ok)) {
          Some(queryService(result).head.toJson)
        } else {
          Option(SifUpdateResponse().addResult(getId.toString, statusCode).toXml.toJsonString.toJson)
        }

      case _ =>
        None
    }
  }

  def getId: Int = {
    if (result == null) {
      id
    } else {
      result.id.getOrElse(id.toString).toInt
    }
  }

  def setId(idValue: Int) = {
    id = idValue
  }

  def toXml: Option[Node] = {

    requestAction match {

      case SifRequestAction.Create =>
        if(responseFormat.equals(SrxResponseFormat.Object) && statusCode.equals(SifHttpStatusCode.Created)) {
          Some(queryService(result).head.toXml)
        } else {
          Option(SifCreateResponse().addResult(getId.toString, statusCode).toXml)
        }

      case SifRequestAction.Delete =>
        Option(SifDeleteResponse().addResult(getId.toString, statusCode).toXml)

      case SifRequestAction.Query =>
        if (statusCode == SifHttpStatusCode.Ok) {
          var xmlRoot = queryRootNode
          for (child <- queryService(result)) {
            xmlRoot = addChildNode(xmlRoot, child.toXml)
          }
          Option(xmlRoot)
        } else {
          None
        }

      case SifRequestAction.Update =>
        if(responseFormat.equals(SrxResponseFormat.Object) && statusCode.equals(SifHttpStatusCode.Ok)) {
          Some(queryService(result).head.toXml)
        } else {
          Option(SifUpdateResponse().addResult(getId.toString, statusCode).toXml)
        }

      case _ =>
        None
    }
  }

  private def addChildNode(n: Node, c: Node): Node = n match {
    case e: Elem => e.copy(child = e.child ++ c)
  }
}
