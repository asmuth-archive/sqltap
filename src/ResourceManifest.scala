package com.paulasmuth.dpump

class ResourceManifest(doc: xml.Node) {

  class ParseException(msg: String) extends Exception{
    override def toString = msg
  }

  class Field(elem: xml.Node) {

    val name = doc.attribute("name").getOrElse(null)

    if (name == null)
      throw new ParseException("missing required attribute: name => " + doc.toString)

    //println(f.attribute("nadme").getOrElse(null).toString) )
  }

  class HasOneRelation {}
  class HasManyRelation {}
  class BelongsToRelation {}

  if (doc.label != "resource")
    throw new ParseException("xml root must be one or more <resource> elements")

  val name = doc.attribute("name").getOrElse(null)

  if (name == null)
    throw new ParseException("missing required attribute: name => " + doc.toString)

  val table_name = doc.attribute("name").getOrElse(null)

  if (table_name == null)
    throw new ParseException("missing required attribute: table_name => " + doc.toString)

  val id_field = doc.attribute("id_field").getOrElse(null)

  if (name == null)
    throw new ParseException("missing required attribute: id_field => " + doc.toString)

  val fields =
    (List[Field]() /: (doc \ "field"))(_ :+ new Field(_))

}
