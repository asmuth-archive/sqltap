// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class TableMapBinlogEvent(data: Array[Byte], ts: Long, fmt: FormatDescriptionBinlogEvent) extends BinlogEvent {
  var cur       = 0
  val timestamp = ts

  val table_id  = if (fmt.header_length(0x13) == 6) {
    cur = 26; BinaryInteger.read(data, 20, 4)
  } else {
    cur = 28; BinaryInteger.read(data, 20, 6)
  }

  private val schema_name_  = BinaryString.read_null(data, cur + 1)
  val schema_name  = schema_name_._1
  cur              = schema_name_._2

  private val table_name_  = BinaryString.read_null(data, cur + 1)
  val table_name   = table_name_._1
  cur              = table_name_._2

  private val column_count_ = LengthEncodedInteger.read(data, cur)
  val column_count = column_count_._1
  cur              = column_count_._2

  val column_def  = BinaryString.read(data, cur, column_count)
  cur            += column_count

  private val column_meta_ = LengthEncodedString.read(data, cur)
  val column_meta = column_meta_._1
  cur             = column_meta_._2

  val null_bitmap = BinaryInteger.read(data, cur, (column_count + 7) / 8)
}
