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

  var max_connections =
    Config.get('memcache_max_connections).toInt

  var max_queue_len =
    Config.get('memcache_queue_max_len).toInt

  private val connections      = new ListBuffer[MemcacheConnection]()
  private val connections_idle = new ListBuffer[MemcacheConnection]()
  private val queue            = new ListBuffer[CacheRequest]()

  // FIXPAUL: proper batching / mget
  def execute(requests: List[CacheRequest]) : Unit = {
    for (request <- requests) {
      val connection = get

      if (connection == null) {
        if (queue.length >= max_queue_len)
          throw new TemporaryException("memcache queue is full")

        request +=: queue
      } else {
        execute(connection, request)
      }
    }
  }

  def ready(connection: MemcacheConnection) : Unit = {
    connections_idle += connection

    val pending = math.min(connections_idle.length, queue.length)

    for (n <- (0 until pending)) {
      val conn = get()

      if (conn != null)
        execute(conn, queue.remove(0))
    }

    Statistics.incr('memcache_requests_total)
    Statistics.incr('memcache_requests_per_second)
  }

  def close(connection: MemcacheConnection) : Unit = {
    connections      -= connection
    connections_idle -= connection
  }

  private def get() : MemcacheConnection = {
    if (connections.length < max_connections)
      connect()

    if (connections_idle.length > 0)
      return connections_idle.remove(0)

    return null
  }

  private def connect() : Unit = {
    val conn = new MemcacheConnection(this)

    conn.connect()
    connections += conn
  }

  private def execute(connection: MemcacheConnection, req: CacheRequest) = {
    req match {

      case get: CacheGetRequest => {
        println("RETRIEVE", get.key)
        get.ready()
        ready(connection)
      }

      case set: CacheStoreRequest => {
        println("STORE", set.key)
        set.ready()
        ready(connection)
      }

      case purge: CachePurgeRequest => {
        connection.execute_delete(purge.key)
      }

    }
  }

}
