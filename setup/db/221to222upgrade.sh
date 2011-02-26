#!/bin/bash

#######
# mysql phase 1

echo Running upgrade for database: cloud from 2.2.1 to 2.2.2
mysql -u root -C cloud < 221to222.sql
if [ $? -gt 0 ]
then 
exit 1
fi
echo Finished upgrade for database: cloud from 2.2.1 to 2.2.2

#######
# mysql phase 2

echo Running upgrade for database: cloud_usage from 2.2.1 to 2.2.2
mysql -u root -C cloud_usage < 221to222_usage.sql
if [ $? -gt 0 ]
then 
exit 1
fi
echo Finished upgrade for database: cloud_usage from 2.2.1 to 2.2.2


