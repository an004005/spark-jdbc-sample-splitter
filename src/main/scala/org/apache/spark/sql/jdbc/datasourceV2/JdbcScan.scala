package org.apache.spark.sql.jdbc.datasourceV2

import org.apache.spark.internal.Logging
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.read.{Batch, InputPartition, PartitionReaderFactory, Scan}
import org.apache.spark.sql.execution.datasources.jdbc.{JDBCOptions, JdbcUtils}
import org.apache.spark.sql.jdbc.JdbcDialects
import org.apache.spark.sql.jdbc.sampling.{JdbcSampleHandler, Utils}
import org.apache.spark.sql.types.StructType

import scala.collection.mutable.ArrayBuffer

case class JdbcInputPartition(whereCond: String, idx: Int) extends InputPartition {
  def whereClause: String = whereCond match {
    case "" => ""
    case _ => s"WHERE $whereCond"
  }
}

case class JdbcScan(
    sparkSession: SparkSession,
    schema: StructType,
    options: JDBCOptions)
  extends Scan with Batch with Logging {
  private var partitions: Array[InputPartition] = null

  override def readSchema(): StructType = schema

  override def planInputPartitions(): Array[InputPartition] = {
    if (partitions == null) {
      val numPartitions = options.numPartitions.getOrElse(1)
      partitions =
        if (numPartitions <= 1) {
          Array(JdbcInputPartition("", 0))
        } else {
          plainPartitions(numPartitions)
        }
    }

    partitions
  }

  override def createReaderFactory(): PartitionReaderFactory =
    JdbcPartitionReaderFactory(schema, options)

  override def toBatch: Batch = this

  private def plainPartitions(numPartitions: Int): Array[InputPartition] = {
    import org.apache.spark.sql.jdbc.sampling.SampleDialect._

    val dialect = JdbcDialects.get(options.url)
    val table = options.tableOrQuery

    Utils.ResourceManager {use =>
      val conn = use(JdbcUtils.createConnectionFactory(options)())
      val stmt = use(conn.createStatement())
      stmt.setQueryTimeout(options.queryTimeout)

      val partColumn = dialect.quoteIdentifier(
        options.partitionColumn
          .getOrElse(dialect.getPartitionColumn(stmt, dialect, options))
      )

      val countQuery = dialect.getCountQuery(table)
      val totalCount = Utils.ResourceManager { use =>
        val rs = use(stmt.executeQuery(countQuery))
        rs.next()
        rs.getLong(1)
      }

      Utils.ResourceManager { use =>
        val samplingQuery = dialect.getTableSamplingQuery(partColumn, table, totalCount)
        val rs = use(stmt.executeQuery(samplingQuery))
        val handler = JdbcSampleHandler(rs)
        val ans = ArrayBuffer[InputPartition]()

        // get samples and sort
        val sortedSamples = new Iterator[Any] {
          def hasNext: Boolean = rs.next()
          def next(): Any = handler.getter(rs)
        }.toList.sorted(handler.ordering)

        // get partitioning points
        val countBetweenPartition = (getSamplingCount(totalCount) / numPartitions).toInt
        val partPoints = (1 until numPartitions)
          .map(i => handler.setter(sortedSamples(i * countBetweenPartition)))

        var i: Int = 0
        while (i <= partPoints.length) {
          val whereCond =
            if (i == 0) {
              s"$partColumn < ${partPoints(i)} OR $partColumn is null"
            } else if (i == partPoints.length) {
              s"${partPoints(i - 1)} <= $partColumn"
            } else {
              s"${partPoints(i - 1)} < $partColumn AND $partColumn <= ${partPoints(i)} "
            }
          ans += JdbcInputPartition(whereCond, i)
          i += 1
        }

        val partitions = ans.toArray
        logInfo(s"Number of partitions: $numPartitions, WHERE clauses of these partitions: " +
          partitions.map(_.asInstanceOf[JdbcInputPartition].whereClause).mkString(", "))

        partitions
      }
    }
  }
}
