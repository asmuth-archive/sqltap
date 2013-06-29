// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}
import java.nio.channels.{SocketChannel,SelectionKey}
import java.nio.{ByteBuffer,ByteOrder}
import java.net.{InetSocketAddress,ConnectException}

class MemcacheConnection(pool: MemcacheConnectionPool) {

  var hostname  : String  = "127.0.0.1"
  var port      : Int     = 11211

  private val MC_STATE_INIT  = 0
  private val MC_STATE_CONN  = 1
  private val MC_STATE_IDLE  = 2
  private val MC_STATE_WRITE = 4
  private val MC_STATE_READ  = 5
  private val MC_STATE_CLOSE = 6

  private val MC_WRITE_BUF_LEN  = 4096
  private val MC_READ_BUF_LEN   = 65535

  private var state = MC_STATE_INIT
  private var last_event : SelectionKey = null
  private val read_buf = ByteBuffer.allocate(MC_READ_BUF_LEN)
  private val write_buf = ByteBuffer.allocate(MC_WRITE_BUF_LEN)
  write_buf.order(ByteOrder.LITTLE_ENDIAN)

  private val sock = SocketChannel.open()
  sock.configureBlocking(false)

  def connect() : Unit = {
    Statistics.incr('memcache_connections_open)

    println("memcache connect")
    val addr = new InetSocketAddress(hostname, port)
    sock.connect(addr)
    state = MC_STATE_CONN

    sock
      .register(pool.loop, SelectionKey.OP_CONNECT)
      .attach(this)
  }

  def execute_flush() : Unit = {
    println("!!FLISH")

    write_buf.clear
    write_buf.put("flush_all\r\nflush_all\r\nflush_all\r\n".getBytes)
    write_buf.flip

    state = MC_STATE_WRITE
    last_event.interestOps(SelectionKey.OP_WRITE)
  }


  def ready(event: SelectionKey) : Unit = {
    try {
      sock.finishConnect
    } catch {
      case e: ConnectException => {
        Logger.error("[Memcache] connection failed: " + e.toString, false)
        return close(e)
      }
    }

    idle(event)
  }

  def read(event: SelectionKey) : Unit = {
    println("read")
    val chunk = sock.read(read_buf)

    if (chunk <= 0) {
      Logger.error("[Memcache] read end of file ", false)
      close(new ExecutionException("memcache connection closed"))
      return
    }

    println("read...")

    var cur = 0
    var pos = 0

    while (cur < read_buf.position) {
      if (read_buf.get(cur) == 10) {
        val line = new String(read_buf.array, pos, cur, "UTF-8")
        println("....!", line)

        pos = cur
      }

      println(pos, cur, read_buf.get(cur))
      cur += 1
    }

    if (cur < read_buf.position) {
      read_buf.limit(read_buf.position)
      read_buf.position(cur)
      read_buf.compact()
    }
  }

  def write(event: SelectionKey) : Unit = {
    println("write")
    try {
      sock.write(write_buf)
    } catch {
      case e: Exception => {
        Logger.error("[Memcache] conn error: " + e.toString, false)
        return close(e)
      }
    }

    if (write_buf.remaining == 0) {
      state = MC_STATE_READ
      write_buf.clear
      event.interestOps(SelectionKey.OP_READ)
    }
  }

  def close(err: Throwable = null) : Unit = {
    if (state == MC_STATE_CLOSE)
      return

    //if (cur_qry != null)
    //  cur_qry.error(err)

    state = MC_STATE_CLOSE

    pool.close(this)
    sock.close()
    Statistics.decr('sql_connections_open)
  }


  private def idle(event: SelectionKey) : Unit = {
    println("memcache idle")
    state = MC_STATE_IDLE
    event.interestOps(0)
    last_event = event
    pool.ready(this)
  }


}
