#!/usr/bin/env bash
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

CP=./

for file in $CATALINA_HOME/webapps/client/WEB-INF/lib/*.jar
do
  CP=${CP}$PATHSEP$file
done

for file in $CATALINA_HOME/lib/*.jar; do
  CP=${CP}$PATHSEP$file
done

#echo CP is $CP

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
