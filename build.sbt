import sbt.Project.projectToRef

val scalaV = "2.11.7"

lazy val root = (project in file(".")).settings(
    name := """panop-web""",
    version := "1.0-SNAPSHOT",
    scalaVersion := scalaV,
    scalaJSProjects := Seq(scalaJsFrontend),
    pipelineStages := Seq(scalaJSProd, gzip),
    libraryDependencies ++= Seq(
      jdbc,
      cache,
      ws,
      specs2 % Test,
      // Databases
      evolutions,
      "com.typesafe.play" %% "anorm" % "2.4.0",
      // Web jars
      "org.webjars" % "jquery" % "2.1.3",
      "org.webjars" % "materializecss" % "0.96.0",
      // ScalaJS
      "com.vmunier" %% "play-scalajs-scripts" % "0.3.0",
      "com.lihaoyi" %% "upickle" % "0.2.8"
    ),
    resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
  ).enablePlugins(PlayScala, SbtWeb, PlayScalaJS)
  .aggregate(projectToRef(scalaJsFrontend))
  .dependsOn(panopCore, scalaJsSharedJVM)

// Directly depending on the latest panopCore version from Git
lazy val panopCore = RootProject(uri("https://github.com/mdemarne/panop-core.git"))

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// Scala JS Settings
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
scalaJSStage in Global := FastOptStage

// Scala JS -- Shared
lazy val scalaJsShared = (crossProject.crossType(CrossType.Pure) in file("scala-js/shared/")).
  settings(scalaVersion := scalaV).
  jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val scalaJsSharedJVM = scalaJsShared.jvm
lazy val scalaJsSharedJS = scalaJsShared.js

// Scala Js -- Client
lazy val scalaJsFrontend = (project in file("scala-js/frontend/")).settings(
  scalaVersion := scalaV,
  persistLauncher := true,
  persistLauncher in Test := false,
  libraryDependencies ++= Seq( // TODO: figure out what is required here
    "org.scala-js" %%% "scalajs-dom" % "0.8.1",
    "be.doeraene" %%% "scalajs-jquery" % "0.8.0",
    "com.lihaoyi" %%% "upickle" % "0.3.4",
    "com.github.japgolly.scalajs-react" %%% "core" % "0.9.2"
  ),
  jsDependencies ++= Seq(
    "org.webjars" % "react" % "0.12.2" / "react-with-addons.js" commonJSName "React"
  )).enablePlugins(ScalaJSPlugin, ScalaJSPlay).dependsOn(scalaJsSharedJS)

// Loads the Play project at sbt startup
onLoad in Global := (Command.process("project root", _: State)) compose (onLoad in Global).value