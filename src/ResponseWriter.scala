package com.paulasmuth.dpump

object ResponseWriter {

  def serialize(req: Request) =
    if (req.resp_status == 200)
      req.resp_data = "fnord"
    else
      serialize_error(req)


  private def serialize_error(req: Request) =
    req.resp_data = "{ \"status\": \"error\", \"error\": \"" +
    req.error_str.replaceAll("\"", "'") + "\" }"

}
