package com.paulasmuth.sqltap

object JSONHelper {

  val sub_table = List(
    (("\"", """\\"""")),
    (("\r", "")),
    (("\n", ""))
  )

  def escape(str: String) : String =
    sub_table.foldLeft(str) ((s, cur) =>
      s.replaceAll(cur._1, cur._2))

}
