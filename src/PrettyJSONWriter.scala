// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

// FIXPAUL refactor this shit...
class PrettyJSONWriter(buf: ByteBuffer) {

  private val INDENT = "  "
  private var ind = 1

  def write_query(head: Query) : Unit =
    next_instruction(head, 0)

  private def next_instruction(cur: Instruction, index: Int) : Unit = {
    var scope : String = null

    if (index != 0)
      append(",\n")

    if (cur.name == "phi" || cur.name == "root")
      { write("{\n"); scope = "}"; ind += 1; }

    if (cur.name == "findMulti") {
      if (cur.next.length == 0)
        write(json(cur.relation.output_name) + ": []")
      else {
        write(json(cur.relation.output_name) + ": [\n")
        scope = "]"; ind += 1
      }

    } else if (cur.name == "count") {
      write(json(cur.relation.output_name) + ": ")
      write(cur.record.get("__count"))

    } else {
      if (cur.name == "findSingle") {
        write(json(cur.relation.name) + ": {\n")
        scope = "}"; ind += 1
      }

      if (cur.record != null) {
        write(json("__resource") + ": " +
          json(cur.record.resource.name))

        for ((field, ind) <- cur.record.fields.zipWithIndex) {
          append(",\n")
          write(json(field) + ": " + json(cur.record.data(ind)))
        }

        if (cur.next.length > 0)
          append(",\n")
      }
    }

    for ((nxt, nxt_ind) <- cur.next.zipWithIndex)
      next_instruction(nxt, nxt_ind)

    if (scope != null)
      { append("\n"); ind -= 1; write(scope) }

  }

  def write_error(error: String) = {
    buf.put("{ \"status\": \"error\", \"error\": \"".getBytes)
    buf.put(JSONHelper.escape(error).getBytes("UTF-8"))
    buf.put("\" }\n".getBytes)
  }

  def write_comma() : Unit =
    append(",\n")

  def write_array_begin() : Unit =
    append("[\n")

  def write_array_end() : Unit =
    append("\n]\r\n")

  private def json(str: String) : String =
    if (str == null) "null" else
      "\"" + JSONHelper.escape(str) + "\""

  private def write(str: String) : Unit =
    if (str != null) append((INDENT * ind) + str)

  private def append(str: String) : Unit =
    buf.put(str.getBytes("UTF-8"))

}
