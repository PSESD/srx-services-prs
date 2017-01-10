name := "srx-services-prs"

version := "1.0"

scalaVersion := "2.11.8"

parallelExecution in Test := false

lazy val apacheCommonsVersion = "2.1"
lazy val apacheHttpClientVersion = "4.5.2"
lazy val apachePoiVersion = "3.14"
lazy val http4sVersion = "0.14.1"
lazy val jodaConvertVersion = "1.8.1"
lazy val jodaTimeVersion = "2.9.4"
lazy val json4sVersion = "3.4.0"
lazy val scalaTestVersion = "2.2.6"

// Date/time
libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % jodaTimeVersion,
  "org.joda" % "joda-convert" % jodaConvertVersion
)

// Test
libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)

// JSON
libraryDependencies ++= Seq(
  "org.json4s" % "json4s-native_2.11" % json4sVersion,
  "org.json4s" % "json4s-jackson_2.11" % json4sVersion
)

// HTTP Client
libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % apacheHttpClientVersion
)

// HTTP Server
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion
)

// SFTP
libraryDependencies ++= Seq(
  "org.apache.commons" % "commons-vfs2" % apacheCommonsVersion,
  "org.apache.poi" % "poi" % apachePoiVersion
)

// Build info
lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  dependsOn(srxCore, srxData).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, BuildInfoKey.map(buildInfoBuildNumber) { case (k, v) =>
      "buildNumber" -> v
    }),
    buildInfoPackage := "org.psesd.srx.services.prs"
  )

lazy val srxCore = RootProject(uri("https://github.com/PSESD/srx-shared-core.git"))
lazy val srxData = RootProject(uri("https://github.com/PSESD/srx-shared-data.git"))

enablePlugins(JavaServerAppPackaging)