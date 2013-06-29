// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

object ExpirationHandlerFactory {

  val handler : ExpirationHandler = null

  def get() : ExpirationHandler = {
    if (handler == null)
      throw new ExecutionException(
        "expiration handler not configured")

    handler
  }

  def configure(name: String) : Unit = {
    name match {

      case _ =>
        throw new ParseException("unknown expiration handler: " + name)

    }
  }

}
