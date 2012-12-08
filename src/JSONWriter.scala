package com.paulasmuth.dpump

class JSONWriter(req: Request) {

  val INDENT = "  "
  var ind = 0

  val buf = new StringBuffer

  def run : Unit = {
    next(req.stack.root)
    req.resp_data = buf.toString
  }

  private def next(cur: Instruction) : Unit = {
    var s : (() => Unit) = null

    cur.name match {

      case "execute" =>
        { indent; s = scope((("{","}"))) }

      case "findSingle" =>
        { write(json(cur.relation.name) + ": "); s = scope((("{","}"))) } 

      case "findMulti" =>
        { write(json(cur.relation.name) + ": "); s = scope((("[","]"))) }

    }

    for (nxt <- cur.next)
      next(nxt)

    if (s != null) s()

    buf.append(",\n")
  }

  private def json(str: String) : String =
    "\"" + str.replaceAll("\"", "\\\"") + "\""

  private def write(str: String) : Unit =
    if (str != null) buf.append((INDENT * ind) + str)

  private def scope(s: (String, String)) : (() => Unit) =
    { buf.append(s._1 + "\n"); ind += 1; (() =>
      { ind -= 1; write(s._2) })}

  private def indent : Unit =
    buf.append(INDENT * ind)

}
