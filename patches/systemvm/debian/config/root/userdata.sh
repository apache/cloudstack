#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

 

source /root/func.sh

lock="biglock"
#default timeout value is 30 mins as userdata command is not synchronized on agent side any more,
#and multiple commands can be sent to the same VR at a time
locked=$(getLockFile $lock 1800)
if [ "$locked" != "1" ]
then
    exit 1
fi

usage() {
  printf "Usage: %s: -v <vm ip> -F <vm data folder> -f <vm data file> -d <data to put in file> \n" $(basename $0) >&2
  unlock_exit 2 $lock $locked
}

set -x

PORT=3922

create_htaccess() {
  local vmIp=$1
  local folder=$2
  local file=$3

  local result=0

  entry="RewriteRule ^$file$ ../$folder/%{REMOTE_ADDR}/$file [L,NC,QSA]"
  htaccessFolder="/var/www/html/latest"
  htaccessFile=$htaccessFolder/.htaccess
  mkdir -p $htaccessFolder
  touch $htaccessFile

# Fixed the issue with checking if record exists, rewrote the else/if logic, reference issue CLOUDSTACK-2053

  if ! grep -Fq "$entry" $htaccessFile
        then
                echo -e $entry >> $htaccessFile;
                result=$?
  fi

  entry="Options -Indexes\\nOrder Deny,Allow\\nDeny from all\\nAllow from $vmIp"
  testentry="Allow from $vmIp"
  htaccessFolder="/var/www/html/$folder/$vmIp"
  htaccessFile=$htaccessFolder/.htaccess
  if ! grep -Fq "$testentry" $htaccessFile
        then
                mkdir -p $htaccessFolder
                echo -e $entry > $htaccessFile
                result=$?
  fi


# Please reference issue CLOUDSTACK-2053, added to fix boto/cloud-init integration

  htaccessFileNoIP="/var/www/html/latest/.htaccess"
  metadataentry1='RewriteRule ^meta-data/$ ../metadata/%{REMOTE_ADDR}/meta-data [L,NC,QSA]'
  metadataentry2='RewriteRule ^meta-data/(.*)$ ../metadata/%{REMOTE_ADDR}/$1 [L,NC,QSA]'
  if ! grep -Fq "$metadataentry1" $htaccessFileNoIP
        then
                echo -e "$metadataentry1" >> $htaccessFileNoIP;
  fi

  if ! grep -Fq "$metadataentry2" $htaccessFileNoIP
        then
                echo -e "$metadataentry2" >> $htaccessFileNoIP;
  fi

  return $result
}

copy_vm_data_file() {
  local vmIp=$1
  local folder=$2
  local file=$3
  local dataFile=$4        
  
  dest=/var/www/html/$folder/$vmIp/$file
  metamanifest=/var/www/html/$folder/$vmIp/meta-data
  chmod +r $dataFile
  cp $dataFile $dest
  chmod 644 $dest
  touch $metamanifest
  chmod 644 $metamanifest
  if [ "$folder" == "metadata" ] || [ "$folder" == "meta-data" ]
  then
    sed -i '/$file/d' $metamanifest
    echo $file >> $metamanifest
  fi
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
                unlock_exit 1 $lock $locked
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
    unlock_exit 1 $lock $locked
  fi
  
  copy_vm_data_file $vmIp $folder $file $dataFile
else
  delete_vm_data_file $vmIp $folder $file
fi

unlock_exit $? $lock $locked
