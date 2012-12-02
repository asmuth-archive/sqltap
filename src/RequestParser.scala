package com.paulasmuth.dpump

class RequestParser(req: Request) {

  var scope = "root"

  val t_rsrc = """^([0-9a-z]+)\.(.*)""".r // fixpaul
  val t_sarg = """^([^,\)]+)(.*)""".r
  val t_func = """^(findOne|findSome|findAll)\.?(.*)""".r
  val t_rbrs = """^\((.*)""".r
  val t_rbre = """^\)(.*)""".r

  def parse : Unit = {
    if(req.req_str == null)
      return error(req, "no query string")

    try {
      parse_next(req.req_str)
    } catch {
      case e: ParseException => error(req, e.toString)
    }

    inspect
  }


  def parse_next(next: String) : Unit = next match {

    case t_rsrc(arg: String, tail: String) =>
      { req.stack.push_down; req.stack.push_arg(arg); parse_next(tail) }

    case t_func(name: String, tail: String) =>
      { req.stack.head.name = name; parse_next(tail) }

    case t_rbrs(tail: String) =>
      { scope = "arg"; parse_next(tail) }

    case t_rbre(tail: String) =>
      { scope = "root"; parse_next(tail) }

    case t_sarg(arg: String, tail: String) =>
      if (scope == "arg")
        { req.stack.push_arg(arg); parse_next(tail) }
      else
        throw new ParseException("unexpected token: " + arg)

    case _ => ()
  }

  private def error(req: Request, msg: String) : Unit = {
    req.resp_status = 400
    req.error_str = msg
    req.ready = true
  }

  private def inspect() : Unit =
    inspect_one(req.stack.root, 0)

  private def inspect_one(cur: req.stack.Instruction, lvl: Int) : Unit = {
    println((" " * (lvl*2)) + "> name: " + cur.name + ", args: " + (
      if (cur.args.size > 0) cur.args.mkString(", ") else "none"))

    for (next <- cur.next)
      inspect_one(next, lvl+1)
  }

}
