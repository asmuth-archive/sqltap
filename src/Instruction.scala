package com.paulasmuth.dpump

import scala.collection.mutable.ListBuffer;

class Instruction {
  var name : String = null
  var args = ListBuffer[String]()
  var next = List[Instruction]()
  var prev : Instruction = null
  var job : DPump.db_pool.Job = null
  var object_id : Int = 0
}
