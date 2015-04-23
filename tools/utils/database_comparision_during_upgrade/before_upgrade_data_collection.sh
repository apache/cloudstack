#!/usr/bin/env bash
dbhost=$1
dbuser=$2
dbpwd=$3
rm -rf data_before_upgrade
mkdir data_before_upgrade

mysql -u $dbuser -p$dbpwd -h $dbhost --skip-column-names  -e "select  name, value  from cloud.configuration" > ./data_before_upgrade/configuration_before_upgrade


