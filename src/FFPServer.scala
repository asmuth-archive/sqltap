package com.paulasmuth.sqltap

import java.io._
import java.net._
import java.nio._
import java.nio.channels._
import scala.collection.mutable.ListBuffer

class FFPServer(port: Int){

  val selector = Selector.open
  val server_sock = ServerSocketChannel.open
  val connections = ListBuffer[FFPConnection]()

  class FFPConnection(sock: SocketChannel) {
    println("connection opened")

    val buf = ByteBuffer.allocate(8192)
    sock.configureBlocking(false)
    val key = sock.register(selector, SelectionKey.OP_READ)
    key.attach(this)

    def yield_read : Unit = {
      val len = sock.read(buf)

      if (len == -1)
        return connection_closed

      println("read " + len.toString + " bytes")
    }

    private def connection_closed :  Unit = {
      println("connection closed")
      sock.close
      key.cancel
    }


  }

  def start : Unit = {
    server_sock.socket.bind(new InetSocketAddress("0.0.0.0", port))
    server_sock.configureBlocking(false)
    server_sock.register(selector, SelectionKey.OP_ACCEPT)

    new Thread(new Runnable {
      def run = { while (true) next }
    }).start
  }

  private def next : Unit = {
    selector.select
    val keys = selector.selectedKeys.iterator

    while (keys.hasNext) {
      val key = keys.next
      keys.remove

      if (key.isAcceptable)
        connections += new FFPConnection(server_sock.accept)

      if (key.isReadable)
        key.attachment.asInstanceOf[FFPConnection].yield_read

    }
  }

}
