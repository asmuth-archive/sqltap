package com.paulasmuth.dpump

object RequestParser {

  def parse(req: Request) : Unit = {

    if(req.req_str == null)
      return error(req, "no query string")

  }


  private def error(req: Request, msg: String) : Unit = {
    req.resp_status = 400
    req.error_str = msg
    req.ready = true
  }

}
