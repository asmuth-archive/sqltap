// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.ListBuffer

trait Instruction extends ReadyCallback[SQLQuery] {

  var next          = ListBuffer[Instruction]()
  var prev          : Instruction = null
  var running       = false
  var ready         = false

  var fields        = ListBuffer[String]()
  var relation      : ResourceRelation = null
  var record        : Record = null
  var request       : Request = null

  var resource_name : String = null
  var record_id     : String = null

  def execute() : Unit

  def prepare() : Unit = {
    if (prev == null)
      relation = SQLTap.manifest(resource_name).to_relation
    else
      relation = prev.relation.resource.relation(resource_name)

    if (relation != null)
      record = new Record(relation.resource)
    else
      throw new ExecutionException("relation not found: " + resource_name)
  }

/*


      }

      case "countMulti" => {

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

      }

    }
  }

*/

  def execute_query(qry_str: String) : Unit = {
    val qry = new SQLQuery(qry_str)
    qry.attach(this)
    request.worker.sql_pool.execute(qry)
    running = true
  }


  def execute_next() : Unit =
    for (ins <- next)
      if (ins.running == false)
        ins.execute()


  def inspect(lvl: Int) : Unit =
    SQLTap.log_debug((" " * (lvl*2)) + "> resource: " + resource_name + ", fields: [" + (
      if (fields.size > 0) fields.mkString(", ") else "none") + "]")


}
