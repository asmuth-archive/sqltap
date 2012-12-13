package com.paulasmuth.dpump

import java.util.Locale
import java.util.Date
import java.text.DateFormat
import java.io.File
import scala.collection.mutable.HashMap;

object DPump{

  val VERSION = "v0.0.1"
  val CONFIG  = HashMap[Symbol,String]()

  var DEFAULTS = HashMap[Symbol, String](
    'db_threads   -> "16",
    'http_threads -> "4"
  )

  val manifest = HashMap[String,ResourceManifest]()

  var debug   = false
  var verbose = false

  val db_pool = new DBConnectionPool

  def main(args: Array[String]) : Unit = {
    var n = 0

    while (n < args.length) {

      if(args(n) == "--http")
        { CONFIG += (('http_port, args(n+1))); n += 2 }

      else if(args(n) == "--http-threads")
        { CONFIG += (('http_threads, args(n+1))); n += 2 }

      else if(args(n) == "--http-timeout")
        { CONFIG += (('http_timeout, args(n+1))); n += 2 }

      else if(args(n) == "--db")
        { CONFIG += (('db_addr, args(n+1))); n += 2 }

      else if(args(n) == "--db-threads")
        { CONFIG += (('db_threads, args(n+1))); n += 2 }

      else if(args(n) == "--db-timeout")
        { CONFIG += (('db_timeout, args(n+1))); n += 2 }

      else if((args(n) == "-c") || (args(n) == "--config"))
        { CONFIG += (('config_dir, args(n+1))); n += 2 }

      else if((args(n) == "-d") || (args(n) == "--debug"))
        { debug = true; n += 1 }

      else if((args(n) == "-v") || (args(n) == "--verbose"))
        { verbose = true; n += 1 }

      else {
        println("error: invalid option: " + args(n) + "\n")
        return usage(false)
      }

    }

    if (CONFIG contains 'config_dir unary_!)
      error("--config required", true)

    if (CONFIG contains 'db_addr unary_!)
      error("--db required", true)

    if (CONFIG contains 'http_port unary_!)
      error("--http required", true)

    DEFAULTS.foreach(d =>
      if (CONFIG contains d._1 unary_!) CONFIG += d )

    boot
  }

  def boot = try {
    val http_port = CONFIG('http_port).toInt
    val http_threads = CONFIG('http_threads).toInt
    val db_addr = CONFIG('db_addr)
    val db_threads = CONFIG('db_threads).toInt

    DPump.log("dpumpd " + VERSION + " booting...")

    load_config

    db_pool.connect(db_addr, db_threads)
    DPump.log("Connected to mysql...")

    val http = new HTTPServer(http_port, http_threads)
    DPump.log("Listening on http://0.0.0.0:" + http_port)
  } catch {
    case e: Exception => exception(e, true)
  }


  def load_config = {
    for (file <- new File(CONFIG('config_dir)).list()){
      log_debug("Loading: " + file)

      val raw = io.Source.fromFile(CONFIG('config_dir) + "/" + file).mkString
      val xml = scala.xml.XML.loadString(raw).head

      val resource = new ResourceManifest(xml)
      manifest += ((resource.name, resource))
    }
  }

  def usage(head: Boolean = true) = {
    if (head)
      println("dpumpd " + VERSION + " (c) 2012 Paul Asmuth\n")

    println("usage: dpumpd [options]                                                    ")
    println("  -c, -config       <dir>     path to xml config files                     ")
    println("  --http            <port>    start http server on this port               ")
    println("  --http-threads    <num>     number of http worker-threads (default: 4)   ")
    println("  --http-timeout    <msecs>   http request timeout (default: 5000ms)       ")
    println("  --db              <addr>    connect to mysql on this jdbc address        ")
    println("  --db-threads      <num>     number of db worker-threads (default: 16)    ")
    println("  --db-timeout      <msecs>   database query timeout (default: 5000ms)     ")
    println("  -d, --debug                 debug mode                                   ")
    println("  -v, --verbose               verbose mode                                 ")
  }


  def log(msg: String) = {
    val now = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.LONG, Locale.FRANCE)
    println("[" + now.format(new Date()) + "] " + msg)
  }

  def error(msg: String, fatal: Boolean) = {
    log("[ERROR] " + msg)

    if (fatal)
      System.exit(1)
  }


  def log_debug(msg: String) =
    if (debug)
      log("[DEBUG] " + msg)


  def exception(ex: Throwable, fatal: Boolean) = {
    error(ex.toString, false)

    for (line <- ex.getStackTrace)
      log_debug(line.toString)

    if (fatal)
      System.exit(1)
  }

}
