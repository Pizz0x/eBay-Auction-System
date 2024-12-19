ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

lazy val root = (project in file("."))
  .settings(
    name := "Assignment_2"
  )

resolvers += "Akka library repository" at "https://repo.akka.io/maven"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % "2.10.0",
  "org.slf4j" % "slf4j-simple" % "2.0.16",
  "com.typesafe.akka" %% "akka-persistence-typed" % "2.6.20"
)

