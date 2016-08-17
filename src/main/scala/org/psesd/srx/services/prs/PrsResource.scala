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
}