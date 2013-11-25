// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import com.paulasmuth.sqltap.mysql.{SQLQuery,SQLConnectionPool}
import java.nio.channels.{ServerSocketChannel,SelectionKey,SocketChannel}
import java.nio.channels.spi.SelectorProvider
import java.util.concurrent.{ConcurrentLinkedQueue}
import java.util.concurrent.atomic.{AtomicInteger}

/**
 * The ReplicationFeed thread is responsible only for a single SQL Connection
 * that pulls the binlog event stream from MySQL.
 */
object ReplicationFeed extends Thread {
  val loop = SelectorProvider.provider().openSelector()

  Logger.log("replication feed starting...")

  override def run : Unit = try {

    while (true) {
      println("SELECT")
      loop.select(java.lang.Long.MAX_VALUE)
      val events = loop.selectedKeys().iterator()

      while (events.hasNext) {
        val event = events.next()
        events.remove()

        if (event.isValid) {
          val conn = event.asInstanceOf[mysql.SQLConnection]

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
