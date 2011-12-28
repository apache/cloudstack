#!/usr/bin/env bash



  #
  # Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
  # 
  # This software is licensed under the GNU General Public License v3 or later.
  # 
  # It is free software: you can redistribute it and/or modify
  # it under the terms of the GNU General Public License as published by
  # the Free Software Foundation, either version 3 of the License, or any later version.
  # This program is distributed in the hope that it will be useful,
  # but WITHOUT ANY WARRANTY; without even the implied warranty of
  # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  # GNU General Public License for more details.
  # 
  # You should have received a copy of the GNU General Public License
  # along with this program.  If not, see <http://www.gnu.org/licenses/>.
  #
 

# deploy-db.sh -- deploys the database configuration.
#
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
