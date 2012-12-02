package com.paulasmuth.dpump

class Request {

  var resp_status : Int = 0
  var resp_data : String = null

  def execute = {
    resp_status = 200
    resp_data = "fnord"
  }

}
