import org.openurp.parent.Dependencies._
import org.openurp.parent.Settings._

ThisBuild / version := "0.0.2-SNAPSHOT"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/openurp/edu-core"),
    "scm:git@github.com:openurp/edu-core.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "OpenURP Edu Core Library"
ThisBuild / homepage := Some(url("http://openurp.github.io/edu-core/index.html"))

val apiVer = "0.30.0"
val starterVer = "0.2.0"
val openurp_edu_api = "org.openurp.edu" % "openurp-edu-api" % apiVer
val openurp_std_api = "org.openurp.std" % "openurp-std-api" % apiVer

lazy val root = (project in file("."))
  .settings(
    name := "openurp-edu-core",
    organization := "org.openurp.edu",
    common,
    libraryDependencies ++= Seq(openurp_edu_api,openurp_std_api),
    libraryDependencies ++= Seq(beangle_data_transfer, beangle_cdi_spring, beangle_security_core, gson)
  )

