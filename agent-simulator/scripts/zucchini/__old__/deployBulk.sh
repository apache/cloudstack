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

  
usage() {
  printf "Deploy many VMs: %s: -h management-server -n numberofvms  [[-b batchsize] [-w wait for success] [-d db-node]]\n" $(basename $0) >&2
}

waitDeploy() {
   local dbnode=$1
   local batchsize=$2
   while [ 1 ]
   do
  	    donecount=$(mysql -uroot -Dcloud -h$dbnode -s -r --skip-column-names -e"select count(*) from async_job where job_cmd like '%DeployVM%' and last_updated is null")
        echo "[DEBUG] " $(date) " " $donecount " VMs still deploying"
        if [[ $donecount == "0" || $donecount -eq 0 ]]
        then
           break
        fi
        sleep $(($donecount*2))s #2 seconds per VM
   done
}

hflag=
nflag=1
wflag=
bflag= 
dflag=
iterator=0

host="127.0.0.1" #defaults to locahost
numberofvms=1040 #defaults
numberofgroups= 
groups_per_vm=1 #default one group per vm
batchsize=100
waitSuccess=false

while getopts 'h:n:b:d:w' OPTION
do
	case $OPTION in
		h)	  hflag=1
			host="$OPTARG"
			;;
		n)    nflag=1
			numberofvms="$OPTARG"
			;;
		w)    wflag=1
			waitSuccess=true
			;;
		b)    bflag=1
			batchsize="$OPTARG"
			;;
		d)    dflag=1
			dbnode="$OPTARG"
			;;
		?)	usage
			exit 2
			;;
	esac
done

if [ $hflag$nflag != "11" ]
then
 usage
 exit 2
fi

if [[ $wflag == "1" && $dflag != "1" ]]
then
   echo "please specify dbnode -d option"
   usage
   exit 2
fi


if [[ $bflag == "1" && $wflag != "1" ]]
then
   echo "-w option mandatory when -b is given"
   usage
   exit 2
fi

tag1=$(($numberofvms*5/13))
tag2=$(($numberofvms*2/13))
tag3=$(($numberofvms*6/13))

tag1_so=9 #defaults from a regular installation
tag2_so=10
tag3_so=11
vmcount=0

echo -n "Service Offering ID with TAG1 hosttag: "
read tag1_so

echo -n "Service Offering ID with TAG2 hosttag: "
read tag2_so

echo -n "Service Offering ID with TAG3 hosttag: "
read tag3_so

echo "Deploying TAG1 VMs with Service Offering: " $tag1_so
for ((c=1;c<$tag1;c++))
do
    if [[ $vmcount -eq $batchsize && waitSuccess ]]
	then
		waitDeploy $dbnode $batchsize
		vmcount=0
	fi
	job_out=$(./deployVirtualMachine.sh -h $host -z 1 -t 2 -s $tag1_so -u)
	job_id=$(echo $job_out | sed 's/\(.*<jobid>\)\([0-9]*\)\(.*\)/\2/g') 
	vmid=$(echo $job_out | sed 's/\(.*<id>\)\([0-9]*\)\(.*\)/\2/g')
	echo "[DEBUG] $(date) deployed vm: " $vmid " in job: " $job_id		
    vmcount=$((vmcount+1))    
done

echo "Deploying TAG2 VMs with Service Offering: " $tag2_so
for ((c=1;c<$tag2;c++))
do
    if [[ $vmcount -eq $batchsize && waitSuccess ]]
	then
		waitDeploy $dbnode $batchsize
		vmcount=0
	fi
	job_out=$(./deployVirtualMachine.sh -h $host -z 1 -t 2 -s $tag2_so -u)
	job_id=$(echo $job_out | sed 's/\(.*<jobid>\)\([0-9]*\)\(.*\)/\2/g') 
	vmid=$(echo $job_out | sed 's/\(.*<id>\)\([0-9]*\)\(.*\)/\2/g')    
	echo "[DEBUG] $(date) deployed vm: " $vmid " in job: " $job_id	
    vmcount=$((vmcount+1))	
done


echo "Deploying TAG3 VMs with Service Offering: " $tag3_so
for ((c=1;c<$tag3;c++))
do
    if [[ $vmcount -eq $batchsize && waitSuccess ]]
	then
		waitDeploy $dbnode $batchsize
		vmcount=0
	fi
	job_out=$(./deployVirtualMachine.sh -h $host -z 1 -t 2 -s $tag3_so -u)
	job_id=$(echo $job_out | sed 's/\(.*<jobid>\)\([0-9]*\)\(.*\)/\2/g') 
	vmid=$(echo $job_out | sed 's/\(.*<id>\)\([0-9]*\)\(.*\)/\2/g')	
	echo "[DEBUG] $(date) deployed vm: " $vmid " in job: " $job_id		
    vmcount=$((vmcount+1))		    
done
