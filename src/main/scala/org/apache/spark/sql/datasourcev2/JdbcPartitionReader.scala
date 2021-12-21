package org.apache.spark.sql.datasourcev2

import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.read.{InputPartition, PartitionReader}
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions
import org.apache.spark.sql.types.StructType

case class JdbcPartitionReader(
    schema: StructType,
    partition: InputPartition,
    options: JDBCOptions)
  extends PartitionReader[InternalRow] {
  override def next(): Boolean = ???

  override def get(): InternalRow = ???

  override def close(): Unit = ???
}
