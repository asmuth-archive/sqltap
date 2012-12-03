package com.paulasmuth.dpump

import scala.collection.mutable.ListBuffer;

class RequestExecutor(req: Request) {

  var stack = ListBuffer[Instruction]()
  var stime : Long = 0

  def run  = try {
    stack += req.stack.root

    if (DPump.debug)
      stime = System.nanoTime

    next
  } catch {
    case e: ExecutionException => req.error(e.toString)
  }

  private def next() : Unit = {
    for (idx <- (0 to stack.length - 1).reverse)
      if (stack(idx).running == false) {
        execute(stack(idx))
        if (stack(idx).running) return next
      }

    if (stack.head.ready unary_!) {
      if (stack.head.job == null)
        throw new ExecutionException("deadlock while executing")

      stack.head.job.retrieve
      stack.head.ready = true

      if (DPump.debug) {
        val qtime = (stack.head.job.result.qtime / 1000000.0).toString
        val otime = ((System.nanoTime - stime) / 1000000.0).toString
        DPump.log_debug("Finished (" + qtime + "ms) @ " + otime + "ms: "  + stack.head.job.query)
      }

      if (stack.head.job.retrieve.data.size > 0) {

        if (stack.head.relation.rtype == "has_one")
          stack.head.record_id = stack.head.job.retrieve.get(0,
            stack.head.relation.resource.id_field).toInt

      }

    }

    stack.remove(0)

    if (stack.length > 0)
      next
  }

  private def execute(cur: Instruction) : Unit = cur.name match {

    case "execute" => {
      cur.ready = true

      for (next <- cur.next)
        execute(next)
    }

    case "findOne" => {
      if (cur.relation == null) {
        if (cur.prev.name == "execute") {
          cur.relation = DPump.manifest(cur.args(0)).to_relation
          cur.record_id = cur.args.remove(1).toInt
        } else {
          cur.relation = cur.prev.relation.resource.relation(cur.args(0))
        }

        if (cur.relation == null)
          throw new ExecutionException("relation not found: " + cur.args(0))

        stack += cur

        for (next <- cur.next)
          execute(next)
      }

      // via cur->record_id
      if (
        cur.relation.rtype == "has_one" &&
        cur.record_id != 0
      ) {
        cur.running = true
        cur.job = DPump.db_pool.execute(
          SQLBuilder.sql_find_one(
            cur.relation.resource, List("*"),
            cur.relation.resource.id_field,
            cur.record_id))
      }

      // via parent->foreign_key
      else if (
        cur.relation.rtype == "has_one" &&
        cur.relation.join_foreign == false &&
        cur.prev.ready
      ) {
        cur.running = true
        cur.record_id = cur.prev.job.retrieve.get(0, cur.relation.join_field).toInt
        cur.job = DPump.db_pool.execute(
          SQLBuilder.sql_find_one(
            cur.relation.resource, List("*"),
            cur.prev.relation.resource.id_field,
            cur.record_id))
      }

      // via parent->id
      else if (
        cur.relation.rtype == "has_one" &&
        cur.relation.join_foreign == true &&
        cur.prev.record_id != 0
      ) {
        cur.running = true
        cur.record_id = cur.prev.record_id
        cur.job = DPump.db_pool.execute(
          SQLBuilder.sql_find_one(cur.relation.resource, List("*"),
            cur.relation.join_field,
            cur.record_id))
      }

    }
  }

  /*private def expand(cur: Instruction) : Unit =
    for (next <- cur.next) next.name match {

      case "fetch" =>
        cur.args += next.args.head

      case "fetch_all" =>
        cur.args += "*"

      case "findOne" => {
        if (cur.name == "execute") {
          next.relation = DPump.manifest(next.args(0)).to_relation
          next.record_id = next.args.remove(1).toInt
        } else {
          next.relation = cur.relation.resource.relation(next.args(0))
        }

        if (next.relation == null)
          throw new ExecutionException("relation not found: " + next.args(0))

        stack += next

        build(next)
      }

    }*/

}
