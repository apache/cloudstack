#!/bin/bash
# Usage
#	save_password -v <user VM IP> -p <password>
#
PASSWD_FILE=/var/cache/cloud/passwords

while getopts 'v:p:' OPTION
do
  case $OPTION in
  v)	VM_IP="$OPTARG"
		;;
  p)	
		ENCODEDPASSWORD="$OPTARG"
		PASSWORD=$(echo $ENCODEDPASSWORD | tr '[a-m][n-z][A-M][N-Z]' '[n-z][a-m][N-Z][A-M]')
		;;
  ?)	echo "Incorrect usage"
		exit 1
		;;
  esac
done

[ -f $PASSWD_FILE ] ||  touch $PASSWD_FILE

sed -i /$VM_IP/d $PASSWD_FILE
echo "$VM_IP=$PASSWORD" >> $PASSWD_FILE

exit $?
