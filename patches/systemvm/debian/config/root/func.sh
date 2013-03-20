#!/bin/bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Only one lock is allowed: biglock

# getLockFile() parameters
# $1 lock filename
# $2 timeout seconds
getLockFile() {
    __locked=0
    __TS=`date +%s%N`
    __LOCKFILE="/tmp/$__TS-$$-$1.lock"
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
    if [ ! -e $__LOCKFILE ]
    then
        return
    fi
    
    for i in `seq 1 $(($__TIMEOUT * 10))`
    do
        currlock=`ls /tmp/*-$1.lock | head -n1`
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
    __LOCKFILE="/tmp/*-$$-$1.lock"
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

