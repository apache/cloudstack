#!/usr/bin/env python
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

""" P1 tests for high availability
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import (prepareHostForMaintenance,
                                  cancelHostMaintenance)
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             Host,
                             VirtualMachine,
                             Network,
                             ServiceOffering,
                             NATRule,
                             LoadBalancerRule,
                             Snapshot,
                             Template,
                             PublicIPAddress)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               get_pod,
                               list_volumes,
                               list_snapshots,
                               list_templates,
                               wait_for_ssvms)
import time


class Services:

    """Test network offering Services
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "HA",
                "lastname": "HA",
                "username": "HA",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,    # in MHz
                                    "memory": 128,       # In MBs
            },
            "lbrule": {
                "name": "SSH",
                "alg": "roundrobin",
                # Algorithm used for load balancing
                "privateport": 22,
                "publicport": 2222,
            },
            "natrule": {
                "privateport": 22,
                "publicport": 22,
                "protocol": "TCP"
            },
            "fw_rule": {
                "startport": 1,
                "endport": 6000,
                "cidr": '55.55.0.0/11',
                # Any network (For creating FW rule)
            },
            "virtual_machine": {
                "displayname": "VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "templates": {
                "displaytext": "Public Template",
                "name": "Public template",
                "ostype": 'CentOS 5.3 (64-bit)',
                "url": "http://download.cloudstack.org/releases/2.0.0/UbuntuServer-10-04-64bit.vhd.bz2",
                "hypervisor": 'XenServer',
                "format": 'VHD',
                "isfeatured": True,
                "ispublic": True,
                "isextractable": True,
                "templatefilter": 'self',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "sleep": 60,
            "timeout": 100,
            "mode": 'advanced'
        }


class TestHighAvailability(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestHighAvailability, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(
            cls.api_client,
            zone_id=cls.zone.id
        )
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("Template creation from root volume is not supported in LXC")
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"],
            offerha=True
        )
        cls._cleanup = [
            cls.service_offering,
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "multihost"],
        required_hardware="true")
    def test_01_host_maintenance_mode(self):
        """Test host maintenance mode
        """

        # Validate the following
        # 1. Create Vms. Acquire IP. Create port forwarding & load balancing
        #    rules for Vms.
        # 2. Host 1: put to maintenance mode. All Vms should failover to Host
        #    2 in cluster. Vms should be in running state. All port forwarding
        #    rules and load balancing Rules should work.
        # 3. After failover to Host 2 succeeds, deploy Vms. Deploy Vms on host
        #    2 should succeed.
        # 4. Host 1: cancel maintenance mode.
        # 5. Host 2 : put to maintenance mode. All Vms should failover to
        #    Host 1 in cluster.
        # 6. After failover to Host 1 succeeds, deploy VMs. Deploy Vms on
        #    host 1 should succeed.

        hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            resourcestate='Enabled',
            type='Routing'
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "List hosts should return valid host response"
        )
        if len(hosts) < 2:
            self.skipTest("There must be at least 2 hosts present in cluster")

        self.debug("Checking HA with hosts: %s, %s" % (
            hosts[0].name,
            hosts[1].name
        ))
        self.debug("Deploying VM in account: %s" % self.account.name)
        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )
        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )
        vm = vms[0]
        self.debug("Deployed VM on host: %s" % vm.hostid)
        self.assertEqual(
            vm.state,
            "Running",
            "Deployed VM should be in RUnning state"
        )
        networks = Network.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return valid list for the account"
        )
        network = networks[0]

        self.debug("Associating public IP for account: %s" %
                   self.account.name)
        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id
        )

        self.debug("Associated %s with network %s" % (
            public_ip.ipaddress.ipaddress,
            network.id
        ))
        self.debug("Creating PF rule for IP address: %s" %
                   public_ip.ipaddress.ipaddress)
        NATRule.create(
            self.apiclient,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=public_ip.ipaddress.id
        )

        self.debug("Creating LB rule on IP with NAT: %s" %
                   public_ip.ipaddress.ipaddress)

        # Create Load Balancer rule on IP already having NAT rule
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name
        )
        self.debug("Created LB rule with ID: %s" % lb_rule.id)

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % virtual_machine.id)
            virtual_machine.get_ssh_client(
                ipaddress=public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine.ipaddress, e)
                      )

        first_host = vm.hostid
        self.debug("Enabling maintenance mode for host %s" % vm.hostid)
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = first_host
        self.apiclient.prepareHostForMaintenance(cmd)

        self.debug("Waiting for SSVMs to come up")
        wait_for_ssvms(
            self.apiclient,
            zoneid=self.zone.id,
            podid=self.pod.id,
        )

        timeout = self.services["timeout"]
        # Poll and check state of VM while it migrates from one host to another
        while True:
            vms = VirtualMachine.list(
                self.apiclient,
                id=virtual_machine.id,
                listall=True
            )
            self.assertEqual(
                isinstance(vms, list),
                True,
                "List VMs should return valid response for deployed VM"
            )
            self.assertNotEqual(
                len(vms),
                0,
                "List VMs should return valid response for deployed VM"
            )
            vm = vms[0]

            self.debug("VM 1 state: %s" % vm.state)
            if vm.state in ["Stopping",
                            "Stopped",
                            "Running",
                            "Starting",
                            "Migrating"]:
                if vm.state == "Running":
                    break
                else:
                    time.sleep(self.services["sleep"])
                    timeout = timeout - 1
            else:
                self.fail(
                    "VM migration from one-host-to-other\
                            failed while enabling maintenance"
                )
        second_host = vm.hostid
        self.assertEqual(
            vm.state,
            "Running",
            "VM should be in Running state after enabling host maintenance"
        )
        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % virtual_machine.id)
            virtual_machine.get_ssh_client(
                ipaddress=public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine.ipaddress, e)
                      )
        self.debug("Deploying VM in account: %s" % self.account.name)
        # Spawn an instance on other host
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_2.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )
        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )
        vm = vms[0]
        self.debug("Deployed VM on host: %s" % vm.hostid)
        self.debug("VM 2 state: %s" % vm.state)
        self.assertEqual(
            vm.state,
            "Running",
            "Deployed VM should be in Running state"
        )

        self.debug("Canceling host maintenance for ID: %s" % first_host)
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = first_host
        self.apiclient.cancelHostMaintenance(cmd)
        self.debug("Maintenance mode canceled for host: %s" % first_host)

        self.debug("Enabling maintenance mode for host %s" % second_host)
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = second_host
        self.apiclient.prepareHostForMaintenance(cmd)
        self.debug("Maintenance mode enabled for host: %s" % second_host)

        self.debug("Waiting for SSVMs to come up")
        wait_for_ssvms(
            self.apiclient,
            zoneid=self.zone.id,
            podid=self.pod.id,
        )

        # Poll and check the status of VMs
        timeout = self.services["timeout"]
        while True:
            vms = VirtualMachine.list(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                listall=True
            )
            self.assertEqual(
                isinstance(vms, list),
                True,
                "List VMs should return valid response for deployed VM"
            )
            self.assertNotEqual(
                len(vms),
                0,
                "List VMs should return valid response for deployed VM"
            )
            vm = vms[0]
            self.debug(
                "VM state after enabling maintenance on first host: %s" %
                vm.state)
            if vm.state in [
                "Stopping",
                "Stopped",
                "Running",
                "Starting",
                "Migrating"
            ]:
                if vm.state == "Running":
                    break
                else:
                    time.sleep(self.services["sleep"])
                    timeout = timeout - 1
            else:
                self.fail(
                    "VM migration from one-host-to-other failed\
                            while enabling maintenance"
                )

                # Poll and check the status of VMs
        timeout = self.services["timeout"]
        while True:
            vms = VirtualMachine.list(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                listall=True
            )
            self.assertEqual(
                isinstance(vms, list),
                True,
                "List VMs should return valid response for deployed VM"
            )
            self.assertNotEqual(
                len(vms),
                0,
                "List VMs should return valid response for deployed VM"
            )
            vm = vms[1]
            self.debug(
                "VM state after enabling maintenance on first host: %s" %
                vm.state)
            if vm.state in [
                "Stopping",
                "Stopped",
                "Running",
                "Starting",
                "Migrating"
            ]:
                if vm.state == "Running":
                    break
                else:
                    time.sleep(self.services["sleep"])
                    timeout = timeout - 1
            else:
                self.fail(
                    "VM migration from one-host-to-other\
                            failed while enabling maintenance"
                )

        for vm in vms:
            self.debug(
                "VM states after enabling maintenance mode on host: %s - %s" %
                (first_host, vm.state))
            self.assertEqual(
                vm.state,
                "Running",
                "Deployed VM should be in Running state"
            )

        # Spawn an instance on other host
        virtual_machine_3 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_3.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )
        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )
        vm = vms[0]

        self.debug("Deployed VM on host: %s" % vm.hostid)
        self.debug("VM 3 state: %s" % vm.state)
        self.assertEqual(
            vm.state,
            "Running",
            "Deployed VM should be in Running state"
        )

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % virtual_machine.id)
            virtual_machine.get_ssh_client(
                ipaddress=public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine.ipaddress, e)
                      )

        self.debug("Canceling host maintenance for ID: %s" % second_host)
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = second_host
        self.apiclient.cancelHostMaintenance(cmd)
        self.debug("Maintenance mode canceled for host: %s" % second_host)

        self.debug("Waiting for SSVMs to come up")
        wait_for_ssvms(
            self.apiclient,
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "multihost"],
        required_hardware="true")
    def test_02_host_maintenance_mode_with_activities(self):
        """Test host maintenance mode with activities
        """

        # Validate the following
        # 1. Create Vms. Acquire IP. Create port forwarding & load balancing
        #    rules for Vms.
        # 2. While activities are ongoing: Create snapshots, recurring
        #    snapshots, create templates, download volumes, Host 1: put to
        #    maintenance mode. All Vms should failover to Host 2 in cluster
        #    Vms should be in running state. All port forwarding rules and
        #    load balancing Rules should work.
        # 3. After failover to Host 2 succeeds, deploy Vms. Deploy Vms on host
        #    2 should succeed. All ongoing activities in step 3 should succeed
        # 4. Host 1: cancel maintenance mode.
        # 5. While activities are ongoing: Create snapshots, recurring
        #    snapshots, create templates, download volumes, Host 2: put to
        #    maintenance mode. All Vms should failover to Host 1 in cluster.
        # 6. After failover to Host 1 succeeds, deploy VMs. Deploy Vms on
        #    host 1 should succeed. All ongoing activities in step 6 should
        #    succeed.

        hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            resourcestate='Enabled',
            type='Routing'
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "List hosts should return valid host response"
        )
        if len(hosts) < 2:
            self.skipTest("There must be at least 2 hosts present in cluster")

        self.debug("Checking HA with hosts: %s, %s" % (
            hosts[0].name,
            hosts[1].name
        ))
        self.debug("Deploying VM in account: %s" % self.account.name)
        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )
        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )
        vm = vms[0]
        self.debug("Deployed VM on host: %s" % vm.hostid)
        self.assertEqual(
            vm.state,
            "Running",
            "Deployed VM should be in RUnning state"
        )
        networks = Network.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            isinstance(networks, list),
            True,
            "List networks should return valid list for the account"
        )
        network = networks[0]

        self.debug("Associating public IP for account: %s" %
                   self.account.name)
        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=network.id
        )

        self.debug("Associated %s with network %s" % (
            public_ip.ipaddress.ipaddress,
            network.id
        ))
        self.debug("Creating PF rule for IP address: %s" %
                   public_ip.ipaddress.ipaddress)
        NATRule.create(
            self.apiclient,
            virtual_machine,
            self.services["natrule"],
            ipaddressid=public_ip.ipaddress.id
        )

        self.debug("Creating LB rule on IP with NAT: %s" %
                   public_ip.ipaddress.ipaddress)

        # Create Load Balancer rule on IP already having NAT rule
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name
        )
        self.debug("Created LB rule with ID: %s" % lb_rule.id)

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % virtual_machine.id)
            virtual_machine.get_ssh_client(
                ipaddress=public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine.ipaddress, e)
                      )
        # Get the Root disk of VM
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=virtual_machine.id,
            type='ROOT',
            listall=True
        )
        volume = volumes[0]
        self.debug(
            "Root volume of VM(%s): %s" % (
                virtual_machine.name,
                volume.name
            ))
        # Create a snapshot from the ROOTDISK
        self.debug("Creating snapshot on ROOT volume: %s" % volume.name)
        snapshot = Snapshot.create(self.apiclient, volumes[0].id)
        self.debug("Snapshot created: ID - %s" % snapshot.id)

        snapshots = list_snapshots(
            self.apiclient,
            id=snapshot.id,
            listall=True
        )
        self.assertEqual(
            isinstance(snapshots, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            snapshots,
            None,
            "Check if result exists in list snapshots call"
        )
        self.assertEqual(
            snapshots[0].id,
            snapshot.id,
            "Check snapshot id in list resources call"
        )

        # Generate template from the snapshot
        self.debug("Generating template from snapshot: %s" % snapshot.name)
        template = Template.create_from_snapshot(
            self.apiclient,
            snapshot,
            self.services["templates"]
        )
        self.debug("Created template from snapshot: %s" % template.id)

        templates = list_templates(
            self.apiclient,
            templatefilter=self.services["templates"]["templatefilter"],
            id=template.id
        )

        self.assertEqual(
            isinstance(templates, list),
            True,
            "List template call should return the newly created template"
        )

        self.assertEqual(
            templates[0].isready,
            True,
            "The newly created template should be in ready state"
        )

        first_host = vm.hostid
        self.debug("Enabling maintenance mode for host %s" % vm.hostid)
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = first_host
        self.apiclient.prepareHostForMaintenance(cmd)

        self.debug("Waiting for SSVMs to come up")
        wait_for_ssvms(
            self.apiclient,
            zoneid=self.zone.id,
            podid=self.pod.id,
        )

        timeout = self.services["timeout"]
        # Poll and check state of VM while it migrates from one host to another
        while True:
            vms = VirtualMachine.list(
                self.apiclient,
                id=virtual_machine.id,
                listall=True
            )
            self.assertEqual(
                isinstance(vms, list),
                True,
                "List VMs should return valid response for deployed VM"
            )
            self.assertNotEqual(
                len(vms),
                0,
                "List VMs should return valid response for deployed VM"
            )
            vm = vms[0]

            self.debug("VM 1 state: %s" % vm.state)
            if vm.state in ["Stopping",
                            "Stopped",
                            "Running",
                            "Starting",
                            "Migrating"]:
                if vm.state == "Running":
                    break
                else:
                    time.sleep(self.services["sleep"])
                    timeout = timeout - 1
            else:
                self.fail(
                    "VM migration from one-host-to-other failed\
                            while enabling maintenance"
                )
        second_host = vm.hostid
        self.assertEqual(
            vm.state,
            "Running",
            "VM should be in Running state after enabling host maintenance"
        )
        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % virtual_machine.id)
            virtual_machine.get_ssh_client(
                ipaddress=public_ip.ipaddress.ipaddress)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" %
                      (virtual_machine.ipaddress, e)
                      )
        self.debug("Deploying VM in account: %s" % self.account.name)
        # Spawn an instance on other host
        virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_2.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )
        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )
        vm = vms[0]
        self.debug("Deployed VM on host: %s" % vm.hostid)
        self.debug("VM 2 state: %s" % vm.state)
        self.assertEqual(
            vm.state,
            "Running",
            "Deployed VM should be in Running state"
        )

        self.debug("Canceling host maintenance for ID: %s" % first_host)
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = first_host
        self.apiclient.cancelHostMaintenance(cmd)
        self.debug("Maintenance mode canceled for host: %s" % first_host)

        # Get the Root disk of VM
        volumes = list_volumes(
            self.apiclient,
            virtualmachineid=virtual_machine_2.id,
            type='ROOT',
            listall=True
        )
        volume = volumes[0]
        self.debug(
            "Root volume of VM(%s): %s" % (
                virtual_machine_2.name,
                volume.name
            ))
        # Create a snapshot from the ROOTDISK
        self.debug("Creating snapshot on ROOT volume: %s" % volume.name)
        snapshot = Snapshot.create(self.apiclient, volumes[0].id)
        self.debug("Snapshot created: ID - %s" % snapshot.id)

        snapshots = list_snapshots(
            self.apiclient,
            id=snapshot.id,
            listall=True
        )
        self.assertEqual(
            isinstance(snapshots, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            snapshots,
            None,
            "Check if result exists in list snapshots call"
        )
        self.assertEqual(
            snapshots[0].id,
            snapshot.id,
            "Check snapshot id in list resources call"
        )

        # Generate template from the snapshot
        self.debug("Generating template from snapshot: %s" % snapshot.name)
        template = Template.create_from_snapshot(
            self.apiclient,
            snapshot,
            self.services["templates"]
        )
        self.debug("Created template from snapshot: %s" % template.id)

        templates = list_templates(
            self.apiclient,
            templatefilter=self.services["templates"]["templatefilter"],
            id=template.id
        )

        self.assertEqual(
            isinstance(templates, list),
            True,
            "List template call should return the newly created template"
        )

        self.assertEqual(
            templates[0].isready,
            True,
            "The newly created template should be in ready state"
        )

        self.debug("Enabling maintenance mode for host %s" % second_host)
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = second_host
        self.apiclient.prepareHostForMaintenance(cmd)
        self.debug("Maintenance mode enabled for host: %s" % second_host)

        self.debug("Waiting for SSVMs to come up")
        wait_for_ssvms(
            self.apiclient,
            zoneid=self.zone.id,
            podid=self.pod.id,
        )

        # Poll and check the status of VMs
        timeout = self.services["timeout"]
        while True:
            vms = VirtualMachine.list(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                listall=True
            )
            self.assertEqual(
                isinstance(vms, list),
                True,
                "List VMs should return valid response for deployed VM"
            )
            self.assertNotEqual(
                len(vms),
                0,
                "List VMs should return valid response for deployed VM"
            )
            vm = vms[0]
            self.debug(
                "VM state after enabling maintenance on first host: %s" %
                vm.state)
            if vm.state in ["Stopping",
                            "Stopped",
                            "Running",
                            "Starting",
                            "Migrating"]:
                if vm.state == "Running":
                    break
                else:
                    time.sleep(self.services["sleep"])
                    timeout = timeout - 1
            else:
                self.fail(
                    "VM migration from one-host-to-other failed\
                            while enabling maintenance"
                )

        # Poll and check the status of VMs
        timeout = self.services["timeout"]
        while True:
            vms = VirtualMachine.list(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                listall=True
            )
            self.assertEqual(
                isinstance(vms, list),
                True,
                "List VMs should return valid response for deployed VM"
            )
            self.assertNotEqual(
                len(vms),
                0,
                "List VMs should return valid response for deployed VM"
            )
            vm = vms[1]
            self.debug(
                "VM state after enabling maintenance on first host: %s" %
                vm.state)
            if vm.state in ["Stopping",
                            "Stopped",
                            "Running",
                            "Starting",
                            "Migrating"]:
                if vm.state == "Running":
                    break
                else:
                    time.sleep(self.services["sleep"])
                    timeout = timeout - 1
            else:
                self.fail(
                    "VM migration from one-host-to-other failed\
                            while enabling maintenance"
                )

        for vm in vms:
            self.debug(
                "VM states after enabling maintenance mode on host: %s - %s" %
                (first_host, vm.state))
            self.assertEqual(
                vm.state,
                "Running",
                "Deployed VM should be in Running state"
            )

        # Spawn an instance on other host
        virtual_machine_3 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_3.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )
        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )
        vm = vms[0]

        self.debug("Deployed VM on host: %s" % vm.hostid)
        self.debug("VM 3 state: %s" % vm.state)
        self.assertEqual(
            vm.state,
            "Running",
            "Deployed VM should be in Running state"
        )

        self.debug("Canceling host maintenance for ID: %s" % second_host)
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = second_host
        self.apiclient.cancelHostMaintenance(cmd)
        self.debug("Maintenance mode canceled for host: %s" % second_host)

        self.debug("Waiting for SSVMs to come up")
        wait_for_ssvms(
            self.apiclient,
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        return
