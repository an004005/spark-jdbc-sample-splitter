package org.apache.spark.sql.datasourcev2

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


//JdbcDataSourcev2 : TableProvider, DataSourceRegister
//
//JdbcTable : Table, SupportRead
//
//JdbcScanBuilder : ScanBuilder
//
//JdbcScan : Scan, Batch (planInputParition)
//
//JdbcPartitionReaderFactory : PartitionReaderFactory
//
//JdbcPartitionReader  : PartitionReader<InternalRow>
//(순서, 3.2와 비교하기)
