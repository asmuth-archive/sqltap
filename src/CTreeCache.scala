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
object CTreeCache {

  val stubcache = new HashMap[String,String]() // STUB

  def store(ctree: CTree, ins: Instruction) : Unit = {
    println("STORE CTREE", ctree, ins)
    val el_buf    = new ElasticBuffer(1024)
    val ctree_buf = new CTreeBuffer(el_buf)

    serialize(ctree_buf, ctree.stack.head, ins)

    val buf = el_buf.retrieve
    val key = ctree.key(ins.record.id)

    println("STORE WITH KEY", key)

    stubcache(key) = new String(buf.array, 0, buf.position, "US-ASCII") // STUB
  }

  // def retrieve(ctree: CTree, record_id: Int) = {

  private def serialize(buf: CTreeBuffer, cins: Instruction, qins: Instruction) : Unit = {
    buf.write_header(qins.resource_name)

    for (field <- cins.fields) {
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

}
