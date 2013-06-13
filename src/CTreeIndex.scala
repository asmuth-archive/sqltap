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
    println("new ctree", ctree.resource_name)

    if (!ctrees.contains(ctree.resource_name)) {
      ctrees(ctree.resource_name) = new ListBuffer[CTree]()
    }

    ctrees(ctree.resource_name) += ctree
  }

}
