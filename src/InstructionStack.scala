package com.paulasmuth.dpump

class InstructionStack {

  val root = new Instruction
  root.name = "execute"

  var head = root

  class Instruction {
    var name : String = null
    var args = List[String]()
    var next = List[Instruction]()
    var prev : Instruction = null
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

}
