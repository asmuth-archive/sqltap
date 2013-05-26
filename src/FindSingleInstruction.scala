// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}

class FindSingleInstruction extends Instruction with ReadyCallback[SQLQuery] {

  var conditions : String = null
  var order      : String = null

  def execute() : Unit = {
    var join_field : String = null
    var join_id    : Int    = 0

    if (fields.length == 0)
      fields += record.resource.id_field

    if (record_id != null)
      record.set_id(record_id)

    if (record.has_id) {
      join_field = relation.resource.id_field
      join_id = record.id
    }

    else if (relation.join_foreign == false && prev.ready) {
      join_field = relation.resource.id_field
      join_id = prev.record.get(relation.join_field).toInt
      record.set_id(join_id)
    }

    else if (relation.join_foreign == true && prev.record.has_id) {
      join_field = relation.join_field
      join_id = prev.record.id
    }

    if (join_field != null)
      execute_query(
        SQLBuilder.select(
          relation.resource, join_field, join_id, fields.toList, 
          conditions, order, null, null))

    if (running == true)
      execute_next
  }

  def ready(qry: SQLQuery) : Unit = {
    if (qry.rows.length == 0)
      throw new NotFoundException(this)
    //else
    //  record.load(job.retrieve.head, job.retrieve.data.head)

    execute_next
  }

}
