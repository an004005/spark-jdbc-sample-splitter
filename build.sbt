name := "spark-jdbc-sample-splitter"

version := "0.1"

scalaVersion := "2.12.2"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.0.0" withSources() withJavadoc()
libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.0.0" withSources() withJavadoc()

