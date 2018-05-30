
val root = project.in(file("."))
  .settings(
    organization := "de.wayofquality.blended",
    name := "CamelSimple",
    version := "0.0.1-SNAPSHOT",

    scalaVersion := "2.11.12",

    libraryDependencies := deps,

    unmanagedBase := baseDirectory.value / "src" / "main" / "libs"
  )

lazy val deps = Seq(
  "de.wayofquality.blended" % "blended.jms.utils" % "2.4.0",

  "org.apache.camel" % "camel-core" % "2.17.3",
  "org.apache.camel" % "camel-core" % "2.17.3",

  "com.typesafe.akka" %% "akka-actor" % "2.5.9",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.9",

  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback" % "logback-core" % "1.2.3",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)
