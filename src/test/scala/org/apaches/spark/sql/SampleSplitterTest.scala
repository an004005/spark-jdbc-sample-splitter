package org.apaches.spark.sql

import org.apache.spark.sql.{SQLContext, SQLImplicits, SparkSession}
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite

class SampleSplitterTest extends AnyFunSuite with BeforeAndAfter { self =>
  var spark: SparkSession = _

  private object testImplicits extends SQLImplicits {
    protected override def _sqlContext: SQLContext = self.spark.sqlContext
  }
  import testImplicits._

  before {
    spark = SparkSession.builder().master("local[*]").getOrCreate()
  }

  test("use jdbc.sample.splitter") {
    val dataset = spark.read
      .format("jdbc.sample.splitter")
      .option("url", "jdbc:postgresql://127.0.0.1:5435/postgres")
      .option("dbtable", "test")
      .option("user", "my")
      .option("password", "my")
      .option("fetchsize", "1024")
      .option("numPartitions", 3)
      .load()

    dataset
      .rdd
      .mapPartitionsWithIndex{case (i,rows) => Iterator((i,rows.size))}
      .toDF("partition_number","number_of_records")
      .show
  }

}
