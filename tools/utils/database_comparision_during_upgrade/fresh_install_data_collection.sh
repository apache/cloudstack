#!/usr/bin/env bash
dbhost=$1
dbuser=$2
dbpwd=$3
rm -rf base_data
mkdir base_data
mkdir ./base_data/usage_data

mysql -u $dbuser -p$dbpwd -h $dbhost -e "show tables from cloud" > ./base_data/tables
for tablename in `cat ./base_data/tables`
do
        if [ $tablename != 'Tables_in_cloud' ]
        then
                mysql -u $dbuser -p$dbpwd -h $dbhost -e "describe cloud.$tablename" > ./base_data/$tablename
        fi
done

mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, value, default_value  from cloud.configuration" > ./base_data/configuration_fresh
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, component  from cloud.configuration" > ./base_data/component_configuration_fresh
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, category  from cloud.configuration" > ./base_data/category_configuration_fresh
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, description  from cloud.configuration" > ./base_data/description_configuration_fresh
mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, scope  from cloud.configuration" > ./base_data/scope_configuration_fresh

mysql -u $dbuser -p$dbpwd -h $dbhost -e "show tables from cloud_usage" > ./base_data/usage_data/usage_tables
for tablename in `cat ./base_data/usage_data/usage_tables`
do
        if [ $tablename != 'Tables_in_cloud_usage' ]
        then
                mysql -u $dbuser -p$dbpwd -h $dbhost -e "describe cloud_usage.$tablename" > ./base_data/usage_data/$tablename
        fi
done

