package org.apache.spark.sql.datasourcev2

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.read.{Batch, InputPartition, PartitionReaderFactory, Scan}
import org.apache.spark.sql.execution.datasources.jdbc.{JDBCOptions, JdbcUtils}
import org.apache.spark.sql.jdbc.JdbcDialects
import org.apache.spark.sql.sampling.{JdbcPartitionDialects, JdbcSampleHandler, Utils}
import org.apache.spark.sql.types.StructType

import scala.collection.mutable.ArrayBuffer

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
      val table = options.tableOrQuery

      Utils.ResourceManager {use =>
        val conn = use(JdbcUtils.createConnectionFactory(options)())
        val stmt = use(conn.createStatement())
        stmt.setQueryTimeout(options.queryTimeout)

        val partColumn = dialect.quoteIdentifier(
          options.partitionColumn
            .getOrElse(partitionDialect.getPartitionColumn(stmt, dialect, options))
        )

        val countQuery = partitionDialect.getCountQuery(table)
        val totalCount = Utils.ResourceManager { use =>
          val rs = use(stmt.executeQuery(countQuery))
          rs.next()
          rs.getLong(1)
        }

        val samplingQuery = partitionDialect.getTableSamplingQuery(partColumn, table, totalCount)
        val countBetweenPartition = (totalCount.toDouble / numPartitions).toInt

        Utils.ResourceManager { use =>
          val rs = use(stmt.executeQuery(samplingQuery))
          val handler = JdbcSampleHandler(rs)
          val ans = ArrayBuffer[JdbcInputPartition]()

          // get samples and sort
          val sortedSamples = new Iterator[Any] {
            def hasNext: Boolean = rs.next()
            def next(): Any = handler.getter(rs)
          }.toList.sorted(handler.ordering)

          // get partitioning points
          val partPoints = (1 until numPartitions)
            .map(i => handler.setter(sortedSamples(i * countBetweenPartition)))

          var i: Int = 0
          while (i <= partPoints.length) {
            val whereClause =
              if (i == 0) {
                s"$partColumn < ${partPoints(i)} OR $partColumn is null"
              } else if (i == partPoints.length) {
                s"${partPoints(i - 1)} <= $partColumn"
              } else {
                s"$partColumn < ${partPoints(i - 1)} AND ${partPoints(i)} <= $partColumn"
              }
            ans += JdbcInputPartition(whereClause, i)
            i += 1
          }

          ans.toArray
        }
      }
    }
  }

  override def createReaderFactory(): PartitionReaderFactory =
    JdbcPartitionReaderFactory(schema, options)
}
