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
  var worker : Worker = null
  var ctree : CTree = null

  var conditions : String = null
  var order      : String = null

  def execute(_worker: Worker) : Unit = {
    var join_field : String = null
    var join_id    : Int    = 0

    worker = _worker

    if (fields.length == 0)
      fields += record.resource.id_field

    if (record_id != null)
      record.set_id(record_id)


    if (record.has_id) {
      println("TRY CTREE")
      ctree = CTreeIndex.find(this)

      if (ctree != null)
        println("!!!!!!! found ctree", ctree.name)
    }

    if (record.has_id) {
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

    if (join_field != null)
      execute_query(worker,
        SQLBuilder.select(
          relation.resource, join_field, join_id, fields.toList, 
          conditions, order, null, null))

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
    if (ctree != null)
      CTreeCache.store(ctree, this)

    prev.unroll()
  }

}
