// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

import com.paulasmuth.sqltap.{SQLTap,Worker}
import java.nio.channels.{SocketChannel,SelectionKey}
import java.nio.{ByteBuffer,ByteOrder}
import java.net.{InetSocketAddress,ConnectException}

class SQLConnection(worker: Worker) {

  var hostname : String = "127.0.0.1"
  var port     : Int    = 3306
  var username : String = "root"
  var password : String = ""
  var database : String = ""

  private val SQL_STATE_SYN     = 1
  private val SQL_STATE_ACK     = 2
  private val SQL_STATE_OLDAUTH = 3
  private val SQL_STATE_IDLE    = 4
  private val SQL_STATE_QINIT   = 5
  private val SQL_STATE_QCOL    = 6
  private val SQL_STATE_QROW    = 7
  private val SQL_STATE_CLOSE   = 8

  // max packet length: 16mb
  private val SQL_MAX_PKT_LEN   = 16777215
  private val SQL_WRITE_BUF_LEN = 4096
  private val SQL_READ_BUF_LEN  = 65535

  private var state : Int = 0
  private val read_buf = ByteBuffer.allocate(SQL_READ_BUF_LEN)
  private val write_buf = ByteBuffer.allocate(SQL_WRITE_BUF_LEN)
  write_buf.order(ByteOrder.LITTLE_ENDIAN)

  private val sock = SocketChannel.open()
  sock.configureBlocking(false)

  private var initial_handshake : HandshakePacket = null

  private var cur_seq : Int = 0
  private var cur_len : Int = 0

  def connect() : Unit = {
    val addr = new InetSocketAddress(hostname, port)
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

        cur_len  = BinaryInteger.read(read_buf.array, 0, 3)
        cur_seq  = BinaryInteger.read(read_buf.array, 3, 1)

        if (cur_len == SQL_MAX_PKT_LEN) {
          SQLTap.error("[SQL] packets > 16mb are currently not supported", false)
          return close()
        }
      }

      if (read_buf.position < 4 + cur_len)
        return

      val pkt = new Array[Byte](cur_len)
      val nxt = new Array[Byte](read_buf.position - cur_len - 4)

      read_buf.flip()
      read_buf.position(4)
      read_buf.get(pkt)
      read_buf.get(nxt)
      read_buf.clear()
      read_buf.put(nxt)

      try {
        next(event, pkt)
      } catch {
        case e: SQLProtocolError => {
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
    state = SQL_STATE_CLOSE
    sock.close()
  }

  private def next(event: SelectionKey, pkt: Array[Byte]) : Unit = {

    // err packet
    if ((pkt(0) & 0x000000ff) == 0xff)
      packet_err(event, pkt)

    // ok packet
    else if ((pkt(0) & 0x000000ff) == 0x00)
      packet_ok(event, pkt)

    // eof packet
    else if ((pkt(0) & 0x000000ff) == 0xfe && pkt.size == 5)
      packet_eof(event, pkt)

    // other packets
    else
      packet(event, pkt)

  }

  private def packet(event: SelectionKey, pkt: Array[Byte]) : Unit = state match {

    case SQL_STATE_SYN => {
      initial_handshake = new HandshakePacket()
      initial_handshake.load(pkt)
      state = SQL_STATE_ACK
      authenticate()

      event.interestOps(SelectionKey.OP_WRITE)
    }

    case SQL_STATE_ACK => {
      if (pkt.size == 1 && (pkt(0) & 0x000000ff) == 0xfe) {
        SQLTap.log_debug("[SQL] switching to mysql old authentication")

        state = SQL_STATE_OLDAUTH
        authenticate()
        state = SQL_STATE_ACK

        event.interestOps(SelectionKey.OP_WRITE)
      } else {
        SQLTap.error("received invalid packet in SQL_STATE_ACK", false)
      }
    }

    case SQL_STATE_QINIT => {
      val field_count = LengthEncodedInteger.read(pkt)
      println("NUM FIELDS: " + field_count)

      state = SQL_STATE_QCOL
    }

    case SQL_STATE_QCOL => {
      val col_def = new ColumnDefinition
      col_def.load(pkt)
    }

    case SQL_STATE_QROW => {
      println("field-data", javax.xml.bind.DatatypeConverter.printHexBinary(pkt))
    }

  }

  private def packet_ok(event: SelectionKey, pkt: Array[Byte]) : Unit = state match {

    case SQL_STATE_ACK => {
      SQLTap.log_debug("[SQL] connection established!")
      cur_seq = 0
      state = SQL_STATE_IDLE

      // STUB
      write_query("select id, username from users where id = 1;")
      event.interestOps(SelectionKey.OP_WRITE)
      state = SQL_STATE_QINIT
      // EOF STUB
    }

  }

  private def packet_eof(event: SelectionKey, pkt: Array[Byte]) : Unit = state match {

    case SQL_STATE_QCOL => {
      println("FIELD LIST COMPLETE")
      state = SQL_STATE_QROW
    }

    case SQL_STATE_QROW => {
      println("QUERY RESPONSE COMPLETE")
      state = SQL_STATE_IDLE
      println("SQL_CONN_IDLE")
    }

  }

  private def packet_err(event: SelectionKey, pkt: Array[Byte]) : Unit = {
    val err_msg  = BinaryString.read(pkt, 9, pkt.size - 9)
    val err_code = BinaryInteger.read(pkt, 0, 2)

    SQLTap.error("[SQL] error (" + err_code + "): " + err_msg, false)

    close()
  }


  private def authenticate() : Unit = state match {

    case SQL_STATE_ACK => {
      val auth_pkt = new HandshakeResponsePacket(initial_handshake)
      auth_pkt.set_username(username)

      if (database.length > 0)
        auth_pkt.set_database(database)

      if (password.length > 0)
        auth_pkt.set_auth_resp(
          SecurePasswordAuthentication.auth(initial_handshake, password))

      write_packet(auth_pkt)
    }

    case SQL_STATE_OLDAUTH => {
      val auth_pkt = new AuthSwitchResponsePacket()
      println("write auth switch response")

      auth_pkt.set_auth_resp(
        OldPasswordAuthentication.auth(initial_handshake, password))

      write_packet(auth_pkt)
    }

  }

  private def write_packet(packet: SQLClientIssuedPacket) = {
    cur_seq += 1
    write_buf.clear

    // write packet len
    write_buf.putShort(packet.length.toShort)
    write_buf.put(0x00.toByte)

    // write sequence number
    write_buf.put(cur_seq.toByte)

    packet.write(write_buf)
    write_buf.flip
  }

  private def write_query(query_str: String) = {
    val query = query_str.getBytes
    write_buf.clear

    // write packet len (query len + 1 byte opcode)
    write_buf.putShort((query.size + 1).toShort)
    write_buf.put(0.toByte)

    // write sequence number
    write_buf.put(cur_seq.toByte)

    // write opcode COMM_QUERY (0x03)
    write_buf.put(0x03.toByte)

    // write query string
    write_buf.put(query)

    cur_seq += 1
    write_buf.flip
  }


}
