package org.apache.spark.sql.jdbc.datasourceV2

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.read.{InputPartition, PartitionReader, PartitionReaderFactory}
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions
import org.apache.spark.sql.types.StructType

case class JdbcPartitionReaderFactory(
    schema: StructType,
    options: JDBCOptions)
  extends PartitionReaderFactory {
  override def createReader(partition: InputPartition): PartitionReader[InternalRow] =
    JdbcPartitionReader(schema, partition, options)
}
