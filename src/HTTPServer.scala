package com.paulasmuth.dpump

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.nio.SelectChannelConnector
import org.eclipse.jetty.util.thread.QueuedThreadPool

class HTTPServer(port: Int) {

  val pool = new QueuedThreadPool
  pool.setMaxThreads(50)

  val server = new Server
  server.setThreadPool(pool)
  server.setGracefulShutdown(1000)
  server.setHandler(new HTTPHandler)
  server.start()

  val connector = new SelectChannelConnector
  connector.setPort(port)
  server.addConnector(connector)
  connector.start()

}
