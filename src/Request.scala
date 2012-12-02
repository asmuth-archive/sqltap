package com.paulasmuth.dpump

class Request(_req_str: String) {

  val req_str = _req_str

  var ready = false
  var error_str = ""

  var resp_status : Int = 200
  var resp_data : String = null

  def execute = {
    RequestParser.parse(this)

    //if (ready unary_!)
    //  RequestExecutor.execute(this)

    ResponseWriter.serialize(this)
  }

}
