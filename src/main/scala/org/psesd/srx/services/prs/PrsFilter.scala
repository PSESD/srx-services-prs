package org.psesd.srx.services.prs

import org.json4s.JValue
import org.psesd.srx.shared.core.exceptions.{ArgumentInvalidException, ArgumentNullException}
import org.psesd.srx.shared.core.extensions.TypeExtensions._
import org.psesd.srx.shared.core.sif.SifRequestAction._
import org.psesd.srx.shared.core.sif.{SifHttpStatusCode, SifRequestAction, SifRequestParameter}
import org.psesd.srx.shared.core.SrxResponseFormat.SrxResponseFormat
import org.psesd.srx.shared.core.{SrxResource, SrxResourceErrorResult, SrxResourceResult, SrxResponseFormat}
import org.psesd.srx.shared.data.{Datasource, DatasourceResult}

import scala.collection.concurrent.TrieMap
import scala.collection.immutable.ListMap
import scala.collection.mutable.ArrayBuffer
import scala.xml.Node

/** Represents a Privacy Rules Service Filter set.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class PrsFilter(node: Node) extends SrxResource with PrsEntity {

  def toJson: JValue = {
    toXml.toJsonStringNoRoot.toJson
  }

  def toXml: Node = {
    node
  }

}

/** Represents a PRS Filter method result.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
class PrsFilterResult(requestAction: SifRequestAction, httpStatusCode: Int, result: DatasourceResult, responseFormat: SrxResponseFormat) extends PrsEntityResult(
  requestAction,
  httpStatusCode,
  result,
  PrsFilter.getFilterFromResult,
  <xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="xml" encoding="UTF-8" standalone="yes"/>
  </xsl:stylesheet>,
  responseFormat
) {
}

/** PRS Filter methods.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object PrsFilter extends PrsEntityService {
  final val AuthorizedEntityIdParameter = "authorizedEntityId"
  final val DistrictStudentIdParameter = "districtStudentId"
  final val ExternalServiceIdParameter = "externalServiceId"
  final val ObjectTypeParameter = "objectType"
  final val PersonnelIdParameter = "personnelId"
  final val ZoneIdParameter = "zoneId"

  def apply(node: Node): PrsFilter = new PrsFilter(node)

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
      getFilterSet(parameters)
    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.InternalServerError, e)
    }
  }

  private def getFilterSet(parameters: List[SifRequestParameter]): SrxResourceResult = {
    var authorizedEntityId: Int = 0
    var districtStudentId: String = null
    var externalServiceId: Int = 0
    var objectType: String = null
    var personnelId: String = null
    var zoneId: String = null

    try {
      if (parameters == null) {
        throw new ArgumentNullException("parameters collection")
      }
      val authorizedEntityIdParam = parameters.find(p => p.key.toLowerCase == AuthorizedEntityIdParameter.toLowerCase)
      authorizedEntityId = if (authorizedEntityIdParam.isDefined && !authorizedEntityIdParam.get.value.isNullOrEmpty) authorizedEntityIdParam.get.value.toInt else throw new ArgumentInvalidException(AuthorizedEntityIdParameter + " parameter")

      val districtStudentIdParam = parameters.find(p => p.key.toLowerCase == DistrictStudentIdParameter.toLowerCase)
      districtStudentId = if (districtStudentIdParam.isDefined && !districtStudentIdParam.get.value.isNullOrEmpty) districtStudentIdParam.get.value else throw new ArgumentInvalidException(DistrictStudentIdParameter + " parameter")

      val externalServiceIdParam = parameters.find(p => p.key.toLowerCase == ExternalServiceIdParameter.toLowerCase)
      externalServiceId = if (externalServiceIdParam.isDefined && !externalServiceIdParam.get.value.isNullOrEmpty) externalServiceIdParam.get.value.toInt else throw new ArgumentInvalidException(ExternalServiceIdParameter + " parameter")

      val objectTypeParam = parameters.find(p => p.key.toLowerCase == ObjectTypeParameter.toLowerCase)
      objectType = if (objectTypeParam.isDefined && !objectTypeParam.get.value.isNullOrEmpty) objectTypeParam.get.value else throw new ArgumentInvalidException(ObjectTypeParameter + " parameter")

      val personnelIdParam = parameters.find(p => p.key.toLowerCase == PersonnelIdParameter.toLowerCase)
      personnelId = if (personnelIdParam.isDefined && !personnelIdParam.get.value.isNullOrEmpty) personnelIdParam.get.value else null

      val zoneIdParam = parameters.find(p => p.key.toLowerCase == ZoneIdParameter.toLowerCase)
      zoneId = if (zoneIdParam.isDefined && !zoneIdParam.get.value.isNullOrEmpty) zoneIdParam.get.value else throw new ArgumentInvalidException(ZoneIdParameter + " parameter")

      try {
        val datasource = new Datasource(datasourceConfig)
        val result = datasource.get(
          "select * from srx_services_prs.get_filter_set(?, ?, ?, ?, ?, ?);",
          objectType,
          zoneId,
          externalServiceId,
          districtStudentId,
          authorizedEntityId, {
            if (personnelId == null) null else personnelId.toInt
          }
        )
        datasource.close()
        if (result.success) {
          if(result.rows.isEmpty) {
            SrxResourceErrorResult(SifHttpStatusCode.NotFound, new Exception("Filters for student '%s' not found.".format(districtStudentId)))
          } else {
            new PrsFilterResult(
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

    } catch {
      case e: Exception =>
        SrxResourceErrorResult(SifHttpStatusCode.BadRequest, e)
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

    val replacementText = "<!-- SRX -->"

    val dataObjects = ArrayBuffer[DataObject]()
    for (row <- result.rows) {
      dataObjects += DataObject(
        row.getString("data_object_id").getOrElse("0").toInt,
        row.getString("data_set_id").getOrElse("0").toInt,
        row.getString("data_object_name").getOrElse(""),
        row.getString("filter_type").getOrElse(""),
        row.getString("include_statement").getOrElse("")
      )
    }

    val filters = ArrayBuffer[PrsFilter]()

    if (dataObjects.nonEmpty) {
      val rootObject = dataObjects.head.name

      // copy all allowed children of the SIF object
      filters += PrsFilter(<xsl:template match={"/%s//* | /%s//@*".format(rootObject, rootObject)}>
        <xsl:copy>
          <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
      </xsl:template>)

      // ignores
      val includeStatements: ArrayBuffer[String] = dataObjects.filter(d => d.filterType.toLowerCase == "include").map(d => d.includeStatement.trimPrecedingSlash)
      val ignores: ArrayBuffer[String] = includeStatements.filter(i => includeStatements.exists(s => i.contains(s) && i != s))

      // excludes
      for (dataObject <- dataObjects.filter(o => o.filterType.toLowerCase != "include")) {
        val path = "/" + rootObject + "/" + dataObject.includeStatement.trimPrecedingSlash
        filters += PrsFilter(<xsl:template match={path}></xsl:template>)
      }

      // includes
      val includes = new TrieMap[String, ArrayBuffer[String]]
      addIncludeMapElement(includes, rootObject, "@*")

      for (includeStatement <- includeStatements.filter(s => !ignores.contains(s))) {
        val modifiedStatement = replaceSlashWithinBrackets(includeStatement, replacementText)
        val includeKey = new StringBuilder(rootObject)
        val elements = modifiedStatement.split("/")
        if (elements.nonEmpty) {
          // iterate through all but the leaf (tail) node
          for (i <- 0 until elements.length - 1) {
            val elementName = elements(i).replace(replacementText, "/")
            addIncludeMapElement(includes, includeKey.toString, elementName)
            includeKey.append("/" + elementName)
            addIncludeMapElement(includes, includeKey.toString, "@*")
          }
          addIncludeMapElement(includes, includeKey.toString, elements.last)
        }
      }

      for (include <- ListMap(includes.toSeq.sortBy(_._1):_*)) {
        val rootElement = "/" + include._1
        var delimiter = ""
        val elements = new StringBuilder
        for (element <- include._2) {
          elements.append(delimiter + "./" + element)
          delimiter = " | "
        }
        filters += PrsFilter(<xsl:template match={rootElement}>
          <xsl:copy>
            <xsl:apply-templates select={elements.toString}/>
          </xsl:copy>
        </xsl:template>)
      }
    }

    filters.toList
  }

  private def addIncludeMapElement(includes: TrieMap[String, ArrayBuffer[String]], absolutePath: String, elementName: String): Unit = {
    if (includes.contains(absolutePath)) {
      if (!includes(absolutePath).contains(elementName)) {
        includes(absolutePath) += elementName
      }
    } else {
      includes.put(absolutePath, ArrayBuffer[String](elementName))
    }
  }

  private def replaceSlashWithinBrackets(value: String, replacementText: String): String = {
    var result = value
    var position = 0

    while (position < value.length && position != -1) {
      val startIndex = result.indexOf("[", position)
      var incIndex = startIndex
      var nextIndex = 0
      var endIndex = if (startIndex > -1) result.indexOf("]", startIndex) else -1

      while (incIndex < endIndex) {
        nextIndex = result.indexOf("[", incIndex + 1)
        if (nextIndex > -1) {
          endIndex = if (result.indexOf("]", endIndex + 1) > -1) result.indexOf("]", endIndex + 1) else endIndex
          incIndex = nextIndex
        } else {
          incIndex = endIndex
        }
      }

      position = endIndex

      if (position != -1) {
        result = result.substring(0, startIndex) +
          result.substring(startIndex, endIndex).replace("/", replacementText) +
          result.substring(endIndex)
      }
    }

    result
  }

}
