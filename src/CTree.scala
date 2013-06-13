// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class CTree(doc: xml.Node) {
  val elem = new XMLHelper(doc)

  val name  : String = elem.attr("name", true)
  val query : String = elem.attr("query", true)

  val stack = new InstructionStack()
  QueryParser.parse(stack, query)

  if (stack.head.name != "findSingle")
    throw new ParseException(
      "ctree queries must have a findOne root instruction")

  CTreeIndex.register(this)
  println(elem)
  stack.head.inspect()

  def resource_name() : String = {
    stack.head.resource_name
  }

  def compare(ins: Instruction) : Unit = {
    println("COMPARING TO", ins)
  }

}
