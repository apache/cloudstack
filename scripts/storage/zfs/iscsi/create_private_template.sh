#!/usr/bin/env bash
# $Id: create_private_template.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/create_private_template.sh $
# create_private_template.sh -- create a private template from a snapshot
#

usage() {
  printf "Usage: %s: -p <snapshot path> -n <snapshot name>\n" $(basename $0) >&2
  exit 2
}

create_template() {
  local fspath=$1
  local snapshotname=$2
  local template_path=$3
  local user_dir=$4
  local instance_dir=$5
  local tname=$6

  if [ -d "/$template_path/$user_dir/$instance_dir/$tname" ]; then
    printf "template exists at path:  $template_path/$user_dir/$instance_dir/$tname\n" >&2
    return 4
  fi

  if [ ! -d "/$template_path/$user_dir" ]; then
    zfs create $template_path/$user_dir
    if [ $? -gt 0 ]
    then
      printf "***Failed to create private template path $template_path/$user_dir\n" >&2
      return  6
    fi
  fi

  if [ ! -d "/$template_path/$user_dir/$instance_dir" ]; then
    zfs create $template_path/$user_dir/$instance_dir
    if [ $? -gt 0 ]
    then
      printf "***Failed to create private template path $template_path/$user_dir/$instance_dir\n" >&2
      return  6
    fi
  fi

  zfs send $fspath@$snapshotname | zfs recv $template_path/$user_dir/$instance_dir/$tname
  zfs send $fspath/rootdisk@$snapshotname | zfs recv $template_path/$user_dir/$instance_dir/$tname/rootdisk
  zfs send $fspath/datadisk1@$snapshotname | zfs recv $template_path/$user_dir/$instance_dir/$tname/datadisk1

  ##### other things we could try are clone then promote
  # zfs clone $fspath@$snapshotname $template_path
  # zfs clone $fspath/rootdisk@$snapshotname $template_path/rootdisk
  # zfs clone $fspath/datadisk1@$snapshotname $template_path/datadisk1
  # zfs promote $template_path

  if [ $? -gt 0 ]
  then
    printf "***Failed to receive snapshot $snapshotname for path $fspath\n" >&2
    return  5
  fi

}

#set -x

pflag=
nflag=
dflag=
uflag=
iflag=
sflag=
pathval=
templatename=
snapshot=
install_dir=
user_folder=
instance_folder=

while getopts 'p:n:d:u:i:s:' OPTION
do
  case $OPTION in
  p)    pflag=1
        pathval="$OPTARG"
        ;;
  n)	nflag=1
        templatename="$OPTARG"
        ;;
  s)    sflag=1
        snapshot="$OPTARG"
        ;;
  d)    dflag=1
        install_dir="$OPTARG"
        ;;
  u)    uflag=1
        user_folder="$OPTARG"
        ;;
  i)    iflag=1
        instance_folder="$OPTARG"
        ;;
  ?)	usage
        ;;
  esac
done

if [ "$pflag$nflag$dflag$uflag$iflag$sflag" != "111111" ]
then
  usage
fi

create_template $pathval $snapshot $install_dir $user_folder $instance_folder $templatename
exit $?
