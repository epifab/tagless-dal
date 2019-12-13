package io.epifab.tydal.runtime

import java.sql.{Connection, PreparedStatement}
import java.time.format.{DateTimeFormatter, DateTimeFormatterBuilder}

import io.epifab.tydal.schema._

import scala.util.Try
import scala.util.control.NonFatal

object SqlDateTime {
  val formatter: DateTimeFormatter = new DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd")
    .appendLiteral("T")
    .appendPattern("HH:mm:ss.SSSSSSSSS")
    .toFormatter()

  val parser: DateTimeFormatter = new DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd")
    .appendLiteral(" ")
    .appendPattern("HH:mm:ss")
    // optional nanos, with 9, 6 or 3 digits
    .appendPattern("[.SSSSSSSSS][.SSSSSSSS][.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]")
    .toFormatter()
}

object SqlDate {
  val formatter: DateTimeFormatter = new DateTimeFormatterBuilder()
    .appendPattern("yyyy-MM-dd")
    .toFormatter()

  val parser: DateTimeFormatter = formatter
}

object Jdbc {
  def initStatement(connection: Connection, sql: String, placeholderValues: Seq[Literal[_]]): Either[DriverError, PreparedStatement] =
    Try(connection.prepareStatement(sql)).toEither match {
      case Right(preparedStatement) =>
        placeholderValues.zipWithIndex.foreach {
          case (value, index) => setPlaceholder(
            connection,
            preparedStatement,
            index + 1,
            value.encoder.dbType,
            value.dbValue
          )
        }
        Right(preparedStatement)

      case Left(NonFatal(e)) =>
        Left(DriverError(e.getMessage))

      case Left(fatalError) =>
        throw fatalError
    }

  @scala.annotation.tailrec
  private def setPlaceholder[U, X](connection: Connection, statement: PreparedStatement, index: Int, dbType: FieldType[U], value: X): Unit = {
    dbType match {
      case TypeInt =>
        statement.setInt(index, value.asInstanceOf[Int])

      case TypeLong =>
        statement.setLong(index, value.asInstanceOf[Long])

      case TypeDouble =>
        statement.setDouble(index, value.asInstanceOf[Double])

      case TypeSeq(innerType) =>
        setPlaceholderSeq(connection, statement, index, innerType, value.asInstanceOf[Seq[_]])

      case TypeOption(innerDbType) =>
        value match {
          case Some(v) => setPlaceholder(connection, statement, index, innerDbType, v)
          case None => statement.setObject(index, null)
        }

      case _ => statement.setObject(index, value)
    }
  }

  private def setPlaceholderSeq[U](connection: Connection, statement: PreparedStatement, index: Int, dbType: FieldType[U], value: Seq[U]): Unit = {
    val array: java.sql.Array = connection.createArrayOf(
      dbType.sqlName,
      value.toArray
    )
    statement.setArray(index, array)
  }
}
