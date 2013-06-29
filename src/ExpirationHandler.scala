// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class ExpirationHandler(worker: Worker) extends ReadyCallback[Record] {

  def expire(resource_name: String, record_id: Int) : Unit = {
    if (!SQLTap.manifest.contains(resource_name))
      throw new ParseException("unknown resource: " + resource_name)

    val resource = SQLTap.manifest(resource_name)

    SQLTap.log_debug(
      "[EXPIRE] fetching record '" + resource.name + "' with id #" +
      record_id.toString)

    val job = new RecordLookupJob(resource)
    job.attach(this)

    worker.synchronized {
      job.execute(worker, record_id)
    }
  }

  def expire(record: Record) : Unit = {
    SQLTap.log_debug(
      "[EXPIRE] resource '" + record.resource.name + "' with id #" +
      record.id.toString + " expired")

    val ctrees = CTreeIndex.find(record.resource.name)
    val fields = RelationTrace.lookup(record.resource.name)

    for (ctree <- ctrees) {
      val keys = fields.map {
        ctree.key(_, record.id)
      }

      val job = new ExpirationJob(worker, ctree, record, keys)
      println(job)
    }
  }

  def ready(record: Record) : Unit = {
    expire(record)
  }

  def error(record: Record, err: Throwable) : Unit = {
    ()
  }

}
