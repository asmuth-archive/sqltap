// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.{ListBuffer}

class CountInstruction extends SQLInstruction {

  def execute() : Unit = {
    if (prev == null)
      throw new ExecutionException("count is not supported on root resources")

    else if (relation.join_foreign == true && prev.record.has_id) {
      record.set_id(prev.record.id)

      execute_query(
        SQLBuilder.count(relation.resource,
          relation.join_field, record.id, relation.join_cond))
    }

    else if (relation.join_foreign == false)
      throw new ParseException("count on a non-foreign relationship is always 1")
  }

  def ready(query: SQLQuery) : Unit = {
    if (query.rows.length == 0)
      throw new NotFoundException(this)
    else
      record.set("__count", query.rows.head.head)
  }

}
