package com.paulasmuth.sqltap

import java.util.Locale
import java.util.Date
import java.text.DateFormat
import java.io.File
import scala.collection.mutable.HashMap;

object SQLTap{

  val VERSION = "v0.0.8"
  val CONFIG  = HashMap[Symbol,String]()

  var DEFAULTS = HashMap[Symbol, String](
    'db_threads   -> "16",
    'http_threads -> "4",
    'ffp_threads -> "4"
  )

  val manifest = HashMap[String,ResourceManifest]()
  val prepared_queries = HashMap[String,PreparedQuery]()
  val prepared_queries_ffp = HashMap[Int,PreparedQuery]()

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

      else if(args(n) == "--ffp")
        { CONFIG += (('ffp_port, args(n+1))); n += 2 }

      else if(args(n) == "--ffp-threads")
        { CONFIG += (('ffp_threads, args(n+1))); n += 2 }

      else if((args(n) == "-c") || (args(n) == "--config"))
        { CONFIG += (('config_base, args(n+1))); n += 2 }

      else if((args(n) == "-d") || (args(n) == "--debug"))
        { debug = true; n += 1 }

      else if((args(n) == "-v") || (args(n) == "--verbose"))
        { verbose = true; n += 1 }

      else if((args(n) == "-h") || (args(n) == "--help"))
        return usage(true)

      else {
        println("error: invalid option: " + args(n) + "\n")
        return usage(false)
      }

    }

    var start = true

    if (CONFIG contains 'config_base unary_!)
      { log("--config required"); start = false }

    if (CONFIG contains 'db_addr unary_!)
      { log("--db required"); start = false }

    if (start unary_!)
      { println; return usage(true) }

    DEFAULTS.foreach(d =>
      if (CONFIG contains d._1 unary_!) CONFIG += d )

    boot
  }

  def boot = try {
    SQLTap.log("sqltapd " + VERSION + " booting...")
    load_config

    val db_threads = CONFIG('db_threads).toInt
    db_pool.connect(CONFIG('db_addr), db_threads)


    val http_threads = CONFIG('http_threads).toInt
    val http_port = CONFIG.getOrElse('http_port, "0")
      .asInstanceOf[String].toInt

    val http = if (http_port > 0)
      new HTTPServer(http_port, http_threads)


    val ffp_threads = CONFIG('ffp_threads).toInt
    val ffp_port = CONFIG.getOrElse('ffp_port, "0")
      .asInstanceOf[String].toInt

    val ffp = if (ffp_port > 0)
      new FFPServer(ffp_port, ffp_threads).start

  } catch {
    case e: Exception => exception(e, true)
  }


  def load_config = {
    val cfg_base = new File(CONFIG('config_base))

    val sources : Array[String] =
      if (cfg_base.isDirectory unary_!)
        Array(io.Source.fromFile(CONFIG('config_base)).mkString)
      else
        cfg_base.list.map(f =>
          io.Source.fromFile(CONFIG('config_base) + "/" + f).mkString)

    for (source <- sources){
      val xml = scala.xml.XML.loadString(source)

      var resources = List[scala.xml.Node]()
      var prepared  = List[scala.xml.Node]()

      if (xml.head.label == "resource")
        resources = List(xml.head)

      else if (xml.head.label == "prepared_query")
        prepared = List(xml.head)

      else {
        resources = (xml \ "resource").toList
        prepared = (xml \ "prepared_query").toList
      }

      for (elem <- resources) {
        val resource = new ResourceManifest(elem)
        log_debug("Loaded resource: " + resource.name)
        manifest += ((resource.name, resource))
      }

      for (elem <- prepared) {
        val pquery = new PreparedQuery(elem)
        log_debug("Loaded prepared_query: " + pquery.name)
        prepared_queries += ((pquery.name, pquery))

        if (pquery.ffp_id != null)
          prepared_queries_ffp += ((pquery.ffp_id.toInt, pquery))
      }

    }
  }

  def usage(head: Boolean = true) = {
    if (head)
      println("sqltapd " + VERSION + " (c) 2012 Paul Asmuth\n")

    println("usage: sqltapd [options]                                                    ")
    println("  -c, -config       <dir>     path to xml config files                     ")
    println("  --http            <port>    start http server on this port               ")
    println("  --http-threads    <num>     number of http worker-threads (default: 4)   ")
    println("  --http-timeout    <msecs>   http request timeout (default: 5000ms)       ")
    println("  --db              <addr>    connect to mysql on this jdbc address        ")
    println("  --db-threads      <num>     number of db worker-threads (default: 16)    ")
    println("  --db-timeout      <msecs>   database query timeout (default: 5000ms)     ")
    println("  --ffp             <port>    start fast fetch protocol server on this port")
    println("  --ffp-threads     <num>     number of ffp worker-threads (default: 4)    ")
    println("  -h, --help                  you're reading it...                         ")
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
