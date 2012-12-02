package com.paulasmuth.dpump

class ResourceManifest(doc: xml.Node) {

  class Field(elem: xml.Node) {

    val name : String = doc.attribute("name").getOrElse("").toString

    if (name == "")
      throw new ParseException("missing required attribute: name => " + doc.toString)

  }

  class HasOneRelation {}
  class HasManyRelation {}
  class BelongsToRelation {}

  if (doc.label != "resource")
    throw new ParseException("xml root must be one or more <resource> elements")

  val name : String = doc.attribute("name").getOrElse("").toString

  if (name == "")
    throw new ParseException("missing required attribute: name => " + doc.toString)

  val table_name : String = doc.attribute("name").getOrElse("").toString

  if (table_name == "")
    throw new ParseException("missing required attribute: table_name => " + doc.toString)

  val id_field : String = doc.attribute("id_field").getOrElse("").toString

  if (name == "")
    throw new ParseException("missing required attribute: id_field => " + doc.toString)

  val fields =
    (List[Field]() /: (doc \ "field"))(_ :+ new Field(_))

}
