// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class InstructionStack {

  val root : Instruction = null
  var head : Instruction = null

  def push_down : Unit = {
    //val next = new Instruction
    //head.next = head.next :+ next
    //next.prev = head
    //head = next
  }

  def pop : Unit =
    head = head.prev

  //def push_arg(arg: String) =
  //  head.args += arg

  def inspect() : Unit =
    inspect_one(root, 1)

  private def inspect_one(cur: Instruction, lvl: Int) : Unit = {
    cur.inspect(lvl)

    for (next <- cur.next)
      inspect_one(next, lvl+1)
  }

}
