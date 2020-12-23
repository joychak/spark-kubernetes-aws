name := "spark-example"
version := "0.1"

organization := "com.datalogs"
scalaVersion := "2.12.10"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "3.0.1" % "provided",
  "org.apache.spark" %% "spark-sql" % "3.0.1" % "provided"
//  "com.amazonaws" % "aws-java-sdk-bundle" % "1.11.874",
//  "org.apache.hadoop" % "hadoop-aws" % "3.2.0"
)

//assemblyMergeStrategy in assembly := {
//  case PathList("META-INF", xs @ _*) =>
//    xs map {_.toLowerCase} match {
//      case "manifest.mf" :: Nil | "index.list" :: Nil | "dependencies" :: Nil =>
//        MergeStrategy.discard
//      case ps @ x :: xs if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
//        MergeStrategy.discard
//      case "plexus" :: xs =>
//        MergeStrategy.discard
//      case "services" :: xs =>
//        MergeStrategy.filterDistinctLines
//      case "spring.schemas" :: Nil | "spring.handlers" :: Nil =>
//        MergeStrategy.filterDistinctLines
//      case _ => MergeStrategy.first
//    }
//  case "application.conf" => MergeStrategy.concat
//  case "reference.conf" => MergeStrategy.concat
//  case _ => MergeStrategy.first
//}

//assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)



