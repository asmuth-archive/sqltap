// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

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

  private val stats = Map[Symbol, Statistic](
    'http_connections_open    -> new IntegralStatistic,
    'http_requests_total      -> new IntegralStatistic,
    'http_requests_per_second -> new IntegralStatistic,
    'sql_connections_open     -> new IntegralStatistic,
    'sql_requests_total       -> new IntegralStatistic,
    'sql_requests_per_second  -> new IntegralStatistic
  )

  private var last_update = System.nanoTime

  def incr(key: Symbol, value: Double = 1.0) : Unit = {
    stats(key).incr(value)
  }

  def decr(key: Symbol, value: Double = 1.0) : Unit = {
    stats(key).incr(value)
  }

  def get() : Map[String, String] = {
    stats.map { s => ((s._1.name, s._2.get())) }
  }

  def update_async() : Unit = {
    val thread = (new Thread {
      override def run() : Unit = {
        while (true) {
          stats.foreach(_._2.flush())
          println(get())
          Thread.sleep(1000)
        }
      }
    })

    thread.start()
  }

}

