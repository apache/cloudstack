#!/usr/bin/env bash
# deploy-db-bridge.sh -- deploys the cloudbridge database configuration.
#
# set -x

if [ ! -f cloudbridge_db.sql ]; then
  printf "Error: Unable to find cloudbridge_db.sql\n"
  exit 4
fi

if [ ! -f cloudbridge_schema.sql ]; then
  printf "Error: Unable to find cloudbridge_schema.sql\n"
  exit 5
fi

if [ ! -f cloudbridge_index.sql ]; then
  printf "Error: Unable to find cloudbridge_index.sql\n"
  exit 6;
fi

echo "Recreating Database."
mysql --user=root --password=$1 < cloudbridge_db.sql > /dev/null 2>/dev/null
mysqlout=$?
if [ $mysqlout -eq 1 ]; then
  printf "Please enter root password for MySQL.\n" 
  mysql --user=root --password < cloudbridge_db.sql
  if [ $? -ne 0 ]; then
    printf "Error: Cannot execute cloudbridge_db.sql\n"
    exit 10
  fi
elif [ $mysqlout -ne 0 ]; then
  printf "Error: Cannot execute cloudbridge_db.sql\n"
  exit 11
fi

mysql --user=cloud --password=cloud < cloudbridge_schema.sql
if [ $? -ne 0 ]; then
  printf "Error: Cannot execute cloudbridge_schema.sql\n"
  exit 11
fi

mysql --user=cloud --password=cloud < cloudbridge_multipart.sql
if [ $? -ne 0 ]
then
    exit 1
fi

echo "Creating Indice and Foreign Keys"
mysql --user=cloud --password=cloud < cloudbridge_index.sql
if [ $? -ne 0 ]; then
  printf "Error: Cannot execute cloudbridge_index.sql\n"
  exit 13
fi

mysql --user=cloud --password=cloud < cloudbridge_multipart_alter.sql
mysql --user=cloud --password=cloud < cloudbridge_bucketpolicy.sql
mysql --user=cloud --password=cloud < cloudbridge_policy_alter.sql
mysql --user=cloud --password=cloud < cloudbridge_offering.sql
mysql --user=cloud --password=cloud < cloudbridge_offering_alter.sql
