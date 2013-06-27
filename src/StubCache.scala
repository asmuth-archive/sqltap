// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{HashMap}

// STUB!
class StubCache extends CacheBackend {

  val stubcache = new HashMap[String,ElasticBuffer]()

  def execute(requests: List[CacheRequest]) = {
    for (req <- requests) {
      req match {
        case get: CacheGetRequest => {
          stubcache.get(req.key) match {
            case Some(buf:  ElasticBuffer) => {
              get.buffer = buf.clone()
            }
            case None => ()
          }
        }
        case set: CacheStoreRequest => {
          stubcache.put(req.key, set.buffer)
        }
      }

      req.ready()
    }
  }

}
