package com.paulasmuth.dpump

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.handler.AbstractHandler
import RequestVisitorImplicits._

class HTTPHandler extends AbstractHandler {

  def handle(target: String, base_req: org.eclipse.jetty.server.Request, req: HttpServletRequest, res: HttpServletResponse) = try {
    res.addHeader("Server", "dpumpd " + DPump.VERSION)
    res.addHeader("X-DPump-Version", DPump.VERSION)

    req.getRequestURI match {
      case "/query" => action_query(req, res)
      case "/query.json" => action_query(req, res)
      case "/config" => action_config(req, res)
      case "/config.xml" => action_config(req, res)

      case _ =>
        { res.setStatus(404); res.getWriter().write("not found") }
   }

    base_req.setHandled(true)
  } catch {
    case e: Exception => DPump.exception(e, false)
  }

  private def action_query(req: HttpServletRequest, res: HttpServletResponse) : Unit = {
    val request = new Request[PlainRequestParser, RequestExecutor, PrettyJSONWriter](req.getQueryString())

    request.run

    res.setStatus(request.resp_status)
    res.addHeader("X-DPump-QTime", request.qtime.mkString(", "))

    if (request.resp_data != null)
      res.getWriter().write(request.resp_data)
  }

  private def action_config(req: HttpServletRequest, res: HttpServletResponse) : Unit = {
    var config = new StringBuffer

    config.append("<resources>\n")

    for ((name, resource) <- DPump.manifest)
      config.append(resource.elem.to_xml + "\n")

    config.append("</resources>")

    res.getWriter().write(config.toString)
 }

}
