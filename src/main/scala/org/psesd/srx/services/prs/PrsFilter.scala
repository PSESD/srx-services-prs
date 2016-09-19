package org.psesd.srx.services.prs

import org.json4s.JValue
import org.psesd.srx.shared.core.{SrxResource, SrxResourceErrorResult, SrxResourceResult}
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter}
import org.psesd.srx.shared.data.{DataRow, DatasourceResult}

import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/** Represents a Privacy Rules Service Filter set.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class PrsFilter(
              ) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    </xsl:stylesheet>
  }

}

/** Represents a PRS Filter method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class PrsFilterResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  PrsFilter.getFilterFromResult,
  <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"/>
) {
}

/** PRS Filter methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object PrsFilter extends PrsEntityService {
  def apply(): PrsFilter = new PrsFilter()

  def apply(filterXml: Node, parameters: Option[List[SifRequestParameter]]): PrsFilter = {
    if (filterXml == null) {
      throw new ArgumentNullException("filterXml parameter")
    }
    val rootElementName = filterXml.label
    if (rootElementName != "xsl:stylesheet") {
      throw new ArgumentInvalidException("root element '%s'".format(rootElementName))
    }
    new PrsFilter()
  }

  def create(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }
      throw new NotImplementedError("PRS filter CREATE method not implemented.")
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def delete(parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      throw new NotImplementedError("PRS filter DELETE method not implemented.")
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def query(parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      getFilterXsl(parameters)
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def update(resource: SrxResource, parameters: List[SifRequestParameter]): SrxResourceResult = {
    try {
      if (resource == null) {
        throw new ArgumentNullException("resource parameter")
      }
      throw new NotImplementedError("PRS filter UPDATE method not implemented.")
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  def getFilterFromResult(result: DatasourceResult): List[PrsFilter] = {
    val filters = ArrayBuffer[PrsFilter]()
    for (row <- result.rows) {
      val filter = PrsFilter()
      filters += filter
    }
    filters.toList
  }

  def getFilterXsl(parameters: List[SifRequestParameter]): SrxResourceResult = {
    val datasourceResult = new DatasourceResult(None, List[DataRow](), List[Exception]())
    new PrsFilterResult(SifRequestAction.Query, SifHttpStatusCode.Ok, datasourceResult)
  }

}
