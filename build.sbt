import org.openurp.parent.Dependencies.*
import org.openurp.parent.Settings.*

ThisBuild / version := "0.3.12-SNAPSHOT"

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

val apiVer = "0.44.1"
val starterVer = "0.3.59"

val openurp_edu_api = "org.openurp.edu" % "openurp-edu-api" % apiVer
val openurp_std_api = "org.openurp.std" % "openurp-std-api" % apiVer
val openurp_stater_ws = "org.openurp.starter" % "openurp-starter-ws" % starterVer

lazy val root = (project in file("."))
  .settings(
    common,
    name := "openurp-edu-core-root",
    organization := "org.openurp.edu")
  .aggregate(core, ws)

lazy val core = (project in file("core"))
  .settings(
    name := "openurp-edu-core",
    organization := "org.openurp.edu",
    common,
    libraryDependencies ++= Seq(openurp_edu_api, openurp_std_api),
    libraryDependencies ++= Seq(beangle_ems_app),
    libraryDependencies ++= Seq(beangle_doc_transfer, beangle_cdi, beangle_security)
  )

lazy val ws = (project in file("ws"))
  .enablePlugins(WarPlugin, UndertowPlugin, TomcatPlugin)
  .settings(
    name := "openurp-edu-ws",
    organization := "org.openurp.edu",
    common,
    libraryDependencies ++= Seq(openurp_edu_api, openurp_std_api),
    libraryDependencies ++= Seq(beangle_ems_app, openurp_stater_ws),
    libraryDependencies ++= Seq(spring_tx, spring_jdbc)
  ).dependsOn(core)

publish / skip := true
