package com.paulasmuth.dpump

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
        perform(queue.take)

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
  }

  def execute(qry: String) : DBResult = {
    val job = new Job(qry)
    queue.put(job)
    job.retrieve
  }

  def shutdown() : Unit = {
  }
}
