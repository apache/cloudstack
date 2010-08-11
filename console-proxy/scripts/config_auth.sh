#!/bin/bash

BASE_DIR="/var/www/html/copy/template/"
HTACCESS="$BASE_DIR/.htaccess"
PASSWDFILE="/etc/httpd/.htpasswd"

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
