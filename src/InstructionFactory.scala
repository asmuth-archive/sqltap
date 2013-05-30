// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.ListBuffer

object InstructionFactory {

  private def copy(src: Instruction) : Instruction = {
    var cpy = make(src.args)
    cpy.fields = src.fields.clone()
    cpy.relation = src.relation
    cpy.args = src.args
    cpy.record = new Record(src.relation.resource)
    cpy
  }

  def link(par: Instruction, cld: Instruction) : Unit = {
    cld.prev = par
    par.next = par.next :+ cld
  }

  def deep_copy(src: Instruction, dst: Instruction) : Unit =
    for (nxt <- src.next) {
      var cpy = copy(nxt)
      link(dst, cpy)
      deep_copy(nxt, cpy)
    }

  def make(args: ListBuffer[String]) : Instruction = {
    var ins : Instruction = null

    args(1) match {

      case "findOne" => {
        ins = new FindSingleInstruction()

        if (args.length == 3)
          ins.record_id = args(2)
      }

      case "findAll" => {
        ins = new FindMultiInstruction()

        if (args.length >= 3)
          ins.asInstanceOf[FindMultiInstruction].limit = args(2)
      }

      case "findAllWhere" => {
        ins = new FindMultiInstruction()

        if (args.length < 3)
          throw new ParseException("findAllWhere requires at least one argument")

        ins.asInstanceOf[FindMultiInstruction].conditions = args(2)

        if (args.length > 3)
          ins.asInstanceOf[FindMultiInstruction].limit = args(3)
      }

      case "countAll" => {
        ins = new CountInstruction()
      }

      case _ =>
        throw new ParseException("invalid instruction: " + args(1))

    }

    ins.resource_name = args(0)
    ins.args = args.clone()

    return ins
  }



}
