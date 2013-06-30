// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.io.File

// TODO
//   > gzip
//   > optimization: don't store keys in ctree cache but key-indexes
//   > limit max number of entries in the conn queue
//   > limit max number of concurrent requests per worker
//   > bug: pin.findAllWhere(%22submitter_id%20=%206052621%22,%2010){pinable_id,product.findOne{*}}
//   > bug: user.findOne(13008){id,images.findOne{*}} vs user.findOne(13008){id,images.findAll(1){*}}
//   > bug: user.findOne(1) hangs
//   > bug: user.findAll(1){id,shop.findOne{id}} kills the worker
//   > bug: SELECT users.`facebook_url` FROM users WHERE `id` = 1 ORDER BY id DESC; crashes
//   > ctree stats
//   > better error messages for invalid query strings
//   > refresh expiration handler

object SQLTap{

  val VERSION = "v0.5.3"

  def main(args: Array[String]) : Unit = {
    var n = 0

    while (n < args.length) {

      if (args(n) == "--http")
        { Config.set('http_port, args(n+1)); n += 2 }

      else if (args(n) == "--mysql-host")
        { Config.set('mysql_host, args(n+1)); n += 2 }

      else if (args(n) == "--mysql-port")
        { Config.set('mysql_port, args(n+1)); n += 2 }

      else if (args(n) == "--mysql-user")
        { Config.set('mysql_user, args(n+1)); n += 2 }

      else if (args(n) == "--mysql-password")
        { Config.set('mysql_pass, args(n+1)); n += 2 }

      else if (args(n) == "--mysql-database")
        { Config.set('mysql_db, args(n+1)); n += 2 }

      else if (args(n) == "--mysql-queuelen")
        { Config.set('sql_queue_max_len, args(n+1)); n += 2 }

      else if (args(n) == "--mysql-numconns")
        { Config.set('sql_max_connections, args(n+1)); n += 2 }

      else if (args(n) == "--expiration-handler")
        { Config.set('expiration_handler, args(n+1)); n += 2 }

      else if (args(n) == "--cache-backend")
        { Config.set('cache_backend, args(n+1)); n += 2 }

      else if (args(n) == "--memcache-hosts")
        { Config.set('memcache_hosts, args(n+1)); n += 2 }

      else if (args(n) == "--memcache-mode")
        { Config.set('memcache_mode, args(n+1)); n += 2 }

      else if (args(n) == "--memcache-queuelen")
        { Config.set('memcache_queue_max_len, args(n+1)); n += 2 }

      else if (args(n) == "--memcache-numconns")
        { Config.set('memcache_max_connections, args(n+1)); n += 2 }

      else if ((args(n) == "-t") || (args(n) == "--threads"))
        { Config.set('threads, args(n+1)); n += 2 }

      else if ((args(n) == "-c") || (args(n) == "--config"))
        { Config.set('config_base, args(n+1)); n += 2 }

      else if ((args(n) == "-d") || (args(n) == "--debug"))
        { Config.debug = true; n += 1 }

      else if ((args(n) == "-h") || (args(n) == "--help"))
        return usage(true)

      else {
        println("error: invalid option: " + args(n) + "\n")
        return usage(false)
      }

    }

    if (!Config.has_key('config_base)) {
      Logger.log("--config required")
      println()

      return usage(true)
    }

    boot()
  }

  def boot() : Unit = try {
    Logger.log("sqltapd " + VERSION + " booting...")

    Statistics.update_async()

    Manifest.load(
      new File(Config.get('config_base)))

    RelationTrace.load(Manifest.resources)

    ExpirationHandlerFactory.configure(
      Config.get('expiration_handler))

    val server = new Server(Config.get('threads).toInt)
    server.run(Config.get('http_port).toInt)
  } catch {
    case e: Exception => Logger.exception(e, true)
  }

  def usage(head: Boolean = true) = {
    if (head)
      println("sqltapd " + VERSION + " (c) 2012 Paul Asmuth\n")

    println("usage: sqltapd [options]                                                        ")
    println("  -c, --config           <dir>     path to xml config files                     ")
    println("  -t, --threads          <num>     number of worker threads (default: 4)        ")
    println("  --http                 <port>    start http server on this port               ")
    println("  --mysql-host           <addr>    mysql server hostname                        ")
    println("  --mysql-port           <port>    mysql server port                            ")
    println("  --mysql-user           <user>    mysql server username                        ")
    println("  --mysql-password       <pass>    mysql server password                        ")
    println("  --mysql-database       <db>      mysql server database (USE ...;)             ")
    println("  --mysql-queuelen       <num>     max mysql queue size per worker              ")
    println("  --mysql-numconns       <num>     max number of mysql connections per worker   ")
    println("  --expiration-handler   <name>    expiration handler (noop, purge, refresh)    ")
    println("  --cache-backend        <name>    cache backend (memcache)                     ")
    println("  --memcache-hosts       <addrs>   comma-seperated memcache servers (host:port) ")
    println("  --memcache-queuelen    <num>     max mysql queue size per worker              ")
    println("  --memcache-numconns    <num>     max number of mysql connections per worker   ")
    println("  --memcache-mode        <name>    replication mode (copy, shard)               ")
    println("  -h, --help                       you're reading it...                         ")
    println("  -d, --debug                      debug mode                                   ")
  }

}
