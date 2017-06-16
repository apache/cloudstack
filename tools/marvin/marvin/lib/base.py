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

import marvin
from marvin.cloudstackAPI import *
from marvin.codes import (FAILED, FAIL, PASS, RUNNING, STOPPED,
                          STARTING, DESTROYED, EXPUNGING,
                          STOPPING, BACKED_UP, BACKING_UP,
                          HOST_RS_MAINTENANCE)
from marvin.cloudstackException import GetDetailExceptionInfo, CloudstackAPIException
from marvin.lib.utils import validateList, is_server_ssh_ready, random_gen, wait_until
# Import System modules
import time
import hashlib
import base64

class Domain:
    """ Domain Life Cycle """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, name=None, networkdomain=None,
               parentdomainid=None):
        """Creates an domain"""

        cmd = createDomain.createDomainCmd()

        if "domainUUID" in services:
            cmd.domainid = "-".join([services["domainUUID"], random_gen()])

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
        try:
            domain = apiclient.createDomain(cmd)
            if domain is not None:
                return Domain(domain.__dict__)
        except Exception as e:
            raise e

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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listDomains(cmd))


class Role:
    """Manage Role"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, domainid=None):
        """Create role"""
        cmd = createRole.createRoleCmd()
        cmd.name = services["name"]
        cmd.type = services["type"]
        if "description" in services:
            cmd.description = services["description"]

        return Role(apiclient.createRole(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Role"""

        cmd = deleteRole.deleteRoleCmd()
        cmd.id = self.id
        apiclient.deleteRole(cmd)

    def update(self, apiclient, **kwargs):
        """Update the role"""

        cmd = updateRole.updateRoleCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updateRole(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Roles matching criteria"""

        cmd = listRoles.listRolesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listRoles(cmd))


class RolePermission:
    """Manage Role Permission"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, domainid=None):
        """Create role permission"""
        cmd = createRolePermission.createRolePermissionCmd()
        cmd.roleid = services["roleid"]
        cmd.rule = services["rule"]
        cmd.permission = services["permission"]
        if "description" in services:
            cmd.description = services["description"]

        return RolePermission(apiclient.createRolePermission(cmd).__dict__)

    def delete(self, apiclient):
        """Delete role permission"""

        cmd = deleteRolePermission.deleteRolePermissionCmd()
        cmd.id = self.id
        apiclient.deleteRolePermission(cmd)

    def update(self, apiclient, **kwargs):
        """Update the role permission"""

        cmd = updateRolePermission.updateRolePermissionCmd()
        cmd.roleid = self.roleid
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updateRolePermission(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all role permissions matching criteria"""

        cmd = listRolePermissions.listRolePermissionsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listRolePermissions(cmd))


class Account:
    """ Account Life Cycle """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, admin=False, domainid=None, roleid=None):
        """Creates an account"""
        cmd = createAccount.createAccountCmd()

        # 0 - User, 1 - Root Admin, 2 - Domain Admin
        cmd.accounttype = 2 if (admin and domainid) else int(admin)

        cmd.email = services["email"]
        cmd.firstname = services["firstname"]
        cmd.lastname = services["lastname"]

        cmd.password = services["password"]
        username = services["username"]
        # Limit account username to 99 chars to avoid failure
        # 6 chars start string + 85 chars apiclientid + 6 chars random string + 2 chars joining hyphen string = 99
        username = username[:6]
        apiclientid = apiclient.id[-85:] if len(apiclient.id) > 85 else apiclient.id
        cmd.username = "-".join([username,
                             random_gen(id=apiclientid, size=6)])

        if "accountUUID" in services:
            cmd.accountid = "-".join([services["accountUUID"], random_gen()])

        if "userUUID" in services:
            cmd.userid = "-".join([services["userUUID"], random_gen()])

        if domainid:
            cmd.domainid = domainid

        if roleid:
            cmd.roleid = roleid

        account = apiclient.createAccount(cmd)

        return Account(account.__dict__)

    def delete(self, apiclient):
        """Delete an account"""
        cmd = deleteAccount.deleteAccountCmd()
        cmd.id = self.id
        apiclient.deleteAccount(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists accounts and provides detailed account information for
        listed accounts"""

        cmd = listAccounts.listAccountsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listAccounts(cmd))

    def disable(self, apiclient, lock=False):
        """Disable an account"""
        cmd = disableAccount.disableAccountCmd()
        cmd.id = self.id
        cmd.lock = lock
        apiclient.disableAccount(cmd)


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

        if "userUUID" in services:
            cmd.userid = "-".join([services["userUUID"], random_gen()])

        cmd.password = services["password"]
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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listUsers(cmd))

    @classmethod
    def registerUserKeys(cls, apiclient, userid):
        cmd = registerUserKeys.registerUserKeysCmd()
        cmd.id = userid
        return apiclient.registerUserKeys(cmd)

    def update(self, apiclient, **kwargs):
        """Updates the user details"""

        cmd = updateUser.updateUserCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return (apiclient.updateUser(cmd))

    @classmethod
    def update(cls, apiclient, id, **kwargs):
        """Updates the user details (class method)"""

        cmd = updateUser.updateUserCmd()
        cmd.id = id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return (apiclient.updateUser(cmd))

    @classmethod
    def login(cls, apiclient, username, password, domain=None, domainid=None):
        """Logins to the CloudStack"""

        cmd = login.loginCmd()
        cmd.username = username
        cmd.password = password
        if domain:
            cmd.domain = domain
        if domainid:
            cmd.domainId = domainid
        return apiclient.login(cmd)


class VirtualMachine:
    """Manage virtual machine lifecycle"""

    '''Class level variables'''
    # Variables denoting VM state - start
    STOPPED = STOPPED
    RUNNING = RUNNING
    DESTROYED = DESTROYED
    EXPUNGING = EXPUNGING
    STOPPING = STOPPING
    STARTING = STARTING
    # Varibles denoting VM state - end

    def __init__(self, items, services):
        self.__dict__.update(items)
        if "username" in services:
            self.username = services["username"]
        else:
            self.username = 'root'
        if "password" in services:
            self.password = services["password"]
        else:
            self.password = 'password'
        if "ssh_port" in services:
            self.ssh_port = services["ssh_port"]
        else:
            self.ssh_port = 22
        self.ssh_client = None
        # extract out the ipaddress
        self.ipaddress = self.nic[0].ipaddress

    @classmethod
    def ssh_access_group(cls, apiclient, cmd):
        """
        Programs the security group with SSH
         access before deploying virtualmachine
        @return:
        """
        zone_list = Zone.list(
            apiclient,
            id=cmd.zoneid if cmd.zoneid else None,
            domainid=cmd.domainid if cmd.domainid else None
        )
        zone = zone_list[0]
        # check if security groups settings is enabled for the zone
        if zone.securitygroupsenabled:
            list_security_groups = SecurityGroup.list(
                apiclient,
                account=cmd.account,
                domainid=cmd.domainid,
                listall=True,
                securitygroupname="basic_sec_grp"
            )

            if not isinstance(list_security_groups, list):
                basic_mode_security_group = SecurityGroup.create(
                    apiclient,
                    {"name": "basic_sec_grp"},
                    cmd.account,
                    cmd.domainid,
                )
                sec_grp_services = {
                    "protocol": "TCP",
                    "startport": 22,
                    "endport": 22,
                    "cidrlist": "0.0.0.0/0"
                }
                # Authorize security group for above ingress rule
                basic_mode_security_group.authorize(apiclient,
                                                    sec_grp_services,
                                                    account=cmd.account,
                                                    domainid=cmd.domainid)
            else:
                basic_mode_security_group = list_security_groups[0]

            if isinstance(cmd.securitygroupids, list):
                cmd.securitygroupids.append(basic_mode_security_group.id)
            else:
                cmd.securitygroupids = [basic_mode_security_group.id]

    @classmethod
    def access_ssh_over_nat(
            cls, apiclient, services, virtual_machine, allow_egress=False,
            networkid=None):
        """
        Program NAT and PF rules to open up ssh access to deployed guest
        @return:
        """
        public_ip = PublicIPAddress.create(
            apiclient=apiclient,
            accountid=virtual_machine.account,
            zoneid=virtual_machine.zoneid,
            domainid=virtual_machine.domainid,
            services=services,
            networkid=networkid
        )
        FireWallRule.create(
            apiclient=apiclient,
            ipaddressid=public_ip.ipaddress.id,
            protocol='TCP',
            cidrlist=['0.0.0.0/0'],
            startport=22,
            endport=22
        )
        nat_rule = NATRule.create(
            apiclient=apiclient,
            virtual_machine=virtual_machine,
            services=services,
            ipaddressid=public_ip.ipaddress.id
        )
        if allow_egress:
            try:
                EgressFireWallRule.create(
                    apiclient=apiclient,
                    networkid=virtual_machine.nic[0].networkid,
                    protocol='All',
                    cidrlist='0.0.0.0/0'
                )
            except CloudstackAPIException, e:
                # This could fail because we've already set up the same rule
                if not "There is already a firewall rule specified".lower() in e.errorMsg.lower():
                    raise
        virtual_machine.ssh_ip = nat_rule.ipaddress
        virtual_machine.public_ip = nat_rule.ipaddress

    @classmethod
    def create(cls, apiclient, services, templateid=None, accountid=None,
               domainid=None, zoneid=None, networkids=None,
               serviceofferingid=None, securitygroupids=None,
               projectid=None, startvm=None, diskofferingid=None,
               affinitygroupnames=None, affinitygroupids=None, group=None,
               hostid=None, keypair=None, ipaddress=None, mode='default',
               method='GET', hypervisor=None, customcpunumber=None,
               customcpuspeed=None, custommemory=None, rootdisksize=None,
               rootdiskcontroller=None):
        """Create the instance"""

        cmd = deployVirtualMachine.deployVirtualMachineCmd()

        if serviceofferingid:
            cmd.serviceofferingid = serviceofferingid
        elif "serviceoffering" in services:
            cmd.serviceofferingid = services["serviceoffering"]

        if zoneid:
            cmd.zoneid = zoneid
        elif "zoneid" in services:
            cmd.zoneid = services["zoneid"]

        if hypervisor:
            cmd.hypervisor = hypervisor

        if "displayname" in services:
            cmd.displayname = services["displayname"]

        if "name" in services:
            cmd.name = services["name"]

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
            allow_egress = False
        elif "networkids" in services:
            cmd.networkids = services["networkids"]
            allow_egress = False
        else:
            # When no networkids are passed, network
            # is created using the "defaultOfferingWithSourceNAT"
            # which has an egress policy of DENY. But guests in tests
            # need access to test network connectivity
            allow_egress = True

        if templateid:
            cmd.templateid = templateid
        elif "template" in services:
            cmd.templateid = services["template"]

        if diskofferingid:
            cmd.diskofferingid = diskofferingid
        elif "diskoffering" in services:
            cmd.diskofferingid = services["diskoffering"]

        if keypair:
            cmd.keypair = keypair
        elif "keypair" in services:
            cmd.keypair = services["keypair"]

        if ipaddress:
            cmd.ipaddress = ipaddress
        elif "ipaddress" in services:
            cmd.ipaddress = services["ipaddress"]

        if securitygroupids:
            cmd.securitygroupids = [str(sg_id) for sg_id in securitygroupids]

        if "affinitygroupnames" in services:
            cmd.affinitygroupnames = services["affinitygroupnames"]
        elif affinitygroupnames:
            cmd.affinitygroupnames = affinitygroupnames

        if affinitygroupids:
            cmd.affinitygroupids = affinitygroupids

        if projectid:
            cmd.projectid = projectid

        if startvm is not None:
            cmd.startvm = startvm

        if hostid:
            cmd.hostid = hostid

        if "userdata" in services:
            cmd.userdata = base64.urlsafe_b64encode(services["userdata"])

        cmd.details = [{}]

        if customcpunumber:
            cmd.details[0]["cpuNumber"] = customcpunumber

        if customcpuspeed:
            cmd.details[0]["cpuSpeed"] = customcpuspeed

        if custommemory:
            cmd.details[0]["memory"] = custommemory

        if rootdisksize >= 0:
            cmd.details[0]["rootdisksize"] = rootdisksize

        if rootdiskcontroller:
            cmd.details[0]["rootDiskController"] = rootdiskcontroller

        if group:
            cmd.group = group

        # program default access to ssh
        if mode.lower() == 'basic':
            cls.ssh_access_group(apiclient, cmd)

        virtual_machine = apiclient.deployVirtualMachine(cmd, method=method)

        virtual_machine.ssh_ip = virtual_machine.nic[0].ipaddress
        if startvm is False:
            virtual_machine.public_ip = virtual_machine.nic[0].ipaddress
            return VirtualMachine(virtual_machine.__dict__, services)

        # program ssh access over NAT via PF
        if mode.lower() == 'advanced':
            cls.access_ssh_over_nat(
                apiclient,
                services,
                virtual_machine,
                allow_egress=allow_egress,
                networkid=cmd.networkids[0] if cmd.networkids else None)
        elif mode.lower() == 'basic':
            if virtual_machine.publicip is not None:
                # EIP/ELB (netscaler) enabled zone
                vm_ssh_ip = virtual_machine.publicip
            else:
                # regular basic zone with security group
                vm_ssh_ip = virtual_machine.nic[0].ipaddress
            virtual_machine.ssh_ip = vm_ssh_ip
            virtual_machine.public_ip = vm_ssh_ip

        return VirtualMachine(virtual_machine.__dict__, services)

    def start(self, apiclient):
        """Start the instance"""
        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.id = self.id
        apiclient.startVirtualMachine(cmd)
        response = self.getState(apiclient, VirtualMachine.RUNNING)
        if response[0] == FAIL:
            raise Exception(response[1])
        return

    def stop(self, apiclient, forced=None):
        """Stop the instance"""
        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.id = self.id
        if forced:
            cmd.forced = forced
        apiclient.stopVirtualMachine(cmd)
        response = self.getState(apiclient, VirtualMachine.STOPPED)
        if response[0] == FAIL:
            raise Exception(response[1])
        return

    def reboot(self, apiclient):
        """Reboot the instance"""
        cmd = rebootVirtualMachine.rebootVirtualMachineCmd()
        cmd.id = self.id
        apiclient.rebootVirtualMachine(cmd)

        response = self.getState(apiclient, VirtualMachine.RUNNING)
        if response[0] == FAIL:
            raise Exception(response[1])

    def recover(self, apiclient):
        """Recover the instance"""
        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.id = self.id
        apiclient.recoverVirtualMachine(cmd)

        response = self.getState(apiclient, VirtualMachine.STOPPED)
        if response[0] == FAIL:
            raise Exception(response[1])

    def restore(self, apiclient, templateid=None):
        """Restore the instance"""
        cmd = restoreVirtualMachine.restoreVirtualMachineCmd()
        cmd.virtualmachineid = self.id
        if templateid:
            cmd.templateid = templateid
        return apiclient.restoreVirtualMachine(cmd)

    def get_ssh_client(
            self, ipaddress=None, reconnect=False, port=None,
            keyPairFileLocation=None, retries=20):
        """Get SSH object of VM"""

        # If NAT Rules are not created while VM deployment in Advanced mode
        # then, IP address must be passed
        if ipaddress is not None:
            self.ssh_ip = ipaddress
        if port:
            self.ssh_port = port

        if keyPairFileLocation is not None:
            self.password = None

        if reconnect:
            self.ssh_client = is_server_ssh_ready(
                self.ssh_ip,
                self.ssh_port,
                self.username,
                self.password,
                retries=retries,
                keyPairFileLocation=keyPairFileLocation
            )
        self.ssh_client = self.ssh_client or is_server_ssh_ready(
            self.ssh_ip,
            self.ssh_port,
            self.username,
            self.password,
            retries=retries,
            keyPairFileLocation=keyPairFileLocation
        )
        return self.ssh_client

    def getState(self, apiclient, state, timeout=600):
        """List VM and check if its state is as expected
        @returnValue - List[Result, Reason]
                       1) Result - FAIL if there is any exception
                       in the operation or VM state does not change
                       to expected state in given time else PASS
                       2) Reason - Reason for failure"""

        returnValue = [FAIL, "VM state not trasited to %s,\
                        operation timed out" % state]

        while timeout > 0:
            try:
                projectid = None
                if hasattr(self, "projectid"):
                    projectid = self.projectid
                vms = VirtualMachine.list(apiclient, projectid=projectid,
                        id=self.id, listAll=True)
                validationresult = validateList(vms)
                if validationresult[0] == FAIL:
                    raise Exception("VM list validation failed: %s" % validationresult[2])
                elif str(vms[0].state).lower().decode("string_escape") == str(state).lower():
                    returnValue = [PASS, None]
                    break
            except Exception as e:
                returnValue = [FAIL, e]
                break
            time.sleep(60)
            timeout -= 60
        return returnValue

    def resetSshKey(self, apiclient, **kwargs):
        """Resets SSH key"""

        cmd = resetSSHKeyForVirtualMachine.resetSSHKeyForVirtualMachineCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.resetSSHKeyForVirtualMachine(cmd))

    def update(self, apiclient, **kwargs):
        """Updates the VM data"""

        cmd = updateVirtualMachine.updateVirtualMachineCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateVirtualMachine(cmd))

    def delete(self, apiclient, expunge=True, **kwargs):
        """Destroy an Instance"""
        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.id = self.id
        cmd.expunge = expunge
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        apiclient.destroyVirtualMachine(cmd)

    def expunge(self, apiclient):
        """Expunge an Instance"""
        cmd = expungeVirtualMachine.expungeVirtualMachineCmd()
        cmd.id = self.id
        apiclient.expungeVirtualMachine(cmd)

    def migrate(self, apiclient, hostid=None):
        """migrate an Instance"""
        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.virtualmachineid = self.id
        if hostid:
            cmd.hostid = hostid
        apiclient.migrateVirtualMachine(cmd)

    def migrate_vm_with_volume(self, apiclient, hostid=None, migrateto=None):
        """migrate an Instance and its volumes"""
        cmd = migrateVirtualMachineWithVolume.migrateVirtualMachineWithVolumeCmd()
        cmd.virtualmachineid = self.id
        if hostid:
            cmd.hostid = hostid
        if migrateto:
            migrateto = []
            for volume, pool in migrateto.items():
                cmd.migrateto.append({
                    'volume': volume,
                    'pool': pool
            })
        apiclient.migrateVirtualMachineWithVolume(cmd)

    def attach_volume(self, apiclient, volume, deviceid=None):
        """Attach volume to instance"""
        cmd = attachVolume.attachVolumeCmd()
        cmd.id = volume.id
        cmd.virtualmachineid = self.id

        if deviceid is not None:
            cmd.deviceid = deviceid

        return apiclient.attachVolume(cmd)

    def detach_volume(self, apiclient, volume):
        """Detach volume to instance"""
        cmd = detachVolume.detachVolumeCmd()
        cmd.id = volume.id
        return apiclient.detachVolume(cmd)

    def add_nic(self, apiclient, networkId, ipaddress=None):
        """Add a NIC to a VM"""
        cmd = addNicToVirtualMachine.addNicToVirtualMachineCmd()
        cmd.virtualmachineid = self.id
        cmd.networkid = networkId

        if ipaddress:
            cmd.ipaddress = ipaddress

        return apiclient.addNicToVirtualMachine(cmd)

    def remove_nic(self, apiclient, nicId):
        """Remove a NIC to a VM"""
        cmd = removeNicFromVirtualMachine.removeNicFromVirtualMachineCmd()
        cmd.nicid = nicId
        cmd.virtualmachineid = self.id
        return apiclient.removeNicFromVirtualMachine(cmd)

    def update_default_nic(self, apiclient, nicId):
        """Set a NIC to be the default network adapter for a VM"""
        cmd = updateDefaultNicForVirtualMachine.\
            updateDefaultNicForVirtualMachineCmd()
        cmd.nicid = nicId
        cmd.virtualmachineid = self.id
        return apiclient.updateDefaultNicForVirtualMachine(cmd)

    def attach_iso(self, apiclient, iso):
        """Attach ISO to instance"""
        cmd = attachIso.attachIsoCmd()
        cmd.id = iso.id
        cmd.virtualmachineid = self.id
        return apiclient.attachIso(cmd)

    def detach_iso(self, apiclient):
        """Detach ISO to instance"""
        cmd = detachIso.detachIsoCmd()
        cmd.virtualmachineid = self.id
        return apiclient.detachIso(cmd)

    def scale_virtualmachine(self, apiclient, serviceOfferingId):
        """ Scale up of service offering for the Instance"""
        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.id = self.id
        cmd.serviceofferingid = serviceOfferingId
        return apiclient.scaleVirtualMachine(cmd)

    def change_service_offering(self, apiclient, serviceOfferingId):
        """Change service offering of the instance"""
        cmd = changeServiceForVirtualMachine.\
            changeServiceForVirtualMachineCmd()
        cmd.id = self.id
        cmd.serviceofferingid = serviceOfferingId
        return apiclient.changeServiceForVirtualMachine(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all VMs matching criteria"""

        cmd = listVirtualMachines.listVirtualMachinesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listVirtualMachines(cmd))

    def resetPassword(self, apiclient):
        """Resets VM password if VM created using password enabled template"""

        cmd = resetPasswordForVirtualMachine.\
            resetPasswordForVirtualMachineCmd()
        cmd.id = self.id
        try:
            response = apiclient.resetPasswordForVirtualMachine(cmd)
        except Exception as e:
            raise Exception("Reset Password failed! - %s" % e)
        if response is not None:
            return response.password

    def assign_virtual_machine(self, apiclient, account, domainid):
        """Move a user VM to another user under same domain."""

        cmd = assignVirtualMachine.assignVirtualMachineCmd()
        cmd.virtualmachineid = self.id
        cmd.account = account
        cmd.domainid = domainid
        try:
            response = apiclient.assignVirtualMachine(cmd)
            return response
        except Exception as e:
            raise Exception("assignVirtualMachine failed - %s" % e)

    def update_affinity_group(self, apiclient, affinitygroupids=None,
                              affinitygroupnames=None):
        """Update affinity group of a VM"""
        cmd = updateVMAffinityGroup.updateVMAffinityGroupCmd()
        cmd.id = self.id

        if affinitygroupids:
            cmd.affinitygroupids = affinitygroupids

        if affinitygroupnames:
            cmd.affinitygroupnames = affinitygroupnames

        return apiclient.updateVMAffinityGroup(cmd)

    def scale(self, apiclient, serviceOfferingId,
            customcpunumber=None, customcpuspeed=None, custommemory=None):
        """Change service offering of the instance"""
        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.id = self.id
        cmd.serviceofferingid = serviceOfferingId
        cmd.details = [{"cpuNumber": "", "cpuSpeed": "", "memory": ""}]
        if customcpunumber:
            cmd.details[0]["cpuNumber"] = customcpunumber
        if customcpuspeed:
            cmd.details[0]["cpuSpeed"] = customcpuspeed
        if custommemory:
            cmd.details[0]["memory"] = custommemory
        return apiclient.scaleVirtualMachine(cmd)


class Volume:
    """Manage Volume Life cycle
    """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, zoneid=None, account=None,
               domainid=None, diskofferingid=None, projectid=None, size=None):
        """Create Volume"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = "-".join([services["diskname"], random_gen()])

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

        if projectid:
            cmd.projectid = projectid

        if size:
            cmd.size = size

        return Volume(apiclient.createVolume(cmd).__dict__)

    @classmethod
    def create_custom_disk(cls, apiclient, services, account=None,
                           domainid=None, diskofferingid=None):
        """Create Volume from Custom disk offering"""
        cmd = createVolume.createVolumeCmd()
        cmd.name = services["diskname"]

        if diskofferingid:
            cmd.diskofferingid = diskofferingid
        elif "customdiskofferingid" in services:
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
        if "size" in services:
            cmd.size = services["size"]
        if services["ispublic"]:
            cmd.ispublic = services["ispublic"]
        else:
            cmd.ispublic = False
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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listVolumes(cmd))

    def resize(self, apiclient, **kwargs):
        """Resize a volume"""
        cmd = resizeVolume.resizeVolumeCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.resizeVolume(cmd))

    @classmethod
    def upload(cls, apiclient, services, zoneid=None,
               account=None, domainid=None, url=None, **kwargs):
        """Uploads the volume to specified account"""

        cmd = uploadVolume.uploadVolumeCmd()
        if zoneid:
            cmd.zoneid = zoneid
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        cmd.format = services["format"]
        cmd.name = services["diskname"]
        if url:
            cmd.url = url
        else:
            cmd.url = services["url"]
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return Volume(apiclient.uploadVolume(cmd).__dict__)

    def wait_for_upload(self, apiclient, timeout=10, interval=60):
        """Wait for upload"""
        # Sleep to ensure template is in proper state before download
        time.sleep(interval)

        while True:
            volume_response = Volume.list(
                apiclient,
                id=self.id,
                zoneid=self.zoneid,
            )
            if isinstance(volume_response, list):

                volume = volume_response[0]
                # If volume is ready,
                # volume.state = Allocated
                if volume.state == 'Uploaded':
                    break

                elif 'Uploading' in volume.state:
                    time.sleep(interval)

                elif 'Installing' not in volume.state:
                    raise Exception(
                        "Error in uploading volume: status - %s" %
                        volume.state)
            elif timeout == 0:
                break

            else:
                time.sleep(interval)
                timeout = timeout - 1
        return

    @classmethod
    def extract(cls, apiclient, volume_id, zoneid, mode):
        """Extracts the volume"""

        cmd = extractVolume.extractVolumeCmd()
        cmd.id = volume_id
        cmd.zoneid = zoneid
        cmd.mode = mode
        return Volume(apiclient.extractVolume(cmd).__dict__)

    @classmethod
    def migrate(cls, apiclient, **kwargs):
        """Migrate a volume"""
        cmd = migrateVolume.migrateVolumeCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.migrateVolume(cmd))


class Snapshot:
    """Manage Snapshot Lifecycle
    """
    '''Class level variables'''
    # Variables denoting possible Snapshot states - start
    BACKED_UP = BACKED_UP
    BACKING_UP = BACKING_UP
    # Variables denoting possible Snapshot states - end

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, volume_id, account=None,
               domainid=None, projectid=None, locationtype=None):
        """Create Snapshot"""
        cmd = createSnapshot.createSnapshotCmd()
        cmd.volumeid = volume_id
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        if projectid:
            cmd.projectid = projectid
        if locationtype:
            cmd.locationtype = locationtype
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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listSnapshots(cmd))

    def validateState(self, apiclient, snapshotstate, timeout=600):
        """Check if snapshot is in required state
           returnValue: List[Result, Reason]
                 @Result: PASS if snapshot is in required state,
                          else FAIL
                 @Reason: Reason for failure in case Result is FAIL
        """
        isSnapshotInRequiredState = False
        try:
            while timeout >= 0:
                snapshots = Snapshot.list(apiclient, id=self.id)
                assert validateList(snapshots)[0] == PASS, "snapshots list\
                        validation failed"
                if str(snapshots[0].state).lower() == snapshotstate:
                    isSnapshotInRequiredState = True
                    break
                timeout -= 60
                time.sleep(60)
            #end while
            if isSnapshotInRequiredState:
                return[PASS, None]
            else:
                raise Exception("Snapshot not in required state")
        except Exception as e:
            return [FAIL, e]


class Template:
    """Manage template life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, volumeid=None,
               account=None, domainid=None, projectid=None):
        """Create template from Volume"""
        # Create template from Virtual machine and Volume ID
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([services["name"], random_gen()])
        if "ostypeid" in services:
            cmd.ostypeid = services["ostypeid"]
        elif "ostype" in services:
            # Find OSTypeId from Os type
            sub_cmd = listOsTypes.listOsTypesCmd()
            sub_cmd.description = services["ostype"]
            ostypes = apiclient.listOsTypes(sub_cmd)

            if not isinstance(ostypes, list):
                raise Exception(
                    "Unable to find Ostype id with desc: %s" %
                    services["ostype"])
            cmd.ostypeid = ostypes[0].id
        else:
            raise Exception(
                "Unable to find Ostype is required for creating template")

        cmd.isfeatured = services[
            "isfeatured"] if "isfeatured" in services else False
        cmd.ispublic = services[
            "ispublic"] if "ispublic" in services else False
        cmd.isextractable = services[
            "isextractable"] if "isextractable" in services else False
        cmd.passwordenabled = services[
            "passwordenabled"] if "passwordenabled" in services else False

        if volumeid:
            cmd.volumeid = volumeid

        if account:
            cmd.account = account

        if domainid:
            cmd.domainid = domainid

        if projectid:
            cmd.projectid = projectid
        return Template(apiclient.createTemplate(cmd).__dict__)

    @classmethod
    def register(cls, apiclient, services, zoneid=None,
                 account=None, domainid=None, hypervisor=None,
                 projectid=None, details=None):
        """Create template from URL"""

        # Create template from Virtual machine and Volume ID
        cmd = registerTemplate.registerTemplateCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([services["name"], random_gen()])
        cmd.format = services["format"]
        if hypervisor:
            cmd.hypervisor = hypervisor
        elif "hypervisor" in services:
            cmd.hypervisor = services["hypervisor"]

        if "ostypeid" in services:
            cmd.ostypeid = services["ostypeid"]
        elif "ostype" in services:
            # Find OSTypeId from Os type
            sub_cmd = listOsTypes.listOsTypesCmd()
            sub_cmd.description = services["ostype"]
            ostypes = apiclient.listOsTypes(sub_cmd)

            if not isinstance(ostypes, list):
                raise Exception(
                    "Unable to find Ostype id with desc: %s" %
                    services["ostype"])
            cmd.ostypeid = ostypes[0].id
        else:
            raise Exception(
                "Unable to find Ostype is required for registering template")

        cmd.url = services["url"]

        if zoneid:
            cmd.zoneid = zoneid
        else:
            cmd.zoneid = services["zoneid"]

        cmd.isfeatured = services[
            "isfeatured"] if "isfeatured" in services else False
        cmd.ispublic = services[
            "ispublic"] if "ispublic" in services else False
        cmd.isextractable = services[
            "isextractable"] if "isextractable" in services else False
        cmd.isdynamicallyscalable=services["isdynamicallyscalable"] if "isdynamicallyscalable" in services else False
        cmd.passwordenabled = services[
            "passwordenabled"] if "passwordenabled" in services else False

        if account:
            cmd.account = account

        if domainid:
            cmd.domainid = domainid

        if projectid:
            cmd.projectid = projectid
        elif "projectid" in services:
            cmd.projectid = services["projectid"]

        if details:
            cmd.details = details

        # Register Template
        template = apiclient.registerTemplate(cmd)

        if isinstance(template, list):
            return Template(template[0].__dict__)

    @classmethod
    def extract(cls, apiclient, id, mode, zoneid=None):
        "Extract template "

        cmd = extractTemplate.extractTemplateCmd()
        cmd.id = id
        cmd.mode = mode
        cmd.zoneid = zoneid

        return apiclient.extractTemplate(cmd)

    @classmethod
    def create_from_snapshot(cls, apiclient, snapshot, services,
                             random_name=True):
        """Create Template from snapshot"""
        # Create template from Virtual machine and Snapshot ID
        cmd = createTemplate.createTemplateCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([
            services["name"],
            random_gen()
        ]) if random_name else services["name"]

        if "ispublic" in services:
            cmd.ispublic = services["ispublic"]

        if "ostypeid" in services:
            cmd.ostypeid = services["ostypeid"]
        elif "ostype" in services:
            # Find OSTypeId from Os type
            sub_cmd = listOsTypes.listOsTypesCmd()
            sub_cmd.description = services["ostype"]
            ostypes = apiclient.listOsTypes(sub_cmd)

            if not isinstance(ostypes, list):
                raise Exception(
                    "Unable to find Ostype id with desc: %s" %
                    services["ostype"])
            cmd.ostypeid = ostypes[0].id
        else:
            raise Exception(
                "Unable to find Ostype is required for creating template")

        cmd.snapshotid = snapshot.id
        return Template(apiclient.createTemplate(cmd).__dict__)

    def delete(self, apiclient, zoneid=None):
        """Delete Template"""

        cmd = deleteTemplate.deleteTemplateCmd()
        cmd.id = self.id
        if zoneid:
            cmd.zoneid = zoneid
        apiclient.deleteTemplate(cmd)

    def download(self, apiclient, retries=300, interval=5):
        """Download Template"""
        while retries > -1:
            time.sleep(interval)
            template_response = Template.list(
                apiclient,
                id=self.id,
                zoneid=self.zoneid,
                templatefilter='self'
            )

            if isinstance(template_response, list):
                template = template_response[0]
                if not hasattr(template, 'status') or not template or not template.status:
                    retries = retries - 1
                    continue

                # If template is ready,
                # template.status = Download Complete
                # Downloading - x% Downloaded
                # Error - Any other string
                if template.status == 'Download Complete' and template.isready:
                    return

                elif 'Downloaded' in template.status:
                    retries = retries - 1
                    continue

                elif 'Installing' not in template.status:
                    if retries >= 0:
                        retries = retries - 1
                        continue
                    raise Exception(
                        "Error in downloading template: status - %s" %
                        template.status)

            else:
                retries = retries - 1
        raise Exception("Template download failed exception")

    def updatePermissions(self, apiclient, **kwargs):
        """Updates the template permissions"""

        cmd = updateTemplatePermissions.updateTemplatePermissionsCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateTemplatePermissions(cmd))

    def update(self, apiclient, **kwargs):
        """Updates the template details"""

        cmd = updateTemplate.updateTemplateCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateTemplate(cmd))

    def copy(self, apiclient, sourcezoneid, destzoneid):
        "Copy Template from source Zone to Destination Zone"

        cmd = copyTemplate.copyTemplateCmd()
        cmd.id = self.id
        cmd.sourcezoneid = sourcezoneid
        cmd.destzoneid = destzoneid

        return apiclient.copyTemplate(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all templates matching criteria"""

        cmd = listTemplates.listTemplatesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listTemplates(cmd))


class Iso:
    """Manage ISO life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, account=None, domainid=None,
               projectid=None, zoneid=None):
        """Create an ISO"""
        # Create ISO from URL
        cmd = registerIso.registerIsoCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([services["name"], random_gen()])
        if "ostypeid" in services:
            cmd.ostypeid = services["ostypeid"]
        elif "ostype" in services:
            # Find OSTypeId from Os type
            sub_cmd = listOsTypes.listOsTypesCmd()
            sub_cmd.description = services["ostype"]
            ostypes = apiclient.listOsTypes(sub_cmd)

            if not isinstance(ostypes, list):
                raise Exception(
                    "Unable to find Ostype id with desc: %s" %
                    services["ostype"])
            cmd.ostypeid = ostypes[0].id
        else:
            raise Exception(
                "Unable to find Ostype is required for creating ISO")

        cmd.url = services["url"]

        if zoneid:
            cmd.zoneid = zoneid
        else:
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
        if projectid:
            cmd.projectid = projectid
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

    def download(self, apiclient, retries=300, interval=5):
        """Download an ISO"""
        # Ensuring ISO is successfully downloaded
        while retries > -1:
            time.sleep(interval)

            cmd = listIsos.listIsosCmd()
            cmd.id = self.id
            iso_response = apiclient.listIsos(cmd)

            if isinstance(iso_response, list):
                response = iso_response[0]
                if not hasattr(response, 'status') or not response or not response.status:
                    retries = retries - 1
                    continue

                # Check whether download is in progress(for Ex:10% Downloaded)
                # or ISO is 'Successfully Installed'
                if response.status == 'Successfully Installed' and response.isready:
                    return
                elif 'Downloaded' not in response.status and \
                        'Installing' not in response.status:
                    if retries >= 0:
                        retries = retries - 1
                        continue
                    raise Exception(
                        "Error In Downloading ISO: ISO Status - %s" %
                        response.status)
            else:
                retries = retries - 1
        raise Exception("ISO download failed exception")

    @classmethod
    def extract(cls, apiclient, id, mode, zoneid=None):
        "Extract ISO "

        cmd = extractIso.extractIsoCmd()
        cmd.id = id
        cmd.mode = mode
        cmd.zoneid = zoneid

        return apiclient.extractIso(cmd)

    def update(self, apiclient, **kwargs):
        """Updates the ISO details"""

        cmd = updateIso.updateIsoCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateIso(cmd))

    @classmethod
    def copy(cls, apiclient, id, sourcezoneid, destzoneid):
        "Copy ISO from source Zone to Destination Zone"

        cmd = copyIso.copyIsoCmd()
        cmd.id = id
        cmd.sourcezoneid = sourcezoneid
        cmd.destzoneid = destzoneid

        return apiclient.copyIso(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all available ISO files."""

        cmd = listIsos.listIsosCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listIsos(cmd))


class PublicIPAddress:
    """Manage Public IP Addresses"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, accountid=None, zoneid=None, domainid=None,
               services=None, networkid=None, projectid=None, vpcid=None,
               isportable=False):
        """Associate Public IP address"""
        cmd = associateIpAddress.associateIpAddressCmd()

        if accountid:
            cmd.account = accountid
        elif services and "account" in services:
            cmd.account = services["account"]

        if zoneid:
            cmd.zoneid = zoneid
        elif "zoneid" in services:
            cmd.zoneid = services["zoneid"]

        if domainid:
            cmd.domainid = domainid
        elif services and "domainid" in services:
            cmd.domainid = services["domainid"]

        if isportable:
            cmd.isportable = isportable

        if networkid:
            cmd.networkid = networkid

        if projectid:
            cmd.projectid = projectid

        if vpcid:
            cmd.vpcid = vpcid
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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listPublicIpAddresses(cmd))


class NATRule:
    """Manage port forwarding rule"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, virtual_machine, services, ipaddressid=None,
               projectid=None, openfirewall=False, networkid=None, vpcid=None,
               vmguestip=None):
        """Create Port forwarding rule"""
        cmd = createPortForwardingRule.createPortForwardingRuleCmd()

        if ipaddressid:
            cmd.ipaddressid = ipaddressid
        elif "ipaddressid" in services:
            cmd.ipaddressid = services["ipaddressid"]

        cmd.privateport = services["privateport"]
        cmd.publicport = services["publicport"]
        if "privateendport" in services:
            cmd.privateendport = services["privateendport"]
        if "publicendport" in services:
            cmd.publicendport = services["publicendport"]
        cmd.protocol = services["protocol"]
        cmd.virtualmachineid = virtual_machine.id

        if projectid:
            cmd.projectid = projectid

        if openfirewall:
            cmd.openfirewall = True

        if networkid:
            cmd.networkid = networkid

        if vpcid:
            cmd.vpcid = vpcid

        if vmguestip:
            cmd.vmguestip = vmguestip

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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listPortForwardingRules(cmd))


class StaticNATRule:
    """Manage Static NAT rule"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, ipaddressid=None,
               networkid=None, vpcid=None):
        """Creates static ip forwarding rule"""

        cmd = createFirewallRule.createFirewallRuleCmd()
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

        if networkid:
            cmd.networkid = networkid

        if vpcid:
            cmd.vpcid = vpcid
        return StaticNATRule(apiclient.createFirewallRule(cmd).__dict__)

    @classmethod
    def createIpForwardingRule(cls, apiclient, startport, endport, protocol, ipaddressid, openfirewall):
        """Creates static ip forwarding rule"""

        cmd = createIpForwardingRule.createIpForwardingRuleCmd()
        cmd.startport = startport
        cmd.endport = endport
        cmd.protocol = protocol
        cmd.openfirewall = openfirewall
        cmd.ipaddressid = ipaddressid
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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listIpForwardingRules(cmd))

    @classmethod
    def enable(cls, apiclient, ipaddressid, virtualmachineid, networkid=None,
               vmguestip=None):
        """Enables Static NAT rule"""

        cmd = enableStaticNat.enableStaticNatCmd()
        cmd.ipaddressid = ipaddressid
        cmd.virtualmachineid = virtualmachineid
        if networkid:
            cmd.networkid = networkid

        if vmguestip:
            cmd.vmguestip = vmguestip
        apiclient.enableStaticNat(cmd)
        return

    @classmethod
    def disable(cls, apiclient, ipaddressid, virtualmachineid=None):
        """Disables Static NAT rule"""

        cmd = disableStaticNat.disableStaticNatCmd()
        cmd.ipaddressid = ipaddressid
        apiclient.disableStaticNat(cmd)
        return


class EgressFireWallRule:

    """Manage Egress Firewall rule"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, networkid, protocol, cidrlist=None,
               startport=None, endport=None, type=None, code=None):
        """Create Egress Firewall Rule"""
        cmd = createEgressFirewallRule.createEgressFirewallRuleCmd()
        cmd.networkid = networkid
        cmd.protocol = protocol
        if cidrlist:
            cmd.cidrlist = cidrlist
        if startport:
            cmd.startport = startport
        if endport:
            cmd.endport = endport
        if type:
            cmd.type = type
        if code:
            cmd.code = code

        return EgressFireWallRule(
            apiclient.createEgressFirewallRule(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Egress Firewall rule"""
        cmd = deleteEgressFirewallRule.deleteEgressFirewallRuleCmd()
        cmd.id = self.id
        apiclient.deleteEgressFirewallRule(cmd)
        return

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Egress Firewall Rules matching criteria"""

        cmd = listEgressFirewallRules.listEgressFirewallRulesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listEgressFirewallRules(cmd))


class FireWallRule:

    """Manage Firewall rule"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, ipaddressid, protocol, cidrlist=None,
               startport=None, endport=None, projectid=None, vpcid=None):
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

        if projectid:
            cmd.projectid = projectid

        if vpcid:
            cmd.vpcid = vpcid

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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listFirewallRules(cmd))


class Autoscale:

    """Manage Auto scale"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def listCounters(cls, apiclient, **kwargs):
        """Lists all available Counters."""

        cmd = listCounters.listCountersCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listCounters(cmd))

    @classmethod
    def createCondition(cls, apiclient, counterid, relationaloperator, threshold):
        """creates condition."""

        cmd = createCondition.createConditionCmd()
        cmd.counterid = counterid
        cmd.relationaloperator = relationaloperator
        cmd.threshold = threshold
        return(apiclient.createCondition(cmd))

    @classmethod
    def listConditions(cls, apiclient, **kwargs):
        """Lists all available Conditions."""

        cmd = listConditions.listConditionsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listConditions(cmd))

    @classmethod
    def listAutoscalePolicies(cls, apiclient, **kwargs):
        """Lists all available Autoscale Policies."""

        cmd = listAutoScalePolicies.listAutoScalePoliciesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listAutoScalePolicies(cmd))

    @classmethod
    def createAutoscalePolicy(cls, apiclient, action, conditionids, duration, quiettime=None):
        """creates condition."""

        cmd = createAutoScalePolicy.createAutoScalePolicyCmd()
        cmd.action = action
        cmd.conditionids = conditionids
        cmd.duration = duration
        if quiettime:
            cmd.quiettime = quiettime

        return(apiclient.createAutoScalePolicy(cmd))

    @classmethod
    def updateAutoscalePolicy(cls, apiclient, id, **kwargs):
        """Updates Autoscale Policy."""

        cmd = updateAutoScalePolicy.updateAutoScalePolicyCmd()
        cmd.id = id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateAutoScalePolicy(cmd))

    @classmethod
    def listAutoscaleVmPofiles(cls, apiclient, **kwargs):
        """Lists all available AutoscaleVM  Profiles."""

        cmd = listAutoScaleVmProfiles.listAutoScaleVmProfilesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listAutoScaleVmProfiles(cmd))

    @classmethod
    def createAutoscaleVmProfile(cls, apiclient, serviceofferingid, zoneid, templateid,
                                 autoscaleuserid=None, destroyvmgraceperiod=None, counterparam=None):
        """creates Autoscale VM Profile."""

        cmd = createAutoScaleVmProfile.createAutoScaleVmProfileCmd()
        cmd.serviceofferingid = serviceofferingid
        cmd.zoneid = zoneid
        cmd.templateid = templateid
        if autoscaleuserid:
            cmd.autoscaleuserid = autoscaleuserid

        if destroyvmgraceperiod:
            cmd.destroyvmgraceperiod = destroyvmgraceperiod

        if counterparam:
            for name, value in counterparam.items():
                cmd.counterparam.append({
                    'name': name,
                    'value': value
                })

        return(apiclient.createAutoScaleVmProfile(cmd))

    @classmethod
    def updateAutoscaleVMProfile(cls, apiclient, id, **kwargs):
        """Updates Autoscale Policy."""

        cmd = updateAutoScaleVmProfile.updateAutoScaleVmProfileCmd()
        cmd.id = id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateAutoScaleVmProfile(cmd))

    @classmethod
    def createAutoscaleVmGroup(cls, apiclient, lbruleid, minmembers, maxmembers,
                                 scaledownpolicyids, scaleuppolicyids, vmprofileid, interval=None):
        """creates Autoscale VM Group."""

        cmd = createAutoScaleVmGroup.createAutoScaleVmGroupCmd()
        cmd.lbruleid = lbruleid
        cmd.minmembers = minmembers
        cmd.maxmembers = maxmembers
        cmd.scaledownpolicyids = scaledownpolicyids
        cmd.scaleuppolicyids = scaleuppolicyids
        cmd.vmprofileid = vmprofileid
        if interval:
            cmd.interval = interval

        return(apiclient.createAutoScaleVmGroup(cmd))

    @classmethod
    def listAutoscaleVmGroup(cls, apiclient, **kwargs):
        """Lists all available AutoscaleVM  Group."""

        cmd = listAutoScaleVmGroups.listAutoScaleVmGroupsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listAutoScaleVmGroups(cmd))

    @classmethod
    def enableAutoscaleVmGroup(cls, apiclient, id, **kwargs):
        """Enables AutoscaleVM  Group."""

        cmd = enableAutoScaleVmGroup.enableAutoScaleVmGroupCmd()
        cmd.id = id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.enableAutoScaleVmGroup(cmd))

    @classmethod
    def disableAutoscaleVmGroup(cls, apiclient, id, **kwargs):
        """Disables AutoscaleVM  Group."""

        cmd = disableAutoScaleVmGroup.disableAutoScaleVmGroupCmd()
        cmd.id = id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.disableAutoScaleVmGroup(cmd))

    @classmethod
    def updateAutoscaleVMGroup(cls, apiclient, id, **kwargs):
        """Updates Autoscale VM Group."""

        cmd = updateAutoScaleVmGroup.updateAutoScaleVmGroupCmd()
        cmd.id = id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateAutoScaleVmGroup(cmd))


class ServiceOffering:

    """Manage service offerings cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, tags=None, domainid=None, **kwargs):
        """Create Service offering"""
        cmd = createServiceOffering.createServiceOfferingCmd()
        cmd.cpunumber = services["cpunumber"]
        cmd.cpuspeed = services["cpuspeed"]
        cmd.displaytext = services["displaytext"]
        cmd.memory = services["memory"]
        cmd.name = services["name"]
        if "storagetype" in services:
            cmd.storagetype = services["storagetype"]

        if "systemvmtype" in services:
            cmd.systemvmtype = services['systemvmtype']

        if "issystem" in services:
            cmd.issystem = services['issystem']

        if "hosttags" in services:
            cmd.hosttags = services["hosttags"]

        if "deploymentplanner" in services:
            cmd.deploymentplanner = services["deploymentplanner"]

        if "serviceofferingdetails" in services:
            count = 1
            for i in services["serviceofferingdetails"]:
                for key, value in i.items():
                    setattr(cmd, "serviceofferingdetails[%d].key" % count, key)
                    setattr(cmd, "serviceofferingdetails[%d].value" % count, value)
                count = count + 1

        if "isvolatile" in services:
            cmd.isvolatile = services["isvolatile"]

        if "customizediops" in services:
            cmd.customizediops = services["customizediops"]

        if "miniops" in services:
            cmd.miniops = services["miniops"]

        if "maxiops" in services:
            cmd.maxiops = services["maxiops"]

        if "hypervisorsnapshotreserve" in services:
            cmd.hypervisorsnapshotreserve = services["hypervisorsnapshotreserve"]

        if "offerha" in services:
            cmd.offerha = services["offerha"]

        # Service Offering private to that domain
        if domainid:
            cmd.domainid = domainid

        if tags:
            cmd.tags = tags
        elif "tags" in services:
            cmd.tags = services["tags"]

        [setattr(cmd, k, v) for k, v in kwargs.items()]
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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listServiceOfferings(cmd))


class DiskOffering:
    """Manage disk offerings cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, tags=None, custom=False, domainid=None):
        """Create Disk offering"""
        cmd = createDiskOffering.createDiskOfferingCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = services["name"]
        if custom:
            cmd.customized = True
        else:
            cmd.disksize = services["disksize"]

        if domainid:
            cmd.domainid = domainid

        if tags:
            cmd.tags = tags
        elif "tags" in services:
            cmd.tags = services["tags"]

        if "storagetype" in services:
            cmd.storagetype = services["storagetype"]

        if "customizediops" in services:
            cmd.customizediops = services["customizediops"]

        if "maxiops" in services:
            cmd.maxiops = services["maxiops"]

        if "miniops" in services:
            cmd.miniops = services["miniops"]

        if "hypervisorsnapshotreserve" in services:
            cmd.hypervisorsnapshotreserve = services["hypervisorsnapshotreserve"]

        if "provisioningtype" in services:
            cmd.provisioningtype = services["provisioningtype"]

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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listDiskOfferings(cmd))


class NetworkOffering:
    """Manage network offerings cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, **kwargs):
        """Create network offering"""

        cmd = createNetworkOffering.createNetworkOfferingCmd()
        cmd.displaytext = "-".join([services["displaytext"], random_gen()])
        cmd.name = "-".join([services["name"], random_gen()])
        cmd.guestiptype = services["guestiptype"]
        cmd.supportedservices = ''
        if "supportedservices" in services:
            cmd.supportedservices = services["supportedservices"]
        cmd.traffictype = services["traffictype"]

        if "useVpc" in services:
            cmd.useVpc = services["useVpc"]
        cmd.serviceproviderlist = []
        if "serviceProviderList" in services:
            for service, provider in services["serviceProviderList"].items():
                cmd.serviceproviderlist.append({
                    'service': service,
                    'provider': provider
                })
        if "serviceCapabilityList" in services:
            cmd.servicecapabilitylist = []
            for service, capability in services["serviceCapabilityList"].\
                                       items():
                for ctype, value in capability.items():
                    cmd.servicecapabilitylist.append({
                        'service': service,
                        'capabilitytype': ctype,
                        'capabilityvalue': value
                    })
        if "specifyVlan" in services:
            cmd.specifyVlan = services["specifyVlan"]
        if "specifyIpRanges" in services:
            cmd.specifyIpRanges = services["specifyIpRanges"]
        if "ispersistent" in services:
            cmd.ispersistent = services["ispersistent"]
        if "egress_policy" in services:
            cmd.egressdefaultpolicy = services["egress_policy"]
        cmd.details = [{}]
        if "servicepackageuuid" in services:
            cmd.details[0]["servicepackageuuid"] = services["servicepackageuuid"]
        if "servicepackagedescription" in services:
            cmd.details[0]["servicepackagedescription"] = services["servicepackagedescription"]


        cmd.availability = 'Optional'

        [setattr(cmd, k, v) for k, v in kwargs.items()]

        return NetworkOffering(apiclient.createNetworkOffering(cmd).__dict__)

    def delete(self, apiclient):
        """Delete network offering"""
        cmd = deleteNetworkOffering.deleteNetworkOfferingCmd()
        cmd.id = self.id
        apiclient.deleteNetworkOffering(cmd)
        return

    def update(self, apiclient, **kwargs):
        """Lists all available network offerings."""

        cmd = updateNetworkOffering.updateNetworkOfferingCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateNetworkOffering(cmd))

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all available network offerings."""

        cmd = listNetworkOfferings.listNetworkOfferingsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listNetworkOfferings(cmd))


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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listSnapshotPolicies(cmd))

class GuestOs:
    """Guest OS calls (currently read-only implemented)"""
    def __init(self, items):
        self.__dict__.update(items)

    @classmethod
    def listMapping(cls, apiclient, **kwargs):
        """List all Guest Os Mappings matching criteria"""
        cmd = listGuestOsMapping.listGuestOsMappingCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]

        return (apiclient.listGuestOsMapping(cmd))

    @classmethod
    def listCategories(cls, apiclient, **kwargs):
        """List all Os Categories"""
        [setattr(cmd, k, v) for k, v in kwargs.items()]

        return (apiclient.listOsCategories(cmd))

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Os Types matching criteria"""

        cmd = listOsTypes.listOsTypesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listOsTypes(cmd))

class Hypervisor:
    """Manage Hypervisor"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists hypervisors"""

        cmd = listHypervisors.listHypervisorsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listHypervisors(cmd))


class LoadBalancerRule:
    """Manage Load Balancer rule"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, ipaddressid=None, accountid=None,
               networkid=None, vpcid=None, projectid=None, domainid=None):
        """Create Load balancing Rule"""

        cmd = createLoadBalancerRule.createLoadBalancerRuleCmd()

        if ipaddressid:
            cmd.publicipid = ipaddressid
        elif "ipaddressid" in services:
            cmd.publicipid = services["ipaddressid"]

        if accountid:
            cmd.account = accountid
        elif "account" in services:
            cmd.account = services["account"]

        if domainid:
            cmd.domainid = domainid

        if vpcid:
            cmd.vpcid = vpcid
        cmd.name = services["name"]
        cmd.algorithm = services["alg"]
        cmd.privateport = services["privateport"]
        cmd.publicport = services["publicport"]

        if "openfirewall" in services:
            cmd.openfirewall = services["openfirewall"]

        if projectid:
            cmd.projectid = projectid

        if networkid:
            cmd.networkid = networkid
        return LoadBalancerRule(apiclient.createLoadBalancerRule(cmd).__dict__)

    def delete(self, apiclient):
        """Delete load balancing rule"""
        cmd = deleteLoadBalancerRule.deleteLoadBalancerRuleCmd()
        cmd.id = self.id
        apiclient.deleteLoadBalancerRule(cmd)
        return

    def assign(self, apiclient, vms=None, vmidipmap=None):
        """Assign virtual machines to load balancing rule"""
        cmd = assignToLoadBalancerRule.assignToLoadBalancerRuleCmd()
        cmd.id = self.id
        if vmidipmap:
            cmd.vmidipmap = vmidipmap
        if vms:
            cmd.virtualmachineids = [str(vm.id) for vm in vms]
        apiclient.assignToLoadBalancerRule(cmd)
        return

    def remove(self, apiclient, vms=None, vmidipmap=None):
        """Remove virtual machines from load balancing rule"""
        cmd = removeFromLoadBalancerRule.removeFromLoadBalancerRuleCmd()
        cmd.id = self.id
        if vms:
            cmd.virtualmachineids = [str(vm.id) for vm in vms]
        if vmidipmap:
            cmd.vmidipmap = vmidipmap
        apiclient.removeFromLoadBalancerRule(cmd)
        return

    def update(self, apiclient, algorithm=None,
               description=None, name=None, **kwargs):
        """Updates the load balancing rule"""
        cmd = updateLoadBalancerRule.updateLoadBalancerRuleCmd()
        cmd.id = self.id
        if algorithm:
            cmd.algorithm = algorithm
        if description:
            cmd.description = description
        if name:
            cmd.name = name

        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updateLoadBalancerRule(cmd)

    def createSticky(
            self, apiclient, methodname, name, description=None, param=None):
        """Creates a sticky policy for the LB rule"""

        cmd = createLBStickinessPolicy.createLBStickinessPolicyCmd()
        cmd.lbruleid = self.id
        cmd.methodname = methodname
        cmd.name = name
        if description:
            cmd.description = description
        if param:
            cmd.param = []
            for name, value in param.items():
                cmd.param.append({'name': name, 'value': value})
        return apiclient.createLBStickinessPolicy(cmd)

    def deleteSticky(self, apiclient, id):
        """Deletes stickyness policy"""

        cmd = deleteLBStickinessPolicy.deleteLBStickinessPolicyCmd()
        cmd.id = id
        return apiclient.deleteLBStickinessPolicy(cmd)

    @classmethod
    def listStickyPolicies(cls, apiclient, lbruleid, **kwargs):
        """Lists stickiness policies for load balancing rule"""

        cmd = listLBStickinessPolicies.listLBStickinessPoliciesCmd()
        cmd.lbruleid = lbruleid
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return apiclient.listLBStickinessPolicies(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Load balancing rules matching criteria"""

        cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listLoadBalancerRules(cmd))

    @classmethod
    def listLoadBalancerRuleInstances(cls, apiclient, id, lbvmips=False, applied=None, **kwargs):
        """Lists load balancing rule Instances"""

        cmd = listLoadBalancerRuleInstances.listLoadBalancerRuleInstancesCmd()
        cmd.id = id
        if applied:
            cmd.applied = applied
        cmd.lbvmips = lbvmips

        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.listLoadBalancerRuleInstances(cmd)


class Cluster:
    """Manage Cluster life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, zoneid=None, podid=None, hypervisor=None):
        """Create Cluster"""
        cmd = addCluster.addClusterCmd()
        cmd.clustertype = services["clustertype"]
        cmd.hypervisor = hypervisor

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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listClusters(cmd))

    @classmethod
    def update(cls, apiclient, **kwargs):
        """Update cluster information"""

        cmd = updateCluster.updateClusterCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateCluster(cmd))


class Host:
    """Manage Host life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, cluster, services, zoneid=None, podid=None, hypervisor=None):
        """
        1. Creates the host based upon the information provided.
        2. Verifies the output of the adding host and its state post addition
           Returns FAILED in case of an issue, else an instance of Host
        """
        try:
            cmd = addHost.addHostCmd()
            cmd.hypervisor = hypervisor
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

            '''
            Adds a Host,
            If response is valid and host is up return
            an instance of Host.
            If response is invalid, returns FAILED.
            If host state is not up, verify through listHosts call
            till host status is up and return accordingly. Max 3 retries
            '''
            host = apiclient.addHost(cmd)
            ret = validateList(host)
            if ret[0] == PASS:
                if str(host[0].state).lower() == 'up':
                    return Host(host[0].__dict__)
                retries = 3
                while retries:
                    lh_resp = apiclient.listHosts(host[0].id)
                    ret = validateList(lh_resp)
                    if (ret[0] == PASS) and \
                            (str(ret[1].state).lower() == 'up'):
                        return Host(host[0].__dict__)
                    retries += -1
            return FAILED
        except Exception as e:
            print "Exception Occurred Under Host.create : %s" % \
                  GetDetailExceptionInfo(e)
            return FAILED

    @staticmethod
    def _check_resource_state(apiclient, hostid, resourcestate):
        hosts = Host.list(apiclient, id=hostid, listall=True)

        validationresult = validateList(hosts)

        assert validationresult is not None, "'validationresult' should not be equal to 'None'."

        assert isinstance(validationresult, list), "'validationresult' should be a 'list'."

        assert len(validationresult) == 3, "'validationresult' should be a list with three items in it."

        if validationresult[0] == FAIL:
            raise Exception("Host list validation failed: %s" % validationresult[2])

        if str(hosts[0].resourcestate).lower().decode("string_escape") == str(resourcestate).lower():
            return True, None

        return False, "Host is not in the following state: " + str(resourcestate)

    def delete(self, apiclient):
        """Delete Host"""
        # Host must be in maintenance mode before deletion
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = self.id
        apiclient.prepareHostForMaintenance(cmd)

        retry_interval = 10
        num_tries = 10

        wait_result, return_val = wait_until(retry_interval, num_tries, Host._check_resource_state, apiclient, self.id, HOST_RS_MAINTENANCE)

        if not wait_result:
            raise Exception(return_val)

        cmd = deleteHost.deleteHostCmd()
        cmd.id = self.id
        apiclient.deleteHost(cmd)
        return

    @classmethod
    def enableMaintenance(cls, apiclient, id):
        """enables maintenance mode Host"""

        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = id
        return apiclient.prepareHostForMaintenance(cmd)

    @classmethod
    def cancelMaintenance(cls, apiclient, id):
        """Cancels maintenance mode Host"""

        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = id
        return apiclient.cancelHostMaintenance(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Hosts matching criteria"""

        cmd = listHosts.listHostsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listHosts(cmd))

    @classmethod
    def listForMigration(cls, apiclient, **kwargs):
        """List all Hosts for migration matching criteria"""

        cmd = findHostsForMigration.findHostsForMigrationCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.findHostsForMigration(cmd))

    @classmethod
    def update(cls, apiclient, **kwargs):
        """Update host information"""

        cmd = updateHost.updateHostCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateHost(cmd))

    @classmethod
    def reconnect(cls, apiclient, **kwargs):
        """Reconnect the Host"""

        cmd = reconnectHost.reconnectHostCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.reconnectHost(cmd))

    @classmethod
    def getState(cls, apiclient, hostid, state, resourcestate, timeout=600):
        """List Host and check if its resource state is as expected
        @returnValue - List[Result, Reason]
                       1) Result - FAIL if there is any exception
                       in the operation or Host state does not change
                       to expected state in given time else PASS
                       2) Reason - Reason for failure"""

        returnValue = [FAIL, "VM state not trasited to %s,\
                        operation timed out" % state]

        while timeout > 0:
            try:
                hosts = Host.list(apiclient,
                          id=hostid, listall=True)
                validationresult = validateList(hosts)
                if validationresult[0] == FAIL:
                    raise Exception("Host list validation failed: %s" % validationresult[2])
                elif str(hosts[0].state).lower().decode("string_escape") == str(state).lower() and str(hosts[0].resourcestate).lower().decode("string_escape") == str(resourcestate).lower():
                    returnValue = [PASS, None]
                    break
            except Exception as e:
                returnValue = [FAIL, e]
                break
            time.sleep(60)
            timeout -= 60
        return returnValue

class StoragePool:
    """Manage Storage pools (Primary Storage)"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, scope=None, clusterid=None,
               zoneid=None, podid=None, provider=None, tags=None,
               capacityiops=None, capacitybytes=None, hypervisor=None):
        """Create Storage pool (Primary Storage)"""

        cmd = createStoragePool.createStoragePoolCmd()
        cmd.name = services["name"]

        if podid:
            cmd.podid = podid
        elif "podid" in services:
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

        if scope:
            cmd.scope = scope
        elif "scope" in services:
            cmd.scope = services["scope"]

        if provider:
            cmd.provider = provider
        elif "provider" in services:
            cmd.provider = services["provider"]

        if tags:
            cmd.tags = tags
        elif "tags" in services:
            cmd.tags = services["tags"]

        if capacityiops:
            cmd.capacityiops = capacityiops
        elif "capacityiops" in services:
            cmd.capacityiops = services["capacityiops"]

        if capacitybytes:
            cmd.capacitybytes = capacitybytes
        elif "capacitybytes" in services:
            cmd.capacitybytes = services["capacitybytes"]

        if hypervisor:
            cmd.hypervisor = hypervisor
        elif "hypervisor" in services:
            cmd.hypervisor = services["hypervisor"]

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

    @classmethod
    def enableMaintenance(cls, apiclient, id):
        """enables maintenance mode Storage pool"""

        cmd = enableStorageMaintenance.enableStorageMaintenanceCmd()
        cmd.id = id
        return apiclient.enableStorageMaintenance(cmd)

    @classmethod
    def cancelMaintenance(cls, apiclient, id):
        """Cancels maintenance mode Host"""

        cmd = cancelStorageMaintenance.cancelStorageMaintenanceCmd()
        cmd.id = id
        return apiclient.cancelStorageMaintenance(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all storage pools matching criteria"""

        cmd = listStoragePools.listStoragePoolsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listStoragePools(cmd))

    @classmethod
    def listForMigration(cls, apiclient, **kwargs):
        """List all storage pools for migration matching criteria"""

        cmd = findStoragePoolsForMigration.findStoragePoolsForMigrationCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.findStoragePoolsForMigration(cmd))

    @classmethod
    def update(cls, apiclient, **kwargs):
        """Update storage pool"""
        cmd = updateStoragePool.updateStoragePoolCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updateStoragePool(cmd)

    @classmethod
    def getState(cls, apiclient, poolid, state, timeout=600):
        """List StoragePools and check if its  state is as expected
        @returnValue - List[Result, Reason]
                       1) Result - FAIL if there is any exception
                       in the operation or pool state does not change
                       to expected state in given time else PASS
                       2) Reason - Reason for failure"""

        returnValue = [FAIL, "VM state not trasited to %s,\
                        operation timed out" % state]

        while timeout > 0:
            try:
                pools = StoragePool.list(apiclient,
                          id=poolid, listAll=True)
                validationresult = validateList(pools)
                if validationresult[0] == FAIL:
                    raise Exception("Pool list validation failed: %s" % validationresult[2])
                elif str(pools[0].state).lower().decode("string_escape") == str(state).lower():
                    returnValue = [PASS, None]
                    break
            except Exception as e:
                returnValue = [FAIL, e]
                break
            time.sleep(60)
            timeout -= 60
        return returnValue

class Network:
    """Manage Network pools"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, accountid=None, domainid=None,
               networkofferingid=None, projectid=None,
               subdomainaccess=None, zoneid=None,
               gateway=None, netmask=None, vpcid=None, aclid=None, vlan=None):
        """Create Network for account"""
        cmd = createNetwork.createNetworkCmd()
        cmd.name = services["name"]
        cmd.displaytext = services["displaytext"]

        if networkofferingid:
            cmd.networkofferingid = networkofferingid
        elif "networkoffering" in services:
            cmd.networkofferingid = services["networkoffering"]

        if zoneid:
            cmd.zoneid = zoneid
        elif "zoneid" in services:
            cmd.zoneid = services["zoneid"]

        if subdomainaccess is not None:
            cmd.subdomainaccess = subdomainaccess

        if gateway:
            cmd.gateway = gateway
        elif "gateway" in services:
            cmd.gateway = services["gateway"]
        if netmask:
            cmd.netmask = netmask
        elif "netmask" in services:
            cmd.netmask = services["netmask"]
        if "startip" in services:
            cmd.startip = services["startip"]
        if "endip" in services:
            cmd.endip = services["endip"]
        if vlan:
            cmd.vlan = vlan
        elif "vlan" in services:
            cmd.vlan = services["vlan"]
        if "acltype" in services:
            cmd.acltype = services["acltype"]

        if accountid:
            cmd.account = accountid
        if domainid:
            cmd.domainid = domainid
        if projectid:
            cmd.projectid = projectid
        if vpcid:
            cmd.vpcid = vpcid
        if aclid:
            cmd.aclid = aclid
        return Network(apiclient.createNetwork(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Account"""

        cmd = deleteNetwork.deleteNetworkCmd()
        cmd.id = self.id
        apiclient.deleteNetwork(cmd)

    def update(self, apiclient, **kwargs):
        """Updates network with parameters passed"""

        cmd = updateNetwork.updateNetworkCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateNetwork(cmd))

    def restart(self, apiclient, cleanup=None):
        """Restarts the network"""

        cmd = restartNetwork.restartNetworkCmd()
        cmd.id = self.id
        if cleanup:
            cmd.cleanup = cleanup
        return(apiclient.restartNetwork(cmd))

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Networks matching criteria"""

        cmd = listNetworks.listNetworksCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listNetworks(cmd))


class NetworkACL:
    """Manage Network ACL lifecycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, networkid=None, protocol=None,
               number=None, aclid=None, action='Allow',
               traffictype=None, cidrlist=[]):
        """Create network ACL rules(Ingress/Egress)"""

        cmd = createNetworkACL.createNetworkACLCmd()
        if "networkid" in services:
            cmd.networkid = services["networkid"]
        elif networkid:
            cmd.networkid = networkid

        if "protocol" in services:
            cmd.protocol = services["protocol"]
            if services["protocol"] == 'ICMP':
                cmd.icmptype = -1
                cmd.icmpcode = -1
        elif protocol:
            cmd.protocol = protocol

        if "startport" in services:
            cmd.startport = services["startport"]
        if "endport" in services:
            cmd.endport = services["endport"]

        if "cidrlist" in services:
            cmd.cidrlist = services["cidrlist"]
        elif cidrlist:
            cmd.cidrlist = cidrlist

        if "traffictype" in services:
            cmd.traffictype = services["traffictype"]
        elif traffictype:
            cmd.traffictype = traffictype

        if "action" in services:
            cmd.action = services["action"]
        elif action:
            cmd.action = action

        if "number" in services:
            cmd.number = services["number"]
        elif number:
            cmd.number = number

        if "aclid" in services:
            cmd.aclid = services["aclid"]
        elif aclid:
            cmd.aclid = aclid

        # Defaulted to Ingress
        return NetworkACL(apiclient.createNetworkACL(cmd).__dict__)

    def delete(self, apiclient):
        """Delete network acl"""

        cmd = deleteNetworkACL.deleteNetworkACLCmd()
        cmd.id = self.id
        return apiclient.deleteNetworkACL(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List Network ACLs"""

        cmd = listNetworkACLs.listNetworkACLsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listNetworkACLs(cmd))


class NetworkACLList:
    """Manage Network ACL lists lifecycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(
            cls, apiclient, services, name=None, description=None, vpcid=None):
        """Create network ACL container list"""

        cmd = createNetworkACLList.createNetworkACLListCmd()
        if "name" in services:
            cmd.name = services["name"]
        elif name:
            cmd.name = name

        if "description" in services:
            cmd.description = services["description"]
        elif description:
            cmd.description = description

        if "vpcid" in services:
            cmd.vpcid = services["vpcid"]
        elif vpcid:
            cmd.vpcid = vpcid

        return NetworkACLList(apiclient.createNetworkACLList(cmd).__dict__)

    def delete(self, apiclient):
        """Delete network acl list"""

        cmd = deleteNetworkACLList.deleteNetworkACLListCmd()
        cmd.id = self.id
        return apiclient.deleteNetworkACLList(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List Network ACL lists"""

        cmd = listNetworkACLLists.listNetworkACLListsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listNetworkACLLists(cmd))


class Vpn:
    """Manage VPN life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, publicipid, account=None, domainid=None,
               projectid=None, networkid=None, vpcid=None, openfirewall=None, iprange=None, fordisplay=False):
        """Create VPN for Public IP address"""
        cmd = createRemoteAccessVpn.createRemoteAccessVpnCmd()
        cmd.publicipid = publicipid
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        if projectid:
            cmd.projectid = projectid
        if networkid:
            cmd.networkid = networkid
        if vpcid:
            cmd.vpcid = vpcid
        if iprange:
            cmd.iprange = iprange
        if openfirewall:
            cmd.openfirewall = openfirewall

        cmd.fordisplay = fordisplay
        return Vpn(apiclient.createRemoteAccessVpn(cmd).__dict__)

    @classmethod
    def createVpnGateway(cls, apiclient, vpcid):
        """Create VPN Gateway """
        cmd = createVpnGateway.createVpnGatewayCmd()
        cmd.vpcid = vpcid
        return (apiclient.createVpnGateway(cmd).__dict__)

    @classmethod
    def createVpnConnection(cls, apiclient, s2scustomergatewayid, s2svpngatewayid, passive=False):
        """Create VPN Connection """
        cmd = createVpnConnection.createVpnConnectionCmd()
        cmd.s2scustomergatewayid = s2scustomergatewayid
        cmd.s2svpngatewayid = s2svpngatewayid
        if passive:
            cmd.passive = passive
        return (apiclient.createVpnGateway(cmd).__dict__)

    @classmethod
    def resetVpnConnection(cls, apiclient, id):
        """Reset VPN Connection """
        cmd = resetVpnConnection.resetVpnConnectionCmd()
        cmd.id = id
        return (apiclient.resetVpnConnection(cmd).__dict__)

    @classmethod
    def deleteVpnConnection(cls, apiclient, id):
        """Delete VPN Connection """
        cmd = deleteVpnConnection.deleteVpnConnectionCmd()
        cmd.id = id
        return (apiclient.deleteVpnConnection(cmd).__dict__)

    @classmethod
    def listVpnGateway(cls, apiclient, **kwargs):
        """List all VPN Gateways matching criteria"""
        cmd = listVpnGateways.listVpnGatewaysCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listVpnGateways(cmd))

    @classmethod
    def listVpnConnection(cls, apiclient, **kwargs):
        """List all VPN Connections matching criteria"""
        cmd = listVpnConnections.listVpnConnectionsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listVpnConnections(cmd))

    def delete(self, apiclient):
        """Delete remote VPN access"""

        cmd = deleteRemoteAccessVpn.deleteRemoteAccessVpnCmd()
        cmd.publicipid = self.publicipid
        apiclient.deleteRemoteAccessVpn(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all VPN matching criteria"""

        cmd = listRemoteAccessVpns.listRemoteAccessVpnsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listRemoteAccessVpns(cmd))


class VpnUser:
    """Manage VPN user"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, username, password, account=None, domainid=None,
               projectid=None, rand_name=True):
        """Create VPN user"""
        cmd = addVpnUser.addVpnUserCmd()
        cmd.username = "-".join([username,
                                 random_gen()]) if rand_name else username
        cmd.password = password

        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        if projectid:
            cmd.projectid = projectid
        return VpnUser(apiclient.addVpnUser(cmd).__dict__)

    def delete(self, apiclient, projectid=None):
        """Remove VPN user"""

        cmd = removeVpnUser.removeVpnUserCmd()
        cmd.username = self.username
        if projectid:
            cmd.projectid = projectid
        else:
            cmd.account = self.account
            cmd.domainid = self.domainid
        apiclient.removeVpnUser(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all VPN Users matching criteria"""

        cmd = listVpnUsers.listVpnUsersCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listVpnUsers(cmd))


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
        if "securitygroupenabled" in services:
            cmd.securitygroupenabled = services["securitygroupenabled"]

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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updateZone(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all Zones matching criteria"""

        cmd = listZones.listZonesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
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
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return apiclient.listPods(cmd)

    @classmethod
    def update(self, apiclient, **kwargs):
        """Update the pod"""

        cmd = updatePod.updatePodCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updatePod(cmd)

class PublicIpRange:
    """Manage VlanIpRange"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, account=None, domainid=None):
        """Create VlanIpRange"""

        cmd = createVlanIpRange.createVlanIpRangeCmd()
        cmd.gateway = services["gateway"]
        cmd.netmask = services["netmask"]
        cmd.forvirtualnetwork = services["forvirtualnetwork"]
        cmd.startip = services["startip"]
        cmd.endip = services["endip"]
        cmd.zoneid = services["zoneid"]
        if "podid" in services:
            cmd.podid = services["podid"]
        cmd.vlan = services["vlan"]

        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid

        return PublicIpRange(apiclient.createVlanIpRange(cmd).__dict__)

    def delete(self, apiclient):
        """Delete VlanIpRange"""

        cmd = deleteVlanIpRange.deleteVlanIpRangeCmd()
        cmd.id = self.vlan.id
        apiclient.deleteVlanIpRange(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all VLAN IP ranges."""

        cmd = listVlanIpRanges.listVlanIpRangesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listVlanIpRanges(cmd))

    @classmethod
    def dedicate(
            cls, apiclient, id, account=None, domainid=None, projectid=None):
        """Dedicate VLAN IP range"""

        cmd = dedicatePublicIpRange.dedicatePublicIpRangeCmd()
        cmd.id = id
        cmd.account = account
        cmd.domainid = domainid
        cmd.projectid = projectid
        return PublicIpRange(apiclient.dedicatePublicIpRange(cmd).__dict__)

    def release(self, apiclient):
        """Release VLAN IP range"""

        cmd = releasePublicIpRange.releasePublicIpRangeCmd()
        cmd.id = self.vlan.id
        return apiclient.releasePublicIpRange(cmd)


class PortablePublicIpRange:
    """Manage portable public Ip Range"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):
        """Create portable public Ip Range"""

        cmd = createPortableIpRange.createPortableIpRangeCmd()
        cmd.gateway = services["gateway"]
        cmd.netmask = services["netmask"]
        cmd.startip = services["startip"]
        cmd.endip = services["endip"]
        cmd.regionid = services["regionid"]

        if "vlan" in services:
            cmd.vlan = services["vlan"]

        return PortablePublicIpRange(
            apiclient.createPortableIpRange(cmd).__dict__)

    def delete(self, apiclient):
        """Delete portable IpRange"""

        cmd = deletePortableIpRange.deletePortableIpRangeCmd()
        cmd.id = self.id
        apiclient.deletePortableIpRange(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all portable public IP ranges."""

        cmd = listPortableIpRanges.listPortableIpRangesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listPortableIpRanges(cmd))

class SecondaryStagingStore:
    """Manage Staging Store"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, url, provider, services=None):
        """Create Staging Storage"""
        cmd = createSecondaryStagingStore.createSecondaryStagingStoreCmd()
        cmd.url = url
        cmd.provider = provider
        if services:
            if "zoneid" in services:
                cmd.zoneid = services["zoneid"]
            if "details" in services:
                cmd.details = services["details"]
            if "scope" in services:
                cmd.scope = services["scope"]

        return SecondaryStagingStore(apiclient.createSecondaryStagingStore(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Staging Storage"""
        cmd = deleteSecondaryStagingStore.deleteSecondaryStagingStoreCmd()
        cmd.id = self.id
        apiclient.deleteSecondaryStagingStore(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        cmd = listSecondaryStagingStores.listSecondaryStagingStoresCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listSecondaryStagingStores(cmd))


class ImageStore:
    """Manage image stores"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, url, provider, services=None):
        """Add Image Store"""
        cmd = addImageStore.addImageStoreCmd()
        cmd.url = url
        cmd.provider = provider
        if services:
            if "zoneid" in services:
                cmd.zoneid = services["zoneid"]
            if "details" in services:
                cmd.details = services["details"]
            if "scope" in services:
                cmd.scope = services["scope"]

        return ImageStore(apiclient.addImageStore(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Image Store"""
        cmd = deleteImageStore.deleteImageStoreCmd()
        cmd.id = self.id
        apiclient.deleteImageStore(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        cmd = listImageStores.listImageStoresCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listImageStores(cmd))


class PhysicalNetwork:
    """Manage physical network storage"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, zoneid, domainid=None):
        """Create physical network"""
        cmd = createPhysicalNetwork.createPhysicalNetworkCmd()

        cmd.name = services["name"]
        cmd.zoneid = zoneid
        if domainid:
            cmd.domainid = domainid
        return PhysicalNetwork(apiclient.createPhysicalNetwork(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Physical Network"""

        cmd = deletePhysicalNetwork.deletePhysicalNetworkCmd()
        cmd.id = self.id
        apiclient.deletePhysicalNetwork(cmd)

    def update(self, apiclient, **kwargs):
        """Update Physical network state"""

        cmd = updatePhysicalNetwork.updatePhysicalNetworkCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updatePhysicalNetwork(cmd)

    def addTrafficType(self, apiclient, type):
        """Add Traffic type to Physical network"""

        cmd = addTrafficType.addTrafficTypeCmd()
        cmd.physicalnetworkid = self.id
        cmd.traffictype = type
        return apiclient.addTrafficType(cmd)

    @classmethod
    def dedicate(cls, apiclient, vlanrange, physicalnetworkid,
                 account=None, domainid=None, projectid=None):
        """Dedicate guest vlan range"""

        cmd = dedicateGuestVlanRange.dedicateGuestVlanRangeCmd()
        cmd.vlanrange = vlanrange
        cmd.physicalnetworkid = physicalnetworkid
        cmd.account = account
        cmd.domainid = domainid
        cmd.projectid = projectid
        return PhysicalNetwork(apiclient.dedicateGuestVlanRange(cmd).__dict__)

    def release(self, apiclient):
        """Release guest vlan range"""

        cmd = releaseDedicatedGuestVlanRange.\
            releaseDedicatedGuestVlanRangeCmd()
        cmd.id = self.id
        return apiclient.releaseDedicatedGuestVlanRange(cmd)

    @classmethod
    def listDedicated(cls, apiclient, **kwargs):
        """Lists all dedicated guest vlan ranges"""

        cmd = listDedicatedGuestVlanRanges.listDedicatedGuestVlanRangesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return apiclient.listDedicatedGuestVlanRanges(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all physical networks"""

        cmd = listPhysicalNetworks.listPhysicalNetworksCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return map(lambda pn: PhysicalNetwork(
            pn.__dict__), apiclient.listPhysicalNetworks(cmd))


class SecurityGroup:
    """Manage Security Groups"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, account=None, domainid=None,
               description=None, projectid=None):
        """Create security group"""
        cmd = createSecurityGroup.createSecurityGroupCmd()

        cmd.name = "-".join([services["name"], random_gen()])
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        if description:
            cmd.description = description
        if projectid:
            cmd.projectid = projectid

        return SecurityGroup(apiclient.createSecurityGroup(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Security Group"""

        cmd = deleteSecurityGroup.deleteSecurityGroupCmd()
        cmd.id = self.id
        apiclient.deleteSecurityGroup(cmd)

    def authorize(self, apiclient, services,
                  account=None, domainid=None, projectid=None):
        """Authorize Ingress Rule"""

        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()

        if domainid:
            cmd.domainid = domainid
        if account:
            cmd.account = account

        if projectid:
            cmd.projectid = projectid
        cmd.securitygroupid = self.id
        cmd.protocol = services["protocol"]

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

        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = id
        return apiclient.revokeSecurityGroupIngress(cmd)

    def authorizeEgress(self, apiclient, services, account=None, domainid=None,
                        projectid=None, user_secgrp_list={}):
        """Authorize Egress Rule"""

        cmd = authorizeSecurityGroupEgress.authorizeSecurityGroupEgressCmd()

        if domainid:
            cmd.domainid = domainid
        if account:
            cmd.account = account

        if projectid:
            cmd.projectid = projectid
        cmd.securitygroupid = self.id
        cmd.protocol = services["protocol"]

        if services["protocol"] == 'ICMP':
            cmd.icmptype = -1
            cmd.icmpcode = -1
        else:
            cmd.startport = services["startport"]
            cmd.endport = services["endport"]

        cmd.cidrlist = services["cidrlist"]

        cmd.usersecuritygrouplist = []
        for account, group in user_secgrp_list.items():
            cmd.usersecuritygrouplist.append({
                'account': account,
                'group': group
            })

        return (apiclient.authorizeSecurityGroupEgress(cmd).__dict__)

    def revokeEgress(self, apiclient, id):
        """Revoke Egress rule"""

        cmd = revokeSecurityGroupEgress.revokeSecurityGroupEgressCmd()
        cmd.id = id
        return apiclient.revokeSecurityGroupEgress(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all security groups."""

        cmd = listSecurityGroups.listSecurityGroupsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listSecurityGroups(cmd))


class VpnCustomerGateway:
    """Manage VPN Customer Gateway"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, name, gateway, cidrlist,
               account=None, domainid=None):
        """Create VPN Customer Gateway"""
        cmd = createVpnCustomerGateway.createVpnCustomerGatewayCmd()
        cmd.name = name
        cmd.gateway = gateway
        cmd.cidrlist = cidrlist
        if "ipsecpsk" in services:
            cmd.ipsecpsk = services["ipsecpsk"]
        if "ikepolicy" in services:
            cmd.ikepolicy = services["ikepolicy"]
        if "ikelifetime" in services:
            cmd.ikelifetime = services["ikelifetime"]
        if "esppolicy" in services:
            cmd.esppolicy = services["esppolicy"]
        if "esplifetime" in services:
            cmd.esplifetime = services["esplifetime"]
        if "dpd" in services:
            cmd.dpd = services["dpd"]
        if "forceencap" in services:
            cmd.forceencap = services["forceencap"]
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        return VpnCustomerGateway(
            apiclient.createVpnCustomerGateway(cmd).__dict__)

    def update(self, apiclient, services, name, gateway, cidrlist):
        """Updates VPN Customer Gateway"""

        cmd = updateVpnCustomerGateway.updateVpnCustomerGatewayCmd()
        cmd.id = self.id
        cmd.name = name
        cmd.gateway = gateway
        cmd.cidrlist = cidrlist
        if "ipsecpsk" in services:
            cmd.ipsecpsk = services["ipsecpsk"]
        if "ikepolicy" in services:
            cmd.ikepolicy = services["ikepolicy"]
        if "ikelifetime" in services:
            cmd.ikelifetime = services["ikelifetime"]
        if "esppolicy" in services:
            cmd.esppolicy = services["esppolicy"]
        if "esplifetime" in services:
            cmd.esplifetime = services["esplifetime"]
        if "dpd" in services:
            cmd.dpd = services["dpd"]
        if "forceencap" in services:
            cmd.forceencap = services["forceencap"]
        return(apiclient.updateVpnCustomerGateway(cmd))

    def delete(self, apiclient):
        """Delete VPN Customer Gateway"""

        cmd = deleteVpnCustomerGateway.deleteVpnCustomerGatewayCmd()
        cmd.id = self.id
        apiclient.deleteVpnCustomerGateway(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all VPN customer Gateway"""

        cmd = listVpnCustomerGateways.listVpnCustomerGatewaysCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listVpnCustomerGateways(cmd))


class Project:
    """Manage Project life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, account=None, domainid=None):
        """Create project"""

        cmd = createProject.createProjectCmd()
        cmd.displaytext = services["displaytext"]
        cmd.name = "-".join([services["name"], random_gen()])
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid

        return Project(apiclient.createProject(cmd).__dict__)

    def delete(self, apiclient):
        """Delete Project"""

        cmd = deleteProject.deleteProjectCmd()
        cmd.id = self.id
        apiclient.deleteProject(cmd)

    def update(self, apiclient, **kwargs):
        """Updates the project"""

        cmd = updateProject.updateProjectCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updateProject(cmd)

    def activate(self, apiclient):
        """Activates the suspended project"""

        cmd = activateProject.activateProjectCmd()
        cmd.id = self.id
        return apiclient.activateProject(cmd)

    def suspend(self, apiclient):
        """Suspend the active project"""

        cmd = suspendProject.suspendProjectCmd()
        cmd.id = self.id
        return apiclient.suspendProject(cmd)

    def addAccount(self, apiclient, account=None, email=None):
        """Add account to project"""

        cmd = addAccountToProject.addAccountToProjectCmd()
        cmd.projectid = self.id
        if account:
            cmd.account = account
        if email:
            cmd.email = email
        return apiclient.addAccountToProject(cmd)

    def deleteAccount(self, apiclient, account):
        """Delete account from project"""

        cmd = deleteAccountFromProject.deleteAccountFromProjectCmd()
        cmd.projectid = self.id
        cmd.account = account
        return apiclient.deleteAccountFromProject(cmd)

    @classmethod
    def listAccounts(cls, apiclient, **kwargs):
        """Lists all accounts associated with projects."""

        cmd = listProjectAccounts.listProjectAccountsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listProjectAccounts(cmd))

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists all projects."""

        cmd = listProjects.listProjectsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listProjects(cmd))


class ProjectInvitation:
    """Manage project invitations"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def update(cls, apiclient, projectid, accept, account=None, token=None):
        """Updates the project invitation for that account"""

        cmd = updateProjectInvitation.updateProjectInvitationCmd()
        cmd.projectid = projectid
        cmd.accept = accept
        if account:
            cmd.account = account
        if token:
            cmd.token = token

        return (apiclient.updateProjectInvitation(cmd).__dict__)

    def delete(self, apiclient, id):
        """Deletes the project invitation"""

        cmd = deleteProjectInvitation.deleteProjectInvitationCmd()
        cmd.id = id
        return apiclient.deleteProjectInvitation(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists project invitations"""

        cmd = listProjectInvitations.listProjectInvitationsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listProjectInvitations(cmd))


class Configurations:
    """Manage Configuration"""

    @classmethod
    def update(cls, apiclient, name, value=None, zoneid=None, clusterid=None, storageid=None):
        """Updates the specified configuration"""

        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.value = value

        if zoneid:
            cmd.zoneid = zoneid
        if clusterid:
            cmd.clusterid = clusterid
        if storageid:
            cmd.storageid=storageid
        apiclient.updateConfiguration(cmd)


    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists configurations"""

        cmd = listConfigurations.listConfigurationsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listConfigurations(cmd))

    @classmethod
    def listCapabilities(cls, apiclient, **kwargs):
        """Lists capabilities"""
        cmd = listCapabilities.listCapabilitiesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listCapabilities(cmd))

class NetScaler:
    """Manage external netscaler device"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def add(cls, apiclient, services, physicalnetworkid,
            username=None, password=None):
        """Add external netscaler device to cloudstack"""

        cmd = addNetscalerLoadBalancer.addNetscalerLoadBalancerCmd()
        cmd.physicalnetworkid = physicalnetworkid
        if username:
            cmd.username = username
        else:
            cmd.username = services["username"]

        if password:
            cmd.password = password
        else:
            cmd.password = services["password"]

        cmd.networkdevicetype = services["networkdevicetype"]

        # Generate the URL
        url = 'https://' + str(services["ipaddress"]) + '?'
        url = url + 'publicinterface=' + str(services["publicinterface"]) + '&'
        url = url + 'privateinterface=' + \
            str(services["privateinterface"]) + '&'
        url = url + 'numretries=' + str(services["numretries"]) + '&'

        if "lbdevicecapacity" in services:
            url = url + 'lbdevicecapacity=' + \
                str(services["lbdevicecapacity"]) + '&'

        url = url + 'lbdevicededicated=' + str(services["lbdevicededicated"])

        cmd.url = url
        return NetScaler(apiclient.addNetscalerLoadBalancer(cmd).__dict__)

    def delete(self, apiclient):
        """Deletes a netscaler device from CloudStack"""

        cmd = deleteNetscalerLoadBalancer.deleteNetscalerLoadBalancerCmd()
        cmd.lbdeviceid = self.lbdeviceid
        apiclient.deleteNetscalerLoadBalancer(cmd)
        return

    def configure(self, apiclient, **kwargs):
        """List already registered netscaler devices"""

        cmd = configureNetscalerLoadBalancer.\
            configureNetscalerLoadBalancerCmd()
        cmd.lbdeviceid = self.lbdeviceid
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.configureNetscalerLoadBalancer(cmd))

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List already registered netscaler devices"""

        cmd = listNetscalerLoadBalancers.listNetscalerLoadBalancersCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listNetscalerLoadBalancers(cmd))

class NiciraNvp:

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def add(cls, apiclient, services, physicalnetworkid,
            hostname=None, username=None, password=None, transportzoneuuid=None, l2gatewayserviceuuid=None):
        cmd = addNiciraNvpDevice.addNiciraNvpDeviceCmd()
        cmd.physicalnetworkid = physicalnetworkid
        if hostname:
            cmd.hostname = hostname
        else:
            cmd.hostname = services['hostname']

        if username:
            cmd.username = username
        else:
            cmd.username = services['username']

        if password:
            cmd.password = password
        else:
            cmd.password = services['password']

        if transportzoneuuid:
            cmd.transportzoneuuid = transportzoneuuid
        else:
            cmd.transportzoneuuid = services['transportZoneUuid']

        if l2gatewayserviceuuid:
            cmd.l2gatewayserviceuuid = l2gatewayserviceuuid
        elif services and 'l2gatewayserviceuuid' in services:
            cmd.l2gatewayserviceuuid = services['l2gatewayserviceuuid']

	return NiciraNvp(apiclient.addNiciraNvpDevice(cmd).__dict__)

    def delete(self, apiclient):
        cmd = deleteNiciraNvpDevice.deleteNiciraNvpDeviceCmd()
        cmd.nvpdeviceid = self.nvpdeviceid
        apiclient.deleteNiciraNvpDevice(cmd)
        return

    @classmethod
    def list(cls, apiclient, **kwargs):
        cmd = listNiciraNvpDevices.listNiciraNvpDevicesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listNiciraNvpDevices(cmd))


class NetworkServiceProvider:
    """Manage network serivce providers for CloudStack"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def add(cls, apiclient, name, physicalnetworkid, servicelist):
        """Adds network service provider"""

        cmd = addNetworkServiceProvider.addNetworkServiceProviderCmd()
        cmd.name = name
        cmd.physicalnetworkid = physicalnetworkid
        cmd.servicelist = servicelist
        return NetworkServiceProvider(
            apiclient.addNetworkServiceProvider(cmd).__dict__)

    def delete(self, apiclient):
        """Deletes network service provider"""

        cmd = deleteNetworkServiceProvider.deleteNetworkServiceProviderCmd()
        cmd.id = self.id
        return apiclient.deleteNetworkServiceProvider(cmd)

    def update(self, apiclient, **kwargs):
        """Updates network service provider"""

        cmd = updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updateNetworkServiceProvider(cmd)

    @classmethod
    def update(cls, apiclient, id, **kwargs):
        """Updates network service provider"""

        cmd = updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
        cmd.id = id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updateNetworkServiceProvider(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List network service providers"""

        cmd = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listNetworkServiceProviders(cmd))


class Nuage:
    """Manage external nuage VSD device"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def add(cls, apiclient, services, physicalnetworkid, username=None, password=None):
        """Add external nuage VSD device to cloudstack"""

        cmd = addNuageVspDevice.addNuageVspDeviceCmd()
        cmd.physicalnetworkid = physicalnetworkid
        if username:
            cmd.username = username
        else:
            cmd.username = services["username"]

        if password:
            cmd.password = password
        else:
            cmd.password = services["password"]

        cmd.hostname = services["hostname"]
        cmd.port = services["port"]
        cmd.retrycount = services["retrycount"]
        cmd.retryinterval = services["retryinterval"]
        cmd.apiversion = services["apiversion"]

        return Nuage(apiclient.addNuageVspDevice(cmd).__dict__)

    def update(self, apiclient, **kwargs):
        """Deletes a nuage VSD device from CloudStack"""

        cmd = updateNuageVspDevice.updateNuageVspDeviceCmd()
        cmd.physicalnetworkid = self.physicalnetworkid
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return apiclient.updateNuageVspDevice(cmd)

    def delete(self, apiclient):
        """Deletes a nuage VSD device from CloudStack"""

        cmd = deleteNuageVspDevice.deleteNuageVspDeviceCmd()
        cmd.vspdeviceid = self.vspdeviceid
        apiclient.deleteNuageVspDevice(cmd)
        return

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List already registered netscaler devices"""

        cmd = listNuageVspDevices.listNuageVspDevicesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listNuageVspDevices(cmd))


class Router:
    """Manage router life cycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def start(cls, apiclient, id):
        """Starts the router"""
        cmd = startRouter.startRouterCmd()
        cmd.id = id
        return apiclient.startRouter(cmd)

    @classmethod
    def stop(cls, apiclient, id, forced=None):
        """Stops the router"""
        cmd = stopRouter.stopRouterCmd()
        cmd.id = id
        if forced:
            cmd.forced = forced
        return apiclient.stopRouter(cmd)

    @classmethod
    def reboot(cls, apiclient, id):
        """Reboots the router"""
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = id
        return apiclient.rebootRouter(cmd)

    @classmethod
    def destroy(cls, apiclient, id):
        """Destroy the router"""
        cmd = destroyRouter.destroyRouterCmd()
        cmd.id = id
        return apiclient.destroyRouter(cmd)

    @classmethod
    def change_service_offering(cls, apiclient, id, serviceofferingid):
        """Change service offering of the router"""
        cmd = changeServiceForRouter.changeServiceForRouterCmd()
        cmd.id = id
        cmd.serviceofferingid = serviceofferingid
        return apiclient.changeServiceForRouter(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List routers"""

        cmd = listRouters.listRoutersCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listRouters(cmd))


class Tag:
    """Manage tags"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, resourceIds, resourceType, tags):
        """Create tags"""

        cmd = createTags.createTagsCmd()
        cmd.resourceIds = resourceIds
        cmd.resourcetype = resourceType
        cmd.tags = []
        for key, value in tags.items():
            cmd.tags.append({
                'key': key,
                'value': value
            })
        return Tag(apiclient.createTags(cmd).__dict__)

    def delete(self, apiclient, resourceIds, resourceType, tags):
        """Delete tags"""

        cmd = deleteTags.deleteTagsCmd()
        cmd.resourceIds = resourceIds
        cmd.resourcetype = resourceType
        cmd.tags = []
        for key, value in tags.items():
            cmd.tags.append({
                'key': key,
                'value': value
            })
        apiclient.deleteTags(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all tags matching the criteria"""

        cmd = listTags.listTagsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listTags(cmd))


class VpcOffering:
    """Manage VPC offerings"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):
        """Create vpc offering"""

        import logging

        cmd = createVPCOffering.createVPCOfferingCmd()
        cmd.name = "-".join([services["name"], random_gen()])
        cmd.displaytext = services["displaytext"]
        cmd.supportedServices = services["supportedservices"]
        if "serviceProviderList" in services:
            for service, provider in services["serviceProviderList"].items():
                providers = provider
                if isinstance(provider, str):
                    providers = [provider]

                for provider_item in providers:
                    cmd.serviceproviderlist.append({
                        'service': service,
                        'provider': provider_item
                    })

        if "serviceCapabilityList" in services:
            cmd.servicecapabilitylist = []
            for service, capability in \
                services["serviceCapabilityList"].items():
                for ctype, value in capability.items():
                    cmd.servicecapabilitylist.append({
                        'service': service,
                        'capabilitytype': ctype,
                        'capabilityvalue': value
                    })
        return VpcOffering(apiclient.createVPCOffering(cmd).__dict__)

    def update(self, apiclient, name=None, displaytext=None, state=None):
        """Updates existing VPC offering"""

        cmd = updateVPCOffering.updateVPCOfferingCmd()
        cmd.id = self.id
        if name:
            cmd.name = name
        if displaytext:
            cmd.displaytext = displaytext
        if state:
            cmd.state = state
        return apiclient.updateVPCOffering(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List the VPC offerings based on criteria specified"""

        cmd = listVPCOfferings.listVPCOfferingsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listVPCOfferings(cmd))

    def delete(self, apiclient):
        """Deletes existing VPC offering"""

        cmd = deleteVPCOffering.deleteVPCOfferingCmd()
        cmd.id = self.id
        return apiclient.deleteVPCOffering(cmd)


class VPC:
    """Manage Virtual Private Connection"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, vpcofferingid,
               zoneid, networkDomain=None, account=None,
               domainid=None, **kwargs):
        """Creates the virtual private connection (VPC)"""

        cmd = createVPC.createVPCCmd()
        cmd.name = "-".join([services["name"], random_gen()])
        cmd.displaytext = "-".join([services["displaytext"], random_gen()])
        cmd.vpcofferingid = vpcofferingid
        cmd.zoneid = zoneid
        if "cidr" in services:
            cmd.cidr = services["cidr"]
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        if networkDomain:
            cmd.networkDomain = networkDomain
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return VPC(apiclient.createVPC(cmd).__dict__)

    def update(self, apiclient, name=None, displaytext=None):
        """Updates VPC configurations"""

        cmd = updateVPC.updateVPCCmd()
        cmd.id = self.id
        if name:
            cmd.name = name
        if displaytext:
            cmd.displaytext = displaytext
        return (apiclient.updateVPC(cmd))

    def delete(self, apiclient):
        """Delete VPC network"""

        cmd = deleteVPC.deleteVPCCmd()
        cmd.id = self.id
        return apiclient.deleteVPC(cmd)

    def restart(self, apiclient):
        """Restarts the VPC connections"""

        cmd = restartVPC.restartVPCCmd()
        cmd.id = self.id
        return apiclient.restartVPC(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List VPCs"""

        cmd = listVPCs.listVPCsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listVPCs(cmd))


class PrivateGateway:
    """Manage private gateway lifecycle"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, gateway, ipaddress, netmask, vlan, vpcid,
               physicalnetworkid=None, aclid=None):
        """Create private gateway"""

        cmd = createPrivateGateway.createPrivateGatewayCmd()
        cmd.gateway = gateway
        cmd.ipaddress = ipaddress
        cmd.netmask = netmask
        cmd.vlan = vlan
        cmd.vpcid = vpcid
        if physicalnetworkid:
            cmd.physicalnetworkid = physicalnetworkid
        if aclid:
            cmd.aclid = aclid

        return PrivateGateway(apiclient.createPrivateGateway(cmd).__dict__)

    def delete(self, apiclient):
        """Delete private gateway"""

        cmd = deletePrivateGateway.deletePrivateGatewayCmd()
        cmd.id = self.id
        return apiclient.deletePrivateGateway(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List private gateways"""

        cmd = listPrivateGateways.listPrivateGatewaysCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listPrivateGateways(cmd))


class AffinityGroup:
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, aff_grp, account=None, domainid=None, projectid=None):
        cmd = createAffinityGroup.createAffinityGroupCmd()
        cmd.name = aff_grp['name']
        cmd.displayText = aff_grp['name']
        cmd.type = aff_grp['type']
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        if projectid:
            cmd.projectid = projectid
        return AffinityGroup(apiclient.createAffinityGroup(cmd).__dict__)

    def update(self, apiclient):
        pass

    def delete(self, apiclient):
        cmd = deleteAffinityGroup.deleteAffinityGroupCmd()
        cmd.id = self.id
        return apiclient.deleteAffinityGroup(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        cmd = listAffinityGroups.listAffinityGroupsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return apiclient.listAffinityGroups(cmd)

class StaticRoute:
    """Manage static route lifecycle"""
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, cidr, gatewayid):
        """Create static route"""

        cmd = createStaticRoute.createStaticRouteCmd()
        cmd.cidr = cidr
        cmd.gatewayid = gatewayid
        return StaticRoute(apiclient.createStaticRoute(cmd).__dict__)

    def delete(self, apiclient):
        """Delete static route"""

        cmd = deleteStaticRoute.deleteStaticRouteCmd()
        cmd.id = self.id
        return apiclient.deleteStaticRoute(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List static route"""

        cmd = listStaticRoutes.listStaticRoutesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listStaticRoutes(cmd))


class VNMC:
    """Manage VNMC lifecycle"""
    def __init__(self, items):
        self.__dict__.update(items)

    def create(cls, apiclient, hostname, username, password,
               physicalnetworkid):
        """Registers VNMC appliance"""

        cmd = addCiscoVnmcResource.addCiscoVnmcResourceCmd()
        cmd.hostname = hostname
        cmd.username = username
        cmd.password = password
        cmd.physicalnetworkid = physicalnetworkid
        return VNMC(apiclient.addCiscoVnmcResource(cmd))

    def delete(self, apiclient):
        """Removes VNMC appliance"""

        cmd = deleteCiscoVnmcResource.deleteCiscoVnmcResourceCmd()
        cmd.resourceid = self.resourceid
        return apiclient.deleteCiscoVnmcResource(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List VNMC appliances"""

        cmd = listCiscoVnmcResources.listCiscoVnmcResourcesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listCiscoVnmcResources(cmd))


class SSHKeyPair:
    """Manage SSH Key pairs"""

    def __init__(self, items, services):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, name=None, account=None,
               domainid=None, projectid=None):
        """Creates SSH keypair"""
        cmd = createSSHKeyPair.createSSHKeyPairCmd()
        cmd.name = name
        if account is not None:
            cmd.account = account
        if domainid is not None:
            cmd.domainid = domainid
        if projectid is not None:
            cmd.projectid = projectid
        return (apiclient.createSSHKeyPair(cmd))

    @classmethod
    def register(cls, apiclient, name, publickey):
        """Registers SSH keypair"""
        cmd = registerSSHKeyPair.registerSSHKeyPairCmd()
        cmd.name = name
        cmd.publickey = publickey
        return (apiclient.registerSSHKeyPair(cmd))

    def delete(self, apiclient):
        """Delete SSH key pair"""
        cmd = deleteSSHKeyPair.deleteSSHKeyPairCmd()
        cmd.name = self.name
        apiclient.deleteSSHKeyPair(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all SSH key pairs"""
        cmd = listSSHKeyPairs.listSSHKeyPairsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listSSHKeyPairs(cmd))


class Capacities:
    """Manage Capacities"""

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists capacities"""

        cmd = listCapacity.listCapacityCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listCapacity(cmd))


class Alert:
    """Manage alerts"""

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists alerts"""

        cmd = listAlerts.listAlertsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listAlerts(cmd))


class InstanceGroup:
    """Manage VM instance groups"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, name=None, account=None, domainid=None,
               projectid=None, networkid=None, rand_name=True):
        """Creates instance groups"""

        cmd = createInstanceGroup.createInstanceGroupCmd()
        cmd.name = "-".join([name, random_gen()]) if rand_name else name
        if account is not None:
            cmd.account = account
        if domainid is not None:
            cmd.domainid = domainid
        if projectid is not None:
            cmd.projectid = projectid
        if networkid is not None:
            cmd.networkid = networkid
        return InstanceGroup(apiclient.createInstanceGroup(cmd).__dict__)

    def delete(self, apiclient):
        """Delete instance group"""
        cmd = deleteInstanceGroup.deleteInstanceGroupCmd()
        cmd.id = self.id
        apiclient.deleteInstanceGroup(cmd)

    def update(self, apiclient, **kwargs):
        """Updates the instance groups"""
        cmd = updateInstanceGroup.updateInstanceGroupCmd()
        cmd.id = self.id
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return (apiclient.updateInstanceGroup(cmd))

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all instance groups"""
        cmd = listInstanceGroups.listInstanceGroupsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return (apiclient.listInstanceGroups(cmd))

    def startInstances(self, apiclient):
        """Starts all instances in a VM tier"""

        cmd = startVirtualMachine.startVirtualMachineCmd()
        cmd.group = self.id
        return apiclient.startVirtualMachine(cmd)

    def stopInstances(self, apiclient):
        """Stops all instances in a VM tier"""

        cmd = stopVirtualMachine.stopVirtualMachineCmd()
        cmd.group = self.id
        return apiclient.stopVirtualMachine(cmd)

    def rebootInstances(self, apiclient):
        """Reboot all instances in a VM tier"""

        cmd = rebootVirtualMachine.rebootVirtualMachineCmd()
        cmd.group = self.id
        return apiclient.rebootVirtualMachine(cmd)

    def deleteInstances(self, apiclient):
        """Stops all instances in a VM tier"""

        cmd = destroyVirtualMachine.destroyVirtualMachineCmd()
        cmd.group = self.id
        return apiclient.destroyVirtualMachine(cmd)

    def changeServiceOffering(self, apiclient, serviceOfferingId):
        """Change service offering of the vm tier"""

        cmd = changeServiceForVirtualMachine.\
            changeServiceForVirtualMachineCmd()
        cmd.group = self.id
        cmd.serviceofferingid = serviceOfferingId
        return apiclient.changeServiceForVirtualMachine(cmd)

    def recoverInstances(self, apiclient):
        """Recover the instances from vm tier"""
        cmd = recoverVirtualMachine.recoverVirtualMachineCmd()
        cmd.group = self.id
        apiclient.recoverVirtualMachine(cmd)


class ASA1000V:
    """Manage ASA 1000v lifecycle"""
    def create(cls, apiclient, hostname, insideportprofile,
               clusterid, physicalnetworkid):
        """Registers ASA 1000v appliance"""

        cmd = addCiscoAsa1000vResource.addCiscoAsa1000vResourceCmd()
        cmd.hostname = hostname
        cmd.insideportprofile = insideportprofile
        cmd.clusterid = clusterid
        cmd.physicalnetworkid = physicalnetworkid
        return ASA1000V(apiclient.addCiscoAsa1000vResource(cmd))

    def delete(self, apiclient):
        """Removes ASA 1000v appliance"""

        cmd = deleteCiscoAsa1000vResource.deleteCiscoAsa1000vResourceCmd()
        cmd.resourceid = self.resourceid
        return apiclient.deleteCiscoAsa1000vResource(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List ASA 1000v appliances"""

        cmd = listCiscoAsa1000vResources.listCiscoAsa1000vResourcesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listCiscoAsa1000vResources(cmd))

class VmSnapshot:
    """Manage VM Snapshot life cycle"""
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, vmid, snapshotmemory="false",
               name=None, description=None):
        cmd = createVMSnapshot.createVMSnapshotCmd()
        cmd.virtualmachineid = vmid

        if snapshotmemory:
            cmd.snapshotmemory = snapshotmemory
        if name:
            cmd.name = name
        if description:
            cmd.description = description
        return VmSnapshot(apiclient.createVMSnapshot(cmd).__dict__)

    @classmethod
    def list(cls, apiclient, **kwargs):
        cmd = listVMSnapshot.listVMSnapshotCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listVMSnapshot(cmd))

    @classmethod
    def revertToSnapshot(cls, apiclient, vmsnapshotid):
        cmd = revertToVMSnapshot.revertToVMSnapshotCmd()
        cmd.vmsnapshotid = vmsnapshotid
        return apiclient.revertToVMSnapshot(cmd)

    @classmethod
    def deleteVMSnapshot(cls, apiclient, vmsnapshotid):
        cmd = deleteVMSnapshot.deleteVMSnapshotCmd()
        cmd.vmsnapshotid = vmsnapshotid
        return apiclient.deleteVMSnapshot(cmd)

class Region:
    """ Regions related Api """
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services):
        cmd = addRegion.addRegionCmd()
        cmd.id = services["regionid"]
        cmd.endpoint = services["regionendpoint"]
        cmd.name = services["regionname"]
        try:
            region = apiclient.addRegion(cmd)
            if region is not None:
                return Region(region.__dict__)
        except Exception as e:
            raise e

    @classmethod
    def list(cls, apiclient, **kwargs):
        cmd = listRegions.listRegionsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        region = apiclient.listRegions(cmd)
        return region

    def update(self, apiclient, services):
        cmd = updateRegion.updateRegionCmd()
        cmd.id = self.id
        if services["regionendpoint"]:
            cmd.endpoint = services["regionendpoint"]
        if services["regionname"]:
            cmd.name = services["regionname"]
        region = apiclient.updateRegion(cmd)
        return region

    def delete(self, apiclient):
        cmd = removeRegion.removeRegionCmd()
        cmd.id = self.id
        region = apiclient.removeRegion(cmd)
        return region


class ApplicationLoadBalancer:
    """Manage Application Load Balancers in VPC"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, services, name=None, sourceport=None,
               instanceport=22, algorithm="roundrobin", scheme="internal",
               sourcenetworkid=None, networkid=None, sourceipaddress=None):
        """Create Application Load Balancer"""
        cmd = createLoadBalancer.createLoadBalancerCmd()

        if "name" in services:
            cmd.name = services["name"]
        elif name:
            cmd.name = name

        if "sourceport" in services:
            cmd.sourceport = services["sourceport"]
        elif sourceport:
            cmd.sourceport = sourceport

        if "instanceport" in services:
            cmd.instanceport = services["instanceport"]
        elif instanceport:
            cmd.instanceport = instanceport

        if "algorithm" in services:
            cmd.algorithm = services["algorithm"]
        elif algorithm:
            cmd.algorithm = algorithm

        if "scheme" in services:
            cmd.scheme = services["scheme"]
        elif scheme:
            cmd.scheme = scheme

        if "sourceipaddressnetworkid" in services:
            cmd.sourceipaddressnetworkid = services["sourceipaddressnetworkid"]
        elif sourcenetworkid:
            cmd.sourceipaddressnetworkid = sourcenetworkid

        if "networkid" in services:
            cmd.networkid = services["networkid"]
        elif networkid:
            cmd.networkid = networkid

        if "sourceipaddress" in services:
            cmd.sourceipaddress = services["sourceipaddress"]
        elif sourceipaddress:
            cmd.sourceipaddress = sourceipaddress

        return LoadBalancerRule(apiclient.createLoadBalancer(cmd).__dict__)

    def delete(self, apiclient):
        """Delete application load balancer"""
        cmd = deleteLoadBalancer.deleteLoadBalancerCmd()
        cmd.id = self.id
        apiclient.deleteLoadBalancerRule(cmd)
        return

    def assign(self, apiclient, vms=None, vmidipmap=None):
        """Assign virtual machines to load balancing rule"""
        cmd = assignToLoadBalancerRule.assignToLoadBalancerRuleCmd()
        cmd.id = self.id
        if vmidipmap:
            cmd.vmidipmap = vmidipmap
        if vms:
            cmd.virtualmachineids = [str(vm.id) for vm in vms]
        apiclient.assignToLoadBalancerRule(cmd)
        return

    def remove(self, apiclient, vms=None, vmidipmap=None):
        """Remove virtual machines from load balancing rule"""
        cmd = removeFromLoadBalancerRule.removeFromLoadBalancerRuleCmd()
        cmd.id = self.id
        if vms:
            cmd.virtualmachineids = [str(vm.id) for vm in vms]
        if vmidipmap:
            cmd.vmidipmap = vmidipmap
        apiclient.removeFromLoadBalancerRule(cmd)
        return

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List all appln load balancers"""
        cmd = listLoadBalancers.listLoadBalancersCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listLoadBalancerRules(cmd))

class Resources:
    """Manage resource limits"""

    def __init__(self, items, services):
        self.__dict__.update(items)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists resource limits"""

        cmd = listResourceLimits.listResourceLimitsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listResourceLimits(cmd))

    @classmethod
    def updateLimit(cls, apiclient, **kwargs):
        """Updates resource limits"""

        cmd = updateResourceLimit.updateResourceLimitCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateResourceLimit(cmd))

    @classmethod
    def updateCount(cls, apiclient, **kwargs):
        """Updates resource count"""

        cmd = updateResourceCount.updateResourceCountCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.updateResourceCount(cmd))

class NIC:
    """NIC related API"""
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def addIp(cls, apiclient, id, ipaddress=None):
        """Add Ip (secondary) to NIC"""
        cmd = addIpToNic.addIpToNicCmd()
        cmd.nicid = id
        if ipaddress:
            cmd.ipaddress = ipaddress
        return(apiclient.addIpToNic(cmd))

    @classmethod
    def removeIp(cls, apiclient, ipaddressid):
        """Remove secondary Ip from NIC"""
        cmd = removeIpFromNic.removeIpFromNicCmd()
        cmd.id = ipaddressid
        return(apiclient.addIpToNic(cmd))

    @classmethod
    def list(cls, apiclient, **kwargs):
        """List NICs belonging to a virtual machine"""

        cmd = listNics.listNicsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listNics(cmd))

class IAMGroup:
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, iam_grp, account=None, domainid=None):
        cmd = createIAMGroup.createIAMGroupCmd()
        cmd.name = iam_grp['name']
        cmd.description = iam_grp['description']
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        return IAMGroup(apiclient.createIAMGroup(cmd).__dict__)

    def update(self, apiclient):
        pass

    def delete(self, apiclient):
        cmd = deleteIAMGroup.deleteIAMGroupCmd()
        cmd.id = self.id
        return apiclient.deleteIAMGroup(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        cmd = listIAMGroups.listIAMGroupsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return apiclient.listIAMGroups(cmd)

    def addAccount(self, apiclient, accts):
        """Add accounts to iam group"""
        cmd = addAccountToIAMGroup.addAccountToIAMGroupCmd()
        cmd.id = self.id
        cmd.accounts = [str(acct.id) for acct in accts]
        apiclient.addAccountToIAMGroup(cmd)
        return

    def removeAccount(self, apiclient, accts):
        """ Remove accounts from iam group"""
        cmd = removeAccountFromIAMGroup.removeAccountFromIAMGroupCmd()
        cmd.id = self.id
        cmd.accounts = [str(acct.id) for acct in accts]
        apiclient.removeAccountFromIAMGroup(cmd)
        return

    def attachPolicy(self, apiclient, policies):
        """Add policies to iam group"""
        cmd = attachIAMPolicyToIAMGroup.attachIAMPolicyToIAMGroupCmd()
        cmd.id = self.id
        cmd.policies = [str(policy.id) for policy in policies]
        apiclient.attachIAMPolicyToIAMGroup(cmd)
        return

    def detachPolicy(self, apiclient, policies):
        """Remove policies from iam group"""
        cmd = removeIAMPolicyFromIAMGroup.removeIAMPolicyFromIAMGroupCmd()
        cmd.id = self.id
        cmd.policies = [str(policy.id) for policy in policies]
        apiclient.removeIAMPolicyFromIAMGroup(cmd)
        return

class IAMPolicy:
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, iam_policy, account=None, domainid=None):
        cmd = createIAMPolicy.createIAMPolicyCmd()
        cmd.name = iam_policy['name']
        cmd.description = iam_policy['description']
        if account:
            cmd.account = account
        if domainid:
            cmd.domainid = domainid
        return IAMPolicy(apiclient.createIAMPolicy(cmd).__dict__)

    def update(self, apiclient):
        pass

    def delete(self, apiclient):
        cmd = deleteIAMPolicy.deleteIAMPolicyCmd()
        cmd.id = self.id
        return apiclient.deleteIAMPolicy(cmd)

    @classmethod
    def list(cls, apiclient, **kwargs):
        cmd = listIAMPolicies.listIAMPoliciesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return apiclient.listIAMPoliciesCmd(cmd)

    def addPermission(self, apiclient, permission):
        """Add permission to iam policy"""
        cmd = addIAMPermissionToIAMPolicy.addIAMPermissionToIAMPolicyCmd()
        cmd.id = self.id
        cmd.action = permission['action']
        cmd.entitytype = permission['entitytype']
        cmd.scope = permission['scope']
        cmd.scopeid = permission['scopeid']
        apiclient.addIAMPermissionToIAMPolicy(cmd)
        return

    def removePermission(self, apiclient, permission):
        """Remove permission from iam policy"""
        cmd = removeIAMPermissionFromIAMPolicy.\
            removeIAMPermissionFromIAMPolicyCmd()
        cmd.id = self.id
        cmd.action = permission['action']
        cmd.entitytype = permission['entitytype']
        cmd.scope = permission['scope']
        cmd.scopeid = permission['scopeid']
        apiclient.removeIAMPermissionFromIAMPolicy(cmd)
        return

    def attachAccount(self, apiclient, accts):
        """Attach iam policy to accounts"""
        cmd = attachIAMPolicyToAccount.attachIAMPolicyToAccountCmd()
        cmd.id = self.id
        cmd.accounts = [str(acct.id) for acct in accts]
        apiclient.attachIAMPolicyToAccount(cmd)
        return

    def detachAccount(self, apiclient, accts):
        """Detach iam policy from accounts"""
        cmd = removeIAMPolicyFromAccount.removeIAMPolicyFromAccountCmd()
        cmd.id = self.id
        cmd.accounts = [str(acct.id) for acct in accts]
        apiclient.removeIAMPolicyFromAccount(cmd)
        return

class SimulatorMock:
    """Manage simulator mock lifecycle"""
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def create(cls, apiclient, command, zoneid=None, podid=None,
               clusterid=None, hostid=None, value="result:fail",
               count=None, jsonresponse=None, method="GET"):
        """Creates simulator mock"""
        cmd = configureSimulator.configureSimulatorCmd()
        cmd.zoneid = zoneid
        cmd.podid = podid
        cmd.clusterid = clusterid
        cmd.hostid = hostid
        cmd.name = command
        cmd.value = value
        cmd.count = count
        cmd.jsonresponse = jsonresponse
        try:
            simulatormock = apiclient.configureSimulator(cmd, method=method)
            if simulatormock is not None:
                return SimulatorMock(simulatormock.__dict__)
        except Exception as e:
            raise e

    def delete(self, apiclient):
        """Removes simulator mock"""
        cmd = cleanupSimulatorMock.cleanupSimulatorMockCmd()
        cmd.id = self.id
        return apiclient.cleanupSimulatorMock(cmd)

    def query(self, apiclient):
        """Queries simulator mock"""
        cmd = querySimulatorMock.querySimulatorMockCmd()
        cmd.id = self.id
        try:
            simulatormock = apiclient.querySimulatorMock(cmd)
            if simulatormock is not None:
                return SimulatorMock(simulatormock.__dict__)
        except Exception as e:
            raise e

class Usage:
    """Manage Usage Generation"""
    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def listRecords(cls, apiclient, **kwargs):
        """Lists domains"""
        cmd = listUsageRecords.listUsageRecordsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listUsageRecords(cmd))

    @classmethod
    def listTypes(cls, apiclient, **kwargs):
        """Lists domains"""
        cmd = listUsageTypes.listUsageTypesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        if 'account' in kwargs.keys() and 'domainid' in kwargs.keys():
            cmd.listall = True
        return(apiclient.listUsageTypes(cmd))

    @classmethod
    def generateRecords(cls, apiclient, **kwargs):
        """Lists domains"""
        cmd = generateUsageRecords.generateUsageRecordsCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.generateUsageRecords(cmd))

class TrafficType:
    """Manage different traffic types in the setup"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists traffic types"""

        cmd = listTrafficTypes.listTrafficTypesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listTrafficTypes(cmd))

class StorageNetworkIpRange:
    """Manage Storage Network Ip Range"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists Storage Network IP Ranges"""

        cmd = listStorageNetworkIpRange.listStorageNetworkIpRangeCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listStorageNetworkIpRange(cmd))

class RegisteredServicePackage:
    """Manage ServicePackage registered with NCC"""

    def __init__(self, items):
        self.__dict__.update(items)

    @classmethod
    def list(cls, apiclient, **kwargs):
        """Lists service packages published by NCC"""

        cmd = listRegisteredServicePackages.listRegisteredServicePackagesCmd()
        [setattr(cmd, k, v) for k, v in kwargs.items()]
        return(apiclient.listRegisteredServicePackages(cmd))

