// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}

class FindSingleInstruction extends SQLInstruction with CTreeInstruction {

  val name = "findSingle"

  var worker : Worker     = null
  var conditions : String = null
  var order      : String = null

  def execute(_worker: Worker) : Unit = {
    var skip       : Boolean = false
    var join_field : String  = null
    var join_id    : Int     = 0

    worker = _worker

    if (finished || ctree_wait)
      return

    if (record_id != null)
      record.set_id(record_id)

    if (!fields.contains(record.resource.id_field))
      fields += record.resource.id_field

    if (record.has_id) {
      // optimization: skip select id from ... where id = ...; queries
      if (fields.length == 1 && fields.head == record.resource.id_field) {
        skip = true
      }

      join_field = relation.resource.id_field
      join_id = record.id
    }

    else if (relation.join_foreign == false && prev.finished) {
      join_id = prev.record.get(relation.join_field).toInt

      if (relation.join_field_remote != null) {
        join_field = relation.join_field_remote
      } else {
        join_field = relation.resource.id_field
        record.set_id(join_id)
      }
    }

    else if (relation.join_foreign == true && prev.record.has_id) {
      join_field = relation.join_field
      join_id = prev.record.id
    }

    else if (prev.finished) {
      throw new ExecutionException("deadlock detected")
    }

    if (join_field != null) {
      if (ctree_try) {
        ctree_try = false

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

      if (skip)
        return cancel(worker)

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
    else {
      if (record.fields.length < 2)
        record.load(query.columns, query.rows(0))
      else
        record.update(query.columns, query.rows(0))
    }

    finished = true
    execute_next(worker)

    unroll()
  }

}
