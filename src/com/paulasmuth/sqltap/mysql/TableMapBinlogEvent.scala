// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class TableMapBinlogEvent(data: Array[Byte], ts: Long, fmt: FormatDescriptionBinlogEvent) extends BinlogEvent {
  private var cur      = 0
  private var meta_cur = 0

  val timestamp    = ts
  val table_id     = read_table_id
  val schema_name  = read_null_string
  val table_name   = read_null_string
  val column_count = read_column_count
  val column_types = read_column_types

  private def read_column_types() : IndexedSeq[Byte] = {
    for (c <- 0 until column_count) yield data(cur + c)
  }

  private def read_table_id() : Int = {
    if (fmt.header_length(0x13) == 6) {
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

  private def read_null_string() : String = {
    val str = BinaryString.read_null(data, cur + 1)
    cur     = str._2
    str._1
  }

  private def read_column_count() : Int = {
    val count = LengthEncodedInteger.read(data, cur)
    cur       = count._2
    meta_cur  = cur + count._1
    count._1
  }

}
