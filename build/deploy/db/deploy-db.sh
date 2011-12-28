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

# set -x

if [ "$1" == "" ]; then
  printf "Usage: %s  [path to additional sql] [root password]\n" $(basename $0) >&2
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
else
  mysql="mysql"
  service mysql status > /dev/null 2>/dev/null
  if [ $? -eq 1 ]; then
    mysql="mysqld"
    service mysqld status > /dev/null 2>/dev/null
    if [ $? -ne 0 ]; then
      printf "Unable to find mysql daemon\n"
      exit 7
    fi
  fi

  echo "Starting mysql"
  service $mysql start > /dev/null 2>/dev/null

fi

echo "Recreating Database."
mysql --user=root --password=$3 < create-database.sql > /dev/null 2>/dev/null
mysqlout=$?
if [ $mysqlout -eq 1 ]; then
  printf "Please enter root password for MySQL.\n" 
  mysql --user=root --password < create-database.sql
  if [ $? -ne 0 ]; then
    printf "Error: Cannot execute create-database.sql\n"
    exit 10
  fi
elif [ $mysqlout -ne 0 ]; then
  printf "Error: Cannot execute create-database.sql\n"
  exit 11
fi

mysql --user=cloud --password=cloud cloud < create-schema.sql
if [ $? -ne 0 ]; then
  printf "Error: Cannot execute create-schema.sql\n"
  exit 11
fi

mysql --user=cloud --password=cloud cloud < create-schema-premium.sql
if [ $? -ne 0 ]; then
  printf "Error: Cannot execute create-schema-premium.sql\n"
  exit 11
fi

if [ "$1" != "" ]; then
  mysql --user=cloud --password=cloud cloud < $1
  if [ $? -ne 0 ]; then
    printf "Error: Cannot execute $1\n"
    exit 12
  fi
fi

if [ "$2" != "" ]; then
  echo "Adding Templates"
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
