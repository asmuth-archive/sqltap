// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.ListBuffer;

class RequestExecutor extends RequestVisitor {

  val SEARCH_DEPTH = 50 // max stack search depth

  var stack = ListBuffer[Instruction]()
  var stime : Long = 0

  def run() : Unit = {
    stime = System.nanoTime
    stack += req.stack.root
    next
  }

  private def next() : Unit = {
    val search_depth =
      Math.min(SEARCH_DEPTH, stack.length - 1)

    for (idx <- (0 to search_depth).reverse)
      if (stack(idx).running == false) {
        execute(stack(idx))

        if (stack(idx).running) 
          return next
      }

    pop(stack.remove(0))

    if (stack.length > 0)
      next
  }

  private def pop(cur: Instruction) : Unit = {
    if (cur.ready)
      return

    if (cur.job == null)
      throw new ExecutionException("deadlock while executing: " + 
        cur.name + ", " + cur.args.mkString(","))

    if (cur.job.retrieve.error != null)
      throw new ExecutionException(cur.job.retrieve.error)

    cur.ready = true

    if (SQLTap.debug) {
      val qtime = (cur.job.result.qtime / 1000000.0).toString
      val otime = ((System.nanoTime - stime) / 1000000.0).toString
      SQLTap.log_debug("Finished (" + qtime + "ms) @ " + otime + "ms: "  + cur.job.query)
    }

    fetch(cur)
  }

  private def execute(cur: Instruction) : Unit =

    if (cur.name == "execute") {
      cur.ready = true

      for (next <- cur.next)
        execute(next)
    }

    else {
      if (cur.relation == null) {
        prepare(cur)

        stack += cur

        if (cur.name != "findMulti")
          for (nxt <- cur.next)
            execute(nxt)

      }

      cur.execute(req)
    }


  private def prepare(cur: Instruction) = {

    if (cur.prev == req.stack.root) {
      cur.relation = SQLTap.manifest(cur.args(0)).to_relation
    } else {
      cur.relation = cur.prev.relation.resource.relation(cur.args(0))
    }

    if (cur.relation != null)
      cur.prepare
    else
      throw new ExecutionException("relation not found: " + cur.args(0))

    if(cur.name == "findSingle" && cur.args(1) != null)
      cur.record.set_id(cur.args(1).toInt)

  }


  private def fetch(cur: Instruction) : Unit =  cur.name match {

    case "findSingle" => {
      if (cur.job.retrieve.data.length == 0)
        throw new NotFoundException(cur)
      else
        cur.record.load(cur.job.retrieve.head, cur.job.retrieve.data.head)
    }

    case "countMulti" => {
      if (cur.job.retrieve.data.length == 0)
        throw new NotFoundException(cur)
      else {
        cur.record.set("__count", cur.job.retrieve.data.head.head)
      }
    }

    case "findMulti" => {
      if (cur.job.retrieve.data.length == 0)
        cur.next = List[Instruction]()

      else {
        val skip_execution = (cur.next.length == 0)
        InstructionFactory.expand(cur)

        if (skip_execution)
          return

        for (ins <- cur.next)
          stack += ins
      }
    }

  }

}
