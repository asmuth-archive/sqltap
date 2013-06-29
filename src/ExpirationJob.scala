// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class ExpirationJob(worker: Worker, ctree: CTree) extends ReadyCallback[Record]  {

  var cache_keys : List[String] = null

  def execute(record: Record) = {
    val handler = ExpirationHandlerFactory.get()
    val fields  = RelationTrace.lookup(record.resource.name)

    Logger.debug(
      "[EXPIRE] resource '" + record.resource.name + "' with id #" +
      record.id.toString + " expired")

    cache_keys = fields.map { field =>
      ctree.key(field, record.get(field))
    }

    handler.execute(this)
  }

  def get_worker() : Worker = {
    worker
  }

  def ready(record: Record) : Unit = {
    execute(record)
  }

  def error(record: Record, err: Throwable) : Unit = {
    ()
  }

}
