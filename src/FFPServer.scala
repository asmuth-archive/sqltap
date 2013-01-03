package com.paulasmuth.sqltap

import java.io._
import java.net._
import java.nio._
import java.nio.channels._
import scala.collection.mutable.ListBuffer
import java.math.BigInteger

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
        val _req_magic  = new Array[Byte](2)
        val _req_id     = new Array[Byte](8)
        val _req_flags  = new Array[Byte](2)
        val _req_res_id = new Array[Byte](2)
        val _req_rec_id = new Array[Byte](6)

        System.arraycopy(buffer, 0,  _req_magic,  0, 2)
        System.arraycopy(buffer, 2,  _req_id,     0, 8)
        System.arraycopy(buffer, 10, _req_flags,  0, 2)
        System.arraycopy(buffer, 12, _req_res_id, 0, 2)
        System.arraycopy(buffer, 14, _req_rec_id, 0, 6)

        System.arraycopy(buffer, REQUEST_SIZE, buffer, 0, buffer_len-REQUEST_SIZE)
        buffer_len -= REQUEST_SIZE

        if ((_req_magic(0) == 0x17 && _req_magic(1) == 0x01) unary_!)
          return println("invalid magic")

        val req_id = new BigInteger(_req_id)
        val res_id = new BigInteger(_req_res_id)
        val rec_id = new BigInteger(_req_rec_id)

        println("parsed request (" + req_id.toString + "): " + res_id.toString + "#" + rec_id.toString)
      }
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
