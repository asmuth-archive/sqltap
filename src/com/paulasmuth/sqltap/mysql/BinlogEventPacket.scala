// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

object BinlogEventPacket {

  def load(data: Array[Byte]) : BinlogEvent = {
    val timestamp  = BinaryInteger.read(data, 1, 4)
    val event_type = data(5)
    val server_id  = BinaryInteger.read(data, 6, 4)
    val event_size = BinaryInteger.read(data, 10, 4)
    val log_pos    = BinaryInteger.read(data, 14, 4)
    val flags      = BinaryInteger.read(data, 16, 2)

    event_type match {
      case 0x18 => new UpdateRowsBinlogEvent(data, timestamp)
      case _    => new UnknownBinlogEvent(data, timestamp)
    }
  }

}
