package com.paulasmuth.dpump

class ResourceManifest(doc: xml.Node) {

  class Field(elem: xml.Node) {
    val name : String = elem.attribute("name").getOrElse("").toString

    if (name == "")
      throw new ParseException("missing required attribute: name => " + elem.toString)

  }

  class Relation(elem: xml.Node) {
    val name : String = elem.attribute("name").getOrElse("").toString

    if (name == "")
      throw new ParseException("missing required attribute: name => " + elem.toString)

  }


  if (doc.label != "resource")
    throw new ParseException("xml root must be one or more <resource> elements")

  val name : String = doc.attribute("name").getOrElse("").toString

  if (name == "")
    throw new ParseException("missing required attribute: name => " + doc.toString)

  val table_name : String = doc.attribute("table_name").getOrElse("").toString

  if (table_name == "")
    throw new ParseException("missing required attribute: table_name => " + doc.toString)

  val id_field : String = doc.attribute("id_field").getOrElse("").toString

  if (name == "")
    throw new ParseException("missing required attribute: id_field => " + doc.toString)

  val fields =
    (List[Field]() /: (doc \ "field"))(_ :+ new Field(_))

  val relations =
    (List[Relation]() /: (doc \ "relation"))(_ :+ new Relation(_))


  def relation(name: String) : Relation = {
    relations.find(_.name == name).getOrElse(null)
  }

}
