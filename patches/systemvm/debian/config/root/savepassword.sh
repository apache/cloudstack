#!/bin/bash
# Usage
#	save_password -v <user VM IP> -p <password>
#

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

if [ ! -f /root/passwords ]; 
  then 
    touch /root/passwords; 
fi

sed -i /$VM_IP/d /root/passwords
echo "$VM_IP=$PASSWORD" >> /root/passwords

if [ $? -ne 0 ]
then
	exit 1
fi

exit 0
