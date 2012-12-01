package com.paulasmuth.dpump

import java.sql.ResultSet;
import scala.collection.mutable.LinkedList;

class DBConnection(db_addr: String) {

  class DBResult(
    _head: Map[String, String],
    _data: LinkedList[List[String]]
  ) {
    val head = _head
    val data = _data
    var qtime: Long = 0
  }

  val conn = java.sql.DriverManager.getConnection(db_addr)
  val stmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)

  def execute(qry: String) : DBResult = {
    val strt = System.nanoTime()
    val rslt = execute_without_stopwatch(qry)
    rslt.qtime = System.nanoTime() - strt
    rslt
  }

  def execute_without_stopwatch(qry: String) : DBResult = {
    val rslt = stmt.executeQuery(qry)
    val meta = rslt.getMetaData()
    val enum = 1 to meta.getColumnCount()
    var data = new LinkedList[List[String]]()

    val head = (Map[String, String]() /: enum) (
      (h: Map[String, String], i: Int) =>
        h + ((meta.getColumnName(i), meta.getColumnTypeName(i))))

    while (rslt.next)
      data = (List[String]() /: enum) (
        _ :+ rslt.getString(_)) +: data

    new DBResult(head, data)
  }

}
