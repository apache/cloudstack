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

if [ "$#" -ne 1 ] ; then
   echo "Usage: $0 <port>" >&2
   exit 1
fi

PORT=$1
echo "starting mysql on port: "$PORT

echo "creating temporary mysql db directories /tmp/mysql"$PORT
mkdir /tmp/mysql$PORT
mkdir /tmp/mysql$PORT/data

echo "install db";

mysql_install_db --user=$USER --datadir=/tmp/mysql$PORT/data
mysqld_safe --datadir=/tmp/mysql$PORT/data --socket=/tmp/mysql$PORT/mysqld.sock --port=$PORT --log-error=/tmp/mysql$PORT/mysql.log --pid-file=/tmp/mysql$PORT/mysql.pid --user=$USER &

attempts=0
while [ $attempts -lt 30 ]; do
    db=$(mysql -h 127.0.0.1 -P $PORT --user=$USER -e "show databases;")
    status=$?
    if [ $status == 0 ]; then
	break
    fi
    attempts=`expr $attempts + 1`
    sleep 1
done

echo "new mysql server is started on port "$PORT
echo $db

echo "commands ...."
echo "to connect(from local host): mysql -h 127.0.0.1 -P "$PORT
echo "to stop: mysqladmin -S /tmp/mysql"$PORT"/mysqld.sock shutdown -u root"
