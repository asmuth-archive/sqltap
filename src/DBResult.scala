package com.paulasmuth.dpump

import scala.collection.mutable.LinkedList;

class DBResult(
  _head: List[String],
  _data: LinkedList[List[String]]
) {
  val head = _head
  val data = _data
  var qtime: Long = 0

  def get(row: Int, field: String) : String = {
    val idx = head.indexOf(field)

    if (idx == -1)
      throw new ExecutionException("unknown field: " + field)

    data(row)(idx)
  }
}
