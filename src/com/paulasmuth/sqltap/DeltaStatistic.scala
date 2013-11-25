// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.util.concurrent.atomic.{AtomicInteger}
import java.text.{DecimalFormat}

class DeltaStatistic extends Statistic {

  private val bucket = new AtomicInteger()
  private var value : Double = 0.0
  private val format = new DecimalFormat("0.00")

  def incr(delta: Double) : Unit= {
    bucket.getAndAdd(delta.toInt)
  }

  def decr(delta: Double) : Unit = {
    bucket.getAndAdd(delta.toInt * -1)
  }

  def get() : String = {
    format.format(value)
  }

  def flush(f: Double) : Unit = {
    value = bucket.getAndSet(0) / f
  }

}
