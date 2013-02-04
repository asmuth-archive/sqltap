package com.paulasmuth.sqltap.test

import com.paulasmuth.sqltap.JSONHelper
import org.scalatest._

class JSONHelperSpec extends FlatSpec {

  behavior of "JSONHelper.escape"


  it should "escape double quotes" in {  
    val input = "\""
    val output = JSONHelper.escape(input)
    assert(output === "\\\"")
  }

  it should "remove carriage returns" in {  
    val input = "\r"
    val output = JSONHelper.escape(input)
    assert(output === "")
  }

  it should "convert newlines" in {  
    val input = "\n"
    val output = JSONHelper.escape(input)
    assert(output === """\n""")
  }

  it should "convert vertical tabs" in {  
    val input = "\n\t"
    val output = JSONHelper.escape(input)
    assert(output === """\n""")
  }
}