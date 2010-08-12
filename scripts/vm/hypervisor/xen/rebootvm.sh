#!/usr/bin/env bash
# $Id: rebootvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/vm/hypervisor/xen/rebootvm.sh $
# reboot.sh -- reboot one vm  
#
#
#set -x

usage() {
  printf "Usage: %s: -l <vm label>> \n" $(basename $0) >&2
}

_vifname() {
 local vmname=$1
 local domid=$(xm domid $vmname)
 echo vif${domid}
}

replace_acct_rule() {
  local oldvif=$1
  local newvif=$2

  #iptables-save | grep $oldvif\\. | sed s/$oldvif/$newvif/ | while read line
  #do
  #    iptables $line
  #done
  iptables-save | grep $oldvif\\. | sed s/-A/-D/ | while read line
  do
      iptables $line
  done

  return 0
}

replace_rate_limit_rules() {
  local olddomid=$1
  local newdomid=$2
  local oldvif=$3
  local newvif=$4

  local oldmark=$(echo "obase=16; $olddomid" | bc | tr [A-Z] [a-z])
  local newmark=$(echo "obase=16; $newdomid" | bc | tr [A-Z] [a-z]) 
  
  /usr/local/sbin/ebtables-save | grep FORWARD | grep -iw 0x${oldmark} | sed s/0x${oldmark}/0x${newmark}/ | sed s/$oldvif/$newvif/ | while read line
  do
    ebtables $line
  done
  /usr/local/sbin/ebtables-save | grep FORWARD | grep -iw 0x${oldmark} | sed s/-A/-D/ | while read line
  do
    ebtables $line
  done

  return 0
}


reboot_one_vm() {

  if  ! xm list $1  &>/dev/null
  then
    printf "Error: vm $1 does not exist\n" >&2
    return 2
  fi

  local vmname=$1
  local vifname=$(_vifname $vmname)
  
  local domId=$(xm domid $vmname)
  
  if [ "$domId" -gt 0 ]
  then
    xm reboot $1 -w
  else
    printf "Invalid domain. Not rebooting $1\n">&2
    return 10
  fi

  local rebooted=$?
  if [ $rebooted -gt 0 ] 
  then
    printf "Failed to reboot instance $1\n">&2
    return 20
  fi

  local newvifname=$(_vifname $vmname)
  local newdomId=$(xm domid $vmname)
  replace_acct_rule $vifname $newvifname
  replace_rate_limit_rules $domId $newdomId $vifname $newvifname
  
  
  return 0;
}


lflag=

while getopts 'l:' OPTION
do
  case $OPTION in
  l)	lflag=1
		vmname="$OPTARG"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$lflag" != "1" ]
then
 usage
 exit 2
fi


reboot_one_vm $vmname 

exit $?

