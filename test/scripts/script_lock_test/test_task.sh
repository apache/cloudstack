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
