import org.openurp.parent.Dependencies.*
import org.openurp.parent.Settings.*

ThisBuild / organization := "org.openurp.edu"
ThisBuild / version := "0.0.19-SNAPSHOT"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/openurp/edu-ws"),
    "scm:git@github.com:openurp/edu-ws.git"
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

ThisBuild / description := "OpenURP Edu WebService"
ThisBuild / homepage := Some(url("http://openurp.github.io/edu-ws/index.html"))

val apiVer = "0.26.0"
val starterVer = "0.0.21"
val openurp_edu_api = "org.openurp.edu" % "openurp-edu-api" % apiVer
val openurp_std_api = "org.openurp.std" % "openurp-std-api" % apiVer
val openurp_stater_ws = "org.openurp.starter" % "openurp-starter-ws" % starterVer

lazy val root = (project in file("."))
  .settings()
  .aggregate(grade, ws)

lazy val grade = (project in file("grade"))
  .settings(
    name := "openurp-edu-grade-core",
    organization := "org.openurp.edu.grade",
    common,
    libraryDependencies ++= Seq(openurp_edu_api,openurp_std_api),
    libraryDependencies ++= Seq(beangle_data_transfer,beangle_cdi_spring,beangle_security_core,gson)
  )

lazy val ws = (project in file("ws"))
  .enablePlugins(WarPlugin, UndertowPlugin)
  .settings(
    name := "openurp-edu-ws",
    common,
    libraryDependencies ++= Seq(openurp_stater_ws, beangle_serializer_text),
    libraryDependencies ++= Seq(openurp_std_api, beangle_ems_app)
  ).dependsOn(grade)
