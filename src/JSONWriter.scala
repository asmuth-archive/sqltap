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

    if (first unary_!)
      buf.append(",\n")

    cur.name match {

      case "execute" =>
        { write("{\n"); scope = "}"; ind += 1; }

      case "findSingle" =>
        { write(json(cur.relation.name) + ": {\n"); scope = "}"; ind += 1; }

      case "findMulti" =>
        { write(json(cur.relation.name) + ": [\n"); scope = "]"; ind += 1; }

    }

    if (cur.name != "findMulti" && cur.record != null)
      { dump(cur); if (cur.next.length > 0) buf.append(",\n") }

    for ((nxt, ind) <- cur.next.zipWithIndex)
      next(nxt, ind == 0)

    if (scope != null)
      { buf.append("\n"); ind -= 1; write(scope) }

  }

  private def dump(cur: Instruction) =
    for ((field, ind) <- cur.record.fields.zipWithIndex) {
      if (ind != 0) buf.append(",\n")
      write(json(field) + ": " + json(cur.record.data(ind)))
    }

  private def json(str: String) : String =
    if (str == null) "null" else
      "\"" + str.replaceAll("\"", "'").replaceAll("\n", "") + "\"" // FIXPAUL

  private def write(str: String) : Unit =
    if (str != null) buf.append((INDENT * ind) + str)

  private def indent : Unit =
    buf.append(INDENT * ind)

}
