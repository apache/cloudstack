#!/bin/bash
MGMT_SERVER=$MGMT_SERVER

#login
curl -c admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=login&username=admin&password=password&response=json"

#delete service offerings
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=deleteServiceOffering&id=1&response=json"
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=deleteServiceOffering&id=2&response=json"
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=deleteServiceOffering&id=3&response=json"

#delete service offerings
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=deleteDiskOffering&id=1&response=json"
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=deleteDiskOffering&id=2&response=json"
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=deleteDiskOffering&id=3&response=json"

#create service offerings
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=createServiceOffering&cpuNumber=2&cpuSpeed=2400&memory=2400&displayText=Memcached%20Node&name=Memcached%20Node&storageType=local&response=json"
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=createServiceOffering&cpuNumber=2&cpuSpeed=2400&memory=2400&displayText=Web%20Node&name=Web%20Node&storageType=local&response=json"
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=createServiceOffering&cpuNumber=2&cpuSpeed=2400&memory=2400&displayText=Database%20Node&name=Database%20Node&storageType=local&response=json"

#create disk offerings
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=createDiskOffering&diskSize=20&displayText=Small%20Size&name=Volume%20#1&isMirrored=false&response=json"
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=createDiskOffering&diskSize=40&displayText=Volume%20Medium%20Size&name=Volume%20#2&isMirrored=false&response=json"
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=createDiskOffering&diskSize=80&displayText=Volume%20Large%20Size&name=Volume%20#3&isMirrored=false&response=json"

#add secondary storage
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=addSecondaryStorage&zoneId=1&url=nfs%3A//192.168.151.16/export/secondary-storage&response=json"

#register template
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=registerTemplate&displayText=Rightscale%20%ami-ccb35ea5&format=VHD&isPublic=true&name=Rightscale%20ami-ccb35ea5&osTypeId=11&passwordEnabled=false&url=http%3A//nfs1.lab.vmops.com/templates/ami-ccb35ea5/ami-ccb35ea5.vhd.bz2&response=json"

#add primary storage
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=createStoragePool&zoneId=1&podId=1&url=nfs%3A//192.168.151.16/export/primary-storage&response=json"

#add hosts here

#check if templates are ready (DomR and Rightscale AMI)
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=listTemplates&isReady=true&showAll=true&response=json"

#add user
curl -b admincookie.txt -sS "http://$MGMT_SERVER/client/api?command=createUser&username=tester&email=tester&firstname=tester&accounttype=0&password=<md5hash>&lastname=tester&response=json"

#login user
curl -c usercookie.txt -sS "http://$MGMT_SERVER/client/api?command=login&username=tester&password=tester&response=json"

#create VMs
curl -b usercookie.txt -sS "http://$MGMT_SERVER/client/api?command=deployVirtualMachine&zoneId=1&serviceofferingId=4&diskOfferingId=4&templateId=201&displayName=Farmville2&group=Farmville%20Group&response=json"

#get IP (n times)
curl -b usercookie.txt -sS "http://$MGMT_SERVER/client/api?command=associateIpAddress&zoneId=1&response=json"

#list IP addresses
curl -b usercookie.txt -sS "http://$MGMT_SERVER/client/api?command=listPublicIpAddresses&zoneId=1&response=json"

#create security group and add rule 
curl -b usercookie.txt -sS "http://$MGMT_SERVER/client/api?command=createSecurityGroup&name=ssh1&description=ssh1&response=json"
curl -b usercookie.txt -sS "http://$MGMT_SERVER/client/api?command=createNetworkRule&privateport=22&protocol=TCP&publicport=22&securitygroupid=1&response=json"
curl -b usercookie.txt -sS "http://$MGMT_SERVER/client/api?command=queryAsyncJobResult&jobId=8&response=json"

#assign security group
curl -b usercookie.txt -sS "http://$MGMT_SERVER/client/api?command=assignSecurityGroup&groupids=1&publicip=172.24.0.102&virtualmachineid=5&response=json" | awk '{print $(NF-1)}'
curl -b usercookie.txt -sS "http://$MGMT_SERVER/client/api?command=queryAsyncJobResult&jobId=8&response=json"
