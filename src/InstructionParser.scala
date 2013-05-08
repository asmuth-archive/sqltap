// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

// Internal Instructions:
//   -> findSingle(res, id, condition, order, fields...)
//   -> findMulti(res, condition, order, limit, offset, fields...)

object InstructionParser {

  def parse(cur: Instruction) : Unit = cur.name match {

    case "findOne" => {
      cur.name = "findSingle"

      if(cur.args.size == 1)
        cur.args += null

      cur.args += null // condition
      cur.args += null // order
    }

    case "findAll" => {
      cur.name = "findMulti"

      val resource = cur.args(0)

      val limit = if (cur.args.size == 2)
        cur.args(1) else null

      cur.args.clear
      cur.args += resource
      cur.args += null // condition
      cur.args += null // order
      cur.args += limit
      cur.args += null // offset
    }

    case "countAll" => {
      cur.name = "countMulti"

      cur.args += null // condition
      cur.args += null // order
      cur.args += null // limit
      cur.args += null // offset
    }

    case "fetch" => {
      cur.prev.next = cur.prev.next diff List(cur)
      cur.prev.args = cur.prev.args :+ cur.args.head
    }

    case "findSingle" => ()
    case "findMulti" => ()
    case "execute" => ()

    case _ =>
      throw new ParseException("invalid instruction: " + cur.name)

  }

}
