#!/usr/bin/env bash
# $Id: stopvm.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/vm/hypervisor/xen/stopvm.sh $
# stopvm.sh -- stop one or all vm  (iscsi version -- does not unmount)
#
#
#set -x

usage() {
  printf "Usage: %s: [-f] [-u] <-a|-l <vm label>> \n" $(basename $0) >&2
}

_vifname() {
 local vmname=$1
 local domid=$(xm domid $vmname)
 echo vif${domid}
}

remove_acct_rule() {
  local vifname=$1
  local intf=$2

  iptables -L FORWARD | grep $vifname &> /dev/null
  if [ $? -eq 0 ]
  then
    iptables-save | grep $vifname\\. | sed s/-A/-D/ | while read line
    do
        iptables $line
    done
  fi

  return $?
}

remove_rate_limit_rules() {
  local domid=$1
  local vifname=$2
  vifname=${vifname}.0 
  local mark=$(echo "obase=16; $domid" | bc) ##convert to hex
  
  /usr/local/sbin/ebtables-save | grep FORWARD | grep -iw 0x${mark} | sed s/-A/-D/ | while read line
    do
      ebtables $line
    done
}

unmount_local() {
  local localdir=$1
  if [ "$localdir" != "" ]
  then
    umount $localdir
  fi
  printf "Unmounting $localdir result=$?\n"
}

unmount_disks() {
  local vmname=$1
  local disks=$(ls  -l $DISKDIR | grep $vmname | awk '{print $NF}')
  for d in $disks ; 
  do 
    disknum=$(echo $d | awk -F"/" '{print $NF}')
    tgt=$(ls -l /dev/disk/by-path  | grep $disknum | awk '{print $(NF-2)}' | grep -v part |  cut -d: -f2- | cut -d- -f3-)
    softlink=$(ls -l $DISKDIR | grep $d | awk '{print $(NF-2)}')
    rm $DISKDIR/$softlink    #delete soft link
    iscsiadm -m node -T $tgt -u  #logout
    sleep 3 #wait for logout to complete?
    iscsiadm -m node -T $tgt -o delete  #delete record
  done

  #unmount the image filesystem from nfs
  unmount_local $2
}

stop_one_vm() {
  local vmname=$1
  local force=$2
  local unmount=$3
  local localdir=
  local kernelpath=$(xm list -l $1 | grep vmlinuz)
  local vifname=$(_vifname $vmname)

  if [ "$kernelpath" != "" ]
  then
    local tmp=($kernelpath)
    if [ ${#tmp[*]} -gt 1 ] 
    then
      kernelpath=${tmp[1]}
    fi
    localdir=$(dirname $kernelpath)
  fi

  xm list $1 &>/dev/null
  local result=$?
  if [ $result -eq 3 ]
  then
    printf "Error: vm $1 does not exist\n" >&2
    return 0
  fi
  
  if [ $result -ne 0 ]
  then 
    printf "Error: xm returned $result\n" >&2
    return 2
  fi

  
  local domId=0;
  if xm list $1 -l | grep domid
  then
    status=($(xm list $1 | grep $1))
    domId=${status[1]};
  fi
  
  if [ $domId -gt 0 ]
  then
    if [ $force -eq 0 ]
    then
    	#Try a graceful shutdown
    	xm shutdown $1 -w
    	if [ $? -ne 0 ]
    	then
    	  sleep 5
    	  xm destroy $1
    	fi
    fi
  else
    #printf "Domain $1 is already shutdown\n"
    xm delete $1
    remove_acct_rule $vifname 2
    remove_rate_limit_rules $domId $vifname 
    return 0
  fi
  
  if [ $? -gt 0 ] || [ $force -eq 1 ] 
  then
    #Try an undignified shutdown
    xm destroy $1 
  fi
  
  if [ $? -gt 0 ]; then
    printf "Failed to terminate instance $1\n">&2
    return 20
  else
    xm delete $1 >/dev/null 2>&1
    if [ $? -gt 0 ] 
    then
      printf "xm shutdown reported success but vm is still around?$1\n">&2
      #Try an undignified shutdown
      sleep 5
      xm destroy $1
      xm delete $1
      if [ $? -gt 0 ]
      then 
        printf "xm delete did not work a second time\n" >&2
      fi
    fi
    remove_acct_rule $vifname 2
    remove_rate_limit_rules $domId $vifname 
  fi
  
  if [ "$unmount" == "1" ]
  then
    unmount_disks $1 $localdir
    if [ $force -eq 1 ]
    then
      for i in $(mount -t nfs | grep $1 | awk '{print $3}') 
      do    
        umount $i
      done
    fi
  fi

  return 0;
    
}

stop_all_vms() {
  for i in `xm list | grep -v Domain-0| grep -v Name| awk -F" " '{print $1}'`
  do 
    xm list -l $i  | grep vmops-has > /dev/null
    if [ $? -ne 0 ]
    then
      stop_one_vm $i $1 $2
    fi
  done
  if [ $1 -eq 1 ]
    then
      for i in $(mount -t nfs  | awk '{print $3}') 
      do    
        umount $i
      done
  fi

}



DISKDIR="/dev/disk/by-vm/"
lflag=
aflag=
fflag=0

while getopts 'fual:' OPTION
do
  case $OPTION in
  l)	lflag=1
		vmname="$OPTARG"
		;;
  a)	aflag=1
		;;
  f)	fflag=1
		;;
  u)	uflag=1
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$aflag$lflag" != "1" ]
then
 usage
 exit 2
fi

if [ "$aflag" == "1" ] 
then
  stop_all_vms $fflag $uflag
  exit 0
fi

stop_one_vm $vmname $fflag $uflag

exit $?


