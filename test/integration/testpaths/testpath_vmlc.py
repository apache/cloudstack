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
""" Test Path for VM Life Cycle (VMLC)
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Template,
                             User,
                             Network,
                             PublicIPAddress,
                             NATRule,
                             FireWallRule,
                             VPC,
                             VpcOffering,
                             SecurityGroup,
                             NetworkACL,
                             LoadBalancerRule)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_builtin_template_info,
                               findSuitableHostForMigration,
                               createEnabledNetworkOffering,
                               setSharedNetworkParams,
                               get_free_vlan)
from marvin.codes import (PASS,
                          ISOLATED_NETWORK,
                          SHARED_NETWORK,
                          VPC_NETWORK)
from marvin.sshClient import SshClient
from ddt import ddt, data


def VerifyChangeInServiceOffering(self, virtualmachine, serviceoffering):
    """List the VM and verify that the new values for cpuspeed,
       cpunumber and memory match with the new service offering"""

    vmlist = VirtualMachine.list(self.userapiclient, id=virtualmachine.id)
    self.assertEqual(
        validateList(vmlist)[0],
        PASS,
        "vm list validation failed")
    vm = vmlist[0]

    # Verify the custom values
    self.assertEqual(str(vm.cpunumber), str(serviceoffering.cpunumber),
                     "vm cpu number %s not matching with cpu number in\
                      service offering %s" %
                     (vm.cpunumber, serviceoffering.cpunumber))

    self.assertEqual(str(vm.cpuspeed), str(serviceoffering.cpuspeed),
                     "vm cpu speed %s not matching with cpu speed in\
                     service offering %s" %
                     (vm.cpuspeed, serviceoffering.cpuspeed))

    self.assertEqual(str(vm.memory), str(serviceoffering.memory),
                     "vm memory %s not matching with memory in\
                     service offering %s" %
                     (vm.memory, serviceoffering.memory))
    return


def CreateNetwork(self, networktype):
    """Create a network of given type (isolated/shared/isolated in VPC)"""

    network = None

    if networktype == ISOLATED_NETWORK:
        try:
            network = Network.create(
                self.apiclient, self.testdata["isolated_network"],
                networkofferingid=self.isolated_network_offering.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                zoneid=self.zone.id)
            self.cleanup.append(network)
        except Exception as e:
            self.fail("Isolated network creation failed because: %s" % e)

    elif networktype == SHARED_NETWORK:
        physical_network, vlan = get_free_vlan(self.apiclient, self.zone.id)

        # create network using the shared network offering created
        self.testdata["shared_network"]["acltype"] = "domain"
        self.testdata["shared_network"]["vlan"] = vlan
        self.testdata["shared_network"]["networkofferingid"] = \
            self.shared_network_offering.id
        self.testdata["shared_network"]["physicalnetworkid"] = \
            physical_network.id

        self.testdata["shared_network"] = \
            setSharedNetworkParams(self.testdata["shared_network"])

        try:
            network = Network.create(
                self.apiclient,
                self.testdata["shared_network"],
                networkofferingid=self.shared_network_offering.id,
                zoneid=self.zone.id)
            self.cleanup.append(network)
        except Exception as e:
            self.fail("Shared Network creation failed because: %s" % e)

    elif networktype == VPC_NETWORK:
        self.testdata["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        vpc = VPC.create(self.apiclient,
                         self.testdata["vpc"],
                         vpcofferingid=self.vpc_off.id,
                         zoneid=self.zone.id,
                         account=self.account.name,
                         domainid=self.account.domainid
                         )
        self.cleanup.append(vpc)
        self.vpcid = vpc.id
        vpcs = VPC.list(self.apiclient, id=vpc.id)
        self.assertEqual(
            validateList(vpcs)[0], PASS,
            "VPC list validation failed, vpc list is %s" % vpcs
        )

        network = Network.create(
            self.apiclient,
            self.testdata["isolated_network"],
            networkofferingid=self.isolated_network_offering_vpc.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            vpcid=vpc.id,
            gateway="10.1.1.1",
            netmask="255.255.255.0")
        self.cleanup.append(network)
    return network


def CreateEnabledNetworkOffering(apiclient, networkServices):
    """Create network offering of given services and enable it"""

    result = createEnabledNetworkOffering(apiclient, networkServices)
    assert result[0] == PASS,\
        "Network offering creation/enabling failed due to %s" % result[2]
    return result[1]


@ddt
class TestPathVMLC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestPathVMLC, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls._cleanup = []

        try:
            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.testdata["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)

            # If local storage is enabled, alter the offerings to use
            # localstorage
            if cls.zone.localstorageenable:
                cls.testdata["service_offering"]["storagetype"] = 'local'

            # Create 3 service offerings with different values for
            # for cpunumber, cpuspeed, and memory

            cls.testdata["service_offering"]["cpuspeed"] = 128
            cls.testdata["service_offering"]["memory"] = 256

            cls.testdata["service_offering"]["cpunumber"] = 1
            if cls.hypervisor.lower() == "hyperv":
                cls.testdata["service_offering"]["cpuspeed"] = 2048
                cls.testdata["service_offering"]["memory"] = 2048

            cls.service_offering_1 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"]
            )
            cls._cleanup.append(cls.service_offering_1)

            cls.testdata["service_offering"]["cpunumber"] = 2
            cls.service_offering_2 = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offering"]
            )
            cls._cleanup.append(cls.service_offering_2)

            # Create isolated network offering
            cls.isolated_network_offering = CreateEnabledNetworkOffering(
                cls.apiclient,
                cls.testdata["isolated_network_offering"]
            )
            cls._cleanup.append(cls.isolated_network_offering)

            # Create shared network offering
            cls.testdata["shared_network_offering_all_services"][
                "specifyVlan"] = "True"
            cls.testdata["shared_network_offering_all_services"][
                "specifyIpRanges"] = "True"

            cls.shared_network_offering = CreateEnabledNetworkOffering(
                cls.apiclient,
                cls.testdata["shared_network_offering_all_services"]
            )
            cls._cleanup.append(cls.shared_network_offering)

            cls.isolated_network_offering_vpc = CreateEnabledNetworkOffering(
                cls.apiclient,
                cls.testdata["nw_offering_isolated_vpc"]
            )
            cls._cleanup.append(cls.isolated_network_offering_vpc)
            cls.vpc_off = VpcOffering.create(cls.apiclient,
                                             cls.testdata["vpc_offering"]
                                             )
            cls.vpc_off.update(cls.apiclient, state='Enabled')
            cls._cleanup.append(cls.vpc_off)

            # This variable will store the id of vpc network whenever
            # test case creates it
            # If not created, it will be None and will not be used
            cls.vpcid = None

            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )

            # Register a private template in the account
            builtin_info = get_builtin_template_info(cls.apiclient,
                                                     cls.zone.id)

            cls.testdata["privatetemplate"]["url"] = builtin_info[0]
            cls.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
            cls.testdata["privatetemplate"]["format"] = builtin_info[2]

            # Register new template
            cls.template = Template.register(
                cls.userapiclient,
                cls.testdata["privatetemplate"],
                zoneid=cls.zone.id,
                account=cls.account.name,
                domainid=cls.account.domainid
            )

            # Wait for template to download
            cls.template.download(cls.apiclient)

            # Check that we are able to login to the created account
            respose = User.login(
                cls.apiclient,
                username=cls.account.name,
                password=cls.testdata["account"]["password"]
            )

            assert respose.sessionkey is not None,\
                "Login to the CloudStack should be successful\
                            response shall have non Null key"

        except Exception as e:
            cls.tearDownClass()
            raise e
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        # Cleanup VM before proceeding the cleanup as networks will be
        # cleaned up properly, continue if VM deletion fails,
        # because in that case VM is already deleted from the test case
        # try:
        #     self.virtual_machine.delete(self.apiclient, expunge=True)
        # except Exception:
        #     self.debug("Exception while destroying VM")
        try:
            self.cleanup = self.cleanup[::-1]
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="True")
    @data(ISOLATED_NETWORK, VPC_NETWORK)
    def test_01_positive_tests_vm_operations_advanced_zone(self, value):
        """ Positive tests for VMLC test path - Advanced Zone

        # 1.  List created service offering in setUpClass by name
        # 2.  List registered template with name
        # 3.  Create VM in account
        # 4.  Enable networking for reaching to VM thorugh SSH
        # 5.  Check VM accessibility through SSH
        # 6.  Stop vm and verify vm is not accessible
        # 7.  Start vm and verify vm is not accessible
        # 8.  Reboot vm and verify vm is not accessible
        # 9.  Destroy and recover VM
        # 10. Change service offering of VM to a different service offering
        # 11. Verify that the cpuspeed, cpunumber and memory of VM matches to
        #     as specified in new service offering
        # 12. Start VM and verify VM accessibility
        # 13. Find suitable host for VM to migrate and migrate the VM
        # 14. Verify VM accessibility on new host
        """
        if self.hypervisor.lower() in ['hyperv', 'lxc']  and value == VPC_NETWORK:
            self.skipTest("cann't be run for {} hypervisor".format(self.hypervisor))

        # List created service offering in setUpClass by name
        listServiceOfferings = ServiceOffering.list(
            self.apiclient,
            name=self.service_offering_1.name,
            listall=True
        )
        self.assertEqual(validateList(listServiceOfferings)[0], PASS,
                         "List validation failed for service offerings list")

        self.assertEqual(listServiceOfferings[0].name,
                         self.service_offering_1.name,
                         "Names of created service offering\
                         and listed service offering not matching")

        # List registered template with name
        listTemplates = Template.list(
            self.userapiclient,
            templatefilter="self",
            name=self.template.name,
            listall=True,
            zone=self.zone.id)
        self.assertEqual(validateList(listTemplates)[0], PASS,
                         "List validation failed for templates list")

        self.assertEqual(listTemplates[0].name, self.template.name,
                         "Names of created template and listed template\
                         not matching")

        network = CreateNetwork(self, value)

        # Create VM in account
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            networkids=[network.id, ],
            zoneid=self.zone.id
        )
        self.cleanup.append(self.virtual_machine)
        publicip = PublicIPAddress.create(
            self.userapiclient, accountid=self.account.name,
            zoneid=self.zone.id, domainid=self.account.domainid,
            networkid=network.id, vpcid=self.vpcid
        )

        if value == VPC_NETWORK:
            lb_rule = LoadBalancerRule.create(
                self.apiclient,
                self.testdata["vpclbrule"],
                ipaddressid=publicip.ipaddress.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkid=network.id,
                vpcid=self.vpcid
            )
            lb_rule.assign(self.apiclient, [self.virtual_machine])

            # Opening up the ports in VPC
            NetworkACL.create(
                self.apiclient,
                networkid=network.id,
                services=self.testdata["natrule"],
                traffictype='Ingress'
            )
        elif value == ISOLATED_NETWORK:
            FireWallRule.create(
                self.userapiclient,
                ipaddressid=publicip.ipaddress.id,
                protocol='TCP',
                cidrlist=[self.testdata["fwrule"]["cidr"]],
                startport=self.testdata["fwrule"]["startport"],
                endport=self.testdata["fwrule"]["endport"]
            )

            NATRule.create(
                self.userapiclient,
                self.virtual_machine,
                self.testdata["natrule"],
                ipaddressid=publicip.ipaddress.id,
                networkid=network.id
            )

        # Check VM accessibility
        try:
            SshClient(host=publicip.ipaddress.ipaddress,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)

        # Stop VM and verify VM is not accessible
        self.virtual_machine.stop(self.userapiclient)

        with self.assertRaises(Exception):
            SshClient(host=publicip.ipaddress.ipaddress,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password,
                      retries=0)

        # Start VM and verify that it is accessible
        self.virtual_machine.start(self.userapiclient)

        try:
            SshClient(host=publicip.ipaddress.ipaddress,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)

        # Reboot VM and verify that it is accessible
        self.virtual_machine.reboot(self.userapiclient)

        try:
            SshClient(host=publicip.ipaddress.ipaddress,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)

        # Destroy and recover VM
        self.virtual_machine.delete(self.apiclient, expunge=False)
        self.virtual_machine.recover(self.apiclient)

        # Change service offering of VM and verify that it is changed
        self.virtual_machine.change_service_offering(
            self.userapiclient,
            serviceOfferingId=self.service_offering_2.id
        )

        VerifyChangeInServiceOffering(self,
                                      self.virtual_machine,
                                      self.service_offering_2)

        # Start VM and verify that it is accessible
        self.virtual_machine.start(self.userapiclient)

        try:
            SshClient(host=publicip.ipaddress.ipaddress,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)


        if not self.hypervisor.lower() == "lxc":
            # Find suitable host for VM to migrate and migrate the VM
            # Verify that it is accessible on the new host
            host = findSuitableHostForMigration(self.apiclient,
                                            self.virtual_machine.id)
            if host is not None:
                self.virtual_machine.migrate(self.apiclient, host.id)

                try:
                    SshClient(host=publicip.ipaddress.ipaddress,
                        port=22,
                        user=self.virtual_machine.username,
                        passwd=self.virtual_machine.password)
                except Exception as e:
                    self.fail("Exception while SSHing to VM: %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="True")
    def test_01_positive_tests_vm_deploy_shared_nw(self):
        """ Positive tests for VMLC test path - Advanced Zone in Shared Network

        # 1.  List created service offering in setUpClass by name
        # 2.  List registered template with name
        # 3.  Create VM in account
        """

        # List created service offering in setUpClass by name
        listServiceOfferings = ServiceOffering.list(
            self.apiclient,
            name=self.service_offering_1.name,
            listall=True
        )
        self.assertEqual(validateList(listServiceOfferings)[0], PASS,
                         "List validation failed for service offerings list")

        self.assertEqual(listServiceOfferings[0].name,
                         self.service_offering_1.name,
                         "Names of created service offering\
                         and listed service offering not matching")

        # List registered template with name
        listTemplates = Template.list(
            self.userapiclient,
            templatefilter="self",
            name=self.template.name,
            listall=True,
            zone=self.zone.id)
        self.assertEqual(validateList(listTemplates)[0], PASS,
                         "List validation failed for templates list")

        self.assertEqual(listTemplates[0].name, self.template.name,
                         "Names of created template and listed template\
                         not matching")

        network = CreateNetwork(self, SHARED_NETWORK)

        # Create VM in account
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            networkids=[network.id, ],
            zoneid=self.zone.id
        )
        self.cleanup.append(self.virtual_machine)
        return

    @attr(tags=["basic"], required_hardware="True")
    def test_01_positive_tests_vm_operations_basic_zone(self):
        """ Positive tests for VMLC test path - Basic Zone

        # 1.  List created service offering in setUpClass by name
        # 2.  List registered template with name
        # 3.  Create VM in account
        # 4.  Enable networking for reaching to VM thorugh SSH
        # 5.  Check VM accessibility through SSH
        # 6.  Stop vm and verify vm is not accessible
        # 7.  Start vm and verify vm is not accessible
        # 8.  Reboot vm and verify vm is not accessible
        # 9.  Destroy and recover VM
        # 10. Change service offering of VM to a different service offering
        # 11. Verify that the cpuspeed, cpunumber and memory of VM matches to
        #     as specified in new service offering
            # 12. Start VM and verify VM accessibility
        # 13. Find suitable host for VM to migrate and migrate the VM
        # 14. Verify VM accessibility on new host
        """

        # List created service offering in setUpClass by name
        listServiceOfferings = ServiceOffering.list(
            self.apiclient,
            name=self.service_offering_1.name,
            listall=True
        )
        self.assertEqual(validateList(listServiceOfferings)[0], PASS,
                         "List validation failed for service offerings list")
        self.assertEqual(listServiceOfferings[0].name,
                         self.service_offering_1.name,
                         "Names of created service offering and\
                         listed service offering not matching")

        # List registered template with name
        listTemplates = Template.list(self.userapiclient,
                                      templatefilter="self",
                                      name=self.template.name,
                                      listall=True,
                                      zone=self.zone.id
                                      )

        self.assertEqual(validateList(listTemplates)[0], PASS,
                         "List validation failed for\
                         templates list")

        self.assertEqual(listTemplates[0].name, self.template.name,
                         "Names of created template and listed template\
                         not matching")

        # Enable networking for reaching to VM thorugh SSH
        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(security_group)
        # Authorize Security group to SSH to VM
        security_group.authorize(
            self.apiclient,
            self.testdata["ingress_rule"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        # Create VM in account
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            zoneid=self.zone.id,
            securitygroupids=[security_group.id, ]
        )
        self.cleanup.append(self.virtual_machine)
        # Check VM accessibility
        try:
            SshClient(host=self.virtual_machine.ssh_ip,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)

        # Stop VM and verify VM is not accessible
        self.virtual_machine.stop(self.userapiclient)

        with self.assertRaises(Exception):
            SshClient(host=self.virtual_machine.ssh_ip,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password,
                      retries=0
                      )

        # Start VM and verify that it is accessible
        self.virtual_machine.start(self.userapiclient)

        try:
            SshClient(host=self.virtual_machine.ssh_ip,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)

        # Reboot VM and verify that it is accessible
        self.virtual_machine.reboot(self.userapiclient)

        try:
            SshClient(host=self.virtual_machine.ssh_ip,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)

        # Destroy and recover VM
        self.virtual_machine.delete(self.userapiclient, expunge=False)
        self.virtual_machine.recover(self.apiclient)

        # Change service offering of VM and verify that it is changed
        self.virtual_machine.change_service_offering(
            self.userapiclient,
            serviceOfferingId=self.service_offering_2.id
        )

        VerifyChangeInServiceOffering(self,
                                      self.virtual_machine,
                                      self.service_offering_2)

        # Start VM and verify that it is accessible
        self.virtual_machine.start(self.userapiclient)

        try:
            SshClient(host=self.virtual_machine.ssh_ip,
                      port=22,
                      user=self.virtual_machine.username,
                      passwd=self.virtual_machine.password)
        except Exception as e:
            self.fail("Exception while SSHing to VM: %s" % e)

        if not self.hypervisor.lower() == "lxc":
            # Find suitable host for VM to migrate and migrate the VM
            # Verify that it is accessible on the new host
            host = findSuitableHostForMigration(self.apiclient,
                                            self.virtual_machine.id)
            if host is not None:
                self.virtual_machine.migrate(self.apiclient, host.id)

                try:
                    SshClient(host=self.virtual_machine.ssh_ip,
                        port=22,
                        user=self.virtual_machine.username,
                        passwd=self.virtual_machine.password)
                except Exception as e:
                    self.fail("Exception while SSHing to VM: %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="True")
    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    def test_02_negative_tests_destroy_VM_operations_advanced_zone(
            self,
            value):
        """ Negative tests for VMLC test path - destroy VM

        # 1. Deploy a VM in the account
        # 2. Stop VM and try to reboot it, operation should fail
        # 3. Destroy VM and try to start the VM in destroyed state,
        #    operation should fail
        # 4. Try to stop the VM in destroyed state, operation should fail
        # 5. Try to reboot the VM in destroyed state, operation should fail
        """
        if self.hypervisor.lower() in ['hyperv', 'lxc'] and value == VPC_NETWORK:
            self.skipTest("cann't be run for {} hypervisor".format(self.hypervisor))
        network = CreateNetwork(self, value)
        networkid = network.id

        # Deploy a VM
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            networkids=[networkid, ],
            zoneid=self.zone.id
        )
        self.cleanup.append(self.virtual_machine)
        # Stop the VM and try to reboot it, it should fail
        self.virtual_machine.stop(self.userapiclient)
        with self.assertRaises(Exception):
            self.virtual_machine.reboot(self.userapiclient)

        # Destroy the VM and try to reboot it, it should fail
        self.virtual_machine.delete(self.userapiclient, expunge=False)

        # try to start VM in destroyed state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.start(self.userapiclient)

        # try to stop VM in destroyed state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.stop(self.userapiclient)

        # try to reboot VM in destroyed state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.reboot(self.userapiclient)

        return

    @attr(tags=["basic"], required_hardware="True")
    def test_02_negative_tests_destroy_VM_operations_basic_zone(self):
        """ Negative tests for VMLC test path - destroy VM

        # 1. Deploy a VM in the account
        # 2. Stop VM and try to reboot it, operation should fail
        # 3. Destroy VM and try to start the VM in destroyed state,
        #    operation should fail
        # 4. Try to stop the VM in destroyed state, operation should fail
        # 5. Try to reboot the VM in destroyed state, operation should fail
        """
        # Deploy a VM
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.virtual_machine)
        # Stop the VM and try to reboot it, it should fail
        self.virtual_machine.stop(self.userapiclient)
        with self.assertRaises(Exception):
            self.virtual_machine.reboot(self.userapiclient)

        # Destroy the VM and try to reboot it, it should fail
        self.virtual_machine.delete(self.userapiclient, expunge=False)

        # try to start VM in destroyed state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.start(self.userapiclient)

        # try to stop VM in destroyed state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.stop(self.userapiclient)

        # try to reboot VM in destroyed state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.reboot(self.userapiclient)

        return

    @attr(tags=["advanced"], required_hardware="True")
    @data(ISOLATED_NETWORK, SHARED_NETWORK, VPC_NETWORK)
    def test_03_negative_tests_expunge_VM_operations_advanced_zone(
            self,
            value):
        """ Negative tests for VMLC test path - expunge VM

        # 1. Deploy a VM in the account
        # 2. Destroy the VM with expunge=True
        # 3. Try to start the VM in expunging state, operation should fail
        # 4. Try to stop the VM in expunging state, operation should fail
        # 5. Try to reboot the VM in expunging state, operation should fail
        # 6. Try to destroy the VM in expunging state, operation should fail
        # 7. Try to recover the VM in expunging state, operation should fail
        """

        if self.hypervisor.lower() in ['hyperv', 'lxc'] and value == VPC_NETWORK:
            self.skipTest("cann't be run for {} hypervisor".format(self.hypervisor))
        network = CreateNetwork(self, value)
        networkid = network.id

        # Deploy a VM in the account
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            networkids=[networkid, ],
            zoneid=self.zone.id
        )

        # Destroy a VM with expunge flag True
        self.virtual_machine.delete(self.apiclient, expunge=True)

        # try to start VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.start(self.userapiclient)

        # try to stop VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.stop(self.userapiclient)

        # try to reboot VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.reboot(self.userapiclient)

        # try to destroy VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.delete(self.userapiclient, expunge=False)

        # try to recover VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.recover(self.apiclient)

        return

    @attr(tags=["basic"], required_hardware="True")
    def test_03_negative_tests_expunge_VM_operations_basic_zone(self):
        """ Negative tests for VMLC test path - expunge VM

        # 1. Deploy a VM in the account
        # 2. Destroy the VM with expunge=True
        # 3. Try to start the VM in expunging state, operation should fail
        # 4. Try to stop the VM in expunging state, operation should fail
        # 5. Try to reboot the VM in expunging state, operation should fail
        # 6. Try to destroy the VM in expunging state, operation should fail
        # 7. Try to recover the VM in expunging state, operation should fail
        """
        # Deploy a VM in the account
        self.virtual_machine = VirtualMachine.create(
            self.userapiclient,
            self.testdata["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_1.id,
            zoneid=self.zone.id
        )

        # Destroy a VM with expunge flag True
        self.virtual_machine.delete(self.apiclient, expunge=True)

        # try to start VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.start(self.userapiclient)

        # try to stop VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.stop(self.userapiclient)

        # try to reboot VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.reboot(self.userapiclient)

        # try to destroy VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.delete(self.userapiclient, expunge=False)

        # try to recover VM in expunging state, it should fail
        with self.assertRaises(Exception):
            self.virtual_machine.recover(self.apiclient)

        return
