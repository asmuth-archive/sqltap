package com.paulasmuth.dpump

object ResponseWriter {

  def serialize(req: Request) : Unit = {
    if (req.resp_status != 200)
      return serialize_error(req)

    req.resp_data = "fnord"
  }

  private def serialize_error(req: Request) : Unit =
    req.resp_data = "{ \"status\": \"error\", \"error\": \"" +
    req.error_str.replaceAll("\"", "'") + "\" }"

}
