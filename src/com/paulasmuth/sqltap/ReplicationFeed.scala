// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql._
import java.nio.channels.{ServerSocketChannel,SelectionKey,SocketChannel}
import java.nio.channels.spi.SelectorProvider

/**
 * The ReplicationFeed thread is responsible only for a single SQL Connection
 * that pulls the binlog event stream from MySQL.
 */
object ReplicationFeed extends Thread with AbstractSQLConnectionPool {
  val loop = SelectorProvider.provider().openSelector()
  private val query     = new SQLQuery("SHOW BINARY LOGS;")

  Logger.log("replication feed starting...")

  def binlog(event: BinlogEvent) : Unit = event match {
    case evt: UpdateRowsBinlogEvent => {
      println("UPDATE", evt.table_name, evt.primary_key)
    }

    case _ => ()
  }

  def ready(conn: SQLConnection) : Unit = {
    if (query.running) {
      conn.execute(query)
    } else {
      val row = query.rows.last
      val position = row.last.toInt
      val filename = row.first

      conn.start_binlog(filename, position)
    }
  }

  def close(conn: SQLConnection) : Unit = {
    throw new Exception("SQL connection closed")
  }

  def busy(conn: SQLConnection) : Unit = ()

  override def run : Unit = try {
    val conn = new SQLConnection(this)
    conn.configure(Config.get() + (('binlog, "enable")))
    conn.connect()

    while (true) {
      loop.select()
      val events = loop.selectedKeys().iterator()

      while (events.hasNext) {
        val event = events.next()
        events.remove()

        if (event.isValid) {
          val conn = event.attachment.asInstanceOf[mysql.SQLConnection]

          if (event.isConnectable) {
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
