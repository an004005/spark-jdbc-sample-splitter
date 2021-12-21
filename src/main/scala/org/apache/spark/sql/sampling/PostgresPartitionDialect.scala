package org.apache.spark.sql.sampling

import org.apache.spark.sql.sampling.Utils.getSamplePercent

import java.util.Locale

object PostgresPartitionDialect extends JdbcPartitionDialect {
  override def canHandle(url: String): Boolean =
    url.toLowerCase(Locale.ROOT).startsWith("jdbc:postgresql")

  override def getTableSamplingQuery(column: String, table: String, totalCount: Long): String =
    s"SELECT $column FROM $table TABLESAMPLE BERNOULLI(${getSamplePercent(totalCount)})"
}
