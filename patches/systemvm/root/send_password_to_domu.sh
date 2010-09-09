#!/bin/bash
# $Id: send_password_to_domu.sh 9947 2010-06-25 19:34:24Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/patches/kvm/root/send_password_to_domu.sh $
# @VERSION@

# set -x

#replace a line in a file of the form key=value
#   $1 filename
#   $2 keyname
#   $3 value
replace_in_file() {
  local filename=$1
  local keyname=$2
  local value=$3
  sed -i /$keyname=/d $filename
  echo "$keyname=$value" >> $filename
  return $?
}

#get a value from a file in the form key=value
#   $1 filename
#   $2 keyname
get_value() {
  local filename=$1
  local keyname=$2
  grep -i $keyname= $filename | cut -d= -f2
}

ip=$1

logger "send_password_to_domu called to service a request for $ip."

while read input
do
	if [ "$input" == "" ]
	then
		break
	fi

	request=$(echo $input | grep "DomU_Request:" | cut -d: -f2 | sed 's/^[ \t]*//')

	if [ "$request" != "" ]
	then
		break
	fi
done

# echo -e \"\\\"HTTP/1.0 200 OK\\\nDocumentType: text/plain\\\n\\\n\\\"\"; 

if [ "$request" == "send_my_password" ]
then
	password=$(get_value /root/passwords $ip)
	if [ "$password" == "" ]
	then
		logger "send_password_to_domu sent bad_request to $ip."
		echo "bad_request"
	else
		logger "send_password_to_domu sent a password to $ip."
		echo $password
	fi
else
	if [ "$request" == "saved_password" ]
	then
		replace_in_file /root/passwords $ip "saved_password"
		logger "send_password_to_domu sent saved_password to $ip."
		echo "saved_password"
	else
		logger "send_password_to_domu sent bad_request to $ip."
		echo "bad_request"
	fi
fi

# echo -e \"\\\"\\\n\\\"\"

exit 0
