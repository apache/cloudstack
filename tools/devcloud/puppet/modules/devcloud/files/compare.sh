#! /bin/bash -eux

FILE=$1
WORKING_DIR=$2
cd $WORKING_DIR
test `grep  $FILE ${WORKING_DIR}/md5sum.txt | awk '{print $1}'` == `md5sum $FILE |awk '{print $1}'`