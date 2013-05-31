// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.util.concurrent.atomic.{AtomicInteger}

// TODO
//  > errors/sec
//  > kbytes/sec
//  > sql queries/sec
//  > number of sql connections
//  > memcache gets/sec
//  > memcache hit rate/sec
//  > mean sql query time/sec
//  > mean http query time/sec
//  > mean memcache query time/sec

object Statistics {

  @volatile private var s_connections_total : Long = 0
  @volatile private var s_connections_sec : Double = 0
  @volatile private var s_requests_total : Long = 0
  @volatile private var s_requests_sec : Double = 0

  private val requests = new AtomicInteger
  private val connections = new AtomicInteger

  private var last_update = System.nanoTime

  def incr_connections() : Unit = {
    connections.incrementAndGet()
  }

  def incr_requests() : Unit = {
    requests.incrementAndGet()
  }

  def get() : Map[String, String] = {
    Map(
      "connections_total" -> s_connections_total.toString,
      "connections_sec"   -> s_connections_sec.toString,
      "requests_total" -> s_requests_total.toString,
      "requests_sec"   -> s_requests_sec.toString
    )
  }

  def update() : Unit = {
    val time = (System.nanoTime - last_update) / 1000000L

    if (time < 1000)
      return

    last_update = System.nanoTime

    val _connections = connections.getAndSet(0)
    s_connections_total += _connections
    s_connections_sec = _connections / (time / 1000.0)

    val _requests = requests.getAndSet(0)
    s_requests_total += _requests
    s_requests_sec = _requests / (time / 1000.0)

    println(time, get())
  }

}

