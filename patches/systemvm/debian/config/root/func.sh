#!/bin/bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 

# Only one lock is allowed: biglock

# getLockFile() parameters
# $1 lock filename
# $2 timeout seconds
getLockFile() {
    __locked=0
    __LOCKFILE="/tmp/$1-$$.lock"
    if [ $2 ]
    then
        __TIMEOUT=$2
    else
        __TIMEOUT=30
    fi

    if [ -e $__LOCKFILE ]
    then
        logger -t cloud "Process $0 pid $$ want to get ECLUSIVE LOCK $1 RECURSIVELY!"
        psline=`ps u $$`
        logger -t cloud "Failed job detail: $psline"
        echo 0
        return
    fi

    psline=`ps u $$`
    echo $psline > $__LOCKFILE

    for i in `seq 1 $(($__TIMEOUT * 10))`
    do
        currlock=`ls -tr /tmp/$1-*.lock | head -n1`
        if [ $currlock -ef $__LOCKFILE ]
        then
            __locked=1
            break
        fi
        sleep 0.1
        if [ $((i % 10)) -eq 0 ]
        then
            logger -t cloud "Process $0 pid $$ waiting for the lock $1 for another 1 second"
        fi
    done
    if [ $__locked -ne 1 ]
    then
	logger -t cloud "fail to acquire the lock $1 for process $0 pid $$ after $__TIMEOUT seconds time out!"
        cmd=`cat $currlock`
	logger -t cloud "waiting for command: $cmd"
        psline=`ps u $$`
        logger -t cloud "Failed job detail: $psline"
        rm $__LOCKFILE
    fi
    echo $__locked
}

# releaseLockFile() parameters
# $1 lock filename
# $2 locked(1) or not(0)
releaseLockFile() {
    __LOCKFILE="/tmp/$1-$$.lock"
    __locked=$2
    if [ "$__locked" == "1" ]
    then
        rm $__LOCKFILE
    fi
}

# releaseLockFile() parameters
# $1 exit value
# $2 lock filename
# $3 locked(1) or not(0)
unlock_exit() {
    releaseLockFile $2 $3
    exit $1
}

