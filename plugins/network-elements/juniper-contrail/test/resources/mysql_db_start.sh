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

echo "starting mysql on port: "$1

echo "creating temporary mysql db directories /tmp/mysql"$1 
mkdir /tmp/mysql$1
mkdir /tmp/mysql$1/data

echo "install db";

mysql_install_db --user=$USER --datadir=/tmp/mysql$1/data
mysqld_safe --datadir=/tmp/mysql$1/data --socket=/tmp/mysql$1/mysqld.sock --port=$1 --log-error=/tmp/mysql$1/mysql.log --pid-file=/tmp/mysql$1/mysql.pid --user=$USER &

echo "new mysql server is started on port "$1

sleep 3

echo "commands ...."
echo "to connect(from local host): mysql -h 127.0.0.1 -P "$1 
echo "to stop: mysqladmin -S /tmp/mysql"$1"/mysqld.sock shutdown -u root"
