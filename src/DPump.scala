package com.paulasmuth.dpump

import java.util.Locale
import java.util.Date
import java.text.DateFormat
import scala.collection.mutable.HashMap;

object DPump{

  val VERSION = "v0.0.1"
  val CONFIG  = HashMap[Symbol,String]()

  var debug   = false
  var verbose = false

  def main(args: Array[String]) : Unit = {
    var n = 0

    while (n < args.length) {

      if((args(n) == "-l") || (args(n) == "--listen"))
        { CONFIG += (('listen, args(n+1))); n += 2 }

      else if((args(n) == "-d") || (args(n) == "--debug"))
        { debug = true; n += 1 }

      else if((args(n) == "-v") || (args(n) == "--verbose"))
        { verbose = true; n += 1 }

      else {
        println("error: invalid option: " + args(n) + "\n")
        return usage(false)
      }

    }

    val conn = new DBConnection("jdbc:mysql://localhost:3306/dawanda?user=root");

    /*for (i <- (1 to 10000)){
      val id = (8910842 - ((Math.random * 100000).toInt * 4)).toString
      val rslt = conn.execute("select * from users where id = " + id + ";")
    //println(rslt.head)
    //println(rslt.data)
      println(rslt.qtime / 1000 / 1000.0)
    }*/

    val http = new HTTPServer(8080)
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


  def error(msg: String) =
    log("[ERROR] " + msg)


  def debug(msg: String) =
    log("[DEBUG] " + msg)


  def exception(ex: Exception, fatal: Boolean) = {
    error(ex.toString)

    if (debug)
      for (line <- ex.getStackTrace)
        debug(line.toString)

    if (fatal)
      System.exit(1)
  }

}
