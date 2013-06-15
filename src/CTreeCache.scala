// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{HashMap}

// TODO
//   > if only a subset of the fields is requested in a query, the cache entry is missing fields
//      -> make sure all ctree fields are fetched after a ctree miss (recursively)
//   > comparison doesnt take into account arguments
//   > mget if multiple children/queries all all ctrees
//   > cache query plans / ctreeindex.find decisions

object CTreeCache {

  val stubcache = new HashMap[String,ElasticBuffer]() // STUB

  def store(ctree: CTree, ins: Instruction) : Unit = {
    println("STORE CTREE", ctree, ins)
    val buf       = new ElasticBuffer(65535)
    val ctree_buf = new CTreeBuffer(buf)

    serialize(ctree_buf, ctree.stack.head, ins)

    println(ins.resource_name, ins.record.fields)
    val key = ctree.key(ins.record.id)

    println("STORE WITH KEY", key)

    buf.retrieve.flip // STUB
    stubcache(key) = buf // STUB
  }

  def retrieve(ctree: CTree, ins: FindSingleInstruction, worker: Worker) : Unit = {
    val key = ctree.key(ins.record.id)

    println("RETRIEVE", key)

    if (stubcache.contains(key)) {
      val buf = stubcache(key)
      val ctree_buf = new CTreeBuffer(buf)

      load(ctree_buf, ins, worker)
      buf.retrieve.position(0) // STUB
    }

    ins.ctree_ready()
  }

  private def serialize(buf: CTreeBuffer, cins: Instruction, qins: Instruction) : Unit = {
    buf.write_header(qins.resource_name)

    val fields = cins.fields

    if (cins.name == "count")
      fields += "__count"

    for (field <- fields) {
      if (qins.record.has_field(field))
        buf.write_field(field, qins.record.get(field))
    }

    for (lins <- cins.next) {
      // FIXPAUL: this doesnt terminate when found
      for (rins <- qins.next) {
        // FIXPAUL: this doesnt compare arguments!
        if (lins.resource_name == rins.resource_name && lins.name == rins.name) {
          serialize(buf, lins, rins)
        }
      }
    }

    buf.write_end()
  }

  private def load(buf: CTreeBuffer, ins: Instruction, worker: Worker) : Unit = {
    while (true) {
      buf.read_next() match {

        case buf.T_RES => {
          val res_name = buf.read_string()

          // FIXPAUL: this doesnt terminate when found
          for (nxt <- ins.next) {
            if (nxt.resource_name == res_name) {
              load(buf, nxt, worker)
            }
          }
        }

        case buf.T_FLD => {
          val field = buf.read_string()
          val value = buf.read_string()
          ins.record.set(field, value)
          ins.fields -= field
        }

        case buf.T_END => {
          if (ins.fields.length == 0)
            ins.cancel(worker)

          return
        }

      }
    }
  }

}
