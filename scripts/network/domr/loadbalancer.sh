#!/usr/bin/env bash
# $Id: loadbalancer.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/network/domr/loadbalancer.sh $
# loadbalancer.sh -- reconfigure loadbalancer rules
#
#
# @VERSION@

usage() {
  printf "Usage: %s:  -i <domR eth1 ip>  -a <added public ip address> -d <removed> -f <load balancer config> -r <firewall IP> -u <firewall username> -y <firewall password> -z <firewall enable password> \n" $(basename $0) >&2
}


# set -x
cert="/root/.ssh/id_rsa.cloud"


# check if gateway domain is up and running
check_gw() {
  ping -c 1 -n -q $1 > /dev/null
  if [ $? -gt 0 ]
  then
    sleep 1
    ping -c 1 -n -q $1 > /dev/null
  fi
  return $?;
}


#firewall entry to ensure that haproxy can receive on specified port
fw_entry() {
  local domRIp=$1
  local added=$2
  local removed=$3
  
  if [ "$added" == "none" ]
  then
  	added=""
  fi
  
  if [ "$removed" == "none" ]
  then
  	removed=""
  fi
  
  local a=$(echo $added | cut -d, -f1- --output-delimiter=" ")
  local r=$(echo $removed | cut -d, -f1- --output-delimiter=" ")
  
  for i in $a
  do
    local pubIp=$(echo $i | cut -d: -f1)
    local dport=$(echo $i | cut -d: -f2)
    local vlanNetmask=$(echo $i | cut -d: -f3)
    
    if [ "$domRIp" != "none" ]
    then
    	ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp "\
      	iptables -D INPUT -i eth2 -p tcp -d $pubIp --dport $dport -j ACCEPT 2> /dev/null
     	"
     	
     	ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp "\
      	iptables -A INPUT -i eth2 -p tcp -d $pubIp --dport $dport -j ACCEPT
     	"
     	
     	if [ $? -gt 0 ]
     	then
     		exit 1
     	fi
    fi
    
  done

 for i in $r
 do
    local pubIp=$(echo $i | cut -d: -f1)
    local dport=$(echo $i | cut -d: -f2)
    local vlanNetmask=$(echo $i | cut -d: -f3)
    
    if [ "$domRIp" != "none" ]
    then
    	ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp "\
     	iptables -D INPUT -i eth2 -p tcp -d $pubIp --dport $dport -j ACCEPT
     	"
    fi
  	
  done
  
  return 0
}


#Hot reconfigure HA Proxy in the routing domain
reconfig_lb() {
  local domRIp=$1
  local cfg=$2

  scp -P 3922 -q -o StrictHostKeyChecking=no -i $cert $cfg root@$domRIp:/etc/haproxy/haproxy.cfg.new

  if [ $? -eq 0 ]
  then
     ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp /root/reconfigLB.sh
  fi

  return $?
}

# Restore the HA Proxy to its previous state, and revert iptables rules on DomR
restore_lb() {
	local domRIp=$1

	# Copy the old version of haproxy.cfg into the file that reconfigLB.sh uses
	ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp "\
     	cp /etc/haproxy/haproxy.cfg.old /etc/haproxy/haproxy.cfg.new
    "
    
    # Run reconfigLB.sh again
    if [ $? -eq 0 ]
  	then
    	ssh -p 3922 -q -o StrictHostKeyChecking=no -i $cert root@$domRIp /root/reconfigLB.sh
  	fi
}


mflag=
iflag=
aflag=
dflag=
fflag=
op=""
addedIps=""
removedIps=""

while getopts 'i:a:d:f:' OPTION
do
  case $OPTION in
  i)	iflag=1
		domRIp="$OPTARG"
		;;
  a)	aflag=1
		addedIps="$OPTARG"
		;;
  d)	dflag=1
		removedIps="$OPTARG"
		;;
  f)	fflag=1
		cfgfile="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

# At this point, $domRIp is guaranteed to be either an IP address (if the DomR is running in the Management Server database), or "none"

# If a DomR IP was passed in, check if DomR is up and running. If it isn't, exit 1.
if [ "$domRIp" != "none" ]
then
	check_gw "$domRIp"
	if [ $? -gt 0 ]
	then
   		exit 1
	fi
fi

# If a DomR IP was passed in, reconfigure the HA Proxy.
if [ "$domRIp" != "none" ]
then
	if [ "$iflag$fflag" != "11" ]
	then
 		usage
 		exit 2
	fi

	#hot reconfigure haproxy
	reconfig_lb $domRIp $cfgfile
	
	if [ $? -gt 0 ]
	then
		printf "Reconfiguring loadbalancer failed\n"
		exit 1
	fi

fi

if [ "$addedIps" == "" ]
then
	addedIps="none"
fi

if [ "$removedIps" == "" ]
then
	removedIps="none"
fi

# iptables entry to ensure that haproxy receives traffic
fw_entry $domRIp $addedIps $removedIps
  	
if [ $? -gt 0 ]
then
	if [ "$domRIp" != "none" ]
  	then
  		# Restore the LB
  		restore_lb $domRIp
  		# Revert iptables rules on DomR, with addedIps and removedIps swapped (don't pass in firewall related values, since we only want to revert DomR)
  		fw_entry $domRIp $removedIps $addedIps
  	fi
  	
  	exit 1
fi
 
exit 0
  	

