FnorDB
======

here be dragons


Usage
-----

### Starting FnorDB

here be dragons

### Sending queries

retrieve user record id#2342 with all fields:

    user.findOne(2342){*}

retrieve user record id#2342 with fields: username and email):


    user.findOne(2342){username,email}

retrieve user record with id#2342 with all orders and all fields::

    user.findOne(2342){*,orders.findAll{*}}


Instructions
------------

### findOne

    resource.findOne(id)

    relation.findOne



### findAll

    relation.findAll


### findSome

    findSome(limit, [, offset [, order]])


### findWhere

    findOneWhere(condition)

    findAllWhere(condition)

    findSomeWhere(condition, limit, [, offset [, order]])


### count

    relation.count



Installation
------------

here be dragons


Todo
----

+ timeouts (http + sql)
+ {*,fnord} issue
+ implement last commands
+ stats aggregation
+ /config.xml action
+ json writer fixes
+ serialize qtime + query
+ serialize relation name
+ catch exceptions in sql conn



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

