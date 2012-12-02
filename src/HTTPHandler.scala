package com.paulasmuth.dpump

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.handler.AbstractHandler

class HTTPHandler extends AbstractHandler {

  def handle(target: String, base_req: org.eclipse.jetty.server.Request, req: HttpServletRequest, res: HttpServletResponse) {
    val request = new Request
    request.execute
    res.setStatus(request.resp_status)
    res.getWriter().write(request.resp_data)
    base_req.setHandled(true)
  }

}
