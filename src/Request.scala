package com.paulasmuth.dpump

class Request(_req_str: String) {

  val req_str = _req_str

  var ready = false
  var error_str = ""

  val stack = new InstructionStack

  var resp_status : Int = 200
  var resp_data : String = null

  var qtime = List[Long]()

  def run : Unit = try {
    DPump.log_debug("-"*80)
    run_unsafe

    DPump.log_debug("QTime (parse, exec, write): " +
      qtime.sliding(2).map(x=>(x(1)-x(0)) / 1000000.0).mkString(", "))

    DPump.log_debug("-"*80)
  } catch {
    case e: ParseException => error(400, e.toString)
    case e: ExecutionException => error(500, e.toString)
    case e => {
      DPump.exception(e, false)
      error(500, "internal error: " + e.toString)
    }
  }

  private def run_unsafe : Unit = {
    qtime = qtime :+ System.nanoTime

    (new RequestParser(this)).run
    qtime = qtime :+ System.nanoTime

    (new RequestExecutor(stack)).run
    qtime = qtime :+ System.nanoTime

    ResponseWriter.serialize(this)
    qtime = qtime :+ System.nanoTime
  }

  private def error(code: Int, msg: String) : Unit = {
    resp_status = code
    error_str = msg

    ResponseWriter.serialize(this)

    ready = true
  }

}
