#!/bin/bash
# $Id: save_password_to_domr.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/network/domr/save_password_to_domr.sh $
# @VERSION@

#replace a line in a file of the form key=value
#   $1 filename
#   $2 keyname
#   $3 value
replace_in_file_on_domr() {
  local filename=$1
  local keyname=$2
  local value=$3
  $VIA_SSH "sed -i /$keyname/d $filename; \
  		 	echo "$keyname=$value" >> $filename "
  		 	
  # $VIA_SSH "sed -e /$keyname/d $filename > $filename.new; \
  #        mv $filename.new $filename;\
  #         echo "$keyname=$value" >> $filename "
  
  return $?
}

cert="/root/.ssh/id_rsa.cloud"

while getopts 'r:v:p:' OPTION
do
  case $OPTION in
  r)	
		DOMR_IP="$OPTARG"
		;;
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

VIA_SSH="ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$DOMR_IP"

$VIA_SSH "if [ ! -f /root/passwords ]; then touch /root/passwords; fi;"

replace_in_file_on_domr /root/passwords $VM_IP $PASSWORD

if [ $? -ne 0 ]
then
	exit 1
fi

exit 0
