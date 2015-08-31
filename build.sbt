import sbt.Project.projectToRef

val scalaV = "2.11.7"

lazy val root = (project in file(".")).settings(
    name := """panop-web""",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaV,
    scalaJSProjects := Seq(scalaJsClient),
    pipelineStages := Seq(scalaJSProd, gzip),
    libraryDependencies ++= Seq(
      jdbc,
      cache,
      ws,
      specs2 % Test,
      // Web jars
      "org.webjars" % "jquery" % "2.1.3",
      "org.webjars" % "materializecss" % "0.96.0",
      // ScalaJS
      "com.vmunier" %% "play-scalajs-scripts" % "0.3.0"
    ),
    resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
  ).enablePlugins(PlayScala, SbtWeb, PlayScalaJS)
  .aggregate(projectToRef(scalaJsClient))
  .dependsOn(panopCore)

// Directly dependingon the latest panopCore version from Git
lazy val panopCore = RootProject(uri("https://github.com/mdemarne/panop-core.git"))

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// Scala JS

lazy val scalaJsClient = (project in file("scala-js/client/")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq( // TODO: figure out what is required here
    "org.scala-js" %%% "scalajs-dom" % "0.8.1",
    "com.lihaoyi" %%% "scalatags" % "0.5.2",
    "com.lihaoyi" %%% "scalarx" % "0.2.8",
    "be.doeraene" %%% "scalajs-jquery" % "0.8.0",
    "com.lihaoyi" %%% "upickle" % "0.3.4"
  )).enablePlugins(ScalaJSPlugin, ScalaJSPlay)