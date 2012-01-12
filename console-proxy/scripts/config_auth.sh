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



 


BASE_DIR="/var/www/html/copy/template/"
HTACCESS="$BASE_DIR/.htaccess"

PASSWDFILE="/etc/httpd/.htpasswd"
if [ -d /etc/apache2 ]
then
  PASSWDFILE="/etc/apache2/.htpasswd"
fi

config_htaccess() {
  mkdir -p $BASE_DIR
  result=$?
  echo "Options -Indexes" > $HTACCESS
  let "result=$result+$?"
  echo "AuthType Basic" >> $HTACCESS
  let "result=$result+$?"
  echo "AuthName \"Authentication Required\"" >> $HTACCESS
  let "result=$result+$?"
  echo "AuthUserFile  \"$PASSWDFILE\"" >> $HTACCESS
  let "result=$result+$?"
  echo "Require valid-user" >> $HTACCESS
  let "result=$result+$?"
  return $result 
}

write_passwd() {
  local user=$1
  local passwd=$2
  htpasswd -bc $PASSWDFILE $user $passwd
  return $?
}

if [ $# -ne 2 ] ; then
	echo $"Usage: `basename $0` username password "
	exit 0
fi

write_passwd $1 $2
if [ $? -ne 0 ]
then
  echo "Failed to update password"
  exit 2
fi

config_htaccess 
exit $?
