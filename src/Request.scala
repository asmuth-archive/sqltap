package com.paulasmuth.dpump

class Request(_req_str: String) {

  val req_str = _req_str

  var ready = false
  var error_str = ""

  val stack = new InstructionStack

  var resp_status : Int = 200
  var resp_data : String = null

  def execute = {
    stack.head.name = "execute"
    stack.push_down

    (new RequestParser(this)).parse

    if (ready unary_!)
      (new RequestExecutor(this)).run

    ResponseWriter.serialize(this)
  }

}
