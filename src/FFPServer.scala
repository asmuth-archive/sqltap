package com.paulasmuth.sqltap

import java.io._
import java.net._
import java.nio._
import java.nio.channels._
import scala.collection.mutable.ListBuffer
import java.math.BigInteger
import java.util.concurrent._

class FFPServer(port: Int, num_threads: Int){

  val BUFFER_SIZE = 4096 * 8
  val REQUEST_SIZE = 20

  val selector = Selector.open
  val server_sock = ServerSocketChannel.open
  val connections = ListBuffer[FFPConnection]()

  val tmp_buffer = ByteBuffer.allocate(BUFFER_SIZE)
  val thread_pool = Executors.newFixedThreadPool(num_threads)

  class FFPConnection(sock: SocketChannel) {
    SQLTap.log_debug("[FFP] connection opened")

    val rbuf = new Array[Byte](BUFFER_SIZE * 2)
    var rbuf_len = 0
    val wbuf = new Array[Byte](BUFFER_SIZE * 2)
    var wbuf_len = 0

    sock.configureBlocking(false)
    val key = sock.register(selector, SelectionKey.OP_READ)
    key.attach(this)

    def yield_read : Unit = {
      val len = sock.read(tmp_buffer)

      if (len == -1)
        return connection_closed

      System.arraycopy(tmp_buffer.array, 0, rbuf, rbuf_len, len)
      rbuf_len += len

      while (rbuf_len >= REQUEST_SIZE) {
        val req_magic  = new Array[Byte](2)
        val req_id     = new Array[Byte](8)
        val req_res_id = new Array[Byte](2)
        val req_rec_id = new Array[Byte](6)

        System.arraycopy(rbuf, 0,  req_magic,  0, 2)
        System.arraycopy(rbuf, 2,  req_id,     0, 8)
        System.arraycopy(rbuf, 12, req_res_id, 0, 2)
        System.arraycopy(rbuf, 14, req_rec_id, 0, 6)

        System.arraycopy(rbuf, REQUEST_SIZE, rbuf, 0, rbuf_len-REQUEST_SIZE)
        rbuf_len -= REQUEST_SIZE

        if ((req_magic(0) == 0x17 && req_magic(1) == 0x01) unary_!) {
          SQLTap.log("[FFP] read invalid magic bytes")
        } else {
          val res_id = new BigInteger(req_res_id)
          val rec_id = new BigInteger(req_rec_id)
          val pquery = SQLTap.prepared_queries_ffp.getOrElse(res_id.intValue, null)

          if (pquery == null)
            SQLTap.log("[FFP] query for invalid resource_id: " + res_id.toString)

          else
            execute_query(req_id, pquery.build(rec_id.intValue))

        }
      }
    }

    private def connection_closed :  Unit = {
      SQLTap.log_debug("[FFP] connection closed")
      sock.close
      key.cancel
    }

    private def execute_query(req_id: Array[Byte], query: String) : Unit = {
      SQLTap.log_debug("[FFP] Execute: " + query)

      thread_pool.execute(new Runnable {
        def run = try {
          val request = new Request(query,
            new PlainRequestParser, new RequestExecutor, new PrettyJSONWriter)

          request.run

          finish_query(req_id, request)
        } catch {
          case e: Exception => SQLTap.exception(e, true)
        }
      })
    }

    private def finish_query(req_id: Array[Byte], req: Request) : Unit = {
      // FIXPAUL: wrap in FFP response packet
      val res = req.resp_data.getBytes("UTF-8")

      if ((res.size + wbuf_len) > (BUFFER_SIZE * 2))
        return SQLTap.log("[FFP] write buffer overflow, discarding request")

      wbuf.synchronized {
        System.arraycopy(res, 0, wbuf, wbuf_len, res.size)
        wbuf_len += res.size
      }
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

    SQLTap.log("Listening on ffp://0.0.0.0:" + port)
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

  private def hexdump(a: Array[Byte]) : String =
    javax.xml.bind.DatatypeConverter.printHexBinary(a).replaceAll("(.{2})", "$1 ")

}
