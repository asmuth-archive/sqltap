package com.paulasmuth.dpump

class JSONWriter(req: Request) {

  val INDENT = "  "
  var ind = 0

  def run : Unit =
    next(req.stack.root)

  private def next(cur: Instruction) : Unit = {
    var s : (() => Unit) = null

    cur.name match {

      case "execute" =>
        if (cur == req.stack.root) s = scope((("{","}")))

      case "findSingle" =>
        { write(json(cur.relation.name) + ":"); s = scope((("{","}"))) } 

      case "findMulti" =>
        { write(json(cur.relation.name) + ":"); s = scope((("[","]"))) }

      case _ => null

    }

    for (nxt <- cur.next)
      next(nxt)

    if (s != null) s()

    write(",\n")
  }

  private def json(str: String) : String =
    "\"" + str.replaceAll("\"", "\\\"") + "\""

  private def write(str: String) : Unit =
    if (str != null) req.resp_data += (INDENT * ind) + str

  private def scope(s: (String, String)) : (() => Unit) =
    { req.resp_data += " " + s._1 + "\n"; ind += 1; (() =>
      { ind -= 1; write(s._2) })}

}
