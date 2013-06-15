// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}

class Query(qry_str: String) extends Instruction {
  val name = "root"

  private var etime = List[Long]()
  private var callback : ReadyCallback[Query] = null
  private var failed = false

  def execute(worker: Worker) : Unit = {
    etime = etime :+ System.nanoTime

    try {
      val stack = new InstructionStack()
      stack.push_down(this)
      QueryParser.parse(stack, qry_str)
    } catch {
      case e: Exception => throw new ExecutionException(
        "error while parsing query: " + e.toString)
    }

    if (SQLTap.debug)
      inspect(0)

    etime = etime :+ System.nanoTime
    execute_next(worker)
  }

  override def unroll() : Unit = {
    if (failed)
      return

    etime = etime :+ System.nanoTime

    println("QUERY UNROLL")

    if (callback != null)
      callback.ready(this)
  }

  def error(err: Throwable) : Unit = {
    // FIXPAUL: kill all running connections
    failed = true

    if (callback != null)
      callback.error(this, err)
  }

  def attach(_callback: ReadyCallback[Query]) =
    callback = _callback

  def qtime : List[Double] =
    if (etime.size < 2) List[Double]() else
      etime.sliding(2).map(x=>(x(1)-x(0))/1000000.0).toList

  override def prepare() : Unit = ()

}
