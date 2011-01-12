#!/bin/bash

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


config_htaccess
for i in $@
do
        ips "$i"
done
exit $?

