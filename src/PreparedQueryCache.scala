package com.paulasmuth.sqltap

import java.util.logging._
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.transcoders.Transcoder;
import net.spy.memcached.CachedData;
import java.util.concurrent._;

object PreparedQueryCache {

  class PlainMemcachedTranscoder extends Transcoder[String] {
    def getMaxSize : Int = Integer.MAX_VALUE
    def asyncDecode(d: CachedData) : Boolean = true

    def encode(s: String) : CachedData =
      new CachedData(0, s.getBytes("UTF-8"), getMaxSize)

    def decode(d: CachedData) : String = {
      new String(d.getData, "UTF-8")
    }

  }

  System.setProperty("net.spy.log.LoggerImpl",
    "net.spy.memcached.compat.log.SunLogger");

  Logger.getLogger("net.spy.memcached")
    .setLevel(Level.SEVERE);

  var memcached : MemcachedClient = null
  val memcached_ttl = SQLTap.CONFIG('memcached_ttl).toInt

  if(SQLTap.CONFIG contains 'memcached) {
    memcached = new MemcachedClient(
      AddrUtil.getAddresses(SQLTap.CONFIG('memcached)))

    SQLTap.log("Connected to memcached...")
  }

  def execute(query: PreparedQuery, ids: List[Int]) : Request = {
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
