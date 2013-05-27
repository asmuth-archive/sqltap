// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}
import scala.collection.mutable.ListBuffer

trait Instruction {

  var next          = ListBuffer[Instruction]()
  var prev          : Instruction = null

  var fields        = ListBuffer[String]()
  var relation      : ResourceRelation = null
  var record        : Record = null
  var request       : Request = null
  var args          : ListBuffer[String] = null

  var resource_name : String = null
  var record_id     : String = null

  var running  = false
  var finished = false

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

  def execute_next() : Unit = {
    for (ins <- next)
      if (ins.running == false)
        ins.execute()
  }

  def inspect(lvl: Int) : Unit = {
    SQLTap.log_debug((" " * (lvl*2)) + "> resource: " + resource_name + ", fields: [" + (
      if (fields.size > 0) fields.mkString(", ") else "none") + "]")
  }

  def unroll() : Unit = {
    var all_finished = finished

    if (all_finished == false)
      return

    for (ins <- next)
      all_finished = (finished & ins.finished)

    if (all_finished)
      if (prev != null)
        prev.unroll()
      else
        request.ready()
  }

}
