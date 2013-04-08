// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import java.util.concurrent._
import scala.collection.mutable.ListBuffer

class DBConnectionPool {

  val queue   = new LinkedBlockingQueue[Job]()
  val workers = new ListBuffer[Worker]()

  var running = true

  class Job(qry: String) {

    val query = qry
    var result : DBResult = null
    val latch = new CountDownLatch(1)

    def finish(_result: DBResult) : Unit = {
      result = _result
      latch.countDown()
    }

    def retrieve : DBResult = {
      latch.await
      result
    }

  }

  class Worker(conn: DBConnection) extends Thread {

    override def run() =
      while (running)
        Option(queue.poll(1, TimeUnit.MILLISECONDS)) map perform

    def perform(job: Job) =
      job.finish(conn.execute(job.query))

  }

  def connect(addr: String, num_workers: Int) : Unit = {
    for (i <- 1 to num_workers) {
      val conn = new DBConnection(addr)
      val worker = new Worker(conn)
      worker.start()
      workers += worker
    }

    SQLTap.log("Connected to mysql...")
  }

  def execute(qry: String) : Job = {
    val job = new Job(qry)
    queue.put(job)

    SQLTap.log_debug("Execute: " + qry)

    job
  }

  def shutdown() : Unit = {
    running = false
  }
}
