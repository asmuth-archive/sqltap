SQLTap
======

SQLTap is a HTTP+JSON <=> MySQL gateway. It fetches nested records from the
database without using SQL JOIN and parallelizes queries where possible.


Usage
-----

### Starting SQLTap

    ./sqltap --db "mysql://localhost:3306/?user=root" --http 8080 -c config/


### Schema and Relations

    mkdir -p config
    cp project/example_resource.xml config/my_resource.xml


### Sending queries

retrieve user record id#2342 with all fields:

    /query?user.findOne(2342){*}

retrieve user record id#2342 with fields: username and email):

    /query?user.findOne(2342){username,email}

retrieve user record with id#2342 with all orders and all fields::

    /query?user.findOne(2342){*,orders.findAll{*}}


Instructions
------------

### findOne

    resource.findOne(id)
    relation.findOne


### findSome / findAll

    relation.findAll
    relation.findAll(limit)


### count

    relation.countAll



Configuration
-------------

here be dragons

Installation
------------

You need java and sbt to build SQLTap:

    ./build jar


FFP
---

yadda yadda, ffp integration

### FQDNs

JSON-like map nesting and lists/arrays are implemented by constructing special FQDNs for every entry.

To convert the nested structures into a flat map, we represent each entry by a FQDN.... here be dragons
(nested keys joined by forward slash, arrays are turned into objects where each key is the index from
zero)

simple example:

    {
      "fnord": "xxx",
      "fubar": {
        "one": "aaa",
        "two": [
          "111",
          "222"
        ]
      }
    }

    ... becomes ...

    {
      "fnord":        "xxx",
      "fubar/one":    "aaa",
      "fubar/two/0":  "111",
      "fubar/two/1":  "222"
    }


complex example:

    {
      "one": {
        "two": [
          {
            "three": "aaaa"
          },
          {
            "four": "bbbb"
          }
        ]
      }
    }

    ... becomes ...

    {
      "one/two/0/three": "aaaa",
      "one/two/1/four":  "bbb"
    }



License
-------

Copyright (c) 2011 Paul Asmuth

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to use, copy and modify copies of the Software, subject 
to the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

