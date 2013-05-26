// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.ListBuffer

class Instruction {
  var name : String = null
  var args = ListBuffer[String]()
  var next = List[Instruction]()
  var prev : Instruction = null

  var running = false
  var ready = false
  var prepared = false

  var query : SQLQuery = null
  var relation : ResourceRelation = null
  var record : Record = null

  def inspect(lvl: Int) : Unit = 
    SQLTap.log_debug((" " * (lvl*2)) + "> name: " + name + ", args: [" + (
      if (args.size > 0) args.mkString(", ") else "none") + "]")


  // FIXPAUL: refactor this, it should be three classes (FindSingleInstruction, ...)
  def execute(req: Request) : Unit = {
    println("EXECUTE NOW")
    inspect(0)

    if (name != "execute" && prepared == false) {
      if (prev == req.stack.root) {
        relation = SQLTap.manifest(args(0)).to_relation
      } else {
        relation = prev.relation.resource.relation(args(0))
      }

      if (relation != null)
        record = new Record(relation.resource)
      else
        throw new ExecutionException("relation not found: " + args(0))

      if(name == "findSingle" && args(1) != null)
        record.set_id(args(1).toInt)

      prepared = true
    }

    name match {

      case "execute" => {
        ready = true

        for (next <- next)
          next.execute(req)
      }

      case "findSingle" => {
        var join_field : String = null
        var join_id    : Int    = 0

        if (args.size < 5)
          throw new ParseException("empty field list")

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

        if (join_field != null) {
          running = true

          val qry = new SQLQuery(
            SQLBuilder.select(
              relation.resource,
              join_field,
              join_id,
              args.slice(4, args.size).toList, // fields
              args(2), // cond
              args(3), // order
              null, // limit
              null  // offset
            ))

          req.worker.sql_pool.execute(qry)
        }
      }

      case "findMulti" => {

        if (args.size < 6)
          throw new ParseException("empty field list")

        if (prev == req.stack.root) {
          running = true
          /*job = SQLTap.db_pool.execute(
            SQLBuilder.select(relation.resource, null, 0,
              args.slice(5, args.size).toList,
              args(1), args(2), args(3), args(4)))*/
        }

        else if (relation.join_foreign == true && prev.record.has_id) {
          record.set_id(prev.record.id)
          running = true

          if (args(1) == null && relation.join_cond != null)
            args(1) = relation.join_cond

          /*job = SQLTap.db_pool.execute(
            SQLBuilder.select(relation.resource,
              relation.join_field, record.id,
              args.slice(5, args.size).toList,
              args(1), args(2), args(3), args(4)))*/
        }

        else if (relation.join_foreign == false)
          throw new ParseException("findSome on a non-foreign relation")

      }

      case "countMulti" => {

        if (prev == req.stack.root)
          throw new ExecutionException("countAll is not supported for root resources")

        else if (relation.join_foreign == true && prev.record.has_id) {
          record.set_id(prev.record.id)
          running = true

          if (args(1) == null && relation.join_cond != null)
            args(1) = relation.join_cond

          /*job = SQLTap.db_pool.execute(
            SQLBuilder.count(relation.resource,
              relation.join_field, record.id, args(1)))*/

        }

        else if (relation.join_foreign == false)
          throw new ParseException("countAll on a non-foreign relation")

      }

    }
  }

}
