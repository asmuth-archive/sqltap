package com.paulasmuth.sqltap

object PreparedQueryCache {

  def execute(query: PreparedQuery, id: Int) : Request = {
    /*val request = new Request("fnord", null, null, null);*/

    val request = new Request(query.build(id),
      new PlainRequestParser, new RequestExecutor, new PrettyJSONWriter).run

    request
  }
}
