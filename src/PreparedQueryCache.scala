package com.paulasmuth.sqltap

import java.util.logging._
import net.spy.memcached.MemcachedClient
import javax.servlet.ServletOutputStream

object PreparedQueryCache {

  System.setProperty("net.spy.log.LoggerImpl",
    "net.spy.memcached.compat.log.SunLogger");

  Logger.getLogger("net.spy.memcached")
    .setLevel(Level.SEVERE);

  val memcached_ttl = SQLTap.CONFIG('memcached_ttl).toInt
  val memcached_pool = new MemcachedPool

  def execute(query: PreparedQuery, ids: List[Int], output: ServletOutputStream) : Unit = {
    var memcached = memcached_pool.get

    val keys : List[String] = ids.map{ id => query.cache_key(id) }
    var cached : java.util.Map[String, String] = null

    if (memcached != null) {
      cached = memcached.getBulk(new MemcachedTranscoder, keys:_*)
    }

    (0 until ids.length).foreach { ind =>
      var cached_resp : String = cached.get(keys(ind))

      if (cached_resp == null) {
        val request = new Request(query.build(ids(ind)),
          new PlainRequestParser,
          new RequestExecutor,
          new PrettyJSONWriter)

        request.ready = true
        request.run

        cached_resp = request.resp_data
          .substring(1, request.resp_data.length - 1)

        memcached.set(keys(ind), memcached_ttl,
          cached_resp, new MemcachedTranscoder)
      }

      output.write(
        if (ind == 0) '[' else ',')

      output.write(
        cached_resp.getBytes("UTF-8"))
    }

    output.write(']')
  }

}
