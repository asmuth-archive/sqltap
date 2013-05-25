// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.channels.{ServerSocketChannel,SelectionKey,SocketChannel}
import java.nio.channels.spi.SelectorProvider
import java.util.concurrent.ConcurrentLinkedQueue

class Worker() extends Thread {

  val loop = SelectorProvider.provider().openSelector()
  val conn_queue = new ConcurrentLinkedQueue[SocketChannel]()

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
        case conn: HTTPConnection => {

          if (event.isReadable)
            conn.read(event)

        }

        case conn: mysql.SQLConnection => {

          if (event.isConnectable)
            conn.ready(event)

          if (event.isValid && event.isReadable)
            conn.read(event)

          if (event.isValid && event.isWritable)
            conn.write(event)

        }
      }
    }
  }

  def execute_sql_query(query: SQLQuery) : Unit = {
    println("execute sql query...")
  }

  def get_sql_connection() : Unit = {
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
  }

  private def accept() : Unit = {
    val conn : SocketChannel = conn_queue.poll()

    if (conn == null)
      return

    conn.configureBlocking(false)

    conn
      .register(loop, SelectionKey.OP_READ)
      .attach(new HTTPConnection(conn, this))

    println("yeah!")
  }

}
