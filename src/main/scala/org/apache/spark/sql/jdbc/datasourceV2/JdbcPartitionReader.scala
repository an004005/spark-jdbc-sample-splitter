package org.apache.spark.sql.jdbc.datasourceV2

import org.apache.spark.TaskContext
import org.apache.spark.executor.InputMetrics
import org.apache.spark.internal.Logging
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.read.{InputPartition, PartitionReader}
import org.apache.spark.sql.execution.datasources.jdbc.{JDBCOptions, JdbcUtils}
import org.apache.spark.sql.jdbc.JdbcDialects
import org.apache.spark.sql.jdbc.sampling.Utils
import org.apache.spark.sql.types.StructType

import java.sql.ResultSet

case class JdbcPartitionReader(
    schema: StructType,
    partition: InputPartition,
    options: JDBCOptions)
  extends PartitionReader[InternalRow] with Logging {
  private val whereClause = partition.asInstanceOf[JdbcInputPartition].whereClause
  private val index = partition.asInstanceOf[JdbcInputPartition].idx

  private val conn = JdbcUtils.createConnectionFactory(options)()
  private val dialect = JdbcDialects.get(options.url)

  import scala.collection.JavaConverters._

  dialect.beforeFetch(conn, options.asProperties.asScala.toMap)

  options.sessionInitStatement match {
    case Some(sql) =>
      Utils.ResourceManager { use =>
        val stmt = use(conn.prepareStatement(sql))
        stmt.setQueryTimeout(options.queryTimeout)
        logInfo(s"Executing sessionInitStatement: $sql")
        stmt.execute()
      }
    case None =>
  }

  private val sql = s"SELECT * FROM ${options.tableOrQuery} $whereClause"
  private val stmt = conn.prepareStatement(sql,
    ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)
  stmt.setFetchSize(options.fetchSize)
  stmt.setQueryTimeout(options.queryTimeout)
  private val rs = stmt.executeQuery()
  // todo: make inputMetrics work to show on ui (current is dummy)
  private val inputMetrics = Option(TaskContext.get()).map(_.taskMetrics().inputMetrics).getOrElse(new InputMetrics)
  private val rowsIterator = JdbcUtils.resultSetToSparkInternalRows(rs, schema, inputMetrics)

  override def next(): Boolean = rowsIterator.hasNext

  override def get(): InternalRow = rowsIterator.next

  override def close(): Unit = {
    Option(rs).foreach(_.close())
    Option(stmt).foreach(_.close())
    Option(conn).foreach(_.close())
  }
}
