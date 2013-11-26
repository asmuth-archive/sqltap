// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class UpdateRowsBinlogEvent(data: Array[Byte], ts: Long, fmt: FormatDescriptionBinlogEvent) extends BinlogEvent {
  var cur       = 0
  val timestamp = ts

  val table_id  = if (fmt.header_length(0x1f) == 6) {
    cur = 24; BinaryInteger.read(data, 20, 4)
  } else {
    cur = 26; BinaryInteger.read(data, 20, 6)
  }

  val flags = BinaryInteger.read(data, cur, 2)
  cur += 2

  val extra_len  = BinaryInteger.read(data, cur, 2)
  val extra_data = if (extra_len > 2) BinaryString.read(data, cur + 2, extra_len - 2) else null
  cur += extra_len

  private val num_cols_ = LengthEncodedInteger.read(data, cur)
  val num_cols = num_cols_._1
  cur          = num_cols_._2

  val columns_present_map1 = BinaryInteger.read(data, cur, (num_cols + 7) / 8)
  val columns_present_num1 = Integer.bitCount(columns_present_map1)
  cur += (num_cols + 7) / 8

  val columns_present_map2 = BinaryInteger.read(data, cur, (num_cols + 7) / 8)
  val columns_present_num2 = Integer.bitCount(columns_present_map2)
  cur += (num_cols + 7) / 8

  println(columns_present_num1, columns_present_num2)
}
