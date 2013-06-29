// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

class PurgeExpirationHandler extends ExpirationHandler {

  def execute(job: ExpirationJob) : Unit = {
    job.get_worker.cache.purge(job.cache_keys)
  }

}
