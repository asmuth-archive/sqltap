package com.paulasmuth.dpump

class Request(_req_str: String) {

  val req_str = _req_str

  var ready = false
  var error_str = ""

  var resp_status : Int = 0
  var resp_data : String = null

  def execute = {
    resp_status = 200
    resp_data = "fnord"

    RequestParser.parse(this)

    //if (ready unary_!)
    //  RequestExecutor.execute(this)

    ResponseWriter.serialize(this)
  }

}
