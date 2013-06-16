// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}

class FindSingleInstruction extends SQLInstruction {

  val name = "findSingle"

  var worker : Worker     = null
  var ctree : CTree       = null
  var ctree_cost          = 0
  var ctree_wait          = false
  var ctree_store         = false
  var ctree_try           = true
  var ctree_key : String  = null

  var conditions : String = null
  var order      : String = null

  def execute(_worker: Worker) : Unit = {
    var join_field : String = null
    var join_id    : Int    = 0

    println("EXECUTE")
    worker = _worker

    if (finished)
      return

    if (ctree_wait)
      return

    if (record_id != null)
      record.set_id(record_id)

    if (fields.length == 0)
      return cancel(worker)

    if (record.has_id) {
      // optimization: skip select id from ... where id = ...; queries
      if (fields.length == 1 && fields.head == record.resource.id_field)
        return cancel(worker)

      join_field = relation.resource.id_field
      join_id = record.id
    }

    else if (relation.join_foreign == false && prev.finished) {
      join_field = relation.resource.id_field
      join_id = prev.record.get(relation.join_field).toInt
      record.set_id(join_id)
    }

    else if (relation.join_foreign == true && prev.record.has_id) {
      join_field = relation.join_field
      join_id = prev.record.id
    }

    else if (prev.finished) {
      throw new ExecutionException("deadlock detected")
    }

    println("FUBAR")
    if (join_field != null) {
      if (ctree_try) {
        ctree_try = false

        println("TRY CTREE", resource_name)

        CTreeIndex.find(this) match {
          case None => ()
          case Some((_ctree, cost)) => {
            ctree      = _ctree
            ctree_wait = true
            ctree_cost = cost
            ctree_key  = ctree.key(join_field, join_id)

            CTreeCache.retrieve(ctree, ctree_key, this, worker)
            return
          }
        }
      }

      execute_query(worker,
        SQLBuilder.select(
          relation.resource, join_field, join_id, fields.toList, 
          conditions, order, null, null))
    }

    if (running == true)
      execute_next(worker)
  }

  def ready(query: SQLQuery) : Unit = {
    if (query.rows.length == 0)
      throw new NotFoundException(this)
    else
      record.load(query.columns, query.rows(0))

    finished = true
    execute_next(worker)

    unroll()
  }


  override def ready() : Unit = {
    if (ctree_store)
      CTreeCache.store(ctree, ctree_key, this)

    prev.unroll()
  }

  def ctree_ready() : Unit = {
    ctree_wait = false

    if (finished)
      return

    // FIXPAUL: expand query only if cost above a certian threshold
    if (ctree_cost != 0) {
      // CTreeCache.expand_query(ctree, this)
      //ctree_cost = 0
    }

    ctree_store = ctree_cost == 0
    execute(worker)
  }

}
