package org.apache.spark.sql.sampling

import java.sql.{Date, ResultSet, SQLException, Timestamp}
import scala.collection.mutable.ListBuffer

case class JdbcSampleHandler(sqlType: Int) {
  val jdbcType: JdbcType = getJdbcType
  val getter: ResultSet => Any = jdbcType.getter
  val setter: Any => String = jdbcType.setter
  def sorted(list: ListBuffer[Any]): ListBuffer[Any] = jdbcType.sorted(list)

  private def getJdbcType: JdbcType = {
    sqlType match {
      case java.sql.Types.BIGINT        => IntegerType
      case java.sql.Types.SMALLINT      => IntegerType
      case java.sql.Types.TINYINT       => IntegerType
      case java.sql.Types.INTEGER       => IntegerType
      case java.sql.Types.DECIMAL       => BigDecimalType
      case java.sql.Types.NUMERIC       => BigDecimalType
      case java.sql.Types.DOUBLE        => FloatingPointType
      case java.sql.Types.FLOAT         => FloatingPointType
      case java.sql.Types.REAL          => FloatingPointType
      case java.sql.Types.CHAR          => StringType
      case java.sql.Types.CLOB          => StringType
      case java.sql.Types.LONGNVARCHAR  => StringType
      case java.sql.Types.LONGVARCHAR   => StringType
      case java.sql.Types.NCHAR         => StringType
      case java.sql.Types.NCLOB         => StringType
      case java.sql.Types.NVARCHAR      => StringType
      case java.sql.Types.REF           => StringType
      case java.sql.Types.SQLXML        => StringType
      case java.sql.Types.STRUCT        => StringType
      case java.sql.Types.VARCHAR       => StringType
      case java.sql.Types.DATE          => DateType
      case java.sql.Types.TIMESTAMP     => TimestampType
      case java.sql.Types.TIMESTAMP_WITH_TIMEZONE => TimestampType
      case _                            =>
        throw new SQLException("Not Supported SQL type for partitioning : " + sqlType)
    }
  }
}

object JdbcSampleHandler {
  def apply(rs: ResultSet): JdbcSampleHandler = {
    JdbcSampleHandler(rs.getMetaData.getColumnType(1))
  }

  def main(args: Array[String]): Unit = {
    val l: List[Any] = List(4,5,6,2,3)
    println(l.asInstanceOf[List[Int]].sorted)

  }
}

sealed trait JdbcType {
  def getter: ResultSet => Any = this match {
    case StringType => rs => rs.getString(1)
    case IntegerType => rs => rs.getLong(1)
    case FloatingPointType => rs => rs.getDouble(1)
    case BigDecimalType => rs => rs.getBigDecimal(1)
    case TimestampType => rs => rs.getTimestamp(1)
    case DateType => rs => rs.getDate(1)
  }

  def setter: Any => String = this match {
    case IntegerType => _.toString
    case FloatingPointType =>_.toString
    case BigDecimalType =>_.toString
    case StringType => any => s"'${any.toString}'"
    case TimestampType => any => s"'${any.toString}'"
    case DateType => any => s"'${any.toString}'"
  }

  def sorted(list: ListBuffer[Any]): ListBuffer[Any] = this match {
    case IntegerType => list.asInstanceOf[ListBuffer[Long]].sorted.asInstanceOf[ListBuffer[Any]]
    case FloatingPointType => list.asInstanceOf[ListBuffer[Double]].sorted.asInstanceOf[ListBuffer[Any]]
    case BigDecimalType => list.asInstanceOf[ListBuffer[BigDecimal]].sorted.asInstanceOf[ListBuffer[Any]]
    case StringType => list.asInstanceOf[ListBuffer[String]].sorted.asInstanceOf[ListBuffer[Any]]
    case TimestampType => list.asInstanceOf[ListBuffer[Timestamp]].sortBy(_.getTime).asInstanceOf[ListBuffer[Any]]
    case DateType => list.asInstanceOf[ListBuffer[Date]].sortBy(_.getTime).asInstanceOf[ListBuffer[Any]]
  }
}
private object StringType extends JdbcType
private object IntegerType extends JdbcType
private object FloatingPointType extends JdbcType
private object BigDecimalType extends JdbcType
private object TimestampType extends JdbcType
private object DateType extends JdbcType
