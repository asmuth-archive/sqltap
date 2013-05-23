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

  override def run : Unit = {
    println("worker started...")

    while (true) {
      loop.select()

      while (!conn_queue.isEmpty)
        accept()

      val events = loop.selectedKeys().iterator()

      while (events.hasNext) {
        next(events.next())
        events.remove()
      }
    }
  }

  private def next(event: SelectionKey) : Unit = {
    if (!event.isValid)
      return

    println(event)
  }

  private def accept() : Unit = {
    val conn : SocketChannel = conn_queue.poll()

    if (conn == null)
      return

    conn.configureBlocking(false)
    conn.register(loop, SelectionKey.OP_READ)

    println("yeah!")
  }

}
