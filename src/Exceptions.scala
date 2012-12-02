package com.paulasmuth.dpump

class ParseException(msg: String) extends Exception{
  override def toString = msg
}
