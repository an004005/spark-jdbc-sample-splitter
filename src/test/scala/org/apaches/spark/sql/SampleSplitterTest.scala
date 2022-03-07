package org.apaches.spark.sql

import org.apache.spark.sql.{SQLContext, SQLImplicits, SparkSession}
import org.scalatest.BeforeAndAfter
import org.scalatest.funsuite.AnyFunSuite

/**
 * Test DB: Postgresql
 * Test data: tpch's lineitem table (6000000 rows)
 * https://github.com/electrum/tpch-dbgen
 */
class SampleSplitterTest extends AnyFunSuite with BeforeAndAfter { self =>
  var spark: SparkSession = _
  val url = "jdbc:postgresql://localhost:5435/postgres"
  val table = "lineitem"
  val user = "po"
  val password = "po"


  private object testImplicits extends SQLImplicits {
    protected override def _sqlContext: SQLContext = self.spark.sqlContext
  }
  import testImplicits._

  before {
    spark = SparkSession.builder().master("local[*]").getOrCreate()
  }

  test("split with l_comment in 5") {
    val dataset = spark.read
      .format("jdbc.sample.splitter")
      .option("url", url)
      .option("partitionColumn", "l_comment")
      .option("lowerBound", "dummy") // prevent spark assertion exception, not used
      .option("upperBound", "dummy") // prevent spark assertion exception, not used
      .option("dbtable", "lineitem_small")
      .option("user", user)
      .option("password", password)
      .option("fetchsize", "1024")
      .option("numPartitions", 5)
      .load()

    dataset
      .rdd
      .mapPartitionsWithIndex{case (i,rows) => Iterator((i,rows.size))}
      .toDF("partition_number","number_of_records")
      .show

    /**
      +----------------+-----------------+
      |partition_number|number_of_records|
      +----------------+-----------------+
      |               0|           388034|
      |               1|          1491470|
      |               2|          1278153|
      |               3|          1336648|
      |               4|          1507014|
      +----------------+-----------------+
     */
  }

  test("split with l_comment in 10") {
    val dataset = spark.read
      .format("jdbc.sample.splitter")
      .option("url", url)
      .option("partitionColumn", "l_comment")
      .option("lowerBound", "dummy") // prevent spark assertion exception, not used
      .option("upperBound", "dummy") // prevent spark assertion exception, not used
      .option("dbtable", "lineitem_small")
      .option("user", user)
      .option("password", password)
      .option("fetchsize", "1024")
      .option("numPartitions", 10)
      .load()

    dataset
      .rdd
      .mapPartitionsWithIndex{case (i,rows) => Iterator((i,rows.size))}
      .toDF("partition_number","number_of_records")
      .show

    /**
    +----------------+-----------------+
    |partition_number|number_of_records|
    +----------------+-----------------+
    |               0|          4266387|
    |               1|                0|
    |               2|          1124880|
    |               3|           403455|
    |               4|           694021|
    |               5|           639136|
    |               6|           656675|
    |               7|           612478|
    |               8|           857118|
    |               9|           673601|
    +----------------+-----------------+
     */
  }

  test("split with l_shipdate in 5") {
    val dataset = spark.read
      .format("jdbc.sample.splitter")
      .option("url", url)
      .option("partitionColumn", "l_shipdate")
      .option("lowerBound", "dummy") // prevent spark assertion exception, not used
      .option("upperBound", "dummy") // prevent spark assertion exception, not used
      .option("dbtable", "lineitem_small")
      .option("user", user)
      .option("password", password)
      .option("fetchsize", "1024")
      .option("numPartitions", 5)
      .load()

    dataset
      .rdd
      .mapPartitionsWithIndex{case (i,rows) => Iterator((i,rows.size))}
      .toDF("partition_number","number_of_records")
      .show

    /**
    +----------------+-----------------+
    |partition_number|number_of_records|
    +----------------+-----------------+
    |               0|          1200437|
    |               1|          1197858|
    |               2|          1189652|
    |               3|          1212573|
    |               4|          1200748|
    +----------------+-----------------+
     */
  }

  test("split with l_shipdate in 10") {
    val dataset = spark.read
      .format("jdbc.sample.splitter")
      .option("url", url)
      .option("partitionColumn", "l_shipdate")
      .option("lowerBound", "dummy") // prevent spark assertion exception, not used
      .option("upperBound", "dummy") // prevent spark assertion exception, not used
      .option("dbtable", "lineitem_small")
      .option("user", user)
      .option("password", password)
      .option("fetchsize", "1024")
      .option("numPartitions", 10)
      .load()

    dataset
      .rdd
      .mapPartitionsWithIndex{case (i,rows) => Iterator((i,rows.size))}
      .toDF("partition_number","number_of_records")
      .show

    /**
    +----------------+-----------------+
    |partition_number|number_of_records|
    +----------------+-----------------+
    |               0|           592779|
    |               1|           592442|
    |               2|           608232|
    |               3|           602439|
    |               4|           603437|
    |               5|           606055|
    |               6|           598513|
    |               7|           608959|
    |               8|           592433|
    |               9|           595981|
    +----------------+-----------------+
     */
  }

  test("split with l_orderkey in 5") {
    // if not set partitionColumn, auto select first column of table
    val dataset = spark.read
      .format("jdbc.sample.splitter")
      .option("url", url)
      .option("dbtable", "lineitem_small")
      .option("user", user)
      .option("password", password)
      .option("fetchsize", "1024")
      .option("numPartitions", 5)
      .load()

    dataset
      .rdd
      .mapPartitionsWithIndex{case (i,rows) => Iterator((i,rows.size))}
      .toDF("partition_number","number_of_records")
      .show

    /**
    +----------------+-----------------+
    |partition_number|number_of_records|
    +----------------+-----------------+
    |               0|          1205404|
    |               1|          1216587|
    |               2|          1209721|
    |               3|          1202692|
    |               4|          1166897|
    +----------------+-----------------+
     */
  }
}
