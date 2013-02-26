package com.paulasmuth.sqltap

import java.util.logging._
import net.spy.memcached.MemcachedClient
import net.spy.memcached.AddrUtil
import net.spy.memcached.transcoders.Transcoder
import net.spy.memcached.CachedData
import java.util.concurrent._
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object PreparedQueryCache {
  System.setProperty("net.spy.log.LoggerImpl",
    "net.spy.memcached.compat.log.SunLogger");

  Logger.getLogger("net.spy.memcached")
    .setLevel(Level.SEVERE);

  class PlainMemcachedTranscoder extends Transcoder[String] {
    def getMaxSize : Int = 4096 * 8
    def asyncDecode(d: CachedData) : Boolean = true

    def encode(s: String) : CachedData = {
      val target = new ByteArrayOutputStream()
      val gzip   = new GZIPOutputStream(target)
      val data   = s.getBytes("UTF-8")

      gzip.write(data, 0, data.size)
      gzip.finish
      gzip.close

      target.close

      new CachedData(3, target.toByteArray, getMaxSize);
    }

    def decode(d: CachedData) : String = try {
      val buffer = new Array[Byte](getMaxSize)
      val source = new ByteArrayInputStream(d.getData);
      val gzip   = new GZIPInputStream(source);

      var read_len = 0
      var read = 0

      while (read >= 0) {
        read = gzip.read(buffer, read_len, buffer.size - read_len)

        if (read > 0)
          read_len += read
      }

      gzip.close
      source.close

      new String(buffer, 0, read_len, "UTF-8")
    } catch {
      case e: Exception => {
        SQLTap.error("[MEMCACHE] " + e, false)
        return "error"
      }
    }

  }

  class LocalMemcachedClient extends ThreadLocal[MemcachedClient] {
    override protected def initialValue : MemcachedClient =
      if (SQLTap.CONFIG contains 'memcached) {
        SQLTap.log("Connecting to memcached...")
        new MemcachedClient(AddrUtil.getAddresses(SQLTap.CONFIG('memcached)))
      } else null
  }

  val memcached_ttl = SQLTap.CONFIG('memcached_ttl).toInt
  val memcached_local = new LocalMemcachedClient

  def execute(query: PreparedQuery, ids: List[Int]) : Request = {
    var memcached = memcached_local.get

    val buf = new StringBuffer
    val keys : List[String] = ids.map{ id => query.cache_key(id) }
    var cached : java.util.Map[String, String] = null

    if (memcached != null) {
      cached = memcached.getBulk(new PlainMemcachedTranscoder, keys:_*)
    }

    (0 until ids.length).foreach { ind =>
      val request = new Request(query.build(ids(ind)),
        new PlainRequestParser, new RequestExecutor, new PrettyJSONWriter)

      request.resp_data = cached.get(keys(ind))

      if (request.resp_data != null)
        request.ready = true

      if (request.ready unary_!) {
        request.run

        memcached.set(keys(ind), memcached_ttl,
          request.resp_data, new PlainMemcachedTranscoder)
      }


      buf.append(
        if (ind == 0) "[" else ",")

      buf.append(request.resp_data
        .substring(1, request.resp_data.length - 1))
    }

    buf.append("]")

    val response = new Request("PreparedQueryCache", null, null, null)
    response.resp_data = buf.toString
    response
  }

}
