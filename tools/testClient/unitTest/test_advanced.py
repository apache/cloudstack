import xml.dom.minidom
import inspect
import uuid
import time
from cloudstackAPI import *
import cloudstackTestClient
if __name__ == "__main__":
    randomName = str(uuid.uuid4())
    testClient = cloudstackTestClient.cloudstackTestClient("localhost")
    api = testClient.getApiClient()
    
    czcmd = createZone.createZoneCmd()
    czcmd.dns1 = "8.8.8.8"
    czcmd.internaldns1 = "192.168.110.254"
    czcmd.name = "test" + randomName
    czcmd.networktype = "Advanced"
    czcmd.guestcidraddress = "10.1.1.0/24"
    czcmd.vlan = "1160-1200"
    czresponse = api.createZone(czcmd)
    zoneId = czresponse.id
    
    cvlancmd = createVlanIpRange.createVlanIpRangeCmd()
    cvlancmd.zoneid = zoneId
    cvlancmd.vlan = "2020"
    cvlancmd.gateway = "172.16.112.1"
    cvlancmd.netmask = "255.255.0.0"
    cvlancmd.startip = "172.16.112.2"
    cvlancmd.endip = "172.16.112.100"
    api.createVlanIpRange(cvlancmd)
    
    cpodcmd = createPod.createPodCmd()
    cpodcmd.zoneid = zoneId
    cpodcmd.gateway = "192.168.137.1"
    cpodcmd.name = "testpod"+ randomName
    cpodcmd.netmask = "255.255.255.0"
    cpodcmd.startip = "192.168.137.200"
    cpodcmd.endip = "192.168.137.220"
    cpodresponse = api.createPod(cpodcmd)
    podId = cpodresponse.id
    
    aclustercmd = addCluster.addClusterCmd()
    aclustercmd.clustername = "testcluster"+ randomName
    aclustercmd.hypervisor = "KVM"
    aclustercmd.podid = podId
    aclustercmd.zoneid = zoneId
    aclustercmd.clustertype = "CloudManaged"
    clusterresponse = api.addCluster(aclustercmd)
    clusterId = clusterresponse[0].id
    '''
    for i in range(1):
        addhostcmd = addHost.addHostCmd()
        addhostcmd.zoneid = zoneId
        addhostcmd.podid = podId
        addhostcmd.clusterid = clusterId
        addhostcmd.hypervisor = "KVM"
        addhostcmd.username = "root"
        addhostcmd.password = "password"
        addhostcmd.url = "http://192.168.137.2"
        addhostresponse = api.addHost(addhostcmd)
        print addhostresponse[0].id, addhostresponse[0].ipaddress
    '''
    createspcmd = createStoragePool.createStoragePoolCmd()
    createspcmd.zoneid = zoneId
    createspcmd.podid = podId
    createspcmd.clusterid = clusterId
    createspcmd.url = "nfs://nfs2.lab.vmops.com/export/home/edison/kvm2"
    createspcmd.name = "storage pool" + randomName
    createspresponse = api.createStoragePool(createspcmd)
    
    addsscmd = addSecondaryStorage.addSecondaryStorageCmd()
    addsscmd.url = "nfs://nfs2.lab.vmops.com/export/home/edison/xen/secondary"
    addsscmd.zoneid = zoneId
    api.addSecondaryStorage(addsscmd)
    
    listtmcmd = listTemplates.listTemplatesCmd()
    listtmcmd.id = 4
    listtmcmd.zoneid = zoneId
    listtmcmd.templatefilter = "featured"
    listtmresponse = api.listTemplates(listtmcmd)
    while True:
        if listtmresponse is not None and listtmresponse[0].isready == "true":
            break
        time.sleep(30)
        listtmresponse = api.listTemplates(listtmcmd)
        
    vmId = []
    for i in range(2):
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        cmd.zoneid = zoneId
        cmd.hypervisor = "KVM"
        cmd.serviceofferingid = "1"
        cmd.templateid = listtmresponse[0].id
        res = api.deployVirtualMachine(cmd)
        
        vmId.append(res.id)
