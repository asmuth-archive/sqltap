// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

class ElasticBuffer(initial_capa: Int) extends AbstractWrappedBuffer {

  private var buffer : ByteBuffer = ByteBuffer.allocate(initial_capa)

  def write(data: Byte) = {
    resize(1)
    buffer.put(data)
  }

  def write(data: Array[Byte]) = {
    resize(data.length)
    buffer.put(data)
  }

  def retrieve() : ByteBuffer = {
    buffer
  }

  private def resize(next_size: Int) : Unit = {
    if (next_size > buffer.remaining) {
      val new_capa = math.max(
        buffer.position + next_size,
        buffer.capacity * 2
      )

      val new_buffer = ByteBuffer.allocate(new_capa)

      buffer.flip()
      new_buffer.put(buffer)
      buffer = new_buffer
    }
  }

}
