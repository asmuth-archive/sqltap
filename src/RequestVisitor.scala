package com.paulasmuth.dpump

trait RequestVisitor {

  var req : Request[RequestVisitor, RequestVisitor, RequestVisitor] = null

  def run(_req: Request[RequestVisitor, RequestVisitor, RequestVisitor]) : Unit =
    { req = _req; run() }

  def run() : Unit

}

object RequestVisitorImplicits {

  implicit val plain_request_parser: PlainRequestParser =
    new PlainRequestParser

  //implicit val json_request_parser: JSONRequestParser =
  //  new JSONRequestParser

  implicit val request_executor: RequestExecutor =
    new RequestExecutor

  //implicit val plain_json_writer : PlainJSONWriter =
  //  new PlainJSONWriter

  implicit val pretty_json_writer : PrettyJSONWriter =
    new PrettyJSONWriter

}
