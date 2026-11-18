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
a=0
rm -rf *.uniq mismatch_config_between_before_and_after_upgrade difference_in_config_between_fresh_and_upgraded configurations_only_before_upgrade  only_in_upgrade only_in_fresh config_difference_before_and_after_upgrade new_in_upgrade
path=./base_data
path1=./data_before_upgrade
path2=./data_after_upgrade

rm -rf $path2
mkdir $path2

mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, value  from cloud.configuration" > $path2/configuration_upgrade
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, component  from cloud.configuration" > $path2/component_configuration_upgrade
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, category  from cloud.configuration" > $path2/category_configuration_upgrade
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, description  from cloud.configuration" > $path2/description_configuration_upgrade
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, scope  from cloud.configuration" > $path2/scope_configuration_upgrade


IFS=$'\n'

# to find  any missing or mismatch  configuration value before upgrade and after upgrade setup
for row in `cat $path1/configuration_before_upgrade`
do

        grep $row $path2/configuration_upgrade > $a
        if [ ! -s $a  ]
        then

                echo $row >> ./mismatch_config_between_before_and_after_upgrade
                count=`wc -l <./mismatch_config_between_before_and_after_upgrade`
                echo $row > temp
                awk '{print $1}' temp > temp1
                for word in `cat ./temp1`
                do
                        #echo $word
                        mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, value  from cloud.configuration where name= '$word'" >> ./mismatch_config_between_before_and_after_upgrade
                        count1=`wc -l <./mismatch_config_between_before_and_after_upgrade`
                        if [ $count == $count1 ]
                        then
                                echo $row >> ./configurations_only_before_upgrade
                                sed -i '$ d' ./mismatch_config_between_before_and_after_upgrade
                        fi



                done
        fi

done


#to find configuration present only in the upgraded setup


for row in `cat $path2/configuration_upgrade`
do

        grep $row $path1/configuration_before_upgrade > $a
        if [ ! -s $a  ]
        then
                echo $row >> ./config_difference_before_and_after_upgrade
                count=`wc -l <./config_difference_before_and_after_upgrade`
                echo $row > temp
                awk '{print $1}' temp > temp1
                for word in `cat ./temp1`
                do
                        grep '^'$word'[^\.]\w*' $path1/configuration_before_upgrade >> ./config_difference_before_and_after_upgrade
                        count1=`wc -l <./config_difference_before_and_after_upgrade`
                        if [ $count == $count1 ]
                        then
                                echo $row >> ./new_in_upgrade
                                sed -i '$ d' ./config_difference_before_and_after_upgrade

                        fi



                done
        fi

done

#to find all the difference between before and after upgrade
cat ./mismatch_config_between_before_and_after_upgrade >> ./config_difference_before_and_after_upgrade
sort ./config_difference_before_and_after_upgrade > ./config_difference_before_and_after_upgrade.sort
uniq ./config_difference_before_and_after_upgrade.sort > ./config_difference_before_and_after_upgrade





rm -rf $path2 *.sort category description scope component temp temp1 $a
rm -rf mismatch_config_between_before_and_after_upgrade  config_difference_before_and_after_upgrade.sort t
