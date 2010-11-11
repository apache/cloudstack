#!/usr/bin/env bash
# $Id: firewall.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/network/domr/firewall.sh $
# firewall.sh -- allow some ports / protocols to vm instances
#
#
# @VERSION@

usage() {
  printf "Usage: %s: (-A|-D) -i <domR eth1 ip>  -r <target-instance-ip> -P protocol (-p port_range | -t icmp_type_code)  -l <public ip address> -d <target port> [-f <firewall ip> -u <firewall user> -y <firewall password> -z <firewall enable password> ] \n" $(basename $0) >&2
}

cert="/root/.ssh/id_rsa.cloud"

# set -x

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

get_dom0_ip () {
 eval "$1=$(ifconfig eth0 | awk '/inet addr/ {split ($2,A,":"); print A[2]}')"
 return 0
}


#Add the tcp firewall entries into iptables in the routing domain
tcp_entry() {
  local dRIp=$1
  local instIp=$2
  local dport=$3
  local pubIp=$4
  local port=$5
  local op=$6
  
  ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
    	iptables -t nat $op PREROUTING --proto tcp -i eth2 -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
    	iptables -t nat $op OUTPUT  --proto tcp -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
    	iptables $op FORWARD -p tcp -s 0/0 -d $instIp -m state --state ESTABLISHED,RELATED -j ACCEPT > /dev/null;
    	iptables $op FORWARD -p tcp -s 0/0 -d $instIp --destination-port $dport --syn -j ACCEPT > /dev/null;
  "
  	
  return $?

}

#Add the udp firewall entries into iptables in the routing domain
udp_entry() {
  local dRIp=$1
  local instIp=$2
  local dport=$3
  local pubIp=$4
  local port=$5
  local op=$6
  
  ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
    	iptables -t nat $op PREROUTING --proto udp -i eth2 -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
    	iptables -t nat $op OUTPUT  --proto udp -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
    	iptables $op FORWARD -p udp -s 0/0 -d $instIp --destination-port $dport  -j ACCEPT > /dev/null;
  "
  		
  return $?

}

#Add the icmp firewall entries into iptables in the routing domain
icmp_entry() {
  local dRIp=$1
  local instIp=$2
  local icmptype=$3
  local pubIp=$4
  local op=$5
  
  ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
    	iptables -t nat $op PREROUTING --proto icmp -i eth2 -d $pubIp --icmp-type $icmptype -j DNAT --to-destination $instIp >/dev/null;
    	iptables -t nat $op OUTPUT  --proto icmp -d $pubIp --icmp-type $icmptype -j DNAT --to-destination $instIp:$dport >/dev/null;
    	iptables $op FORWARD -p icmp -s 0/0 -d $instIp --icmp-type $icmptype  -j ACCEPT > /dev/null;
  "
  	
  return $?
  		
}

reverse_op() {
	local op=$1
	
	if [ "$op" == "-A" ]
	then
		echo "-D"
	else
		echo "-A"
	fi
}

rflag=
iflag=
Pflag=
pflag=
tflag=
lflag=
dflag=
oflag=
wflag=
xflag=
nflag=
Nflag=
op=""
oldPrivateIP=""
oldPrivatePort=""

while getopts 'ADr:i:P:p:t:l:d:w:x:n:N:' OPTION
do
  case $OPTION in
  A)	Aflag=1
		op="-A"
		;;
  D)	Dflag=1
		op="-D"
		;;
  i)	iflag=1
		domRIp="$OPTARG"
		;;
  r)	rflag=1
		instanceIp="$OPTARG"
		;;
  P)	Pflag=1
		protocol="$OPTARG"
		;;
  p)	pflag=1
		ports="$OPTARG"
		;;
  t)	tflag=1
		icmptype="$OPTARG"
		;;
  l)	lflag=1
		publicIp="$OPTARG"
		;;
  d)	dflag=1
		dport="$OPTARG"
		;;
  w)	wflag=1
  		oldPrivateIP="$OPTARG"
  		;;
  x)	xflag=1
  		oldPrivatePort="$OPTARG"
  		;;	
  n)	nflag=1
  		domRName="$OPTARG"
  		;;
  N)	Nflag=1
  		netmask="$OPTARG"
  		;;
  ?)	usage
		exit 2
		;;
  esac
done

# domRIp is guaranteed to be present at this point

# Check if DomR is up and running. If not, exit with error code 1.
check_gw "$domRIp"
if [ $? -gt 0 ]
then
	exit 1
fi

#Either the A flag or the D flag but not both
if [ "$Aflag$Dflag" != "1" ]
then
 usage
 exit 2
fi

#Either the tflag or the p flag but not both
if [ "$rflag$iflag$Pflag$pflag$tflag$lflag" != "11111" ]
then
 usage
 exit 2
fi

#Require -d with -p
if [ "$pflag$dflag" != 11 -a "$pflag$dflag" != "" ]
then
 usage
 exit 2
fi

reverseOp=$(reverse_op $op)

case $protocol  in
  "tcp")	
  		# If oldPrivateIP was passed in, this is an update. Delete the old rule from DomR. 
  		if [ "$oldPrivateIP" != "" ]
  		then
  			tcp_entry $domRIp $oldPrivateIP $oldPrivatePort $publicIp $ports "-D"
  		fi
  		
  		# Add/delete the new rule
		tcp_entry $domRIp $instanceIp $dport $publicIp $ports $op 
		;;
  "udp")  
  		# If oldPrivateIP was passed in, this is an update. Delete the old rule from DomR. 
  		if [ "$oldPrivateIP" != "" ]
  		then
  			udp_entry $domRIp $oldPrivateIP $oldPrivatePort $publicIp $ports "-D"
		fi
  
		# Add/delete the new rule
		udp_entry $domRIp $instanceIp $dport $publicIp $ports $op 
        ;;
  "icmp")  
  		# If oldPrivateIP was passed in, this is an update. Delete the old rule from DomR. 
  		if [ "$oldPrivateIP" != "" ]
  		then
  			icmp_entry $domRIp $oldPrivateIp $icmptype $publicIp "-D"
  		fi
  
  		# Add/delete the new rule
		icmp_entry $domRIp $instanceIp $icmptype $publicIp $op 
        ;;
      *)
        printf "Invalid protocol-- must be tcp, udp or icmp\n" >&2
        exit 5
        ;;
esac

exit 0