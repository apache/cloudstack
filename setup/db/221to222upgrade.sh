#!/bin/bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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



 


# mysql phase 1

pgrep mysqld
if [ $? -ne 0 ]; then
   echo "Mysqld is not running"
   exit 1
fi

echo Running upgrade for database: cloud from 2.2.1 to 2.2.2
mysql -u root -C cloud < 221to222.sql
if [ $? -gt 0 ]
then 
exit 1
fi
echo Finished upgrade for database: cloud from 2.2.1 to 2.2.2

# mysql phase 2

echo Running upgrade for database: cloud_usage from 2.2.1 to 2.2.2
mysql -u root -C cloud_usage < 221to222_usage.sql
if [ $? -gt 0 ]
then 
exit 1
fi
echo Finished upgrade for database: cloud_usage from 2.2.1 to 2.2.2


