#!/bin/bash

usage() {
  printf "Usage: %s: -v <vm ip> -F <vm data folder> -f <vm data file> -d <data to put in file> \n" $(basename $0) >&2
  exit 2
}

set -x

PORT=3922

create_htaccess() {
  local vmIp=$1
  local folder=$2
  local file=$3
  
  local result=0
  
  entry="RewriteRule ^$file$  ../$folder/%{REMOTE_ADDR}/$file [L,NC,QSA]"
  htaccessFolder="/var/www/html/latest"
  htaccessFile=$htaccessFolder/.htaccess
  mkdir -p $htaccessFolder
  touch $htaccessFile
  grep -F \"$entry\" $htaccessFile 
  if [ \$? -gt 0 ]; then 
    echo -e \"$entry\" >> $htaccessFile; 
  fi
  result=$?
  
  if [ $result -eq 0 ]; then
    entry="Options -Indexes\\nOrder Deny,Allow\\nDeny from all\\nAllow from $vmIp"
    htaccessFolder="/var/www/html/$folder/$vmIp"
    htaccessFile=$htaccessFolder/.htaccess
    
    mkdir -p $htaccessFolder
    echo -e \"$entry\" > $htaccessFile
    result=$?
  fi
  
  return $result  
}

copy_vm_data_file() {
  local vmIp=$1
  local folder=$2
  local file=$3
  local dataFile=$4        
  
  chmod +r $dataFile
  cp $dataFile /var/www/html/$folder/$vmIp/$file >/dev/null
  return $?
}

delete_vm_data_file() {
  local domrIp=$1
  local vmIp=$2
  local folder=$3
  local file=$4
  
  vmDataFilePath="/var/www/html/$folder/$vmIp/$file"
  if [ -f $vmDataFilePath ]; then 
    rm -rf $vmDataFilePath 
  fi
  return $?
}

vmIp=
folder=
file=
dataFile=

while getopts 'v:F:f:d:' OPTION
do
  case $OPTION in
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

[ "$vmIp" == "" ]  || [ "$folder" == "" ] || [ "$file" == "" ] && usage 
[ "$folder" != "userdata" ] && [ "$folder" != "metadata" ] && usage

if [ "$dataFile" != "" ]
then
  create_htaccess $vmIp $folder $file
  
  if [ $? -gt 0 ]
  then
    exit 1
  fi
  
  copy_vm_data_file $vmIp $folder $file $dataFile
else
  delete_vm_data_file $vmIp $folder $file
fi

exit $?
