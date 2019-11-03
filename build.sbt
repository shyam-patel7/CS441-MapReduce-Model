name := "Shyam_Patel_hw2"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  // Logback Classic Module
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  // Typesafe Config
  "com.typesafe" % "config" % "1.4.0",
  // Apache Hadoop Common
  "org.apache.hadoop" % "hadoop-common" % "3.2.1" exclude("org.slf4j", "slf4j-log4j12"),
  // Apache Hadoop MapReduce Core
  "org.apache.hadoop" % "hadoop-mapreduce-client-core" % "3.2.1" exclude("org.slf4j", "slf4j-log4j12"),
  // Plotly Render
  "org.plotly-scala" %% "plotly-render" % "0.7.1",
  // Scala XML
  "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
  // ScalaTest
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  // SLF4J API Module
  "org.slf4j" % "slf4j-api" % "1.7.28"
)

// override default deduplicate merge strategy
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case _                             => MergeStrategy.first
}

// show deprecation warnings
scalacOptions := Seq("-unchecked", "-deprecation")
