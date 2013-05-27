// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap
/*
import java.util.concurrent._
import net.spy.memcached.MemcachedClient
import net.spy.memcached.AddrUtil
import java.util.logging._

class MemcachedPool extends ThreadLocal[MemcachedClient] {

  override protected def initialValue : MemcachedClient = {
    System.setProperty("net.spy.log.LoggerImpl",
      "net.spy.memcached.compat.log.SunLogger");

    Logger.getLogger("net.spy.memcached")
      .setLevel(Level.SEVERE);

    if (SQLTap.CONFIG contains 'memcached unary_!)
      null

    else {
      SQLTap.log("Connecting to memcached...")
      new MemcachedClient(AddrUtil.getAddresses(SQLTap.CONFIG('memcached)))
    }
  }

}
*/
