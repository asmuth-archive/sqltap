SQLTap
======

_This project is old and has become obsolete; it has not been verified to work with newer versions
of MySQL and there is only a single company that uses it in production that I know of which maintains
their own private fork. Additionally, Facebook published the GraphQL project, which looks like a much
nicer API and has gained significant momentum (GraphQL was released many years after SQLTap). This
means I will not maintain or work on this code anymore._

SQLTap is a document-oriented query frontend and cache for MySQL.

Users request (nested) documents using a custom declarative query language (see
below). SQLTap takes these requests and rewrites them into many small SQL queries,
which are then executed on the backend MySQL database. Once all data has
been returned from the database, SQLTap assembles the responses into a
single JSON document and returns it to the user.

SQLTap is a caching proxy; responses from MySQL are also stored in a local
memcache server. This allows SQLTap - after a short warmup phase - to answer most
repetitive queries without actually having to consult the database. It is important
to note that SQLTap caches _partial_ query responses, i.e. it doesn't cache the
full query response, but the individual parts from which the response was constructed.
The cached data partials are shared accross similar queries, reducing the total
cache size and increasing hitrate.

The query cache is completely transparent to the user - there is no need for
explicit expiration and SQLTap will never serve stale data. This is achieved by
using the MySQL's row based replication protocol to subscribe to notifications on
row changes and expiring cached data partials accordingly.

SQLTap requires MySQL 5.6+ with Row Based Replication enabled.


### Table of Contents

+ [Usage](#usage)
+ [HTTP API](#http-api)
+ [Query Language](#query-language)
+ [Internals](#internals)
+ [Examples](#examples)
+ [License](#license)


Usage
-----

### Starting SQLTap

    ./sqltap \
        --mysql-host localhost \
        --mysql-port 3006 \
        --mysql-user root \
        --mysql-database mydb \
        --http 8080 \
        -c config.xml


HTTP API
--------

retrieve user record id#2342 with all fields:

    /query?q=user.findOne(2342){*}

retrieve user record id#2342 with fields: username and email):

    /query?q=user.findOne(2342){username,email}

retrieve user record with id#2342 with all orders and all fields::

    /query?q=user.findOne(2342){*,orders.findAll{*}}

you can send multiple queries seperated by semicolon (`;`):

    /query?q=user.findOne(1){*};user.findOne(2){*}

you can repeat a single queries n times using this syntax...

    /query?q=user.findOne($){*}&for=1,2,3

is the same as:

    /query?q=user.findOne(1){*};user.findOne(2){*};user.findOne(3){*}


Query Language
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

    products.findAllWhere("is_valid = 1", 10){*}


##### relation.countAll{}

example: count the number of products user #1234 has

    user.findOne(1234){products.countAll{}}


##### relation.countAllWhere("condition"){}

countAllWhere is DANGEROUS: you must not construct a countAllWhere query from user supplied
data unless sanatizing it first (it allows for sql injection...)

example (count the first 10 valid products):

    products.countAllWhere("is_valid = 1"){}

WARNING: this doesn't work that well when used on a relation

Compare

  /query?q=product.findOne(44244778){id,votes.findAllWhere("created_at>'2014-01-01'"){}}
  
to

  /query?q=product.findOne(44244778){id,votes.countAllWhere("created_at>'2014-01-01'"){}}

and 

  /query?q=vote.countAllWhere("product_id = 44244778 and created_at>'2014-01-01'"){}


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

+ Cached partial query responses are gzipped and cached in memcache


Examples
--------

Real-life product detail page query:

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

