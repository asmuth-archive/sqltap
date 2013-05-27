// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.nio.{ByteBuffer}

class JSONWriter(buf: ByteBuffer) {

  // FIXPAUL: clean up
  def write_error(error: String) = {
    buf.put("{ \"status\": \"error\", \"error\": \"".getBytes)
    buf.put(JSONHelper.escape(error).getBytes)
    buf.put("\" }".getBytes)
  }

}
