#!/usr/bin/env bash
# $Id: call_firewall.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.0.0/java/scripts/vm/hypervisor/xenserver/patch/call_firewall.sh $
# firewall.sh -- allow some ports / protocols to vm instances
#
#

usage() {
  printf "Usage: %s: (-A|-D) -i <domR eth1 ip>  -r <target-instance-ip> -P protocol (-p port_range | -t icmp_type_code)  -l <public ip address> -d <target port> [-f <firewall ip> -u <firewall user> -y <firewall password> -z <firewall enable password> ] \n" $(basename $0) >&2
}

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

CERT="$(dirname $0)/id_rsa"

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

ssh -p 3922 -q -o StrictHostKeyChecking=no -i $CERT root@$domRIp "/root/firewall.sh $*"
exit $?









