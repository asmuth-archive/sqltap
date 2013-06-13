// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

class WrappedBuffer(buf: ByteBuffer) {

  def write(data: Array[Byte]) : Unit = {
    buf.put(data)
  }

  def write(data: Byte) : Unit = {
    buf.put(data)
  }

  def write(data: Int) : Unit = {
    buf.putInt(data)
  }

  def retrieve() : ByteBuffer = {
    buf
  }

  def read_int() : Int = {
    buf.getInt()
  }

  def read_string(len: Int) : String = {
    buf.position(buf.position + len)
    len.toString
  }

}
