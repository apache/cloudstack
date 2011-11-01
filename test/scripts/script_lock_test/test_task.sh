#!/bin/bash

source ../../../patches/systemvm/debian/config/root/func.sh

lock="biglock"

for i in `seq 1 100`
do
    locked=$(getLockFile $lock)
    if [ "$locked" != "1" ]
    then
        echo WRONG, Task $1 can''t get the lock
        exit 1
    fi
    echo `date +%H:%M:%S.%N` TASK $1 get the lock
    sleep $2
    releaseLockFile $lock $locked
    echo `date +%H:%M:%S.%N` TASK $1 release the lock
done
