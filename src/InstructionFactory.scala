// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

object InstructionFactory {

  def make(name: String) : Instruction = {
    println("MAKE", name)
    val ins = new FindSingleInstruction()

    return ins
  }


  def expand(cur: Instruction) : Unit = {
    /*
    val instructions = (List[Instruction]() /: cur.job.retrieve.data)(
      (lst: List[Instruction], row: List[String]) => {
        val ins = new Instruction
        ins.name = "execute"
        ins.relation = cur.relation
        ins.prepare
        ins.record.load(cur.job.retrieve.head, row)
        ins.prev = cur
        deep_copy(cur, ins)
        lst :+ ins
      })

    cur.next = instructions
    */
  }

  private def copy(src: Instruction) : Instruction = {
    //var cpy = new Instruction
    //cpy.name = src.name
    //cpy.args = src.args
    //cpy
    src
  }

  private def link(par: Instruction, cld: Instruction) : Unit = {
    cld.prev = par
    par.next = par.next :+ cld
  }

  private def deep_copy(src: Instruction, dst: Instruction) : Unit =
    for (nxt <- src.next) {
      var cpy = copy(nxt)
      link(dst, cpy)
      deep_copy(nxt, cpy)
    }

}
