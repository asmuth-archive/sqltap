// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{HashMap,ListBuffer}

// TODO
//   > comparison doesnt take into account arguments
//   > query vs. ctree expansion
//   > cache query plans / ctreeindex.find decisions
//   > direct ctree access / fastpath (direct serial to json) with memcache mget "/ctree/name?id=1,2,3"

object CTreeCache {

  val stubcache = new HashMap[String,ElasticBuffer]() // STUB

  def store(ctree: CTree, ins: Instruction) : Unit = {
    val buf       = new ElasticBuffer(65535)
    val ctree_buf = new CTreeBuffer(buf)

    serialize(ctree_buf, ctree.stack.head, ins)

    val key = ctree.key(ins.record.id)

    buf.retrieve.flip // STUB
    stubcache(key) = buf // STUB
  }

  def retrieve(ctree: CTree, ins: FindSingleInstruction, worker: Worker) : Unit = {
    val key = ctree.key(ins.record.id)

    if (stubcache.contains(key)) {
      val buf = stubcache(key)
      val ctree_buf = new CTreeBuffer(buf)

      load(ctree_buf, ins, worker)
      buf.retrieve.position(0) // STUB
    }

    ins.ctree_ready()
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
            // FIXPAUL: this doesnt compare arguments!
            if (lins.resource_name == rins.resource_name && lins.name == rins.name) {
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
            // FIXPAUL: this doesnt compare arguments!
            if (lins.resource_name == rins.resource_name && lins.name == rins.name) {
              serialize(buf, lins, rins)
            }
          }
        }

        buf.write_end()
      }

    }
  }

  private def load(buf: CTreeBuffer, ins: Instruction, worker: Worker) : Unit = {
    while (true) {
      buf.read_next() match {

        case buf.T_RES => {
          val res_name = buf.read_string()

          // FIXPAUL: this doesnt terminate when found
          for (nxt <- ins.next) {
            if (nxt.resource_name == res_name) {
              load(buf, nxt, worker)
            }
          }
        }

        case buf.T_FLD => {
          val field = buf.read_string()
          val value = buf.read_string()
          ins.record.set(field, value)
          ins.fields -= field
        }

        case buf.T_END => {
          ins match {
            case i: PhiInstruction => return
            case _ => {
              if (ins.fields.length == 0)
                ins.cancel(worker)

              return
            }
          }
        }

        case buf.T_PHI => {
          val len = buf.read_next()
          val res_name = buf.read_string()

          // FIXPAUL: this doesnt terminate when found
          for (nxt <- ins.next) {
            if (nxt.resource_name == res_name) {
              var n = len
              val instructions = new ListBuffer[Instruction]()

              while (n > 0) {
                val nins = new PhiInstruction()
                nins.relation = nxt.relation
                nins.prev = nxt
                nins.record = new Record(nxt.relation.resource)
                InstructionFactory.deep_copy(nxt, nins)
                instructions += nins

                load(buf, nins, worker)

                n -= 1
              }

              nxt.next = instructions
              nxt.cancel(worker)
            }
          }
        }

      }
    }
  }

}
