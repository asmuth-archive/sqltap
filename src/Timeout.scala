// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class Timeout(_ms: Long, callback: TimeoutCallback) extends Comparable[Timeout] {
  val ms = _ms
  val expires = (System.nanoTime / 1000000) + ms

  def compareTo(o: Timeout) = {
    ms.compareTo(o.ms)
  }

  def fire() =
    callback.timeout()

}
