package org.apache.spark.sql.sampling

import java.sql.{Date, ResultSet, SQLException, Timestamp}
import scala.collection.mutable.ListBuffer

case class JdbcSampleHandler(sqlType: Int) {
  val jdbcType: JdbcType = getJdbcType
  val getter: ResultSet => Any = jdbcType.getter
  val setter: Any => String = jdbcType.setter
  val ordering: Ordering[Any] = jdbcType.ordering

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
}

private[sampling] sealed trait JdbcType {
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

  def ordering: Ordering[Any] = this match {
    case StringType => Ordering.by(_.asInstanceOf[String])
    case IntegerType => Ordering.by(_.asInstanceOf[Long])
    case FloatingPointType => Ordering.by(_.asInstanceOf[Double])
    case BigDecimalType => Ordering.by(_.asInstanceOf[BigDecimal])
    case TimestampType => Ordering.by(_.asInstanceOf[Timestamp].getTime)
    case DateType => Ordering.by(_.asInstanceOf[Date].getTime)
  }
}
private object StringType extends JdbcType
private object IntegerType extends JdbcType
private object FloatingPointType extends JdbcType
private object BigDecimalType extends JdbcType
private object TimestampType extends JdbcType
private object DateType extends JdbcType
