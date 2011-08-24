#!/bin/bash
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #
  
usage() {
  printf "Stop Router: %s: -h management-server -i vmid\n" $(basename $0) >&2
}

iflag=
hflag=

host="127.0.0.1" #defaults to localhost
vmid=

while getopts 'h:i:' OPTION
do
 case $OPTION in
  h)	hflag=1
        host="$OPTARG"
        ;;
  i)	iflag=1
        vmid="$OPTARG"
        ;;        
  ?)	usage
		exit 2
		;;
  esac
done

if [[ $iflag != "1" ]]
then
 usage
 exit 2
fi
stop_vm="GET  http://$host:8096/client/?command=stopRouter&id=$vmid	HTTP/1.0\n\n"
echo -e $stop_vm | nc -v -w 60 $host 8096
