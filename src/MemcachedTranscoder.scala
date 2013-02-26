// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import net.spy.memcached.transcoders.Transcoder
import net.spy.memcached.CachedData
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class MemcachedTranscoder extends Transcoder[String] {

  def getMaxSize : Int = 4096 * 32
  def asyncDecode(d: CachedData) : Boolean = true

  def encode(s: String) : CachedData = {
    val target = new ByteArrayOutputStream()
    val gzip   = new GZIPOutputStream(target)
    val data   = s.getBytes("UTF-8")

    if (data.size > getMaxSize)
      throw new Exception("CachedData too large")

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
      throw e
      return "error"
    }
  }

}
