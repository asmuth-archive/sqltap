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

        case conn: SQLConnection => {

          if (event.isConnectable)
            conn.ready(event)

        }
      }
    }
  }

  def get_sql_connection() : Unit = {
    printf("opening a new connection to mysql...")

    val conn = new SQLConnection(this)
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
