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

rm /tmp/biglock*
echo

#Test task A would acquire one lock again and again in little interval
./test_task.sh A 0.3 &

sleep 1
#At the same time, task B would try to acquire the lock as well.
./test_task.sh B 0.5 &

#For the original version, task B would essiental fail, because task A do it
# quicker and task B, so task B may not have time to execute. But for new
# version, since it's ordered by time, then nobody should fail.

read end
pkill test_task
