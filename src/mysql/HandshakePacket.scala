// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class HandshakePacket(data: Array[Byte]) extends SQLServerIssuedPacket {

  private var str_offset = 1
  private var capabilities : Long = 0
  private var authp_len : Int = 0

  if (data(0) != 0x0a)
    throw new SQLProtocolError("unsupported mysql protocol version: " + data(0).toInt)

  while (str_offset < data.size && data(str_offset) != 0)
    str_offset += 1

  if (data.size < str_offset + 17)
    throw new SQLProtocolError("missing mysql capability flags")

  // yuck, unsigned bytes are a big ugly hack in java... ~paul
  capabilities += (data(str_offset + 14) & 0x000000ff).toLong
  capabilities += (data(str_offset + 15) & 0x000000ff) << 8L

  if ((capabilities & 0x02) == 0)
    throw new SQLProtocolError("mysql server does not support CLIENT_PROTOCOL_41")

  if (data.size <= str_offset + 16)
    throw new SQLProtocolError("mysql server version too old")

  capabilities += (data(str_offset + 19) & 0x000000ff) << 16L
  capabilities += (data(str_offset + 20) & 0x000000ff) << 24L

  if ((capabilities & 0x8000) != 0x8000)
    throw new SQLProtocolError("mysql server does not support CLIENT_SECURE_CONNECTION")

  //if ((capabilities & 0x80000) == 0x80000)
  //  throw new SQLProtocolError("CLIENT_AUTH_PLUGIN is not supported")

    //authp_len = (data(str_offset + 21) & 0x000000ff)


  println("CAPA", capabilities)
}
