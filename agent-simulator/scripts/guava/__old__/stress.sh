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
zoneid=1
templateid=3
account="u"
x=$1
y=$2
p=$3
q=$4

for i in `seq 1 3`
do
	serviceOfferingId=10
	query="GET	http://127.0.0.1/client/?command=deployVirtualMachine&zoneId=$zoneid&hypervisor=Simulator&templateId=$templateid&serviceOfferingId=$serviceOfferingId&account=$account&domainid=1	HTTP/1.0\n\n"
	echo -e $query | nc -v -q 20 127.0.0.1 8096
done

sleep 15s

for x in `seq $x $y`
do
	destroy="GET  http://127.0.0.1:8096/client/?command=destroyVirtualMachine&id=$x	HTTP/1.0\n\n"
	echo -e $destroy | nc -v -q 60 127.0.0.1 8096
done

sleep 240s

for i in `seq 1 30`
do
	serviceOfferingId=9
	query="GET	http://127.0.0.1/client/?command=deployVirtualMachine&zoneId=$zoneid&hypervisor=Simulator&templateId=$templateid&serviceOfferingId=$serviceOfferingId&account=$account&domainid=1	HTTP/1.0\n\n"
	echo -e $query | nc -v -q 20 127.0.0.1 8096
done

sleep 150s

for x in `seq $p $q`
do
	destroy="GET  http://127.0.0.1:8096/client/?command=destroyVirtualMachine&id=$x	HTTP/1.0\n\n"
	echo -e $destroy | nc -v -q 60 127.0.0.1 8096
done
