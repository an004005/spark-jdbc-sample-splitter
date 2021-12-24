package org.apache.spark.sql.jdbc.sampling

import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions
import org.apache.spark.sql.jdbc.{JdbcDialect, PostgresDialect}

import java.sql.Statement

object SampleDialect {
  implicit class DialectWith(dialect: JdbcDialect) {
    def getTableRandomQuery(column: String, table: String, totalCount: Long): String = {
      dialect match {
        case _ => s"SELECT $column FROM $table ORDER BY RAND() LIMIT ${getSamplingCount(totalCount)}"
      }
    }

    def getTableSamplingQuery(column: String, table: String, totalCount: Long): String = {
      dialect match {
        case PostgresDialect => s"SELECT $column FROM $table TABLESAMPLE BERNOULLI(${getSamplePercent(totalCount)})"
        case _ => s"SELECT $column FROM $table SAMPLE(${getSamplePercent(totalCount)})"
      }
    }

    def getCountQuery(table: String): String = {
      dialect match {
        case _ => s"SELECT COUNT(1) FROM $table"
      }
    }

    def getPartitionColumn(stmt: Statement, dialect: JdbcDialect, options: JDBCOptions): String = {
      dialect match {
        case _ =>
          Utils.ResourceManager { use =>
            val rs = use(stmt.executeQuery(dialect.getSchemaQuery(options.tableOrQuery)))
            rs.getMetaData.getColumnName(1)
          }
      }
    }
  }

  // todo : this is not optimized calculation to get sample's count. some one help me to proof this
  def getSamplingCount(totalCount: Long): Long =
    if (totalCount < 1000) {
      totalCount
    } else if (totalCount < 10000000) {
      totalCount / 100
    } else {
      totalCount / 1000
    }


  def getSamplePercent(totalCount: Long): Double =
    BigDecimal((getSamplingCount(totalCount).toDouble / totalCount) * 100)
      .setScale(2, BigDecimal.RoundingMode.HALF_UP)
      .toDouble
}
