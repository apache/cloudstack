#! /bin/bash
# Did cloud-agent installed
set -x
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
    # disable selinux
    selenabled=`cat /selinux/enforce`
    if [ "$selenabled" == "1" ]
    then
        sed -i  's/\(SELINUX\)\(.*\)/\1=permissive/' /etc/selinux/config
        setenforce 0
    fi
    cloud-setup-agent --host=$host --zone=$zone --pod=$pod -a
}

cloud_consoleP_setup() {
    local host=$1
    local zone=$2
    local pod=$3
    cloud-setup-console-proxy --host=$host --zone=$zone --pod=$pod 
}

host=
zone=
pod=
dflag=
while getopts 'h:z:p:d' OPTION
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
  d)    
        dflag=1
        ;;
  *)    ;;
  esac
done

install_cloud_agent $dflag
install_cloud_consoleP $dflag
cloud_agent_setup $host $zone $pod
cloud_consoleP_setup $host $zone $pod
