// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class Request(_req_str: String, _worker: Worker) {

  val stack = new InstructionStack()
  var etime = List[Long]()

  val req_str = _req_str
  val worker  = _worker

  //var ready = false
  var resp_status : Int = 200
  var resp_data : String = null


  def run() : Request = {
    etime = etime :+ System.nanoTime

    // FIXPAUL: this should be a static method!
    (new PlainRequestParser(this)).run

    etime = etime :+ System.nanoTime

    if (SQLTap.debug) {
      SQLTap.log_debug("Request:")
      stack.inspect()
    }

    stack.head.execute()
    etime = etime :+ System.nanoTime

    //writer.run(this)
    etime = etime :+ System.nanoTime

    this
  }

  def qtime : List[Double] =
    if (etime.size < 2) List[Double]() else
      etime.sliding(2).map(x=>(x(1)-x(0))/1000000.0).toList

}
