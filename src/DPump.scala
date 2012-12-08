package com.paulasmuth.dpump

import java.util.Locale
import java.util.Date
import java.text.DateFormat
import java.io.File
import scala.collection.mutable.HashMap;

object DPump{

  val VERSION = "v0.0.1"
  val CONFIG  = HashMap[Symbol,String]()

  val manifest = HashMap[String,ResourceManifest]()

  var db_threads   = 30
  var http_threads = 10

  var debug   = false
  var verbose = false

  val db_pool = new DBConnectionPool

  def main(args: Array[String]) : Unit = {
    var n = 0

    while (n < args.length) {

      if((args(n) == "-l") || (args(n) == "--listen"))
        { CONFIG += (('listen, args(n+1))); n += 2 }

      if((args(n) == "-c") || (args(n) == "--config"))
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

    boot
  }

  def boot = try {
    val http_port = 8080  // FIXPAUL
    val db_addr = "mysql://localhost:3306/dawanda?user=root" // FIXPAUL

    DPump.log("dpumpd " + VERSION + " booting...")

    load_config

    db_pool.connect(db_addr, db_threads)
    DPump.log("Connected to mysql...")

    val http = new HTTPServer(http_port, http_threads)
    DPump.log("Listening on http://0.0.0.0:8080")
  } catch {
    case e: Exception => exception(e, true)
  }


  def load_config = {
    for (file <- new File(CONFIG('config_dir)).list()){
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
    println("  -l, --listen      <port>    listen for clients on this tcp port          ")
    println("  -t, --timeout     <msecs>   connection idle timeout (default: 5000ms)    ")
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
