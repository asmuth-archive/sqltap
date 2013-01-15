package com.paulasmuth.sqltap

import java.util.logging._
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.AddrUtil;

object PreparedQueryCache {

  System.setProperty("net.spy.log.LoggerImpl",
    "net.spy.memcached.compat.log.SunLogger");

  Logger.getLogger("net.spy.memcached")
    .setLevel(Level.SEVERE);

  var memcached : MemcachedClient = null

  if(SQLTap.CONFIG contains 'memcached) {
    memcached = new MemcachedClient(
      AddrUtil.getAddresses(SQLTap.CONFIG('memcached)))

    SQLTap.log("Connected to memcached...")
  }

  def execute(query: PreparedQuery, id: Int) : Request = {
    val request = new Request(query.build(id),
      new PlainRequestParser, new RequestExecutor, new PrettyJSONWriter)

    if (memcached != null) {
      request.resp_data = "from memcached..."
      //val future = memcached.asyncGet("fnord")
      request.ready = true
    }

    if (request.ready unary_!)
      request.run

    request
  }

}
