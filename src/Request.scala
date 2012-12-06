package com.paulasmuth.dpump

class Request(_req_str: String) {

  val req_str = _req_str

  var ready = false
  var error_str = ""

  val stack = new InstructionStack

  var resp_status : Int = 200
  var resp_data : String = null

  var etime = List[Long]()

  def qtime : List[Double] =
    etime.sliding(2).map(x=>(x(1)-x(0))/1000000.0).toList

  def run : Unit = try {
    DPump.log_debug("-"*80)
    run_unsafe

    DPump.log_debug("QTime (parse, exec, write): " +
      qtime.mkString(", "))

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
    etime = etime :+ System.nanoTime

    (new RequestParser(this)).run
    etime = etime :+ System.nanoTime

    (new RequestExecutor(stack)).run
    etime = etime :+ System.nanoTime

    ResponseWriter.serialize(this)
    etime = etime :+ System.nanoTime
  }

  private def error(code: Int, msg: String) : Unit = {
    resp_status = code
    error_str = msg

    ResponseWriter.serialize(this)

    ready = true
  }

}
