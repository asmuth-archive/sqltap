
// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.{ListBuffer}

class CountInstruction extends Instruction with ReadyCallback[SQLQuery] {
  def execute() : Unit = ()
  def ready(query: SQLQuery) : Unit = ()
}

/*
        if (prev == req.stack.root)
          throw new ExecutionException("countAll is not supported for root resources")

        else if (relation.join_foreign == true && prev.record.has_id) {
          record.set_id(prev.record.id)
          running = true

          if (args(1) == null && relation.join_cond != null)
            args(1) = relation.join_cond

          //job = SQLTap.db_pool.execute(
          //  SQLBuilder.count(relation.resource,
          //    relation.join_field, record.id, args(1)))

        }

        else if (relation.join_foreign == false)
          throw new ParseException("countAll on a non-foreign relation")
      
      
      case "countMulti" => {
        /*
        if (query.rows.length == 0)
          throw new NotFoundException(cur)
        else {
          record.set("__count", job.retrieve.data.head.head)
        }
        */
      }
*/
