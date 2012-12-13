package com.paulasmuth.dpump

class PrettyJSONWriter extends RequestVisitor {

  val INDENT = "  "
  var ind = 1

  val buf = new StringBuffer

  val sub_table = List(
    (("\"", """\\"""")),
    (("\r", "")),
    (("\n", ""))
  )

  def run : Unit = {
    buf.append("[\n")

    for (ind <- (0 until req.stack.root.next.length))
      next(req.stack.root, ind)

    buf.append("\n]")
    req.resp_data = buf.toString
  }

  private def next(cur: Instruction, index: Int) : Unit = {
    var scope : String = null

    if (index != 0)
      buf.append(",\n")

    if (cur.name == "execute")
      { write("{\n"); scope = "}"; ind += 1; }

    if (cur.name == "findMulti") {
      write(json(cur.relation.name) + ": [\n")
      scope = "]"; ind += 1
    } else {

      if (cur.name == "findSingle") {
        write(json(cur.relation.name) + ": {\n")
        scope = "}"; ind += 1
      }

      if (cur.record != null) {
        write(json("__resource") + ": " +
          json(cur.record.resource.name))

        for ((field, ind) <- cur.record.fields.zipWithIndex) {
          buf.append(",\n")
          write(json(field) + ": " + json(cur.record.data(ind)))
        }

        if (cur.next.length > 0)
          buf.append(",\n")
      }
    }

    if (cur.name == "execute" && cur.prev == null)
      next(cur.next(index), index)

    else
      for ((nxt, nxt_ind) <- cur.next.zipWithIndex)
        next(nxt, nxt_ind)

    if (scope != null)
      { buf.append("\n"); ind -= 1; write(scope) }

  }

  private def json(str: String) : String =
    if (str == null) "null" else
      "\"" + escape(str) + "\""

  private def escape(str: String) : String =
    sub_table.foldLeft(str) ((s, cur) =>
      s.replaceAll(cur._1, cur._2))


  private def write(str: String) : Unit =
    if (str != null) buf.append((INDENT * ind) + str)

}
