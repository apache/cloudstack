#!/usr/bin/env bash
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
# gcc.sh - compiles javascript into one file

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