// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

class Request(callback: ReadyCallback[Request]) extends ReadyCallback[Query] {

  val buffer = ByteBuffer.allocate(32768)
  val json_stream = new PrettyJSONWriter(buffer)

  val queries = List(
    new Query("user.findOne(1){email,username}"),
    new Query("user.findOne(2){email,username}"))

  var remaining = 2

  def execute(worker: Worker) : Unit = {
    json_stream.write_array_begin()
    queries.foreach(_.attach(this))
    queries.foreach(_.execute(worker))
  }

  def ready(query: Query) : Unit = {
    remaining -= 1

    json_stream.write_query(query)

    if (remaining > 0) {
      json_stream.write_comma()
    } else {
      json_stream.write_array_end()
      buffer.flip()
      callback.ready(this)
    }
  }

}
