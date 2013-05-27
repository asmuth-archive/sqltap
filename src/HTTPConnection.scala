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

  private val HTTP_STATE_INIT  = 1
  private val HTTP_STATE_CLOSE = 2

  private val buf = ByteBuffer.allocate(4096) // FIXPAUL
  private val parser = new HTTPParser()
  private var state = HTTP_STATE_INIT

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
      event.interestOps(0)
      execute()
      buf.clear
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
      (new Request("product.findOne(35975305){id,slug,user_id,milli_units_per_item,unit,cents,currency,first_published_at,channel_id,mailable_in_option,user.findOne{id,country,shop.findOne{id,subdomain,auto_confirm_enabled_at},standard_images.findAll{id,filename,synced,imageable_type,imageable_id}},translations_only_title.findAll{language,attribute,text},standard_images.findAll{id,filename,synced,imageable_type,imageable_id}}", worker)).run()
      (new Request("user.findOne(13008){products.countAll{}}", worker)).run()
    //EOF STUB
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

  private def http_error(code: Int, message: String) : Unit = {
    println("HTTP_ERROR", code, message)
  }

}
