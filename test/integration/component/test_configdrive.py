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

""" Component tests for user data, meta data, ssh keys
    and password reset functionality with
    ConfigDrive
"""
import base64
import os
import socket
# Import Local Modules
import subprocess
import tempfile
from contextlib import contextmanager

import time
from marvin.cloudstackAPI import (restartVPC)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             createVlanIpRange,
                             Configurations,
                             FireWallRule,
                             Host,
                             listVlanIpRanges,
                             Network,
                             NetworkACL,
                             NetworkACLList,
                             NetworkOffering,
                             NetworkServiceProvider,
                             PublicIPAddress,
                             Router,
                             ServiceOffering,
                             createSSHKeyPair,
                             deleteSSHKeyPair,
                             StaticNATRule,
                             VirtualMachine,
                             VPC,
                             VpcOffering,
                             Hypervisor, Template)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone, get_test_template,
                               is_config_suitable)
from marvin.lib.utils import random_gen
# Import System Modules
from nose.plugins.attrib import attr
from retry import retry

VPC_SERVICES = 'Dhcp,StaticNat,SourceNat,NetworkACL,UserData,Dns'
ISO_SERVICES = 'Dhcp,SourceNat,StaticNat,UserData,Firewall,Dns'

NO_SUCH_FILE = "No such file or directory"


class MySSHKeyPair:
    """Manage SSH Key pairs"""

    def __init__(self, items):
        self.private_key_file = None
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
        return MySSHKeyPair(apiclient.createSSHKeyPair(cmd).__dict__)

    def delete(self, apiclient):
        """Delete SSH key pair"""
        cmd = deleteSSHKeyPair.deleteSSHKeyPairCmd()
        cmd.name = self.name
        cmd.account = self.account
        cmd.domainid = self.domainid
        apiclient.deleteSSHKeyPair(cmd)


class Services:
    """Test Add Remove Network Services
    """

    def __init__(self):
        self.services = {
            "test_templates": {
                "kvm": {
                    "name": "Centos-5.5-configdrive",
                    "displaytext": "ConfigDrive enabled CentOS",
                    "format": "qcow2",
                    "hypervisor": "kvm",
                    "ostype": "CentOS 5.5 (64-bit)",
                    "url": "http://people.apache.org/~fmaximus/centos55-extended.qcow2.bz2",
                    "requireshvm": "False",
                    "ispublic": "True",
                    "isextractable": "True"
                }
            },
            "vpc_offering_configdrive": {
                "name": 'VPC offering ConfigDrive',
                "displaytext": 'VPC offering ConfigDrive',
                "supportedservices": VPC_SERVICES,
                "serviceProviderList": {
                    "Dhcp": "VpcVirtualRouter",
                    "StaticNat": "VpcVirtualRouter",
                    "SourceNat": "VpcVirtualRouter",
                    "NetworkACL": "VpcVirtualRouter",
                    "UserData": "ConfigDrive",
                    "Dns": "VpcVirtualRouter"
                }
            },
            "vpc_network_offering_configdrive": {
                "name": 'vpc_net_off_marvin_configdrive',
                "displaytext": 'vpc_net_off_marvin_configdrive',
                "guestiptype": 'Isolated',
                "supportedservices": VPC_SERVICES,
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "ispersistent": 'True',
                "serviceProviderList": {
                    "Dhcp": "VpcVirtualRouter",
                    "StaticNat": "VpcVirtualRouter",
                    "SourceNat": "VpcVirtualRouter",
                    "NetworkACL": "VpcVirtualRouter",
                    "UserData": "ConfigDrive",
                    "Dns": "VpcVirtualRouter"
                }
            },
            "isolated_configdrive_network_offering": {
                "name": 'isolated_configdrive_net_off_marvin',
                "displaytext": 'isolated_configdrive_net_off_marvin',
                "guestiptype": 'Isolated',
                "supportedservices": ISO_SERVICES,
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "tags": 'native',
                "serviceProviderList": {
                    "Dhcp": 'VirtualRouter',
                    "StaticNat": 'VirtualRouter',
                    "SourceNat": 'VirtualRouter',
                    "Firewall": 'VirtualRouter',
                    "UserData": 'ConfigDrive',
                    "Dns": 'VirtualRouter'
                }
            },
            "shared_network_config_drive_offering": {
                "name": 'shared_network_config_drive_offering',
                "displaytext": 'shared_network_config_drive_offering',
                "guestiptype": 'shared',
                "supportedservices": 'Dhcp,UserData',
                "traffictype": 'GUEST',
                "specifyVlan": "True",
                "specifyIpRanges": "True",
                "availability": 'Optional',
                "serviceProviderList": {
                    "Dhcp": "VirtualRouter",
                    "UserData": 'ConfigDrive'
                }
            },
            "publiciprange2": {
                "gateway": "10.219.1.1",
                "netmask": "255.255.255.0",
                "startip": "10.219.1.2",
                "endip": "10.219.1.5",
                "forvirtualnetwork": "false"
            },
            "acl": {
                "network_all_1": {
                    "name": "SharedNetwork-All-1",
                    "displaytext": "SharedNetwork-All-1",
                    "vlan": "4001",
                    "gateway": "10.200.100.1",
                    "netmask": "255.255.255.0",
                    "startip": "10.200.100.21",
                    "endip": "10.200.100.100",
                    "acltype": "Domain"
                },
                "network_all_2": {
                    "name": "SharedNetwork2-All-2",
                    "displaytext": "SharedNetwork2-All-2",
                    "vlan": "4002",
                    "gateway": "10.200.200.1",
                    "netmask": "255.255.255.0",
                    "startip": "10.200.200.21",
                    "endip": "10.200.200.100",
                    "acltype": "Domain"
                }
            }
        }


class ConfigDriveUtils:
    template = None

    class CreateResult:
        def __init__(self, success, offering=None, network=None, vpc=None):
            self.success = success
            self.network = network
            self.offering = offering
            self.vpc = vpc

    class PasswordTest:
        def __init__(self, vm=None, expect_pw=None):
            """
            :param vm: vm
            :param expect_pw: Is a password expected
            """
            self.presence = expect_pw
            self.password = None

            if vm:
                self.password = vm.password
                self.presence = True

            self.test_presence = self.presence is not None

        def __str__(self):
            if self.test_presence:
                return "PasswordTest(presence=%s, password=%s)" % \
                       (self.presence, self.password)
            else:
                return "NoPasswordTest()"

    def __init__(self):
        self.offering = None
        self.vpc = None
        self.vpc_acl_list = None
        self.vpc_acl_rule = None

    @contextmanager
    def stopped_vm(self, vm):
        was_running = (vm.state == VirtualMachine.RUNNING)

        if was_running:
            vm.stop(self.api_client)
            vm.state = VirtualMachine.STOPPED

        yield

        if was_running:
            vm.start(self.api_client)
            vm.state = VirtualMachine.RUNNING
            vm.ssh_client = None

    def update_template(self, **kwargs):
        """Updates value of the guest VM template's password enabled setting
        :param passwordenabled:
        :type passwordenabled: bool
        """
        self.debug("Updating value of guest VM template's password enabled "
                   "setting")
        if not isinstance(self.template, Template):
            self.template = Template(self.template.__dict__)

        Template.update(self.template, self.api_client, **kwargs)
        response = Template.list(self.api_client,
                                 templatefilter="all",
                                 id=self.template.id)
        self.template = Template(response[0].__dict__)
        self.debug("Updated guest VM template")

    def get_userdata_url(self, vm):
        """Returns user data URL for the given VM object"""
        self.debug("Getting user data url")
        nic = vm.nic[0]
        gateway = str(nic.gateway)
        self.debug("Gateway: " + gateway)
        user_data_url = 'curl "http://' + gateway + ':80/latest/user-data"'
        return user_data_url

    def generate_ssh_keys(self):
        """Generates ssh key pair

        Writes the private key into a temp file and returns the file name

        :returns: generated keypair
        :rtype: MySSHKeyPair
        """
        self.keypair = MySSHKeyPair.create(
            self.api_client,
            name=random_gen() + ".pem",
            account=self.account.user[0].account,
            domainid=self.account.domainid)

        self.cleanup.append(self.keypair)
        self.debug("Created keypair with name: %s" % self.keypair.name)
        self.debug("Writing the private key to local file")
        pkfile = tempfile.gettempdir() + os.sep + self.keypair.name
        self.keypair.private_key_file = pkfile
        self.tmp_files.append(pkfile)
        self.debug("File path: %s" % pkfile)
        with open(pkfile, "w+") as f:
            f.write(self.keypair.privatekey)
        os.chmod(pkfile, 0o400)

        return self.keypair

    def validate_acl_rule(self, fw_rule):
        pass

    def validate_vm_networking(self, vm):
        pass

    def validate_network_networking(self, network, vpc):
        pass

    def validate_shared_networking(self, network, vm):
        pass

    def validate_StaticNat_rule_For_VM(self, public_ip, network, vm):
        self.validate_PublicIPAddress(
            public_ip, network, static_nat=True, vm=vm)

    # =========================================================================
    # ---             Config Drive Validation helper methods                ---
    # =========================================================================

    def _mount_config_drive(self, ssh):
        """
        This method is to verify whether configdrive iso
        is attached to vm or not
        Returns mount path if config drive is attached else None
        """
        mountdir = "/root/iso"
        cmd = "blkid -t LABEL='config-2' " \
              "/dev/sr? /dev/hd? /dev/sd? /dev/xvd? -o device"
        tmp_cmd = [
            'bash -c "if [ ! -d {0} ]; then mkdir {0}; fi"'.format(mountdir),
            "umount %s" % mountdir]
        self.debug("Unmounting drive from %s" % mountdir)
        for tcmd in tmp_cmd:
            ssh.execute(tcmd)

        self.debug("Trying to find ConfigDrive device")
        configDrive = ssh.execute(cmd)
        if not configDrive:
            self.warn("ConfigDrive is not attached")
            return None

        res = ssh.execute("mount {} {}".format(str(configDrive[0]), mountdir))
        if str(res).lower().find("mounting read-only") > -1:
            self.debug("ConfigDrive iso is mounted at location %s" % mountdir)
            return mountdir
        else:
            return None

    def _get_config_drive_data(self, ssh, file, name, fail_on_missing=True):
        """Fetches the content of a file file on the config drive

        :param ssh: SSH connection to the VM
        :param file: path to the file to fetch
        :param name: description of the file
        :param fail_on_missing:
                 whether the test should fail if the file is missing
        :type ssh: marvin.sshClient.SshClient
        :type file: str
        :type name: str
        :type fail_on_missing: bool
        :returns: the content of the file
        :rtype: str
        """
        cmd = "cat %s" % file
        res = ssh.execute(cmd)
        content = '\n'.join(res)

        if fail_on_missing and NO_SUCH_FILE in content:
            self.debug("{} is not found".format(name))
            self.fail("{} is not found".format(name))

        return content

    def _verify_config_drive_data(self, ssh, file, expected_content, name):
        """Verifies that the file contains the expected content

        :param ssh: SSH connection to the VM
        :param file: path to the file to verify
        :param expected_content:
        :param name:
        :type ssh: marvin.sshClient.SshClient
        :type file: str
        :type expected_content: str
        :type name: str
        """
        actual_content = self._get_config_drive_data(ssh, file, name)

        self.debug("Expected {}: {}".format(name, expected_content))
        self.debug("Actual {}: {}".format(name, actual_content))

        self.assertEqual(expected_content, actual_content,
                         'Userdata found: %s is not equal to expected: %s'
                         % (actual_content, expected_content))

    def _verify_userdata(self, ssh, mount_path, userdata):
        """
        verify Userdata

        :param ssh: SSH connection to the VM
        :param mount_path: mount point of the config drive
        :param userdata: Expected userdata
        :type ssh: marvin.sshClient.SshClient
        :type mount_path: str
        :type userdata: str
        """
        self._verify_config_drive_data(
            ssh,
            mount_path + "/cloudstack/userdata/user_data.txt",
            userdata,
            "userdata (ACS)"
        )

    def _verify_openstack_userdata(self, ssh, mount_path, userdata):
        """
        verify Userdata in Openstack format

        :param ssh: SSH connection to the VM
        :param mount_path: mount point of the config drive
        :param userdata: Expected userdata
        :type ssh: marvin.sshClient.SshClient
        :type mount_path: str
        :type userdata: str
        """
        self._verify_config_drive_data(
            ssh,
            mount_path + "/openstack/latest/user_data",
            userdata,
            "userdata (Openstack)"
        )

    def _verifyPassword(self, ssh, mount_path, password_test):
        """
        Verify Password
        :param ssh: SSH connection to the VM
        :param mount_path: Mount path of the config drive disk
        :param password_test: expected Password behavior
        :type ssh: marvin.sshClient.SshClient
        :type mount_path: str
        :type password_test: ConfigDriveUtils.PasswordTest
        """

        if not password_test.test_presence:
            return

        if password_test.password is not None:
            self.debug("Expected VM password is %s " % password_test.password)

        password_file = mount_path + "/cloudstack/password/vm_password.txt"
        vmpassword = self._get_config_drive_data(ssh, password_file,
                                                 "ConfigDrive password",
                                                 fail_on_missing=False)

        password_found = NO_SUCH_FILE not in vmpassword

        self.assertEqual(password_found, password_test.presence,
                         "Expected is that password is present: %s "
                         " but found is: %s"
                         % (password_test.presence, password_found))

        if password_test.password is not None:
            self.debug("ConfigDrive password is %s " % vmpassword)
            self.debug("Expected Password for vm is %s" %
                       password_test.password)
            self.assertTrue(password_test.password in vmpassword,
                            "Password value test failed, expected %s, was %s" %
                            (password_test.password, vmpassword))

    def _verify_ssh_key(self, ssh, mount_path, ssh_key):
        """
        Verify SSH Key
        :param ssh: SSH connection to the VM
        :param mount_path: Mount path of the config drive disk
        :param ssh_key: expected SSH key
        :type ssh: marvin.sshClient.SshClient
        :type mount_path: str
        :type ssh_key: MySSHKeyPair
        """

        self.debug("Fingerprint of Expected sshkey %s is %s " %
                   (ssh_key.name, ssh_key.fingerprint))
        publicKey_file = mount_path + "/cloudstack/metadata/public-keys.txt"
        cmd = "ssh-keygen -lf %s | cut -f2 -d' '" % publicKey_file
        res = ssh.execute(cmd)
        vmsshkey = str(res[0])

        self.debug("ConfigDrive ssh key is %s " % vmsshkey)

        if NO_SUCH_FILE in vmsshkey:
            self.fail("SSH keyfile is not found")

        self.assertEqual(
            vmsshkey,
            ssh_key.fingerprint,
            "Fingerprint of authorized key does not match ssh key fingerprint"
        )

    def _verify_metadata(self, vm, ssh, mount_path):
        """
        verify metadata files in CloudStack format

        :param vm: the VM
        :param ssh: SSH connection to the VM
        :param mount_path: mount point of the config drive
        :type vm: VirtualMachine
        :type ssh: marvin.sshClient.SshClient
        :type mount_path: str
        """

        metadata_dir = mount_path + "/cloudstack/metadata/"
        vm_files = ["availability-zone.txt",
                    "service-offering.txt",
                    "instance-id.txt",
                    "vm-id.txt",
                    "local-hostname.txt",
                    "local-ipv4.txt",
                    "public-ipv4.txt"]
        # Verify hostname if the appropriate settings are true
        configs = Configurations.list(
            self.api_client,
            name="global.allow.expose.host.hostname",
            listall=True
        )
        exposeHypevisorHostnameGS = configs[0].value

        configs = Configurations.list(
            self.api_client,
            name="account.allow.expose.host.hostname",
            listall=True
        )

        exposeHypevisorHostnameAcc = configs[0].value

        if exposeHypevisorHostnameGS == 'true' and exposeHypevisorHostnameAcc == 'true':
            vm_files.append("hypervisor-host-name.txt")

        def get_name(vm_file):
            return "{} metadata".format(
                vm_file.split('.'[-1].replace('-', ' '))
            )

        metadata = {vm_file:
                        self._get_config_drive_data(ssh,
                                                    metadata_dir + vm_file,
                                                    get_name(vm_file))
                    for vm_file in vm_files}

        self.assertEqual(
            str(metadata["availability-zone.txt"]),
            self.zone.name,
            "Zone name inside metadata does not match with the zone"
        )
        self.assertEqual(
            str(metadata["local-hostname.txt"]),
            vm.name,
            "vm name inside metadata does not match with the "
            "instance name"
        )
        self.assertEqual(
            str(metadata["vm-id.txt"]),
            vm.id,
            "vm name inside metadata does not match with the "
            "instance name"
        )
        self.assertEqual(
            str(metadata["instance-id.txt"]),
            vm.id,
            "vm name inside metadata does not match with the "
            "instance name"
        )
        self.assertEqual(
            str(metadata["service-offering.txt"]),
            vm.serviceofferingname,
            "Service offering inside metadata does not match "
            "with the instance offering"
        )

        if exposeHypevisorHostnameGS == 'true' and exposeHypevisorHostnameAcc == 'true':
            hostname = vm.hostname

            self.debug("Verifying hypervisor hostname of the VM: %s" % vm.name)
            self.assertEqual(
                str(metadata["hypervisor-host-name.txt"]),
                hostname,
                "Hostname in the metadata file does not match the host "
                "on which the VM is spawned"
            )

        return

    def _verify_openstack_metadata(self, ssh, mount_path):
        """
        verify existence of metadata and user data files
        in OpenStack format

        :param ssh: SSH connection to the VM
        :param mount_path: mount point of the config drive
        :type ssh: marvin.sshClient.SshClient
        :type mount_path: str
        """
        openstackdata_dir = mount_path + "/openstack/latest/"
        openstackdata_files = ["meta_data.json",
                               "vendor_data.json",
                               "network_data.json"]
        for file in openstackdata_files:
            res = ssh.execute("cat %s" % openstackdata_dir + file)
            if NO_SUCH_FILE in res[0]:
                self.fail("{} file not found in vm openstack".format(file))

    def _umount_config_drive(self, ssh, mount_path):
        """unmount config drive inside guest vm

        :param ssh: SSH connection to the VM
        :param mount_path: mount point of the config drive
        :type ssh: marvin.sshClient.SshClient
        :type mount_path: str
        """
        ssh.execute("umount -d %s" % mount_path)
        # Give the VM time to unlock the iso device
        time.sleep(2)
        # Verify umount
        result = ssh.execute("ls %s" % mount_path)
        self.assertTrue(len(result) == 0,
                        "After umount directory should be empty "
                        "but contains: %s" % result)

    # =========================================================================
    # ---                    Gherkin style helper methods                   ---
    # =========================================================================

    def given_template_password_enabled_is(self, new_state):
        """Updates value of the guest VM template's password enabled setting
        :param new_state:
        :type new_state: bool
        """
        orig_state = self.template.passwordenabled
        self.debug("Updating guest VM template to password enabled "
                   "from %s to %s" % (orig_state, new_state))
        if orig_state != new_state:
            self.update_template(passwordenabled=new_state)
        self.assertEqual(self.template.passwordenabled, new_state,
                         "Guest VM template is not password enabled")
        return orig_state

    def given_config_drive_provider_is(self, new_state):
        """
        Enables or disables the ConfigDrive Service Provider

        :param new_state: "Enabled" | "Disabled"
        :type new_state: str
        :return: original state
        :rtype: str
        """
        self.debug("Updating Service Provider ConfigDrive to %s" % new_state)
        configdriveprovider = self.get_configdrive_provider()
        orig_state = configdriveprovider.state
        NetworkServiceProvider.update(self.api_client,
                                      configdriveprovider.id,
                                      state=new_state)
        self.validate_NetworkServiceProvider("ConfigDrive", state=new_state)
        return orig_state

    def given_a_network_offering(self, offering_name):
        self.offering = self.create_NetworkOffering(self._get_test_data(
            offering_name))

    def given_a_network_offering_with_configdrive(self):
        self.given_a_network_offering(self.get_network_offering_name())

    def given_a_network_offering_for_vpc_with_configdrive(self):
        self.given_a_network_offering(self.get_network_offering_name_for_vpc())

    def given_a_vpc_with_offering(self, offering_name):
        self.given_config_drive_provider_is("Enabled")
        create_vpc = self.verify_vpc_creation(
            offering_name=offering_name)
        self.assertTrue(create_vpc.success,
                        "Vpc found success = %s, expected success = %s"
                        % (str(create_vpc.success), 'True'))
        self.vpc_acl_list = self.create_NetworkAclList(
            name="acl", description="acl", vpc=create_vpc.vpc)
        self.vpc_acl_rule = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], acl_list=self.vpc_acl_list)

        self.vpc = create_vpc.vpc

    def given_a_vpc(self):
        self.given_a_vpc_with_offering(self.get_vpc_offering_name())

    def when_I_create_a_network_with_that_offering(self, gateway='10.1.1.1'):
        return self.verify_network_creation(
            offering=self.offering,
            gateway=gateway)

    def when_I_create_a_vpc_tier_with_that_offering(self, gateway='10.1.1.1'):
        return self.verify_network_creation(
            offering=self.offering,
            gateway=gateway,
            vpc=self.vpc,
            acl_list=self.vpc_acl_list)

    def when_I_restart_the_vpc_with(self, cleanup=True):
        self.restart_Vpc(self.vpc, cleanup=cleanup)
        self.validate_vpc(self.vpc, state="Enabled")

    def when_I_restart_the_network_with(self, network, cleanup):
        network.restart(self.api_client, cleanup=cleanup)
        self.validate_Network(network, state="Implemented")

    def when_I_deploy_a_vm(self, networks, acl_item=None,
                           vpc=None, keypair=None):

        test_data = self.test_data["virtual_machine_userdata"]

        vm = self.create_VM(
            networks,
            testdata=test_data,
            keypair=keypair)

        # Check VM
        self.check_VM_state(vm, state="Running")
        self.validate_vm_networking(vm)

        if keypair and vm.passwordenabled:
            self._decrypt_password(vm)

        vm.key_pair = self.keypair if keypair else None
        vm.user_data = test_data["userdata"]
        vm.password_test = self.PasswordTest(vm=vm) \
            if vm.passwordenabled \
            else self.PasswordTest(expect_pw=False)

        # Check networks
        network_list = networks \
            if hasattr(networks, "__iter__") \
            else [networks]

        for network in network_list:
            self.validate_Network(network, state="Implemented")
            if network.type == "Shared":
                self.validate_shared_networking(network, self.vpc)
            else:
                self.validate_network_networking(network, self.vpc)

        if self.vpc_acl_rule is not None:
            self.validate_acl_rule(self.vpc_acl_rule)

        return vm

    def when_I_deploy_a_vm_with_keypair_in(self, network):
        return self.when_I_deploy_a_vm(network,
                                       keypair=self.keypair.name)

    def when_I_create_a_static_nat_ip_to(self, vm, network, public_ip=None):
        """
        Creates and verifies (Ingress) firewall rule
        with a Static NAT rule enabled public IP
        :type vm: VirtualMachine
        :type network: Network
        :type public_ip: PublicIPAddress
        :rtype: PublicIPAddress
        """

        if not public_ip:
            public_ip = self.acquire_PublicIPAddress(network, vpc=self.vpc)

        self.debug("Creating and verifying firewall rule")
        self.create_StaticNatRule_For_VM(vm, public_ip, network)

        # Verification

        # self.validate_StaticNat_rule_For_VM(public_ip, network, vm)

        if not self.vpc:
            fw_rule = self.create_FirewallRule(public_ip,
                                               self.test_data["ingress_rule"])
            self.validate_acl_rule(fw_rule)
            self.debug("Successfully created and verified firewall rule")

        self.cleanup.append(public_ip)

        return public_ip

    def then_creating_a_network_with_that_offering_fails(self):
        create_network = self.verify_network_creation(
            offering=self.offering,
            gateway='10.6.6.6')
        self.assertFalse(create_network.success,
                         'Network found success = %s, expected success =%s'
                         % (str(create_network.success), 'False'))

    def then_creating_a_vpc_tier_with_that_offering_fails(self):
        create_network = self.verify_network_creation(
            offering=self.offering,
            gateway='10.6.6.6',
            vpc=self.vpc,
            acl_list=self.vpc_acl_list)
        self.assertFalse(create_network.success,
                         'Create Tier success = %s, expected success =%s'
                         % (str(create_network.success), 'False'))

    def then_the_network_has(self, network_result, state):
        self.validate_Network(network_result.network, state=state)

    def then_the_network_is_successfully_created(self, network):
        self.assertTrue(network.success,
                        'Network found success = %s, expected success = %s'
                        % (str(network.success), 'True'))

    def then_vr_is_as_expected(self, network):
        self.check_Router_state(network=network, state="Running")

    def then_config_drive_is_as_expected(self, vm,
                                         public_ip,
                                         metadata=False,
                                         reconnect=True):
        """Verify Config Drive Content

        :param vm: Virtual Machine
        :param public_ip: public IP
        :param metadata: whether to verify metadata
        :param reconnect: recreate SSH Connection
        :type vm: VirtualMachine
        :type public_ip: PublicIPAddress
        :type metadata: bool
        :type reconnect: bool
        """

        if self.isSimulator:
            self.debug("Simulator Environment: "
                       "Skipping Config Drive content verification")
            return

        self.debug("SSHing into the VM %s" % vm.name)

        ssh = self.ssh_into_VM(vm, public_ip, reconnect=reconnect)

        d = {x.name: x for x in ssh.logger.handlers}
        ssh.logger.handlers = list(d.values())

        mount_path = self._mount_config_drive(ssh)
        self.assertIsNotNone(mount_path,
                             'ConfigdriveIso is not attached to vm')
        if metadata:
            self.debug("Verifying metadata for vm: %s" % vm.name)
            self._verify_metadata(vm, ssh, mount_path)
            self.debug("Verifying openstackdata for vm: %s" % vm.name)
            self._verify_openstack_metadata(ssh, mount_path)

        if hasattr(vm, "user_data") and vm.user_data is not None:
            self.debug("Verifying userdata for vm: %s" % vm.name)
            self._verify_userdata(ssh, mount_path, vm.user_data)
            self._verify_openstack_userdata(ssh, mount_path, vm.user_data)

        if hasattr(vm, "password_test") \
                and vm.password_test is not None \
                and vm.password_test.test_presence:
            self.debug("Verifying password for vm: %s" % vm.name)
            self._verifyPassword(ssh, mount_path, vm.password_test)

        if hasattr(vm, "key_pair") and vm.key_pair is not None:
            self.debug("Verifying sshkey for vm: %s" % vm.name)
            self._verify_ssh_key(ssh, mount_path, vm.key_pair)

        self._umount_config_drive(ssh, mount_path)

    # =========================================================================

    def _get_test_data(self, key):
        return self.test_data[key]

    def get_vpc_offering_name(self):
        return "vpc_offering_configdrive"

    def get_network_offering_name(self):
        return "isolated_configdrive_network_offering"

    def get_network_offering_name_for_vpc(self):
        return "vpc_network_offering_configdrive"

    def get_configdrive_provider(self):
        return NetworkServiceProvider.list(
            self.api_client,
            name="ConfigDrive")[0]

    def verify_network_creation(self, offering=None,
                                offering_name=None,
                                gateway=None,
                                vpc=None, acl_list=None, testdata=None):
        """
        Creates a network

        :param offering: Network Offering
        :type offering: NetworkOffering
        :param offering_name: Offering name
        :type offering_name: Optional[str]
        :param gateway: gateway
        :type gateway: str
        :param vpc: in case of a VPC tier, the parent VPC
        :type vpc: VPC
        :param acl_list: in case of a VPC tier, the acl list
        :type acl_list: NetworkACLList
        :param testdata: Test data
        :type testdata: dict
        :return: Network Creation Result
        :rtype: ConfigDriveUtils.CreateResult
        """
        if offering is None:
            self.debug("Creating network offering...")
            offering = self.create_NetworkOffering(
                self._get_test_data(offering_name))
            self.validate_NetworkOffering(offering, state="Enabled")
        try:
            network = self.create_Network(offering,
                                          gateway=gateway,
                                          vpc=vpc,
                                          acl_list=acl_list,
                                          testdata=testdata)
            return self.CreateResult(True, offering=offering, network=network)
        except Exception as e:
            self.debug("Exception: %s" % e)
            return self.CreateResult(False, offering=offering)

    def verify_vpc_creation(self, offering=None, offering_name=None):
        """
        Creates a VPC

        :param offering: VPC Offering
        :type offering: VpcOffering
        :param offering_name: Offering name
        :type offering_name: Optional[str]
        :return: VPC Creation Result
        :rtype: CreateResult
        """
        if offering is None:
            self.debug("Creating VPC offering...")
            offering = self.create_VpcOffering(
                self._get_test_data(offering_name))
            self.validate_VpcOffering(offering, state="Enabled")
        try:
            vpc = self.create_vpc(offering, cidr='10.1.0.0/16')
            self.validate_vpc(vpc, state="Enabled")
            return self.CreateResult(True, offering=offering, vpc=vpc)
        except Exception as e:
            self.debug("Exception: %s" % e)
            return self.CreateResult(False, offering=offering)

    def _find_nic(self, vm, network):
        vm = VirtualMachine.list(self.api_client, id=vm.id)[0]
        return next(nic for nic in vm.nic if nic.networkid == network.id)

    def get_public_shared_ip(self, vm, network):
        nic = self._find_nic(vm, network)
        return PublicIPAddress({"ipaddress": nic})

    def plug_nic(self, vm, network):
        vm.add_nic(self.api_client, network.id)
        self.debug("Added NIC in VM with ID - %s and network with ID - %s"
                   % (vm.id, network.id))

    def unplug_nic(self, vm, network):
        nic = self._find_nic(vm, network)
        vm.remove_nic(self.api_client, nic.id)
        self.debug("Removed NIC with ID - %s in VM with ID - %s and "
                   "network with ID - %s" % (nic.id, vm.id, network.id))

    def update_default_nic(self, vm, network):
        nic = self._find_nic(vm, network)
        vm.update_default_nic(self.api_client, nic.id)
        self.debug("Removed NIC with ID - %s in VM with ID - %s and "
                   "network with ID - %s" % (nic.id, vm.id, network.id))

    def when_I_update_userdata(self, vm, new_user_data):
        """Updates the user data of a VM

        :param public_ip: Public ip of the VM
        :param vm: the Virtual Machine
        :param new_user_data: UserData to set
        :type public_ip: PublicIPAddress
        :type vm: VirtualMachine
        :type new_user_data: str
        :returns: User data in base64 format
        :rtype: str
        """
        self.debug("Updating userdata for VM - %s" % vm.name)
        updated_user_data = base64.encodestring(new_user_data)
        with self.stopped_vm(vm):
            vm.update(self.api_client, userdata=updated_user_data)

        vm.user_data = new_user_data
        if vm.state == VirtualMachine.RUNNING:
            vm.password_test = ConfigDriveUtils.PasswordTest(expect_pw=False)

    def update_and_validate_userdata(self, vm, new_user_data, public_ip=None,
                                     **kwargs):
        """Updates the user data of a VM

        :param public_ip: Public ip of the VM
        :param vm: the Virtual Machine
        :param new_user_data: UserData to set
        :type public_ip: PublicIPAddress
        :type vm: VirtualMachine
        :type new_user_data: str
        :returns: User data in base64 format
        :rtype: str
        """

        self.when_I_update_userdata(vm, new_user_data)
        self.then_config_drive_is_as_expected(vm, public_ip, **kwargs)

    def when_I_reset_the_password(self, vm):
        """Resets the password of a VM

        :param vm: the Virtual Machine
        :type vm: VirtualMachine
        :returns: The new password
        :rtype: str
        """

        self.debug("Resetting password for VM - %s" % vm.name)
        with self.stopped_vm(vm):
            vm.password = vm.resetPassword(self.api_client)

        self.debug("Password reset to - %s" % vm.password)
        self.debug("VM - %s password - %s !" %
                   (vm.name, vm.password))

        vm.password_test = ConfigDriveUtils.PasswordTest(vm=vm)

        return vm.password

    def stop_and_start_vm(self, vm):
        self.debug("+++ Verify userdata after stopstartVM - %s" % vm.name)
        with self.stopped_vm(vm):
            pass

        vm.password_test = ConfigDriveUtils.PasswordTest(expect_pw=False)

    def delete_and_recover_vm(self, vm):
        self.debug("+++ Verify userdata after VM recover- %s" % vm.name)
        vm.delete(self.api_client, expunge=False)

        self.debug("Recover VM - %s" % vm.name)
        vm.recover(self.api_client)
        vm.start(self.api_client)
        vm.state = VirtualMachine.RUNNING

        vm.password_test = ConfigDriveUtils.PasswordTest(expect_pw=False)

    def wait_until_done(self, thread_list, name):
        for aThread in thread_list:
            self.debug("[Concurrency]Join %s for vm %s" % (name,
                                                           aThread.get_vm()))
            aThread.join()

    def update_and_validate_sshkeypair(self, vm, public_ip=None):
        """

        :type vm: VirtualMachine
        """

        self.generate_ssh_keys()

        with self.stopped_vm(vm):
            vm_new_ssh = vm.resetSshKey(self.api_client,
                                        keypair=self.keypair.name,
                                        account=self.account.user[0].account,
                                        domainid=self.account.domainid)

        self.debug("Sshkey reset to - %s" % self.keypair.name)

        vm.details = vm_new_ssh.details

        # reset SSH key also resets the password.
        self._decrypt_password(vm)

        vm.password_test = ConfigDriveUtils.PasswordTest(vm=vm)
        vm.key_pair = self.keypair

        if public_ip:
            self.then_config_drive_is_as_expected(vm, public_ip, metadata=True)

    def _decrypt_password(self, vm):
        """Decrypt VM password

        the new password is available in VM detail,
        named "Encrypted.Password".
        It is encrypted using the SSH Public Key,
        and thus can be decrypted using the SSH Private Key

        :type vm: VirtualMachine
        """
        password_ = vm.details['Encrypted.Password']
        if password_ is not None:
            from base64 import b64decode
            try:
                from Crypto.PublicKey import RSA
                from Crypto.Cipher import PKCS1_v1_5
                with open(self.keypair.private_key_file, "r") as pkfile:
                    key = RSA.importKey(pkfile.read())
                    cipher = PKCS1_v1_5.new(key)
                new_password = cipher.decrypt(b64decode(password_), None)
                if new_password:
                    vm.password = new_password
                else:
                    self.fail("Failed to decrypt new password")
            except ImportError:
                # No pycrypto, fallback to openssl
                cmd = ["echo " + password_ +
                       " | base64 -d"
                       " | openssl rsautl -decrypt -inkey "
                       + self.keypair.private_key_file
                       + " 2> /dev/null"
                       ]

                new_password = subprocess.check_output(cmd, shell=True)
                self.debug("Decrypted password %s" % new_password)
                if new_password:
                    vm.password = new_password
                else:
                    self.fail("Failed to decrypt new password")

    def add_subnet_to_shared_network_and_verify(self, network, services):
        """verify required nic is present in the VM"""

        self.debug("Going to add new ip range in shared network %s" %
                   network.name)
        cmd = createVlanIpRange.createVlanIpRangeCmd()
        cmd.networkid = network.id
        cmd.gateway = services["gateway"]
        cmd.netmask = services["netmask"]
        cmd.startip = services["startip"]
        cmd.endip = services["endip"]
        cmd.forVirtualNetwork = services["forvirtualnetwork"]
        addedsubnet = self.api_client.createVlanIpRange(cmd)

        self.debug("verify above iprange is successfully added in shared "
                   "network %s or not" % network.name)

        cmd1 = listVlanIpRanges.listVlanIpRangesCmd()
        cmd1.networkid = network.id
        cmd1.id = addedsubnet.vlan.id

        allsubnets = self.api_client.listVlanIpRanges(cmd1)
        self.assertEqual(
            allsubnets[0].id,
            addedsubnet.vlan.id,
            "Check New subnet is successfully added to the shared Network"
        )
        return addedsubnet

    def ssh_into_VM(self, vm, public_ip, reconnect=True, keypair=None):
        pass

    def delete(self, obj, **kwargs):
        if isinstance(obj, VirtualMachine) and "expunge" not in kwargs:
            kwargs["expunge"] = True

        obj.delete(self.api_client, **kwargs)

        if obj in self.cleanup:
            self.cleanup.remove(obj)


class TestConfigDrive(cloudstackTestCase, ConfigDriveUtils):
    """Test user data and password reset functionality
    using configDrive
    """

    def __init__(self, methodName='runTest'):
        super(cloudstackTestCase, self).__init__(methodName)
        ConfigDriveUtils.__init__(self)

    @classmethod
    def setUpClass(cls):
        # We want to fail quicker, if it's a failure
        socket.setdefaulttimeout(60)

        test_client = super(TestConfigDrive, cls).getClsTestClient()
        cls.api_client = test_client.getApiClient()
        cls.db_client = test_client.getDbConnection()
        cls.test_data = test_client.getParsedTestDataConfig()
        cls.test_data.update(Services().services)

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client)
        cls.domain = get_domain(cls.api_client)

        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_test_template(cls.api_client,
                                         cls.zone.id,
                                         cls.hypervisor,
                                         cls.test_data["test_templates"])
        cls.test_data["virtual_machine"]["zoneid"] = cls.zone.id
        cls.test_data["virtual_machine"]["template"] = cls.template.id

        # Create service offering
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.test_data["service_offering"])
        cls._cleanup = [cls.service_offering]

        hypervisors = Hypervisor.list(cls.api_client, zoneid=cls.zone.id)
        cls.isSimulator = any(h.name == "Simulator" for h in hypervisors)
        return

    def setUp(self):
        # Create an account
        self.account = Account.create(self.api_client,
                                      self.test_data["account"],
                                      admin=True,
                                      domainid=self.domain.id
                                      )
        self.tmp_files = []
        self.cleanup = [self.account]
        self.generate_ssh_keys()
        return

    @classmethod
    def tearDownClass(cls):
        # Cleanup resources used
        cls.debug("Cleaning up the resources")
        for obj in reversed(cls._cleanup):
            try:
                if isinstance(obj, VirtualMachine):
                    obj.delete(cls.api_client, expunge=True)
                else:
                    obj.delete(cls.api_client)
            except Exception as e:
                cls.error("Failed to cleanup %s, got %s" % (obj, e))
        # cleanup_resources(cls.api_client, cls._cleanup)
        cls._cleanup = []
        cls.debug("Cleanup complete!")
        return

    def tearDown(self):
        # Cleanup resources used
        self.debug("Cleaning up the resources")
        for obj in reversed(self.cleanup):
            try:
                if isinstance(obj, VirtualMachine):
                    obj.delete(self.api_client, expunge=True)
                else:
                    obj.delete(self.api_client)
            except Exception as e:
                self.error("Failed to cleanup %s, got %s" % (obj, e))
        # cleanup_resources(self.api_client, self.cleanup)
        self.cleanup = []
        for tmp_file in self.tmp_files:
            os.remove(tmp_file)
        self.debug("Cleanup complete!")
        return

    # create_StaticNatRule_For_VM - Creates Static NAT rule on the given
    # public IP for the given VM in the given network
    def create_StaticNatRule_For_VM(self, vm, public_ip, network,
                                    vmguestip=None):
        self.debug("Enabling Static NAT rule on public IP - %s for VM with ID "
                   "- %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))
        static_nat_rule = StaticNATRule.enable(
            self.api_client,
            ipaddressid=public_ip.ipaddress.id,
            virtualmachineid=vm.id,
            networkid=network.id,
            vmguestip=vmguestip
        )
        self.debug("Static NAT rule enabled on public IP - %s for VM with ID "
                   "- %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))
        return static_nat_rule

    def create_FirewallRule(self, public_ip, rule=None):
        """Creates an Ingress Firewall Rule on the given public IP
           to allow traffic to a VM in an isolated network
        :param public_ip: Static NAT rule enabled public IP
        :param rule: (optional) Rule to add, defaults to test_data
        :type public_ip: PublicIPAddress
        """
        if not rule:
            rule = self.test_data["ingress_rule"]
        self.debug("Adding an (Ingress) Firewall rule to make Guest VMs "
                   "accessible through Static NAT rule - %s" % rule)
        return FireWallRule.create(self.api_client,
                                   ipaddressid=public_ip.ipaddress.id,
                                   protocol=rule["protocol"],
                                   cidrlist=rule["cidrlist"],
                                   startport=rule["startport"],
                                   endport=rule["endport"]
                                   )

    # validate_NetworkServiceProvider - Validates the given Network Service
    # Provider in the Physical Network, matches the given provider
    # name and state against the list of providers fetched
    def validate_NetworkServiceProvider(self, provider_name, state=None):
        """Validates the Network Service Provider in the Physical
        Network"""
        self.debug("Validating the creation and state of Network Service "
                   "Provider - %s" % provider_name)
        providers = NetworkServiceProvider.list(
            self.api_client,
            name=provider_name
        )
        self.assertEqual(isinstance(providers, list), True,
                         "List Network Service Provider should return a "
                         "valid list"
                         )
        self.assertEqual(provider_name, providers[0].name,
                         "Name of the Network Service Provider should match "
                         "with the returned list data"
                         )
        if state:
            self.assertEqual(providers[0].state, state,
                             "Network Service Provider state should be '%s'" %
                             state
                             )
        self.debug("Successfully validated the creation and state of Network "
                   "Service Provider - %s" % provider_name)

    # validate_PublicIPAddress - Validates if the given public IP address is in
    # the expected state form the list of fetched public IP addresses
    def validate_PublicIPAddress(self, public_ip, network, static_nat=False,
                                 vm=None):
        """Validates the Public IP Address"""
        self.debug("Validating the assignment and state of public IP address "
                   "- %s" % public_ip.ipaddress.ipaddress)
        public_ips = PublicIPAddress.list(self.api_client,
                                          id=public_ip.ipaddress.id,
                                          networkid=network.id,
                                          isstaticnat=static_nat,
                                          listall=True
                                          )
        self.debug("PUBLIC IP = " + public_ips[0])
        self.assertEqual(isinstance(public_ips, list), True,
                         "List public IP for network should return a "
                         "valid list"
                         )
        self.assertEqual(public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public IP for network should list the assigned "
                         "public IP address"
                         )
        self.assertEqual(public_ips[0].state, "Allocated",
                         "Assigned public IP is not in the allocated state"
                         )
        if static_nat and vm:
            self.assertEqual(public_ips[0].virtualmachineid, vm.id,
                             "Static NAT rule is not enabled for the VM on "
                             "the assigned public IP"
                             )
        self.debug("Successfully validated the assignment and state of public "
                   "IP address - %s" % public_ip.ipaddress.ipaddress)

    # create_NetworkOffering - Creates Network offering
    def create_NetworkOffering(self, net_offering, suffix=None,
                               conserve_mode=False):
        self.debug("Creating Network offering")
        if suffix:
            net_offering["name"] = "NET_OFF-" + str(suffix)
        nw_off = NetworkOffering.create(self.api_client,
                                        net_offering,
                                        conservemode=conserve_mode
                                        )
        # Enable Network offering
        nw_off.update(self.api_client, state="Enabled")
        self.debug("Created and Enabled Network offering")
        self.cleanup.append(nw_off)
        return nw_off

    # validate_NetworkOffering - Validates the given Network offering, matches
    # the given network offering name and state against the list of network
    # offerings fetched
    def validate_NetworkOffering(self, net_offering, state=None):
        """Validates the Network offering.
        :param net_offering: Network Offering to validate
        :param state: expected state
        :type net_offering: NetworkOffering
        :type state: str
        """
        self.debug("Validating the creation and state of Network offering - %s"
                   % net_offering.name)
        net_offs = NetworkOffering.list(self.api_client,
                                        id=net_offering.id
                                        )
        self.assertEqual(isinstance(net_offs, list), True,
                         "List Network offering should return a valid list"
                         )
        self.assertEqual(net_offering.name, net_offs[0].name,
                         "Name of the Network offering should match with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(net_offs[0].state, state,
                             "Network offering state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of Network "
                   "offering - %s" % net_offering.name)

    # create_Network - Creates network with the given Network offering
    def create_Network(self, nw_off, gateway="10.1.1.1",
                       netmask="255.255.255.0", vpc=None, acl_list=None,
                       testdata=None, account=None):
        if not account:
            account = self.account
        self.debug("Creating a network in the account - %s" % account.name)
        if not testdata:
            testdata = self.test_data["network"]
            testdata["name"] = "TestNet-" + gateway + "-" + str(nw_off.name)
            testdata["displaytext"] = "Test Network"
            testdata["gateway"] = gateway
            testdata["netmask"] = netmask
        network = Network.create(self.api_client,
                                 testdata,
                                 accountid=account.name,
                                 domainid=account.domainid,
                                 networkofferingid=nw_off.id,
                                 zoneid=self.zone.id,
                                 vpcid=vpc.id if vpc
                                 else self.vpc.id if self.vpc
                                 else None,
                                 aclid=acl_list.id if acl_list else None
                                 )
        self.debug("Created network with ID - %s" % network.id)
        self.cleanup.append(network)
        return network

    # create_VpcOffering - Creates VPC offering
    def create_VpcOffering(self, vpc_offering, suffix=None):
        self.debug("Creating VPC offering")
        if suffix:
            vpc_offering["name"] = "VPC_OFF-" + str(suffix)
        vpc_off = VpcOffering.create(self.api_client,
                                     vpc_offering
                                     )
        # Enable VPC offering
        vpc_off.update(self.api_client, state="Enabled")
        self.debug("Created and Enabled VPC offering")
        self.cleanup.append(vpc_off)
        return vpc_off

    # create_Vpc - Creates VPC with the given VPC offering
    def create_vpc(self, vpc_offering, cidr='10.1.0.0/16', testdata=None,
                   account=None, networkDomain=None):
        if not account:
            account = self.account
        self.debug("Creating a VPC in the account - %s" % account.name)
        if not testdata:
            testdata = self.test_data["vpc"]
            testdata["name"] = "TestVPC-" + cidr + "-" + str(vpc_offering.name)
            testdata["displaytext"] = "Test VPC"
            testdata["cidr"] = cidr
        vpc = VPC.create(self.api_client,
                         testdata,
                         vpcofferingid=vpc_offering.id,
                         zoneid=self.zone.id,
                         account=account.name,
                         domainid=account.domainid,
                         networkDomain=networkDomain
                         )
        self.debug("Created VPC with ID - %s" % vpc.id)
        self.cleanup.append(vpc)
        return vpc

    # validate_VpcOffering - Validates the given VPC offering, matches the
    # given VPC offering name and state against the list of VPC offerings
    # fetched
    def validate_VpcOffering(self, vpc_offering, state=None):
        """Validates the VPC offering"""
        self.debug("Validating the creation and state of VPC offering - %s" %
                   vpc_offering.name)
        vpc_offs = VpcOffering.list(self.api_client,
                                    id=vpc_offering.id
                                    )
        self.assertEqual(isinstance(vpc_offs, list), True,
                         "List VPC offering should return a valid list"
                         )
        self.assertEqual(vpc_offering.name, vpc_offs[0].name,
                         "Name of the VPC offering should match with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(vpc_offs[0].state, state,
                             "VPC offering state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of VPC "
                   "offering - %s" % vpc_offering.name)

    # validate_Vpc - Validates the given VPC, matches the given VPC name and
    # state against the list of VPCs fetched
    def validate_vpc(self, vpc, state=None):
        """Validates the VPC"""
        self.debug("Validating the creation and state of VPC - %s" % vpc.name)
        vpcs = VPC.list(self.api_client,
                        id=vpc.id
                        )
        self.assertEqual(isinstance(vpcs, list), True,
                         "List VPC should return a valid list"
                         )
        self.assertEqual(vpc.name, vpcs[0].name,
                         "Name of the VPC should match with the returned "
                         "list data"
                         )
        if state:
            self.assertEqual(vpcs[0].state, state,
                             "VPC state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of VPC - %s"
                   % vpc.name)

    # ssh_into_VM - Gets into the shell of the given VM using its public IP
    def ssh_into_VM(self, vm, public_ip, reconnect=True,
                    negative_test=False, keypair=None):
        self.debug("SSH into VM with ID - %s on public IP address - %s" %
                   (vm.id, public_ip.ipaddress.ipaddress))
        tries = 1 if negative_test else 3

        @retry(tries=tries)
        def retry_ssh():
            ssh_client = vm.get_ssh_client(
                ipaddress=public_ip.ipaddress.ipaddress,
                reconnect=reconnect,
                retries=3 if negative_test else 30
            )
            self.debug("Successful to SSH into VM with ID - %s on "
                       "public IP address - %s" %
                       (vm.id, public_ip.ipaddress.ipaddress))
            return ssh_client

        return retry_ssh()

    # create_VM - Creates VM in the givsen network(s)
    def create_VM(self, network_list, host_id=None, start_vm=True,
                  testdata=None, account=None, keypair=None):
        network_ids = []
        if isinstance(network_list, list):
            for network in network_list:
                network_ids.append(str(network.id))
        else:
            network_ids.append(str(network_list.id))
        if not account:
            account = self.account
        self.debug("Creating VM in network(s) with ID(s) - %s in the "
                   "account - %s" % (network_ids, account.name))
        if not testdata:
            testdata = self.test_data["virtual_machine"]
        vm = VirtualMachine.create(self.api_client,
                                   testdata,
                                   accountid=account.name,
                                   domainid=account.domainid,
                                   serviceofferingid=self.service_offering.id,
                                   templateid=self.template.id,
                                   zoneid=self.zone.id,
                                   networkids=network_ids,
                                   startvm=start_vm,
                                   hostid=host_id,
                                   keypair=keypair
                                   )
        self.debug("Created VM with ID - %s in network(s) with ID(s) - %s"
                   % (vm.id, network_ids))
        self.cleanup.append(vm)
        return vm

    def check_VM_state(self, vm, state=None):
        """Validates the VM state
        Checks if the given VM is in the expected state
        from the list of fetched VMs
        """
        self.debug("Validating the deployment and state of VM - %s" % vm.name)
        vms = VirtualMachine.list(self.api_client,
                                  id=vm.id,
                                  listall=True
                                  )
        self.assertEqual(isinstance(vms, list), True,
                         "List virtual machine should return a valid list"
                         )
        if state:
            self.assertEqual(vms[0].state, state,
                             "Virtual machine is not in the expected state"
                             )
        self.debug("Successfully validated the deployment and state of VM - %s"
                   % vm.name)

    def validate_Network(self, network, state=None):
        """Validates the network
           matches the given network name and state
           against the list of networks fetched
        """
        self.debug("Validating the creation and state of Network - %s" %
                   network.name)
        networks = Network.list(self.api_client,
                                id=network.id
                                )
        self.assertEqual(isinstance(networks, list), True,
                         "List network should return a valid list"
                         )
        self.assertEqual(network.name, networks[0].name,
                         "Name of the network should match with with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(networks[0].state, state,
                             "Network state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of Network "
                   "- %s" % network.name)

    def get_Router(self, network):
        """Returns router for the given network"""
        self.debug("Finding the virtual router for network with ID - %s" %
                   network.id)
        routers = Router.list(self.api_client,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(isinstance(routers, list), True,
                         "List routers should return a valid virtual router "
                         "for network"
                         )
        return routers[0]

    # check_Router_state - Checks if the given router is in the expected state
    # form the list of fetched routers
    def check_Router_state(self, router=None, network=None, state=None):
        """Validates the Router state"""

        if router:
            self.debug("Validating the deployment and state of Router - %s" %
                       router.name)
            routers = Router.list(self.api_client, id=router.id,
                                  listall=True)
        elif network:
            self.debug("Validating the deployment and state of Router "
                       "in network - %s" % network.name)
            routers = Router.list(self.api_client, networkid=network.id,
                                  listall=True)
        else:
            raise AttributeError("Either router or network "
                                 "has to be specified")

        self.assertTrue(isinstance(routers, list),
                        "List router should return a valid list")
        self.assertTrue(len(routers) > 0,
                        "List routers should not return an empty list")

        if state:
            self.assertEqual(routers[0].state, state,
                             "Virtual router is not in the expected state"
                             )
        self.debug("Successfully validated the deployment and state of Router "
                   "- %s" % routers[0].name)

    def acquire_PublicIPAddress(self, network, vpc=None, account=None):
        """Acquires public IP address for the given network/VPC"""
        if not account:
            account = self.account
        self.debug("Associating public IP for network with ID - %s in the "
                   "account - %s" % (network.id, account.name))
        public_ip = PublicIPAddress.create(self.api_client,
                                           accountid=account.name,
                                           domainid=account.domainid,
                                           zoneid=self.zone.id,
                                           networkid=network.id if vpc is None
                                           else None,
                                           vpcid=vpc.id if vpc
                                           else self.vpc.id if self.vpc
                                           else None
                                           )
        self.debug("Associated public IP address - %s with network with ID - "
                   "%s" % (public_ip.ipaddress.ipaddress, network.id))
        self.cleanup.append(public_ip)
        return public_ip

    def migrate_VM(self, vm):
        """Migrates VM to another host, if available"""
        self.debug("+++ Migrating one of the VMs in the created "
                   "VPC Tier network to another host, if available...")
        self.debug("Checking if a host is available for migration...")
        hosts = Host.listForMigration(self.api_client, virtualmachineid=vm.id)
        if hosts:
            self.assertEqual(isinstance(hosts, list), True,
                             "List hosts should return a valid list"
                             )
            host = hosts[0]
            self.debug("Migrating VM with ID: "
                       "%s to Host: %s" % (vm.id, host.id))
            try:
                vm.migrate(self.api_client, hostid=host.id)
            except Exception as e:
                self.fail("Failed to migrate instance, %s" % e)
            self.debug("Migrated VM with ID: "
                       "%s to Host: %s" % (vm.id, host.id))
        else:
            self.debug("No host available for migration. "
                       "Test requires at-least 2 hosts")
        return host

    # create_NetworkAclList - Creates network ACL list in the given VPC
    def create_NetworkAclList(self, name, description, vpc):
        self.debug("Adding NetworkACL list in VPC with ID - %s" % vpc.id)
        return NetworkACLList.create(self.api_client,
                                     services={},
                                     name=name,
                                     description=description,
                                     vpcid=vpc.id
                                     )

    # create_NetworkAclRule - Creates Ingress/Egress Network ACL rule in the
    # given VPC network/acl list
    def create_NetworkAclRule(self, rule, traffic_type="Ingress", network=None,
                              acl_list=None):
        self.debug("Adding NetworkACL rule - %s" % rule)
        if acl_list:
            return NetworkACL.create(self.api_client,
                                     networkid=network.id if network else None,
                                     services=rule,
                                     traffictype=traffic_type,
                                     aclid=acl_list.id
                                     )
        else:
            return NetworkACL.create(self.api_client,
                                     networkid=network.id if network else None,
                                     services=rule,
                                     traffictype=traffic_type
                                     )

    # restart_Vpc - Restarts the given VPC with/without cleanup
    def restart_Vpc(self, vpc, cleanup=False):
        self.debug("Restarting VPC with ID - %s" % vpc.id)
        cmd = restartVPC.restartVPCCmd()
        cmd.id = vpc.id
        cmd.cleanup = cleanup
        cmd.makeredundant = False
        self.api_client.restartVPC(cmd)
        self.debug("Restarted VPC with ID - %s" % vpc.id)

    @attr(tags=["advanced", "isonw"], required_hardware="true")
    def test_configdrive_isolated_network(self):
        """Test Configdrive as provider for isolated Networks
           to provide userdata and password reset functionality
        """

        # 1. Given ConfigDrive provider is disabled in zone
        #    And a network offering which has
        #      user data provided by ConfigDrive
        #    Then creating an Isolated Network
        #    using that network offering fails

        # 2. Given ConfigDrive provider is enabled in zone
        #    And a network offering which has
        #    * user data provided by ConfigDrive
        #    When I create an Isolated Network using that network offering
        #    Then the network is successfully created,
        #    And is in the "Allocated" state.

        # 3. When I deploy a VM in the created Isolated network with user data,
        #    Then the Isolated network state is changed to "Implemented"
        #    And the VM is successfully deployed and is in the "Running" state
        #    And there is no VR is deployed.
        # 4. And the user data in the ConfigDrive device is as expected
        # 5. And the the vm password in the ConfigDrive device is as expected

        # 6. When I stop, reset the password, and start the VM
        # 7. Then I can login into the VM using the new password.
        # 8. And the the vm password in the ConfigDrive device is the new one

        # 9. Verify various scenarios and check the data in configdriveIso
        # 10. Delete all the created objects (cleanup).

        self.debug("+++ Scenario: creating an Isolated network with "
                   "config drive fails when config drive provider is "
                   "disabled.")
        self.given_config_drive_provider_is("Disabled")
        self.given_a_network_offering_with_configdrive()
        self.then_creating_a_network_with_that_offering_fails()

        self.debug("+++ Preparation Scenario: "
                   "creating an Isolated networks with "
                   "config drive when config drive provider is "
                   "enabled.")

        self.given_config_drive_provider_is("Enabled")

        create_network1 = self.when_I_create_a_network_with_that_offering()
        self.then_the_network_is_successfully_created(create_network1)
        self.then_the_network_has(create_network1, state="Allocated")

        create_network2 = self.when_I_create_a_network_with_that_offering()
        self.then_the_network_is_successfully_created(create_network2)
        self.then_the_network_has(create_network2, state="Allocated")

        network1 = create_network1.network
        network2 = create_network2.network

        self.given_template_password_enabled_is(True)

        self.debug("+++Deploy VM in the created Isolated network "
                   "with user data provider as configdrive")

        vm1 = self.when_I_deploy_a_vm_with_keypair_in(network1)
        public_ip_1 = \
            self.when_I_create_a_static_nat_ip_to(vm1, network1)

        self.then_vr_is_as_expected(network1)
        self.then_config_drive_is_as_expected(
            vm1, public_ip_1,
            metadata=True)

        self.update_and_validate_userdata(vm1, "helloworld vm1", public_ip_1)
        self.update_and_validate_sshkeypair(vm1, public_ip_1)

        # =====================================================================

        self.debug("Adding a non-default nic to the VM "
                   "making it a multi-nic VM...")
        self.plug_nic(vm1, network2)
        self.then_config_drive_is_as_expected(vm1, public_ip_1,
                                              metadata=True, reconnect=False)

        with self.stopped_vm(vm1):
            self.when_I_reset_the_password(vm1)
            self.when_I_update_userdata(vm1, "hellomultinicvm1")

        self.then_config_drive_is_as_expected(vm1, public_ip_1)

        # =====================================================================
        # Test using network2 as default network
        # =====================================================================

        self.debug("updating non-default nic as the default nic "
                   "of the multi-nic VM and enable staticnat...")
        self.update_default_nic(vm1, network2)

        public_ip_2 = \
            self.when_I_create_a_static_nat_ip_to(vm1, network2)
        self.stop_and_start_vm(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_2, metadata=True)

        self.when_I_reset_the_password(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_2)

        user_data = "hellomultinicvm1again"
        self.update_and_validate_userdata(vm1, user_data, public_ip_2)

        self.debug("Updating the default nic of the multi-nic VM, "
                   "deleting the non-default nic...")
        self.update_default_nic(vm1, network1)
        self.stop_and_start_vm(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)

        self.delete(public_ip_2)
        self.unplug_nic(vm1, network2)

        # =====================================================================
        # Another Multinic VM
        # =====================================================================
        self.debug("+++ Scenario: "
                   "Reset password and update userdata on a multi nic vm")
        multinicvm1 = self.when_I_deploy_a_vm([network2, network1])
        self.when_I_reset_the_password(multinicvm1)
        public_ip_3 = self.when_I_create_a_static_nat_ip_to(multinicvm1,
                                                            network2)
        self.then_config_drive_is_as_expected(
            multinicvm1, public_ip_3,
            metadata=True)

        user_data2 = "hello multinicvm1"
        self.update_and_validate_userdata(multinicvm1, user_data2, public_ip_3)

        self.delete(multinicvm1, expunge=True)
        self.delete(public_ip_3)
        self.delete(network2)

        # =====================================================================
        # Network restart tests
        # =====================================================================

        self.debug("+++ Scenario: "
                   "verify config drive after restart Isolated network without"
                   " cleanup...")
        self.when_I_reset_the_password(vm1)
        self.when_I_restart_the_network_with(network1, cleanup=False)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after restart Isolated network with"
                   " cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=True)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after reboot")
        vm1.reboot(self.api_client)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)
        self.update_and_validate_userdata(vm1, "hello afterboot", public_ip_1)
        self.when_I_reset_the_password(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after migrate")
        self.migrate_VM(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)
        self.debug("Updating userdata after migrating VM - %s" % vm1.name)
        self.update_and_validate_userdata(vm1, "hello after migrate",
                                          public_ip_1)
        self.when_I_reset_the_password(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after stop/start")
        self.stop_and_start_vm(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)
        self.update_and_validate_userdata(vm1, "hello afterstopstart",
                                          public_ip_1)
        self.when_I_reset_the_password(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after delete/recover")
        self.delete_and_recover_vm(vm1)
        self.then_config_drive_is_as_expected(vm1, public_ip_1,
                                              metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Start VM fails when ConfigDrive provider is disabled")
        self.given_config_drive_provider_is("Disabled")
        with self.assertRaises(Exception):
            self.when_I_update_userdata(vm1, "hi with provider state Disabled")
        self.given_config_drive_provider_is("Enabled")

        self.delete(vm1, expunge=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Update Userdata on a VM that is not password enabled")
        self.update_template(passwordenabled=False)
        vm1 = self.when_I_deploy_a_vm_with_keypair_in(network1)

        public_ip_1 = \
            self.when_I_create_a_static_nat_ip_to(vm1, network1)

        self.update_and_validate_userdata(vm1,
                                          "This is sample data",
                                          public_ip_1,
                                          metadata=True)

        self.delete(vm1, expunge=True)
        self.delete(network1)

    @attr(tags=["advanced", "vpc"], required_hardware="true")
    def test_configdrive_vpc_network(self):
        """Test Configdrive for VPC Networks
           choose user data with configDrive as service provider
           and test password reset functionality using ConfigDrive
        """

        # 1. Given ConfigDrive provider is disabled in zone
        #    And a network offering for VPC which has
        #      user data provided by ConfigDrive
        #    And a VPC
        #    Then creating an VPC Tier in the VPC
        #    using that network offering fails

        # 2. Given ConfigDrive provider is enabled in zone
        #    And a network offering for VPC which has
        #      user data provided by ConfigDrive
        #    And a VPC
        #    When I create an VPC Tier in the VPC  using that network offering
        #    Then the network is successfully created,
        #    And is in the "Allocated" state.

        # 3. When I deploy a VM in the created VPC tier with user data,
        #    Then the network state is changed to "Implemented"
        #    And the VM is successfully deployed and is in the "Running" state

        # 4. And the user data in the ConfigDrive device is as expected
        # 5. And the the vm password in the ConfigDrive device is as expected

        # 6. When I stop, reset the password, and start the VM
        # 7. Then I can login into the VM using the new password.
        # 8. And the the vm password in the ConfigDrive device is the new one

        # 9. Verify various scenarios and check the data in configdriveIso
        # 10. Delete all the created objects (cleanup).

        self.debug("+++ Scenario: creating an VPC tier with "
                   "config drive fails when config drive provider is "
                   "disabled.")
        self.given_a_vpc()
        self.given_config_drive_provider_is("Disabled")
        self.given_a_network_offering_for_vpc_with_configdrive()
        self.then_creating_a_vpc_tier_with_that_offering_fails()

        self.debug("+++ Preparation Scenario: "
                   "Create 2 tier with config drive "
                   "when config drive provider is enabled.")

        self.given_config_drive_provider_is("Enabled")

        create_network1 = self.when_I_create_a_vpc_tier_with_that_offering(
            gateway='10.1.1.1')
        self.then_the_network_is_successfully_created(create_network1)
        self.then_the_network_has(create_network1, state="Implemented")

        create_network2 = self.when_I_create_a_vpc_tier_with_that_offering(
            gateway='10.1.2.1')
        self.then_the_network_is_successfully_created(create_network2)
        self.then_the_network_has(create_network2, state="Implemented")

        network1 = create_network1.network
        network2 = create_network2.network

        self.given_template_password_enabled_is(True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Deploy VM in the Tier 1 with user data")
        vm = self.when_I_deploy_a_vm(network1,
                                     keypair=self.keypair.name)
        public_ip_1 = \
            self.when_I_create_a_static_nat_ip_to(vm, network1)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        self.update_and_validate_userdata(vm, "helloworld vm1",
                                          public_ip_1,
                                          metadata=True)

        self.when_I_reset_the_password(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1)

        self.update_and_validate_sshkeypair(vm, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Restarting the created vpc without cleanup...")
        self.restart_Vpc(self.vpc, cleanup=False)
        self.validate_vpc(self.vpc, state="Enabled")
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        # =====================================================================
        self.debug("Adding a non-default nic to the VM "
                   "making it a multi-nic VM...")
        self.plug_nic(vm, network2)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        with self.stopped_vm(vm):
            self.when_I_reset_the_password(vm)
            self.when_I_update_userdata(vm, "hellomultinicvm1")

        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        self.unplug_nic(vm, network2)
        self.delete(network2)

        # =====================================================================
        # Network restart tests
        # =====================================================================

        self.debug("+++ Scenario: "
                   "verify config drive after Restart VPC with cleanup...")
        self.when_I_restart_the_vpc_with(cleanup=True)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after Restart VPC without cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=False)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after restart tier with cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=True)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after reboot")
        vm.reboot(self.api_client)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)
        self.update_and_validate_userdata(vm, "hello reboot", public_ip_1)

        self.when_I_reset_the_password(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after migrate")
        self.migrate_VM(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)
        self.update_and_validate_userdata(vm, "hello migrate", public_ip_1)

        self.when_I_reset_the_password(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "update userdata and reset password after stop/start")
        self.stop_and_start_vm(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        self.update_and_validate_userdata(vm, "hello stop/start", public_ip_1)

        self.when_I_reset_the_password(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after delete/recover")
        self.delete_and_recover_vm(vm)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Verify configdrive when template is not password enabled")
        self.given_config_drive_provider_is("Disabled")
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)
        self.given_config_drive_provider_is("Enabled")

        self.delete(vm, expunge=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Update Userdata on a VM that is not password enabled")

        self.update_template(passwordenabled=False)

        vm = self.when_I_deploy_a_vm(network1,
                                     keypair=self.keypair.name)
        public_ip_1 = \
            self.when_I_create_a_static_nat_ip_to(vm, network1)
        self.update_and_validate_userdata(vm, "This is sample data",
                                          public_ip_1,
                                          metadata=True)

        self.delete(vm, expunge=True)
        self.delete(network1)

    @attr(tags=["advanced", "shared"], required_hardware="true")
    def test_configdrive_shared_network(self):
        """Test Configdrive as provider for shared Networks
           to provide userdata and password reset functionality
        """

        # 1. When ConfigDrive is disabled as provider in zone
        #    Verify Shared Network creation with a network offering
        #    which has userdata provided by ConfigDrive fails
        # 2. When ConfigDrive is enabled as provider in zone
        #    Create a shared Network with Isolated Network
        #    offering specifying ConfigDrive as serviceProvider
        #    for userdata.
        #    check if it is successfully created and
        #    is in the "Setup" state.
        # 3. Deploy a VM in the created Shared network with user data,
        #    check if the Shared network state is changed to
        #    "Implemented", and the VM is successfully deployed and
        #    is in the "Running" state.
        # 4. Verify that the guest VM's password in the iso.
        # 5. Reset VM password, and start the VM.
        # 6. Verify that the new guest VM template is password enabled by
        #     checking the VM's password (password != "password").
        # 7. Verify various scenarios and check the data in configdriveIso
        # 8. Delete all the created objects (cleanup).

        self.debug("+++Testing configdrive in an shared network fails..."
                   "as provider configdrive is still disabled...")
        self.given_config_drive_provider_is("Disabled")
        shared_test_data = self.test_data["acl"]["network_all_1"]
        shared_network = self.verify_network_creation(
            offering_name="shared_network_config_drive_offering",
            testdata=shared_test_data)
        self.assertFalse(shared_network.success,
                         'Network found success = %s, expected success =%s'
                         % (str(shared_network.success), 'False'))

        self.given_config_drive_provider_is("Enabled")
        shared_network = self.verify_network_creation(
            offering=shared_network.offering, testdata=shared_test_data)
        self.assertTrue(shared_network.success,
                        'Network found success = %s, expected success = %s'
                        % (str(shared_network.success), 'True'))

        self.validate_Network(shared_network.network, state="Setup")

        shared_test_data2 = self.test_data["acl"]["network_all_2"]
        shared_network2 = self.verify_network_creation(
            offering=shared_network.offering,
            testdata=shared_test_data2)
        self.assertTrue(shared_network2.success,
                        'Network found success = %s, expected success = %s'
                        % (str(shared_network2.success), 'True'))

        self.validate_Network(shared_network2.network, state="Setup")

        self.debug("+++Test user data & password reset functionality "
                   "using configdrive in an Isolated network")

        self.given_template_password_enabled_is(True)

        self.generate_ssh_keys()
        self.debug("keypair name %s " % self.keypair.name)

        self.debug("+++Deploy of a VM on a shared network with multiple "
                   "ip ranges, all should have the same value for the "
                   "underlay flag.")
        # Add subnet of different gateway
        self.debug("+++ Adding subnet of different gateway")

        self.add_subnet_to_shared_network_and_verify(shared_network.network,
                                                     self.test_data[
                                                         "publiciprange2"])
        self.test_data["virtual_machine"]["ipaddress"] = \
            self.test_data["acl"]["network_all_1"]["endip"]

        # with self.assertRaises(Exception):
        #         self.create_VM(
        #             [shared_network.network],
        #             testdata=self.test_data["virtual_machine_userdata"])

        self.debug("+++ In a shared network with multiple ip ranges, "
                   "userdata with config drive must be allowed.")

        vm1 = self.create_VM(
            [shared_network.network],
            testdata=self.test_data["virtual_machine_userdata"],
            keypair=self.keypair.name)
        # Check VM
        self.check_VM_state(vm1, state="Running")
        self.check_Router_state(network=shared_network.network,
                                state="Running")

        self.when_I_update_userdata(vm1, "helloworld vm1")

        self.debug("Adding a non-default nic to the VM "
                   "making it a multi-nic VM...")
        self.plug_nic(vm1, shared_network2.network)
        self.when_I_reset_the_password(vm1)

        self.debug("updating non-default nic as the default nic "
                   "of the multi-nic VM...")
        self.update_default_nic(vm1, shared_network2.network)
        self.when_I_reset_the_password(vm1)

        self.when_I_update_userdata(vm1, "hellomultinicvm1")

        self.debug("Updating the default nic of the multi-nic VM, "
                   "deleting the non-default nic...")
        self.update_default_nic(vm1, shared_network.network)
        vm1.stop(self.api_client)
        vm1.start(self.api_client)

        self.unplug_nic(vm1,
                        shared_network2.network)
        self.delete(shared_network2.network)

        self.debug("+++ When template is not password enabled, "
                   "verify configdrive of VM - %s" % vm1.name)
        self.delete(vm1, expunge=True)

        self.given_config_drive_provider_is("Enabled")
        self.update_template(passwordenabled=False)

        vm1 = self.create_VM(
            [shared_network.network],
            testdata=self.test_data["virtual_machine_userdata"],
            keypair=self.keypair.name)
        self.delete(vm1, expunge=True)
        self.delete(shared_network.network)

    @attr(tags=["advanced", "isonw"], required_hardware="true")
    def test_configdrive_isolated_network_hypervisor_hostname_exposed(self):
        """Test Configdrive as provider for isolated Networks
           to provide userdata and password reset functionality
        """

        # 1. Given a ConfigDrive provider and a network offering
        #    which has userdata provided ConfigDrive, create
        #    an Isolated network using that network offering.
        #    Verify network is successfully created and in the Allocated state
        #    Set the "global.allow.expose.host.hostname" and "account.allow.expose.host.hostname" flags to true
        #    to enable viewing hypevisor host name in the metadata file
        #    Deploy VM in the network created, verify metadata in the configdrive
        #    my mounting the configdrive ISO and verify the respective files
        #
        # 2. Create another Isolated network and plug NIC of the VM to this network
        #    make it the default NIC, verify the metadata file contents.
        #
        # 3. Remove the default NIC, reboot the VM and verify the metadata file contents
        #
        # 4. Restart network without cleanup and verify the metadata file contents
        #
        # 5. Restart the network with cleanup and verify the metadata file contents
        # 6. Migrate the VM to another host and verify the metadata file contents
        # 10. Delete all the created objects (cleanup).

        self.debug("+++ Preparation Scenario: "
                   "creating an Isolated networks with "
                   "config drive when config drive provider is "
                   "enabled.")

        self.given_config_drive_provider_is("Enabled")
        self.given_a_network_offering_with_configdrive()

        create_network1 = self.when_I_create_a_network_with_that_offering()
        self.then_the_network_is_successfully_created(create_network1)
        self.then_the_network_has(create_network1, state="Allocated")

        network1 = create_network1.network

        # Update global setting for "allow.expose.host.hostname"
        Configurations.update(self.api_client,
                              name="global.allow.expose.host.hostname",
                              value="true"
                              )

        # Update Account level setting
        Configurations.update(self.api_client,
                              name="account.allow.expose.host.hostname",
                              value="true"
                              )

        # Verify that the above mentioned settings are set to true before proceeding
        if not is_config_suitable(
                apiclient=self.api_client,
                name='global.allow.expose.host.hostname',
                value='true'):
            self.skipTest('global.allow.expose.host.hostname should be true. skipping')

        if not is_config_suitable(
                apiclient=self.api_client,
                name='account.allow.expose.host.hostname',
                value='true'):
            self.skipTest('Account level setting account.allow.expose.host.hostname should be true. skipping')

        self.debug("+++Deploy VM in the created Isolated network "
                   "with user data provider as configdrive")

        vm1 = self.when_I_deploy_a_vm(network1)

        public_ip_1 = self.when_I_create_a_static_nat_ip_to(vm1, network1)

        self.then_vr_is_as_expected(network1)
        self.then_config_drive_is_as_expected(
            vm1, public_ip_1,
            metadata=True)


        # =====================================================================
        # Network restart tests
        # =====================================================================

        self.debug("+++ Scenario: "
                   "verify config drive after restart Isolated network without"
                   " cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=False)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after restart Isolated network with"
                   " cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=True)
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify vm metadata after migrate")
        host = self.migrate_VM(vm1)
        vm1.hostname = host.name
        self.then_config_drive_is_as_expected(vm1, public_ip_1, metadata=True)

        # Reset configuration values to default values i.e., false
        Configurations.update(self.api_client,
                              name="global.allow.expose.host.hostname",
                              value="false"
                              )

        # Update Account level setting
        Configurations.update(self.api_client,
                              name="account.allow.expose.host.hostname",
                              value="false"
                              )

        self.delete(vm1, expunge=True)
        self.delete(network1)

    @attr(tags=["advanced", "vpc"], required_hardware="true")
    def test_configdrive_vpc_network_verify_metadata(self):
        """Test Configdrive for VPC Networks
           choose user data with configDrive as service provider
           and test vmdata functionality using ConfigDrive
        """

        # 1. Given ConfigDrive provider is enabled in zone
        #    And a network offering for VPC which has
        #      user data provided by ConfigDrive
        #    And a VPC
        #    When I create an VPC Tier in the VPC  using that network offering
        #    Then the network is successfully created,
        #    And is in the "Allocated" state.

        # 2. When I deploy a VM in the created VPC tier with user data,
        #    Then the network state is changed to "Implemented"
        #    And the VM is successfully deployed and is in the "Running" state

        # 3. And the user data in the ConfigDrive device is as expected
        # 4. Verify various scenarios and check the data in configdriveIso
        # 5. Delete all the created objects (cleanup).

        self.debug("+++ Preparation Scenario: "
                   "Create a tier with config drive "
                   "when config drive provider is enabled.")

        self.given_a_vpc()
        self.given_config_drive_provider_is("Enabled")
        self.given_a_network_offering_for_vpc_with_configdrive()
        create_network1 = self.when_I_create_a_vpc_tier_with_that_offering(
            gateway='10.1.1.1')
        self.then_the_network_is_successfully_created(create_network1)
        self.then_the_network_has(create_network1, state="Implemented")

        network1 = create_network1.network

        # Update global setting for "allow.expose.host.hostname"
        Configurations.update(self.api_client,
                              name="global.allow.expose.host.hostname",
                              value="true"
                              )

        # Update Account level setting
        Configurations.update(self.api_client,
                              name="account.allow.expose.host.hostname",
                              value="true"
                              )

        # Verify that the above mentioned settings are set to true before proceeding
        if not is_config_suitable(
                apiclient=self.api_client,
                name='global.allow.expose.host.hostname',
                value='true'):
            self.skipTest('global.allow.expose.host.hostname should be true. skipping')

        if not is_config_suitable(
                apiclient=self.api_client,
                name='account.allow.expose.host.hostname',
                value='true'):
            self.skipTest('Account level setting account.allow.expose.host.hostname should be true. skipping')

        # =====================================================================
        self.debug("+++ Scenario: "
                   "Deploy VM in the Tier 1 with user data")
        vm = self.when_I_deploy_a_vm(network1)
        public_ip_1 = self.when_I_create_a_static_nat_ip_to(vm, network1)

        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        # =====================================================================
        # Network restart tests
        # =====================================================================

        self.debug("+++ Scenario: "
                   "verify config drive after Restart VPC with cleanup...")
        self.when_I_restart_the_vpc_with(cleanup=True)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after Restart VPC without cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=False)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "verify config drive after restart tier with cleanup...")
        self.when_I_restart_the_network_with(network1, cleanup=True)
        self.then_config_drive_is_as_expected(vm, public_ip_1,
                                              metadata=True, reconnect=False)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "validate vm metadata after reboot")
        vm.reboot(self.api_client)
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        # =====================================================================
        self.debug("+++ Scenario: "
                   "validate updated userdata after migrate")
        host = self.migrate_VM(vm)
        vm.hostname = host.name
        self.then_config_drive_is_as_expected(vm, public_ip_1, metadata=True)

        # Reset configuration values to default values i.e., false
        Configurations.update(self.api_client,
                              name="global.allow.expose.host.hostname",
                              value="false"
                              )

        # Update Account level setting
        Configurations.update(self.api_client,
                              name="account.allow.expose.host.hostname",
                              value="false"
                              )

        self.delete(vm, expunge=True)
        self.delete(network1)
