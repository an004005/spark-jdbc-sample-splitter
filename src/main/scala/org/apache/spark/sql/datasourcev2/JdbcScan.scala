package org.apache.spark.sql.datasourcev2

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.read.{Batch, InputPartition, PartitionReaderFactory, Scan}
import org.apache.spark.sql.execution.datasources.jdbc.{JDBCOptions, JdbcUtils}
import org.apache.spark.sql.jdbc.JdbcDialects
import org.apache.spark.sql.sampling.{JdbcPartitionDialects, Utils}
import org.apache.spark.sql.types.StructType

import scala.collection.mutable.ListBuffer

case class JdbcInputPartition(whereClause: String, idx: Int) extends InputPartition

case class JdbcScan(
    sparkSession: SparkSession,
    schema: StructType,
    options: JDBCOptions)
  extends Scan with Batch {
  override def readSchema(): StructType = schema

  override def planInputPartitions(): Array[InputPartition] = {
    val numPartitions = options.numPartitions.getOrElse(1)
    if (numPartitions <= 1) {
      Array(JdbcInputPartition("", 1))
    } else {
      val dialect = JdbcDialects.get(options.url)
      val partitionDialect = JdbcPartitionDialects.get(options.url)
      val conn = JdbcUtils.createConnectionFactory(options)()
      val table = options.tableOrQuery

      val partitionColumn = dialect.quoteIdentifier(
        options.partitionColumn.getOrElse(partitionDialect.getPartitionColumn(conn, options))
      )

      val countQuery = partitionDialect.getCountQuery(table)
      val totalCount = Utils.tryResources(conn.prepareStatement(countQuery)) {
        stmt => {
          stmt.setQueryTimeout(options.queryTimeout)
          Utils.tryResources(stmt.executeQuery()) {
            rs => {
              rs.next()
              rs.getLong(1)
            }
          }
        }
      }
      val samplingQuery = partitionDialect.getTableSamplingQuery(partitionColumn, table, totalCount)

      val partitionPoints = ListBuffer()
      Utils.tryResources(conn.prepareStatement(samplingQuery)) {
        stmt => {
          stmt.setQueryTimeout(options.queryTimeout)
          Utils.tryResources(stmt.executeQuery()) {
            rs => {

            }
          }
        }
      }



    null
    }
  }

  override def createReaderFactory(): PartitionReaderFactory =
    JdbcPartitionReaderFactory(schema, options)
}
