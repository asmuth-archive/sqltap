// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.ListBuffer

class PlainRequestParser extends RequestVisitor {

  var scope = 'root
  var depth = 0
  var funcb = 0

  var args  = new ListBuffer[String]()
  var stack = new InstructionStack()

  val t_rsrc = """^([0-9a-z_\-]+)\.(.*)""".r // fixpaul
  val t_sfld = """^([0-9a-z_\-]+)([,\}].*)""".r // fixpaul
  val t_sarg = """^([^,\)]+)(.*)""".r
  val t_func = """^(findOne|findSome|findAll|countAll)\.?(.*)""".r
  val t_rbrs = """^\((.*)""".r
  val t_rbre = """^\)(.*)""".r
  val t_cbrs = """^\{(.*)""".r
  val t_cbre = """^\}(.*)""".r
  val t_ssep = """^,(.*)""".r
  val t_fall = """\*([\},].*)""".r

  def run : Unit = {
    if (req.req_str == null)
      throw new ParseException("no query string")

    SQLTap.log_debug("Request: " + req.req_str)

    parse(req.req_str)

    if (depth != 0)
      throw new ParseException("unbalanced braces")

    if (funcb != 0)
      throw new ParseException("instruction missing {}-body")

    if (SQLTap.debug) {
      SQLTap.log_debug("Parser stack:")
      stack.inspect
    }
  }


  private def parse(next: String) : Unit = {
    var done = true

    if (next == "")
      return

    next match {

      case t_rbrs(tail: String) =>
        { scope = 'arg; parse(tail) }

      case t_rbre(tail: String) =>
        { scope = 'root; parse(tail) }

      case t_cbrs(tail: String) =>
        { push_down; parse(tail); funcb -= 1 }

      case t_cbre(tail: String) =>
        { pop; parse(tail) }

      case t_ssep(tail: String) =>
        { parse(tail) }

      case _ => done = false
    }

    if (done)
      return
    else
      done = true

    next match {

      case t_rsrc(arg: String, tail: String) =>
        { args += arg; parse(tail) }

      case t_func(name: String, tail: String) =>
        { args += name; parse(tail); if (name != "countAll") funcb += 1 }

      case _ => done = false
    }

    if (done)
      return

    next match {

      case t_fall(tail: String) =>
        { stack.push_field("*"); parse(tail) }

      case t_sfld(name: String, tail: String) =>
        { stack.push_field(name); parse(tail) }

      case t_sarg(arg: String, tail: String) =>
        if (scope == 'arg)
          { args += arg; parse(tail) }
        else
          throw new ParseException("unexpected token: " + arg)

      case _ => println("nomatch: " + next)

    }
  }

  private def push_down = {
    stack.push_down(
      InstructionFactory.make(args))

    args.clear
    depth += 1
  }

  private def pop = {
    if (stack.length != 1)
      stack.pop()

    depth -= 1
  }

}
