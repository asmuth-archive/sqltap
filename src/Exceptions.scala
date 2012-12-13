package com.paulasmuth.sqltap

class ParseException(msg: String) extends Exception{
  override def toString = msg
}

class ExecutionException(msg: String) extends Exception{
  override def toString = msg
}

class NotFoundException(cur: Instruction) extends Exception{
  override def toString =
    "could not find record '" +
    (if (cur.relation == null) "null" else cur.relation.name) +
    "' with id #" + cur.record.id.toString
}
