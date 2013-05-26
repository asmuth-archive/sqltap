// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.{ListBuffer}

class FindMultiInstruction extends Instruction with ReadyCallback[SQLQuery] {

  var conditions : String = null
  var order      : String = null
  var limit      : String = null
  var offset     : String = null

  def execute() : Unit = {
    if (fields.length == 0)
      fields += record.resource.id_field

    if (prev.prev == null) {
      execute_query(
        SQLBuilder.select(
          relation.resource, null, 0, fields.toList, 
          conditions, order, limit, offset))

    } else if (relation.join_foreign == true && prev.record.has_id) {
      record.set_id(prev.record.id)

      if (conditions == null)
        conditions = relation.join_cond
      else
        conditions += " AND " + relation.join_cond

      execute_query(
        SQLBuilder.select(
          relation.resource, relation.join_field, record.id,
          fields.toList, conditions, order, limit, offset))
    }

    else if (relation.join_foreign == false)
      throw new ParseException("findMulti on a non-foreign relation")
  }

  def ready(query: SQLQuery) : Unit = {
    if (query.rows.length == 0)
      next = new ListBuffer[Instruction]()

    else if (next.length == 0)
      ()//InstructionFactory.expand(this)

    else {
      //InstructionFactory.expand(this)
      execute_next
    }
  }

}
