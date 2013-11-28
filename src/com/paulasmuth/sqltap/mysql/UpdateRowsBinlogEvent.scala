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
    load_row_part(table, column_map1)
    System.exit(0)
  }

  private def load_row_part(table: TableMapBinlogEvent, colmap: Int) : IndexedSeq[(Int, String)] = {
    val cols_present = bitmap_count(colmap, num_cols)
    val null_bitmap  = read_bitmap(cols_present)
    def test(c: Int) = bitmap_test(colmap, c) && !bitmap_test(null_bitmap, c)

    for (col <- 0 until num_cols)
      yield (test(col)) match {
        case true  => ((col, load_column(col, table.column_types(col))))
        case false => ((col, null))
      }
  }

  private def load_column(col: Int, column_type: Byte) : String = {
    val x = column_type match {
      case 0x01 => read_int(1).toString       // 0x01 TINY
      case 0x02 => read_int(2).toString       // 0x02 SHORT
      case 0x03 => read_int(4).toString       // 0x03 LONG
      case 0x04 => read_float(4).toString     // 0x04 FLOAT
      case 0x05 => read_float(8).toString     // 0x05 DOUBLE
      case 0x06 => null                       // 0x06 NULL
      case 0x07 => read_int(4).toString       // 0x07 TIMESTAMP
      case 0x08 => read_int(8).toString       // 0x08 LONGLONG
      case 0x09 => read_int(3).toString       // 0x09 INT24
      case 0x0a => read_date(3)               // 0x0a DATE
      case 0x0b => read_date(3)               // 0x0b TIME
      case 0x0c => read_date(8)               // 0x0c DATETIME
      case 0x0d => read_date(1)               // 0x0d YEAR
      case 0x0f => read_varchar()             // 0x0f VARCHAR
      case c: Byte => {
        throw new Exception("unknown mysql column type: " + c.toString)
      }
    }

    println("READ COL", cur, col, column_type, x)
    x
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

  private def read_date(len: Int) : String = {
    //println(java.util.Arrays.copyOfRange(data, cur, cur + 11).toList.map{x=>x&0x000000ff})
    //println("DATE LEN", data(cur - 1) & 0x000000ff)
    //println("DATE LEN", data(cur) & 0x000000ff)
    //println("DATE LEN", data(cur + 1) & 0x000000ff)
    cur    += len
    "date" // FIXPAUL
  }

  private def read_varchar() : String = {
    val str = LengthEncodedString.read(data, cur)
    cur     = str._2
    str._1
  }

  private def read_float(len: Int) : Double = {
    val value = read_int(len)
    java.lang.Double.longBitsToDouble(value) // FIXPAUL
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
