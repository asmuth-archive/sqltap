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
  val HTTP_STATE_BODY    = 6

  var http_method : String = null
  var http_uri    : String = null

  private var pos = 0
  private var token = 0
  private var state = HTTP_STATE_METHOD
  private val buf = new Array[Byte](4096)

  def read(src: ByteBuffer) = {
    val max = src.position

    while (pos < max) {
      src.get(pos) match {

        case ' ' => state match {

          case HTTP_STATE_METHOD => {
            http_method = next_token(src)
            println("method", http_method)
            state = HTTP_STATE_URI
          }

          case HTTP_STATE_URI => {
            http_uri = next_token(src)
            println("uri", http_uri)
            state = HTTP_STATE_VERSION
          }

          case _ =>
            ()

        }

        case '\n' => state match {

          case HTTP_STATE_VERSION => {
            println("version", next_token(src))
            state = HTTP_STATE_HKEY
          }

          case HTTP_STATE_HVAL => {
            println("hval", next_token(src))
            state = HTTP_STATE_HKEY
          }

          case HTTP_STATE_HKEY => {
            println("request finished")
            state = HTTP_STATE_BODY
          }

          case _ =>
            ()

        }

        case ':' => state match {

          case HTTP_STATE_HKEY => {
            println("hkey", next_token(src))
            state = HTTP_STATE_HVAL
          }

          case _ =>
            ()

        }

        case _ =>
          ()

      }

      pos += 1
    }
  }

  private def next_token(src: ByteBuffer) : String = {
    var start = 0
    var end   = pos - token

    System.arraycopy(src.array, token, buf, 0, pos - token)
    token = pos + 1

    while (buf(start) == ' ')
      start += 1

    while (buf(end - 1) == '\r' || buf(end - 1) == '\n')
      end -= 1

    new String(buf, start, end - start, "UTF-8")
  }

  def reset : Unit =  {
    pos = 0
    token = 0
    state = HTTP_STATE_METHOD
  }

}
