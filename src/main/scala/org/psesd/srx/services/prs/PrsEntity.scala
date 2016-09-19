package org.psesd.srx.services.prs

import org.json4s.JValue

import scala.xml.Node

/** Interface for Privacy Rules Service entity objects.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
trait PrsEntity {

  def toJson: JValue

  def toXml: Node

}
