// This file is part of the "SQLTap" project
//   (c) 2011-2013 Paul Asmuth <paul@paulasmuth.com>
//
// Licensed under the MIT License (the "License"); you may not use this
// file except in compliance with the License. You may obtain a copy of
// the License at: http://opensource.org/licenses/MIT

package com.paulasmuth.sqltap

import scala.collection.mutable.{HashMap,ListBuffer}

class RelationTrace {

  private val trace = new HashMap[String, ListBuffer[String]]

  def lookup(resource_name: String) : List[String] = {
    trace(resource_name).toList
  }

  def load(manifest: List[ResourceManifest]) : Unit = {
    for (resource <- manifest) {
      append(resource.name, resource.id_field)

      for (relation <- resource.relations) {
        if (relation.join_foreign) {
          append(relation.resource.name, relation.join_field)
        }

        else if (relation.join_field_remote != null) {
          append(relation.resource.name, relation.join_field_remote)
        }
      }
    }
  }

  private def append(resource_name: String, field: String) : Unit = {
    if (!trace.contains(resource_name))
      trace(resource_name) = new ListBuffer[String]()

    if (!trace(resource_name).contains(field))
      trace(resource_name) += field
  }

}
