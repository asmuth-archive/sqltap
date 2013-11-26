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
    cur = 22; BinaryInteger.read(data, 18, 4)
  } else {
    cur = 24; BinaryInteger.read(data, 18, 6)
  }
}
