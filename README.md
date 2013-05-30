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

    /query?q=user.findOne(2342){*}

retrieve user record id#2342 with fields: username and email):

    /query?q=user.findOne(2342){username,email}

retrieve user record with id#2342 with all orders and all fields::

    /query?q=user.findOne(2342){*,orders.findAll{*}}


Instructions
------------


##### resource.findOne(id){...}
##### relation.findOne{...}

example: find user 1234 with all fields

    user.find(1234){*}

example: find user 1234 with fields username and email:

    user.find(1234){username,email}

example find user 1234 with her shop title:

    user.find(1234){shop.findOne{title}}


##### relation.findAll{...}
##### relation.findAll(limit){...}
##### relation.findAll(limit, offset){...}

example: find a user and all her product-ids

    user.findOne(1){products.findAll{id}}

example: find the first 10 products with all fields:

    products.findAll(10){*}


##### relation.findAllWhere("condition"){...}
##### relation.findAllWhere("condition", limit){...}
##### relation.findAllWhere("condition", limit, offset){...}

findAllWhere is DANGEROUS: you must not construct a findAllWhere query from user supplied
data unless sanatizing it first (it allows for sql injection...)

example (find the first 10 valid products):

    products.findAllWhere("", 10){*}



##### relation.countAll{}

example: count the number of products user #1234 has

    user.findOne(1234){products.countAll{}}



XML Schema
----------

here be dragons


Configuration
-------------

here be dragons


Installation
------------

You need java and sbt to build SQLTap:

    ./build jar



Sending Multiple Queries
------------------------

you can send multiple queries seperated by semicolon (`;`):

    /query?q=user.findOne(1){*};user.findOne(2){*}

you can repeat a single queries n times using this syntax...

    /query?q=user.findOne($){*}&for=1,2,3

is the same as:

    /query?q=user.findOne(1){*};user.findOne(2){*};user.findOne(3){*}


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

