// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{HashMap,ListBuffer}

object CTreeIndex {

  val ctrees = new HashMap[String, ListBuffer[CTree]]

  def register(ctree: CTree) = {
    find(ctree.resource_name) += ctree
  }

  def find(resource_name: String) : ListBuffer[CTree] = {
    if (!ctrees.contains(resource_name)) {
      ctrees(resource_name) = new ListBuffer[CTree]()
    }

    ctrees(resource_name)
  }

  def find(root: Instruction) : Option[(CTree, Int)] = {
    val candidates = find(root.resource_name)
    var winner : CTree = null
    var winner_cost : Int = 0
    var top_score : Int = 10

    for (ctree <- candidates) {
      val (score, cost) = ctree.compare(root)

      SQLTap.log_debug("CTree: evaluating candidate: '" + ctree.name + 
       "' (score: " + score + ", cost: " + cost + ")")

      if (score > top_score) {
        winner = ctree
        winner_cost = cost
      }
    }

    if (winner != null)
      SQLTap.log_debug("CTree: using ctree '" + winner.name + "'")

    if (winner == null)
      None
    else
      Some(((winner, winner_cost)))
  }

}
