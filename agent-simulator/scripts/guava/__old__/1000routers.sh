#
# Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
#




zoneid=$1
templateId=$2
serviceOfferingId=$3

for j in `seq 1 100`
do
	let add=0
	for i in `seq 1 10`
	do
		let account=$(($i+$add))
		echo Account Name: , $account
		query="GET	http://127.0.0.1/client/?command=deployVirtualMachine&zoneId=$1&hypervisor=Simulator&templateId=$2&serviceOfferingId=$3&account=DummyAccount$account&domainid=1	HTTP/1.0\n\n"
	  echo -e $query | nc -v -q 20 127.0.0.1 8096
	done
	let add=add+10
	sleep 60s
done
