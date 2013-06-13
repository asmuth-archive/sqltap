SQLTap
======

SQLTap is a caching HTTP+JSON <=> MySQL gateway. It fetches nested records from the
database without using SQL JOIN and parallelizes queries where possible.

Rationale
---------

A question that comes up frequently is "Why would I want use a proxy to retrieve records
from MySQL rather than accessing it directly"?

SQLTap was created under the name "Fast Fetch Service" while re-designing a substantial
part of the DaWanda.com ecommerce application. The goal was to improve page render time
and to obviate some of the anti-patterns that are commonly found in ORM-based web apps.

#### Automatic Parallelization

In a web application context, you often need to retrieve a collection of related
database records to fulfill a http request. For example, to render a product detail page,
you might need to retrieve a 'product' record and all 'image' records that belong to
the product record.

The naive way to do this without putting the burden on the database by using an
expensive join operation is to sequentially execute multiple SQL queries. E.g. first
retrieve the product record and then retrieve all the image records. This is also what
some ORMs like ActiveRecord will do.

Retrieving the records in parallel rather than sequentially can result in a huge drop
in response time, which is highly desirable for user facing applications.

As an example, assume retrieving a single record takes 10ms. Then retrieving 5 records
using sequential execution would take 50ms, but retrieving them in parallel would (in a
perfect world) still only take 10ms.

While this parallelization could be implemented explicitly in your application code, it
would introduce redundant logic and unnessecary complexity; Running parallel sql queries
from a single threaded web framework is not trivial, as the MySQL protocol does not allow
for pipelining per se and most MySQL client implementations use blocking I/O.

SQLTap executes all sql queries in parallel where possible using multiple connections to
MySQL and non-blocking I/O. This is a fully transparent optimization.

#### Query Caching

SQLTap contains some secret sauce that intelligently caches common query subtrees in
memcache. This makes a lot of requests a lot faster and in general reduces load on the
database.

The query cache is completely transparent; there is no need for explicit cache expiration,
as SQLTap uses MySQL's row based replication protocol to get notifications on record changes
and refresh caches accordingly.

The query cache is not fully implemented yet ;)

#### Encapsulation

SQLTap permits only a subset of SQL to be executed and enforces limits on maximum execution
time and result set size. This is to prevent SQL queries that might seem harmless when
implemented, but turn out to be a bottleneck as the data set grows.

Some of the modern web frameworks encourage you to use an ORM for database access. This often
results in bad code where requests to the sql database are scattered all over the code and
sometimes even the templates. In these codebases it can get really hard to predict the runtime
of a method/template and whether it will block.

Take as an example a helper method that renders one entry in a navigation menu. For each entry
the helper calls something like "entry.translation" which in turn issues a request to the
database to retrieve the translation record for this entry. As the number of entries in the
navigation grows, this would lead to potentially thousands of sql queries being executed just
to render a simple navigation menu.

The SQLTap query language encourages you to write descriptive queries (you specify what you
want rather than in which way you want it to be retrieved) and to fetch all required data with
a single, large query. This will hopefully make applications easier to maintain and less bloated
in the long term.

#### Query Optimization

SQLTap performs some trivial query optimizations (i.e. eliminating redundant queries)


Usage
-----

### Starting SQLTap

    ./sqltap --mysql-host localhost --mysql-port 3006 --mysql-user root --mysql-database mydb --http 8080 -c config.xml


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


Internals
---------

+ One main thread, accepts connections and hands them off to N worker
threads.

+ All worker threads are completely independent, each worker thread runs an event
loop (NIO + timeoutscheduler) in which http, sql and memcache connections are
multiplexed.

+ All connections (http, sql, memcache) are bound to a single worker/thread, timeouts
are also thread local. This means there is very little locking in the hot path
(basically only the handover from the main thread to the worker threads)

+ It contains a non blocking implementation of a subset of the mysql protocol. Each
worker owns one sql connection pool which opens a fixed max number of connections
and also contains a query queue.

+ Main thread also runs watchdog; kills dead workers.

+ The QueryParser and the HTTP and SQL protocl are implemented as simple state
machines.

+ CTrees are only used if the CTree is a subtree of the request, i.e. a ctree
is not used when the ctree is a "supertree" of the request. CTree are only matched
on findOne Instructions and each CTree query must start with a findOne Instruction.

Bechmarks
---------

ab / weighttp benchmarks here


Example Queries
---------------

Real-life product detail page:

    product.findOne(12345){
      deleted_at,view_counter,category_id,category_parent_id,is_valid,id,
      slug,availability,user_id,milli_units_per_item,unit,cents,currency,
      first_published_at,channel_id,mailable_in_option,

      user.findOne{
        id,seller_rating,country,rating,username,platform,

        ratings.countAll{},
        listed_products.countAll{},
        orders.countAll{},

        standard_addresses.findAll{
          city,country
        },

        standard_images.findAll{
          id,filename,synced,imageable_type,imageable_id
        },

        shop.findOne{
          id,subdomain,holiday_from,holiday_to,title,shipping_info,profile,
          featured_message_blocked,

          shop_translations.findAll{
            language,attribute,text
          }
        },

        payment_methods_users.findAll{
          *
        }
      },

      category_attribute_values_products.findAll{
        category_attribute_value_id,

        category_attribute_value.findOne{
          id,value
        }
      },

      shipping_summary.findAll{
        *
      },

      images.findAll{
        id,filename,synced,imageable_type,imageable_id
      },

      translations.findAll{
        language,attribute,text
      }
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

