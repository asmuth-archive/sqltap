// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.{ListBuffer}

class FindMultiInstruction extends SQLInstruction with CTreeInstruction  {

  val name = "findMulti"
  var worker : Worker = null

  var conditions : String  = null
  var order      : String  = null
  var limit      : String  = null
  var offset     : String  = null
  var expanded   : Boolean = false

  def execute(_worker: Worker) : Unit = {
    var _conditions = conditions
    worker = _worker

    if (finished)
      return

   if (!has_field(record.resource.id_field))
      fields += record.resource.id_field

    if (prev.name == "root") {
      execute_query(worker,
        SQLBuilder.select(
          relation.resource, null, 0, fields.toList,
          conditions, order, limit, offset))

    } else if (relation.join_foreign == true) {
      var join_id : Int = 0

      if (conditions == null)
        _conditions = relation.join_cond
      else if (relation.join_cond != null)
        _conditions += " AND " + relation.join_cond

      if (relation.join_field_local == null && prev.record.has_id) {
        join_id = prev.record.id
        prev.record.set_id(join_id)
      }

      else if (relation.join_field_local != null && prev.finished) {
        join_id = prev.record.get(relation.join_field_local).toInt
      }

      if (join_id > 0) {
        if (ctree_try) {
          ctree_try = false

          CTreeIndex.find(this) match {
            case None => ()
            case Some((_ctree, cost)) => {
              if (_ctree.allow_conditions) {
                ctree      = _ctree
                ctree_wait = true
                ctree_cost = cost
                ctree_key  = ctree.key(_conditions, join_id) // FIXPAUL conditions should be md5-hashed

                CTreeCache.retrieve(ctree, ctree_key, this, worker)
                return
              }
            }
          }
        }

        execute_query(worker,
          SQLBuilder.select(
              relation.resource, relation.join_field, join_id,
              fields.toList, _conditions, order, limit, offset))
      }
    }

    else if (relation.join_foreign == false) {
      throw new ParseException("findAll can't be used with a non-foreign relation")
    }

  }

  def ready(query: SQLQuery) : Unit = {
    var execute_ = true

    if (expanded) {
      for (row <- query.rows) {
        var n     = next.length
        var found = false

        val this_id = row(
          query.columns.indexOf(relation.resource.id_field)).toInt

        while (!found && n > 0) {
          val this_ins = next(n - 1)

          if (this_ins.record.id == this_id) {
            this_ins.record.update(query.columns, row)
            found = true
          }

          n -= 1
        }
      }
    } else {
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
    }

    finished = true

    execute_next(worker)
    unroll()
  }

}
