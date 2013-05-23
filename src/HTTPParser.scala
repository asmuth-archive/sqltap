// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.ByteBuffer

class HTTPParser {

  class HTTPParseError extends Exception

  val HTTP_STATE_METHOD  = 1
  val HTTP_STATE_URI     = 2
  val HTTP_STATE_VERSION = 3
  val HTTP_STATE_HKEY    = 4
  val HTTP_STATE_HVAL    = 5

  var http_method : String = null
  var http_uri    : String = null

  private var last_pos = 0
  private var token = 0
  private var state = HTTP_STATE_METHOD
  private val buf = new Array[Byte](4096)

  def read(src: java.nio.ByteBuffer) = {
    var pos = last_pos
    val max = src.position

    while (pos < max) {
      src.get(pos) match {

        case ' ' =>
          state match {

            case HTTP_STATE_METHOD => {
              System.arraycopy(src.array, token, buf, 0, pos - token)
              http_method = new String(buf, 0, pos - token, "UTF-8")
              println("method", http_method)
              state = HTTP_STATE_URI
              token = pos + 1
            }

            case HTTP_STATE_URI => {
              System.arraycopy(src.array, token, buf, 0, pos - token)
              http_uri = new String(buf, 0, pos - token, "UTF-8")
              println("uri", http_uri)
              state = HTTP_STATE_VERSION
              token = pos
            }

          }

          case _ =>
            ()

      }

      pos += 1
    }

    last_pos = pos
  }

}
