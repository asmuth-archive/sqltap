// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.channels.{SocketChannel,SelectionKey}
import java.nio.{ByteBuffer}

class HTTPConnection(sock: SocketChannel, worker: Worker) {

  val buf = ByteBuffer.allocate(4096) // FIXPAUL
  val parser = new HTTPParser()

  println("new http connection opened")

  def read(event: SelectionKey) : Unit = {
    var ready = false
    val chunk = sock.read(buf)

    if (chunk <= 0) {
      close()
      return
    }

    println("read ... bytes", chunk)

    try {
      ready = parser.read(buf)
    } catch {
      case e: HTTPParseError => return close()
    }

    if (ready) {
      execute_request()
      buf.clear

      worker.get_sql_connection()
      //event.interestOps(SelectionKey.OP_WRITE)
    }
  }

  private def close() : Unit = {
    println("connection closed")
    sock.close()
  }

  private def execute_request() : Unit = {
    println("request: ",
      parser.http_version,
      parser.http_method,
      parser.http_uri,
      parser.http_headers)
  }

}