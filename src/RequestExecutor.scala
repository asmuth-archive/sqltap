package com.paulasmuth.dpump

import scala.collection.mutable.ListBuffer;

class RequestExecutor(root: Instruction) {

  var stack = ListBuffer[Instruction]()
  var stime = System.nanoTime

  stack += root

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

    if (cur.name == "findOne" && cur.job.retrieve.data.size == 1)
      cur.record.load(cur.job.retrieve.head, cur.job.retrieve.data.head)

  }

  private def execute(cur: Instruction) : Unit = cur.name match {

    case "execute" => {
      cur.ready = true

      for (next <- cur.next)
        execute(next)
    }

    case "findAll" => {
      cur.name = "findSome"
      execute(cur)
    }

    case _ => {
      if (cur.relation == null) {
        if (cur.prev.name == "execute") {
          cur.relation = DPump.manifest(cur.args(0)).to_relation
          cur.prepare

          if(cur.name == "findOne" && cur.args.size == 2)
            cur.record.set_id(cur.args.remove(1).toInt)

        } else {
          cur.relation = cur.prev.relation.resource.relation(cur.args(0))
          cur.prepare
        }

        if (cur.relation == null)
          throw new ExecutionException("relation not found: " + cur.args(0))


        stack += cur

        for (next <- cur.next)
          execute(next)
      }

      peek(cur)
    }

  }

  private def peek(cur: Instruction) : Unit = cur.name match {

      case "findOne" =>

        // via cur->record.id
        if (cur.record.has_id) {
          cur.running = true
          cur.job = DPump.db_pool.execute(
            SQLBuilder.sql_find_one(
              cur.relation.resource, List("*"),
              cur.relation.resource.id_field,
              cur.record.id))
        }

        // via parent->foreign_key
        else if (cur.relation.join_foreign == false && cur.prev.ready) {
          cur.running = true
          cur.record.set_id(cur.prev.record.get(cur.relation.join_field))
          cur.job = DPump.db_pool.execute(
            SQLBuilder.sql_find_one(
              cur.relation.resource, List("*"),
              cur.prev.relation.resource.id_field,
              cur.record.id))
        }

        // via parent->id
        else if (cur.relation.join_foreign == true && cur.prev.record.has_id) {
          cur.running = true
          cur.record.set_id(cur.prev.record.id)
          cur.job = DPump.db_pool.execute(
            SQLBuilder.sql_find_one(cur.relation.resource, List("*"),
              cur.relation.join_field,
              cur.record.id))
        }


      case "findSome" =>

        // via parent->id
        if (cur.relation.join_foreign == true && cur.prev.record.has_id) {
          cur.running = true
          cur.record.set_id(cur.prev.record.id)
          cur.job = DPump.db_pool.execute(
            SQLBuilder.sql_find_some(
              cur.relation.resource, List("*"),
              cur.relation.join_field,
              cur.record.id,
              "", "id DESC", 10, 0))
        }


  }

}
