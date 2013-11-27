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

  var rows         = new ListBuffer[List[String]]()
  val timestamp    = ts
  val table_id     = read_table_id
  val flags        = read_flags
  val extra_data   = read_extra_data
  val num_cols     = read_num_cols
  val column_map1  = read_bitmap(num_cols)
  val column_map2  = read_bitmap(num_cols)

  def load(table: TableMapBinlogEvent) : Unit = {
    println(data.map{ x => x & 0x000000ff}.toList)
    println(table.table_name)

    val null_bitmap = read_bitmap(num_cols)
    println(null_bitmap)

    val row = for (col <- 0 until num_cols) {
      val column_value = bitmap_test(null_bitmap, col) match {
        case false => load_column(col, table.column_types(col))
        case true  => null
      }

      println("READ COL", col, table.column_types(col), cur, column_value, bitmap_test(null_bitmap, col), bitmap_test(column_map1, col))
    }
  }

  private def load_column(col: Int, column_type: Byte) : String = {
    if (!bitmap_test(column_map1, col)) {
      return null
    }

    column_type match {
      case 0x01 => read_int(1).toString
      case 0x03 => read_int(4).toString
      case 0x0a => read_date()
      case 0x0c => read_date()
      case 0x0f => read_varchar()
      case c: Byte => {
        throw new Exception("unknown mysql column type: " + c.toString)
      }
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

  private def read_int(bytes: Int) : Int = {
    val num = BinaryInteger.read(data, cur, bytes)
    cur += bytes
    num
  }

  private def read_date() : String = {
    val len = data(cur) & 0x000000ff
    //println("DATE LEN", data(cur - 1) & 0x000000ff)
    //println("DATE LEN", data(cur) & 0x000000ff)
    //println("DATE LEN", data(cur + 1) & 0x000000ff)
    cur    += 7
    "date"
  }

  private def read_varchar() : String = {
    val str = LengthEncodedString.read(data, cur)
    cur     = str._2
    str._1
  }

  private def read_bitmap(len: Int) : Integer = {
    val old = cur
    cur    += (len + 7) / 8
    old
  }

  private def bitmap_count(len: Int, pos: Int) : Int = {
    (0 until (len + 7) / 8).foldLeft(0) {
      (m, c) => m + Integer.bitCount(data(pos + c) & 0x000000ff)
    }
  }

  private def bitmap_test(pos: Int, bit: Int) : Boolean = {
    val byte = data(pos + (bit / 8)) & 0x000000ff
    (byte & (1 << (bit % 8))) > 0
  }

}
