// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

//import java.io._
import java.nio.{ByteBuffer}
import java.net.{InetSocketAddress}
import java.nio.channels.{ServerSocketChannel,SelectionKey}
import java.nio.channels.spi.SelectorProvider

class Acceptor(workers: List[Worker]) {

  val loop = SelectorProvider.provider().openSelector()
  val ssock = ServerSocketChannel.open()
  ssock.configureBlocking(false)

  var seq = 0

  def run(port: Int) : Unit = {
    ssock.socket().bind(new InetSocketAddress("0.0.0.0", port))
    ssock.register(loop, SelectionKey.OP_ACCEPT)

    while (true) {
      loop.select()
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
      println("accept", seq)

      workers(seq).conn_queue.add(conn)
      workers(seq).loop.wakeup()
    }
  }

}
