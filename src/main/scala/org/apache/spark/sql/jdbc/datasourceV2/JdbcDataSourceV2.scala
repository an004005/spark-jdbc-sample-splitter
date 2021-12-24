package org.apache.spark.sql.jdbc.datasourceV2

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.catalog.{Table, TableProvider}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.execution.datasources.jdbc.{JDBCOptions, JDBCRelation}
import org.apache.spark.sql.sources.DataSourceRegister
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.util
import java.util.UUID

class JdbcDataSourceV2 extends TableProvider with DataSourceRegister {
  private var t: Table = null
  private val sparkSession: SparkSession = SparkSession.active

  override def inferSchema(options: CaseInsensitiveStringMap): StructType = {
    if (t == null) {
      t = getTable(options)
    }
    t.schema()
  }

  def getTable(options: CaseInsensitiveStringMap): Table = {
    var plainStringMap = Map[String, String]()
    options.entrySet().forEach(kv => plainStringMap += (kv.getKey -> kv.getValue))
    val jdbcOptions = new JDBCOptions(plainStringMap)
    val schema = JDBCRelation.getSchema(sparkSession.sqlContext.conf.resolver, jdbcOptions)
    JdbcTable(UUID.randomUUID().toString, sparkSession, schema, jdbcOptions)
  }

  override def getTable(schema: StructType, partitioning: Array[Transform], properties: util.Map[String, String]): Table = {
    if (t != null) {
      t
    } else {
      getTable(new CaseInsensitiveStringMap(properties))
    }
  }

  override def shortName(): String = "jdbc.sample.splitter"
}
