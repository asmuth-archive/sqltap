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

  private val buf = ByteBuffer.allocate(4096) // FIXPAUL
  private val parser = new HTTPParser()

  println("new http connection opened")

  def read(event: SelectionKey) : Unit = {
    var ready = false
    val chunk = sock.read(buf)

    if (chunk <= 0) {
      close()
      return
    }

    try {
      ready = parser.read(buf)
    } catch {
      case e: HTTPParseError => return close()
    }

    if (ready) {
      execute_request()
      buf.clear

      // STUB
      for (n <- (1 to 20)) worker.execute_sql_query(new SQLQuery("select id, username, email from users where id < 3;"))
      //EOF STUB

      //event.interestOps(SelectionKey.OP_WRITE)
    }
  }

  def close() : Unit = {
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
