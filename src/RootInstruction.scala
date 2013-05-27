// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}

class RootInstruction(qry_str: String) extends Instruction {
  finished = false

  var etime = List[Long]()
  val query_string = qry_str
  val name = "phi"

  def execute(worker: Worker) : Unit = {
    if (finished)
      return

    etime = etime :+ System.nanoTime
    // FIXPAUL: this should be a static method!
    (new QueryParser(this)).run

    etime = etime :+ System.nanoTime
    finished = true
    execute_next(worker)
  }

  override def unroll() : Unit = {
    var all_finished = finished

    if (all_finished == false)
      return

    for (ins <- next)
      all_finished = (finished & ins.finished)

    // FIXPAUL: this should be a static method!
    (new PrettyJSONWriter).write(this)

    etime = etime :+ System.nanoTime
  }

  def qtime : List[Double] =
    if (etime.size < 2) List[Double]() else
      etime.sliding(2).map(x=>(x(1)-x(0))/1000000.0).toList

}
