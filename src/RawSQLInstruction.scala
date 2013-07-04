// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}

class RawSQLInstruction(qry_str: String, ttl: Int) extends SQLInstruction {

  val INS_STATE_INIT = 1
  val INS_STATE_RUNNING = 2
  val INS_STATE_DONE = 3

  val name = "rawSQL"
  var state = INS_STATE_INIT

  def execute(_worker: Worker) : Unit = {
    worker = _worker
    state = INS_STATE_RUNNING
    execute_query(worker, qry_str)
  }

  def ready(query: SQLQuery) : Unit = {
    state = INS_STATE_DONE

    if (query.rows.length == 0) {
      throw new NotFoundException(this)
    } else {
      record = new Record(null)
      record.load(query.columns, query.rows(0))
    }

    finished = true
    prev.unroll()
  }

  override def meta() : List[(String,String)] = {
    List(
      (("__query", qry_str))
    )
  }

}
