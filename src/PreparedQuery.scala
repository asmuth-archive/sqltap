package com.paulasmuth.sqltap

class PreparedQuery(doc: xml.Node) {

  val elem = new XMLHelper(doc)

  val name : String =
    elem.attr("name", true)

  val query : String =
    elem.attr("query", true)

  val ffp_id : String =
    elem.attr("ffp_id", false)


  def build(id: Int) =
    query.replace("$$", id.toString)

}
