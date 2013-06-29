// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery}

class RecordLookupJob(resource: ResourceManifest) extends ReadyCallback[SQLQuery] {

  var callback : ReadyCallback[Record] = null

  def execute(worker: Worker, record_id: Int) = {
    val qry = new SQLQuery(
      SQLBuilder.select(resource, resource.id_field, record_id,
        resource.field_names, null, null, null, null))

    qry.attach(this)
    worker.sql_pool.execute(qry)
  }

  def attach(_callback: ReadyCallback[Record]) = {
    callback = _callback
  }

  def ready(qry: SQLQuery) : Unit = {
    if (callback != null) {
      val record = new Record(resource)
      record.load(qry.columns, qry.rows.head)

      callback.ready(record)
    }
  }

  def error(qry: SQLQuery, err: Throwable) : Unit = {
    SQLTap.exception(err, false)
  }

}
