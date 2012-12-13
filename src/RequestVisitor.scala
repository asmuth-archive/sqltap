package com.paulasmuth.sqltap

trait RequestVisitor {

  var req : Request = null

  def run(_req: Request) : Unit =
    { req = _req; run() }

  def run() : Unit

}
