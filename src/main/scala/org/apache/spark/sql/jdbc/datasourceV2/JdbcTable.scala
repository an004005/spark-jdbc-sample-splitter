package org.apache.spark.sql.jdbc.datasourceV2

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.catalog.{SupportsRead, Table, TableCapability}
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.util
import scala.collection.JavaConverters.{mapAsScalaMapConverter, setAsJavaSetConverter}

case class JdbcTable(
    name: String,
    sparkSession: SparkSession,
    schema: StructType,
    jdbcOptions: JDBCOptions)
  extends Table with SupportsRead {
  override def newScanBuilder(options: CaseInsensitiveStringMap): ScanBuilder = {
    val merged = new JDBCOptions(
      jdbcOptions.parameters.originalMap ++ options.asCaseSensitiveMap().asScala)
    JdbcScanBuilder(sparkSession, schema, merged)
  }

  override def capabilities(): util.Set[TableCapability] =
    Set(TableCapability.BATCH_READ).asJava
}
