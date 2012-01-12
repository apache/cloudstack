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
 
 
usage() {
  printf "Query job result Usage: %s: -h management-server -j jobid\n" $(basename $0) >&2
}

#options
jflag=
hflag=

jobid=
host="127.0.0.1" #defaults to localhost

while getopts 'h:j:' OPTION
do
 case $OPTION in
  h)	hflag=1
        host="$OPTARG"
        ;;
  j)    jflag=1
        jobid="$OPTARG"
        ;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ $jflag != "1" ]
then
 usage
 exit 2
fi


job_query="GET  http://$host:8096/client/?command=queryAsyncJobResult&jobid=$jobid	HTTP/1.0\n\n"
echo -e $job_query | nc -v -w 60 $host 8096
