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



 

# $Id: vm_data.sh 9307 2010-06-08 00:43:08Z chiradeep $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/vm/hypervisor/xenserver/patch/vm_data.sh $
# vm_data.sh -- adds userdata and metadata for a VM to a DomR
# @VERSION@

usage() {
  printf "Usage: %s: -r <domr-ip> -v <vm ip> -F <vm data folder> -f <vm data file> -d <data to put in file> \n" $(basename $0) >&2
  exit 2
}

set -x
cert="/root/.ssh/id_rsa.cloud"
PORT=3922

create_htaccess() {
  local domrIp=$1
  local vmIp=$2
  local folder=$3
  local file=$4
  
  local result=0
  #rewrite rule in top level /latest folder to redirect 
  #to vm specific folder based on source ip
  entry="RewriteRule ^$file$  ../$folder/%{REMOTE_ADDR}/$file [L,NC,QSA]"
  htaccessFolder="/var/www/html/latest"
  htaccessFile=$htaccessFolder/.htaccess
  ssh -p $PORT -o StrictHostKeyChecking=no -i $cert root@$domrIp "mkdir -p $htaccessFolder; touch $htaccessFile; grep -F \"$entry\" $htaccessFile; if [ \$? -gt 0 ]; then echo -e \"$entry\" >> $htaccessFile; fi" >/dev/null
  result=$?
  
  if [ $result -eq 0 ]
  then
    #ensure that vm specific folder cannot be listed and that only 
    #the vm that owns the data can access the items in this directory
    entry="Options -Indexes\\nOrder Deny,Allow\\nDeny from all\\nAllow from $vmIp"
    htaccessFolder="/var/www/html/$folder/$vmIp"
    htaccessFile=$htaccessFolder/.htaccess
    ssh -p $PORT -o StrictHostKeyChecking=no -i $cert root@$domrIp "mkdir -p $htaccessFolder; echo -e \"$entry\" > $htaccessFile" >/dev/null
    result=$?
  fi
  
  #support access by http://<dhcp server>/latest/<metadata key> (legacy, see above) also
  # http://<dhcp server>/latest/meta-data/<metadata key> (correct)
  if [ "$folder" == "metadata" ] || [ "$folder" == "meta-data" ]
  then
    entry="RewriteRule ^meta-data/(.+)$  ../$folder/%{REMOTE_ADDR}/\\\$1 [L,NC,QSA]"
    htaccessFolder="/var/www/html/latest"
    htaccessFile=$htaccessFolder/.htaccess
    ssh -p $PORT -o StrictHostKeyChecking=no -i $cert root@$domrIp "grep -F \"$entry\" $htaccessFile; if [ \$? -gt 0 ]; then echo -e \"$entry\" >> $htaccessFile; fi" >/dev/null
    entry="RewriteRule ^meta-data/$  ../$folder/%{REMOTE_ADDR}/meta-data [L,NC,QSA]"
    ssh -p $PORT -o StrictHostKeyChecking=no -i $cert root@$domrIp "grep -F \"$entry\" $htaccessFile; if [ \$? -gt 0 ]; then echo -e \"$entry\" >> $htaccessFile; fi" >/dev/null
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
  
  dest=/var/www/html/$folder/$vmIp/$file
  metamanifest=/var/www/html/$folder/$vmIp/meta-data
  scp -P $PORT -o StrictHostKeyChecking=no -i $cert $dataFile root@$domrIp:$dest >/dev/null
  ssh -p $PORT -o StrictHostKeyChecking=no -i $cert root@$domrIp "chmod 644 $dest" > /dev/null
  ssh -p $PORT -o StrictHostKeyChecking=no -i $cert root@$domrIp "touch $metamanifest; chmod 644 $metamanifest" > /dev/null
  if [ "$folder" == "metadata" ] || [ "$folder" == "meta-data" ]
  then
    ssh -p $PORT -o StrictHostKeyChecking=no -i $cert root@$domrIp "sed -i '/$file/d' $metamanifest; echo $file >> $metamanifest" > /dev/null
  fi
  
  return $?
}

delete_vm_data_file() {
  local domrIp=$1
  local vmIp=$2
  local folder=$3
  local file=$4
  
  vmDataFilePath="/var/www/html/$folder/$vmIp/$file"
  ssh -p $PORT -o StrictHostKeyChecking=no -i $cert root@$domrIp "if [ -f $vmDataFilePath ]; then rm -rf $vmDataFilePath; fi" >/dev/null
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
