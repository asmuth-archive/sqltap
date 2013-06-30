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

class MemcacheConnection(pool: MemcacheConnectionPool) extends TimeoutCallback {

  var hostname  : String  = "127.0.0.1"
  var port      : Int     = 11211

  private val MC_STATE_INIT       = 0
  private val MC_STATE_CONN       = 1
  private val MC_STATE_IDLE       = 2
  private val MC_STATE_CMD_DELETE = 4
  private val MC_STATE_CMD_SET    = 5
  private val MC_STATE_CMD_MGET   = 6
  private val MC_STATE_READ       = 7
  private val MC_STATE_CLOSE      = 8

  private val MC_WRITE_BUF_LEN  = 65535
  private val MC_READ_BUF_LEN   = 65535

  private var state = MC_STATE_INIT
  private var last_event : SelectionKey = null
  private val read_buf = ByteBuffer.allocate(MC_READ_BUF_LEN)
  private val write_buf = ByteBuffer.allocate(MC_WRITE_BUF_LEN)
  write_buf.order(ByteOrder.LITTLE_ENDIAN)

  private val sock = SocketChannel.open()
  sock.configureBlocking(false)

  private var timer = TimeoutScheduler.schedule(1000, this)
  private var requests : List[CacheRequest] = null
  private var cur_buf  : ElasticBuffer = null
  private var cur_len  = 0

  def connect() : Unit = {
    Statistics.incr('memcache_connections_open)

    val addr = new InetSocketAddress(hostname, port)
    sock.connect(addr)
    state = MC_STATE_CONN

    timer.start()

    sock
      .register(pool.loop, SelectionKey.OP_CONNECT)
      .attach(this)
  }

  def execute_mget(keys: List[String], _requests: List[CacheRequest]) : Unit = {
    requests = _requests

    if (state != MC_STATE_IDLE)
      throw new ExecutionException("memcache connection busy")

    timer.start()

    write_buf.clear
    write_buf.put("get".getBytes)

    for (key <- keys) {
      write_buf.put(32.toByte)
      write_buf.put(key.getBytes("UTF-8"))
    }

    write_buf.put(13.toByte)
    write_buf.put(10.toByte)
    write_buf.flip

    state = MC_STATE_CMD_MGET
    last_event.interestOps(SelectionKey.OP_WRITE)
  }

  def execute_set(key: String, request: CacheStoreRequest) : Unit = {
    val buf = request.buffer.buffer
    val len = buf.position

    request.ready()
    buf.position(0)
    buf.limit(len)

    if (state != MC_STATE_IDLE)
      throw new ExecutionException("memcache connection busy")

    timer.start()

    write_buf.clear
    write_buf.put("set".getBytes)
    write_buf.put(32.toByte)
    write_buf.put(key.getBytes("UTF-8"))
    write_buf.put(32.toByte)
    write_buf.put(48.toByte)
    write_buf.put(32.toByte)
    write_buf.put(48.toByte)
    write_buf.put(32.toByte)
    write_buf.put(len.toString.getBytes("UTF-8"))
    write_buf.put(13.toByte)
    write_buf.put(10.toByte)
    write_buf.put(buf)
    write_buf.put(13.toByte)
    write_buf.put(10.toByte)
    write_buf.flip

    state = MC_STATE_CMD_SET
    last_event.interestOps(SelectionKey.OP_WRITE)
  }

  def execute_delete(key: String) : Unit = {
    if (state != MC_STATE_IDLE)
      throw new ExecutionException("memcache connection busy")

    timer.start()

    write_buf.clear
    write_buf.put("delete".getBytes)
    write_buf.put(32.toByte)
    write_buf.put(key.getBytes("UTF-8"))
    write_buf.put(13.toByte)
    write_buf.put(10.toByte)
    write_buf.flip

    state = MC_STATE_CMD_DELETE
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
    val chunk = sock.read(read_buf)

    if (chunk <= 0) {
      Logger.error("[Memcache] read end of file ", false)
      close(new ExecutionException("memcache connection closed"))
      return
    }

    var cur = 0
    var pos = 0

    while (cur < read_buf.position) {
      if (state == MC_STATE_READ) {
        cur = math.min(read_buf.position, pos + cur_len)

        cur_len -= cur - pos
        cur_buf.write(read_buf.array, pos, cur - pos)

        if (cur_len == 0) {
          cur += 2
          cur_buf.buffer.flip()
          state = MC_STATE_CMD_MGET
        }

        pos = cur
      } else {
        if (read_buf.get(cur) == 10) {
          next(new String(read_buf.array, pos, cur - 1 - pos, "UTF-8"))
          pos = cur + 1
        }

        cur += 1
      }
    }

    if (cur < read_buf.position) {
      println("READ REMAINING")
      read_buf.limit(read_buf.position)
      read_buf.position(cur)
      read_buf.compact()
    } else {
      read_buf.clear()
    }
  }

  def write(event: SelectionKey) : Unit = {
    try {
      sock.write(write_buf)
    } catch {
      case e: Exception => {
        Logger.error("[Memcache] conn error: " + e.toString, false)
        return close(e)
      }
    }

    if (write_buf.remaining == 0) {
      write_buf.clear
      event.interestOps(SelectionKey.OP_READ)
    }
  }

  def close(err: Throwable = null) : Unit = {
    if (state == MC_STATE_CLOSE)
      return

    try {
      if (requests != null) {
        for (req <- requests) {
          req.ready()
        }
      }
    } catch {
      case e: Exception => {
        Logger.exception(e, false)
      }
    }

    state = MC_STATE_CLOSE

    pool.close(this)
    sock.close()
    Statistics.decr('sql_connections_open)
  }

  def timeout() : Unit = {
    Logger.error("[Memcache] connection timed out...", false)
    close()
  }

  private def next(cmd: String) : Unit = {
    state match {

      case MC_STATE_CMD_DELETE => {
        cmd match {

          case "DELETED" => {
            idle(last_event)
          }

          case "NOT_FOUND" => {
            idle(last_event)
          }

        }
      }

      case MC_STATE_CMD_SET => {
        cmd match {

          case "STORED" => {
            idle(last_event)
          }

          case "NOT_STORED" => {
            idle(last_event)
          }

        }
      }

      case MC_STATE_CMD_MGET => {
        val parts = cmd.split(" ")

        if (parts.length == 1 && parts.head == "END") {
          for (req <- requests) {
            req.ready()
          }

          return idle(last_event)
        }

        if (parts.length != 4) {
          throw new ExecutionException("[Memcache] protocol error: " + cmd)
        }

        for (req <- requests) {
          if (req.buffer == null && req.key == parts(1)) {
            val buf = new ElasticBuffer(65535)
            req.buffer = buf

            cur_buf = buf
            cur_len = parts(3).toInt

            state = MC_STATE_READ
            return
          }
        }
      }

      case _ => {
        throw new ExecutionException(
          "unexpected token " + cmd + " (" + state.toString + ")")
      }

    }
  }

  private def idle(event: SelectionKey) : Unit = {
    timer.cancel()
    state = MC_STATE_IDLE
    event.interestOps(0)
    last_event = event
    requests = null
    pool.ready(this)
  }


}
