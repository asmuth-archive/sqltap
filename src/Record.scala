package com.paulasmuth.dpump

class Record(_resource: ResourceManifest) {
  val resource = _resource

  var fields = List[String]()
  var data   = List[String]()


  def id : Int =
    get(resource.id_field).toInt

  def set_id(id: Int) : Unit =
    set(resource.id_field, id.toString)

  def set_id(id: String) : Unit =
    set_id(id.toInt)

  def has_id : Boolean =
    fields.indexOf(resource.id_field) >= 0


  def load(_fields: List[String], _data: List[String]) =
    { fields = _fields; data = _data }


  def get(field: String) : String = {
    val idx = fields.indexOf(field)

    if (idx == -1)
      throw new ExecutionException("unknown field: " + field)

    data(idx)
  }


  def set(field: String, value: String) : Unit = {
    val idx = fields.indexOf(field)

    if (idx >= 0)
      data = data.updated(idx, value)
    else
      append(field, value)
  }


  private def append(field: String, value: String) : Unit = {
    fields = fields :+ field
    data   = data   :+ value
  }


}
