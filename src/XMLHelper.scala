package com.paulasmuth.dpump

class XMLHelper(elem: xml.Node) {

  def attr(name: String, required: Boolean = false, default: String = null) = {

    val value = elem.attribute(name).getOrElse(null)

    if (value != null)
      value.toString

    else if (required) throw new ParseException(
     "missing attribute: " + name + " => " + elem.toString)

    else
      (if (default != null) default else null)

  }

}

