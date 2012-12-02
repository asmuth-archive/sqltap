package com.paulasmuth.dpump

class InstructionStack {

  val root = new Instruction
  var head = root

  class Instruction {
    var name : String = null
    var args = List[String]()
    var next = List[Instruction]()
    var prev : Instruction = null
    var job : DPump.db_pool.Job = null
  }

  def push_down : Unit = {
    val next = new Instruction
    head.next = head.next :+ next
    next.prev = head
    head = next
  }

  def pop : Unit =
    head = head.prev

  def push_arg(arg: String) =
    head.args = head.args :+ arg

  def inspect() : Unit =
    inspect_one(root, 0)

  private def inspect_one(cur: Instruction, lvl: Int) : Unit = {
    DPump.log_debug((" " * (lvl*2)) + "> name: " + cur.name + ", args: " + (
      if (cur.args.size > 0) cur.args.mkString(", ") else "none"))

    for (next <- cur.next)
      inspect_one(next, lvl+1)
  }

}
