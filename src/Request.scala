package com.paulasmuth.dpump

class Request(_req_str: String) {

  val req_str = _req_str

  var ready = false
  var error_str = ""

  val stack = new InstructionStack

  var resp_status : Int = 200
  var resp_data : String = null

  def execute = {
    DPump.log_debug("-"*80)

    stack.head.name = "execute"
    stack.head.running = false
    stack.push_down

    (new RequestParser(this)).parse

    if (ready unary_!)
      (new RequestExecutor(this)).run

    ResponseWriter.serialize(this)

    DPump.log_debug("-"*80)
  }

  def error(msg: String) : Unit = {
    resp_status = 400
    error_str = msg
    ready = true
  }


}
