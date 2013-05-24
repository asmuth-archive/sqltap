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

class SQLError extends Exception

class SQLConnection(worker: Worker) {

  val SQL_STATE_SYN  = 1
  val SQL_STATE_ACK  = 2
  val SQL_STATE_AUTH = 3

  private var state : Int = 0
  private val buf = ByteBuffer.allocate(4096) // FIXPAUL

  private val sock = SocketChannel.open()
  sock.configureBlocking(false)

  private var cur_seq : Int = 0
  private var cur_len : Int = 0

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

  def read(event: SelectionKey) : Unit = {
    val chunk = sock.read(buf)

    if (chunk <= 0) {
      close()
      return
    }

    while (buf.position > 0) {
      if (cur_len == 0) {
        if (buf.position < 4)
          return

        cur_len  = buf.get(0) - 20
        cur_len += buf.get(1) << 8
        cur_len += buf.get(2) << 16
        cur_seq  = buf.get(3)
      }

      if (buf.position < 4 + cur_len)
        return

      val pkt = new Array[Byte](cur_len)
      val nxt = new Array[Byte](buf.position - cur_len - 4)

      buf.flip()
      buf.position(4)
      buf.get(pkt)
      buf.get(nxt)
      buf.clear()
      buf.put(nxt)

      packet(pkt)

      cur_seq = 0
      cur_len = 0
    }
  }

  def close() : Unit = {
    println("sql connection closed")
    sock.close()
  }

  private def packet(pkt: Array[Byte]) : Unit = {
    println(javax.xml.bind.DatatypeConverter.printHexBinary(pkt))

    if (pkt(0) != 0x0a) {
      SQLTap.error("unsupported mysql protocol version: " + pkt(0).toInt, true)
      close()
    }
  }

}
