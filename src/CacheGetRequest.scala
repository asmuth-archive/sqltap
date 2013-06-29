// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

class CacheGetRequest(_key: String) extends CacheRequest {
  val key : String = _key
  var instruction : CTreeInstruction = null
  var buffer : ElasticBuffer = null

  def retrieve() : Option[ElasticBuffer] = {
    if (buffer == null) None else Some(buffer)
  }

  def ready() : Unit = {
    if (buffer != null) {
      val ctree_buf = new CTreeBuffer(buffer)

      CTreeCache.load(ctree_buf, instruction, worker)
      instruction.ctree_ready(worker)
    }

    instruction.ctree_ready(worker)
  }

}
