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
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (resetSSHKeyForVirtualMachine,
                                  updateTemplate,
                                  restartVPC)
from marvin.lib.base import (Account,
                             createVlanIpRange,
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
                             Hypervisor)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone,
                               list_templates)
from marvin.lib.utils import random_gen
# Import System Modules
from nose.plugins.attrib import attr
from retry import retry
import tempfile
import socket
import base64
import sys
import time
import os

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
            "vpc_offering_configdrive": {
                "name": 'VPC offering ConfigDrive',
                "displaytext": 'VPC offering ConfigDrive',
                "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,UserData,Dns',
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
                "supportedservices": 'Dhcp,StaticNat,SourceNat,NetworkACL,UserData,Dns',
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
                "supportedservices": 'Dhcp,SourceNat,StaticNat,UserData,Firewall,Dns',
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
                    "vlan": "3998",
                    "gateway": "10.200.100.1",
                    "netmask": "255.255.255.0",
                    "startip": "10.200.100.21",
                    "endip": "10.200.100.100",
                    "acltype": "Domain"
                },
                "network_all_2": {
                    "name": "SharedNetwork2-All-2",
                    "displaytext": "SharedNetwork2-All-2",
                    "vlan": "3999",
                    "gateway": "10.200.200.1",
                    "netmask": "255.255.255.0",
                    "startip": "10.200.200.21",
                    "endip": "10.200.200.100",
                    "acltype": "Domain"
                }
            }
        }


class ConfigDriveUtils:
    class CreateResult:
        def __init__(self, success, offering=None, network=None, vpc=None):
            self.success = success
            self.network = network
            self.offering = offering
            self.vpc = vpc

    class PasswordTest:
        def __init__(self, password):
            self.test_presence = False
            self.presence = None
            self.password = None
            if type(password) is bool:
                self.test_presence = True
                self.presence = password
                self.password = None
            elif type(password) is unicode or type(password) is str:
                self.test_presence = True
                self.password = password
                self.presence = True

    def updateTemplate(self, value):
        """Updates value of the guest VM template's password enabled setting
        """
        self.debug("Updating value of guest VM template's password enabled "
                   "setting")
        cmd = updateTemplate.updateTemplateCmd()
        cmd.id = self.template.id
        cmd.passwordenabled = value
        self.api_client.updateTemplate(cmd)
        list_template_response = list_templates(self.api_client,
                                                templatefilter="all",
                                                id=self.template.id
                                                )
        self.template = list_template_response[0]
        self.debug("Updated guest VM template")

    def get_userdata_url(self, vm):
        """Returns user data URL for the given VM object"""
        self.debug("Getting user data url")
        nic = vm.nic[0]
        gateway = str(nic.gateway)
        self.debug("Gateway: " + gateway)
        user_data_url = 'curl "http://' + gateway + ':80/latest/user-data"'
        return user_data_url

    def validate_firewall_rule(self, fw_rule):
        pass

    def validate_StaticNat_rule_For_VM(self, public_ip, network, vm):
        self.validate_PublicIPAddress(
            public_ip, network, static_nat=True, vm=vm)

    def create_and_verify_fip_and_fw(self, vm, public_ip, network):
        """
        Creates and verifies (Ingress) firewall rule
        with a Static NAT rule enabled public IP"""

        self.debug("Creating and verifying firewall rule")
        self.create_StaticNatRule_For_VM(vm, public_ip, network)

        # Verification
        self.validate_StaticNat_rule_For_VM(public_ip, network, vm)

        fw_rule = self.create_FirewallRule(public_ip, self.test_data["ingress_rule"])
        self.validate_firewall_rule(fw_rule)
        self.debug("Successfully created and verified firewall rule")

    def mount_config_drive(self, ssh):
        """
        This method is to verify whether configdrive iso
        is attached to vm or not
        Returns mount path if config drive is attached else False
        """
        mountdir = "/root/iso"
        cmd = "blkid -t LABEL='config-2' /dev/sr? /dev/hd? /dev/sd? /dev/xvd? -o device"
        tmp_cmd = [
            'bash -c "if [ ! -d /root/iso ] ; then mkdir /root/iso ; fi"',
            "umount /root/iso"]
        for tcmd in tmp_cmd:
            ssh.execute(tcmd)
        configDrive = ssh.execute(cmd)
        res = ssh.execute("mount {} {}".format(str(configDrive[0]), mountdir))
        if str(res).lower().find("mounting read-only") > -1:
            self.debug("configDrive iso is mounted at location %s" % mountdir)
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

    def verify_config_drive_data(self, ssh, file, expected_content, name):
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

    def verifyUserData(self, ssh, iso_path, userdata):
        """
        verify Userdata

        :param ssh: SSH connection to the VM
        :param iso_path: mount point of the config drive
        :param userdata: Expected userdata
        :type ssh: marvin.sshClient.SshClient
        :type iso_path: str
        :type userdata: str
        """
        self.verify_config_drive_data(
            ssh,
            iso_path + "/cloudstack/userdata/user_data.txt",
            userdata,
            "userdata (ACS)"
        )

    def verifyOpenStackUserData(self, ssh, iso_path, userdata):
        """
        verify Userdata in Openstack format

        :param ssh: SSH connection to the VM
        :param iso_path: mount point of the config drive
        :param userdata: Expected userdata
        :type ssh: marvin.sshClient.SshClient
        :type iso_path: str
        :type userdata: str
        """
        self.verify_config_drive_data(
            ssh,
            iso_path + "/openstack/latest/user_data",
            userdata,
            "userdata (Openstack)"
        )

    def verifyPassword(self, ssh, iso_path, password):
        self.debug("Expected VM password is %s " % password.password)
        password_file = iso_path + "/cloudstack/password/vm_password.txt"
        vmpassword = self._get_config_drive_data(ssh, password_file,
                                                 "ConfigDrive password",
                                                 fail_on_missing=False)

        self.debug("ConfigDrive password is %s " % vmpassword)

        if NO_SUCH_FILE in vmpassword:
            self.debug("Password file is not found")
            return False, False
        elif (password.password is not None) \
                and (password.password in vmpassword):
            self.debug("Expected Password is found in configDriveIso")
            return True, True
        else:
            self.debug("Expected password is not found in configDriveIso")
            return True, False

    def verifySshKey(self, ssh, iso_path, ssh_key):
        self.debug("Expected VM sshkey is %s " % ssh_key.name)
        publicKey_file = iso_path + "/cloudstack/metadata/public-keys.txt"
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

    def verifyMetaData(self, vm, ssh, iso_path):
        """
        verify metadata files in CloudStack format

        :param vm: the VM
        :param ssh: SSH connection to the VM
        :param iso_path: mount point of the config drive
        :type vm: VirtualMachine
        :type ssh: marvin.sshClient.SshClient
        :type iso_path: str
        """

        metadata_dir = iso_path + "/cloudstack/metadata/"
        vm_files = ["availability-zone.txt",
                    "service-offering.txt",
                    "instance-id.txt",
                    "vm-id.txt",
                    "local-hostname.txt",
                    "local-ipv4.txt",
                    "public-ipv4.txt"]

        get_name = lambda file: \
            "{} metadata".format(file.split('.'[-1].replace('-', ' ')))

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
            vm.instancename,
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
        return

    def verifyOpenStackData(self, ssh, iso_path):
        """
        verify existence of metadata and user data files in OpenStack format

        :param ssh: SSH connection to the VM
        :param iso_path: mount point of the config drive
        :type ssh: marvin.sshClient.SshClient
        :type iso_path: str
        """
        openstackdata_dir = iso_path + "/openstack/latest/"
        openstackdata_files = ["meta_data.json",
                               "vendor_data.json",
                               "network_data.json"]
        for file in openstackdata_files:
            res = ssh.execute("cat %s" % openstackdata_dir + file)
            if NO_SUCH_FILE in res[0]:
                self.fail("{} file not found in vm openstack".format(file))

    def generate_ssh_keys(self):
        """Generates ssh key pair

        Writes the private key into a temp file and returns the file name

        :returns: path of the private key file

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

    def umount_config_drive(self, ssh, iso_path):
        """unmount config drive inside guest vm

        :param ssh: SSH connection to the VM
        :param iso_path: mount point of the config drive
        :type ssh: marvin.sshClient.SshClient
        :type iso_path: str
        """
        ssh.execute("umount -d %s" % iso_path)
        # Give the VM time to unlock the iso device
        time.sleep(2)
        # Verify umount
        result = ssh.execute("ls %s" % iso_path)
        self.assertTrue(len(result) == 0,
                        "After umount directory should be empty "
                        "but contains: %s" % result)

    def update_provider_state(self, new_state):
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

    def _get_test_data(self, key):
        return self.test_data[key]

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
        :rtype: CreateResult
        """
        if offering is None:
            self.debug("Creating Nuage VSP network offering...")
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
        except Exception:
            self.debug("Exception: %s" % sys.exc_info()[0])
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
            self.debug("Creating Nuage VSP VPC offering...")
            offering = self.create_VpcOffering(
                self._get_test_data(offering_name))
            self.validate_VpcOffering(offering, state="Enabled")
        try:
            vpc = self.create_Vpc(offering, cidr='10.1.0.0/16')
            self.validate_Vpc(vpc, state="Enabled")
            return self.CreateResult(True, offering=offering, vpc=vpc)
        except Exception:
            return self.CreateResult(False, offering=offering)

    def update_password_enable_in_template(self, new_state):
        self.debug("Updating guest VM template to password %s" % new_state)
        orig_state = self.template.passwordenabled
        if self.template.passwordenabled is not new_state:
            self.updateTemplate(new_state)
        self.assertEqual(self.template.passwordenabled, new_state,
                         "Guest VM template is not password enabled")
        return orig_state

    def verify_config_drive_content(self, vm,
                                    public_ip,
                                    password_test,
                                    userdata=None,
                                    metadata=False,
                                    ssh_key=None,
                                    ssh_client=None):
        """Verify Config Drive Content

        :param vm:
        :param public_ip:
        :param password_test:
        :param userdata:
        :param metadata:
        :param ssh_key:
        :param ssh_client: SSH Connection
        :type vm:
        :type public_ip:
        :type password_test:
        :type userdata: object
        :type metadata:
        :type ssh_key:
        :type ssh_client:
        :return: SSH Connection
        """

        if self.isSimulator:
            self.debug(
                "Simulator Environment: Skipping Config Drive content verification")
            return

        self.debug("SSHing into the VM %s" % vm.name)
        if ssh_client is None:
            ssh = self.ssh_into_VM(vm, public_ip)
        else:
            ssh = ssh_client
        d = {x.name: x for x in ssh.logger.handlers}
        ssh.logger.handlers = list(d.values())
        config_drive_path = self.mount_config_drive(ssh)
        self.assertIsNotNone(config_drive_path,
                             'ConfigdriveIso is not attached to vm')
        if metadata:
            self.debug("Verifying metadata for vm: %s" % vm.name)
            self.verifyMetaData(vm, ssh, config_drive_path)
            self.debug("Verifying openstackdata for vm: %s" % vm.name)
            self.verifyOpenStackData(ssh, config_drive_path)

        if userdata is not None:
            self.debug("Verifying userdata for vm: %s" % vm.name)
            self.verifyUserData(ssh, config_drive_path, userdata)
            self.verifyOpenStackUserData(ssh, config_drive_path, userdata)

        if password_test.test_presence:
            self.debug("Verifying password for vm: %s" % vm.name)
            test_result = self.verifyPassword(ssh, config_drive_path,
                                              password_test)
            self.assertEqual(test_result[0], password_test.presence,
                             "Expected is that password is present: %s "
                             " but found is: %s"
                             % (test_result[0], password_test.presence))

        if password_test.password is not None:
            self.debug("Password for vm is %s" % password_test.password)
            self.assertEqual(test_result[1], True,
                             "Password value test failed.")
        if ssh_key is not None:
            self.debug("Verifying sshkey for vm: %s" % vm.name)
            self.verifySshKey(ssh, config_drive_path, ssh_key)

        self.umount_config_drive(ssh, config_drive_path)
        return ssh

    def create_guest_vm(self, networks, acl_item=None,
                        vpc=None, keypair=None):
        vm = self.create_VM(
            networks,
            testdata=self.test_data["virtual_machine_userdata"],
            keypair=keypair)
        # Check VM
        self.check_VM_state(vm, state="Running")

        if keypair:
            self.decrypt_password(vm)

        # Check networks
        network_list = []
        if isinstance(networks, list):
            for network in networks:
                network_list.append(network)
        else:
            network_list.append(networks)

        for network in network_list:
            self.validate_Network(network, state="Implemented")

        if acl_item is not None:
            self.validate_firewall_rule(acl_item)
        return vm

    # nic_operation_VM - Performs NIC operations such as add, remove, and
    # update default NIC in the given VM and network
    def nic_operation_VM(self, vm, network, operation="add"):
        self.debug("Performing %s NIC operation in VM with ID - %s and "
                   "network with ID - %s" % (operation, vm.id, network.id))
        if operation is "add":
            vm.add_nic(self.api_client, network.id)
            self.debug("Added NIC in VM with ID - %s and network with ID - %s"
                       % (vm.id, network.id))
        vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
        for nic in vm_info.nic:
            if nic.networkid == network.id:
                nic_id = nic.id
        if operation is "update":
            vm.update_default_nic(self.api_client, nic_id)
            self.debug("Updated default NIC to NIC with ID- %s in VM with ID "
                       "- %s and network with ID - %s" %
                       (nic_id, vm.id, network.id))
        if operation is "remove":
            vm.remove_nic(self.api_client, nic_id)
            self.debug("Removed NIC with ID - %s in VM with ID - %s and "
                       "network with ID - %s" % (nic_id, vm.id, network.id))

    def update_userdata(self, vm, new_user_data):
        """Updates the user data of a VM

        :param vm: the Virtual Machine
        :param new_user_data: UserData to set
        :type vm: VirtualMachine
        :type new_user_data: str
        :returns: User data in base64 format
        :rtype: str
        """
        updated_user_data = base64.b64encode(new_user_data)
        vm.update(self.api_client, userdata=updated_user_data)
        return new_user_data

    def reset_password(self, vm):
        vm.password = vm.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm.password)
        self.debug("VM - %s password - %s !" %
                   (vm.name, vm.password))

    def wait_until_done(self, thread_list, name):
        for aThread in thread_list:
            self.debug("[Concurrency]Join %s for vm %s" % (name,
                                                           aThread.get_vm()))
            aThread.join()

    def update_sshkeypair(self, vm):
        """

        :type vm: VirtualMachine
        """
        vm.stop(self.api_client)
        vm_new_ssh = vm.resetSshKey(self.api_client,
                       keypair=self.keypair.name,
                       account=self.account.user[0].account,
                       domainid=self.account.domainid)

        self.debug("Sshkey reset to - %s" % self.keypair.name)
        vm.start(self.api_client)

        vm.details = vm_new_ssh.details

        # reset SSH key also resets the password.
        self.decrypt_password(vm)

    def decrypt_password(self, vm):
        """Decrypt VM password

        the new password is available in VM detail,
        named "Encrypted.Password".
        It is encrypted using the SSH Public Key,
        and thus can be decrypted using the SSH Private Key

        :type vm: VirtualMachine
        """
        try:
            from base64 import b64decode
            from Crypto.PublicKey import RSA
            from Crypto.Cipher import PKCS1_v1_5
            with open(self.keypair.private_key_file, "r") as pkfile:
                key = RSA.importKey(pkfile.read())
                cipher = PKCS1_v1_5.new(key)
            new_password = cipher.decrypt(
                b64decode(vm.details['Encrypted.Password']), None)
            if new_password:
                vm.password = new_password
            else:
                self.debug("Failed to decrypt new password")
        except:
            self.debug("Failed to decrypt new password")

    def add_subnet_verify(self, network, services):
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

    def ssh_into_VM(self, vm, public_ip, keypair):
        pass


class TestConfigDrive(cloudstackTestCase, ConfigDriveUtils):
    """Test user data and password reset functionality
    using configDrive
    """

    @classmethod
    def setUpClass(cls):
        # We want to fail quicker, if it's a failure
        socket.setdefaulttimeout(60)

        test_client = super(TestConfigDrive, cls).getClsTestClient()
        cls.api_client = test_client.getApiClient()
        cls.db_client = test_client.getDbConnection()
        cls.test_data = test_client.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.template = get_template(cls.api_client,
                                    cls.zone.id,
                                    cls.test_data["ostype"]
                                    )
        cls.test_data["virtual_machine"]["zoneid"] = cls.zone.id
        cls.test_data["virtual_machine"]["template"] = cls.template.id
        cls.test_data.update(Services().services)

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

    # create_FirewallRule - Creates (Ingress) Firewall rule on the given
    # Static NAT rule enabled public IP for Isolated networks
    def create_FirewallRule(self, public_ip, rule=None):
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
    # Provider in the Nuage VSP Physical Network, matches the given provider
    # name and state against the list of providers fetched
    def validate_NetworkServiceProvider(self, provider_name, state=None):
        """Validates the Network Service Provider in the Nuage VSP Physical
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
        return nw_off

    # validate_NetworkOffering - Validates the given Network offering, matches
    # the given network offering name and state against the list of network
    # offerings fetched
    def validate_NetworkOffering(self, net_offering, state=None):
        """Validates the Network offering"""
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
                                 vpcid=vpc.id if vpc else self.vpc.id
                                 if hasattr(self, "vpc") else None,
                                 aclid=acl_list.id if acl_list else None
                                 )
        self.debug("Created network with ID - %s" % network.id)
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
        return vpc_off

    # create_Vpc - Creates VPC with the given VPC offering
    def create_Vpc(self, vpc_offering, cidr='10.1.0.0/16', testdata=None,
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
    def validate_Vpc(self, vpc, state=None):
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

    # create_VM - Creates VM in the given network(s)
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
        return vm

    # check_VM_state - Checks if the given VM is in the expected state form the
    # list of fetched VMs
    def check_VM_state(self, vm, state=None):
        """Validates the VM state"""
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

    # validate_Network - Validates the given network, matches the given network
    # name and state against the list of networks fetched
    def validate_Network(self, network, state=None):
        """Validates the network"""
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

    # get_Router - Returns router for the given network
    def get_Router(self, network):
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
    def check_Router_state(self, router, state=None):
        """Validates the Router state"""
        self.debug("Validating the deployment and state of Router - %s" %
                   router.name)
        routers = Router.list(self.api_client,
                              id=router.id,
                              listall=True
                              )
        self.assertEqual(isinstance(routers, list), True,
                         "List router should return a valid list"
                         )
        if state:
            self.assertEqual(routers[0].state, state,
                             "Virtual router is not in the expected state"
                             )
        self.debug("Successfully validated the deployment and state of Router "
                   "- %s" % router.name)

    # acquire_PublicIPAddress - Acquires public IP address for the given
    # network/VPC
    def acquire_PublicIPAddress(self, network, vpc=None, account=None):
        if not account:
            account = self.account
        self.debug("Associating public IP for network with ID - %s in the "
                   "account - %s" % (network.id, account.name))
        public_ip = PublicIPAddress.create(self.api_client,
                                           accountid=account.name,
                                           domainid=account.domainid,
                                           zoneid=self.zone.id,
                                           networkid=network.id
                                           if vpc is None else None,
                                           vpcid=vpc.id if vpc else self.vpc.id
                                           if hasattr(self, "vpc") else None
                                           )
        self.debug("Associated public IP address - %s with network with ID - "
                   "%s" % (public_ip.ipaddress.ipaddress, network.id))
        return public_ip

    # migrate_VM - Migrates VM to another host, if available
    def migrate_VM(self, vm):
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

    @attr(tags=["advanced", "isonw"], required_hardware="true")
    def test_configdrive_isolated_network(self):
        """Test Configdrive as provider for isolated Networks
           to provide userdata and password reset functionality
        """

        # 1. When ConfigDrive is disabled as provider in zone
        #    Verify Isolated Network creation with a network offering
        #    which has userdata provided by ConfigDrive fails
        # 2. When ConfigDrive is enabled as provider in zone
        #    Create an Isolated Network with Isolated Network
        #    offering specifying ConfigDrive as serviceProvider
        #    for userdata.
        #    check if it is successfully created and
        #    is in the "Allocated" state.
        # 3. Deploy a VM in the created Isolated network with user data,
        #    check if the Isolated network state is changed to
        #    "Implemented", and the VM is successfully deployed and
        #    is in the "Running" state.
        # 4. SSH into the deployed VM and verify its user data in the iso
        #    (expected user data == actual user data).
        # 5. Verify that the guest VM's password in the iso.
        # 6. Reset VM password, and start the VM.
        # 7. Verify that the new guest VM template is password enabled by
        #     checking the VM's password (password != "password").
        # 8. SSH into the VM for verifying its new password
        #     after its password reset.
        # 9. Verify various scenarios and check the data in configdriveIso
        # 10. Delete all the created objects (cleanup).

        self.debug("+++Testing configdrive in an Isolated network fails..."
                   "as provider configdrive is still disabled...")
        self.update_provider_state("Disabled")
        create_network = self.verify_network_creation(
            offering_name="isolated_configdrive_network_offering",
            gateway='10.1.1.1')
        self.assertFalse(create_network.success,
                         'Network found success = %s, expected success =%s'
                         % (str(create_network.success), 'False'))

        self.debug("+++Test user data & password reset functionality "
                   "using configdrive in an Isolated network")

        self.update_provider_state("Enabled")
        create_network1 = self.verify_network_creation(
            offering=create_network.offering,
            gateway='10.1.1.1')
        self.assertTrue(create_network1.success,
                        'Network found success = %s, expected success = %s'
                        % (str(create_network1.success), 'True'))
        self.validate_Network(create_network1.network, state="Allocated")
        create_network2 = self.verify_network_creation(
            offering=create_network.offering,
            gateway='10.1.2.1')
        self.assertTrue(create_network2.success,
                        'Network found success = %s,expected success = %s'
                        % (str(create_network2.success), 'True'))
        self.validate_Network(create_network2.network, state="Allocated")
        self.update_password_enable_in_template(True)

        self.debug("+++Deploy VM in the created Isolated network "
                   "with user data provider as configdrive")
        self.generate_ssh_keys()
        self.debug("keypair name %s " % self.keypair.name)
        vm1 = self.create_guest_vm(create_network1.network,
                                   keypair=self.keypair.name)

        vr = self.get_Router(create_network1.network)
        self.check_Router_state(vr, state="Running")

        # We need to have the vm password
        vm1.password = vm1.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))

        public_ip_1 = self.acquire_PublicIPAddress(create_network1.network)
        self.create_and_verify_fip_and_fw(vm1, public_ip_1,
                                          create_network1.network)

        self.verify_config_drive_content(
            vm1, public_ip_1,
            self.PasswordTest(vm1.password),
            metadata=True,
            userdata=self.test_data[
                "virtual_machine_userdata"]["userdata"],
            ssh_key=self.keypair)

        expected_user_data1 = self.update_userdata(vm1, "helloworld vm1")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(True),
                                         userdata=expected_user_data1)

        self.generate_ssh_keys()
        self.update_sshkeypair(vm1)
        # After sshkey reset we need to have the vm password again
        vm1.password = vm1.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password),
                                         metadata=True,
                                         userdata=expected_user_data1,
                                         ssh_key=self.keypair)

        self.debug("Adding a non-default nic to the VM "
                   "making it a multi-nic VM...")
        self.nic_operation_VM(vm1, create_network2.network,
                              operation="add")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password),
                                         metadata=True,
                                         userdata=expected_user_data1,
                                         ssh_key=self.keypair)
        vm1.password = vm1.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))

        expected_user_data1 = self.update_userdata(vm1,
                                                   "hellomultinicvm1")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password),
                                         userdata=expected_user_data1,
                                         ssh_key=self.keypair)

        self.debug("updating non-default nic as the default nic "
                   "of the multi-nic VM and enable staticnat...")
        self.nic_operation_VM(vm1,
                              create_network2.network, operation="update")

        public_ip_2 = \
            self.acquire_PublicIPAddress(create_network2.network)
        self.create_and_verify_fip_and_fw(vm1, public_ip_2,
                                          create_network2.network)
        vm1.stop(self.api_client)
        vm1.start(self.api_client)
        self.verify_config_drive_content(vm1, public_ip_2,
                                         self.PasswordTest(False),
                                         metadata=True,
                                         userdata=expected_user_data1)
        vm1.password = vm1.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))
        self.verify_config_drive_content(vm1, public_ip_2,
                                         self.PasswordTest(vm1.password),
                                         userdata=expected_user_data1)
        expected_user_data1 = self.update_userdata(vm1,
                                                   "hellomultinicvm1")
        self.verify_config_drive_content(vm1, public_ip_2,
                                         self.PasswordTest(True),
                                         userdata=expected_user_data1)

        self.debug("Updating the default nic of the multi-nic VM, "
                   "deleting the non-default nic...")
        self.nic_operation_VM(vm1,
                              create_network1.network, operation="update")
        vm1.stop(self.api_client)
        vm1.start(self.api_client)
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(False),
                                         metadata=True,
                                         userdata=expected_user_data1)

        multinicvm1 = self.create_guest_vm([create_network2.network,
                                            create_network1.network])
        multinicvm1.password = multinicvm1.resetPassword(self.api_client)
        self.debug("MultiNICVM Password reset to - %s"
                   % multinicvm1.password)
        self.debug("MultiNICVM - %s password - %s !"
                   % (multinicvm1.name, multinicvm1.password))

        public_ip_3 = self.acquire_PublicIPAddress(create_network2.network)
        self.create_and_verify_fip_and_fw(multinicvm1, public_ip_3,
                                          create_network2.network)
        self.verify_config_drive_content(
            multinicvm1, public_ip_3,
            self.PasswordTest(multinicvm1.password),
            metadata=True,
            userdata=self.test_data[
                "virtual_machine_userdata"]["userdata"])
        expected_user_data2 = self.update_userdata(multinicvm1,
                                                   "hello multinicvm1")
        self.verify_config_drive_content(multinicvm1, public_ip_3,
                                         self.PasswordTest(True),
                                         userdata=expected_user_data2)

        multinicvm1.delete(self.api_client, expunge=True)
        public_ip_3.delete(self.api_client)
        public_ip_2.delete(self.api_client)
        self.nic_operation_VM(vm1,
                              create_network2.network, operation="remove")
        create_network2.network.delete(self.api_client)

        vm1.password = vm1.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))

        self.debug("+++ Restarting the created Isolated network without "
                   "cleanup...")
        create_network1.network.restart(self.api_client, cleanup=False)
        self.validate_Network(create_network1.network,
                              state="Implemented")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("+++ Restarting the created Isolated network with "
                   "cleanup...")
        create_network1.network.restart(self.api_client, cleanup=True)
        self.validate_Network(create_network1.network,
                              state="Implemented")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("+++Verifying userdata after rebootVM - %s" % vm1.name)
        vm1.reboot(self.api_client)
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password),
                                         metadata=True,
                                         userdata=expected_user_data1,
                                         ssh_key=self.keypair)

        self.debug("Updating userdata for VM - %s" % vm1.name)
        expected_user_data1 = self.update_userdata(vm1, "hello afterboot")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password),
                                         userdata=expected_user_data1,
                                         ssh_key=self.keypair)
        self.debug("Resetting password for VM - %s" % vm1.name)
        self.reset_password(vm1)
        self.debug("SSHing into the VM for verifying its new password "
                   "after its password reset...")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password))

        self.debug("+++ Migrating one of the VMs in the created Isolated "
                   "network to another host, if available...")
        self.migrate_VM(vm1)
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("Updating userdata after migrating VM - %s" % vm1.name)
        expected_user_data1 = self.update_userdata(vm1,
                                                   "hello after migrate")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password),
                                         userdata=expected_user_data1)
        self.debug("Resetting password for VM - %s" % vm1.name)
        self.reset_password(vm1)
        self.debug("SSHing into the VM for verifying its new password "
                   "after its password reset...")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password))

        self.debug("+++Verify userdata after stopstartVM - %s" % vm1.name)
        vm1.stop(self.api_client)
        vm1.start(self.api_client)
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("Updating userdata for VM - %s" % vm1.name)
        expected_user_data1 = self.update_userdata(vm1,
                                                   "hello afterstopstart")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data1)
        self.debug("Resetting password for VM - %s" % vm1.name)
        self.reset_password(vm1)
        self.debug("SSHing into the VM for verifying its new password "
                   "after its password reset...")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(vm1.password))

        self.debug("+++ Verify userdata after VM recover- %s" % vm1.name)
        vm1.delete(self.api_client, expunge=False)
        self.debug("Recover VM - %s" % vm1.name)
        vm1.recover(self.api_client)
        vm1.start(self.api_client)
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)
        self.update_provider_state("Disabled")
        expected_user_data1 = self.update_userdata(vm1,
                                                   "hello after recover")
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("+++ When template is not password enabled, "
                   "verify configdrive of VM - %s" % vm1.name)
        vm1.delete(self.api_client, expunge=True)
        self.update_provider_state("Enabled")
        self.updateTemplate(False)
        self.generate_ssh_keys()
        self.debug("keypair name %s " % self.keypair.name)
        vm1 = self.create_guest_vm(create_network1.network,
                                   keypair=self.keypair.name)

        expected_user_data1 = self.update_userdata(vm1,
                                                   "This is sample data")
        public_ip_1 = \
            self.acquire_PublicIPAddress(create_network1.network)
        self.create_and_verify_fip_and_fw(vm1, public_ip_1,
                                          create_network1.network)
        self.verify_config_drive_content(vm1, public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)
        vm1.delete(self.api_client, expunge=True)
        create_network1.network.delete(self.api_client)

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

    @attr(tags=["advanced", "vpc"], required_hardware="true")
    def test_configdrive_vpc_network(self):
        """Test Configdrive for VPC Networks
           choose user data with configDrive as service provider
           and test password reset functionality using ConfigDrive
        """

        # 1. Verify VPC Network creation with ConfigDrive fails
        #    as ConfigDrive is disabled as provider
        # 2. Create a VPC Network with VPC tier Network
        #    offering specifying ConfigDrive as serviceProvider for userdata.
        #    check if it is successfully created and is in "Allocated" state.
        # 3. Deploy a VM in the created VPC tier network with user data,
        #    check if the Isolated network state is changed to "Implemented",
        #    and the VM is successfully deployed and is in "Running" state.
        # 4. SSH into the deployed VM and verify its user data in the iso
        #    (expected user data == actual user data).
        # 5. Verify that the guest VM's password in the iso.
        # 6. Reset VM password, and start the VM.
        # 7. Verify that the new guest VM template is password enabled by
        #    checking the VM's password (password != "password").
        # 8. SSH into the VM for verifying its new password
        #     after its password reset.
        # 9. Verify various scenarios and check the data in configdrive iso
        # 10. Delete all the created objects (cleanup).
        self.update_provider_state("Enabled")
        create_vpc = self.verify_vpc_creation(
            offering_name="vpc_offering_configdrive")
        self.assertTrue(create_vpc.success,
                        "Vpc found success = %s, expected success = %s"
                        % (str(create_vpc.success), 'True'))
        acl_list = self.create_NetworkAclList(
            name="acl", description="acl", vpc=create_vpc.vpc)
        acl_item = self.create_NetworkAclRule(
            self.test_data["ingress_rule"], acl_list=acl_list)
        self.update_provider_state("Disabled")
        self.debug("+++Testing configdrive in a VPC Tier network fails..."
                   "as provider configdrive is still disabled...")
        create_networkfails = \
            self.verify_network_creation(
                offering_name="vpc_network_offering_configdrive",
                gateway='10.1.1.1',
                vpc=create_vpc.vpc,
                acl_list=acl_list)
        self.assertFalse(create_networkfails.success,
                         "Create Network found success = %s, "
                         "expected success = %s"
                         % (str(create_networkfails.success), 'False'))
        self.debug("Testing user data&password reset functionality using"
                   "configdrive in a VPC network...")
        self.update_provider_state("Enabled")

        create_tiernetwork = \
            self.verify_network_creation(
                offering=create_networkfails.offering,
                gateway='10.1.1.1',
                vpc=create_vpc.vpc,
                acl_list=acl_list)
        self.assertTrue(create_tiernetwork.success,
                        "Create Network found success = %s, "
                        "expected success = %s"
                        % (str(create_tiernetwork.success), 'True'))
        self.validate_Network(create_tiernetwork.network,
                              state="Implemented")

        vpc_vr = self.get_Router(create_tiernetwork.network)
        self.check_Router_state(vpc_vr, state="Running")

        create_tiernetwork2 = \
            self.verify_network_creation(
                offering=create_networkfails.offering,
                gateway='10.1.2.1',
                vpc=create_vpc.vpc,
                acl_list=acl_list)
        self.assertTrue(create_tiernetwork2.success,
                        'Network found success= %s, expected success= %s'
                        % (str(create_tiernetwork2.success), 'True'))
        self.validate_Network(create_tiernetwork2.network,
                              state="Implemented")

        vpc_vr2 = self.get_Router(create_tiernetwork2.network)
        self.check_Router_state(vpc_vr2, state="Running")

        self.update_password_enable_in_template(True)

        self.generate_ssh_keys()
        self.debug("keypair name %s " % self.keypair.name)
        vm = self.create_guest_vm(create_tiernetwork.network,
                                  acl_item,
                                  vpc=create_vpc.vpc,
                                  keypair=self.keypair.name)

        vpc_public_ip_1 = \
            self.acquire_PublicIPAddress(create_tiernetwork.network,
                                         create_vpc.vpc)
        self.create_StaticNatRule_For_VM(vm, vpc_public_ip_1,
                                         create_tiernetwork.network)

        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(True),
                                         metadata=True,
                                         ssh_key=self.keypair)

        expected_user_data = self.update_userdata(vm, "helloworld vm1")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(True),
                                         metadata=True,
                                         userdata=expected_user_data,
                                         ssh_key=self.keypair)

        self.debug("Resetting password for VM - %s" % vm.name)
        self.reset_password(vm)
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data,
                                         ssh_key=self.keypair)

        self.generate_ssh_keys()
        self.update_sshkeypair(vm)
        # After sshkey reset we need to have the vm password again
        vm.password = vm.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm.password)
        self.debug("VM - %s password - %s !" %
                   (vm.name, vm.password))

        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         metadata=True,
                                         userdata=expected_user_data,
                                         ssh_key=self.keypair)

        self.debug("+++ Restarting the created vpc without "
                   "cleanup...")
        self.restart_Vpc(create_vpc.vpc, cleanup=False)
        self.validate_Vpc(create_vpc.vpc, state="Enabled")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("Adding a non-default nic to the VM "
                   "making it a multi-nic VM...")
        self.nic_operation_VM(vm, create_tiernetwork2.network,
                              operation="add")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         metadata=True,
                                         userdata=expected_user_data,
                                         ssh_key=self.keypair)

        vm.password = vm.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm.password)
        self.debug("VM - %s password - %s !" %
                   (vm.name, vm.password))
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         metadata=True,
                                         userdata=expected_user_data,
                                         ssh_key=self.keypair)
        expected_user_data1 = self.update_userdata(vm, "hellomultinicvm1")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data1,
                                         ssh_key=self.keypair)

        self.debug("updating non-default nic as the default nic "
                   "of the multi-nic VM and enable staticnat...")
        self.nic_operation_VM(vm,
                              create_tiernetwork2.network,
                              operation="update")
        vm.stop(self.api_client)
        vm.start(self.api_client)
        vpc_public_ip_2 = \
            self.acquire_PublicIPAddress(create_tiernetwork2.network,
                                         create_vpc.vpc)
        self.create_StaticNatRule_For_VM(vm, vpc_public_ip_2,
                                     create_tiernetwork2.network)

        self.verify_config_drive_content(vm, vpc_public_ip_2,
                                         self.PasswordTest(vm.password),
                                         metadata=True,
                                         userdata=expected_user_data1)
        vm.password = vm.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm.password)
        self.debug("VM - %s password - %s !" %
                   (vm.name, vm.password))
        self.verify_config_drive_content(vm, vpc_public_ip_2,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data1)
        expected_user_data1 = self.update_userdata(vm, "hellomultinicvm1")
        self.verify_config_drive_content(vm, vpc_public_ip_2,
                                         self.PasswordTest(True),
                                         userdata=expected_user_data1)

        self.debug("Updating the default nic of the multi-nic VM, "
                   "deleting the non-default nic...")
        self.nic_operation_VM(vm,
                              create_tiernetwork.network,
                              operation="update")
        self.verify_config_drive_content(vm, vpc_public_ip_2,
                                         self.PasswordTest(True),
                                         metadata=True,
                                         userdata=expected_user_data1)
        vpc_public_ip_2.delete(self.api_client)
        self.nic_operation_VM(vm,
                              create_tiernetwork2.network,
                              operation="remove")
        create_tiernetwork2.network.delete(self.api_client)

        vm.password = vm.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm.password)
        self.debug("VM - %s password - %s !" %
                   (vm.name, vm.password))

        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("+++ Restarting the created vpc with "
                   "cleanup...")
        self.restart_Vpc(create_vpc.vpc, cleanup=True)
        self.validate_Vpc(create_vpc.vpc, state="Enabled")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("+++ Restarting the created VPC Tier network without "
                   "cleanup...")
        create_tiernetwork.network.restart(self.api_client, cleanup=False)
        self.validate_Network(create_tiernetwork.network,
                              state="Implemented")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("+++ Restarting the created VPC Tier network with "
                   "cleanup...")
        create_tiernetwork.network.restart(self.api_client, cleanup=True)
        self.validate_Network(create_tiernetwork.network,
                              state="Implemented")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data1,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("+++ Restarting the created vpc without "
                   "cleanup...")
        self.restart_Vpc(create_vpc.vpc, cleanup=False)
        self.validate_Vpc(create_vpc.vpc, state="Enabled")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data1,
                                         metadata=True)

        self.debug("+++ Restarting the created vpc with "
                   "cleanup...")
        self.restart_Vpc(create_vpc.vpc, cleanup=True)
        self.validate_Vpc(create_vpc.vpc, state="Enabled")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data1,
                                         metadata=True)

        self.debug("+++ Verify userdata after rebootVM - %s" % vm.name)
        vm.reboot(self.api_client)
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         metadata=True,
                                         userdata=expected_user_data1,
                                         ssh_key=self.keypair)

        self.debug("Updating userdata for VM - %s" % vm.name)
        expected_user_data = self.update_userdata(vm,
                                                  "hellovm after reboot")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data,
                                         ssh_key=self.keypair)
        self.debug("Resetting password for VM - %s" % vm.name)
        self.reset_password(vm)
        self.debug("SSHing into the VM for verifying its new password "
                   "after its password reset...")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password))

        self.debug("+++ Migrating one of the VMs in the created "
                   "VPC Tier network to another host, if available...")
        self.migrate_VM(vm)
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("Updating userdata after migrating VM - %s" % vm.name)
        expected_user_data = self.update_userdata(vm,
                                                  "hellovm after migrate")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password),
                                         userdata=expected_user_data,
                                         ssh_key=self.keypair)
        self.debug("Resetting password for VM - %s" % vm.name)
        self.reset_password(vm)
        self.debug("SSHing into the VM for verifying its new password "
                   "after its password reset...")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password))

        self.debug("+++ Verify userdata after stopstartVM - %s" % vm.name)
        vm.stop(self.api_client)
        vm.start(self.api_client)
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("Updating userdata for VM - %s" % vm.name)
        expected_user_data = self.update_userdata(vm,
                                                  "hello after stopstart")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data,
                                         ssh_key=self.keypair)
        self.debug("Resetting password for VM - %s" % vm.name)
        self.reset_password(vm)
        self.debug("SSHing into the VM for verifying its new password "
                   "after its password reset...")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(vm.password))

        self.debug("+++ Verify userdata after recoverVM - %s" % vm.name)
        vm.delete(self.api_client, expunge=False)
        self.debug("Recover VM - %s" % vm.name)
        vm.recover(self.api_client)
        vm.start(self.api_client)
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data,
                                         metadata=True,
                                         ssh_key=self.keypair)
        self.update_provider_state("Disabled")
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data,
                                         metadata=True,
                                         ssh_key=self.keypair)

        self.debug("+++ When template is not password enabled "
                   "verify configdrive of VM - %s" % vm.name)
        vm.delete(self.api_client, expunge=True)
        self.update_provider_state("Enabled")
        self.updateTemplate(False)

        self.generate_ssh_keys()
        self.debug("keypair name %s " % self.keypair.name)
        vm = self.create_guest_vm(create_tiernetwork.network,
                                  acl_item,
                                  vpc=create_vpc.vpc,
                                  keypair=self.keypair.name)

        expected_user_data = self.update_userdata(vm,
                                                  "This is sample data")
        vpc_public_ip_1 = \
            self.acquire_PublicIPAddress(create_tiernetwork.network,
                                         create_vpc.vpc)
        self.create_StaticNatRule_For_VM(vm, vpc_public_ip_1,
                                         create_tiernetwork.network)
        self.verify_config_drive_content(vm, vpc_public_ip_1,
                                         self.PasswordTest(False),
                                         userdata=expected_user_data,
                                         metadata=True,
                                         ssh_key=self.keypair)
        vm.delete(self.api_client, expunge=True)
        create_tiernetwork.network.delete(self.api_client)

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
        self.update_provider_state("Disabled")
        shared_test_data = self.test_data["acl"]["network_all_1"]
        shared_network = self.verify_network_creation(
            offering_name="shared_network_config_drive_offering",
            testdata=shared_test_data)
        self.assertFalse(shared_network.success,
                         'Network found success = %s, expected success =%s'
                         % (str(shared_network.success), 'False'))

        self.update_provider_state("Enabled")
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

        self.update_password_enable_in_template(True)

        self.generate_ssh_keys()
        self.debug("keypair name %s " % self.keypair.name)

        self.debug("+++Deploy of a VM on a shared network with multiple "
                   "ip ranges, all should have the same value for the "
                   "underlay flag.")
        # Add subnet of different gateway
        self.debug("+++ Adding subnet of different gateway")

        self.add_subnet_verify(
            shared_network.network,
            self.test_data["publiciprange2"])
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

        shared_vr = self.get_Router(shared_network.network)
        self.check_Router_state(shared_vr, state="Running")

        # We need to have the vm password
        vm1.password = vm1.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))
        self.update_userdata(vm1, "helloworld vm1")

        self.debug("Adding a non-default nic to the VM "
                   "making it a multi-nic VM...")
        self.nic_operation_VM(vm1, shared_network2.network,
                              operation="add")
        vm1.password = vm1.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))

        self.debug("updating non-default nic as the default nic "
                   "of the multi-nic VM...")
        self.nic_operation_VM(vm1,
                              shared_network2.network, operation="update")
        vm1.stop(self.api_client)
        vm1.start(self.api_client)

        vm1.password = vm1.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))
        self.update_userdata(vm1, "hellomultinicvm1")

        self.debug("Updating the default nic of the multi-nic VM, "
                   "deleting the non-default nic...")
        self.nic_operation_VM(vm1,
                              shared_network.network, operation="update")
        vm1.stop(self.api_client)
        vm1.start(self.api_client)

        self.nic_operation_VM(vm1,
                              shared_network2.network, operation="remove")
        shared_network2.network.delete(self.api_client)
        # We need to have the vm password
        vm1.password = vm1.resetPassword(self.api_client)
        self.debug("Password reset to - %s" % vm1.password)
        self.debug("VM - %s password - %s !" %
                   (vm1.name, vm1.password))

        self.debug("+++ When template is not password enabled, "
                   "verify configdrive of VM - %s" % vm1.name)
        vm1.delete(self.api_client, expunge=True)
        self.update_provider_state("Enabled")
        self.updateTemplate(False)
        self.generate_ssh_keys()
        self.debug("keypair name %s " % self.keypair.name)
        vm1 = self.create_VM(
            [shared_network.network],
            testdata=self.test_data["virtual_machine_userdata"],
            keypair=self.keypair.name)
        vm1.delete(self.api_client, expunge=True)
        shared_network.network.delete(self.api_client)
