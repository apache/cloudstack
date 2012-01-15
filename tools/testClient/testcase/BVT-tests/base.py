# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#

""" Base class for all Cloudstack resources - Virtual machine, Volume, Snapshot etc
"""

from utils import is_server_ssh_ready, random_gen
from cloudstackAPI import *
#Import System modules
import time

class Account:
    """ Account Life Cycle """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, admin=False):
        cmd = createAccount.createAccountCmd()
        #0 - User, 1 - Root Admin
        cmd.accounttype = int(admin)
        cmd.email = services["email"]
        cmd.firstname = services["firstname"]
        cmd.lastname = services["lastname"]
        cmd.password = services["password"]
        cmd.username = services["username"]
        account = apiclient.createAccount(cmd)

        return Account(account.__dict__)

    def delete(self, apiclient):
        cmd = deleteAccount.deleteAccountCmd()
        cmd.id = self.account.id
        apiclient.deleteAccount(cmd)

class VirtualMachine:
    """Manage virtual machine lifecycle
    """
    def __init__(self, items, services):
        self.__dict__.update(items)
        self.username = services["username"]
        self.password = services["password"]
        self.ssh_port = services["ssh_port"]
        self.ssh_client = None
        #extract out the ipaddress
        self.ipaddress = self.nic[0].ipaddress

    @classmethod
    def create(cls, apiclient, services, templateid=None, accountid=None, networkids = None):
        cmd = deployVirtualMachine.deployVirtualMachineCmd()
        cmd.serviceofferingid = services["serviceoffering"]
        cmd.zoneid = services["zoneid"]
        cmd.hypervisor = services["hypervisor"]
        cmd.account = accountid or services["account"]
        cmd.domainid = services["domainid"]

        if networkids:
            cmd.networkids = networkids
        elif "networkids" in services:
            cmd.networkids = services["networkids"]

        if templateid:
            cmd.templateid = templateid
        elif "template" in services:
            cmd.templateid = services["template"]

        if "diskoffering" in services:
            cmd.diskofferingid = services["diskoffering"]
        return VirtualMachine(apiclient.deployVirtualMachine(cmd).__dict__, services)

    def get_ssh_client(self, ipaddress = None, reconnect=False):
            if ipaddress != None:
                self.ipaddress = ipaddress
            if reconnect:
                self.ssh_client = is_server_ssh_ready(
                                                    self.ipaddress,
                                                    self.ssh_port,
                                                    self.username,
                                                    self.password
                                                )
            self.ssh_client = self.ssh_client or is_server_ssh_ready(
                                                    self.ipaddress,
                                                    self.ssh_port,
                                                    self.username,
                                                    self.password
                                                )
            return self.ssh_client

    def create_nat_rule(self, apiclient, services):
        cmd = createPortForwardingRule.createPortForwardingRuleCmd()
        cmd.ipaddressid = services["ipaddressid"]
        cmd.privateport = services["privateport"]
        cmd.publicport = services["publicport"]
        cmd.protocol = services["protocol"]
        cmd.virtualmachineid = self.id
        return apiclient.createPortForwardingRule(cmd)


    def delete_nat_rule(self, apiclient, nat_rule):
        cmd = deletePortForwardingRule.deletePortForwardingRuleCmd()
        cmd.id = nat_rule.id
        apiclient.deletePortForwardingRule(cmd)
        return

    def delete(self, apiclient):
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.id
        apiclient.destroyVirtualMachine(cmd)

    def attach_volume(self, apiclient, volume):
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = volume.id
        cmd.virtualmachineid = self.id
        return apiclient.attachVolume(cmd)

    def detach_volume(self, apiclient, volume):
        cmd = detachVolume.detachVolumeCmd()
        cmd.id = volume.id
        return apiclient.detachVolume(cmd)


class Volume:
    """Manage Volume Lifecycle
    """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):
        cmd = createVolume.createVolumeCmd()
        cmd.name = services["diskname"]
        cmd.diskofferingid = services["volumeoffering"]
        cmd.zoneid = services["zoneid"]
        cmd.account = services["account"]
        cmd.domainid= services["domainid"]
        return Volume(apiclient.createVolume(cmd).__dict__)

    @classmethod
    def create_custom_disk(cls, apiclient, services):
        cmd = createVolume.createVolumeCmd()
        cmd.name = services["diskname"]
        cmd.diskofferingid = services["customdiskofferingid"]
        cmd.size = services["customdisksize"]
        cmd.zoneid = services["zoneid"]
        cmd.account = services["account"]
        cmd.domainid= services["domainid"]
        return Volume(apiclient.createVolume(cmd).__dict__)


    @classmethod
    def create_from_snapshot(cls, apiclient, snapshot_id, services):
        cmd = createVolume.createVolumeCmd()
        cmd.name = "-".join([services["diskname"], random_gen()])
        cmd.snapshotid = snapshot_id
        cmd.zoneid = services["zoneid"]
        cmd.size = services["size"]
        cmd.account = services["account"]
        cmd.domainid = services["domainid"]
        return Volume(apiclient.createVolume(cmd).__dict__)

    def delete(self, apiclient):
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.id
        apiclient.deleteVolume(cmd)

class Snapshot:
    """Manage Snapshot Lifecycle
    """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, volume_id):
        cmd = createSnapshot.createSnapshotCmd()
        cmd.volumeid = volume_id
        return Snapshot(apiclient.createSnapshot(cmd).__dict__)

    def delete(self, apiclient):
        cmd = deleteSnapshot.deleteSnapshotCmd()
        cmd.id = self.id
        apiclient.deleteSnapshot(cmd)

class Template:
    """Manage template life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, volume, services):

        #Create template from Virtual machine and Volume ID
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([services["name"], random_gen()])
        cmd.ostypeid = services["ostypeid"]

        if "isfeatured" in services:
            cmd.isfeatured = services["isfeatured"]
        else:
            cmd.isfeatured = False

        if "ispublic" in services:
            cmd.ispublic = services["ispublic"]
        else:
            cmd.ispublic = False

        if "isextractable" in services:
            cmd.isextractable = services["isextractable"]
        else:
            cmd.isextractable = False

        cmd.volumeid = volume.id
        return Template(apiclient.createTemplate(cmd).__dict__)

    @classmethod
    def create_from_snapshot(cls, apiclient, snapshot, services):

        #Create template from Virtual machine and Snapshot ID
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = services["name"]
        cmd.ostypeid = services["ostypeid"]
        cmd.snapshotid = snapshot.id
        return Template(apiclient.createTemplate(cmd).__dict__)

    def delete(self, apiclient):
        cmd = deleteTemplate.deleteTemplateCmd()
        cmd.id = self.id
        apiclient.deleteTemplate(cmd)


class Iso:
    """Manage ISO life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):

        #Create ISO from URL
        cmd = registerIso.registerIsoCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = services["name"]
        cmd.ostypeid = services["ostypeid"]
        cmd.url = services["url"]
        cmd.zoneid = services["zoneid"]
        cmd.isextractable = services["isextractable"]
        cmd.isfeatured = services["isfeatured"]
        cmd.ispublic = services["ispublic"]
        return Iso(apiclient.createTemplate(cmd)[0].__dict__)

    def delete(self, apiclient):

        cmd = deleteIso.deleteIsoCmd()
        cmd.id = self.id
        apiclient.deleteIso(cmd)
        return

    def download(self, apiclient):
        #Ensuring ISO is successfully downloaded
        while True:
            time.sleep(120)

            cmd= listIsos.listIsosCmd()
            cmd.id = self.id
            response = apiclient.listIsos(cmd)[0]
            # Check whether download is in progress (for Ex: 10% Downloaded)
            # or ISO is 'Successfully Installed'
            if response.status == 'Successfully Installed':
                return
            elif 'Downloaded' not in response.status.split():
                raise Exception
        return


class PublicIPAddress:
    """Manage Public IP Addresses"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, accountid, zoneid = None, domainid = None):
        cmd = associateIpAddress.associateIpAddressCmd()
        cmd.account = accountid
        cmd.zoneid = zoneid or services["zoneid"]
        cmd.domainid = domainid or services["domainid"]
        return PublicIPAddress(apiclient.associateIpAddress(cmd).__dict__)

    def delete(self, apiclient):
        cmd = disassociateIpAddress.disassociateIpAddressCmd()
        cmd.id = self.ipaddress.id
        apiclient.disassociateIpAddress(cmd)
        return

class NATRule:
    """Manage NAT rule"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, virtual_machine, services, ipaddressid=None):

        cmd = createPortForwardingRule.createPortForwardingRuleCmd()
        cmd.ipaddressid = ipaddressid or services["ipaddressid"]
        cmd.privateport = services["privateport"]
        cmd.publicport = services["publicport"]
        cmd.protocol = services["protocol"]
        cmd.virtualmachineid = virtual_machine.id
        return NATRule(apiclient.createPortForwardingRule(cmd).__dict__)


    def delete(self, apiclient):
        cmd = deletePortForwardingRule.deletePortForwardingRuleCmd()
        cmd.id = self.id
        apiclient.deletePortForwardingRule(cmd)
        return

class ServiceOffering:
    """Manage service offerings cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):

        cmd = createServiceOffering.createServiceOfferingCmd()
        cmd.cpunumber = services["cpunumber"]
        cmd.cpuspeed  = services["cpuspeed"]
        cmd.displaytext = services["displaytext"]
        cmd.memory  = services["memory"]
        cmd.name = services["name"]
        return ServiceOffering(apiclient.createServiceOffering(cmd).__dict__)


    def delete(self, apiclient):

        cmd = deleteServiceOffering.deleteServiceOfferingCmd()
        cmd.id = self.id
        apiclient.deleteServiceOffering(cmd)
        return


class DiskOffering:
    """Manage disk offerings cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):

        cmd =createDiskOffering.createDiskOfferingCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = services["name"]
        cmd.disksize=services["disksize"]
        return DiskOffering(apiclient.createDiskOffering(cmd).__dict__)


    def delete(self, apiclient):

        cmd = deleteDiskOffering.deleteDiskOfferingCmd()
        cmd.id = self.id
        apiclient.deleteDiskOffering(cmd)
        return

class SnapshotPolicy:
    """Manage snapshot policies"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, volumeid, services):

        cmd = createSnapshotPolicy.createSnapshotPolicyCmd()
        cmd.intervaltype = services["intervaltype"]
        cmd.maxsnaps = services["maxsnaps"]
        cmd.schedule = services["schedule"]
        cmd.timezone = services["timezone"]
        cmd.volumeid = volumeid
        return SnapshotPolicy(apiclient.createSnapshotPolicy(cmd).__dict__)


    def delete(self, apiclient):

        cmd = deleteSnapshotPolicies.deleteSnapshotPoliciesCmd()
        cmd.id = self.id
        apiclient.deleteSnapshotPolicies(cmd)
        return

class LoadBalancerRule:
    """Manage Load Balancer rule"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, ipaddressid, accountid=None):
        cmd = createLoadBalancerRule.createLoadBalancerRuleCmd()
        cmd.publicipid = ipaddressid or services["ipaddressid"]
        cmd.account = accountid or services["account"]
        cmd.name = services["name"]
        cmd.algorithm = services["alg"]
        cmd.privateport = services["privateport"]
        cmd.publicport = services["publicport"]
        return LoadBalancerRule(apiclient.createLoadBalancerRule(cmd).__dict__)

    def delete(self, apiclient):
        cmd = deleteLoadBalancerRule.deleteLoadBalancerRuleCmd()
        cmd.id =  self.id
        apiclient.deleteLoadBalancerRule(cmd)
        return

    def assign(self, apiclient, vms):
        cmd = assignToLoadBalancerRule.assignToLoadBalancerRuleCmd()
        cmd.id = self.id
        cmd.virtualmachineids = [str(vm.id) for vm in vms]
        apiclient.assignToLoadBalancerRule(cmd)
        return

    def remove(self, apiclient, vms):
        cmd = removeFromLoadBalancerRule.removeFromLoadBalancerRuleCmd()
        cmd.virtualmachineids = [vm.id for vm in vms]
        self.apiclient.removeFromLoadBalancerRule(cmd)
        return

class Cluster:
    """Manage Cluster life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):

        cmd = addCluster.addClusterCmd()
        cmd.clustertype = services["clustertype"]
        cmd.hypervisor = services["hypervisor"]
        cmd.zoneid = services["zoneid"]
        cmd.podid = services["podid"]

        if "username" in services:
            cmd.username = services["username"]
        if "password" in services:
            cmd.password = services["password"]
        if "url" in services:
            cmd.url = services["url"]
        if "clustername" in services:
            cmd.clustername = services["clustername"]

        return Cluster(apiclient.addCluster(cmd)[0].__dict__)

    def delete(self, apiclient):
        cmd = deleteCluster.deleteClusterCmd()
        cmd.id =  self.id
        apiclient.deleteCluster(cmd)
        return

class Host:
    """Manage Host life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, cluster, services):

        cmd = addHost.addHostCmd()
        cmd.hypervisor = services["hypervisor"]
        cmd.url = services["url"]
        cmd.zoneid = services["zoneid"]
        cmd.clusterid = cluster.id
        cmd.podid = services["podid"]

        if "clustertype" in services:
            cmd.clustertype = services["clustertype"]
        if "username" in services:
            cmd.username = services["username"]
        if "password" in services:
            cmd.password = services["password"]

        return Host(apiclient.addHost(cmd).__dict__)

    def delete(self, apiclient):
        # Host must be in maintenance mode before deletion
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = self.id
        apiclient.prepareHostForMaintenance(cmd)
        time.sleep(60)

        cmd = deleteHost.deleteHostCmd()
        cmd.id =  self.id
        apiclient.deleteHost(cmd)
        return

class StoragePool:
    """Manage Storage pools"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):

        cmd = createStoragePool.createStoragePoolCmd()
        cmd.name = services["name"]
        cmd.podid = services["podid"]
        cmd.url = services["url"]
        cmd.clusterid = services["clusterid"]
        cmd.zoneid = services["zoneid"]

        return StoragePool(apiclient.createStoragePool(cmd).__dict__)

    def delete(self, apiclient):
        # Storage pool must be in maintenance mode before deletion
        cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
        cmd.id =  self.id
        apiclient.enableStorageMaintenance(cmd)
        time.sleep(60)
        cmd = deleteStoragePool.deleteStoragePoolCmd()
        cmd.id =  self.id
        apiclient.deleteStoragePool(cmd)
        return
