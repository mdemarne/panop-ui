name := """panop-web"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala).dependsOn(panopCore)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  jdbc,
  cache,
  ws,
  specs2 % Test,

  // Web jars
  "org.webjars" % "jquery" % "2.1.3",
  "org.webjars" % "materializecss" % "0.95.3"
)

// Directly dependingon the latest panopCore version from Git
lazy val panopCore = RootProject(uri("https://github.com/mdemarne/panop-core.git"))

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator
