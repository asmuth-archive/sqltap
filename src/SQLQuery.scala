// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.LinkedList
import scala.collection.mutable.ListBuffer

class SQLQuery(query_str: String) {

  val query    : String = query_str
  var columns  = new ListBuffer[String]()
  var rows     = new LinkedList[LinkedList[String]]()
  var num_cols = 0

  def ready() : Unit = {
    println("SQL_QUERY_READY", query, columns, rows)
  }

}
