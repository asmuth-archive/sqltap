package com.paulasmuth.dpump

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

  val default_order : String =
    elem.attr("default_order", false, "id DESC")

  val id_field : String =
    elem.attr("id_field", false, "id")


  val relations =
    ((List[ResourceRelation]() /: (doc \ "relation"))
      (_ :+ new RealResourceRelation(_)))

  val fields =
    ((List[ResourceField]() /: (doc \ "field"))
      (_ :+ new ResourceField(_)))


  def field(name: String) : ResourceField = {
    fields.find(_.name == name).getOrElse(null)
  }

  def relation(name: String) : ResourceRelation = {
    relations.find(_.name == name).getOrElse(null)
  }

  def to_relation : ResourceRelation =
    new DummyResourceRelation(this)

}
