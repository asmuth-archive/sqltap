package com.paulasmuth.dpump

object InstructionParser {

  def parse(cur: Instruction) : Unit = cur.name match {

    case "findOne" => {
      cur.name = "findSingle"

      if(cur.args.size == 1)
        cur.args += null

      cur.args += null
      cur.args += null
    }

    case "findAll" => {
      cur.name = "findMulti"

      cur.args += null
      cur.args += null
      cur.args += null
      cur.args += null
    }

    case "findSingle" => ()
    case "findMulti" => ()
    case "fetch" => ()

    case _ =>
      throw new ParseException("invalid instruction: " + cur.name)

  }

}
