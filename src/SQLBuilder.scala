package com.paulasmuth.dpump

object SQLBuilder {

  def sql_find_one(res: ResourceManifest, fields: List[String], id_field: String, id: Int) =
    "select " + sql_fields(res, fields) + " from " + res.table_name +
    " where `" + id_field + "` = " + id.toString + ";"

  def sql_find_some(res: ResourceManifest, fields: List[String], id_field: String, id: Int, cond: String, order: String, limit: Int, offset: Int) =
    "select " + sql_fields(res, fields) + " from " + res.table_name +
    " where `" + id_field + "` = " + id.toString +
    " order by " + order + " limit " + limit.toString + " offset " +
    offset.toString + ";"

  private def sql_fields(res: ResourceManifest, fields: List[String]) = {
    if (fields.size == 1 && fields.head == "*")
      "*"
    else
      fields.map(res.table_name + ".`" + _ + "`").mkString(", ")
  }

}
