name := "scala-json-benchmark"
version := "2.0.0"
scalaVersion := "2.13.18"

// Latest stable releases available for Scala 2.13, checked against Maven Central.
// Retain this Scala line so every original library remains in the comparison.
libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-jackson" % "4.0.7",
  "io.spray" %% "spray-json" % "1.3.6",
  "net.liftweb" %% "lift-json" % "3.5.0",
  "org.playframework" %% "play-json" % "3.0.6",
  "io.circe" %% "circe-core" % "0.14.16",
  "io.circe" %% "circe-generic" % "0.14.16",
  "io.circe" %% "circe-parser" % "0.14.16",
  "io.argonaut" %% "argonaut" % "6.3.10",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-core" % "2.41.2",
  "com.github.plokhotnyuk.jsoniter-scala" %% "jsoniter-scala-macros" % "2.41.2" % Provided
)

scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
enablePlugins(JmhPlugin)

