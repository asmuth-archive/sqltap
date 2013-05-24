// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class HandshakeResponsePacket() extends SQLClientIssuedPacket {

  var username = "root"

  def serialize : Array[Byte] = {
    val buf = new Array[Byte](33 + username.size)

    // cap flags: CLIENT_PROTCOL_41
    buf(0) = 0x0.toByte

    // max packet size: 65535 byte
    buf(4) = 0xff.toByte
    buf(5) = 0xff.toByte

    // character set: utf-8
    buf(8) = 0x21.toByte

    // username
    System.arraycopy(username.getBytes, 0, buf, 32, username.size)
    buf(32 + username.size) = 0

    return buf
  }

}
