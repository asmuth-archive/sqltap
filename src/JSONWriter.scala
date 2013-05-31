// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

class JSONWriter(buf: AbstractWrappedBuffer) {

  private def write(str: String) : Unit = {
    buf.write(str.getBytes("UTF-8"))
  }

  def write_comma() : Unit = {
    write(",")
  }

  def write_object_begin() : Unit = {
    write("{")
  }

  def write_object_end() : Unit = {
    write("}")
  }

  def write_array_begin() : Unit = {
    write("[")
  }

  def write_array_end() : Unit = {
    write("]")
  }

  def write_string(str: String) : Unit = {
    if (str == null) {
      write("null")
    } else {
      write("\"")
      write_escaped(str)
      write("\"")
    }
  }

  def write_tuple(left: String, right: String) : Unit = {
    write_string(left)
    write(": ")
    write_string(right)
  }

  def write_error(error: String) = {
    write_object_begin()
    write_tuple("status", "error")
    write_comma()
    write_tuple("error", error)
    write_object_end()
  }

  def write_map(map: Map[String, String]) = {
    var n = 0

    write_object_begin()

    for (tuple <- map) {
      if (n != 0)
        write_comma()

      write_tuple(tuple._1, tuple._2)

      n += 1
    }

    write_object_end()
  }

  // FIXPAUL: escape...
  def write_escaped(str: String) {
    for (byte <- str.getBytes("UTF-8")) {
      buf.write(byte)
    }
  }

  def write_query(head: Query) : Unit = {
    write_instruction(head, 0)
  }

  def write_instruction(cur: Instruction, index: Int) : Unit = {
    var append : String = null

    if (index != 0)
      write_comma()

    if (cur.name == "phi" || cur.name == "root") { 
      write_object_begin()
      append = "}"
    }

    if (cur.name == "findMulti") {
      if (cur.next.length == 0) {
        write_string(cur.relation.output_name)
        write(": []")
      } else {
        write_string(cur.relation.output_name)
        write(": [")
        append = "]"
      }

    }

    else if (cur.name == "count") {
      write_string(cur.relation.output_name)
      write(": ")
      write(cur.record.get("__count"))
    }

    else {
      if (cur.name == "findSingle") {
        write_string(cur.relation.name)
        write(": {")
        append = "}"
      }

      if (cur.record != null) {
        write_string("__resource")
        write(": ")
        write_string(cur.record.resource.name)

        for ((field, ind) <- cur.record.fields.zipWithIndex) {
          write_comma()
          write_tuple(field, cur.record.data(ind))
        }

        if (cur.next.length > 0) {
          write_comma()
        }
      }
    }

    for ((nxt, nxt_ind) <- cur.next.zipWithIndex) {
      write_instruction(nxt, nxt_ind)
    }

    if (append != null) {
      write(append)
    }
  }

}
