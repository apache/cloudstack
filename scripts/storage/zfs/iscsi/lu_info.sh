#!/usr/bin/env bash
# $Id: lu_info.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/branches/2.1.x.beta/java/scripts/storage/zfs/iscsi/lu_info.sh $

iscsitadm list target $1
