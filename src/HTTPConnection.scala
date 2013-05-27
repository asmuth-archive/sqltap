// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.channels.{SocketChannel,SelectionKey}
import java.nio.{ByteBuffer}

class HTTPConnection(sock: SocketChannel, worker: Worker) extends ReadyCallback[Request] {

  private val HTTP_STATE_INIT  = 1
  private val HTTP_STATE_EXEC  = 2
  private val HTTP_STATE_HEAD  = 3
  private val HTTP_STATE_BODY  = 4
  private val HTTP_STATE_WAIT  = 5
  private val HTTP_STATE_CLOSE = 6

  private val buf = ByteBuffer.allocate(4096) // FIXPAUL
  private val parser = new HTTPParser()
  private var state = HTTP_STATE_INIT
  private var content_length : Int = 0
  private var last_event : SelectionKey = null
  private var keepalive : Boolean = false
  private var resp_buf : ByteBuffer = null

  println("new http connection opened")

  def read(event: SelectionKey) : Unit = {
    var ready = false
    val chunk = sock.read(buf)

    last_event = event

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
      event.interestOps(0)
      execute()
      buf.clear
    }
  }

  def write(event: SelectionKey) : Unit = {
    var remaining = 0

    try {
      if (state == HTTP_STATE_BODY) {
        sock.write(resp_buf)
        remaining = resp_buf.remaining
      } else {
        sock.write(buf)
        remaining = buf.remaining
      }
    } catch {
      case e: Exception => {
        SQLTap.error("[HTTP] conn error: " + e.toString, false)
        return close()
      }
    }

    if (remaining == 0) {
      if (state == HTTP_STATE_HEAD && resp_buf != null) {
        buf.clear
        state = HTTP_STATE_BODY
        write(event)
      } else {
        resp_buf = null
        state = HTTP_STATE_WAIT
        close() // FIXPAUL: implement keepalive
      }
    }
  }

  def close() : Unit = {
    if (state == HTTP_STATE_CLOSE)
      return

    println("connection closed")
    state = HTTP_STATE_CLOSE
    sock.close()
  }

  private def execute() : Unit = {
    println("request: ",
      parser.http_version,
      parser.http_method,
      parser.http_uri,
      parser.http_headers)

    // STUB
    (new Request(this)).execute(worker)

    state = HTTP_STATE_EXEC
    last_event.interestOps(0)
  }

  def error(e: Throwable) : Unit = e match {
    case e: ParseException =>
      http_error(404, e.toString)

    case e: NotFoundException =>
      http_error(404, e.toString)

    case e: ExecutionException =>
      http_error(500, e.toString)

    case e: TemporaryException =>
      http_error(503, e.toString)

    case e: Exception => {
      SQLTap.error("[HTTP] exception: " + e.toString, false)
      SQLTap.exception(e, false)
      close
    }
  }

  def ready(request: Request) = {
    val http_buf = new HTTPWriter(buf)
    buf.clear

    http_buf.write_status(200)
    http_buf.write_content_length(request.buffer.limit)
    http_buf.write_default_headers()
    http_buf.finish_headers()
    buf.flip

    resp_buf = request.buffer
    flush()
  }

  private def http_error(code: Int, message: String) : Unit = {
    val http_buf = new HTTPWriter(buf)
    buf.clear

    http_buf.write_status(code)
    http_buf.write_default_headers()
    http_buf.finish_headers()

    val json_buf = new PrettyJSONWriter(buf)
    json_buf.write_error(message)
    buf.flip

    keepalive = false
    flush()
  }

  private def flush() = {
    state = HTTP_STATE_HEAD
    last_event.interestOps(SelectionKey.OP_WRITE)
  }

}
