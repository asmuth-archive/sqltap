package com.paulasmuth.dpump

object InstructionParser {

  def parse(cur: Instruction) : Unit =
    cur.inspect(8)

}
