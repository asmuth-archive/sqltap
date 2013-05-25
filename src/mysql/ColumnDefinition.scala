// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap.mysql

class ColumnDefinition {

  private var catalog   : (String, Int) = null
  private var schema    : (String, Int) = null
  private var table     : (String, Int) = null
  private var org_table : (String, Int) = null
  private var name      : (String, Int) = null
  private var org_name  : (String, Int) = null

  def load(data: Array[Byte]) = {
    println("load-col-def", data.size, javax.xml.bind.DatatypeConverter.printHexBinary(data))

    catalog   = LengthEncodedString.read(data, 0)
    schema    = LengthEncodedString.read(data, catalog._2)
    table     = LengthEncodedString.read(data, schema._2)
    org_table = LengthEncodedString.read(data, table._2)
    name      = LengthEncodedString.read(data, org_table._2)
    org_name  = LengthEncodedString.read(data, name._2)

    println(catalog, schema, table, org_table, name, org_name)
  }

}
