// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.handler.AbstractHandler

class HTTPHandler extends AbstractHandler {

  def handle(target: String, base_req: org.eclipse.jetty.server.Request, req: HttpServletRequest, res: HttpServletResponse) = {
    res.addHeader("Server", "sqltapd " + SQLTap.VERSION)
    res.addHeader("X-SQLTap-Version", SQLTap.VERSION)

    try {
      req.getRequestURI match {
        case "/query" => action_query(req, res)
        case "/query.json" => action_query(req, res)
        case "/prepared_query" => action_prepared_query(req, res)
        case "/prepared_query.json" => action_prepared_query(req, res)
        case "/config" => action_config(req, res)
        case "/config.xml" => action_config(req, res)
        case "/ping" => res.getWriter().write("pong")
        case _ => throw new NotFoundException()
      }
    } catch {
      case e => handle_error(res, e)
    }

    base_req.setHandled(true)
  }

  private def handle_error(res: HttpServletResponse, exception: Throwable) = {
    exception match {
      case e: ParseException =>
        res.setStatus(400)

      case e: NotFoundException =>
        res.setStatus(404)

      case _ => {
        SQLTap.exception(exception, false)
        res.setStatus(500)
      }
    }

    val stream = res.getOutputStream()
    stream.write("{ \"status\": \"error\", \"error\": \"".getBytes("UTF-8"))
    stream.write(JSONHelper.escape(exception.toString).getBytes("UTF-8"))
    stream.write("\" }".getBytes("UTF-8"))
  }

  private def action_query(req: HttpServletRequest, res: HttpServletResponse) : Unit = {
    val request = new Request(req.getQueryString(),
      new PlainRequestParser, new RequestExecutor, new PrettyJSONWriter).run

    res.setStatus(200)
    res.addHeader("X-SQLTap-QTime", request.qtime.mkString(", "))
    res.addHeader("Content-Type", "application/json; charset=utf-8")

    if (request.resp_data != null)
      res.getOutputStream().write(request.resp_data.getBytes("UTF-8"))
  }

  private def action_prepared_query(req: HttpServletRequest, res: HttpServletResponse) : Unit = {
    var qry_name : String = null
    var qry_ids : String = null
    var qry: PreparedQuery = null

    try {
      qry_name = req.getParameter("name").toString
      qry_ids = req.getParameter("id").toString
      qry = SQLTap.prepared_queries(qry_name)
    } catch {
      case e: Exception => ()
    }

    if (qry_name == null || qry_ids == null)
      res.setStatus(400)

    else if (qry == null)
      res.setStatus(404)

    else {
      PreparedQueryCache.execute(qry,
        qry_ids.split(",").map{ x => x.toInt}.toList,
        res.getOutputStream,
        (req.getParameter("expire") != null))

      res.setStatus(200)
      res.addHeader("Content-Type", "application/json; charset=utf-8")
    }

  }

  private def action_config(req: HttpServletRequest, res: HttpServletResponse) : Unit = {
    var config = new StringBuffer

    config.append("<resources>\n")

    for ((name, resource) <- SQLTap.manifest)
      config.append(resource.elem.to_xml + "\n")

    config.append("</resources>")

    res.setStatus(200)
    res.addHeader("Content-Type", "application/xml; charset=utf-8")
    res.getOutputStream().write(config.toString.getBytes("UTF-8"))
 }

}
