package org.psesd.srx.services.admin

import org.scalatest.FunSuite

class BuildTests extends FunSuite {

  test("package") {
    assert(Build.name == "srx-services-admin")
    assert(Build.version == "1.0")
    assert(Build.buildNumber > 0)
  }

  test("framework") {
    assert(!Build.javaVersion.isEmpty)
    assert(Build.scalaVersion == "2.11.8")
    assert(!Build.sbtVersion.isEmpty)
  }

}
