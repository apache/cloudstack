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

dbhost=$1
dbuser=$2
dbpwd=$3
path=./base_data/usage_data
path1=./data_after_upgrade
rm -rf $path1
mkdir $path1

mysql -u $dbuser -p$dbpwd -h $dbhost -e "show tables from cloud_usage" > $path1/usage_tables_upgrade

# to check if number of tables and table name differs

diff $path/usage_tables $path1/usage_tables_upgrade > usage_table_difference
if [ -s usage_table_difference ]
then
        echo "usage table differs between fresh and upgraded install "
        cat usage_table_difference
        # do something as file has data
else
        echo "usage tables  are identicals between fresh and upgraded install "
        rm -rf usage_tables_difference
        # do something as file is empty

fi



for tablename in `cat $path1/usage_tables_upgrade`
do
        if [ $tablename != 'Tables_in_cloud_usage' ]
        then
                mysql -u $dbuser -p$dbpwd -h $dbhost -e "describe cloud_usage.$tablename" > $path1/upgradedschema
                cat $path/$tablename >  $tablename.diff
                cat $path1/upgradedschema >> $tablename.diff
                sort $tablename.diff > $tablename.sort
                uniq -u $tablename.sort > $tablename.uniq

                if [ -s $tablename.uniq ]
                then
                        echo $tablename  "table schema is different."
                        cat $path1/upgradedschema > usage_$tablename
                        rm -rf $tablename.diff $tablename.sort

                        # do something as file has data
                else

                        rm -rf $tablename.diff $tablename.sort $tablename.uniq
                fi


        fi
done




rm -rf $path1
