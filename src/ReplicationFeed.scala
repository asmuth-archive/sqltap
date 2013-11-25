// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLConnection,AbstractSQLConnectionPool}
import java.nio.channels.{ServerSocketChannel,SelectionKey,SocketChannel}
import java.nio.channels.spi.SelectorProvider

/**
 * The ReplicationFeed thread is responsible only for a single SQL Connection
 * that pulls the binlog event stream from MySQL.
 */
object ReplicationFeed extends Thread with AbstractSQLConnectionPool {
  val loop = SelectorProvider.provider().openSelector()
  Logger.log("replication feed starting...")

  def ready(conn: SQLConnection) : Unit = {
    println("start the engines! ;)")
  }

  def close(conn: SQLConnection) : Unit = {
    throw new Exception("SQL connection closed")
  }

  def busy(conn: SQLConnection) : Unit = ()

  override def run : Unit = try {
    val conn = new SQLConnection(this)
    conn.configure(Config.get())
    conn.connect()

    while (true) {
      println("SELECT")
      loop.select(java.lang.Long.MAX_VALUE)
      val events = loop.selectedKeys().iterator()

      while (events.hasNext) {
        val event = events.next()
        events.remove()

        if (event.isValid) {
          val conn = event.attachment.asInstanceOf[mysql.SQLConnection]

          if (event.isConnectable) {
            println("CONNECTED!")
            conn.ready(event)
          }

          if (event.isValid && event.isReadable) {
            conn.read(event)
          }

          if (event.isValid && event.isWritable) {
            conn.write(event)
          }
        }
      }
    }

    Logger.log("replication feed closed")
  } catch {
    case e: Exception => {
      Logger.error("[SQL] [Replication] exception: " + e.toString, false)
      Logger.exception(e, true)
      //conn.close(e) // FIXPAUL: reconnect
    }
  }

}
