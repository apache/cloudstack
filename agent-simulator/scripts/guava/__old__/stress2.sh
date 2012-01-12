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


# Setup Stress. Destroy and Deploy Virtual Machines on 
# Guava like environment
#Environment
#1. 1 host per cluster
#2. 100 clusters in a pod

# Create 300 Accounts
# Deploy 300 VMs
# Destroy 300 VMs
# Repeat


usage() {
  printf "Setup Stress\nUsage: %s: -h management-server -z zoneid [-d delay] [-t templateid] -s service-offering-id [-n number of accounts]\n" $(basename $0) >&2
}

create_account() {
    seq=$1
    account_query="GET	http://$host/client/?command=createAccount&accounttype=0&email=simulator%40simulator.com&username=$account_prefix$seq&firstname=first$seq&lastname=last$seq&password=5f4dcc3b5aa765d61d8327deb882cf99&account=$account_prefix$seq&domainid=1	HTTP/1.1\n\n"
    echo -e $account_query | nc -v -q 120 $host 8096
}

stress() {
    #Deploy 300 VMs in these accounts
    for ((i=1;i<=$numberofaccounts;i++))
    do
        out=$(./deployVirtualMachine.sh -h $host -z $zoneid -t $template -s $service -a $account_prefix$i)
        id=$(echo $out | sed 's/\(.*<id>\)\([0-9]*\)\(.*\)/\2/g')
        echo "deployed vm with id: " $id
        VmArray[$i]=$id
    done
    sleep $delay
    
    rindex=$(($RANDOM%$numberofaccounts))
    #Stop/Start 300 VMs at random
    for ((i=1;i<=$numberofaccounts;i++))
    do
	    ./stopVirtualMachine.sh -h $host -i ${VmArray:$rindex}
        rindex=$(($RANDOM%$numberofaccounts))
	    echo "stopped vm with id: " ${VmArray:$rindex}
	    
   	    ./startVirtualMachine.sh -h $host -i ${VmArray:$rindex}
        rindex=$(($RANDOM%$numberofaccounts))
	    echo "started vm with id: " ${VmArray:$rindex}
    done
    sleep $delay
}

#options
hflag=1
zflag=
dflag=1
tflag=1
sflag=
nflag=1


declare -a VmArray
host="127.0.0.1" #default localhost
zoneid=
delay=300 #default 5 minutes
template=2 #default centos
service=
account_prefix="USER"
numberofaccounts=300

while getopts 'h:z:d:t:s:n:' OPTION
do
 case $OPTION in
  h)	hflag=1
        host="$OPTARG"
        ;;
  z)    zflag=1
        zoneid="$OPTARG"
        ;;    
  d)    dflag=1
        delay="$OPTARG"
        ;;
  t)    tflag=1
        template="$OPTARG"
        ;;
  s)    sflag=1
        service="$OPTARG"
        ;;
  n)    nflag=1
        numberofaccounts="$OPTARG"
        ;; 
  ?)	usage
		exit 2
		;;
  esac
done

if [ $hflag$zflag$dflag$tflag$sflag$nflag != "111111" ]
then
 usage
 exit 2
fi

#Create 300 Accounts
#for ((i=1;i<=$numberofaccounts;i++))
#do
#    create_account $i
#done

for i in {1..5}
do
    #Do the stress test
    stress
done
