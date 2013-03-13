#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.



 

# deploy-db.sh -- deploys the database configuration.
# set -x

if [ "$1" == "" ]; then
  printf "Usage: %s [path to server-setup.xml] [path to additional sql] [root password]\n" $(basename $0) >&2
  exit 1;
fi

if [ ! -f $1 ]; then
  echo "Error: Unable to find $1"
  exit 2
fi

if [ "$2" != "" ]; then
  if [ ! -f $2 ]; then
    echo "Error: Unable to find $2"
    exit 3
  fi
fi

if [ ! -f create-database.sql ]; then
  printf "Error: Unable to find create-database.sql\n"
  exit 4
fi

if [ ! -f create-schema.sql ]; then
  printf "Error: Unable to find create-schema.sql\n"
  exit 5
fi

if [ ! -f create-index-fk.sql ]; then
  printf "Error: Unable to find create-index-fk.sql\n"
  exit 6;
fi

PATHSEP=':'
if [[ $OSTYPE == "cygwin" ]] ; then
  export CATALINA_HOME=`cygpath -m $CATALINA_HOME`
  PATHSEP=';'
fi

handle_error() {
    mysqlout=$?
    if [ $mysqlout -eq 1 ]; then
        printf "Please enter root password for MySQL.\n" 
        mysql --user=root --password < $1
        if [ $? -ne 0 ]; then
            printf "Error: Cannot execute $1\n"
            exit 10
        fi
    elif [ $mysqlout -eq 127 ]; then
        printf "Error: Cannot execute $1 - mysql command not found.\n"
        exit 11
    elif [ $mysqlout -ne 0 ]; then
        printf "Error: Cannot execute $1\n"
        exit 11
    fi
}

echo "Recreating Database cloud."
mysql --user=root --password=$3 < create-database.sql > /dev/null 2>/dev/null
handle_error create-database.sql


echo "Recreating Database cloud_usage"
mysql --user=root --password=$3 < create-database-premium.sql > /dev/null 2>/dev/null
handle_error create-database-premium.sql

mysql --user=cloud --password=cloud cloud < create-schema.sql
if [ $? -ne 0 ]; then
  printf "Error: Cannot execute create-schema.sql\n"
  exit 11
fi

mysql --user=cloud --password=cloud < create-schema-premium.sql
if [ $? -ne 0 ]; then
  printf "Error: Cannot execute create-schema-premium.sql\n"
  exit 11
fi

CP=./

CP=${CP}$PATHSEP$CATALINA_HOME/conf

# Add mysql jar from mysql-connector-java package to CP
# for Jenkins
CP=${CP}${PATHSEP}/usr/share/java/mysql-connector-java.jar

for file in $CATALINA_HOME/webapps/client/WEB-INF/lib/*.jar
do
  CP=${CP}$PATHSEP$file
done

for file in $CATALINA_HOME/lib/*.jar; do
  CP=${CP}$PATHSEP$file
done

echo CP is $CP

java -cp $CP com.cloud.test.DatabaseConfig $*

if [ $? -ne 0 ]
then
	exit 1
fi

if [ "$2" != "" ]; then
  mysql --user=cloud --password=cloud cloud < $2
  if [ $? -ne 0 ]; then
    printf "Error: Cannot execute $2\n"
    exit 12
  fi
fi
  

echo "Creating Indice and Foreign Keys"
mysql --user=cloud --password=cloud cloud < create-index-fk.sql
if [ $? -ne 0 ]; then
  printf "Error: Cannot execute create-index-fk.sql\n"
  exit 13
fi
