package com.paulasmuth.dpump

class RequestParser(req: Request) {

  var scope = 'root

  val t_rsrc = """^([0-9a-z]+)\.(.*)""".r // fixpaul
  val t_sfld = """^([0-9a-z]+)([,\}].*)""".r // fixpaul
  val t_sarg = """^([^,\)]+)(.*)""".r
  val t_func = """^(findOne|findSome|findAll)\.?(.*)""".r
  val t_rbrs = """^\((.*)""".r
  val t_rbre = """^\)(.*)""".r
  val t_cbrs = """^\{(.*)""".r
  val t_cbre = """^\}(.*)""".r
  val t_ssep = """^,(.*)""".r
  val t_fall = """\*(\}.*)""".r

  def run : Unit = {
    if(req.req_str == null)
      throw new ParseException("no query string")

    DPump.log_debug("Request: " + req.req_str)

    parse(req.req_str)

    if (DPump.debug) {
      DPump.log_debug("Parser stack:")
      req.stack.inspect
    }
  }


  def parse(next: String) : Unit = {
    var done = true

    if (next == "")
      return

    next match {

      case t_rbrs(tail: String) =>
        { scope = 'arg; parse(tail) }

      case t_rbre(tail: String) =>
        { scope = 'root; parse(tail) }

      case t_cbrs(tail: String) =>
        { req.stack.push_down; parse(tail) }

      case t_cbre(tail: String) =>
        { req.stack.pop; parse(tail) }

      case t_ssep(tail: String) =>
        if (scope == 'root)
          { req.stack.pop; req.stack.push_down; parse(tail) }
        else
          { parse(tail) }

      case _ => done = false
    }

    if (done)
      return
    else
      done = true

    next match {

      case t_rsrc(arg: String, tail: String) =>
        { req.stack.push_arg(arg); parse(tail) }

      case t_func(name: String, tail: String) =>
        { req.stack.head.name = name; parse(tail) }

      case _ => done = false
    }

    if (done)
      return

    next match {

      case t_fall(tail: String) =>
        { req.stack.head.name = "fetch"; req.stack.push_arg("*"); parse(tail) }

      case t_sfld(name: String, tail: String) =>
        { req.stack.head.name = "fetch"; req.stack.push_arg(name); parse(tail) }

      case t_sarg(arg: String, tail: String) =>
        if (scope == 'arg)
          { req.stack.push_arg(arg); parse(tail) }
        else
          throw new ParseException("unexpected token: " + arg)

      case _ => println("nomatch: " + next)

    }
  }

}
