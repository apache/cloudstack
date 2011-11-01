#!/bin/bash

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
