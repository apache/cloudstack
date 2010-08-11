#!/bin/bash
# $Id: vm_data.sh 9307 2010-06-08 00:43:08Z chiradeep $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xenserver/patch/vm_data.sh $
# vm_data.sh -- adds userdata and metadata for a VM to a DomR
#
# @VERSION@

usage() {
  printf "Usage: %s: -r <domr-ip> -v <vm ip> -F <vm data folder> -f <vm data file> -d <data to put in file> \n" $(basename $0) >&2
  exit 2
}

set -x
CERT="/root/.ssh/id_rsa.cloud"
PORT=3922

create_htaccess() {
  local domrIp=$1
  local vmIp=$2
  local folder=$3
  local file=$4
  
  local result=0
  
  entry="RewriteRule ^$file$  ../$folder/%{REMOTE_ADDR}/$file [L,NC,QSA]"
  htaccessFolder="/var/www/html/latest"
  htaccessFile=$htaccessFolder/.htaccess
  ssh -p $PORT -o StrictHostKeyChecking=no -i $CERT root@$domrIp "mkdir -p $htaccessFolder; touch $htaccessFile; grep -F \"$entry\" $htaccessFile; if [ \$? -gt 0 ]; then echo -e \"$entry\" >> $htaccessFile; fi" >/dev/null
  result=$?
  
  if [ $result -eq 0 ]
  then
    entry="Options -Indexes\\nOrder Deny,Allow\\nDeny from all\\nAllow from $vmIp"
    htaccessFolder="/var/www/html/$folder/$vmIp"
    htaccessFile=$htaccessFolder/.htaccess
    ssh -p $PORT -o StrictHostKeyChecking=no -i $CERT root@$domrIp "mkdir -p $htaccessFolder; echo -e \"$entry\" > $htaccessFile" >/dev/null
    result=$?
  fi
  
  return $result  
}

copy_vm_data_file() {
  local domrIp=$1
  local vmIp=$2
  local folder=$3
  local file=$4
  local dataFile=$5        
  
  chmod +r $dataFile
  scp -P $PORT -o StrictHostKeyChecking=no -i $CERT $dataFile root@$domrIp:/var/www/html/$folder/$vmIp/$file >/dev/null
  return $?
}

delete_vm_data_file() {
  local domrIp=$1
  local vmIp=$2
  local folder=$3
  local file=$4
  
  vmDataFilePath="/var/www/html/$folder/$vmIp/$file"
  ssh -p $PORT -o StrictHostKeyChecking=no -i $CERT root@$domrIp "if [ -f $vmDataFilePath ]; then rm -rf $vmDataFilePath; fi" >/dev/null
  return $?
}

domrIp=
vmIp=
folder=
file=
dataFile=

while getopts 'r:v:F:f:d:' OPTION
do
  case $OPTION in
  r)	domrIp="$OPTARG"
		;;
  v)	vmIp="$OPTARG"
		;;
  F)	folder="$OPTARG"
  		;;
  f)	file="$OPTARG"
  		;;
  d)	dataFile="$OPTARG"
  		;;
  ?)    usage
		exit 1
		;;
  esac
done

[ "$domrIp" == "" ] || [ "$vmIp" == "" ]  || [ "$folder" == "" ] || [ "$file" == "" ] && usage 
[ "$folder" != "userdata" ] && [ "$folder" != "metadata" ] && usage

if [ "$dataFile" != "" ]
then
  create_htaccess $domrIp $vmIp $folder $file
  
  if [ $? -gt 0 ]
  then
    exit 1
  fi
  
  copy_vm_data_file $domrIp $vmIp $folder $file $dataFile
else
  delete_vm_data_file $domrIp $vmIp $folder $file
fi

exit $?
