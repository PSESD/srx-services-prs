package org.psesd.srx.services.prs

import org.psesd.srx.shared.core.extensions.ExtendedEnumeration

/** Enumeration of supported PRS resources.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  **/
object PrsResource extends ExtendedEnumeration {
  type PrsResource = Value
  val AuthorizedEntities = Value("authorizedEntities")
  val Consents = Value("consents")
  val DataObjects = Value("sifDataObjects")
  val DataSets = Value("dataSets")
  val Districts = Value("districts")
  val ExternalServices = Value("services")
  val Filters = Value("filters")
  val Personnel = Value("personnel")
  val Students = Value("students")
}