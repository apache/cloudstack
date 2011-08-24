#!/bin/bash
  #
  # Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
  #

usage() {
  printf "Destroy System Virtual Machine\nUsage: %s: -h management-server-ip -i vmid\n" $(basename $0) >&2
}


hflag=
iflag=

vmid=
host="127.0.0.1" #defaults to localhost

while getopts 'h:i:' OPTION
do
 case $OPTION in
  h)	hflag=1
        host="$OPTARG"
        ;;
  i)    iflag=1
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

destroy="GET  http://$host:8096/client/?command=destroySystemVm&id=$vmid	HTTP/1.0\n\n"
echo -e $destroy | nc -v -w 60 $host 8096

