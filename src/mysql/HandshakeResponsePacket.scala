// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class HandshakeResponsePacket(req: HandshakePacket) extends SQLClientIssuedPacket {

  var username = "root"
  var auth_resp = "ads123"

  def serialize : Array[Byte] = {
    val buf = new Array[Byte](34 + username.size)

    // cap flags: CLIENT_PROTCOL_41 | CLIENT_SECURE_CONNECTION
    buf(1) = 0x82.toByte

    // max packet size: 65535 byte
    buf(4) = 0xff.toByte
    buf(5) = 0xff.toByte

    // character set: utf-8
    buf(8) = 0x21.toByte

    val username_bytes = username.getBytes
    val auth_resp_bytes = auth_resp.getBytes

    // username
    System.arraycopy(username_bytes, 0, buf, 32, username_bytes.size)
    buf(32 + username_bytes.size) = 0

    /*System.arraycopy(auth_resp_bytes, 0, buf, 33 + username_bytes.size,
      auth_resp_bytes.size)

    buf(33 + username_bytes.size + auth_resp_bytes.size) = 0*/

    return buf
  }

}
