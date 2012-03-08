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

BASE_DIR="/var/www/html/copy/"
HTACCESS="$BASE_DIR/.htaccess"

config_htaccess() {
  mkdir -p $BASE_DIR
  result=$?
  echo "Options -Indexes" > $HTACCESS
  let "result=$result+$?"
  echo "order deny,allow" >> $HTACCESS
  let "result=$result+$?"
  echo "deny from all" >> $HTACCESS
  let "result=$result+$?"
  return $result
}

ips(){
  echo "allow from $1" >> $HTACCESS
  result=$?
  return $result
}

is_append="$1"
shift
if [ $is_append != "true" ]; then
	config_htaccess
fi
for i in $@
do
        ips "$i"
done
exit $?

