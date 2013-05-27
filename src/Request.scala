// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

class Request(callback: ReadyCallback[Request]) extends ReadyCallback[Query] {

  val queries = List(
    new Query("user.findOne(1){email,username}"),
    new Query("user.findOne(2){email,username}"))

  var remaining = 2

  def execute(worker: Worker) : Unit = {
    queries.foreach(_.attach(this))
    queries.foreach(_.execute(worker))
  }

  def ready(query: Query) : Unit = {
    remaining -= 1

    if (remaining == 0)
      callback.ready(this)
  }

  def write(buf: ByteBuffer) = {
    buf.put("[\n".getBytes)

    for (ind <- (0 until queries.length)) {
      buf.put(queries(ind).json.toString.getBytes)

      if (ind < queries.length - 1)
        buf.put(",\n".getBytes)
    }

    buf.put("\n]".getBytes)
  }

}
