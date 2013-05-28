// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.util.{PriorityQueue}

class LocalTimeoutScheduler extends ThreadLocal[TimeoutScheduler]

class TimeoutScheduler {

  private val timeouts = new PriorityQueue[Timeout]()

  def schedule(millisecs: Long, callback: TimeoutCallback) : Timeout = {
    val timeout = new Timeout(millisecs, callback)
    timeouts.add(timeout)
    timeout
  }

  def remove(timeout: Timeout) : Unit = {
    timeouts.remove(timeout)
  }

  def run() = {
    println("RUN TIMEOUTS", timeouts)
  }

}
