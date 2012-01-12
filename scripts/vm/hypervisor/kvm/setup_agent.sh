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



 

# Did cloud-agent installed
#set -x
install_cloud_agent() {
    local dev=$1
    local retry=10
    which cloud-setup-agent
    if [ $? -gt 0 ]
    then 
        # download  the repo
        which wget
        if [ $? -gt 0 ]
        then
            yum install wget -y
            if [ $? -gt 0 ]
            then
                printf "failed to install wget"
                exit 1
            fi 
        fi
        wget -N -P /etc/yum.repos.d/ http://download.cloud.com/foss/fedora/cloud.repo
        if [ $? -gt 0 ]
        then
            printf "Failed to download repo"
            exit 1
        fi
        if [ "$dev" == "1" ]
        then
            sed -i 's/\(baseurl\)\(.*\)/\1=http:\/\/yumrepo.lab.vmops.com\/repositories\/fedora\/vmdev\/oss\//'	/etc/yum.repos.d/cloud.repo 
        fi
        while [ "$retry" -gt "0" ]
        do
            yum clean all
            yum install cloud-agent -y
            if [ $? -gt 0 ]
            then
                let retry=retry-1
            else
                break
            fi
        done
    else
	    # is there update?
        while [ "$retry" -gt "0" ]
        do
	        yum clean all
	        yum update cloud-agent -y
            if [ $? -gt 0 ]
            then
                let retry=retry-1
            else
                break
            fi

        done
    fi
    
    if [ $? -gt 0 ]
    then
        printf "Failed to install agent"
        exit 2
    fi
}

install_cloud_consoleP() {
    local dev=$1
    local retry=10
    which cloud-setup-console-proxy
    if [ $? -gt 0 ]
    then 
        # download  the repo
        which wget
        if [ $? -gt 0 ]
        then
            yum install wget -y
            if [ $? -gt 0 ]
            then
                printf "failed to install wget"
                exit 1
            fi 
        fi
        wget -N -P=/etc/yum.repos.d/ http://download.cloud.com/foss/fedora/cloud.repo
        if [ $? -gt 0 ]
        then
            printf "Failed to download repo"
            exit 1
        fi
        if [ "$dev" == "1" ]
        then
            sed -i 's/\(baseurl\)\(.*\)/\1=http:\/\/yumrepo.lab.vmops.com\/repositories\/fedora\/vmdev\/oss\//'	/etc/yum.repos.d/cloud.repo 
        fi
        while [ "$retry" -gt "0" ]
        do
            yum clean all
            yum install cloud-console-proxy -y
            if [ $? -gt 0 ]
            then
                let retry=retry-1
            else
                break
            fi
        done
    else
	    # is there update?
        while [ "$retry" -gt "0" ]
        do
	        yum clean all
	        yum update cloud-console-proxy -y
            if [ $? -gt 0 ]
            then
                let retry=retry-1
            else
                break
            fi

        done
    fi
    
    if [ $? -gt 0 ]
    then
        printf "Failed to install console"
        exit 2
    fi
}
cloud_agent_setup() {
    local host=$1
    local zone=$2
    local pod=$3
    local cluster=$4
    local guid=$5
    # disable selinux
    selenabled=`cat /selinux/enforce`
    if [ "$selenabled" == "1" ]
    then
        sed -i  's/\(SELINUX\)\(.*\)/\1=permissive/' /etc/selinux/config
        setenforce 0
    fi
    cloud-setup-agent --host=$host --zone=$zone --pod=$pod --cluster=$cluster --guid=$guid -a > /dev/null
}

cloud_consoleP_setup() {
    local host=$1
    local zone=$2
    local pod=$3
    cloud-setup-console-proxy --host=$host --zone=$zone --pod=$pod -a > /dev/null
}

host=
zone=
pod=
cluster=
guid=
dflag=
pubNic=
prvNic=
while getopts 'h:z:p:u:c:P:N:d' OPTION
do
  case $OPTION in
  h) 
        host="$OPTARG"
        ;;
  z)    
        zone="$OPTARG"
        ;;
  p)    
        pod="$OPTARG"
        ;;
  c)    
        cluster="$OPTARG"
        ;;
  u)    
        guid="$OPTARG"
        ;;
  d)    
        dflag=1
        ;;
  P)    
	pubNic="$OPTARG"
        ;;
  N)    
	prvNic="$OPTARG"
	;;
  *)    ;;
  esac
done

#install_cloud_agent $dflag
#install_cloud_consoleP $dflag
paramters=
if [ -n "$pubNic" ]
then
   paramters=" --pubNic=$pubNic"
fi

if [ -n "$prvNic" ]
then
   paramters=" --prvNic=$prvNic $paramters"
fi

selenabled=`cat /selinux/enforce`
if [ "$selenabled" == "1" ]
then
    sed -i  's/\(SELINUX\)\(.*\)/\1=permissive/' /etc/selinux/config
    setenforce 0
fi

cloud-setup-agent --host=$host --zone=$zone --pod=$pod --cluster=$cluster --guid=$guid $paramters -a > /dev/null
#cloud_consoleP_setup $host $zone $pod
