mysql_host="localhost"
mysql_user="root"
mysql_database="sqltap"

sql_schema="example/schema.sql"
mysql -u $mysql_user -h $mysql_host $mysql_database < $sql_schema || exit 1

sbt "run \
  -c example/schema.xml \
  --cache-backend noop \
  --expiration-handler noop \
  --http 8080 \
  --mysql-host $mysql_host \
  --mysql-user $mysql_user \
  --mysql-database $mysql_database\
  --debug"

# TEST QUERIES
#   http://localhost:8080/query?q=user.findAll{*}
#   http://localhost:8080/query?q=user.findAllWhere("username='hans'"){*}
#   http://localhost:8080/query?q=user.findAllWhere("username='i_lövé_ümlautß'"){*}
#   http://localhost:8080/query?q=user.findAllWhere(%22username=%27i_l%c3%b6v%c3%a9_%c3%bcmlaut%c3%9f%27%22){*}

