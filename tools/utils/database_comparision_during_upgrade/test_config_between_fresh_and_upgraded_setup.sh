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
rm -rf *.uniq   only_in_upgraded only_in_fresh  new_in_upgrade difference_in_config_between_fresh_and_upgraded
path=./base_data
path2=./data_after_upgrade

rm -rf $path2
mkdir $path2

mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, value  from cloud.configuration" > $path2/configuration_upgrade
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, component  from cloud.configuration" > $path2/component_configuration_upgrade
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, category  from cloud.configuration" > $path2/category_configuration_upgrade
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, description  from cloud.configuration" > $path2/description_configuration_upgrade
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, scope  from cloud.configuration" > $path2/scope_configuration_upgrade


IFS=$'\n'


#to find difference between upgraded configuration and fresh install configuration

mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, value, default_value  from cloud.configuration" > $path2/configuration_upgrade



for row in `cat $path2/configuration_upgrade`
do

        grep $row $path/configuration_fresh > $a
        if [ ! -s $a  ]
        then

		echo $row > temp
		name=`awk '{print $1}' temp`
		value=`awk '{print $2}' temp`
		default=`awk '{print $3}' temp`
		grep '^'$name'[^\.]\w*' $path/configuration_fresh > t
		if [ ! -s t ]
                then
                        echo $row >> ./only_in_upgraded
                else
			fname=`awk '{print $1}' t`
	                fvalue=`awk '{print $2}' t`
                	fdefault=`awk '{print $3}' t`
			#echo $fname $fvalue $fdefault
			if [ $default !=  $value  ]
			then
				if [ $default == $fdefault ] && [ $value == $fvalue ]
				then
					echo
				else
					first="$name	$value"
					second="$fname    $fvalue"
					echo "in upgrade" >>difference_in_config_between_fresh_and_upgraded
					echo $first >> ./difference_in_config_between_fresh_and_upgraded
					echo "in fresh" >>difference_in_config_between_fresh_and_upgraded
					echo $second >> ./difference_in_config_between_fresh_and_upgraded
				fi
			fi
		fi

	fi
done

# to find configuration only available in fresh install but missing in upgraded setup

for row in `cat $path/configuration_fresh`
do

        grep $row $path2/configuration_upgrade > $a
        if [ ! -s $a  ]
        then

                echo "in fresh install" >>final_diff4
		echo $row >> ./final_diff4
		count=`wc -l <./final_diff4`
                echo $row > temp
                awk '{print $1}' temp > temp1
                for word in `cat ./temp1`
                do
                        #echo $word
			echo "in upgrde\n" >> final_diff4
			grep '^'$word'[^\.]\w*' $path2/configuration_upgrade >> ./final_diff4
		        count1=`wc -l <./final_diff4`
			count1=`expr $count1 - 1`

			if [ $count == $count1 ]
			then
				echo $row >> ./only_in_fresh
			fi

                done
        fi

done
rm -rf final_diff4


# to find difference between upgraded and fresh install on component field

cat $path2/component_configuration_upgrade > ./component
cat $path/component_configuration_fresh >> ./component
sort ./component > ./component.sort
uniq -u ./component.sort > component.uniq





# to find different between upgraded and fresh install on category field

cat $path2/category_configuration_upgrade > ./category
cat $path/category_configuration_fresh >> ./category
sort ./category > ./category.sort
uniq -u ./category.sort > category.uniq

# to find different between upgraded and fresh install on scope  field

cat $path2/scope_configuration_upgrade > ./scope
cat $path/scope_configuration_fresh >> ./scope
sort ./scope > ./scope.sort
uniq -u ./scope.sort > scope.uniq

# to find different between upgraded and fresh install on description  field

cat $path2/description_configuration_upgrade > ./description
cat $path/description_configuration_fresh >> ./description
sort ./description > ./description.sort
uniq -u ./description.sort > description.uniq




rm -rf $path2 *.sort category description scope component temp temp1 $a
rm -rf mismatch_config_between_before_and_after_upgrade  config_difference_before_and_after_upgrade.sort t
#rm -rf $path2
