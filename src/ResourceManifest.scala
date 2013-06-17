// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class ResourceManifest(doc: xml.Node) {

  class ResourceField(doc: xml.Node) {
    val elem = new XMLHelper(doc)

    val name : String =
      elem.attr("name", true)
  }

  val elem = new XMLHelper(doc)

  if (doc.label != "resource")
    throw new ParseException("xml root must be one or more <resource> elements")


  val name : String =
    elem.attr("name", true)

  val table_name : String =
    elem.attr("table_name", true)

  val id_field : String =
    elem.attr("id_field", false, "id")

  val default_order : String =
    elem.attr("default_order", false, id_field + " DESC")

  val relations =
    ((List[ResourceRelation]() /: (doc \ "relation"))
      (_ :+ new RealResourceRelation(_)))

  val fields =
    ((List[ResourceField]() /: (doc \ "field"))
      (_ :+ new ResourceField(_)))

  val field_names : List[String] =
    fields.map(_.name)

  def field(name: String) : ResourceField = {
    fields.find(_.name == name).getOrElse(null)
  }

  def relation(name: String) : ResourceRelation = {
    relations.find(_.name == name).getOrElse(null)
  }

  def to_relation : ResourceRelation =
    new DummyResourceRelation(this)

}
