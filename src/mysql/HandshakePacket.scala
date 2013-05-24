// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class HandshakePacket(data: Array[Byte]) extends SQLServerIssuedPacket {

  private var str_offset = 1

  if (data(0) != 0x0a)
    throw new SQLProtocolError("unsupported mysql protocol version: " + data(0).toInt)

  while (str_offset < data.size && data(str_offset) != 0)
    str_offset += 1

  if (data.size < str_offset + 17)
    throw new SQLProtocolError("missing mysql capability flags")

  println("FUUUUU",  (data(str_offset + 14)), ((data(str_offset + 14) & 0x0200)))

  // yuck, unsigned bytes are a big ugly hack in java... ~paul
  if ((0x000000ff & (data(str_offset + 14) & 0x02)) == 0)
    throw new SQLProtocolError("mysql server does not support CLIENT_PROTOCOL_41")

}
