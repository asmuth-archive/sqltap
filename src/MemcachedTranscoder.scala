package com.paulasmuth.sqltap

import net.spy.memcached.transcoders.Transcoder
import net.spy.memcached.CachedData
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class MemcachedTranscoder extends Transcoder[String] {

  def getMaxSize : Int = 4096 * 8
  def asyncDecode(d: CachedData) : Boolean = true

  val buffer = new Array[Byte](getMaxSize)

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
