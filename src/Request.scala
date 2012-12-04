package com.paulasmuth.dpump

class Request(_req_str: String) {

  val req_str = _req_str

  var ready = false
  var error_str = ""

  val stack = new InstructionStack

  var resp_status : Int = 200
  var resp_data : String = null

  def run = try {
    DPump.log_debug("-"*80)

    stack.head.name = "execute"
    stack.head.running = false
    stack.push_down

    (new RequestParser(this)).run

    (new RequestExecutor(stack)).run

    ResponseWriter.serialize(this)

    DPump.log_debug("-"*80)
  } catch {
    //case e: ParseException => error(400, e.toString)
    //case e: ExecutionException => error(500, e.toString)
    case e => {
      DPump.exception(e, false)
      error(500, "internal error:" + e.toString)
    }
  }

  private def error(code: Int, msg: String) : Unit = {
    resp_status = code
    error_str = msg

    ResponseWriter.serialize(this)

    ready = true
  }

}
