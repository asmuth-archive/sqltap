package com.paulasmuth.dpump

object ResponseWriter {

  def serialize(req: Request) : Unit =
    if (req.resp_status != 200)
      serialize_error(req)
    else
      (new JSONWriter(req)).run

  private def serialize_error(req: Request) : Unit =
    req.resp_data = "{ \"status\": \"error\", \"error\": \"" +
    req.error_str.replaceAll("\"", "'") + "\" }"

}
