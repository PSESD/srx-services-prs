package org.psesd.srx.services.prs

/** Provides information about current build.
  *
  * @version 1.0
  * @since 1.0
  * @author Stephen Pugmire (iTrellis, LLC)
  */
object Build {
  val name: String = org.psesd.srx.services.prs.BuildInfo.name

  val version: String = org.psesd.srx.services.prs.BuildInfo.version

  val scalaVersion: String = org.psesd.srx.services.prs.BuildInfo.scalaVersion

  val sbtVersion: String = org.psesd.srx.services.prs.BuildInfo.sbtVersion

  val buildNumber: Int = org.psesd.srx.services.prs.BuildInfo.buildNumber

  val javaVersion: String = scala.util.Properties.javaVersion
}