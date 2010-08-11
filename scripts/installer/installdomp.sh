#!/bin/bash
# $Id: installdomp.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/installer/installdomp.sh $

#set -x
usage() {
 echo "Usage: $(basename $0) <templates base location>"
 echo "eg: $(basename $0) tank/volumes/demo/template"
}

fflag=

while getopts 'f' OPTION
do
  case $OPTION in
  f)	fflag=1
        ;;
  ?)	usage
		exit 2
		;;
  esac
done

shift $(($OPTIND - 1))

if [ $# -lt 1 ]
then
 usage
 exit 2
fi

tmpltfs=$1/private/u000000/os/consoleproxy
if [ "$fflag" == "1" ] 
then
  zfs destroy -r $tmpltfs 2> /dev/null
fi

snaps=$(zfs list -t snapshot -r $tmpltfs)
if [ $? -eq 0 -a "$snaps" != "no datasets available" ]
then
  echo "Warning: some snapshots already exist at target location $tmpltfs"
  echo "Use -f to delete these first"
  exit 2
fi

$(dirname $0)/createtmplt.sh  -t $tmpltfs -n consoleproxy -f /root/template/vmi-root-fc8-x86_64-domP.img.bz2 -s 3 -d consoleproxy -u

rm -f /$tmpltfs/consoleproxy.tar

exit 0
