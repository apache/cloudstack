#!/bin/bash
# $Id: installrtr.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/installer/installrtr.sh $

#set -x
usage() {
 echo "Usage: $(basename $0) -t <templates base location> -c <createtmplt.sh path> "
 echo "eg: $(basename $0) -t tank/volumes/demo/template -c /root/createmplt.sh "
}

fflag=
tflag=
cflag=
while getopts 'f:t:c:' OPTION
do
  case $OPTION in
  f)	fflag=1
        ;;
  t)	tflag=1
  		template_location="$OPTARG"
  		;;
  c)	cflag=1
  		create_tmplt_path="$OPTARG"
  		;;    
  ?)	usage
		exit 2
		;;
  esac
done

shift $(($OPTIND - 1))

if [ "$tflag" != "1" ] || [ "$cflag" != "1" ] 
then
 usage
 exit 2
fi

tmpltfs=$template_location/private/u000000/os/routing
if [ "$fflag" == "1" ] 
then
  zfs destroy -Rr $tmpltfs 2> /dev/null
fi

snaps=$(zfs list -t snapshot -r $tmpltfs 2> /dev/null)
if [ $? -eq 0 -a "$snaps" != "no datasets available" ]
then
  echo "Warning: some snapshots already exist at target location $tmpltfs"
  echo "Use -f to delete these first"
  exit 2
fi

$create_tmplt_path -t $tmpltfs -n routing -f /root/template/vmi-root-fc8-x86_64-domR.img.bz2 -s 2 -d routing -u

rm -f /$tmpltfs/routing.tar

exit 0
