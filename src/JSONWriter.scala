package com.paulasmuth.dpump

class JSONWriter(req: Request) {

  val INDENT = "  "
  var ind = 0

  val buf = new StringBuffer

  def run : Unit = {
    next(req.stack.root, true)
    req.resp_data = buf.toString
  }

  private def next(cur: Instruction, first: Boolean = false) : Unit = {
    var scope : String = null
    var scopei = 1

    if (first unary_!)
      buf.append(",\n")

    if (cur.name == "execute" && cur.prev == null)
      { write("[\n"); scope = "]"; ind += 1; }
    else if (cur.name == "execute")
      { write("{\n"); scope = "}"; ind += 1; }

    if (cur.name == "findMulti") {
      write(json(cur.relation.name) + ": [\n")
      scope = "]"; ind += 1
    } else {

      if (cur.name == "findSingle") {
        write("{\n"); ind += 1
        write(json(cur.relation.name) + ": {\n")
        scope = INDENT + "}\n" + (INDENT*(ind-1)) + "}"
        ind += 1; scopei = 2
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

    for ((nxt, ind) <- cur.next.zipWithIndex)
      next(nxt, ind == 0)

    if (scope != null)
      { buf.append("\n"); ind -= scopei; write(scope) }

  }

  private def json(str: String) : String =
    if (str == null) "null" else
      "\"" + str.replaceAll("\"", "'").replaceAll("\n", "") + "\"" // FIXPAUL

  private def write(str: String) : Unit =
    if (str != null) buf.append((INDENT * ind) + str)

}
