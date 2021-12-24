name := "spark-jdbc-sample-splitter"

version := "1.0"

scalaVersion := "2.12.2"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.0.0" withSources() withJavadoc()
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.0.0" withSources() withJavadoc()
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % "test"
libraryDependencies += "org.postgresql" % "postgresql" % "42.2.24"


