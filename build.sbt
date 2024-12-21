ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

lazy val root = (project in file("."))
  .settings(
    name := "Assignment_2"
  )

resolvers += "Akka library repository" at "https://repo.akka.io/maven"

val AKKA_VERSION = "2.10.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % AKKA_VERSION,
  "com.typesafe.akka" %% "akka-persistence-typed" % AKKA_VERSION,
  "org.slf4j" % "slf4j-simple" % "2.0.16",

  // For persistence
  "org.iq80.leveldb" % "leveldb" % "0.12",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
)

