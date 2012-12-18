package com.paulasmuth.sqltap

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.handler.AbstractHandler

class HTTPHandler extends AbstractHandler {

  def handle(target: String, base_req: org.eclipse.jetty.server.Request, req: HttpServletRequest, res: HttpServletResponse) = try {
    res.addHeader("Server", "sqltapd " + SQLTap.VERSION)
    res.addHeader("X-SQLTap-Version", SQLTap.VERSION)

    req.getRequestURI match {
      case "/query" => action_query(req, res)
      case "/query.json" => action_query(req, res)
      case "/config" => action_config(req, res)
      case "/config.xml" => action_config(req, res)
      case "/ping" => res.getWriter().write("pong")

      case _ =>
        { res.setStatus(404); res.getWriter().write("not found") }
   }

    base_req.setHandled(true)
  } catch {
    case e: Exception => SQLTap.exception(e, false)
  }

  private def action_query(req: HttpServletRequest, res: HttpServletResponse) : Unit = {
    val request = new Request(req.getQueryString(),
      new PlainRequestParser, new RequestExecutor, new PrettyJSONWriter)

    request.run

    res.setStatus(request.resp_status)
    res.addHeader("X-SQLTap-QTime", request.qtime.mkString(", "))

    if (request.resp_data != null)
      res.getWriter().write(request.resp_data)
  }

  private def action_config(req: HttpServletRequest, res: HttpServletResponse) : Unit = {
    var config = new StringBuffer

    config.append("<resources>\n")

    for ((name, resource) <- SQLTap.manifest)
      config.append(resource.elem.to_xml + "\n")

    config.append("</resources>")

    res.getWriter().write(config.toString)
 }

}
