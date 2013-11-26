// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql
import scala.collection.mutable.{HashMap,ListBuffer}

class UpdateRowsBinlogEvent(data: Array[Byte], ts: Long, fmt: FormatDescriptionBinlogEvent) extends BinlogEvent {
  private var cur = 0

  val timestamp   = ts
  val table_id    = read_table_id
  val flags       = read_flags
  val extra_data  = read_extra_data
  val num_cols    = read_num_cols
  val column_map  = read_column_map
  val column_set  = Integer.bitCount(column_map)
  var rows        = new ListBuffer[List[String]]()

  def load(table: TableMapBinlogEvent) : Unit = {
    val row = for (col <- 0 until num_cols) yield
      load_column(col, table.column_types(col))

    println(row)
  }

  private def load_column(col: Int, column_type: Byte) : String = {
    if ((column_map & (1 << col)) == 0) {
      return null
    }

    column_type match {
      case 0x01 => "fnord"
      case _    => "fnord"
    }
  }

  private def read_table_id() : Int = {
    if (fmt.header_length(0x1f) == 6) {
      cur = 24
      BinaryInteger.read(data, 20, 4)
    } else {
      cur = 26
      BinaryInteger.read(data, 20, 6)
    }
  }

  private def read_flags() : Int = {
    cur += 2
    BinaryInteger.read(data, cur - 2, 2)
  }

  private def read_extra_data() : String = {
    val len = BinaryInteger.read(data, cur, 2)
    cur += len

    if (len > 2) {
      BinaryString.read(data, cur + 2 - len, len - 2)
    } else {
      null
    }
  }

  private def read_num_cols() : Int = {
    val num = LengthEncodedInteger.read(data, cur)
    cur     = num._2
    num._1
  }

  private def read_column_map() : Int = {
    val len = (num_cols + 7) / 8
    val map = BinaryInteger.read(data, cur, len)
    cur    += len
    map
  }

}
