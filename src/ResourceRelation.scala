package com.paulasmuth.dpump

trait ResourceRelation {
  val name : String
  def resource : ResourceManifest

  val join_foreign : Boolean
  val join_field : String
  val join_cond : String
}


class DummyResourceRelation(_res: ResourceManifest) extends ResourceRelation {
  val name = _res.name

  val join_field : String = null
  val join_cond : String = null
  val join_foreign : Boolean = false

  def resource : ResourceManifest =
    _res
}

class RealResourceRelation(doc: xml.Node) extends ResourceRelation {
  val elem = new XMLHelper(doc)

  val _resource : String =
    elem.attr("resource", true)

  val name : String = 
    elem.attr("name", true)

  val join_field : String =
    elem.attr("join_field", true)

  val join_cond : String =
    elem.attr("join_cond", false)

  val join_foreign : Boolean = 
    (elem.attr("join_foreign", false) == "true")

  def resource : ResourceManifest =
    DPump.manifest(_resource)
}

