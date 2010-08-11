#!/usr/bin/env bash
# gcc.sh - compiles javascript into one file
#
#

usage() {
  printf "Usage:\n %s [source directory where the java script files are] [destination directory to put the result] \n" $(basename $0) >&2
}

if [ $# -ne 2 ]; then
  usage
  exit 2;
fi

set -x

jsfiles="--js $1/jquery-1.4.min.js --js $1/date.js "
for file in `ls -l $1/jquery*.js | grep -v jquery-1.4 | awk '{print $NF}'`; do
  jsfiles=`echo $jsfiles "--js " $file`
done
jsfiles=`echo $jsfiles "--js $1/cloud.core.callbacks.js --js $1/cloud.core.js "`
for file in `ls -l $1/cloud*.js | egrep -v 'callback|core.js' | awk '{print $NF}'`; do
  jsfiles=`echo $jsfiles "--js " $file`
done

java -jar compiler.jar $jsfiles --js_output_file $2/cloud.core.min.js