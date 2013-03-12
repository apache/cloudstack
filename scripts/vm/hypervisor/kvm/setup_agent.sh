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


 

# Did cloudstack-agent installed
#set -x
install_cloud_agent() {
    local dev=$1
    local retry=10
    which cloudstack-setup-agent
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
            yum install cloudstack-agent -y
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
	        yum update cloudstack-agent -y
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
    cloudstack-setup-agent --host=$host --zone=$zone --pod=$pod --cluster=$cluster --guid=$guid -a > /dev/null
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

cloudstack-setup-agent --host=$host --zone=$zone --pod=$pod --cluster=$cluster --guid=$guid $paramters -a > /dev/null
#cloud_consoleP_setup $host $zone $pod
