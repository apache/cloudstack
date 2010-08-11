#
# $Id: functions.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/zfs/iscsi/comstar/filebacked/functions.sh $
# functions.sh - OpenSolaris utility functions
#

list_views() {
  for lu in $(sbdadm list-lu  | awk '{print $1}')
  do
    if stmfadm list-view -l $lu >/dev/null 2>/dev/null
    then
      echo $lu
      stmfadm list-view -l $lu
    else
      echo $lu "no_view"
    fi
  done
}

list_zvol() {  # <zvol-path>
  for lu in $(sbdadm list-lu | grep zvol | awk '{print $1}')
  do
    if stmfadm list-lu -v $lu | grep $1 >/dev/null
    then
      echo "lu = $lu"
      stmfadm list-view -l $lu
    fi
  done
}

# takes about 3 seconds per volume
destroy_zvol () {  # <zvol-path> 
	local luname=$(sbdadm list-lu | grep $1 | awk '{print $1}');
	sbdadm delete-lu $luname;
	zfs destroy $1
}
