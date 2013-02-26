// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class PreparedQuery(doc: xml.Node) {

  val elem = new XMLHelper(doc)

  val name : String =
    elem.attr("name", true)

  val query : String =
    elem.attr("query", true)

  def build(id: Int) : String =
    query.replace("$$", id.toString)

  def cache_key(id: Int) : String =
    "sqltap-cache-" + name + "-" + id.toString

}
