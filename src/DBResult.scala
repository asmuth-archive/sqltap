// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.LinkedList;

class DBResult(
  _head: List[String],
  _data: LinkedList[List[String]]
) {
  val head = _head
  val data = _data
  var qtime: Long = 0
  var error: String = null
}
