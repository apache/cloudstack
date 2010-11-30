#!/usr/bin/env bash
# $Id: firewall.sh 9947 2010-06-25 19:34:24Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/patches/xenserver/root/firewall.sh $
# firewall.sh -- allow some ports / protocols to vm instances
#
#
# @VERSION@

usage() {
  printf "Usage: %s: (-A|-D) -i <domR eth1 ip>  -r <target-instance-ip> -P protocol (-p port_range | -t icmp_type_code)  -l <public ip address> -d <target port> [-f <firewall ip> -u <firewall user> -y <firewall password> -z <firewall enable password> ] \n" $(basename $0) >&2
}

# set -x

get_dom0_ip () {
 eval "$1=$(ifconfig eth0 | awk '/inet addr/ {split ($2,A,":"); print A[2]}')"
 return 0
}


#Add the tcp firewall entries into iptables in the routing domain
tcp_entry() {
  local instIp=$1
  local dport=$2
  local pubIp=$3
  local port=$4
  local op=$5
  
  for vif in $VIF_LIST; do 
    iptables -t nat $op PREROUTING --proto tcp -i $vif -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
  done;
    	
  iptables -t nat $op OUTPUT  --proto tcp -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
  iptables $op FORWARD -p tcp -s 0/0 -d $instIp -m state --state ESTABLISHED,RELATED -j ACCEPT > /dev/null;
  iptables $op FORWARD -p tcp -s 0/0 -d $instIp --destination-port $dport --syn -j ACCEPT > /dev/null;
  	
  return $?
}

#Add the udp firewall entries into iptables in the routing domain
udp_entry() {
  local instIp=$1
  local dport=$2
  local pubIp=$3
  local port=$4
  local op=$5
  
  for vif in $VIF_LIST; do 
    iptables -t nat $op PREROUTING --proto udp -i $vif -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
  done;
   	
  iptables -t nat $op OUTPUT  --proto udp -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
  iptables $op FORWARD -p udp -s 0/0 -d $instIp --destination-port $dport  -j ACCEPT > /dev/null;
  		
  return $?
}

#Add the icmp firewall entries into iptables in the routing domain
icmp_entry() {
  local instIp=$1
  local icmptype=$2
  local pubIp=$3
  local op=$4
  
  for vif in $VIF_LIST; do 
    iptables -t nat $op PREROUTING --proto icmp -i $vif -d $pubIp --icmp-type $icmptype -j DNAT --to-destination $instIp >/dev/null;
  done;
   	
  iptables -t nat $op OUTPUT  --proto icmp -d $pubIp --icmp-type $icmptype -j DNAT --to-destination $instIp:$dport >/dev/null;
  iptables $op FORWARD -p icmp -s 0/0 -d $instIp --icmp-type $icmptype  -j ACCEPT > /dev/null;
  	
  return $?
}

#Add 1:1 NAT entry
add_one_to_one_nat_entry() {
  local guestIp=$1
  local publicIp=$2  
  local dIp=$3
  local op=$4 
  if [ "$op" == "-D" ]
  then
  	iptables -t nat $op PREROUTING -i eth2 -d $publicIp -j DNAT --to-destination $guestIp
  	if [ $? -gt 0 ]
  	then
  	  return 0
  	fi
  else
  	iptables -t nat $op PREROUTING -i eth2 -d $publicIp -j DNAT --to-destination $guestIp
  fi
  iptables -t nat $op POSTROUTING -o eth2 -s $guestIp -j SNAT --to-source $publicIp
  if [ "$op" == "-A" ]
  then
    iptables -P FORWARD DROP
  fi
  iptables $op FORWARD -m state --state RELATED,ESTABLISHED -j ACCEPT
  iptables $op FORWARD -i eth2 -o eth0 -d $guestIp -m state --state NEW -j ACCEPT
  iptables $op FORWARD -i eth0 -o eth2 -s $guestIp -m state --state NEW -j ACCEPT
  
  return $?
}

get_vif_list() {
  local vif_list=""
  for i in /sys/class/net/eth*; do 
    vif=$(basename $i);
    if [ "$vif" != "eth0" ] && [ "$vif" != "eth1" ]
    then
      vif_list="$vif_list $vif";
    fi
  done
  
  echo $vif_list
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
Gflag=
op=""
oldPrivateIP=""
oldPrivatePort=""

while getopts 'ADr:i:P:p:t:l:d:w:x:n:N:G:' OPTION
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
  G)	Gflag=1
		nat="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

#1:1 NAT
if [ "$Gflag" == "1" ]
then
  add_one_to_one_nat_entry $instanceIp $publicIp $domRIp $op
  exit $?
fi

reverseOp=$(reverse_op $op)

VIF_LIST=$(get_vif_list)

case $protocol  in
  "tcp")	
  		# If oldPrivateIP was passed in, this is an update. Delete the old rule from DomR. 
  		if [ "$oldPrivateIP" != "" ]
  		then
  			tcp_entry $oldPrivateIP $oldPrivatePort $publicIp $ports "-D"
  		fi
  		
  		# Add/delete the new rule
		tcp_entry $instanceIp $dport $publicIp $ports $op 
		exit $?
		;;
  "udp")  
  		# If oldPrivateIP was passed in, this is an update. Delete the old rule from DomR. 
  		if [ "$oldPrivateIP" != "" ]
  		then
  			udp_entry $oldPrivateIP $oldPrivatePort $publicIp $ports "-D"
		fi
  
		# Add/delete the new rule
		udp_entry $instanceIp $dport $publicIp $ports $op 
		exit $?
        ;;
  "icmp")  
  		# If oldPrivateIP was passed in, this is an update. Delete the old rule from DomR. 
  		if [ "$oldPrivateIP" != "" ]
  		then
  			icmp_entry $oldPrivateIp $icmptype $publicIp "-D"
  		fi
  
  		# Add/delete the new rule
		icmp_entry $instanceIp $icmptype $publicIp $op 
		exit $?
        ;;
      *)
        printf "Invalid protocol-- must be tcp, udp or icmp\n" >&2
        exit 5
        ;;
esac
