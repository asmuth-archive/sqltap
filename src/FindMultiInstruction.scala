// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.{ListBuffer}

class FindMultiInstruction extends SQLInstruction {

  val name = "findMulti"
  var worker : Worker = null

  var conditions : String = null
  var order      : String = null
  var limit      : String = null
  var offset     : String = null

  def execute(_worker: Worker) : Unit = {
    worker = _worker

    if (finished)
      return

    if (fields.length == 0)
      fields += record.resource.id_field

    if (prev.name == "root") {
      execute_query(worker,
        SQLBuilder.select(
          relation.resource, null, 0, fields.toList,
          conditions, order, limit, offset))

    } else if (relation.join_foreign == true) {
      var join_id : Int = 0

      if (conditions == null)
        conditions = relation.join_cond
      else if (relation.join_cond != null)
        conditions += " AND " + relation.join_cond

      if (relation.join_field_local == null && prev.record.has_id) {
        join_id = prev.record.id
        prev.record.set_id(join_id)
      }

      else if (relation.join_field_local != null && prev.finished) {
        join_id = prev.record.get(relation.join_field_local).toInt
      }

      if (join_id > 0)
        execute_query(worker,
          SQLBuilder.select(
              relation.resource, relation.join_field, join_id,
              fields.toList, conditions, order, limit, offset))
    }

    else if (relation.join_foreign == false) {
      throw new ParseException("findAll can't be used with a non-foreign relation")
    }
  }

  def ready(query: SQLQuery) : Unit = {
    var execute_ = true

    if (query.rows.length == 0)
      next = new ListBuffer[Instruction]()

    val instructions = new ListBuffer[Instruction]()

    for (row <- query.rows) {
      val ins = new PhiInstruction()
      ins.relation = relation
      ins.prev = this
      ins.record = new Record(relation.resource)
      ins.record.load(query.columns, row)
      InstructionFactory.deep_copy(this, ins)
      instructions += ins
    }

    next = instructions
    finished = true

    execute_next(worker)
    unroll()
  }

}
