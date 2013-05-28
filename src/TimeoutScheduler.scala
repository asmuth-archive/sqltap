// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.util.{PriorityQueue}

object TimeoutScheduler {

  private val timeouts = new ThreadLocal[PriorityQueue[Timeout]]{
    override def initialValue() = new PriorityQueue[Timeout]()
  }

  def schedule(millisecs: Long, callback: TimeoutCallback) : Timeout = {
    val timeout = new Timeout(millisecs, callback)
    timeouts.get().add(timeout)
    timeout
  }

  def remove(timeout: Timeout) : Unit = {
    timeouts.get().remove(timeout)
  }

  def run() : Unit = {
    val iter = timeouts.get().iterator()
    val now = System.nanoTime() / 1000000

    while (iter.hasNext()) {
      val timeout = iter.next()

      if (timeout.expires > now)
        return

      iter.remove()
      timeout.fire()
    }
  }

}
