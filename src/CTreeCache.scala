// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{ListBuffer}

object CTreeCache extends ReadyCallback[CacheRequest] {

  def store(ctree: CTree, key: String, ins: CTreeInstruction, worker: Worker) : Unit = {
    val buf       = new ElasticBuffer(65535)
    val ctree_buf = new CTreeBuffer(buf)

    serialize(ctree_buf, ctree.stack.head, ins)

    val request = new CacheStoreRequest(key, buf)
    request.worker = worker
    request.attach(this)

    worker.cache.enqueue(request)
    worker.cache.flush()
  }

  def retrieve(ctree: CTree, key: String, ins: CTreeInstruction, worker: Worker) : Unit = {
    val request = new CacheGetRequest(key)
    request.instruction = ins
    request.worker = worker
    request.attach(this)

    worker.cache.enqueue(request)
  }

  def flush(worker: Worker) : Unit = {
    worker.cache.flush()
  }

  def ready(req: CacheRequest) : Unit = {
    req match {
      case get: CacheGetRequest => {
        get.retrieve match {
          case Some(buf: ElasticBuffer) => {
            val ctree_buf = new CTreeBuffer(buf)
            load(ctree_buf, get.instruction, req.worker)
            get.instruction.ctree_ready(req.worker)
          }
          case None => {
            get.instruction.ctree_ready(req.worker)
          }
        }
      }
      case set: CacheStoreRequest => ()
    }
  }

  def error(req: CacheRequest, err: Throwable) : Unit = {
    SQLTap.exception(err, false)
  }

  private def serialize(buf: CTreeBuffer, cins: Instruction, qins: Instruction) : Unit = {
    qins match {

      case qins_m: CountInstruction => {
        buf.write_header(qins.resource_name)
        buf.write_field("__count", qins.record.get("__count"))
        buf.write_end()
      }

      case qins_m: FindSingleInstruction => {
        buf.write_header(qins.resource_name)

        for (field <- cins.fields) {
          if (qins.record.has_field(field))
            buf.write_field(field, qins.record.get(field))
        }

        for (lins <- cins.next) {
          // FIXPAUL: this doesnt terminate when found
          for (rins <- qins.next) {
            if (lins.compare(rins)) {
              serialize(buf, lins, rins)
            }
          }
        }

        buf.write_end()
      }

      case qins_m: FindMultiInstruction => {
        buf.write_phi(cins.resource_name, qins.next.length)

        for (nxt <- qins.next)
          serialize(buf, cins, nxt)
      }

      case qins_m: PhiInstruction => {
        buf.write_header(cins.resource_name)

        for (field <- cins.fields) {
          if (qins.record.has_field(field))
            buf.write_field(field, qins.record.get(field))
        }

        for (lins <- cins.next) {
          // FIXPAUL: this doesnt terminate when found
          for (rins <- qins.next) {
            if (lins.compare(rins)) {
              serialize(buf, lins, rins)
            }
          }
        }

        buf.write_end()
      }

    }
  }

  private def load(buf: CTreeBuffer, ins: Instruction, worker: Worker) : Unit = {
    while (buf.remaining > 0) {
      buf.read_next() match {

        case buf.T_RES => {
          val res_name = buf.read_string()
          var found    = false

          if (ins != null) {
            var n = ins.next.length

            if (ins.resource_name == res_name) {
              found = true
            }

            if (ins.relation.resource.name == res_name) {
              found = true
            }

            if (ins != null) {
              while (!found && n > 0) {
                if (ins.next(n - 1).resource_name == res_name) {
                  found = true
                  load(buf, ins.next(n - 1), worker)
                }

                n -= 1
              }
            }
          }

          if (!found) {
            load(buf, null, worker)
          }
        }

        case buf.T_FLD => {
          val field = buf.read_string()
          var value = buf.read_string()

          if (ins != null) {
            ins.record.set(field, value)
            ins.fields -= field
          }
        }

        case buf.T_END => {
          if (ins == null)
            return

          ins match {

            case ins: PhiInstruction => return

            case ins: FindMultiInstruction => {
              if (ins.fields.length == 0) {
                ins.cancel(worker)
              }

              return
            }

            case ins: FindSingleInstruction => {
              ins.ctree_try = false

              if (ins.fields.length == 0) {
                ins.cancel(worker)
              }

              return
            }

            case ins: CountInstruction => {
              ins.cancel(worker)
              return
            }
          }
        }

        case buf.T_PHI => {
          val len = buf.read_next()
          val res_name = buf.read_string()

          if (ins != null) {
            var nxt : Instruction = null

            if (ins.relation.resource.name.equals(res_name)) {
              nxt = ins
            } else {
              for (_nxt <- ins.next) {
                // FIXPAUL: this doesnt terminate when found
                if (_nxt.resource_name == res_name) {
                  nxt = _nxt
                }
              }
            }

            if (nxt != null) {
              var n = len
              val instructions = new ListBuffer[Instruction]()
              while (n > 0) {
                val nins = new PhiInstruction()
                nins.relation = nxt.relation
                nins.resource_name = nxt.resource_name
                nins.prev = nxt
                nins.record = new Record(nxt.relation.resource)
                InstructionFactory.deep_copy(nxt, nins)
                instructions += nins

                load(buf, nins, worker)

                n -= 1
              }

              nxt.next = instructions

              if (len > 0)
                nxt match {
                  case multi_ins: FindMultiInstruction => {
                    multi_ins.ctree_try = false
                    multi_ins.expanded = true

                    for (cfield <- nxt.next.head.record.fields)
                      nxt.fields -= cfield
                  }
                  case _ => ()
                }

              if (nxt.fields.length == 0)
                nxt.cancel(worker)
            }
          }
        }

      }
    }
  }

  def expand_query(ctree: CTree, ins: CTreeInstruction) : Unit = {
    expand_query(ctree.stack.head, ins)
  }

  private def expand_query(left: Instruction, right: Instruction) : Unit = {
    var score = 0
    var cost  = 0

    for (lfield <- left.fields)
      if (!right.record.has_field(lfield))
        right.fields += lfield

    for (rins <- right.next) {
      var found = false
      var n     = left.next.length

      while (n > 0 && !found) {
        n -= 1

        val lins = left.next(n)

        if (lins.compare(rins)) {
          found = true

          expand_query(lins, rins)
        }
      }
    }
  }

}
