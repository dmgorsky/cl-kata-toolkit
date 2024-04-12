
val toolkitV = "0.2.0"
val toolkit = "org.scala-lang" %% "toolkit" % toolkitV
val toolkitTest = "org.scala-lang" %% "toolkit-test" % toolkitV

ThisBuild / scalaVersion := "3.3.0"
libraryDependencies += toolkit
libraryDependencies += (toolkitTest % Test)
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.5.2"

enablePlugins(
  JavaAppPackaging,
  DockerPlugin
)

compile / mainClass := Some("example.Main")
Docker / packageName := "dmgorsky/command-line-kata"
dockerBaseImage := "adoptopenjdk:11-jre-hotspot"
Docker / containerBuildImage := Some("adoptopenjdk:11")
