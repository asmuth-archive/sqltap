// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

class MemcacheConnectionPool extends CacheBackend {

  //val MEMCACHE_BATCH_SIZE = 10
  val conn = new MemcacheConnection(this)

  def connect() : Unit = {
    conn.connect()
  }

  def execute(requests: List[CacheRequest]) : Unit = {
  }

  def ready(conn: MemcacheConnection) : Unit = {
    conn.execute_flush()
  }

  def close(conn: MemcacheConnection) : Unit = {


  }

}
