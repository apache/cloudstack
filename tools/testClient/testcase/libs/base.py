# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#

""" Base class for all Cloudstack resources
    -Virtual machine, Volume, Snapshot etc
"""

from utils import is_server_ssh_ready, random_gen
from cloudstackAPI import *
#Import System modules
import time
import hashlib

class Account:
    """ Account Life Cycle """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, admin=False):
        cmd = createAccount.createAccountCmd()
        """Creates an account"""

        #0 - User, 1 - Root Admin
        cmd.accounttype = int(admin)
        cmd.email = services["email"]
        cmd.firstname = services["firstname"]
        cmd.lastname = services["lastname"]

        # Password Encoding
        mdf = hashlib.md5()
        mdf.update(services["password"])
        cmd.password = mdf.hexdigest()
        cmd.username = "-".join([services["username"], random_gen()])
        account = apiclient.createAccount(cmd)

        return Account(account.__dict__)

    def delete(self, apiclient):
        """Delete an account"""
        cmd = deleteAccount.deleteAccountCmd()
        cmd.id = self.account.id
        apiclient.deleteAccount(cmd)


class VirtualMachine:
    """Manage virtual machine lifecycle"""

    def __init__(self, items, services):
        self.__dict__.update(items)
        self.username = services["username"]
        self.password = services["password"]
        self.ssh_port = services["ssh_port"]
        self.ssh_client = None
        #extract out the ipaddress
        self.ipaddress = self.nic[0].ipaddress

    @classmethod
    def create(cls, apiclient, services, templateid=None, accountid=None,
                                networkids=None, serviceofferingid=None,
                                mode='basic'):
        """Create the instance"""
        cmd = deployVirtualMachine.deployVirtualMachineCmd()

        if serviceofferingid:
            cmd.serviceofferingid = serviceofferingid
        elif "serviceoffering" in services:
            cmd.serviceofferingid = services["serviceoffering"]

        cmd.zoneid = services["zoneid"]
        cmd.hypervisor = services["hypervisor"]

        if accountid:
            cmd.account = accountid
        elif "account" in services:
            cmd.account = services["account"]

        cmd.domainid = services["domainid"]

        # List Networks for that user
        command = listNetworks.listNetworksCmd()
        command.zoneid = services["zoneid"]
        command.account = accountid or services["account"]
        command.domainid = services["domainid"]
        network = apiclient.listNetworks(command)

        if networkids:
            cmd.networkids = networkids
        elif "networkids" in services:
            cmd.networkids = services["networkids"]
        elif network:   #If user already has source NAT created, then use that
            if hasattr(network[0], "account"):
                cmd.networkids = network[0].id

        if templateid:
            cmd.templateid = templateid
        elif "template" in services:
            cmd.templateid = services["template"]

        if "diskoffering" in services:
            cmd.diskofferingid = services["diskoffering"]

        virtual_machine = apiclient.deployVirtualMachine(cmd)
        if mode.lower() == 'advanced':
            public_ip = PublicIPAddress.create(
                                           apiclient,
                                           virtual_machine.account,
                                           virtual_machine.zoneid,
                                           virtual_machine.domainid,
                                           services
                                           )
            nat_rule = NATRule.create(
                                    apiclient,
                                    virtual_machine,
                                    services,
                                    ipaddressid=public_ip.ipaddress.id
                                    )
            virtual_machine.ssh_ip = nat_rule.ipaddress
            virtual_machine.public_ip = nat_rule.ipaddress
        else:
            virtual_machine.ssh_ip = virtual_machine.nic[0].ipaddress
            virtual_machine.public_ip = virtual_machine.nic[0].ipaddress

        return VirtualMachine(virtual_machine.__dict__, services)

    def start(self, apiclient):
        """Start the instance"""
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = self.id
        apiclient.startVirtualMachine(cmd)

    def stop(self, apiclient):
        """Stop the instance"""
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = self.id
        apiclient.stopVirtualMachine(cmd)

    def reboot(self, apiclient):
        """Reboot the instance"""
        cmd = rebootVirtualMachine.rebootVirtualMachineCmd()
        cmd.id = self.id
        apiclient.rebootVirtualMachine(cmd)

    def get_ssh_client(self, ipaddress=None, reconnect=False):
        """Get SSH object of VM"""
        if ipaddress != None:
            self.ssh_ip = ipaddress
        if reconnect:
            self.ssh_client = is_server_ssh_ready(
                                                    self.ssh_ip,
                                                    self.ssh_port,
                                                    self.username,
                                                    self.password
                                                )
        self.ssh_client = self.ssh_client or is_server_ssh_ready(
                                                    self.ssh_ip,
                                                    self.ssh_port,
                                                    self.username,
                                                    self.password
                                                )
        return self.ssh_client

    def delete(self, apiclient):
        """Destroy an Instance"""
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.id
        apiclient.destroyVirtualMachine(cmd)

    def attach_volume(self, apiclient, volume):
        """Attach volume to instance"""
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = volume.id
        cmd.virtualmachineid = self.id
        return apiclient.attachVolume(cmd)

    def detach_volume(self, apiclient, volume):
        """Detach volume to instance"""
        cmd = detachVolume.detachVolumeCmd()
        cmd.id = volume.id
        return apiclient.detachVolume(cmd)


class Volume:
    """Manage Volume Lifecycle
    """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, zoneid=None, account=None,
               diskofferingid=None):
        """Create Volume"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = services["diskname"]
        cmd.domainid = services["domainid"]

        if diskofferingid:
            cmd.diskofferingid = diskofferingid
        elif "diskofferingid" in services:
            cmd.diskofferingid = services["diskofferingid"]

        if zoneid:
            cmd.zoneid = zoneid
        elif "zoneid" in services:
            cmd.zoneid = services["zoneid"]

        if account:
            cmd.account = account
        elif "account" in services:
            cmd.account = services["account"]

        return Volume(apiclient.createVolume(cmd).__dict__)

    @classmethod
    def create_custom_disk(cls, apiclient, services):
        """Create Volume from Custom disk offering"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = services["diskname"]
        cmd.diskofferingid = services["customdiskofferingid"]
        cmd.size = services["customdisksize"]
        cmd.zoneid = services["zoneid"]
        cmd.account = services["account"]
        cmd.domainid = services["domainid"]
        return Volume(apiclient.createVolume(cmd).__dict__)

    @classmethod
    def create_from_snapshot(cls, apiclient, snapshot_id, services):
        """Create Volume from snapshot"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = "-".join([services["diskname"], random_gen()])
        cmd.snapshotid = snapshot_id
        cmd.zoneid = services["zoneid"]
        cmd.size = services["size"]
        cmd.account = services["account"]
        cmd.domainid = services["domainid"]
        return Volume(apiclient.createVolume(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Volume"""
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
        """Create Snapshot"""
        cmd = createSnapshot.createSnapshotCmd()
        cmd.volumeid = volume_id
        return Snapshot(apiclient.createSnapshot(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Snapshot"""
        cmd = deleteSnapshot.deleteSnapshotCmd()
        cmd.id = self.id
        apiclient.deleteSnapshot(cmd)


class Template:
    """Manage template life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, volumeid=None,
               account=None, domainid=None):
        """Create template from Volume or URL"""
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

        if volumeid:
            cmd.volumeid = volumeid
        elif "url" in services:
            cmd.url = services["url"]

        if account:
            cmd.account = account

        if domainid:
            cmd.domainid = domainid

        return Template(apiclient.createTemplate(cmd).__dict__)

    @classmethod
    def create_from_snapshot(cls, apiclient, snapshot, services):
        """Create Template from snapshot"""
        #Create template from Virtual machine and Snapshot ID
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([services["name"], random_gen()])
        cmd.ostypeid = services["ostypeid"]
        cmd.snapshotid = snapshot.id
        return Template(apiclient.createTemplate(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Template"""
        cmd = deleteTemplate.deleteTemplateCmd()
        cmd.id = self.id
        apiclient.deleteTemplate(cmd)

    def download(self, apiclient):
        """Download Template"""
        while True:
                template_response = list_templates(
                                    apiclient,
                                    id=self.id,
                                    zoneid=self.zoneid,
                                    templatefilter='self'
                                    )
                template = template_response[0]
                # If template is ready,
                # template.status = Download Complete
                # Downloading - x% Downloaded
                # Error - Any other string 

                if template.status == 'Download Complete'  :
                    break
                elif 'Downloaded' not in template.status.split():
                    raise Exception
                elif 'Downloaded' in template.status.split():
                    time.sleep(120)


class Iso:
    """Manage ISO life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, account=None, domainid=None):
        """Create an ISO"""
        #Create ISO from URL
        cmd = registerIso.registerIsoCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = services["name"]
        cmd.ostypeid = services["ostypeid"]
        cmd.url = services["url"]
        cmd.zoneid = services["zoneid"]
        if "isextractable" in services:
            cmd.isextractable = services["isextractable"]
        if "isfeatured" in services:
            cmd.isfeatured = services["isfeatured"]
        if "ispublic" in services:
            cmd.ispublic = services["ispublic"]

        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid

        return Iso(apiclient.registerIso(cmd)[0].__dict__)

    def delete(self, apiclient):
        """Delete an ISO"""
        cmd = deleteIso.deleteIsoCmd()
        cmd.id = self.id
        apiclient.deleteIso(cmd)
        return

    def download(self, apiclient):
        """Download an ISO"""
        #Ensuring ISO is successfully downloaded
        while True:
            time.sleep(120)

            cmd = listIsos.listIsosCmd()
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
    def create(cls, apiclient, accountid, zoneid=None, domainid=None,
               services=None, networkid=None):
        """Associate Public IP address"""
        cmd = associateIpAddress.associateIpAddressCmd()
        cmd.account = accountid
        cmd.zoneid = zoneid or services["zoneid"]
        cmd.domainid = domainid or services["domainid"]

        if networkid:
            cmd.networkid = networkid
        return PublicIPAddress(apiclient.associateIpAddress(cmd).__dict__)

    def delete(self, apiclient):
        """Dissociate Public IP address"""
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
        """Create Port forwarding rule"""
        cmd = createPortForwardingRule.createPortForwardingRuleCmd()

        if ipaddressid:
            cmd.ipaddressid = ipaddressid
        elif "ipaddressid" in services:
            cmd.ipaddressid = services["ipaddressid"]

        cmd.privateport = services["privateport"]
        cmd.publicport = services["publicport"]
        cmd.protocol = services["protocol"]
        cmd.virtualmachineid = virtual_machine.id
        return NATRule(apiclient.createPortForwardingRule(cmd).__dict__)

    def delete(self, apiclient):
        """Delete port forwarding"""
        cmd = deletePortForwardingRule.deletePortForwardingRuleCmd()
        cmd.id = self.id
        apiclient.deletePortForwardingRule(cmd)
        return


class FireWallRule:
    """Manage Firewall rule"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, ipaddressid, protocol, cidrlist=None,
               startport=None, endport=None):
        """Create Firewall Rule"""
        cmd = createFirewallRule.createFirewallRuleCmd()
        cmd.ipaddressid = ipaddressid
        cmd.protocol = protocol
        if cidrlist:
            cmd.cidrlist = cidrlist
        if startport:
            cmd.startport = startport
        if endport:
            cmd.endport = endport

        return NATRule(apiclient.createFirewallRule(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Firewall rule"""
        cmd = deleteFirewallRule.deleteFirewallRuleCmd()
        cmd.id = self.id
        apiclient.deleteFirewallRule(cmd)
        return


class ServiceOffering:
    """Manage service offerings cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):
        """Create Service offering"""
        cmd = createServiceOffering.createServiceOfferingCmd()
        cmd.cpunumber = services["cpunumber"]
        cmd.cpuspeed = services["cpuspeed"]
        cmd.displaytext = services["displaytext"]
        cmd.memory = services["memory"]
        cmd.name = services["name"]
        return ServiceOffering(apiclient.createServiceOffering(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Service offering"""
        cmd = deleteServiceOffering.deleteServiceOfferingCmd()
        cmd.id = self.id
        apiclient.deleteServiceOffering(cmd)
        return


class DiskOffering:
    """Manage disk offerings cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, custom=False):
        """Create Disk offering"""
        cmd = createDiskOffering.createDiskOfferingCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = services["name"]
        if custom:
            cmd.customized = True
        else:
            cmd.disksize = services["disksize"]
        return DiskOffering(apiclient.createDiskOffering(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Disk offering"""
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
        """Create Snapshot policy"""
        cmd = createSnapshotPolicy.createSnapshotPolicyCmd()
        cmd.intervaltype = services["intervaltype"]
        cmd.maxsnaps = services["maxsnaps"]
        cmd.schedule = services["schedule"]
        cmd.timezone = services["timezone"]
        cmd.volumeid = volumeid
        return SnapshotPolicy(apiclient.createSnapshotPolicy(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Snapshot policy"""
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
        """Create Load balancing Rule"""

        cmd = createLoadBalancerRule.createLoadBalancerRuleCmd()
        cmd.publicipid = ipaddressid or services["ipaddressid"]
        cmd.account = accountid or services["account"]
        cmd.name = services["name"]
        cmd.algorithm = services["alg"]
        cmd.privateport = services["privateport"]
        cmd.publicport = services["publicport"]
        return LoadBalancerRule(apiclient.createLoadBalancerRule(cmd).__dict__)

    def delete(self, apiclient):
        """Delete load balancing rule"""
        cmd = deleteLoadBalancerRule.deleteLoadBalancerRuleCmd()
        cmd.id = self.id
        apiclient.deleteLoadBalancerRule(cmd)
        return

    def assign(self, apiclient, vms):
        """Assign virtual machines to load balancing rule"""
        cmd = assignToLoadBalancerRule.assignToLoadBalancerRuleCmd()
        cmd.id = self.id
        cmd.virtualmachineids = [str(vm.id) for vm in vms]
        apiclient.assignToLoadBalancerRule(cmd)
        return

    def remove(self, apiclient, vms):
        """Remove virtual machines from load balancing rule"""
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
        """Create Cluster"""
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
        """Delete Cluster"""
        cmd = deleteCluster.deleteClusterCmd()
        cmd.id = self.id
        apiclient.deleteCluster(cmd)
        return


class Host:
    """Manage Host life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, cluster, services):
        """Create Host"""
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

        return Host(apiclient.addHost(cmd)[0].__dict__)

    def delete(self, apiclient):
        """Delete Host"""
        # Host must be in maintenance mode before deletion
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = self.id
        apiclient.prepareHostForMaintenance(cmd)
        time.sleep(60)

        cmd = deleteHost.deleteHostCmd()
        cmd.id = self.id
        apiclient.deleteHost(cmd)
        return


class StoragePool:
    """Manage Storage pools"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, clusterid=None):
        """Create Storage pool"""

        cmd = createStoragePool.createStoragePoolCmd()
        cmd.name = services["name"]
        cmd.podid = services["podid"]
        cmd.url = services["url"]
        if clusterid:
            cmd.clusterid = clusterid
        elif "clusterid" in services:
            cmd.clusterid = services["clusterid"]
        cmd.zoneid = services["zoneid"]

        return StoragePool(apiclient.createStoragePool(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Storage pool"""

        # Storage pool must be in maintenance mode before deletion
        cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
        cmd.id = self.id
        apiclient.enableStorageMaintenance(cmd)
        time.sleep(60)
        cmd = deleteStoragePool.deleteStoragePoolCmd()
        cmd.id = self.id
        apiclient.deleteStoragePool(cmd)
        return

class Network:
    """Manage Network pools"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, accountid=None, domainid=None):
        """Create Network for account"""
        cmd = createNetwork.createNetworkCmd()
        cmd.name = services["name"]
        cmd.displaytext = services["displaytext"]
        cmd.networkofferingid = services["networkoffering"]
        cmd.zoneid = services["zoneid"]
        if accountid:
            cmd.account = accountid
        if domainid:
            cmd.domainid = domainid

        return Network(apiclient.createNetwork(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Account"""

        cmd = deleteNetwork.deleteNetworkCmd()
        cmd.id = self.id
        apiclient.deleteNetwork(cmd)


class Vpn:
    """Manage Network pools"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, publicipid, account=None, domainid=None):
        """Create VPN for Public IP"""
        cmd = createRemoteAccessVpn.createRemoteAccessVpnCmd()
        cmd.publicipid = publicipid
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid

        return Vpn(apiclient.createRemoteAccessVpn(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Account"""

        cmd = deleteRemoteAccessVpn.deleteRemoteAccessVpnCmd()
        cmd.publicipid = self.publicipid
        apiclient.deleteRemoteAccessVpn(cmd)


class VpnUser:
    """Manage Network pools"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, username, password, account=None, domainid=None):
        """Create VPN for Public IP"""
        cmd = addVpnUser.addVpnUserCmd()
        cmd.username = username
        cmd.password = password

        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid

        return VpnUser(apiclient.addVpnUser(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Account"""

        cmd = removeVpnUser.removeVpnUserCmd()
        cmd.username = self.username
        apiclient.removeVpnUser(cmd)
