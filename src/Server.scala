// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}
import java.net.{InetSocketAddress}
import java.nio.channels.{ServerSocketChannel,SelectionKey}
import java.nio.channels.spi.SelectorProvider
import scala.collection.mutable.{ListBuffer}

class Server(num_workers : Int) {

  private val TICK = 100
  var workers = new ListBuffer[Worker]()

  private val watchdog = new Watchdog(this)
  private var seq      = 0
  private val loop     = SelectorProvider.provider().openSelector()
  private val ssock    = ServerSocketChannel.open()

  def run(port: Int) : Unit = {
    ssock.configureBlocking(false)
    ssock.socket().bind(new InetSocketAddress("0.0.0.0", port))
    ssock.register(loop, SelectionKey.OP_ACCEPT)

    while (true) {
      for (n <- (0 until (num_workers - workers.length)))
        start_worker()

      loop.select(TICK)

      try {
        watchdog.run()
      } catch {
        case e: Exception => {
          SQLTap.error("error running watchdog: " + e.toString, false)
          SQLTap.exception(e, false)
        }
      }

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

    if (event.isAcceptable) {
      val conn = ssock.accept()

      seq = (seq + 1) % workers.size

      workers(seq).queue.add(conn)
      workers(seq).loop.wakeup()
      workers(seq).requests_queued.incrementAndGet()
    }
  }

  private def start_worker() : Unit = {
    val worker = new Worker()
    worker.start()
    workers += worker
  }

}
