package org.apache.spark.sql.datasourcev2

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.read.{Batch, InputPartition, PartitionReaderFactory, Scan}
import org.apache.spark.sql.execution.datasources.jdbc.JdbcUtils.getCatalystType
import org.apache.spark.sql.execution.datasources.jdbc.{JDBCOptions, JdbcUtils}
import org.apache.spark.sql.jdbc.JdbcDialects
import org.apache.spark.sql.sampling.{JdbcPartitionDialects, JdbcSampleHandler, Utils}
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
      Array(JdbcInputPartition("", 0))
    } else {
      val dialect = JdbcDialects.get(options.url)
      val partitionDialect = JdbcPartitionDialects.get(options.url)
      val conn = JdbcUtils.createConnectionFactory(options)()
      val table = options.tableOrQuery

      Utils.ResourceManager {use =>
        val stmt = use(conn.createStatement())
        stmt.setQueryTimeout(options.queryTimeout)

        val partitionColumn = dialect.quoteIdentifier(
          options.partitionColumn.getOrElse(partitionDialect.getPartitionColumn(stmt, dialect, options))
        )

        val countQuery = partitionDialect.getCountQuery(table)
        val totalCount = Utils.ResourceManager { use =>
          val rs = use(stmt.executeQuery(countQuery))
          rs.next()
          rs.getLong(1)
        }

        val samplingQuery = partitionDialect.getTableSamplingQuery(partitionColumn, table, totalCount)

        val partitionPerCount = totalCount.toDouble / numPartitions
        val partitionPoints = ListBuffer[Any]()
        var samples = ListBuffer[Any]()

        Utils.ResourceManager { use =>
          val rs = use(stmt.executeQuery(samplingQuery))
          val handler = JdbcSampleHandler(rs)

          while (rs.next()) {
            samples :+ handler.getter(rs)
          }
          samples = handler.sorted(samples)

          // until 확인하기
          (1 until numPartitions).foreach(i =>
            partitionPoints :+ handler.setter(samples(i * partitionPerCount.toInt))
          )

          // make where clause


        }


      }





    null
    }
  }

  override def createReaderFactory(): PartitionReaderFactory =
    JdbcPartitionReaderFactory(schema, options)
}
