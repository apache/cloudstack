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

""" Component tests for Pre and Post Upgrade CCP Functionality.

"""
FAIL = 0
SUCCESS = "SUCCESS"

from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (Account,
                             VpcOffering,
                             VPC,
                             ServiceOffering,
                             DiskOffering,
                             NetworkACL,
                             NetworkOffering,
                             Network,
                             VirtualMachine,
                             Template,
                             Iso,
                             Snapshot,
                             Volume)
from marvin.lib.common import (list_publicIP,
                               get_domain,
                               list_templates,
                               list_isos,
                               list_volumes,
                               list_zones,
                               list_virtual_machines)
from marvin.lib.utils import cleanup_resources
import time
from marvin.sshClient import SshClient
from marvin.lib.utils import (random_gen)
import json
import types
import marvin.jsonHelper as jsonHelper


class TestMinimalUpgradeChecks(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(
            TestMinimalUpgradeChecks,
            cls
        ).getClsTestClient()

        cls.api_client = cloudstackTestClient.getApiClient()
        cls.services = cloudstackTestClient.getConfigParser().parsedDict

        # Store Objects to this dictionary that needs to be tested post Upgrade
        cls.ug_dict = {}

        if cls.services is None:
            cls.debug("Services Object is None")
            raise Exception("Services Object is None")

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)

        #cls.zone = get_zone(cls.api_client, cls.services)
        list_zones_response = list_zones(
            cls.api_client,
            name=cls.services["zone"]["name"]
        )

        cls.zone = list_zones_response[0]

    @classmethod
    def tearDownClass(cls):
        return

    @classmethod
    def filecopy(cls, virtual_machine, localfile=None, remotefilelocation=None, permissions="644"):
        cls.ssh = SshClient(
            host=virtual_machine.public_ip,
            port=TestMinimalUpgradeChecks.services["virtual_machine"]["ssh_port"],
            user='root',
            passwd='password')

        cls.ssh.scp(localfile, remotefilelocation)
        cls.ssh.runCommand('chmod %s %s' % (permissions, remotefilelocation))
        cls.debug("%s file successfully copied to %s " % (localfile, remotefilelocation))

        cls.ssh.close()

    @classmethod
    def filecreation(cls, virtual_machine):
        cls.ssh = SshClient(
            host=virtual_machine.public_ip,
            port=TestMinimalUpgradeChecks.services["virtual_machine"]["ssh_port"],
            user='root',
            passwd='password')

        ssh_output = cls.ssh.runCommand('dd if=/dev/zero of=/root/test1.txt bs=2MB count=100')

        if ssh_output["status"] is SUCCESS:
            cls.debug("test1.txt file got successfully created on VM at %s " % virtual_machine.public_ip)
        else:
            cls.debug("Failed to create test1.txt random file on the VM")
            raise Exception("Failed to create test1.txt random file on the VM")

        cls.ssh.close()

    @staticmethod
    def get_guest_ip_address(guest_ip_address):

        for ch in ["'", "[", "]", "\\", "n"]:
            if ch in guest_ip_address:
                guest_ip_address = guest_ip_address.replace(ch, "")

        return guest_ip_address

    def check_type_value(self, obj_dict):

        type_dict = {}

        self.debug(obj_dict)

        for m, n in obj_dict.items():

            if type(n) is types.DictType:
                for k, v in n.items():

                    if type(v) in type_dict:
                        type_dict[type(v)][k] = v
                    else:
                        type_dict[type(v)] = {}
                        type_dict[type(v)][k] = v

                    if isinstance(v, jsonHelper.jsonLoader):
                        self.debug("JSON LOADER Class Identified. It is not in a lis")

                        if type(v) in type_dict:
                            type_dict[type(v)][k] = v
                        else:
                            type_dict[type(v)] = {}
                            type_dict[type(v)][k] = v

                    if isinstance(v, types.ListType):
                        if len(v) == 0:
                            self.debug("Length of this List is Zero")
                            continue

                        self.debug("List Object Type")
                        self.debug(type(v[0]))

                        if isinstance(v[0], jsonHelper.jsonLoader):
                            self.debug("JSON LOADER Class Identified in a List!!")

                        if type(v[0]) in type_dict:
                            type_dict[type(v[0])][k] = v[0]
                        else:
                            type_dict[type(v[0])] = {}
                            type_dict[type(v[0])][k] = v[0]

            else:
                self.debug("N Value is of %s Type" % type(n))

        return type_dict

    def contruct_dictionary(self, obj_dict, type_dict):

        output_dict = {}

        self.debug("Upgrade Objects collected Data: ")
        self.debug(obj_dict)

        for m, n in obj_dict.items():

            if type(n) is types.DictType:

                refined_dict = {}

                for k, v in n.items():

                    if isinstance(v, jsonHelper.jsonLoader):
                        self.debug("JSON LOADER Class Identified. It is not in a list")
                        continue

                    if isinstance(v, types.ListType):
                        if len(v) == 0:
                            self.debug("Length of this List is Zero")
                            continue

                        self.debug("List Object Type")
                        self.debug(type(v[0]))

                        if isinstance(v[0], jsonHelper.jsonLoader):
                            self.debug("JSON LOADER Class Identified in a List!!")
                            continue

                    if type(v) in type_dict:
                        refined_dict[k] = v
                    else:
                        raise Exception("Type of the Object is not Registered!!")

                output_dict[m] = refined_dict

            else:
                self.debug("N Value is of %s Type" % type(n))
                raise Exception("N Value is of %s Type" % type(n))

        return output_dict

    def vm_accessibility_check(self, src_VM, dst_VM, first_time=True):

        self.ssh = SshClient(
            host=src_VM.public_ip,
            port=TestMinimalUpgradeChecks.services["virtual_machine"]["ssh_port"],
            user='root',
            passwd='password')

        self.debug("Source VM Accessed. VM with ID: %s" % src_VM.id)

        if first_time:
            # Execute "firstconn_expectscript.exp" expect script on the Source VM, in order to connect to
            # Destination VM deployed before upgrade and retrieve the guest ip address of the VM
            self.debug(
                'Execute "firstconn_expectscript.exp" expect script on the Source VM, '
                'in order to connect to Destination VM deployed before upgrade and '
                'retrieve the guest ip address of the VM')

            ssh_output = self.ssh.runCommand('/tmp/firstconn_expectscript.exp %s %s %s %s' % (
                dst_VM.public_ip, 'password', src_VM.public_ip, 'password'))

        else:
            # Execute "secondconn_expectscript.exp" expect script on the Source VM,
            # in order to connect to Destination VM deployed before upgrade and
            # retrieve the guest ip address of the VM
            self.debug(
                'Execute "secondconn_expectscript.exp" expect script on the Source VM, '
                'in order to connect to Destination VM deployed before upgrade and '
                'retrieve the guest ip address of the VM')

            ssh_output = self.ssh.runCommand('/tmp/secondconn_expectscript.exp %s %s %s %s' % (
                dst_VM.public_ip, 'password', src_VM.public_ip, 'password'))

        if ssh_output["status"] is SUCCESS:
            ssh_output = self.ssh.runCommand('cat /tmp/vm_ip_address')
        else:
            self.debug("firstconn/secondconn_expectscript.exp Script execution didnt succeed")
            raise Exception("firstconn/secondconn_expectscript.exp Script execution didnt succeed")

        guest_ip_address = str(ssh_output["stdout"])

        self.debug("IP Address SCPed: %s" % guest_ip_address)
        self.debug("IP Address to be compared with: %s" % str(dst_VM.nicip))

        guest_ip_address = self.get_guest_ip_address(guest_ip_address)

        self.assertEqual(guest_ip_address, dst_VM.nicip, "Failed to Test the Connection")
        ssh_output = self.ssh.runCommand('rm -rf /tmp/vm_ip_address')
        self.debug('rm -rf /tmp/vm_ip_address')

        if ssh_output["status"] is SUCCESS:
            removed_file = self.ssh.runCommand('ls -lh /tmp/vm_ip_address')
            self.debug('ls -lh /tmp/vm_ip_address')

            if removed_file["status"] == FAIL and "No such file" in str(removed_file["stderr"]):
                self.ssh.close()
                return
            elif removed_file["status"] == SUCCESS:
                self.debug("Unable to delete vm_ip_address file")
                raise Exception("Unable to delete vm_ip_address file")
            else:
                raise Exception("SSH Command Execution Failure")

    def vm_inaccessibility_check(self, src_VM, dst_VM):

        self.ssh = SshClient(
            host=src_VM.public_ip,
            port=TestMinimalUpgradeChecks.services["virtual_machine"]["ssh_port"],
            user='root',
            passwd='password')

        self.debug("Source VM Accessed. VM with ID: %s" % src_VM.id)

        # Check if the Source VM is able to reach the Destination VM
        ssh_output = self.ssh.runCommand('ssh root@%s' % dst_VM.public_ip)

        self.debug("Error Information of SSH Connection: %s " % (ssh_output["stderr"]))
        # Verify that the VPN Client fails to reach the VM on the VPC
        if ssh_output["status"] == FAIL:
            # Verify that the VM is no longer accessible after being stopped
            self.debug("Verified that the VM is no longer accessible after being stopped")
            self.ssh.close()
            return
        elif ssh_output["status"] == SUCCESS:
            raise Exception("Looks like the VM is not stopped and is still accessible")
        else:
            raise Exception("SSH Command Execution Failure")

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = None
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            #raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["preupgrade"])
    def test_01_PreUpgradeTest(self):
        """ Test case no : Generation of CCP Objects Prior to Upgrade
        """

        # Validate the following
        # 1. Create Service Offering with Host tag 'h1'
        # 2. Create Service Offering with Host tag 'h2'
        # 3. Create Disk Offering of 5 GB Size
        # 4. Create Disk Offering of 15 GB Size
        # 5. Create First Account
        # 6. Register a Template
        # 7. Register an Windows 2008 R2 ISO
        # 8. Download the Registered Template
        # 9. Download the Registered ISO
        # 10. Deploy First VM from the Template using h1 tagged Service Offering and 5GB Data Disk Offering
        # 11. Create Port Forwarding Rule for the First VM
        # 12. Deploy Second VM from the Template using h2 tagged Service Offering and 5GB Data Disk Offering
        # 13. Create Port Forwarding Rule for the Second VM
        # 14. Deploy Third VM from Windows 2008 R2 ISO using h1 tagged Service Offering with 15GB Disk Offering
        # 15. Create a Second Account
        # 16. Create VPC Offering
        # 17. Create First VPC for the Second Account
        # 18. Create Network Offering for the network tier creation for VPC
        # 19. Create First Network Tier in the First VPC
        # 20. Create Egress and Ingress Rules to the First Network Tier
        # 21. Deploy a VM in the First Network Tier using h1 tagged Service Offering
        # 22. Deploy another VM in the First Network Tier using h2 tagged Service Offering
        # 23. Create File on First VM's ROOT Volume
        # 24. Create Snapshot of First VM's ROOT Volume
        # 25. Create File on First VM's ROOT Volume
        # 26. Create Snapshot of Second VM's ROOT Volume

        try:

            # Create a service offering with host tag 'h1'
            self.service_offering_h1 = ServiceOffering.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["service_offering_h1"],
                hosttags=TestMinimalUpgradeChecks.services["service_offering_h1"]["hosttags"]
            )

            # Create a service offering with host tag 'h2'
            self.service_offering_h2 = ServiceOffering.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["service_offering_h2"],
                hosttags=TestMinimalUpgradeChecks.services["service_offering_h2"]["hosttags"]
            )

            # Create a Disk Offering of 5 GB Size
            self.disk_offering_5gb = DiskOffering.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["disk_offering_shared_5GB"],
                domainid=TestMinimalUpgradeChecks.domain.id
            )

            # Create a Disk Offering of 15 GB Size
            self.disk_offering_15gb = DiskOffering.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["disk_offering_shared_15GB"],
                domainid=TestMinimalUpgradeChecks.domain.id
            )

            # Store the service offerings and disk offerings created
            TestMinimalUpgradeChecks.ug_dict.update({
                'service_offering_h1': self.service_offering_h1.__dict__,
                'service_offering_h2': self.service_offering_h2.__dict__,
                'disk_offering_5gb': self.disk_offering_5gb.__dict__,
                'disk_offering_15gb': self.disk_offering_15gb.__dict__
            })

            # Create First Account that possesses the VPN Client, which is used to test Remote VPN Access to VPCs
            self.firstaccount = Account.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["account"],
                admin=True,
                domainid=TestMinimalUpgradeChecks.domain.id
            )

            # Add firstaccount to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'firstaccount': self.firstaccount.__dict__})

            # Register a Template that already has VPN client installed on it. The template registered here
            # has extra scripts to facilitate automated operations to execute Test Cases.
            # Template has pre-configured configuration files required for the VPN Client operations.
            # The following files are present on the registered template. The location of the files are locations
            # on a VM deployed from this template
            #            1. "/tmp/ipsec.conf"
            #            2. "/tmp/ipsec.secrets"
            #            3. "/tmp/options.xl2tpd.client"
            #            4. "/tmp/xl2tpd.conf"
            #            5  "/tmp/vpnclient_services.sh"
            #            6. "/tmp/firstconn_expectscript.exp"
            #            7. "/tmp/secondconn_expectscript.exp"

            self.template = Template.register(
                self.apiclient,
                TestMinimalUpgradeChecks.services["vpn_template"],
                zoneid=TestMinimalUpgradeChecks.zone.id,
                account=self.firstaccount.name,
                domainid=TestMinimalUpgradeChecks.domain.id
            )

            # Add Template to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'template': self.template.__dict__})

            TestMinimalUpgradeChecks.services["windows_2008_R2_iso"]["zoneid"] = TestMinimalUpgradeChecks.zone.id

            # Register a Windows 2008 R2 ISO"
            self.iso = Iso.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["windows_2008_R2_iso"],
                account=self.firstaccount.name,
                domainid=TestMinimalUpgradeChecks.domain.id
            )

            # Add ISO to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'iso': self.iso.__dict__})

            # Wait for template status to be changed across
            time.sleep(TestMinimalUpgradeChecks.services["sleep"])
            timeout = TestMinimalUpgradeChecks.services["timeout"]

            while True:
                list_template_response = list_templates(
                    self.apiclient,
                    templatefilter='all',
                    id=self.template.id,
                    zoneid=TestMinimalUpgradeChecks.zone.id,
                    account=self.firstaccount.name,
                    domainid=TestMinimalUpgradeChecks.domain.id
                )

                if isinstance(list_template_response, list):
                    break
                elif timeout == 0:
                    raise Exception("List template failed!")

                time.sleep(5)
                timeout = timeout - 1

            #Verify template response to check whether template added successfully
            assert isinstance(list_template_response, list), "Check for list template response return valid data"

            assert len(list_template_response) != 0, "Check template available in List Templates"

            template_response = list_template_response[0]

            assert template_response.isready, "Template state is not ready, it is %r" % template_response.isready

            #            # Download the Registered Template
            self.template.download(self.apiclient, interval=120)

            # Wait for ISO status to be changed
            time.sleep(TestMinimalUpgradeChecks.services["sleep"])
            timeout = TestMinimalUpgradeChecks.services["timeout"]

            # Check if the ISO is downloaded
            while True:
                list_isos_response = list_isos(
                    self.apiclient,
                    isofilter='all',
                    id=self.iso.id,
                    zoneid=TestMinimalUpgradeChecks.zone.id,
                    account=self.firstaccount.name,
                    domainid=TestMinimalUpgradeChecks.domain.id
                )

                if isinstance(list_isos_response, list):
                    break
                elif timeout == 0:
                    raise Exception("List ISO failed!")

                time.sleep(5)
                timeout = timeout - 1

            #Verify ISO response to check whether ISO registered successfully
            assert isinstance(list_isos_response, list), "Check for list ISOs response return valid data"

            assert len(list_isos_response) != 0, "Check ISO available in List Templates"

            iso_response = list_isos_response[0]

            assert iso_response.isready, "ISO state is not ready, it is %r" % template_response.isready

            # Download the Registered ISO
            self.iso.download(self.apiclient, interval=120)

            # Deploy first VM in first account.
            self.firstvm = VirtualMachine.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["virtual_machine"],
                accountid=self.firstaccount.name,
                zoneid=TestMinimalUpgradeChecks.zone.id,
                domainid=self.firstaccount.domainid,
                serviceofferingid=self.service_offering_h1.id,
                templateid=self.template.id,
                diskofferingid=self.disk_offering_5gb.id
            )

            # Add firstvm to Post Upgrade Objects dictionary
            self.firstvm.__dict__.update({'nicip': self.firstvm.nic[0].ipaddress})
            TestMinimalUpgradeChecks.ug_dict.update({'firstvm': self.firstvm.__dict__})

            # Allow SSH Access to the VPN Client VM
            self.firstvm.access_ssh_over_nat(self.apiclient, TestMinimalUpgradeChecks.services, self.firstvm,
                                             allow_egress=True)
            self.debug("VM for VPNClient Access Got Created with Public IP Address %s" % self.firstvm.public_ip)

            # Deploy a second VM in first account.
            self.secondvm = VirtualMachine.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["virtual_machine"],
                accountid=self.firstaccount.name,
                zoneid=TestMinimalUpgradeChecks.zone.id,
                domainid=self.firstaccount.domainid,
                serviceofferingid=self.service_offering_h2.id,
                templateid=self.template.id,
                diskofferingid=self.disk_offering_5gb.id
            )

            # Add secondvm to Post Upgrade Objects dictionary
            self.secondvm.__dict__.update({'nicip': self.secondvm.nic[0].ipaddress})
            TestMinimalUpgradeChecks.ug_dict.update({'secondvm': self.secondvm.__dict__})

            # Allow SSH Access to the VPN Client VM
            self.secondvm.access_ssh_over_nat(self.apiclient, TestMinimalUpgradeChecks.services, self.secondvm,
                                              allow_egress=True)
            self.debug("VM for VPNClient Access Got Created with Public IP Address %s" % self.secondvm.public_ip)

            # Deploy a Third VM in first account.
            self.thirdvm = VirtualMachine.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["virtual_machine"],
                accountid=self.firstaccount.name,
                zoneid=TestMinimalUpgradeChecks.zone.id,
                domainid=self.firstaccount.domainid,
                serviceofferingid=self.service_offering_h1.id,
                templateid=self.iso.id,
                diskofferingid=self.disk_offering_15gb.id
            )

            # Add thirdvm to Post Upgrade Objects dictionary
            self.thirdvm.__dict__.update({'nicip': self.thirdvm.nic[0].ipaddress})
            TestMinimalUpgradeChecks.ug_dict.update({'thirdvm': self.thirdvm.__dict__})

            # Create a Second Account in which we deploy VPCs and test
            # remote access to them from the First Account's isolated Network
            self.secondaccount = Account.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["account"],
                admin=True,
                domainid=TestMinimalUpgradeChecks.domain.id
            )

            # Add second account to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'secondaccount': self.secondaccount.__dict__})

            self.debug("Creating a VPC offering..")

            # Create a VPC Offering that is used to deploy VPCs in Second Account
            self.vpc_offering = VpcOffering.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["vpc_offering"]
            )

            # Add vpc_offering to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'vpc_offering': self.vpc_offering.__dict__})

            # Enable to created VPC Offering inorder to deploy VPCs with it
            self.debug("Enabling the VPC offering created")
            self.vpc_offering.update(self.apiclient, state='Enabled')

            # Create a VPC for the second account
            self.debug("Creating a VPC in the account: %s" % self.secondaccount.name)
            self.firstvpc = VPC.create(
                self.api_client,
                TestMinimalUpgradeChecks.services["vpc"],
                vpcofferingid=self.vpc_offering.id,
                zoneid=TestMinimalUpgradeChecks.zone.id,
                account=self.secondaccount.name,
                domainid=self.secondaccount.domainid
            )

            # Add firstvpc to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'firstvpc': self.firstvpc.__dict__})

            self.debug('Create NetworkOffering for Networks in VPC')
            TestMinimalUpgradeChecks.services["vpc_network_offering"][
                "name"] = "NET_OFF-RemoteAccessVPNTest-" + random_gen()
            self.network_off = NetworkOffering.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["vpc_network_offering"],
                conservemode=False
            )

            # Add network_off to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'network_off': self.network_off.__dict__})

            # Enable Network offering
            self.network_off.update(self.apiclient, state='Enabled')

            self.debug('Created and Enabled NetworkOffering')
            TestMinimalUpgradeChecks.services["network"]["name"] = "NETWORK-" + random_gen()

            # Create First Network Tier in the First VPC created for second
            # account using the network offering created above.
            self.debug('Adding Network=%s' % TestMinimalUpgradeChecks.services["network"])

            self.firstnetworktier = Network.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["network"],
                accountid=self.secondaccount.name,
                domainid=self.secondaccount.domainid,
                networkofferingid=self.network_off.id,
                zoneid=TestMinimalUpgradeChecks.zone.id,
                gateway=TestMinimalUpgradeChecks.services["firstnetwork_tier"]["gateway"],
                vpcid=self.firstvpc.id
            )

            # Add firstnetworktier to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'firstnetworktier': self.firstnetworktier.__dict__})

            self.debug("Created network with ID: %s" % self.firstnetworktier.id)

            # Create Ingress and Egress NetworkACL rules for First Network Tier
            # in the First VPC created for second account.
            self.debug("Adding NetworkACL rules to make Network accessible"
                       " for all Protocols and all CIDRs ")
            NetworkACL.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["all_rule"],
                networkid=self.firstnetworktier.id,
                traffictype='Ingress'
            )

            NetworkACL.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["all_rule"],
                networkid=self.firstnetworktier.id,
                traffictype='Egress'
            )

            listFirstVPC = VPC.list(self.apiclient, id=self.firstvpc.id)
            self.debug(str(listFirstVPC))

            # Obtain the source nat IP Address of the first VPC of second account.
            self.listFirstVPCPublicIpAddress = list_publicIP(self.apiclient, issourcenat="true",
                                                             vpcid=listFirstVPC[0].id, listall="true")
            self.debug(str(self.listFirstVPCPublicIpAddress))

            # Create a VM using the default template on the First Network Tier in the First VPC of the Second Account
            self.vm1 = VirtualMachine.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["virtual_machine"],
                accountid=self.secondaccount.name,
                zoneid=TestMinimalUpgradeChecks.zone.id,
                domainid=self.secondaccount.domainid,
                serviceofferingid=self.service_offering_h1.id,
                templateid=self.template.id,
                networkids=[str(self.firstnetworktier.id)]
            )

            self.debug(" First VM deployed in the first Network Tier")

            # Add vm1 to Post Upgrade Objects dictionary
            self.vm1.__dict__.update({'nicip': self.vm1.nic[0].ipaddress})
            TestMinimalUpgradeChecks.ug_dict.update({'vm1': self.vm1.__dict__})

            # Create another VM using the default template on the
            # First Network Tier in the First VPC of the Second Account
            self.vm2 = VirtualMachine.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["virtual_machine"],
                accountid=self.secondaccount.name,
                zoneid=TestMinimalUpgradeChecks.zone.id,
                domainid=self.secondaccount.domainid,
                serviceofferingid=self.service_offering_h2.id,
                templateid=self.template.id,
                networkids=[str(self.firstnetworktier.id)]
            )

            self.debug(" Second VM deployed in the first Network Tier")

            # Add vm2 to Post Upgrade Objects dictionary
            self.vm2.__dict__.update({'nicip': self.vm2.nic[0].ipaddress})
            TestMinimalUpgradeChecks.ug_dict.update({'vm2': self.vm2.__dict__})

            # Create a random file on First VM's Root Disk
            TestMinimalUpgradeChecks.filecreation(self.firstvm)

            # List ROOT Volume of first VM
            list_root_volumes = list_volumes(
                self.apiclient,
                account=self.firstaccount.name,
                domainid=self.firstaccount.domainid,
                listall='True',
                type='ROOT',
                virtualmachineid=self.firstvm.id
            )

            self.root_volume_firstvm = list_root_volumes[0]

            # Add root_volume_firstvm to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'root_volume_firstvm': self.root_volume_firstvm.__dict__})

            # Create Snapshot of First VM's Root Disk
            self.snapshot_root_firstvm = Snapshot.create(
                self.apiclient,
                volume_id=self.root_volume_firstvm.id,
                account=self.firstaccount.name,
                domainid=self.firstaccount.domainid
            )

            # Add snapshot_root_firstvm to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'snapshot_root_firstvm': self.snapshot_root_firstvm.__dict__})

            # Create a random file on Second VM's Root Disk
            TestMinimalUpgradeChecks.filecreation(self.secondvm)

            # List ROOT Volume of Second VM
            list_root_volumes = list_volumes(
                self.apiclient,
                account=self.firstaccount.name,
                domainid=self.firstaccount.domainid,
                listall='True',
                type='ROOT',
                virtualmachineid=self.secondvm.id
            )

            self.root_volume_secondvm = list_root_volumes[0]

            # Add root_volume_secondvm to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'root_volume_secondvm': self.root_volume_secondvm.__dict__})

            # Create Snapshot of Second VM's Root Disk
            self.snapshot_root_secondvm = Snapshot.create(
                self.apiclient,
                volume_id=self.root_volume_secondvm.id,
                account=self.firstaccount.name,
                domainid=self.firstaccount.domainid
            )

            # Add snapshot_root_secondvm to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'snapshot_root_secondvm': self.snapshot_root_secondvm.__dict__})

            self.debug("Check Type Value:")
            type_dict = self.check_type_value(TestMinimalUpgradeChecks.ug_dict)
            self.debug(type_dict)
            self.debug("Value Types Information is as follows")
            self.debug(type_dict.keys())

            self.debug("Upgrade Dictionary Objects stored in ug_dict are as mentioned below:")
            self.debug(TestMinimalUpgradeChecks.ug_dict)

            self.debug("Refine the Upgrade Dictionary Objects inorder to store them in a Json File")
            refined_dict = self.contruct_dictionary(TestMinimalUpgradeChecks.ug_dict, type_dict)

            with open(TestMinimalUpgradeChecks.services["ug_stp_obj_file"], 'wb') as fp:
                json.dump(refined_dict, fp)

        except Exception as e:
            raise Exception("Warning: Exception during PreUpgrade Test Suite Execution : %s" % e)

        return

    @attr(tags=["postupgrade"])
    def test_02_PostUpgradeTest(self):
        """ Test case no : Verification of CCP Objects Post Upgrade
        """

        # Load the Objects in the JSON File into a dictionary by the name "pullobjects"
        # Validate the following
        # 1. Download the Prior Registered Template
        # 2. Download the Prior Registered ISO
        # 3. Create a new Account by the name Third Account
        # 4. Using Prior Registered Template, Service and Disk Offerings, deploy a VM by
        # the name fourthvm in Third Account
        # 5. Verify the network accessibility of the prior deployed existing firstvm from fourthvm
        # 6. Verify Stop Command on firstvm
        # 7. Verify that the Stopped First VM is no longer accessible
        # 8. Verify Template Creation from Root Volume of the First VM
        # 9. Verify Start Command on firstvm
        # 10. Download the Template Created from Root Volume of the First VM
        # 11. Using Template created from Root Volume of the First VM, Prior Service and Disk Offerings,
        # deploy a VM by the name fifthvm in Third Account
        # 12. Verify whether the Running First VM is accessible after Stopping and Starting it.
        # 13. Reboot the firstVM
        # 14. Verify whether the Running First VM is accessible after Rebooting it.
        # 15. Create Snapshot of First VM's Root Disk

        try:

            with open(TestMinimalUpgradeChecks.services["ug_stp_obj_file"], 'rb') as fp:
                self.pullobjects = json.load(fp)

            # Create Object References for the Objects pulled from the JSON File
            self.service_offering_h1 = ServiceOffering(self.pullobjects['service_offering_h1'])
            self.service_offering_h2 = ServiceOffering(self.pullobjects['service_offering_h2'])
            self.disk_offering_5gb = DiskOffering(self.pullobjects['disk_offering_5gb'])
            self.disk_offering_15gb = DiskOffering(self.pullobjects['disk_offering_15gb'])
            self.firstaccount = Account(self.pullobjects['firstaccount'])
            self.template = Template(self.pullobjects['template'])
            self.iso = Iso(self.pullobjects['iso'])
            self.firstvm = VirtualMachine(self.pullobjects['firstvm'], TestMinimalUpgradeChecks.services)
            self.secondvm = VirtualMachine(self.pullobjects['secondvm'], TestMinimalUpgradeChecks.services)
            self.thirdvm = VirtualMachine(self.pullobjects['thirdvm'], TestMinimalUpgradeChecks.services)
            self.secondaccount = Account(self.pullobjects['secondaccount'])
            self.vpc_offering = VpcOffering(self.pullobjects['vpc_offering'])
            self.firstvpc = VPC(self.pullobjects['firstvpc'])
            self.network_off = NetworkOffering(self.pullobjects['network_off'])
            self.firstnetworktier = Network(self.pullobjects['firstnetworktier'])
            self.vm1 = VirtualMachine(self.pullobjects['vm1'], TestMinimalUpgradeChecks.services)
            self.vm2 = VirtualMachine(self.pullobjects['vm2'], TestMinimalUpgradeChecks.services)
            self.root_volume_firstvm = Volume(self.pullobjects['root_volume_firstvm'])
            self.snapshot_root_firstvm = Snapshot(self.pullobjects['snapshot_root_firstvm'])
            self.root_volume_secondvm = Volume(self.pullobjects['root_volume_secondvm'])
            self.snapshot_root_secondvm = Snapshot(self.pullobjects['snapshot_root_secondvm'])

            ## Download the template registered before Upgrade
            self.template.download(self.apiclient, interval=120)

            ## Download the iso registered before Upgrade
            self.iso.download(self.apiclient, interval=120)

            ## Connect to VMs deployed prior to Upgrade
            self.thirdaccount = Account.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["account"],
                admin=True,
                domainid=TestMinimalUpgradeChecks.domain.id
            )

            ## Deploy a VM in third account using the Template registered before Upgrade
            self.fourthvm = VirtualMachine.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["virtual_machine"],
                accountid=self.thirdaccount.name,
                zoneid=TestMinimalUpgradeChecks.zone.id,
                domainid=self.thirdaccount.domainid,
                serviceofferingid=self.service_offering_h1.id,
                templateid=self.template.id,
                diskofferingid=self.disk_offering_5gb.id
            )

            # Add fourthvm to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update({'fourthvm': self.fourthvm.__dict__})

            # Allow SSH Access to the VPN Client VM
            self.fourthvm.access_ssh_over_nat(self.apiclient, TestMinimalUpgradeChecks.services, self.fourthvm,
                                              allow_egress=True)
            self.debug(
                "Fourth VM that will be used to check SSH Access to other VMs Got "
                "Created with Public IP Address %s" % self.fourthvm.public_ip)

            # Wait for one minute to check for the accessibility of the VM
            self.debug("Wait for one minute to check for the accessibility of the Fourth VM")
            time.sleep(60)

            self.debug("Verify the network accessibility of the firstvm from fourthvm")
            self.vm_accessibility_check(self.fourthvm, self.firstvm)

            self.debug("Verify the network accessibility of the secondvm from fourthvm")
            self.vm_accessibility_check(self.fourthvm, self.secondvm)
            ## Stop first VM
            self.debug("Stopping First VM - ID: %s" % self.firstvm.id)

            self.firstvm.stop(self.apiclient)

            list_vm_response = list_virtual_machines(
                self.apiclient,
                id=self.firstvm.id
            )

            self.assertEqual(
                isinstance(list_vm_response, list),
                True,
                "Check list response returns a valid list"
            )
            self.assertNotEqual(
                len(list_vm_response),
                0,
                "Check First VM available in List Virtual Machines"
            )

            self.assertEqual(
                list_vm_response[0].state,
                "Stopped",
                "Check First virtual machine is in stopped state"
            )

            self.debug("Verify that the Stopped First VM is no longer accessible")

            self.vm_inaccessibility_check(self.fourthvm, self.firstvm)

            ## Stop second VM
            self.debug("Stopping Second VM - ID: %s" % self.secondvm.id)
            self.secondvm.stop(self.apiclient)

            list_vm_response = list_virtual_machines(
                self.apiclient,
                id=self.secondvm.id
            )

            self.assertEqual(
                isinstance(list_vm_response, list),
                True,
                "Check list response returns a valid list"
            )
            self.assertNotEqual(
                len(list_vm_response),
                0,
                "Check Second VM available in List Virtual Machines"
            )

            self.assertEqual(
                list_vm_response[0].state,
                "Stopped",
                "Check Second virtual machine is in stopped state"
            )

            self.debug("Verify that the Stopped Second VM is no longer accessible")
            self.vm_inaccessibility_check(self.fourthvm, self.secondvm)

            ## Create a Template from Root Volume of the First VM
            self.debug("Create Template from Root Volume of the First VM")
            self.template_from_root_volume_firstvm = Template.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["template_from_root_volume"],
                volumeid=self.root_volume_firstvm.id,
                account=self.firstaccount.name,
                domainid=self.firstaccount.domainid
            )

            ## Create a Template from Root Volume of the Second VM
            self.debug("Create Template from Root Volume of the Second VM")
            self.template_from_root_volume_secondvm = Template.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["template_from_root_volume"],
                volumeid=self.root_volume_secondvm.id,
                account=self.firstaccount.name,
                domainid=self.firstaccount.domainid
            )

            ## Start the Stopped First VM
            self.debug(
                "Deploy Fifth VM in third account using the Template created from the ROOT Volume of the First VM")

            self.firstvm.start(self.apiclient)

            ## Start the Stopped Second VM
            self.debug("Starting Second VM - ID: %s" % self.secondvm.id)
            self.secondvm.start(self.apiclient)

            # Wait for template status to be changed across
            time.sleep(TestMinimalUpgradeChecks.services["sleep"])
            timeout = TestMinimalUpgradeChecks.services["timeout"]

            # Verify Whether the Template From ROOT Volume of First VM is Ready
            self.debug("Verify Whether the Template From ROOT Volume of First VM is Ready")

            while True:
                list_template_response = list_templates(
                    self.apiclient,
                    templatefilter='all',
                    id=self.template_from_root_volume_firstvm.id,
                    zoneid=TestMinimalUpgradeChecks.zone.id,
                    account=self.firstaccount.name,
                    domainid=TestMinimalUpgradeChecks.domain.id
                )

                if isinstance(list_template_response, list):
                    break
                elif timeout == 0:
                    raise Exception("List template failed!")

                time.sleep(5)
                timeout = timeout - 1

                #Verify template response to check whether template added successfully
                assert isinstance(list_template_response, list), "Check for list template " \
                                                                 "response return valid data"

                assert len(
                    list_template_response) != 0, "Check Template From ROOT Volume " \
                                                  "of First VM available in List Templates"

                template_response = list_template_response[0]

                assert template_response.isready, "Template From ROOT Volume of " \
                                                  "First VM state is not ready, it is %r" % \
                                                  template_response.isready

                if isinstance(list_template_response, list):
                    break
                elif timeout == 0:
                    raise Exception("List template failed!")

                time.sleep(5)
                timeout = timeout - 1

            #Verify template response to check whether template added successfully
            assert isinstance(list_template_response, list), "Check for list template response return valid data"

            assert len(
                list_template_response) != 0, "Check Template From ROOT Volume of Second VM available in List Templates"

            template_response = list_template_response[0]

            assert template_response.isready, "Template From ROOT Volume of Second " \
                                              "VM state is not ready, it is %r" % template_response.isready

            # Download the created Template From ROOT Volume of First VM
            self.debug("Download the created Template From ROOT Volume of First VM")
            self.template_from_root_volume_firstvm.download(self.apiclient, interval=120)

            # Download the created Template From ROOT Volume of Second VM
            self.debug("Download the created Template From ROOT Volume of Second VM")
            self.template_from_root_volume_secondvm.download(self.apiclient, interval=120)

            ## Deploy a VM in third account using the Template created from the ROOT Volume of the First VM
            self.debug(
                "Deploy Fifth VM in third account using the Template created from the ROOT Volume of the First VM")

            self.fifthvm = VirtualMachine.create(
                self.apiclient,
                TestMinimalUpgradeChecks.services["virtual_machine"],
                accountid=self.thirdaccount.name,
                zoneid=TestMinimalUpgradeChecks.zone.id,
                domainid=self.thirdaccount.domainid,
                serviceofferingid=self.service_offering_h2.id,
                templateid=self.template_from_root_volume_firstvm.id,
                diskofferingid=self.disk_offering_15gb.id
            )

            ## Verify whether the started First VM is in "Running" State
            self.debug("Verify whether the started First VM is in 'Running' State")
            list_vm_response = list_virtual_machines(
                self.apiclient,
                id=self.firstvm.id
            )

            self.assertEqual(
                isinstance(list_vm_response, list),
                True,
                "Check list response returns a valid list"
            )

            self.assertNotEqual(
                len(list_vm_response),
                0,
                "Check First VM avaliable in List Virtual Machines"
            )

            self.debug(
                "Verify listVirtualMachines response for First virtual machine: %s"
                % self.firstvm.id
            )

            self.assertEqual(
                list_vm_response[0].state,
                "Running",
                "Check First virtual machine is in running state"
            )

            ## Verify whether the started Second VM is in "Running" State
            self.debug("Verify whether the started Second VM is in 'Running' State")
            list_vm_response = list_virtual_machines(
                self.apiclient,
                id=self.secondvm.id
            )

            self.assertEqual(
                isinstance(list_vm_response, list),
                True,
                "Check list response returns a valid list"
            )

            self.assertNotEqual(
                len(list_vm_response),
                0,
                "Check Second VM avaliable in List Virtual Machines"
            )

            self.debug(
                "Verify listVirtualMachines response for Second virtual machine: %s"
                % self.secondvm.id
            )

            self.assertEqual(
                list_vm_response[0].state,
                "Running",
                "Check Second virtual machine is in running state"
            )

            ## Verify whether the Running First VM is accessible after Stopping and Starting it.
            self.debug("Verify whether the Running First VM is accessible after Stopping and Starting it.")

            # Wait for one minute to check for the accessibility of the First VM
            self.debug("Wait for one minute to check for the accessibility of the First VM")
            time.sleep(60)

            self.vm_accessibility_check(self.fourthvm, self.firstvm, first_time=False)

            ## Verify whether the Running Second VM is accessible after Stopping and Starting it.
            self.debug("Verify whether the Running Second VM is accessible after Stopping and Starting it.")

            # Wait for one minute to check for the accessibility of the Second VM
            self.debug("Wait for one minute to check for the accessibility of the Second VM")
            time.sleep(60)

            self.vm_accessibility_check(self.fourthvm, self.secondvm, first_time=False)

            ## Reboot the First VM
            self.debug("Reboot the Running First VM")
            self.firstvm.reboot(self.apiclient)

            ## Verify whether the rebooted First VM is in "Running" State
            list_vm_response = list_virtual_machines(
                self.apiclient,
                id=self.firstvm.id
            )

            self.assertEqual(
                isinstance(list_vm_response, list),
                True,
                "Check list response returns a valid list"
            )

            self.assertNotEqual(
                len(list_vm_response),
                0,
                "Check First VM avaliable in List Virtual Machines"
            )

            self.debug(
                "Verify listVirtualMachines response for First virtual machine: %s"
                % self.firstvm.id
            )

            self.assertEqual(
                list_vm_response[0].state,
                "Running",
                "Check First virtual machine is in running state"
            )
            ## Reboot the Second VM
            self.debug("Reboot the Running Second VM")
            self.secondvm.reboot(self.apiclient)

            ## Verify whether the rebooted Second VM is in "Running" State
            self.debug("Verify whether the rebooted Second VM is in 'Running' State")
            list_vm_response = list_virtual_machines(
                self.apiclient,
                id=self.secondvm.id
            )

            self.assertEqual(
                isinstance(list_vm_response, list),
                True,
                "Check list response returns a valid list"
            )

            self.assertNotEqual(
                len(list_vm_response),
                0,
                "Check Second VM avaliable in List Virtual Machines"
            )

            self.debug(
                "Verify listVirtualMachines response for Second virtual machine: %s"
                % self.secondvm.id
            )

            self.assertEqual(
                list_vm_response[0].state,
                "Running",
                "Check Second virtual machine is in running state"
            )
            ## Verify whether the Running First VM is accessible after Rebooting it.
            self.debug("Verify whether the Running First VM is accessible after Rebooting it.")

            # Wait for one minute to check for the accessibility of the VM
            self.debug("Wait for one minute to check for the accessibility of the First VM")
            time.sleep(60)

            self.vm_accessibility_check(self.fourthvm, self.firstvm, first_time=False)

            ## Verify whether the Running Second VM is accessible after Rebooting it.
            self.debug("Verify whether the Running Second VM is accessible after Rebooting it.")

            # Wait for one minute to check for the accessibility of the VM
            self.debug("Wait for one minute to check for the accessibility of the Second VM")
            time.sleep(60)

            self.vm_accessibility_check(self.fourthvm, self.secondvm, first_time=False)

            self.debug("Create Snapshot of First VM's Root Disk")
            # Create Snapshot of First VM's Root Disk
            self.snapshot_root_firstvm_afterug = Snapshot.create(
                self.apiclient,
                volume_id=self.root_volume_firstvm.id,
                account=self.firstaccount.name,
                domainid=self.firstaccount.domainid
            )

            # Add snapshot_root_firstvm to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update(
                {'snapshot_root_firstvm_afterug': self.snapshot_root_firstvm_afterug.__dict__})

            self.debug("Create Snapshot of Second VM's Root Disk")
            # Create Snapshot of Second VM's Root Disk
            self.snapshot_root_secondvm_afterug = Snapshot.create(
                self.apiclient,
                volume_id=self.root_volume_secondvm.id,
                account=self.firstaccount.name,
                domainid=self.firstaccount.domainid
            )

            # Add snapshot_root_secondvm to Post Upgrade Objects dictionary
            TestMinimalUpgradeChecks.ug_dict.update(
                {'snapshot_root_secondvm_afterug': self.snapshot_root_secondvm_afterug.__dict__})

            # Add pullobjects in order to register the updated object values to the json file
            TestMinimalUpgradeChecks.ug_dict.update(self.pullobjects)

            self.debug("Check Type Value:")
            type_dict = self.check_type_value(TestMinimalUpgradeChecks.ug_dict)
            self.debug(type_dict)
            self.debug("Value Types Information is as follows")
            self.debug(type_dict.keys())

            self.debug("Upgrade Dictionary Objects stored in ug_dict are as mentioned below:")
            self.debug(TestMinimalUpgradeChecks.ug_dict)

            self.debug("Refine the Upgrade Dictionary Objects inorder to store them in a Json File")
            refined_dict = self.contruct_dictionary(TestMinimalUpgradeChecks.ug_dict, type_dict)

            self.debug("Re-Use the Objects for Future Validations")
            with open(TestMinimalUpgradeChecks.services["ug_stp_obj_file"], 'wb') as fp:
                json.dump(refined_dict, fp)

        except Exception as e:
            raise Exception("Warning: Exception during PreUpgrade Test Suite Execution : %s" % e)

        return




