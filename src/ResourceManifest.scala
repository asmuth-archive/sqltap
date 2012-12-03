package com.paulasmuth.dpump

class ResourceField(elem: xml.Node) {
  val name : String = elem.attribute("name").getOrElse("").toString

  if (name == "")
    throw new ParseException("missing required attribute: name => " + elem.toString)

}

trait ResourceRelation {
  val name : String
  def resource : ResourceManifest

  val rtype : String = "has_one"

  val join_foreign : Boolean
  val join_field : String
}

class RealResourceRelation(elem: xml.Node) extends ResourceRelation {
  val _resource : String = elem.attribute("resource").getOrElse("").toString

  val name : String = elem.attribute("name").getOrElse("").toString
  val join_field : String = elem.attribute("join_field").getOrElse("").toString
  val join_foreign : Boolean = false

  if (name == "")
    throw new ParseException("missing required attribute: name => " + elem.toString)

  def resource : ResourceManifest =
    DPump.manifest(_resource)
}

class DummyResourceRelation(_resource: ResourceManifest) extends ResourceRelation {
  val name = _resource.name

  val join_field : String = null
  val join_foreign : Boolean = false

  def resource : ResourceManifest =
    _resource
}

class ResourceManifest(doc: xml.Node) {

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
    (List[ResourceField]() /: (doc \ "field"))(_ :+ new ResourceField(_))

  val relations =
    (List[ResourceRelation]() /: (doc \ "relation"))(_ :+ new RealResourceRelation(_))
/*
  val relations =
    (List[ResourceRelation]() /: (doc \ "relation"))((l: List[ResourceRelation], x: xml.Node) =>
      (l :+ (new RealResourceRelation(x).asInstanceOf[ResourceRelation])))
*/

  def field(name: String) : ResourceField = {
    fields.find(_.name == name).getOrElse(null)
  }

  def relation(name: String) : ResourceRelation = {
    relations.find(_.name == name).getOrElse(null)
  }

  def to_relation : ResourceRelation =
    new DummyResourceRelation(this)

}
