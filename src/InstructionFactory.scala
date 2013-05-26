// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.ListBuffer

object InstructionFactory {

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

  def make(args: ListBuffer[String]) : Instruction = {
    println("MAKE", args)
    var ins : Instruction = null

    args(1) match {

      case "findOne" => {
        ins = new FindSingleInstruction()

        if (args.length == 3)
          ins.record_id = args(2)
      }

      case "findAll" => {
        ins = new FindSingleInstruction()

        /*
        val limit = if (cur.args.size == 2)
          cur.args(1) else null
        */
      }

      case "countAll" => {
        ins = new FindSingleInstruction()
      }

      /*
      case "fetch" => {
        cur.prev.next = cur.prev.next diff List(cur)
        cur.prev.args = cur.prev.args :+ cur.args.head
      }
      */

      case _ =>
        throw new ParseException("invalid instruction: " + args(1))

    }

    ins.resource_name = args(0)

    return ins
  }



}
