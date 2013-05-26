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
      if (stack(idx).running != true) {
        execute(stack(idx))

        if (stack(idx).running)
          return next
      }
  }

  private def pop(cur: Instruction) : Unit = {
    if (cur.query.error != null)
      throw new ExecutionException(cur.query.error)

    cur.ready = true

    if (SQLTap.debug) {
      val qtime = (cur.query.qtime / 1000000.0).toString
      val otime = ((System.nanoTime - stime) / 1000000.0).toString
      SQLTap.log_debug("Finished (" + qtime + "ms) @ " + otime + "ms: "  + cur.query.query)
    }
  }

  private def execute(cur: Instruction) : Unit =()
/*

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
*/

}
