// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

object ExpirationHandler {

  def expire(resource_name: String, record_id: Int) : Unit = {
    if (!SQLTap.manifest.contains(resource_name))
      throw new ParseException("unknown resource: " + resource_name)

    val resource = SQLTap.manifest(resource_name)

    SQLTap.log_debug(
      "[EXPIRE] resource '" + resource.name + "' with id #" +
      record_id.toString + " expired")

    println("here be dragons...")
  }

}
