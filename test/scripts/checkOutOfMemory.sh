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



 


while true
do
sleep 600
pid=`ps -ef | grep 'management.jmxremote' | grep -v 'cloud-management' | grep -v grep | awk '{print \$2}'`
if grep -q java.lang.OutOfMemoryError /var/log/vmops/vmops.log
then
while true
do
t=$(date  +"%h%d_%H_%M_%S")
jmap -dump:format=b,file=/root/dump/heap.bin.$t $pid
sleep 1800
done
fi
done