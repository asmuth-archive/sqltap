// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.channels.{SocketChannel,SelectionKey}
import java.nio.{ByteBuffer}
import java.net.{InetSocketAddress,ConnectException}

class SQLConnection(worker: Worker) {

  val SQL_STATE_SYN  = 1
  val SQL_STATE_ACK  = 2
  val SQL_STATE_AUTH = 3

  private var state : Int = 0

  private val sock = SocketChannel.open()
  sock.configureBlocking(false)

  def connect() : Unit = {
    val addr = new InetSocketAddress("127.0.0.1", 3306)
    sock.connect(addr)
    state = SQL_STATE_SYN

    sock
      .register(worker.loop, SelectionKey.OP_CONNECT)
      .attach(this)
  }

  def ready(event: SelectionKey) : Unit = {
    try {
      sock.finishConnect
    } catch {
      case e: ConnectException => {
        SQLTap.error("SQL connection failed: " + e.toString, false)
        return close()
      }
    }

    event.interestOps(SelectionKey.OP_READ)
  }

  def close() : Unit = {
    sock.close()
  }

}
