package com.paulasmuth.dpump

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.handler.AbstractHandler

class HTTPHandler extends AbstractHandler {

  def handle(target: String, base_req: org.eclipse.jetty.server.Request, req: HttpServletRequest, res: HttpServletResponse) = try {
    val request = new Request(req.getQueryString())
    request.run

    res.setStatus(request.resp_status)
    res.addHeader("X-DPump-QTime", request.qtime.mkString(", "))
    res.addHeader("X-DPump-Version", DPump.VERSION)
    res.addHeader("Server", "dpumpd " + DPump.VERSION)

    if (request.resp_data != null)
      res.getWriter().write(request.resp_data)

    base_req.setHandled(true)
  } catch {
    case e: Exception => DPump.exception(e, false)
  }

}
