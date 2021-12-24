package org.apache.spark.sql.jdbc.datasourceV2

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.read.{Scan, ScanBuilder}
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions
import org.apache.spark.sql.types.StructType

case class JdbcScanBuilder(
    sparkSession: SparkSession,
    schema: StructType,
    options: JDBCOptions)
  extends ScanBuilder {
  override def build(): Scan = JdbcScan(sparkSession, schema, options)
}
