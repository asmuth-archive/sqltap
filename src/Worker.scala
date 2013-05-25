// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLConnection}
import java.nio.channels.{ServerSocketChannel,SelectionKey,SocketChannel}
import java.nio.channels.spi.SelectorProvider
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.mutable.ListBuffer

class Worker() extends Thread {

  val loop = SelectorProvider.provider().openSelector()
  val conn_queue = new ConcurrentLinkedQueue[SocketChannel]()

  private val sql_conns        = new ListBuffer[SQLConnection]()
  private val sql_conns_idle   = new ListBuffer[SQLConnection]()
  private var sql_conns_max    = 20
  private var sql_conns_num    = 0
  private val sql_queue        = new ListBuffer[SQLQuery]()
  private var sql_queue_maxlen = 100

  override def run : Unit = while (true) {
    println("select...")
    loop.select()

    while (!conn_queue.isEmpty)
      accept()

    val events = loop.selectedKeys().iterator()

    while (events.hasNext) {
      val event = events.next()
      events.remove()

      if (event.isValid) event.attachment match {
        case conn: HTTPConnection => try {

          if (event.isReadable)
            conn.read(event)

        } catch {
          case e: Exception => conn.error(e)
        }

        case conn: mysql.SQLConnection => try {

          if (event.isConnectable)
            conn.ready(event)

          if (event.isValid && event.isReadable)
            conn.read(event)

          if (event.isValid && event.isWritable)
            conn.write(event)

        } catch {
          case e: Exception => {
            SQLTap.error("[SQL] exception: " + e.toString, false)
            conn.close
          }
        }
      }
    }
  }

  def execute_sql_query(query: SQLQuery) : Unit = {
    val conn = get_sql_connection

    if (conn == null) {
      if (sql_queue.length >= sql_queue_maxlen)
        throw new TemporaryException("sql queue is full")

      sql_queue += query
    } else {
      conn.execute(query)
    }
  }

  def sql_connection_ready(conn: SQLConnection) : Unit = {
    sql_conns_idle += conn

    for (n <- (0 until sql_queue.length)) {
      val exec_conn = get_sql_connection()

      if (exec_conn != null) {
        exec_conn.execute(sql_queue.remove(0))
      }
    }
  }

  def sql_connection_close(conn: SQLConnection) : Unit = {
    sql_conns -= conn
    sql_conns_idle -= conn
    sql_conns_num -= 1
  }

  private def get_sql_connection() : SQLConnection = {
    if (sql_conns_num < sql_conns_max)
      new_sql_connection()

    if (sql_conns_idle.length > 0)
      return sql_conns_idle.remove(0)

    return null
  }

  private def new_sql_connection() : Unit = {
    val conn = new mysql.SQLConnection(this)

    if (SQLTap.CONFIG contains 'mysql_host)
      conn.hostname = SQLTap.CONFIG('mysql_host)

    if (SQLTap.CONFIG contains 'mysql_port)
      conn.port = SQLTap.CONFIG('mysql_port).toInt

    if (SQLTap.CONFIG contains 'mysql_user)
      conn.username = SQLTap.CONFIG('mysql_user)

    if (SQLTap.CONFIG contains 'mysql_pass)
      conn.password = SQLTap.CONFIG('mysql_pass)

    if (SQLTap.CONFIG contains 'mysql_db)
      conn.database = SQLTap.CONFIG('mysql_db)

    conn.connect()

    sql_conns_num += 1
    sql_conns     += conn
  }

  private def accept() : Unit = {
    val conn : SocketChannel = conn_queue.poll()

    if (conn == null)
      return

    conn.configureBlocking(false)

    conn
      .register(loop, SelectionKey.OP_READ)
      .attach(new HTTPConnection(conn, this))
  }

}
