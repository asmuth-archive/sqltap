// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class CTreeBuffer(buf: ElasticBuffer) {

  val T_RES : Int = 1
  val T_FLD : Int = 2
  val T_END : Int = 3
  val T_PHI : Int = 4

  def write_header(resource_name: String) : Unit = {
    val bytes = resource_name.getBytes("UTF-8")
    println("SERIALIZE RESNAME", resource_name)

    buf.write(T_RES)
    buf.write(bytes.size)
    buf.write(bytes)
  }

  def write_field(name: String, value: String) : Unit = {
    println("SERIALIZE FIELD", name, value)

    buf.write(T_FLD)

    val name_bytes = name.getBytes("UTF-8")
    buf.write(name_bytes.size)
    buf.write(name_bytes)

    if (value == null) {
      buf.write(0)
    } else {
      val value_bytes = value.getBytes("UTF-8")
      buf.write(value_bytes.size)
      buf.write(value_bytes)
    }
  }

  def write_phi(len: Int) : Unit = {
    println("SERIALIZE PHI")
    buf.write(T_PHI)
    buf.write(len)
  }

  def write_end() : Unit = {
    println("SERIALIZE END")
    buf.write(T_END)
  }

  def read_next() : Int = {
    buf.read_int()
  }

  def read_string() : String = {
    val len : Int = buf.read_int()

    if (len == 0)
      "null"
    else
      buf.read_string(len)
  }

}
