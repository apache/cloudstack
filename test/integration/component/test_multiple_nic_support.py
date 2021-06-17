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
""" tests for supporting multiple NIC's in advanced zone with security groups in cloudstack 4.14.0.0

"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.sshClient import SshClient
from marvin.lib.utils import (validateList,
                              cleanup_resources,
                              get_host_credentials,
                              get_process_status,
                              execute_command_in_host,
                              random_gen)
from marvin.lib.base import (PhysicalNetwork,
                             Account,
                             Host,
                             TrafficType,
                             Domain,
                             Network,
                             NetworkOffering,
                             VirtualMachine,
                             ServiceOffering,
                             Zone,
                             NIC,
                             SecurityGroup)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_virtual_machines,
                               list_routers,
                               list_hosts,
                               get_free_vlan)
from marvin.codes import (PASS, FAILED)
import logging
import random
import time

class TestMulipleNicSupport(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestMulipleNicSupport,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.services = cls.testClient.getParsedTestDataConfig()
        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls._cleanup = []

        cls.skip = False
        if str(cls.zone.securitygroupsenabled) != "True":
            cls.skip = True
            return

        cls.logger = logging.getLogger("TestMulipleNicSupport")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.services['mode'] = cls.zone.networktype

        cls.template = get_template(cls.apiclient, cls.zone.id, hypervisor="KVM")
        if cls.template == FAILED:
            cls.skip = True
            return

        # Create new domain, account, network and VM
        cls.user_domain = Domain.create(
            cls.apiclient,
            services=cls.testdata["acl"]["domain2"],
            parentdomainid=cls.domain.id)

        # Create account
        cls.account1 = Account.create(
            cls.apiclient,
            cls.testdata["acl"]["accountD2"],
            admin=True,
            domainid=cls.user_domain.id
        )

        # Create small service offering
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offerings"]["small"]
        )

        cls._cleanup.append(cls.service_offering)
        cls.services["network"]["zoneid"] = cls.zone.id
        cls.network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.services["network_offering"],
        )
        # Enable Network offering
        cls.network_offering.update(cls.apiclient, state='Enabled')

        cls._cleanup.append(cls.network_offering)
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = cls.template.id

        if cls.zone.securitygroupsenabled:
            # Enable networking for reaching to VM thorugh SSH
            security_group = SecurityGroup.create(
                cls.apiclient,
                cls.testdata["security_group"],
                account=cls.account1.name,
                domainid=cls.account1.domainid
            )

            # Authorize Security group to SSH to VM
            ingress_rule = security_group.authorize(
                cls.apiclient,
                cls.testdata["ingress_rule"],
                account=cls.account1.name,
                domainid=cls.account1.domainid
            )

            # Authorize Security group to SSH to VM
            ingress_rule2 = security_group.authorize(
                cls.apiclient,
                cls.testdata["ingress_rule_ICMP"],
                account=cls.account1.name,
                domainid=cls.account1.domainid
            )

        cls.testdata["shared_network_offering_sg"]["specifyVlan"] = 'True'
        cls.testdata["shared_network_offering_sg"]["specifyIpRanges"] = 'True'
        cls.shared_network_offering = NetworkOffering.create(
            cls.apiclient,
            cls.testdata["shared_network_offering_sg"],
            conservemode=False
        )

        NetworkOffering.update(
            cls.shared_network_offering,
            cls.apiclient,
            id=cls.shared_network_offering.id,
            state="enabled"
        )

        physical_network, vlan = get_free_vlan(cls.apiclient, cls.zone.id)
        cls.testdata["shared_network_sg"]["physicalnetworkid"] = physical_network.id

        random_subnet_number = random.randrange(90, 99)
        cls.testdata["shared_network_sg"]["name"] = "Shared-Network-SG-Test-vlan" + str(random_subnet_number)
        cls.testdata["shared_network_sg"]["displaytext"] = "Shared-Network-SG-Test-vlan" + str(random_subnet_number)
        cls.testdata["shared_network_sg"]["vlan"] = "vlan://" + str(random_subnet_number)
        cls.testdata["shared_network_sg"]["startip"] = "192.168." + str(random_subnet_number) + ".240"
        cls.testdata["shared_network_sg"]["endip"] = "192.168." + str(random_subnet_number) + ".250"
        cls.testdata["shared_network_sg"]["gateway"] = "192.168." + str(random_subnet_number) + ".254"
        cls.network1 = Network.create(
            cls.apiclient,
            cls.testdata["shared_network_sg"],
            networkofferingid=cls.shared_network_offering.id,
            zoneid=cls.zone.id,
            accountid=cls.account1.name,
            domainid=cls.account1.domainid
        )

        random_subnet_number = random.randrange(100, 110)
        cls.testdata["shared_network_sg"]["name"] = "Shared-Network-SG-Test-vlan" + str(random_subnet_number)
        cls.testdata["shared_network_sg"]["displaytext"] = "Shared-Network-SG-Test-vlan" + str(random_subnet_number)
        cls.testdata["shared_network_sg"]["vlan"] = "vlan://" + str(random_subnet_number)
        cls.testdata["shared_network_sg"]["startip"] = "192.168." + str(random_subnet_number) + ".240"
        cls.testdata["shared_network_sg"]["endip"] = "192.168." + str(random_subnet_number) + ".250"
        cls.testdata["shared_network_sg"]["gateway"] = "192.168." + str(random_subnet_number) + ".254"
        cls.network2 = Network.create(
            cls.apiclient,
            cls.testdata["shared_network_sg"],
            networkofferingid=cls.shared_network_offering.id,
            zoneid=cls.zone.id,
            accountid=cls.account1.name,
            domainid=cls.account1.domainid
        )

        random_subnet_number = random.randrange(111, 120)
        cls.testdata["shared_network_sg"]["name"] = "Shared-Network-SG-Test-vlan" + str(random_subnet_number)
        cls.testdata["shared_network_sg"]["displaytext"] = "Shared-Network-SG-Test-vlan" + str(random_subnet_number)
        cls.testdata["shared_network_sg"]["vlan"] = "vlan://" + str(random_subnet_number)
        cls.testdata["shared_network_sg"]["startip"] = "192.168." + str(random_subnet_number) + ".240"
        cls.testdata["shared_network_sg"]["endip"] = "192.168." + str(random_subnet_number) + ".250"
        cls.testdata["shared_network_sg"]["gateway"] = "192.168." + str(random_subnet_number) + ".254"
        cls.network3 = Network.create(
            cls.apiclient,
            cls.testdata["shared_network_sg"],
            networkofferingid=cls.shared_network_offering.id,
            zoneid=cls.zone.id,
            accountid=cls.account1.name,
            domainid=cls.account1.domainid
        )

        try:
            cls.virtual_machine1 = VirtualMachine.create(
                cls.apiclient,
                cls.testdata["virtual_machine"],
                accountid=cls.account1.name,
                domainid=cls.account1.domainid,
                serviceofferingid=cls.service_offering.id,
                templateid=cls.template.id,
                securitygroupids=[security_group.id],
                networkids=cls.network1.id
            )
            for nic in cls.virtual_machine1.nic:
                if nic.isdefault:
                    cls.virtual_machine1.ssh_ip = nic.ipaddress
                    cls.virtual_machine1.default_network_id = nic.networkid
                    break
        except Exception as e:
            cls.fail("Exception while deploying virtual machine: %s" % e)

        try:
            cls.virtual_machine2 = VirtualMachine.create(
                cls.apiclient,
                cls.testdata["virtual_machine"],
                accountid=cls.account1.name,
                domainid=cls.account1.domainid,
                serviceofferingid=cls.service_offering.id,
                templateid=cls.template.id,
                securitygroupids=[security_group.id],
                networkids=[str(cls.network1.id), str(cls.network2.id)]
            )
            for nic in cls.virtual_machine2.nic:
                if nic.isdefault:
                    cls.virtual_machine2.ssh_ip = nic.ipaddress
                    cls.virtual_machine2.default_network_id = nic.networkid
                    break
        except Exception as e:
            cls.fail("Exception while deploying virtual machine: %s" % e)

        cls._cleanup.append(cls.virtual_machine1)
        cls._cleanup.append(cls.virtual_machine2)
        cls._cleanup.append(cls.network1)
        cls._cleanup.append(cls.network2)
        cls._cleanup.append(cls.network3)
        cls._cleanup.append(cls.shared_network_offering)
        if cls.zone.securitygroupsenabled:
            cls._cleanup.append(security_group)
        cls._cleanup.append(cls.account1)
        cls._cleanup.append(cls.user_domain)

    @classmethod
    def tearDownClass(self):
        try:
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        if self.skip:
            self.skipTest("Test can be run only on advanced zone and KVM hypervisor")
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def verify_network_rules(self, vm_id):
        virtual_machine = VirtualMachine.list(
             self.apiclient,
             id=vm_id
        )
        vm = virtual_machine[0]
        hosts = list_hosts(
            self.apiclient,
            id=vm.hostid
        )
        host = hosts[0]
        if host.hypervisor.lower() not in "kvm":
            return
        host.user, host.password = get_host_credentials(self.config, host.ipaddress)
        for nic in vm.nic:
            secips = ""
            if len(nic.secondaryip) > 0:
                for secip in nic.secondaryip:
                    secips += secip.ipaddress + ";"
            command="/usr/share/cloudstack-common/scripts/vm/network/security_group.py verify_network_rules --vmname %s --vmip %s --vmmac %s --nicsecips '%s'" % (vm.instancename, nic.ipaddress, nic.macaddress, secips)
            self.logger.debug("Executing command '%s' in host %s" % (command, host.ipaddress))
            result=execute_command_in_host(host.ipaddress, 22,
                host.user,
                host.password,
                command)
            if len(result) > 0:
                self.fail("The iptables/ebtables rules for nic %s on vm %s on host %s are not correct" %(nic.ipaddress, vm.instancename, host.name))

    @attr(tags=["adeancedsg"], required_hardware="false")
    def test_01_create_vm_with_multiple_nics(self):
        """Create Vm with multiple NIC's

            Steps:
            # 1. Create more than 1 isolated or shared network
            # 2. Create a vm and select more than 1 network while deploying
            # 3. Vm is deployed successfully with 1 nic from each network
            # 4. All the vm's should be pingable
        :return:
        """
        virtual_machine = VirtualMachine.list(
             self.apiclient,
             id=self.virtual_machine2.id
        )
        self.assertEqual(
            len(virtual_machine), 1,
            "Virtual Machine create with 2 NIC's failed")

        nicIdInVm = virtual_machine[0].nic[0]
        self.assertIsNotNone(nicIdInVm, "NIC 1 not found in Virtual Machine")

        nicIdInVm = virtual_machine[0].nic[1]
        self.assertIsNotNone(nicIdInVm, "NIC 2 not found in Virtual Machine")

        self.verify_network_rules(self.virtual_machine2.id)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_02_add_nic_to_vm(self):
        """Create VM with single NIC and then add additional NIC

            Steps:
            # 1. Create a VM by selecting one default NIC
            # 2. Create few more isolated or shared networks
            # 3. Add extra NIC's to the vm from the newly created networks
            # 4. The deployed VM should have extra nic's added in the above
            #    step without any fail
            # 5. The IP's of the extra NIC's should be pingable
        :return:
        """
        self.virtual_machine1.add_nic(self.apiclient, self.network2.id)

        virtual_machine = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine1.id
        )

        nicIdInVm = virtual_machine[0].nic[1]
        self.assertIsNotNone(nicIdInVm, "Second NIC not found")

        self.verify_network_rules(self.virtual_machine1.id)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_03_add_ip_to_default_nic(self):
        """ Add secondary IP's to the VM

            Steps:
            # 1. Create a VM with more than 1 NIC
            # 2) Navigate to Instances->NIC->Edit Secondary IP's
            # ->Aquire new Secondary IP"
            # 3) Add as many secondary Ip as possible to the VM
            # 4) Configure the secondary IP's by referring to "Configure
            # the secondary IP's" in the "Action Item" section
        :return:
        """
        ipaddress = NIC.addIp(
            self.apiclient,
            id=self.virtual_machine2.nic[0].id
        )

        self.assertIsNotNone(
            ipaddress,
            "Unable to add secondary IP to the default NIC")

        self.verify_network_rules(self.virtual_machine2.id)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_04_add_ip_to_remaining_nics(self):
        """ Add secondary IP's to remaining NIC's

            Steps:
            # 1) Create a VM with more than 1 NIC
            # 2)Navigate to Instances-NIC's->Edit Secondary IP's
            # ->Acquire new Secondary IP
            # 3) Add secondary IP to all the NIC's of the VM
            # 4) Confiugre the secondary IP's by referring to "Configure the
            # secondary IP's" in the "Action Item" section
        :return:
        """

        self.virtual_machine1.add_nic(self.apiclient, self.network3.id)

        vms = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine1.id
        )

        self.assertIsNotNone(
            vms[0].nic[2],
            "Third NIC is not added successfully to the VM")

        vms1_nic1_id = vms[0].nic[1]['id']
        vms1_nic2_id = vms[0].nic[2]['id']

        ipaddress21 = NIC.addIp(
            self.apiclient,
            id=vms1_nic1_id
        )

        ipaddress22 = NIC.addIp(
            self.apiclient,
            id=vms1_nic1_id
        )

        self.assertIsNotNone(
            ipaddress21,
            "Unable to add first secondary IP to the second nic")
        self.assertIsNotNone(
            ipaddress22,
            "Unable to add second secondary IP to second NIC")

        ipaddress31 = NIC.addIp(
            self.apiclient,
            id=vms1_nic2_id
        )

        ipaddress32 = NIC.addIp(
            self.apiclient,
            id=vms1_nic2_id
        )

        self.assertIsNotNone(
            ipaddress31,
            "Unable to add first secondary IP to third NIC")
        self.assertIsNotNone(
            ipaddress32,
            "Unable to add second secondary IP to third NIC")

        self.verify_network_rules(self.virtual_machine1.id)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_05_stop_start_vm_with_multiple_nic(self):
        """ Stop and Start a VM with Multple NIC

            Steps:
            # 1) Create a Vm with multiple NIC's
            # 2) Configure secondary IP's on the VM
            # 3) Try to stop/start the VM
            # 4) Ping the IP's of the vm
            # 5) Remove Secondary IP from one of the NIC
        :return:
        """
        ipaddress1 = NIC.addIp(
            self.apiclient,
            id=self.virtual_machine2.nic[0].id
        )

        ipaddress2 = NIC.addIp(
            self.apiclient,
            id=self.virtual_machine2.nic[1].id
        )
        # Stop the VM with multiple NIC's
        self.virtual_machine2.stop(self.apiclient)

        virtual_machine = VirtualMachine.list(
             self.apiclient,
             id=self.virtual_machine2.id
        )

        self.assertEqual(
            virtual_machine[0]['state'], 'Stopped',
            "Could not stop the VM with multiple NIC's")

        if virtual_machine[0]['state'] == 'Stopped':
            # If stopped then try to start the VM
            self.virtual_machine2.start(self.apiclient)
            virtual_machine = VirtualMachine.list(
                self.apiclient,
                id=self.virtual_machine2.id
            )
            self.assertEqual(
                virtual_machine[0]['state'], 'Running',
                "Could not start the VM with multiple NIC's")

        self.verify_network_rules(self.virtual_machine2.id)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_06_migrate_vm_with_multiple_nic(self):
        """ Migrate a VM with Multple NIC

            Steps:
            # 1) Create a Vm with multiple NIC's
            # 2) Configure secondary IP's on the VM
            # 3) Try to stop/start the VM
            # 4) Ping the IP's of the vm
        :return:
        """
        # Skipping adding Secondary IP to NIC since its already
        # done in the previous test cases

        virtual_machine = VirtualMachine.list(
             self.apiclient,
             id=self.virtual_machine1.id
        )
        old_host_id = virtual_machine[0]['hostid']

        try:
            hosts = Host.list(
                self.apiclient,
                virtualmachineid=self.virtual_machine1.id,
                listall=True)
            self.assertEqual(
                validateList(hosts)[0],
                PASS,
                "hosts list validation failed")

            # Get a host which is not already assigned to VM
            for host in hosts:
                if host.id == old_host_id:
                    continue
                else:
                    host_id = host.id
                    break

            self.virtual_machine1.migrate(self.apiclient, host_id)
        except Exception as e:
            self.fail("Exception occured: %s" % e)

        # List the vm again
        virtual_machine = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine1.id)

        new_host_id = virtual_machine[0]['hostid']

        self.assertNotEqual(
            old_host_id, new_host_id,
            "Migration of VM to new host failed"
        )

        self.verify_network_rules(self.virtual_machine1.id)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_07_remove_secondary_ip_from_nic(self):
        """ Remove secondary IP from any NIC
            Steps:
            # 1) Navigate to Instances
            # 2) Select any vm
            # 3) NIC's ->Edit secondary IP's->Release IP
            # 4) The secondary IP should be successfully removed
        """
        virtual_machine = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine2.id)

        # Check which NIC is having secondary IP
        secondary_ips = virtual_machine[0].nic[1].secondaryip

        for secondary_ip in secondary_ips:
            NIC.removeIp(self.apiclient, ipaddressid=secondary_ip['id'])

        virtual_machine = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine2.id
        )

        self.assertFalse(
            virtual_machine[0].nic[1].secondaryip,
            'Failed to remove secondary IP')

        self.verify_network_rules(self.virtual_machine2.id)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_08_remove_nic_from_vm(self):
        """ Remove NIC from VM
            Steps:
            # 1) Navigate to Instances->select any vm->NIC's->NIC 2
            # ->Click on "X" button to remove the second NIC
            # 2) Remove other NIC's as well from the VM
            # 3) All the NIC's should be successfully removed from the VM
        :return:
        """
        virtual_machine = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine2.id)

        for nic in virtual_machine[0].nic:
            if nic.isdefault:
                continue
            self.virtual_machine2.remove_nic(self.apiclient, nic.id)

        virtual_machine = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine2.id)

        self.assertEqual(
            len(virtual_machine[0].nic), 1,
            "Failed to remove all the nics from the virtual machine")

        self.verify_network_rules(self.virtual_machine2.id)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_09_reboot_vm_with_multiple_nic(self):
        """ Reboot a VM with Multple NIC

            Steps:
            # 1) Create a Vm with multiple NIC's
            # 2) Configure secondary IP's on the VM
            # 3) Try to reboot the VM
            # 4) Ping the IP's of the vm
        :return:
        """
        # Skipping adding Secondary IP to NIC since its already
        # done in the previous test cases

        virtual_machine = VirtualMachine.list(
             self.apiclient,
             id=self.virtual_machine1.id
        )
        try:
            self.virtual_machine1.reboot(self.apiclient)
        except Exception as e:
            self.fail("Exception occured: %s" % e)

        self.verify_network_rules(self.virtual_machine1.id)

