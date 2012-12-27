package com.paulasmuth.sqltap

class Request(_req_str: String, parser: RequestVisitor, executor: RequestVisitor, writer: RequestVisitor) {

  val req_str = _req_str

  var ready = false
  var error_str = ""

  val stack = new InstructionStack

  var resp_status : Int = 200
  var resp_data : String = null

  var etime = List[Long]()

  def qtime : List[Double] =
    if (etime.size < 2) List[Double]() else
      etime.sliding(2).map(x=>(x(1)-x(0))/1000000.0).toList

  def run : Unit = try {
    SQLTap.log_debug("-"*80)
    run_unsafe

    SQLTap.log_debug("QTime (parse, exec, write): " +
      qtime.mkString(", "))

    SQLTap.log_debug("-"*80)
  } catch {
    case e: ParseException => error(400, e.toString)
    case e: ExecutionException => error(500, e.toString)
    case e: NotFoundException => error(404, e.toString)
    case e => {
      SQLTap.exception(e, false)
      error(500, "internal error: " + e.toString)
    }
  }

  private def run_unsafe : Unit = {
    etime = etime :+ System.nanoTime

    parser.run(this)
    etime = etime :+ System.nanoTime

    executor.run(this)
    etime = etime :+ System.nanoTime

    writer.run(this)
    etime = etime :+ System.nanoTime
  }

  private def error(code: Int, msg: String) : Unit = {
    resp_status = code
    error_str = msg

    resp_data = "{ \"status\": \"error\", \"error\": \"" +
      JSONHelper.escape(error_str) + "\" }"

    ready = true
  }

}
