// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

trait CTreeInstruction extends Instruction {
  var ctree : CTree       = null
  var ctree_cost          = 0
  var ctree_wait          = false
  var ctree_store         = false
  var ctree_try           = true
  var ctree_key : String  = null

  override def ready() : Unit = {
    if (ctree_store)
      CTreeCache.store(ctree, ctree_key, this)

    prev.unroll()
  }

  def ctree_ready(worker: Worker) : Unit = {
    ctree_wait = false

    if (finished)
      return

    if (ctree_cost > -100) {
      CTreeCache.expand_query(ctree, this)
      ctree_store = true
    }

    execute(worker)
    CTreeCache.flush()
  }
}
