package com.paulasmuth.dpump

import scala.collection.mutable.ListBuffer;

class RequestExecutor(base: InstructionStack) {

  var stack = ListBuffer[Instruction]()
  var stime = System.nanoTime

  stack += base.root

  def run() : Unit =
    next

  private def next() : Unit = {
    for (idx <- (0 to stack.length - 1).reverse)
      if (stack(idx).running == false) {
        execute(stack(idx))
        if (stack(idx).running) return next
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

    cur.job.retrieve
    cur.ready = true

    if (DPump.debug) {
      val qtime = (cur.job.result.qtime / 1000000.0).toString
      val otime = ((System.nanoTime - stime) / 1000000.0).toString
      DPump.log_debug("Finished (" + qtime + "ms) @ " + otime + "ms: "  + cur.job.query)
    }

    fetch(cur)
  }

  private def execute(cur: Instruction) : Unit = cur.name match {

    case "execute" => {
      cur.ready = true

      for (next <- cur.next)
        execute(next)
    }

    case _ => {
      if (cur.relation == null) {
        if (cur.prev == base.root) {
          cur.relation = DPump.manifest(cur.args(0)).to_relation
          cur.prepare

          if(cur.name == "findSingle" && cur.args(1) != null)
            cur.record.set_id(cur.args(1).toInt)

        } else {
          cur.relation = cur.prev.relation.resource.relation(cur.args(0))
          cur.prepare
        }

        if (cur.relation == null)
          throw new ExecutionException("relation not found: " + cur.args(0))

        stack += cur

        if (cur.name != "findMulti")
          for (next <- cur.next)
            execute(next)

      }

      peek(cur)
    }

  }

  private def fetch(cur: Instruction) = {

    if (cur.name == "findSingle" && cur.job.retrieve.data.size == 1)
      cur.record.load(cur.job.retrieve.head, cur.job.retrieve.data.head)

    else if (cur.name == "findMulti" && cur.job.retrieve.data.size > 0) {
      InstructionFactory.expand(cur)

      for (ins <- cur.next)
        stack += ins
    }
  }

  private def peek(cur: Instruction) : Unit = cur.name match {

    case "findSingle" => {
      var join_id : Int = 0
      var join_field : String = null

      if (cur.args.size < 5)
        throw new ParseException("emtpy field list")

      if (cur.record.has_id) {
        join_id = cur.record.id
        join_field = cur.relation.resource.id_field
      }

      else if (cur.relation.join_foreign == false && cur.prev.ready) {
        cur.record.set_id(cur.prev.record.get(cur.relation.join_field))
        join_id = cur.record.id
        join_field = cur.prev.relation.resource.id_field
      }

      else if (cur.relation.join_foreign == true && cur.prev.record.has_id) {
        cur.record.set_id(cur.prev.record.id)
        join_id = cur.record.id
        join_field = cur.relation.join_field
      }

      if (join_id != 0) {
        cur.running = true
        cur.job = DPump.db_pool.execute(
          SQLBuilder.sql(cur.relation.resource,
            join_field, join_id.toString,
            cur.args.slice(4, cur.args.size).toList,
            cur.args(2), cur.args(3), null, null))
      }
    }

    case "findMulti" => {

      if (cur.args.size < 6)
        throw new ParseException("emtpy field list")

      if (cur.prev == base.root) {
        cur.running = true
        cur.job = DPump.db_pool.execute(
          SQLBuilder.sql(cur.relation.resource, null, null,
            cur.args.slice(5, cur.args.size).toList,
            cur.args(1), cur.args(2), cur.args(3), cur.args(4)))
      }

      else if (cur.relation.join_foreign == true && cur.prev.record.has_id) {
        cur.record.set_id(cur.prev.record.id)
        cur.running = true
        cur.job = DPump.db_pool.execute(
          SQLBuilder.sql(cur.relation.resource,
            cur.relation.join_field, cur.record.id.toString,
            cur.args.slice(5, cur.args.size).toList,
            cur.args(1), cur.args(2), cur.args(3), cur.args(4)))
      }

      else if (cur.relation.join_foreign == false)
        throw new ParseException("findSome on a non-foreign relation")

    }

  }
}
