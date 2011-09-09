#!/bin/bash

# IMPORTANT: Ordering of lock:
# biglock --> rrouter

# getLockFile() parameters
# $1 lock filename
# $2 timeout seconds
getLockFile() {
    __locked=0
    __LOCKFILE="/tmp/$1.lock"
    if [ $2 ]
    then
        __TIMEOUT=$2
    else
        __TIMEOUT=10
    fi

    for i in `seq 1 $__TIMEOUT`
    do
        if [ ! -e $__LOCKFILE ]
        then
            touch $__LOCKFILE
            __locked=1
            break
        fi
        sleep 1
        logger -t cloud "sleep 1 second wait for the lock file " $__LOCKFILE
    done
    if [ $__locked -ne 1 ]
    then
	logger -t cloud "fail to acquire the lock file $__LOCKFILE after $__TIMEOUT seconds time out!"
    fi
    echo $__locked
}

# releaseLockFile() parameters
# $1 lock filename
# $2 locked(1) or not(0)
releaseLockFile() {
    __LOCKFILE="/tmp/$1.lock"
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

