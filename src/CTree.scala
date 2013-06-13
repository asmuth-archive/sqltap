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

  def compare(ins: Instruction) : Int = {
    compare(ins, stack.head)
  }

  def compare(left: Instruction, right: Instruction) : Int = {
    var score = 0

    // FIXPAUL this is naive and expeeeeensive ;) 
    for (lfield <- left.fields) {
      var found = false

      // FIXPAUL this doesnt even terminate when a field is found
      for (rfield <- right.fields)
        if (lfield == rfield)
          found = true

      if (found) {
        println("yeah", lfield)
        score += 1
      } else {
        println("nooo", lfield)
        score -= 1
      }
    }


    for (lins <- left.next) {
      // FIXPAUL this doesnt terminate when a field is found
      for (rins <- right.next) {
        // FIXPAUL: this doesnt compare arguments!
        if (lins.resource_name == rins.resource_name && lins.name == rins.name) {
          println("BAM!")
          score += 20
          score += compare(lins, rins)
        }
      }
    }

    println("COMPARING", left, right, score)
    score
  }

}
