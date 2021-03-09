# -*- encoding: utf-8 -*-
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
#   http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

""" Base class for all Cloudstack resources
    -Virtual machine, Volume, Snapshot etc
"""

from .utils import is_server_ssh_ready, random_gen
from marvin.cloudstackAPI import *
#Import System modules
import time
import hashlib
import base64
import types


class Domain:
    """ Domain Life Cycle """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, name=None, networkdomain=None,
               parentdomainid=None):
        """Creates an domain"""

        cmd = createDomain.createDomainCmd()

        if name:
            cmd.name = "-".join([name, random_gen()])
        elif "name" in services:
            cmd.name = "-".join([services["name"], random_gen()])

        if networkdomain:
            cmd.networkdomain = networkdomain
        elif "networkdomain" in services:
            cmd.networkdomain = services["networkdomain"]

        if parentdomainid:
            cmd.parentdomainid = parentdomainid
        elif "parentdomainid" in services:
            cmd.parentdomainid = services["parentdomainid"]

        return Domain(apiclient.createDomain(cmd).__dict__)

    def delete(self, apiclient, cleanup=None):
        """Delete an domain"""
        cmd = deleteDomain.deleteDomainCmd()
        cmd.id = self.id
        if cleanup:
            cmd.cleanup = cleanup
        apiclient.deleteDomain(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists domains"""
        cmd = listDomains.listDomainsCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listDomains(cmd))


class Account:
    """ Account Life Cycle """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, admin=False, domainid=None):
        """Creates an account"""
        cmd = createAccount.createAccountCmd()

        #0 - User, 1 - Root Admin, 2 - Domain Admin
        cmd.accounttype = 2 if (admin and domainid) else int(admin)

        cmd.email = services["email"]
        cmd.firstname = services["firstname"]
        cmd.lastname = services["lastname"]

        # Password Encoding
        mdf = hashlib.md5()
        mdf.update(services["password"])
        cmd.password = mdf.hexdigest()
        cmd.username = "-".join([services["username"], random_gen()])

        if domainid:
            cmd.domainid = domainid
        account = apiclient.createAccount(cmd)

        return Account(account.__dict__)

    def delete(self, apiclient):
        """Delete an account"""
        cmd = deleteAccount.deleteAccountCmd()
        cmd.id = self.account.id
        apiclient.deleteAccount(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists accounts and provides detailed account information for
        listed accounts"""

        cmd = listAccounts.listAccountsCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listAccounts(cmd))


class User:
    """ User Life Cycle """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, account, domainid):
        cmd = createUser.createUserCmd()
        """Creates an user"""

        cmd.account = account
        cmd.domainid = domainid
        cmd.email = services["email"]
        cmd.firstname = services["firstname"]
        cmd.lastname = services["lastname"]

        # Password Encoding
        mdf = hashlib.md5()
        mdf.update(services["password"])
        cmd.password = mdf.hexdigest()
        cmd.username = "-".join([services["username"], random_gen()])
        user = apiclient.createUser(cmd)

        return User(user.__dict__)

    def delete(self, apiclient):
        """Delete an account"""
        cmd = deleteUser.deleteUserCmd()
        cmd.id = self.id
        apiclient.deleteUser(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists users and provides detailed account information for
        listed users"""

        cmd = listUsers.listUsersCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listUsers(cmd))


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
                    domainid=None, networkids=None, serviceofferingid=None,
                    securitygroupids=None, mode='basic'):
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

        if domainid:
            cmd.domainid = domainid
        elif "domainid" in services:
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
        
        if securitygroupids:
            cmd.securitygroupids = [str(sg_id) for sg_id in securitygroupids]
        
        if "userdata" in services:
            cmd.userdata = base64.b64encode(services["userdata"])
        
        virtual_machine = apiclient.deployVirtualMachine(cmd)
        
        # VM should be in Running state after deploy
        timeout = 10
        while True:    
            vm_status = VirtualMachine.list(
                                            apiclient,
                                            id=virtual_machine.id
                                            )
            if isinstance(vm_status, list):
                if vm_status[0].state == 'Running':
                    break
                elif timeout == 0:
                    raise Exception(
                            "TimeOutException: Failed to start VM (ID: %s)" % 
                                                        virtual_machine.id)
            
            time.sleep(10)
            timeout = timeout -1

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

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all VMs matching criteria"""

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listVirtualMachines(cmd))


class Volume:
    """Manage Volume Lifecycle
    """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, zoneid=None, account=None, domainid=None,
               diskofferingid=None):
        """Create Volume"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = services["diskname"]

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

        if domainid:
            cmd.domainid = domainid
        elif "domainid" in services:
            cmd.domainid = services["domainid"]

        return Volume(apiclient.createVolume(cmd).__dict__)

    @classmethod
    def create_custom_disk(cls, apiclient, services, account=None, domainid=None):
        """Create Volume from Custom disk offering"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = services["diskname"]
        cmd.diskofferingid = services["customdiskofferingid"]
        cmd.size = services["customdisksize"]
        cmd.zoneid = services["zoneid"]
        
        if account:
            cmd.account = account
        else:
            cmd.account = services["account"]
        
        if domainid:
            cmd.domainid = domainid
        else:
            cmd.domainid = services["domainid"]
            
        return Volume(apiclient.createVolume(cmd).__dict__)

    @classmethod
    def create_from_snapshot(cls, apiclient, snapshot_id, services,
                             account=None, domainid=None):
        """Create Volume from snapshot"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = "-".join([services["diskname"], random_gen()])
        cmd.snapshotid = snapshot_id
        cmd.zoneid = services["zoneid"]
        cmd.size = services["size"]
        if account:
            cmd.account = account
        else:
            cmd.account = services["account"]
        if domainid:
            cmd.domainid = domainid
        else:
            cmd.domainid = services["domainid"]
        return Volume(apiclient.createVolume(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Volume"""
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.id
        apiclient.deleteVolume(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all volumes matching criteria"""

        cmd = listVolumes.listVolumesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listVolumes(cmd))


class Snapshot:
    """Manage Snapshot Lifecycle
    """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, volume_id, account=None, domainid=None):
        """Create Snapshot"""
        cmd = createSnapshot.createSnapshotCmd()
        cmd.volumeid = volume_id
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        return Snapshot(apiclient.createSnapshot(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Snapshot"""
        cmd = deleteSnapshot.deleteSnapshotCmd()
        cmd.id = self.id
        apiclient.deleteSnapshot(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all snapshots matching criteria"""

        cmd = listSnapshots.listSnapshotsCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listSnapshots(cmd))


class Template:
    """Manage template life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, volumeid=None,
               account=None, domainid=None):
        """Create template from Volume"""
        #Create template from Virtual machine and Volume ID
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([services["name"], random_gen()])
        cmd.ostypeid = services["ostypeid"]

        cmd.isfeatured = services["isfeatured"] if "isfeatured" in services else False
        cmd.ispublic = services["ispublic"] if "ispublic" in services else False
        cmd.isextractable = services["isextractable"] if "isextractable" in services else False
        cmd.passwordenabled = services["passwordenabled"] if "passwordenabled" in services else False
        
        if volumeid:
            cmd.volumeid = volumeid

        if account:
            cmd.account = account

        if domainid:
            cmd.domainid = domainid

        return Template(apiclient.createTemplate(cmd).__dict__)

    @classmethod
    def register(cls, apiclient, services, zoneid=None, account=None, domainid=None):
        """Create template from URL"""
        
        #Create template from Virtual machine and Volume ID
        cmd = registerTemplate.registerTemplateCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([services["name"], random_gen()])
        cmd.format = services["format"]
        cmd.hypervisor = services["hypervisor"]
        cmd.ostypeid = services["ostypeid"]
        cmd.url = services["url"]
        
        if zoneid:
            cmd.zoneid = zoneid
        else:
            cmd.zoneid = services["zoneid"]

        cmd.isfeatured = services["isfeatured"] if "isfeatured" in services else False
        cmd.ispublic = services["ispublic"] if "ispublic" in services else False
        cmd.isextractable = services["isextractable"] if "isextractable" in services else False

        if account:
            cmd.account = account

        if domainid:
            cmd.domainid = domainid
        
        # Register Template
        template = apiclient.registerTemplate(cmd)
    
        if isinstance(template, list):
            return Template(template[0].__dict__)

    @classmethod
    def create_from_snapshot(cls, apiclient, snapshot, services, random_name=True):
        """Create Template from snapshot"""
        #Create template from Virtual machine and Snapshot ID
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([
                             services["name"], 
                             random_gen()
                            ]) if random_name else services["name"]
        cmd.ostypeid = services["ostypeid"]
        cmd.snapshotid = snapshot.id
        return Template(apiclient.createTemplate(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Template"""
        
        cmd = deleteTemplate.deleteTemplateCmd()
        cmd.id = self.id
        apiclient.deleteTemplate(cmd)

    def download(self, apiclient, timeout=5, interval=60):
        """Download Template"""
        #Sleep to ensure template is in proper state before download
        time.sleep(interval)
        
        while True:
            template_response = Template.list(
                                    apiclient,
                                    id=self.id,
                                    zoneid=self.zoneid,
                                    templatefilter='self'
                                    )
            if isinstance(template_response, list):
                
                template = template_response[0]
                # If template is ready,
                # template.status = Download Complete
                # Downloading - x% Downloaded
                # Error - Any other string 
                if template.status == 'Download Complete':
                    break
                
                elif 'Downloaded' in template.status:
                    time.sleep(interval)

                elif 'Installing' not in template.status:
                    raise Exception("ErrorInDownload")

            elif timeout == 0:
                break
            
            else:
                time.sleep(interval)
                timeout = timeout - 1
        return
                
    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all templates matching criteria"""

        cmd = listTemplates.listTemplatesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listTemplates(cmd))


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
        # Register ISO
        iso = apiclient.registerIso(cmd)
        
        if iso:
            return Iso(iso[0].__dict__)

    def delete(self, apiclient):
        """Delete an ISO"""
        cmd = deleteIso.deleteIsoCmd()
        cmd.id = self.id
        apiclient.deleteIso(cmd)
        return

    def download(self, apiclient, timeout=5, interval=60):
        """Download an ISO"""
        #Ensuring ISO is successfully downloaded
        while True:
            time.sleep(interval)

            cmd = listIsos.listIsosCmd()
            cmd.id = self.id
            iso_response = apiclient.listIsos(cmd)
            
            if isinstance(iso_response, list):     
                response = iso_response[0]
                # Again initialize timeout to avoid listISO failure
                timeout = 5

                # Check whether download is in progress(for Ex:10% Downloaded)
                # or ISO is 'Successfully Installed'
                if response.status == 'Successfully Installed':
                    return

                elif 'Downloaded' in response.status:
                    time.sleep(interval)

                elif 'Installing' not in response.status:
                    raise Exception("ErrorInDownload")

            elif timeout == 0:
                raise Exception("TimeoutException")
            else:
                timeout = timeout - 1
        return

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all available ISO files."""

        cmd = listIsos.listIsosCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listIsos(cmd))


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

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Public IPs matching criteria"""

        cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listPublicIpAddresses(cmd))

class NATRule:
    """Manage port forwarding rule"""

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

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all NAT rules matching criteria"""

        cmd = listPortForwardingRules.listPortForwardingRulesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listPortForwardingRules(cmd))
    

class StaticNATRule:
    """Manage Static NAT rule"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, ipaddressid=None):
        """Creates static ip forwarding rule"""
        
        cmd = createIpForwardingRule.createIpForwardingRuleCmd()
        cmd.protocol = services["protocol"]
        cmd.startport = services["startport"]
        
        if "endport" in services:
            cmd.endport = services["endport"]
            
        if "cidrlist" in services:
            cmd.cidrlist = services["cidrlist"]
        
        if ipaddressid:
            cmd.ipaddressid = ipaddressid
        elif "ipaddressid" in services:
            cmd.ipaddressid = services["ipaddressid"]
            
        return StaticNATRule(apiclient.createIpForwardingRule(cmd).__dict__)

    def delete(self, apiclient):
        """Delete IP forwarding rule"""
        cmd = deleteIpForwardingRule.deleteIpForwardingRuleCmd()
        cmd.id = self.id
        apiclient.deleteIpForwardingRule(cmd)
        return

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all IP forwarding rules matching criteria"""

        cmd = listIpForwardingRules.listIpForwardingRulesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listIpForwardingRules(cmd))
    
    @classmethod
    def enable(cls, apiclient, ipaddressid, virtualmachineid):
        """Enables Static NAT rule"""
        
        cmd = enableStaticNat.enableStaticNatCmd()
        cmd.ipaddressid = ipaddressid
        cmd.virtualmachineid = virtualmachineid
        apiclient.enableStaticNat(cmd)
        return
        
    @classmethod
    def disable(cls, apiclient, ipaddressid, virtualmachineid):
        """Disables Static NAT rule"""
        
        cmd = disableStaticNat.disableStaticNatCmd()
        cmd.ipaddressid = ipaddressid
        apiclient.disableStaticNat(cmd)
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

        return FireWallRule(apiclient.createFirewallRule(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Firewall rule"""
        cmd = deleteFirewallRule.deleteFirewallRuleCmd()
        cmd.id = self.id
        apiclient.deleteFirewallRule(cmd)
        return

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Firewall Rules matching criteria"""

        cmd = listFirewallRules.listFirewallRulesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listFirewallRules(cmd))


class ServiceOffering:
    """Manage service offerings cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, domainid=None):
        """Create Service offering"""
        cmd = createServiceOffering.createServiceOfferingCmd()
        cmd.cpunumber = services["cpunumber"]
        cmd.cpuspeed = services["cpuspeed"]
        cmd.displaytext = services["displaytext"]
        cmd.memory = services["memory"]
        cmd.name = services["name"]

        # Service Offering private to that domain
        if domainid:
            cmd.domainid = domainid

        return ServiceOffering(apiclient.createServiceOffering(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Service offering"""
        cmd = deleteServiceOffering.deleteServiceOfferingCmd()
        cmd.id = self.id
        apiclient.deleteServiceOffering(cmd)
        return

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all available service offerings."""

        cmd = listServiceOfferings.listServiceOfferingsCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listServiceOfferings(cmd))

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

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all available disk offerings."""

        cmd = listDiskOfferings.listDiskOfferingsCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listDiskOfferings(cmd))


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

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists snapshot policies."""

        cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listSnapshotPolicies(cmd))


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
        cmd.id = self.id
        cmd.virtualmachineids = [str(vm.id) for vm in vms]
        apiclient.removeFromLoadBalancerRule(cmd)
        return

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Load balancing rules matching criteria"""

        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listLoadBalancerRules(cmd))


class Cluster:
    """Manage Cluster life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, zoneid=None, podid=None):
        """Create Cluster"""
        cmd = addCluster.addClusterCmd()
        cmd.clustertype = services["clustertype"]
        cmd.hypervisor = services["hypervisor"]
        
        if zoneid:
            cmd.zoneid = zoneid
        else:
            cmd.zoneid = services["zoneid"]
        
        if podid:
            cmd.podid = podid
        else:
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

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Clusters matching criteria"""

        cmd = listClusters.listClustersCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listClusters(cmd))


class Host:
    """Manage Host life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, cluster, services, zoneid=None, podid=None):
        """Create Host in cluster"""
        
        cmd = addHost.addHostCmd()
        cmd.hypervisor = services["hypervisor"]
        cmd.url = services["url"]
        cmd.clusterid = cluster.id
        
        if zoneid:
            cmd.zoneid = zoneid
        else:
            cmd.zoneid = services["zoneid"]
        
        if podid:
            cmd.podid = podid
        else:
            cmd.podid = services["podid"]

        if "clustertype" in services:
            cmd.clustertype = services["clustertype"]
        if "username" in services:
            cmd.username = services["username"]
        if "password" in services:
            cmd.password = services["password"]
        
        # Add host
        host = apiclient.addHost(cmd)
        
        if isinstance(host, list):
            return Host(host[0].__dict__)

    def delete(self, apiclient):
        """Delete Host"""
        # Host must be in maintenance mode before deletion
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = self.id
        apiclient.prepareHostForMaintenance(cmd)
        time.sleep(30)

        cmd = deleteHost.deleteHostCmd()
        cmd.id = self.id
        apiclient.deleteHost(cmd)
        return

    def enableMaintenance(self, apiclient):
        """enables maintenance mode Host"""

        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = self.id
        return apiclient.prepareHostForMaintenance(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Hosts matching criteria"""

        cmd = listHosts.listHostsCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listHosts(cmd))


class StoragePool:
    """Manage Storage pools (Primary Storage)"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, clusterid=None, zoneid=None, podid=None):
        """Create Storage pool (Primary Storage)"""

        cmd = createStoragePool.createStoragePoolCmd()
        cmd.name = services["name"]
        
        if podid:
            cmd.podid = podid
        else:
            cmd.podid = services["podid"]
            
        cmd.url = services["url"]
        if clusterid:
            cmd.clusterid = clusterid
        elif "clusterid" in services:
            cmd.clusterid = services["clusterid"]
        
        if zoneid:
            cmd.zoneid = zoneid
        else:
            cmd.zoneid = services["zoneid"]

        return StoragePool(apiclient.createStoragePool(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Storage pool (Primary Storage)"""

        # Storage pool must be in maintenance mode before deletion
        cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
        cmd.id = self.id
        apiclient.enableStorageMaintenance(cmd)
        time.sleep(30)
        cmd = deleteStoragePool.deleteStoragePoolCmd()
        cmd.id = self.id
        apiclient.deleteStoragePool(cmd)
        return

    def enableMaintenance(self, apiclient):
        """enables maintenance mode Storage pool"""

        cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
        cmd.id = self.id
        return apiclient.enableStorageMaintenance(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all storage pools matching criteria"""

        cmd = listStoragePools.listStoragePoolsCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listStoragePools(cmd))


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

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Networks matching criteria"""

        cmd = listNetworks.listNetworksCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listNetworks(cmd))


class Vpn:
    """Manage VPN life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, publicipid, account=None, domainid=None):
        """Create VPN for Public IP address"""
        cmd = createRemoteAccessVpn.createRemoteAccessVpnCmd()
        cmd.publicipid = publicipid
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid

        return Vpn(apiclient.createRemoteAccessVpn(cmd).__dict__)

    def delete(self, apiclient):
        """Delete remote VPN access"""

        cmd = deleteRemoteAccessVpn.deleteRemoteAccessVpnCmd()
        cmd.publicipid = self.publicipid
        apiclient.deleteRemoteAccessVpn(cmd)


class VpnUser:
    """Manage VPN user"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, username, password, account=None, domainid=None):
        """Create VPN user"""
        cmd = addVpnUser.addVpnUserCmd()
        cmd.username = username
        cmd.password = password

        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid

        return VpnUser(apiclient.addVpnUser(cmd).__dict__)

    def delete(self, apiclient):
        """Remove VPN user"""

        cmd = removeVpnUser.removeVpnUserCmd()
        cmd.username = self.username
        cmd.account = self.account
        cmd.domainid = self.domainid
        apiclient.removeVpnUser(cmd)


class Zone:
    """Manage Zone"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, domainid=None):
        """Create zone"""
        cmd = createZone.createZoneCmd()
        cmd.dns1 = services["dns1"]
        cmd.internaldns1 = services["internaldns1"]
        cmd.name = services["name"]
        cmd.networktype = services["networktype"]

        if "dns2" in services:
            cmd.dns2 = services["dns2"]
        if "internaldns2" in services:
            cmd.internaldns2 = services["internaldns2"]
        if domainid:
            cmd.domainid = domainid

        return Zone(apiclient.createZone(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Zone"""

        cmd = deleteZone.deleteZoneCmd()
        cmd.id = self.id
        apiclient.deleteZone(cmd)

    def update(self, apiclient, **kwargs):
        """Update the zone"""
        
        cmd = updateZone.updateZoneCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return apiclient.updateZone(cmd)
        
        
    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Zones matching criteria"""

        cmd = listZones.listZonesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listZones(cmd))


class Pod:
    """Manage Pod"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):
        """Create Pod"""
        cmd = createPod.createPodCmd()
        cmd.gateway = services["gateway"]
        cmd.netmask = services["netmask"]
        cmd.name = services["name"]
        cmd.startip = services["startip"]
        cmd.endip = services["endip"]
        cmd.zoneid = services["zoneid"]

        return Pod(apiclient.createPod(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Pod"""

        cmd = deletePod.deletePodCmd()
        cmd.id = self.id
        apiclient.deletePod(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        "Returns a default pod for specified zone"

        cmd = listPods.listPodsCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return apiclient.listPods(cmd)


class PublicIpRange:
    """Manage VlanIpRange"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):
        """Create VlanIpRange"""
        
        cmd = createVlanIpRange.createVlanIpRangeCmd()
        cmd.gateway = services["gateway"]
        cmd.netmask = services["netmask"]
        cmd.forvirtualnetwork = services["forvirtualnetwork"]
        cmd.startip = services["startip"]
        cmd.endip = services["endip"]
        cmd.zoneid = services["zoneid"]
        cmd.podid = services["podid"]
        cmd.vlan = services["vlan"]

        return PublicIpRange(apiclient.createVlanIpRange(cmd).__dict__)

    def delete(self, apiclient):
        """Delete VlanIpRange"""

        cmd = deleteVlanIpRange.deleteVlanIpRangeCmd()
        cmd.id = self.id
        apiclient.deleteVlanIpRange(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all VLAN IP ranges."""

        cmd = listVlanIpRanges.listVlanIpRangesCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listVlanIpRanges(cmd))


class SecondaryStorage:
    """Manage Secondary storage"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):
        """Create Secondary Storage"""
        cmd = addSecondaryStorage.addSecondaryStorageCmd()

        cmd.url = services["url"]
        if "zoneid" in services:
            cmd.zoneid = services["zoneid"]
        return SecondaryStorage(apiclient.addSecondaryStorage(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Secondary Storage"""

        cmd = deleteHost.deleteHostCmd()
        cmd.id = self.id
        apiclient.deleteHost(cmd)
        

class SecurityGroup:
    """Manage Security Groups"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, account=None, domainid=None,
               description=None):
        """Create security group"""
        cmd = createSecurityGroup.createSecurityGroupCmd()

        cmd.name = services["name"]
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid=domainid
        if description:
            cmd.description=description
            
        return SecurityGroup(apiclient.createSecurityGroup(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Security Group"""

        cmd = deleteSecurityGroup.deleteSecurityGroupCmd()
        cmd.id = self.id
        apiclient.deleteSecurityGroup(cmd)
        
    def authorize(self, apiclient, services,
                  account=None, domainid=None):
        """Authorize Ingress Rule"""
        
        cmd=authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        
        if domainid:
            cmd.domainid = domainid
        if account:
            cmd.account = account
                
        cmd.securitygroupid=self.id
        cmd.protocol=services["protocol"]
        
        if services["protocol"] == 'ICMP':
            cmd.icmptype = -1
            cmd.icmpcode = -1
        else:
            cmd.startport = services["startport"]
            cmd.endport = services["endport"]
        
        cmd.cidrlist = services["cidrlist"]
        return (apiclient.authorizeSecurityGroupIngress(cmd).__dict__)
    
    def revoke(self, apiclient, id):
        """Revoke ingress rule"""
        
        cmd=revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id=id
        return apiclient.revokeSecurityGroupIngress(cmd)
    
    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all security groups."""

        cmd = listSecurityGroups.listSecurityGroupsCmd()
        [setattr(cmd, k, v) for k, v in list(kwargs.items())]
        return(apiclient.listSecurityGroups(cmd))
