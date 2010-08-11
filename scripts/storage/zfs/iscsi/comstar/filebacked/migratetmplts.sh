#!/bin/bash
# $Id: migratetmplts.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/iscsi/comstar/filebacked/migratetmplts.sh $
# set -x
for dir in $(find /tank/vmops -name template)
do
  dir=${dir:1} #strip out leading slash
  for tmplt in $(zfs list -H -t volume -o name -r $dir ); 
  do   
    dd if=/dev/zvol/dsk/$tmplt of=/$tmplt bs=8096k; 
    today=$(date '+%m_%d_%Y')
    zfs snapshot -r $(dirname $tmplt)@${today}
    #zfs destroy -Rf $tmplt
    echo "Done: $tmplt"
  done
done
