#!/usr/bin/env bash
# $Id: ipassoc.sh 9804 2010-06-22 18:36:49Z alex $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/network/domr/ipassoc.sh $
# ipassoc.sh -- associate/disassociate a public ip with an instance
#
#
# 2.1.4
usage() {
  printf "Usage:\n %s -A  -i <domR eth1 ip>  -l <public-ip-address>  -r <domr name> [-f] \n" $(basename $0) >&2
  printf " %s -D -i <domR eth1 ip> -l <public-ip-address> -r <domr name> [-f] \n" $(basename $0) >&2
}

cert="/root/.ssh/id_rsa.cloud"

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

#Add 1:1 NAT entry
add_one_to_one_nat_entry() {
  local guestIp=$1
  local publicIp=$2  
  local dIp=$3
  ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dIp "\
  iptables -t nat -A PREROUTING -i eth2 -d $publicIp -j DNAT --to-destination $guestIp
  iptables -t nat -A POSTROUTING -o eth2 -s $guestIp -j SNAT --to-source $publicIp
  iptables -P FORWARD DROP
  iptables -A FORWARD -m state --state RELATED,ESTABLISHED -j ACCEPT
  iptables -A FORWARD -i eth2 -o eth0 -d $guestIp -m state --state NEW -j ACCEPT
  iptables -A FORWARD -i eth0 -o eth2 -s $guestIp -m state --state NEW -j ACCEPT
  "
  return $?
}

#Add the NAT entries into iptables in the routing domain
add_nat_entry() {
  local dRIp=$1
  local pubIp=$2
  local ipNoMask=$(echo $2 | awk -F'/' '{print $1}')
   ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
      ip addr add dev $correctVif $pubIp
      iptables -t nat -I POSTROUTING   -j SNAT -o $correctVif --to-source $ipNoMask ;
      arping -c 3 -I $correctVif -A -U -s $ipNoMask $ipNoMask;
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
  local ipNoMask=$(echo $2 | awk -F'/' '{print $1}')
  local mask=$(echo $2 | awk -F'/' '{print $2}')
  [ "$mask" == "" ] && mask="32"
   ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
      iptables -t nat -D POSTROUTING   -j SNAT -o $correctVif --to-source $ipNoMask;
      ip addr del dev $correctVif "$ipNoMask/$mask"
     "
 
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     return 1
  fi

  return $?
}


add_an_ip () {
  local dRIp=$1
  local pubIp=$2
  local ipNoMask=$(echo $2 | awk -F'/' '{print $1}')
   ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
   	  ifconfig $correctVif up;
      ip addr add dev $correctVif $pubIp ;
      arping -c 3 -I $correctVif -A -U -s $ipNoMask $ipNoMask;
     "
   return $?
}

remove_an_ip () {
  local dRIp=$1
  local pubIp=$2
  local ipNoMask=$(echo $2 | awk -F'/' '{print $1}')
  local mask=$(echo $2 | awk -F'/' '{print $2}')
  [ "$mask" == "" ] && mask="32"
   ssh -p 3922 -o StrictHostKeyChecking=no -i $cert root@$dRIp "\
      ip addr del dev $correctVif "$ipNoMask/$mask"
     "
  if [ $? -gt 0  -a $? -ne 2 ]
  then
     return 1
  fi
}

#set -x

rflag=
iflag=
lflag=
aflag=
nflag=
fflag=
vflag=
gflag=
nflag=
cflag=
Gflag=
op=""

while getopts 'fADr:i:a:l:v:g:n:c:G:' OPTION
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
  c)	cflag=1
  		correctVif="$OPTARG"
  		;;
  G)    Gflag=1
        guestIp="$OPTARG"
        ;;
  ?)	usage
		exit 2
		;;
  esac
done

#1:1 NAT
if [ "$Gflag" == "1" ]
then
  add_nat_entry $domRIp $publicIp 
  if [ $? -eq 0 ]
  then
    add_one_to_one_nat_entry $guestIp $publicIp $domRIp
  fi
  exit $?
fi

#Either the A flag or the D flag but not both
if [ "$Aflag$Dflag" != "1" ]
then
 usage
 exit 2
fi

if [ "$Aflag$lflag$iflag$cflag" != "1111" ] && [ "$Dflag$lflag$iflag$cflag" != "1111" ]
then
   exit 2
fi

# check if gateway domain is up and running
if ! check_gw "$domRIp"
then
   printf "Unable to ping the routing domain, exiting\n" >&2
   exit 3
fi

if [ "$fflag" == "1" ] && [ "$Aflag" == "1" ]
then
  add_nat_entry $domRIp $publicIp 
  exit $?
fi

if [ "$Aflag" == "1" ]
then  
  add_an_ip $domRIp $publicIp 
  exit $?
fi

if [ "$fflag" == "1" ] && [ "$Dflag" == "1" ]
then
  del_nat_entry $domRIp $publicIp 
  exit $?
fi

if [ "$Dflag" == "1" ]
then
  remove_an_ip $domRIp $publicIp 
  exit $?
fi

exit 0
