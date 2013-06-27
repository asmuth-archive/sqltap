// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

class MemcacheConnectionPool {

  val MEMCACHE_BATCH_SIZE = 10

  val current = ListBuffer[MemcacheRequest]()
  val queue   = ListBuffer[MemcacheRequest]()

  def enqueue(request: MemcacheRequest) = {
    request +=: current

    if (queue.length >= MEMCACHE_BATCH_SIZE)
      flush()
  }

  def flush() : Unit = {
    if (current.length == 0)
      return

    queue ++= current
    current.clear()

    ready()
  }

  private def ready() {
    val jobs = queue.toList
    queue.clear()

    for (job <- jobs) {
      // STUB...
      job.ready()
    }
  }

}
