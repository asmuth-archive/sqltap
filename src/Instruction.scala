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

  def execute_query(qry_str: String) : Unit = {
    val qry = new SQLQuery(qry_str)
    qry.attach(this)
    request.worker.sql_pool.execute(qry)
    running = true
  }


  def execute_next() : Unit = {
    for (ins <- next)
      if (ins.running == false)
        ins.execute()
  }

  def inspect(lvl: Int) : Unit = {
    SQLTap.log_debug((" " * (lvl*2)) + "> resource: " + resource_name + ", fields: [" + (
      if (fields.size > 0) fields.mkString(", ") else "none") + "]")
  }

}
