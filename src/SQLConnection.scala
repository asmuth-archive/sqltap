// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.channels.{SocketChannel,SelectionKey}
import java.nio.{ByteBuffer,ByteOrder}
import java.net.{InetSocketAddress,ConnectException}

class SQLConnection(worker: Worker) {

  val SQL_STATE_SYN   = 1
  val SQL_STATE_ACK   = 2
  val SQL_STATE_AUTH  = 3
  val SQL_STATE_EST   = 4
  val SQL_STATE_QUERY = 5

  val SQL_MAX_PKT_LEN = 16777215

  private var state : Int = 0
  private val read_buf = ByteBuffer.allocate(4096) // FIXPAUL
  private val write_buf = ByteBuffer.allocate(4096) // FIXPAUL
  write_buf.order(ByteOrder.LITTLE_ENDIAN)

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
        SQLTap.error("[SQL] connection failed: " + e.toString, false)
        return close()
      }
    }

    event.interestOps(SelectionKey.OP_READ)
  }

  def read(event: SelectionKey) : Unit = {
    val chunk = sock.read(read_buf)

    if (chunk <= 0) {
      SQLTap.error("[SQL] read end of file ", false)
      close()
      return
    }

    while (read_buf.position > 0) {
      if (cur_len == 0) {
        if (read_buf.position < 4)
          return

        cur_len  = read_buf.get(0)
        cur_len += read_buf.get(1) << 8
        cur_len += read_buf.get(2) << 16
        cur_seq  = read_buf.get(3)

        if (cur_len == SQL_MAX_PKT_LEN) {
          SQLTap.error("[SQL] packets > 16mb are currently not supported", false)
          return close()
        }
      }

      if (read_buf.position < 4 + cur_len)
        return

      println(cur_len)
      val pkt = new Array[Byte](cur_len)
      val nxt = new Array[Byte](read_buf.position - cur_len - 4)

      read_buf.flip()
      read_buf.position(4)
      read_buf.get(pkt)
      read_buf.get(nxt)
      read_buf.clear()
      read_buf.put(nxt)

      try {
        packet(event, pkt)
      } catch {
        case e: mysql.SQLProtocolError => {
          SQLTap.error("[SQL] protocol error: " + e.toString, false)
          return close()
        }
      }

      cur_seq = 0
      cur_len = 0
    }
  }

  def write(event: SelectionKey) : Unit = {
    try {
      sock.write(write_buf)
    } catch {
      case e: Exception => {
        SQLTap.error("[SQL] conn error: " + e.toString, false)
        return close()
      }
    }

    if (write_buf.remaining == 0) {
      write_buf.clear
      event.interestOps(SelectionKey.OP_READ)
      println("write ready!")
    }
  }

  def close() : Unit = {
    println("sql connection closed")
    sock.close()
  }

  private def packet(event: SelectionKey, pkt: Array[Byte]) : Unit = {

    if ((pkt(0) & 0x000000ff) == 0xff) {
      val err_msg = new String(pkt, 9, pkt.size - 9, "UTF-8")
      var err_code = (pkt(0) & 0x000000ff) + ((pkt(1) & 0x000000ff) << 8)

      SQLTap.error("[SQL] error (" + err_code + "): " + err_msg, false)
      return close()
    }

    else if ((pkt(0) & 0x000000ff) == 0x00) {
      println("!!!!!OK PACKET!!!!!")

      state match {

        case SQL_STATE_ACK => {
          println("connection established!")
          state = SQL_STATE_EST

          cur_seq = 0
          write_query("select version();")
          event.interestOps(SelectionKey.OP_WRITE)
          state = SQL_STATE_QUERY

        }

      }

      return
    }

    state match {

      case SQL_STATE_SYN => {
        val handshake_req = new mysql.HandshakePacket(pkt)
        val handshake_res = new mysql.HandshakeResponsePacket(handshake_req)

        handshake_res.username = "fnordinator"

        write_packet(handshake_res.serialize)
        state = SQL_STATE_ACK
        event.interestOps(SelectionKey.OP_WRITE)

        println(javax.xml.bind.DatatypeConverter.printHexBinary(handshake_res.serialize))
      }

    }
  }

  private def write_packet(data: Array[Byte]) = {
    cur_seq += 1
    write_buf.clear
    write_buf.putShort(data.size.toShort)
    write_buf.put(0.toByte)
    write_buf.put(cur_seq.toByte)
    write_buf.put(data)
    write_buf.flip
  }

  private def write_query(query: String) = {
    write_buf.clear
    write_buf.putShort((query.size + 1).toShort)
    write_buf.put(0.toByte)
    write_buf.put(cur_seq.toByte)
    write_buf.put(0x03.toByte)
    write_buf.put(query.getBytes)
    write_buf.flip
    cur_seq += 1
  }

}
