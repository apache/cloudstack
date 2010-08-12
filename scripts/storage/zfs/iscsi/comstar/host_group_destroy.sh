#!/usr/bin/env bash
# $Id: host_group_destroy.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/comstar/host_group_destroy.sh $
# host_group_destroy.sh -- delete all iSCSI host groups
#
# Usage:  host_group_destroy.sh
#
# Removes all iSCSI host groups that are not in use.
#
# OpenSolaris

# Delete iSCSI host groups
host_groups=$(stmfadm list-hg | cut -d' ' -f 3)

for host_group in $host_groups
do
        stmfadm delete-hg $host_group
done

