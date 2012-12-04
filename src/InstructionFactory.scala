package com.paulasmuth.dpump

object InstructionFactory {

  def expand(cur: Instruction) : Unit = {
    DPump.log_debug("expand called")

    val instructions = (List[Instruction]() /: cur.job.retrieve.data)(
      (lst: List[Instruction], row: List[String]) => {
        val ins = new Instruction
        ins.name = "execute"
        ins.relation = cur.relation
        ins.prepare
        ins.record.load(cur.job.retrieve.head, row)
        deep_copy(cur, ins)
        lst :+ ins
      })

    cur.next = instructions
  }

  private def copy(src: Instruction) : Instruction = {
    var cpy = new Instruction
    cpy.name = src.name
    cpy.args = src.args
    cpy
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
