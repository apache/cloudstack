#!/usr/bin/env bash
# $Id: ipassoc_vlan.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/network/domr/ipassoc_vlan.sh $
# ipassoc.sh -- associate/disassociate a public ip with an instance
#
#
# @VERSION@

# set -x
usage() {
  printf "Usage:\n %s -A  -i <domR eth1 ip>  -l <public-ip-address>  -r <domr name> [-f] \n" $(basename $0) >&2
  printf " %s -D -i <domR eth1 ip> -l <public-ip-address> -r <domr name> [-f] \n" $(basename $0) >&2
  printf "If associating first IP in vlan or disassociating last IP in vlan, the following are required: -v <vlan id> -g <vlan gateway> -n <vlan netmask> \n" $(basename $0) >&2
}

cert="/root/.ssh/id_rsa.cloud"

get_value() {
  local filename=$1
  local keyname=$2
  grep -i $keyname= $filename | cut -d= -f2
}

#verify if supplied ip is indeed in the public domain
check_public_ip() {
 if [[ $(expr match $1 "10.") -gt 0 ]] 
  then
    echo "Public IP ($1) cannot be a private IP address!\n"
    exit 1
  fi
}

#ensure that dom0 is set up to do routing and proxy arp
check_ip_fw () {
  if [ $(cat /proc/sys/net/ipv4/ip_forward) != 1 ];
  then
    printf "Warning. Dom0 not set up to do forwarding.\n" >&2
    printf "Executing: echo 1 > /proc/sys/net/ipv4/ip_forward\n" >&2
    printf "To make this permanent, set net.ipv4.ip_forward = 1 in /etc/sysctl.conf\n" >&2
    echo 1 > /proc/sys/net/ipv4/ip_forward
  fi
  #if [ $(cat /proc/sys/net/ipv4/conf/eth0/proxy_arp) != 1 ];
  #then
    #printf "Warning. Dom0 not set up to do proxy ARP.\n"
    #printf "Executing: echo 1 > /proc/sys/net/ipv4/conf/eth0/proxy_arp\n"
    #printf "To make this permanent, set net.ipv4.conf.eth0.proxy_arp = 1 in /etc/sysctl.conf\n"
    #echo 1 > /proc/sys/net/ipv4/conf/eth0/proxy_arp
  #fi
}


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

#Add the NAT entries into iptables in the routing domain
add_nat_entry() {
  local dRIp=$1
  local pubIp=$2
   ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
      ip addr add dev eth2 $pubIp
      iptables -t nat -I POSTROUTING   -j SNAT -o eth2 --to-source $pubIp ;
      /sbin/arping -c 3 -I eth2 -A -U -s $pubIp $pubIp;
     "
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     return 1
  fi

  return 0
}

#remove the NAT entries into iptables in the routing domain
del_nat_entry() {
  local dRIp=$1
  local pubIp=$2
   ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
      iptables -t nat -D POSTROUTING   -j SNAT -o eth2 --to-source $pubIp;
      ip addr del dev eth2 $pubIp/32
     "
 
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     return 1
  fi

  return $?
}

_vifname() {
 local vmname=$1
 local domid=$(xm domid $vmname)
 echo vif${domid}
}

add_acct_rule() {
  local vmname=$1
  local intf=$2
  local dstip=$3

  local vifname=$(_vifname $vmname)

  iptables -A FORWARD -m physdev  --physdev-out $vifname.$intf -d $dstip -j ACCEPT

  return $?
}

remove_acct_rule() {
  local vmname=$1
  local intf=$2
  local dstip=$3

  local vifname=$(_vifname $vmname)

  iptables -D FORWARD -m physdev  --physdev-out $vifname.$intf -d $dstip -j ACCEPT

  return $?
}

add_first_ip() {
  local domRname=$1
  local domRIp=$2
  local publicIp=$3
  local eth2mac=$4
  local vifname=""
  local domid=0

  if  ! xm list $domRname  &>/dev/null
  then
    printf "Error: routing domain $domRname does not exist\n" >&2
    exit 2
  fi


  #check_public_ip "$publicIp"

    
  # Ensure that dom0 is set up to do routing
  #check_ip_fw
   
  
  #program ip tables in domR and route in dom0
  if  ! add_nat_entry $domRIp $publicIp 
  then
     printf  "Unable add nat entry on gateway, exiting\n" >&2
     return 4
  fi

  return 0
}

check_if_ip_assigned_to_vif() {
		local domRIp=$1
        local vif=$2
        local ip=$3

        local vifIp=$(ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domRIp "\
                                text=\$(ifconfig $vif | grep \"inet addr\" | cut -d: -f2); \
                                ip="none"; \
                                for i in \$text; do ip=\$i; break; done; \
                                echo \$ip;")

        if [ "$ip" == "$vifIp" ]
        then
            	return 0
        else
            	return 1
        fi
}


add_an_ip () {
  local dRIp=$1
  local pubIp=$2
  local vif=$3
  local vflag=$4
  local vlanNetmask=$5
  
   ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
      ip addr add dev $vif $pubIp; \
      if [ "$vflag" == "1" ]; then ifconfig $vif netmask $vlanNetmask up; fi; \
      /sbin/arping -c 3 -I $vif -A -U -s $pubIp $pubIp; \
     "
   return $?
}

number_of_bits() {
        local decNum=$1

        local bits=0
        while [ $decNum -gt 0 ]
        do
          	local lastBit=$(expr $decNum % 2)
                if [ "$lastBit" == "1" ]
                then
                    	let bits+=1
                fi
                let decNum=$decNum/2
        done

	echo $bits
}

netmask_to_cidr() {
        local netmask=$1

        local cidr=0
        local IFS=.
        for octet in $netmask
        do
                local octetBits=$(number_of_bits $octet)
                let cidr+=$octetBits
        done

        echo $cidr
}


remove_an_ip () {
  local dRIp=$1
  local pubIp=$2
  local vif=$3
  local vlanNetmask=$4
  
  # Check if the public IP is assigned to the VIF itself
  check_if_ip_assigned_to_vif $dRIp $vif $pubIp
  
  if [ $? -eq 0 ]
  then
	# Convert the VLAN netmask to a CIDR
	local cidr=$(netmask_to_cidr $vlanNetmask)
	
	# Delete the IP address by providing the correct CIDR (not 32 in this case)
	# Then, set the correct netmask on the VIF, since at this point it will have a netmask of 255.255.255.255
	# Finally, detect what the new IP address on the VIF is, and correct the ifcfg-VIF file to reflect this
	
	ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
		ip addr del dev $vif $pubIp/$cidr; \
		ifconfig $vif netmask $vlanNetmask up; \
		text=\$(ifconfig $vif | grep \"inet addr\" | cut -d: -f2); \
		ip="none"; \
		for i in \$text; do ip=\$i; break; done; \
		sed -i /IPADDR/d /etc/sysconfig/network-scripts/ifcfg-$vif; \
		echo "IPADDR=\$ip" >> /etc/sysconfig/network-scripts/ifcfg-$vif; \
	"
  else
	ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
		ip addr del dev $vif $pubIp/32
	"
  fi
  
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     return 1
  fi
}

attach_bridge_to_domr() {
	local domRName=$1
	local domRIp=$2
	local vlanId=$3
	local vlanGateway=$4
	local vlanNetmask=$5
	local publicIp=$6
	
	local bridgeName=xenbr1.$vlanId
	
	xm network-attach $domRName bridge=$bridgeName 
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	# Figure out what VIF we just added! It will be the only VIF that does not have an IP address assigned in ifcfg-eth*, since we haven't created any ifcfg-eth* file yet.
	local newVif="none"
	local vifList=$(get_vif_list $domRIp)
	
	for i in $vifList
	do
		local vif=$(echo $i | cut -d: -f1)
		local vifIp=$(echo $i | cut -d: -f2)
		
		if [ "$vifIp" == "" ]
		then
			newVif="$vif"
			break
		fi
	done
	
	if [ "$newVif" == "none" ]
	then
		echo "Could not detect newly added VIF"
		exit 1
	fi
	
	local ifcfgFilePath="/etc/sysconfig/network-scripts/ifcfg-$newVif"
	
	# Via SSH: Create ifcfg-eth* file and add vlanGateway to /etc/sysconfig/network
	ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domRIp "\
		touch $ifcfgFilePath; \
		echo "DEVICE=$newVif" >> $ifcfgFilePath; \
		echo "ONBOOT=yes" >> $ifcfgFilePath; \
		echo "TYPE=Ethernet" >> $ifcfgFilePath; \
		echo "IPADDR=$publicIp" >> $ifcfgFilePath; \
		echo "NETMASK=$vlanNetmask" >> $ifcfgFilePath; \
		echo "GATEWAY=$vlanGateway" >> /etc/sysconfig/network; \
	"
	
	if [ $? -gt 0 ]
	then
		echo "Could not create ifcfg-eth* file for newly added VIF"
		exit 1
	fi
	
	return $?
}

get_vif_id() {
	local domRIp=$1
	local domRName=$2
	local vifName=$3

	# First get the MAC address of the VIF from DomR
	local command="\
		macAddrOfVif=\"none\"; \
		for i in \$(ifconfig $vifName | grep HWaddr); do macAddrOfVif=\$i; done; \
		echo \$macAddrOfVif;"

	local macAddr=$(ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domRIp "$command")

	if [ "$macAddr" == "none" ]
	then
		echo "none"
		return
	fi

	# Do xm network-list and find the vif ID that corresponds to the MAC address
	local vifId="none"
	local networkList=$(xm network-list $domRName | grep -i "$macAddr")
	for i in $networkList; do vifId=$i; break; done;

	echo $vifId
}

detach_bridge_from_domr() {
	local domRName=$1
	local domRIp=$2
	local vlanGateway=$3
	local correctVif=$4

	local vifId=$(get_vif_id $domRIp $domRName $correctVif)
	
	xm network-detach $domRName $vifId
	
	if [ $? -gt 0 ]
	then
		return 1
	fi
	
	# Remove ifcfg-eth* file and vlanGateway from /etc/sysconfig/network via SSH
	ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$domRIp "\
		rm /etc/sysconfig/network-scripts/ifcfg-$correctVif;
		sed -i /"GATEWAY=$vlanGateway"/d /etc/sysconfig/network;
	"
	
	return $?
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

#set -x

fflag=
Aflag=
Dflag=
rflag=
iflag=
aflag=
lflag=
vflag=0
gflag=
nflag=
op=""

while getopts 'fADr:i:a:l:v:g:n:' OPTION
do
  case $OPTION in
  A)	Aflag=1
		op="-A"
		;;
  D)	Dflag=1
		op="-D"
		;;
  f)	fflag=1
		;;
  r)	rflag=1
		domRname="$OPTARG"
		;;
  i)	iflag=1
		domRIp="$OPTARG"
		;;
  l)	lflag=1
		publicIp="$OPTARG"
		;;
  a)	aflag=1
		eth2mac="$OPTARG"
		;;
  v)	vflag=1
  		vlanId="$OPTARG"
  		;;
  g)	gflag=1	
  		gateway="$OPTARG"
  		;;
  n)	nflag=1
  		netmask="$OPTARG"
  		;;
  ?)	usage
		exit 2
		;;
  esac
done

#Either the A flag or the D flag but not both
if [ "$Aflag$Dflag" != "1" ]
then
 usage
 exit 2
fi

if [ "$Aflag$lflag$iflag$rflag$nflag" != "11111" ] && [ "$Dflag$lflag$iflag$rflag$nflag" != "11111" ]
then
   exit 2
fi

# If a vlanId is passed in, then a vlanGateway must be passed in
if [ "$vflag" == "1" ] && [ "$gflag" != "1" ]
then
	usage
	exit 2
fi

# check if gateway domain is up and running
if ! check_gw "$domRIp"
then
   printf "Unable to ping the routing domain, exiting\n" >&2
   exit 3
fi

# If this is an add and a vlanId was passed in, hotplug a new vif on DomR
if [ "$Aflag" == "1" ] && [ "$vflag" == "1" ]
then
	attach_bridge_to_domr $domRname $domRIp $vlanId $gateway $netmask $publicIp
  	
	if [ $? -gt 0 ]
	then
		exit 1
	fi
fi

# Find the VIF that we need to use on DomR
correctVif=$(find_correct_vif $domRIp $publicIp $netmask)

if [ "$fflag" == "1" ] && [ "$Aflag" == "1" ]
then
  add_nat_entry $domRIp $publicIp 
  exit $?
fi

if [ "$Aflag" == "1" ]
then  
  add_an_ip $domRIp $publicIp $correctVif $vflag $netmask
  exit $?
fi

if [ "$fflag" == "1" ] && [ "$Dflag" == "1" ]
then
  del_nat_entry $domRIp $publicIp 
  exit $?
fi

if [ "$Dflag" == "1" ]
then
  remove_an_ip $domRIp $publicIp $correctVif $netmask
  
  # If a vlanId was passed in, remove the vlan's vif from DomR
  if [ "$vflag" == "1" ]
  then
  	detach_bridge_from_domr $domRname $domRIp $gateway $correctVif
  	
	if [ $? -gt 0 ]
	then
		exit 1
	fi
  fi
  
  exit $?
fi

exit 0

