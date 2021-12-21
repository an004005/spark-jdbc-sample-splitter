package org.apache.spark.sql.sampling

import org.apache.spark.sql.connector.read.InputPartition
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions
import org.apache.spark.sql.jdbc.JdbcDialects
import Utils.{getSamplePercent, getSamplingCount}

import java.sql.Connection

abstract class JdbcPartitionDialect extends Serializable {

  def canHandle(url: String): Boolean

  def getTableSamplingQuery(column: String, table: String, totalCount: Long): String = {
    s"SELECT $column FROM $table SAMPLE(${getSamplePercent(totalCount)})"
  }

  def getTableRandomQuery(column: String, table: String, totalCount: Long): String = {
    s"SELECT $column FROM $table ORDER BY RAND() LIMIT ${getSamplingCount(totalCount)}"
  }

  def getCountQuery(table: String): String = {
    s"SELECT COUNT(1) FROM $table"
  }

  def getPartitionColumn(conn: Connection, options: JDBCOptions): String = {
    val table = options.tableOrQuery
    val dialect = JdbcDialects.get(options.url)
    Utils.tryResources(conn.prepareStatement(dialect.getSchemaQuery(table))) {
      stmt => stmt.getMetaData.getColumnName(1)
    }
  }
}

object JdbcPartitionDialects {
  private[this] var dialects = List[JdbcPartitionDialect]()

  def registerDialect(dialect: JdbcPartitionDialect) : Unit = {
    dialects = dialect :: dialects.filterNot(_ == dialect)
  }

  registerDialect(PostgresPartitionDialect)

  def get(url: String): JdbcPartitionDialect = {
    val matchingDialects = dialects.filter(_.canHandle(url))
    matchingDialects.length match {
      case 0 => NoopDialect
      case 1 => matchingDialects.head
    }
  }
}

private object NoopDialect extends JdbcPartitionDialect {
  override def canHandle(url : String): Boolean = true
}
