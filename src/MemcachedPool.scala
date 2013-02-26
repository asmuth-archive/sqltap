package com.paulasmuth.sqltap

import java.util.concurrent._
import net.spy.memcached.MemcachedClient
import net.spy.memcached.AddrUtil

class MemcachedPool extends ThreadLocal[MemcachedClient] {

  override protected def initialValue : MemcachedClient =
    if (SQLTap.CONFIG contains 'memcached) {
      SQLTap.log("Connecting to memcached...")
      new MemcachedClient(AddrUtil.getAddresses(SQLTap.CONFIG('memcached)))
    } else null

}
