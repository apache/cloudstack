#!/usr/bin/env bash
# $Id: firewall_vlan.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/network/domr/firewall_vlan.sh $
# firewall.sh -- allow some ports / protocols to vm instances
#
#
# @VERSION@

usage() {
  printf "Usage: %s: (-A|-D) -i <domR eth1 ip>  -r <target-instance-ip> -P protocol (-p port_range | -t icmp_type_code)  -l <public ip address> -d <target port> [-f <firewall ip> -u <firewall user> -y <firewall password> -z <firewall enable password> -n <domr name> -N <VLAN netmask> ] \n" $(basename $0) >&2
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

get_dom0_ip () {
 eval "$1=$(ifconfig eth0 | awk '/inet addr/ {split ($2,A,":"); print A[2]}')"
 return 0
}

get_value() {
  local filename=$1
  local keyname=$2
  grep -i $keyname= $filename | cut -d= -f2
}

get_subnet() {
	local ip=$1
	local netmask=$2

	local ip1=($(echo $ip | awk -F"." '{print $1,$2,$3,$4}'))
	local netmask1=($(echo $netmask | awk -F"." '{print $1,$2,$3,$4}'))
	local subnet=$((${ip1[0]} & ${netmask1[0]})).$((${ip1[1]} & ${netmask1[1]})).$((${ip1[2]} & ${netmask1[2]})).$((${ip1[3]} & ${netmask1[3]}))

	echo $subnet
}

get_vif_list() {
        local domRIp=$1

        local command=" vifListDomR=\"\"; \
                                        for i in /sys/class/net/eth*; do \
                                                vif=\$(basename \$i); \
                                                vifIp=\$(grep -i IPADDR= /etc/sysconfig/network-scripts/ifcfg-\$vif | cut -d= -f2); \
                                                vifNetmask=\$(grep -i NETMASK= /etc/sysconfig/network-scripts/ifcfg-\$vif | cut -d= -f2); \
                                                vifListDomR=\$vifListDomR\" \$vif:\$vifIp:\$vifNetmask\"; \
                                        done; \
                                        echo \$vifListDomR;"

        local vifList=$(ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domRIp $command)

        echo $vifList
}

find_correct_vif() {
	local domRIp=$1
	local publicIp=$2
	local vlanNetmask=$3
	
	local correctVif="none"
	
	local vlanSubnet=$(get_subnet $publicIp $vlanNetmask)
	local vifList=$(get_vif_list $domRIp)

	for i in $vifList
	do
		local vif=$(echo $i | cut -d: -f1)
		local vifIp=$(echo $i | cut -d: -f2)
		local vifNetmask=$(echo $i | cut -d: -f3)
		local vifSubnet=$(get_subnet $vifIp $vifNetmask)
		
		if [ "$vlanSubnet" == "$vifSubnet" ]
		then
			correctVif="$vif"
			break
		fi
	done
	
	echo $correctVif
}

#Add the tcp firewall entries into iptables in the routing domain
tcp_entry() {
  local dRIp=$1
  local instIp=$2
  local dport=$3
  local pubIp=$4
  local port=$5
  local op=$6
  local vif=$7
  
  ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
    	iptables -t nat $op PREROUTING --proto tcp -i $vif -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
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
  local vif=$7
  
  ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
    	iptables -t nat $op PREROUTING --proto udp -i $vif -d $pubIp --destination-port $port -j DNAT --to-destination $instIp:$dport >/dev/null;
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
  local vif=$6
  
  ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
    	iptables -t nat $op PREROUTING --proto icmp -i $vif -d $pubIp --icmp-type $icmptype -j DNAT --to-destination $instIp >/dev/null;
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

# Router name must be passed in
if [ "$nflag" != "1" ]
then
	usage
	exit 2
fi

# The netmask of the public IP's VLAN must be passed in
if [ "$Nflag" != "1" ]
then
	usage
	exit 2
fi

reverseOp=$(reverse_op $op)

# Find the VIF that we need to use on DomR
correctVif=$(find_correct_vif $domRIp $publicIp $netmask)

case $protocol  in
  "tcp")	
  		# If oldPrivateIP was passed in, this is an update. Delete the old rule from DomR. 
  		if [ "$oldPrivateIP" != "" ]
  		then
  			tcp_entry $domRIp $oldPrivateIP $oldPrivatePort $publicIp $ports "-D" $correctVif
  		fi
  		
  		# Add/delete the new rule
		tcp_entry $domRIp $instanceIp $dport $publicIp $ports $op $correctVif
		;;
  "udp")  
  		# If oldPrivateIP was passed in, this is an update. Delete the old rule from DomR. 
  		if [ "$oldPrivateIP" != "" ]
  		then
  			udp_entry $domRIp $oldPrivateIP $oldPrivatePort $publicIp $ports "-D" $correctVif
		fi
  
		# Add/delete the new rule
		udp_entry $domRIp $instanceIp $dport $publicIp $ports $op $correctVif
        ;;
  "icmp")  
  		# If oldPrivateIP was passed in, this is an update. Delete the old rule from DomR. 
  		if [ "$oldPrivateIP" != "" ]
  		then
  			icmp_entry $domRIp $oldPrivateIp $icmptype $publicIp "-D" $correctVif
  		fi
  
  		# Add/delete the new rule
		icmp_entry $domRIp $instanceIp $icmptype $publicIp $op $correctVif
        ;;
      *)
        printf "Invalid protocol-- must be tcp, udp or icmp\n" >&2
        exit 5
        ;;
esac

exit 0
