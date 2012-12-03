package com.paulasmuth.dpump

import scala.collection.mutable.ListBuffer;

class RequestExecutor(req: Request) {

  var stack = ListBuffer[Instruction]()

  def run  = try {
    build(req.stack.root)

    if (DPump.debug) {
      DPump.log_debug("Execution stack:")

      for (instruction <- stack)
        instruction.inspect(1)
    }

    next
  } catch {
    case e: ExecutionException => req.error(e.toString)
  }

  private def build(cur: Instruction) : Unit = {
    for (next <- cur.next) { 
      next.name match {

        case "fetch" =>
          stack.head.args += cur.args.head

        case "fetch_all" =>
          stack.head.args += "*"

        case "findOne" => {
          if (cur.name == "execute") {
            next.relation = DPump.manifest(next.args(0)).to_relation
            next.relation_args = List(next.args.remove(1).toInt)
          } else {
            next.relation = cur.relation.resource.relation(next.args(0))
          }

          if (next.relation == null)
            throw new ExecutionException("relation not found: " + next.args(0))

          stack += next
          build(next)
        }

        case "findAll" => {
          stack += next
          build(next)
        }

      }
    }
    //req.stack.inspect
  }

  private def next() : Unit = {
    for (idx <- (0 to stack.length - 1).reverse)
      if (stack(idx).job == null)
        execute(stack(idx))

    if (stack.head.job == null)
      throw new ExecutionException("deadlock while executing")

    stack.head.job.retrieve

    if (DPump.debug) {
      val qtime = (stack.head.job.result.qtime / 1000000.0).toString
      DPump.log_debug("Execute (" + qtime + "ms): "  + stack.head.job.query)
    }

    stack.remove(0)

    if (stack.length > 0)
      next
  }

  private def execute(cur: Instruction) : Unit = {
    println("execute: " + cur.name + " - " + cur.args.mkString(", "))
    cur.name match {

    case "findOne" => {
      println(cur.prev.name)

      /*if (cur.prev.name == "execute") {
        cur.resource = DPump.manifest(cur.args(0))
      } else {
        println("fnord: " + cur.prev.resource
        resource = DPump.manifest(cur.args(0))
      }*/

      if (cur.relation_args.size != 1) return
      val query = SQLBuilder.sql_find_one(cur.relation.resource, List("*"), cur.relation_args.head)
      cur.job = DPump.db_pool.execute(query)
    }

  }}

}
