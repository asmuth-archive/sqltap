package com.paulasmuth.sqltap

import java.io._
import java.net._
import java.nio._
import java.nio.channels._
import scala.collection.mutable.ListBuffer

class FFPServer(port: Int){

  val BUFFER_SIZE = 256
  val REQUEST_SIZE = 20

  val selector = Selector.open
  val server_sock = ServerSocketChannel.open
  val connections = ListBuffer[FFPConnection]()

  val read_buffer = ByteBuffer.allocate(BUFFER_SIZE)

  class FFPConnection(sock: SocketChannel) {
    println("connection opened")

    val buffer = new Array[Byte](BUFFER_SIZE * 2)
    var buffer_len = 0

    sock.configureBlocking(false)
    val key = sock.register(selector, SelectionKey.OP_READ)
    key.attach(this)

    def yield_read : Unit = {
      val len = sock.read(read_buffer)

      if (len == -1)
        return connection_closed

      System.arraycopy(read_buffer.array, 0, buffer, buffer_len, len)
      buffer_len += len

      while (buffer_len >= REQUEST_SIZE) {
        val req = new Array[Byte](REQUEST_SIZE)
        System.arraycopy(buffer, 0, req, 0, REQUEST_SIZE)
        System.arraycopy(buffer, REQUEST_SIZE, buffer, 0, buffer_len-REQUEST_SIZE)
        buffer_len -= REQUEST_SIZE
        println("parsed request")
        println(javax.xml.bind.DatatypeConverter.printHexBinary(req).replaceAll(".{2}", "$0 "))
      }

      println("read " + len.toString + " bytes")
      println(buffer_len.toString + " bytes in buffer")
      println(javax.xml.bind.DatatypeConverter.printHexBinary(buffer).replaceAll(".{2}", "$0 "))
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
      def run = try { while (true) next }
        catch { case e: Exception => SQLTap.exception(e, true) }
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
