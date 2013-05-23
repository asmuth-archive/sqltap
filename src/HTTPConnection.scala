// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.channels.{SocketChannel}
import java.nio.{ByteBuffer}

class HTTPConnection(sock: SocketChannel, worker: Worker) {

  val buf = ByteBuffer.allocate(4096) // FIXPAUL
  val parser = new HTTPParser()

  println("new http connection opened")

  def read() : Unit = {
    val chunk = sock.read(buf)

    if (chunk <= 0) {
      close()
      return
    }

    println("read ... bytes", chunk)
    parser.read(buf)
    //buf.clear
  }

  def close() : Unit = {
    println("connection closed")
    sock.close()
  }

}
