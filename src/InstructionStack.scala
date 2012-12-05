package com.paulasmuth.dpump

class InstructionStack {

  val root = new Instruction
  var head = root

  head.name = "execute"
  head.running = false
  push_down

  def push_down : Unit = {
    val next = new Instruction
    head.next = head.next :+ next
    next.prev = head
    head = next
  }

  def pop : Unit =
    head = head.prev

  def push_arg(arg: String) =
    head.args += arg

  def inspect() : Unit =
    inspect_one(root, 1)

  private def inspect_one(cur: Instruction, lvl: Int) : Unit = {
    cur.inspect(lvl)

    for (next <- cur.next)
      inspect_one(next, lvl+1)
  }

}
