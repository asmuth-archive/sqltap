// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class Request(_worker: Worker, callback: ReadyCallback[Request]) {
/*
  val stack  = new InstructionStack()
  var etime  = List[Long]()
  val worker = _worker

  def run_query(qry_str: String) : Request = {
    etime = etime :+ System.nanoTime

    val head = new RootInstruction(qry_str)
    head.execute(worker)

    this
  }

  def ready() : Unit = {
    // FIXPAUL: this should be a static method!
    (new PrettyJSONWriter).write(stack.head)
    etime = etime :+ System.nanoTime

    callback.ready(this)

    SQLTap.log_debug("Request finished: " + qtime.toString)
  }
*/
}
