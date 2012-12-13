FnorDB
======

FnorDB is a HTTP+JSON <=> MySQL gateway. It fetches nested records from the
database without using SQL JOIN and parallizes queries where possible.


Usage
-----

### Starting FnorDB

    ./fnordb --db "mysql://localhost:3306/?user=root" --http 8080 -c config/


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

    findSome(limit, [, offset [, order]])


### findWhere

    findOneWhere(condition)

    findAllWhere(condition)

    findSomeWhere(condition, limit, [, offset [, order]])


### count

    relation.count



Configuration
-------------

here be dragons

Installation
------------

You need java and sbt to build FnorDB:

    ./build jar


Todo
----

+ implement last commands
+ stats aggregation
+ serialize qtime + query
+ timeouts (http + sql)
+ macros
+ better json escaping

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

