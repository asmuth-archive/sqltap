// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class CTreeBuffer(buf: ElasticBuffer) {

  private val T_RES : Int = 1
  private val T_FLD : Int = 2
  private val T_END : Int = 3

  def write_header(resource_name: String) : Unit = {
    val bytes = resource_name.getBytes("UTF-8")
    println("SERIALIZE RESNAME", resource_name)

    buf.write(T_RES)
    buf.write(bytes.size)
    buf.write(bytes)
  }

  def write_field(name: String, value: String) : Unit = {
    val name_bytes = name.getBytes("UTF-8")
    val value_bytes = value.getBytes("UTF-8")
    println("SERIALIZE FIELD", name, value)

    buf.write(T_FLD)
    buf.write(name_bytes.size)
    buf.write(value_bytes.size)
    buf.write(name_bytes)
    buf.write(value_bytes)
  }

  def write_end() : Unit = {
    println("SERIALIZE END")
    buf.write(T_END)
  }

}
