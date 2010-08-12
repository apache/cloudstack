#!/usr/bin/env bash
# $Id: lu_share.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/comstar/lu_share.sh $
# lu_share.sh -- make a logical unit (LU) available over iSCSI
# OpenSolaris

usage() {
  printf "Usage:  %s -i <initiator-iqn> -t <target-name> [ -u | -m ]\n" $(basename $0) >&2
}

valid_target_name() {  # <target-name>
  echo $1 | grep ':lu:' >/dev/null
  return $?
}

target_iqn_from_target_name() {  # <target-name>
  echo $1 | cut -d':' -f1,2,3
}

hg_from_initiator_iqn() {  # <initiator-iqn>
  echo $1
  return 0
}

lu_name_from_target_name() {  # <target-name>
  echo $1 | cut -d':' -f5
}

view_entry_from_hg_and_lu_name() {  # <host-group-name> <lu-name>
  local hg=$1
  local lu_name=$2
  local view=
  local last_view=
  local last_hg=
  for w in $(stmfadm list-view -l $lu_name)
  do
    case $w in
    [0-9]*) last_view=$w
            ;;
    esac
    
    if [ "$w" == "$hg" ]
    then
      echo $last_view
      return 0
    fi
  done
  return 1
}

create_host_group() {  # <initiator-iqn>
  local i_iqn=$1
  local host_group=
  local hg=

  local lines=$(stmfadm list-hg -v $i_iqn | grep $i_iqn | wc -l)    
  if [ $lines -eq 2 ]
  then
    return 0
  fi
  
  local result=
  result=$(stmfadm create-hg $i_iqn 2>&1)
  if [ $? -ne 0 ]
  then
     echo $result | grep "already exists" > /dev/null
     if [ $? -ne 0 ]
     then  
       printf "%s: create-hg %s failed due to %s\n" $(basename $0) $i_iqn $result >&2
       return 11
     fi
   fi
   
   result=$(stmfadm add-hg-member -g $i_iqn $i_iqn 2>&1)
   if [ $? -ne 0 ]
   then
     echo $result | grep "already exists" > /dev/null
     if [ $? -ne 0 ]
     then
       printf "%s: unable to add %s due to %s\n" $(basename $0) $i_iqn $result >&2
       return 12
     fi
   fi
  return 0
}

add_view() { # <hg> <lu_name>
  local i=1
  local hg=$1
  local lu=$2
  
  while [ $i -lt 500 ]
  do
    local lun=$[ ( $RANDOM % 512 ) ]
    local result=
    result=$(stmfadm add-view -h $hg -n $lun $lu 2>&1)
    if [ $? -eq 0 ]
    then
      printf "lun %s for luname %s\n" $lun $lu
      #stmfadm list-view -l $lu
      #sbdadm list-lu 
      return 0
    fi
    echo $result | grep "view entry exists" > /dev/null
    if [ $? -eq 0 ]
    then
      return 0
    fi
    echo $result | grep "LUN already in use" > /dev/null
    if [ $? -ne 0 ]
    then
      echo $result
      return 1
    fi
    let i=i+1
  done
  printf "Unable to add view after lots of tries\n" >&2
  return 1
}

add_view_and_hg() {  # <initiator_iqn> <lu_name>
  local i_iqn=$1
  local lu_name=$2
  local hg=$(hg_from_initiator_iqn $i_iqn)
  local result=
  result=$(add_view $hg $lu_name)
  if [ $? -eq 0 ] 
  then
    echo $result
    return 0
  fi
 
  # create host group if necessary and try again
  echo $result | grep "invalid host group" > /dev/null
  if [ $? -ne 0 ]
  then
    printf "Unable to add view due to: $result\n" >&2
    return 22
  fi 
  if ! create_host_group $i_iqn
  then
    printf "%s: create_host_group failed: %s %s\n" $(basename $0) $i_iqn $lu_name >&2
    return 22
  fi
  result=$(add_view $hg $lu_name)
  if [ $? -eq 0 ]
  then 
    echo $result
    return 0
  fi
  printf "Unable to create view due to: $result\n" >&2
  return 24
}

remove_view() {  # <initiator-iqn> <lu-name>
  local i_iqn=$1
  local lu_name=$2
  local hg=$(hg_from_initiator_iqn $i_iqn)
  local view=$(view_entry_from_hg_and_lu_name $hg $lu_name)
  if [ -n "$hg" -a -n "$view" ]
  then
    local result=
    result=$(stmfadm remove-view -l $lu_name $view 2>&1)
    if [ $? -ne 0 ]
    then
      echo $result | grep "not found"
      if [ $? -eq 0 ]
      then
        return 0
      fi
      echo $result | grep "no views found"
      if [ $? -eq 0 ]
      then
       return 0
      fi
      printf "Unable to remove view due to: $result\n" >&2
      return 5
    fi
  fi
  return 0
}

remove_view_unknown_init_iqn() {  # <lu_name>
  local view_count=$(stmfadm list-view -l $1 2>/dev/null | grep View | wc -l)
  local view_number
  # if no view for LU, then report success
  if [ $view_count -eq 0 ]
  then
    return 0
  fi
  # if no init iqn specified, LU should only have one view
  # if more than one view, squawk and do nothing
  if [ "$view_count" == "1" ]
  then
    view_number=$(stmfadm list-view -l $1 2>/dev/null | grep View | awk '{print $NF}')
    stmfadm remove-view -l $1 $view_number
  else
    printf "remove_view_unknown_init_iqn: %s views! \n" $view_count >&2
  fi
}

remove_all_views() {  # <lu-name>
  local lu_name=$1
  stmfadm remove-view -a -l $lu_name
  if [ $? -eq 1 ]
  then
    return 0
  fi
}

#set -x

if [ $# -lt 4 ]
then
  usage
  exit 1
fi

iflag=
tflag=
uflag=
mflag=

while getopts 'mui:t:' OPTION
do
  case $OPTION in
  i)	iflag=1
  		init_iqn="$OPTARG"
  		;;
  m)    mflag=1
        ;;
  t)	tflag=1
  		tgtname="$OPTARG"
  		;;
  u)	uflag=1
  		;;
  *)	usage
  		exit 2
  		;;
  esac
done

if [ "$iflag$tflag" != "11" -o "$mflag$uflag" == "11" ]
then
  usage
  exit 3
fi

if ! valid_target_name $tgtname
then
  printf "%s: invalid target name format: %s\n" $(basename $0) $tgtname >&2
  exit 4
fi

lu_name=$(lu_name_from_target_name $tgtname)

if [ "$uflag" == "1" ]
then
  if [ "$init_iqn" == "unshare_all" ]
  then
    if ! remove_all_views $lu_name
    then
      printf "%s: remove_all_views failed: %s\n" $(basename $0) $lu_name >&2
      exit 20
    fi
  else
    if [ "$init_iqn" == "n/a" ]
    then
      if ! remove_view_unknown_init_iqn $lu_name
      then
        printf "%s: remove_view_unknown_init_iqn failed: %s\n" $(basename $0) $lu_name >&2
        exit 25
      fi
    else
      if ! remove_view $init_iqn $lu_name
      then
        printf "%s: remove_view failed: %s\n" $(basename $0) $lu_name >&2
        exit 21
      fi
    fi
  fi
else
  if [ "$mflag" == "1" ]
  then
    remove_all_views $lu_name
  fi
  # finally add a view of the lu
  RANDOM=$(perl -e 'print time;')
  if ! add_view_and_hg $init_iqn $lu_name
  then
    printf "%s: add_view failed: %s %s\n" $(basename $0) $host_group $lu_name >&2
    exit 23
  fi
fi

exit 0
