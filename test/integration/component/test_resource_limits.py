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
""" P1 tests for Resource limits
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (VirtualMachine,
                                         Snapshot,
                                         Template,
                                         PublicIPAddress,
                                         Account,
                                         Domain,
                                         Volume,
                                         Network,
                                         DiskOffering,
                                         NetworkOffering,
                                         ServiceOffering,
                                         Configurations)
from marvin.lib.common import (list_volumes,
                                           get_domain,
                                           get_zone,
                                           get_template,
                                           update_resource_limit,
                                           list_configurations,
                                           wait_for_cleanup)
from marvin.lib.utils import cleanup_resources
import time


class Services:
    """Test Resource Limits Services
    """

    def __init__(self):
        self.services = {
                        "domain": {
                                   "name": "Domain",
                                   },
                        "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                         },
                         "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 100,
                                    # in MHz
                                    "memory": 128,
                                    # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small",
                                    "name": "Small",
                                    "disksize": 1
                        },
                        "volume": {
                                   "diskname": "TestDiskServ",
                        },
                        "server": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                        },
                        "template": {
                                    "displaytext": "Cent OS Template",
                                    "name": "Cent OS Template",
                                    "ostype": 'CentOS 5.3 (64-bit)',
                                    "templatefilter": 'self',
                        },
                        "network_offering": {
                                    "name": 'Network offering',
                                    "displaytext": 'Network offering',
                                    "guestiptype": 'Isolated',
                                    "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "serviceProviderList": {
                                            "Dhcp": 'VirtualRouter',
                                            "Dns": 'VirtualRouter',
                                            "SourceNat": 'VirtualRouter',
                                            "PortForwarding": 'VirtualRouter',
                                            "Vpn": 'VirtualRouter',
                                            "Firewall": 'VirtualRouter',
                                            "Lb": 'VirtualRouter',
                                            "UserData": 'VirtualRouter',
                                            "StaticNat": 'VirtualRouter',
                                        },
                                    },
                         "network": {
                                     "name": "test network",
                                     "displaytext": "test network"
                        },
                        "ostype": 'CentOS 5.3 (64-bit)',
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "timeout": 10,
                    }


class TestResourceLimitsAccount(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestResourceLimitsAccount, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["server"]["zoneid"] = cls.zone.id

        # Create Account, VMs etc
        cls.account_1 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        # Create Account, VMs etc
        cls.account_2 = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls._cleanup = [
                        cls.disk_offering,
                        cls.service_offering,
                        cls.account_1,
                        cls.account_2
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            # Wait for VMs to expunge
            wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_01_vm_per_account(self):
        """Test VM limit per account
        """

        # Validate the following
        # 1. Set user_vm=1 limit for account 1.
        # 2. Try to start 2 VMs account 1. Verify start of second VM is denied
        #    for this account.
        # 3. Try to start 2 VMs account 2. Verify 2 SM are started properly

        self.debug(
            "Updating instance resource limit for account: %s" %
                                                self.account_1.name)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              0, # Instance
                              account=self.account_1.name,
                              domainid=self.account_1.domainid,
                              max=1
                              )
        self.debug(
            "Deploying VM instance in account: %s" %
                                        self.account_1.name)

        virtual_machine = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_1.name,
                                domainid=self.account_1.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine)

        # Verify VM state
        self.assertEqual(
                            virtual_machine.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        # Exception should be raised for second instance (account_1)
        with self.assertRaises(Exception):
            VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_1.name,
                                domainid=self.account_1.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.debug(
            "Deploying VM instance in account: %s" %
                                        self.account_2.name)
        # Start 2 instances for account_2
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_2.name,
                                domainid=self.account_2.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        self.debug(
            "Deploying VM instance in account: %s" %
                                        self.account_2.name)
        virtual_machine_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_2.name,
                                domainid=self.account_2.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_2)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_2.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_02_publicip_per_account(self):
        """Test Public IP limit per account
        """

        # Validate the following
        # 1. Set Public_IP= 2 limit for account 1.
        # 2. start 1 VMs account 1
        # 3. start 1 VMs account 2
        # 4. Acquire 2 IP in account 1. Verify  account with limit should be
        #    denied to acquire more than one IP.
        # 5. Acquire 2 IP in account 2. Verify account 2 should be able to
        #    Acquire IP without any warning

        self.debug(
            "Updating public IP resource limit for account: %s" %
                                                self.account_1.name)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              1, # Public Ip
                              account=self.account_1.name,
                              domainid=self.account_1.domainid,
                              max=2
                              )

        self.debug(
            "Deploying VM instance in account: %s" %
                                        self.account_1.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_1.name,
                                domainid=self.account_1.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        self.debug(
            "Deploying VM instance in account: %s" %
                                        self.account_2.name)
        # Create VM for second account
        virtual_machine_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_2.name,
                                domainid=self.account_2.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_2)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_2.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        self.debug(
            "Associating public IP for account: %s" %
                                        virtual_machine_1.account)
        public_ip_1 = PublicIPAddress.create(
                                           self.apiclient,
                                           virtual_machine_1.account,
                                           virtual_machine_1.zoneid,
                                           virtual_machine_1.domainid,
                                           self.services["server"]
                                           )
        self.cleanup.append(public_ip_1)

        # Sleep to ensure that state is reflected across all the calls
        time.sleep(self.services["sleep"])

        # Verify Public IP state
        self.assertEqual(
                            public_ip_1.ipaddress.state in [
                                                            'Allocated',
                                                            'Allocating'
                                                            ],
                            True,
                            "Check Public IP state is allocated or not"
                        )

        # Exception should be raised for second instance (account_1)
        with self.assertRaises(Exception):
            PublicIPAddress.create(
                                   self.apiclient,
                                   virtual_machine_1.account,
                                   virtual_machine_1.zoneid,
                                   virtual_machine_1.domainid,
                                   self.services["server"]
                                   )

        self.debug(
            "Associating public IP for account: %s" %
                                        virtual_machine_2.account)
        # Assign Public IP for account 2
        public_ip_3 = PublicIPAddress.create(
                                           self.apiclient,
                                           virtual_machine_2.account,
                                           virtual_machine_2.zoneid,
                                           virtual_machine_2.domainid,
                                           self.services["server"]
                                           )
        self.cleanup.append(public_ip_3)

        # Verify Public IP state
        self.assertEqual(
                            public_ip_3.ipaddress.state in [
                                                            'Allocated',
                                                            'Allocating'
                                                            ],
                            True,
                            "Check Public IP state is allocated or not"
                        )
        self.debug(
            "Associating public IP for account: %s" %
                                        virtual_machine_2.account)
        public_ip_4 = PublicIPAddress.create(
                                           self.apiclient,
                                           virtual_machine_2.account,
                                           virtual_machine_2.zoneid,
                                           virtual_machine_2.domainid,
                                           self.services["server"]
                                           )
        self.cleanup.append(public_ip_4)
        # Verify Public IP state
        self.assertEqual(
                            public_ip_4.ipaddress.state in [
                                                            'Allocated',
                                                            'Allocating'
                                                            ],
                            True,
                            "Check Public IP state is allocated or not"
                        )
        return

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_03_snapshots_per_account(self):
        """Test Snapshot limit per account
        """

        # Validate the following
        # 1. Set snapshot= 2 limit for account 1.
        # 2. start 1 VMs account 1
        # 3. start 1 VMs account 2
        # 4. Create 2 snapshot  in account 1. Verify account with limit should
        #    be denied to create more than one snapshot.
        # 5. Create 2 snapshot in account 2. Verify account 2 should be able to
        #    create snapshots without any warning

        if self.hypervisor.lower() in ['hyperv', 'lxc']:
            self.skipTest("Snapshots feature is not supported on Hyper-V and LXC")
        self.debug(
            "Updating public IP resource limit for account: %s" %
                                                self.account_1.name)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              3, # Snapshot
                              account=self.account_1.name,
                              domainid=self.account_1.domainid,
                              max=1
                              )

        self.debug(
            "Deploying VM instance in account: %s" %
                                        self.account_1.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_1.name,
                                domainid=self.account_1.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        self.debug(
            "Deploying VM instance in account: %s" %
                                        self.account_1.name)
        # Create VM for second account
        virtual_machine_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_2.name,
                                domainid=self.account_2.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_2)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_2.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        # Get the Root disk of VM
        volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=virtual_machine_1.id,
                            type='ROOT',
                            listall=True
                            )
        self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
        volume = volumes[0]

        self.debug("Creating snapshot from volume: %s" % volumes[0].id)
        # Create a snapshot from the ROOTDISK (Account 1)
        snapshot_1 = Snapshot.create(self.apiclient,
                            volumes[0].id,
                            account=self.account_1.name,
                            domainid=self.account_1.domainid,
                            )
        self.cleanup.append(snapshot_1)
        # Verify Snapshot state
        self.assertEqual(
                            snapshot_1.state in [
                                                 'BackedUp',
                                                 'CreatedOnPrimary',
                                                 'Allocated'
                                                 ],
                            True,
                            "Snapshot state is not valid, it is %s" % snapshot_1.state
                        )

        # Exception should be raised for second snapshot (account_1)
        with self.assertRaises(Exception):
            Snapshot.create(self.apiclient,
                            volumes[0].id,
                            account=self.account_1.name,
                            domainid=self.account_1.domainid,
                            )

        # Get the Root disk of VM
        volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=virtual_machine_2.id,
                            type='ROOT',
                            listall=True
                            )
        self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
        volume = volumes[0]

        self.debug("Creating snapshot from volume: %s" % volumes[0].id)
        # Create a snapshot from the ROOTDISK (Account 2)
        snapshot_2 = Snapshot.create(self.apiclient,
                            volume.id,
                            account=self.account_2.name,
                            domainid=self.account_2.domainid,
                            )
        self.cleanup.append(snapshot_2)
        # Verify Snapshot state
        self.assertEqual(
                            snapshot_2.state in [
                                                 'BackedUp',
                                                 'CreatedOnPrimary',
                                                 'Allocated'
                                                 ],
                            True,
                            "Snapshot state is not valid, it is %s" % snapshot_2.state
                        )

        self.debug("Creating snapshot from volume: %s" % volumes[0].id)
        # Create a second snapshot from the ROOTDISK (Account 2)
        snapshot_3 = Snapshot.create(self.apiclient,
                            volume.id,
                            account=self.account_2.name,
                            domainid=self.account_2.domainid,
                            )
        self.cleanup.append(snapshot_3)
        # Verify Snapshot state
        self.assertEqual(
                            snapshot_3.state in [
                                                 'BackedUp',
                                                 'CreatedOnPrimary',
                                                 'Allocated'
                                                 ],
                            True,
                            "Snapshot state is not valid, it is %s" % snapshot_3.state
                        )
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_04_volumes_per_account(self):
        """Test Volumes limit per account
        """

        # Validate the following
        # 1. Set volumes=2 limit for account 1.
        # 2. Start 1 VMs account 1
        # 3. Start 1 VMs account 2
        # 4. Create 2 volumes in account 1. Verify account with limit should be
        #    denied to create more than one volume.
        # 5. Create 2 volumes in account 2. Verify account 2 should be able to
        #    create Volume without any warning

        self.debug(
            "Updating volume resource limit for account: %s" %
                                                self.account_1.name)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              2, # Volume
                              account=self.account_1.name,
                              domainid=self.account_1.domainid,
                              max=2
                              )

        self.debug(
            "Deploying VM for account: %s" % self.account_1.name)

        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_1.name,
                                domainid=self.account_1.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        self.debug(
            "Deploying VM for account: %s" % self.account_2.name)

        # Create VM for second account
        virtual_machine_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_2.name,
                                domainid=self.account_2.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_2)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_2.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        self.debug(
            "Create a data volume for account: %s" % self.account_1.name)
        volume_1 = Volume.create(
                                   self.apiclient,
                                   self.services["volume"],
                                   zoneid=self.zone.id,
                                   account=self.account_1.name,
                                   domainid=self.account_1.domainid,
                                   diskofferingid=self.disk_offering.id
                                   )
        self.cleanup.append(volume_1)
        # Verify Volume state
        self.assertEqual(
                            volume_1.state in [
                                                 'Allocated',
                                                 'Ready'
                                                 ],
                            True,
                            "Check Volume state is Ready or not"
                        )

        # Exception should be raised for second snapshot (account_1)
        with self.assertRaises(Exception):
            Volume.create(
                          self.apiclient,
                          self.services["volume"],
                          zoneid=self.zone.id,
                          account=self.account_1.name,
                          domainid=self.account_1.domainid,
                          diskofferingid=self.disk_offering.id
                        )

        self.debug(
            "Create a data volume for account: %s" % self.account_2.name)
        # Create volume for Account 2
        volume_2 = Volume.create(
                                   self.apiclient,
                                   self.services["volume"],
                                   zoneid=self.zone.id,
                                   account=self.account_2.name,
                                   domainid=self.account_2.domainid,
                                   diskofferingid=self.disk_offering.id
                                   )
        self.cleanup.append(volume_2)
        # Verify Volume state
        self.assertEqual(
                            volume_2.state in [
                                                 'Allocated',
                                                 'Ready'
                                                 ],
                            True,
                            "Check Volume state is Ready or not"
                        )

        self.debug(
            "Create a data volume for account: %s" % self.account_2.name)
        # Create a second volume from the ROOTDISK (Account 2)
        volume_3 = Volume.create(
                                   self.apiclient,
                                   self.services["volume"],
                                   zoneid=self.zone.id,
                                   account=self.account_2.name,
                                   domainid=self.account_2.domainid,
                                   diskofferingid=self.disk_offering.id
                                   )
        self.cleanup.append(volume_3)
        # Verify Volume state
        self.assertEqual(
                            volume_3.state in [
                                                 'Allocated',
                                                 'Ready'
                                                 ],
                            True,
                            "Check Volume state is Ready or not"
                        )
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_05_templates_per_account(self):
        """Test Templates limit per account
        """

        # Validate the following
        # 1. Set templates=1 limit for account 1.
        # 2. Try to create 2 templates in account 1. Verify account with limit
        #    should be denied to create more than 1 template.
        # 3. Try to create 2 templates in account 2. Verify account 2 should be
        #    able to create template without any error
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("Template feature is not supported on LXC")

        try:
            apiclient_account1 = self.testClient.getUserApiClient(
                                      UserName=self.account_1.name,
                                      DomainName=self.account_1.domain)

            apiclient_account2 = self.testClient.getUserApiClient(
                                      UserName=self.account_2.name,
                                      DomainName=self.account_2.domain)
            self.debug(
                       "Updating template resource limit for account: %s" %
                                                self.account_1.name)
            # Set usage_vm=1 for Account 1
            update_resource_limit(
                              self.apiclient,
                              4, # Template
                              account=self.account_1.name,
                              domainid=self.account_1.domainid,
                              max=1
                              )

            self.debug(
                "Updating volume resource limit for account: %s" %
                                                self.account_1.name)
            virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_1.name,
                                domainid=self.account_1.domainid,
                                serviceofferingid=self.service_offering.id
                                )
            self.cleanup.append(virtual_machine_1)
            # Verify VM state
            self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

            self.debug(
                "Deploying virtual machine for account: %s" %
                                                self.account_2.name)
            # Create VM for second account
            virtual_machine_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_2.name,
                                domainid=self.account_2.domainid,
                                serviceofferingid=self.service_offering.id
                                )
            self.cleanup.append(virtual_machine_2)
            # Verify VM state
            self.assertEqual(
                            virtual_machine_2.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

            virtual_machine_1.stop(self.apiclient)
            # Get the Root disk of VM
            volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=virtual_machine_1.id,
                            type='ROOT',
                            listall=True
                            )
            self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
            volume = volumes[0]

            self.debug(
                "Creating template from volume: %s" % volume.id)
            # Create a template from the ROOTDISK (Account 1)
            template_1 = Template.create(
                            apiclient_account1,
                            self.services["template"],
                            volumeid=volume.id,
                            account=self.account_1.name,
                            domainid=self.account_1.domainid,
                            )

            self.cleanup.append(template_1)
            # Verify Template state
            self.assertEqual(
                            template_1.isready,
                            True,
                            "Check Template is in ready state or not"
                        )
        except Exception as e:
            self.fail("Exception occured: %s" % e)
        # Exception should be raised for second snapshot (account_1)
        with self.assertRaises(Exception):
            Template.create(
                            apiclient_account1,
                            self.services["template"],
                            volumeid=volume.id,
                            account=self.account_1.name,
                            domainid=self.account_1.domainid,
                            )

        try:
            virtual_machine_2.stop(self.apiclient)
            # Get the Root disk of VM
            volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=virtual_machine_2.id,
                            type='ROOT',
                            listall=True
                            )
            self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
            volume = volumes[0]

            self.debug(
                "Creating template from volume: %s" % volume.id)
            # Create a snapshot from the ROOTDISK (Account 1)
            template_2 = Template.create(
                            apiclient_account2,
                            self.services["template"],
                            volumeid=volume.id,
                            account=self.account_2.name,
                            domainid=self.account_2.domainid,
                            )

            self.cleanup.append(template_2)
            # Verify Template state
            self.assertEqual(
                            template_2.isready,
                            True,
                            "Check Template is in ready state or not"
                            )
            self.debug(
                "Creating template from volume: %s" % volume.id)
            # Create a second volume from the ROOTDISK (Account 2)
            template_3 = Template.create(
                            apiclient_account2,
                            self.services["template"],
                            volumeid=volume.id,
                            account=self.account_2.name,
                            domainid=self.account_2.domainid,
                            )

            self.cleanup.append(template_3)
            # Verify Template state
            self.assertEqual(
                            template_3.isready,
                            True,
                            "Check Template is in ready state or not"
                        )
        except Exception as e:
            self.fail("Exception occured: %s" % e)
        return


class TestResourceLimitsDomain(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestResourceLimitsDomain, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["server"]["zoneid"] = cls.zone.id

        # Create Domains, Account etc
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"]
                                   )

        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.name

        # Create Service offering and disk offerings etc
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls._cleanup = [
                        cls.service_offering,
                        cls.disk_offering,
                        cls.account,
                        cls.domain
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            # Wait for VMs to expunge
            wait_for_cleanup(self.apiclient, ["expunge.delay", "expunge.interval"])
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_01_vm_per_domain(self):
        """Test VM limit per domain
        """

        # Validate the following
        # 1. Set max VM per domain to 2
        # 2. Create account and start 2 VMs. Verify VM state is Up and Running
        # 3. Try to create 3rd VM instance. The appropriate error or alert
        #    should be raised

        self.debug(
            "Updating instance resource limits for domain: %s" %
                                        self.account.domainid)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              0, # Instance
                              domainid=self.account.domainid,
                              max=2
                              )

        self.debug("Deploying VM for account: %s" % self.account.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        self.debug("Deploying VM for account: %s" % self.account.name)
        virtual_machine_2 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_2)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_2.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        # Exception should be raised for second instance
        with self.assertRaises(Exception):
            VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account_1.name,
                                domainid=self.account.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_01_publicip_per_domain(self):
        """Test Public IP limit per domain
        """

        # Validate the following
        # 1. set max no of IPs per domain to 2.
        # 2. Create an account in this domain
        # 3. Create 1 VM in this domain
        # 4. Acquire 1 IP in the domain. IP should be successfully acquired
        # 5. Try to acquire 3rd IP in this domain. It should give the user an
        #    appropriate error and an alert should be generated.

        self.debug(
            "Updating public IP resource limits for domain: %s" %
                                        self.account.domainid)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              1, # Public Ip
                              domainid=self.account.domainid,
                              max=2
                              )

        self.debug("Deploying VM for account: %s" % self.account.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
        self.debug("Associating public IP for account: %s" % self.account.name)
        public_ip_1 = PublicIPAddress.create(
                                           self.apiclient,
                                           virtual_machine_1.account,
                                           virtual_machine_1.zoneid,
                                           virtual_machine_1.domainid,
                                           self.services["server"]
                                           )
        self.cleanup.append(public_ip_1)
        # Verify Public IP state
        self.assertEqual(
                            public_ip_1.ipaddress.state in [
                                                 'Allocated',
                                                 'Allocating'
                                                 ],
                            True,
                            "Check Public IP state is allocated or not"
                        )

        # Exception should be raised for second Public IP
        with self.assertRaises(Exception):
            PublicIPAddress.create(
                                   self.apiclient,
                                   virtual_machine_1.account,
                                   virtual_machine_1.zoneid,
                                   virtual_machine_1.domainid,
                                   self.services["server"]
                                   )
        return

    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_03_snapshots_per_domain(self):
        """Test Snapshot limit per domain
        """

        # Validate the following
        # 1. set max no of snapshots per domain to 1.
        # 2. Create an account in this domain
        # 3. Create 1 VM in this domain
        # 4. Create one snapshot in the domain. Snapshot should be successfully
        #    created
        # 5. Try to create another snapshot in this domain. It should give the
        #    user an appropriate error and an alert should be generated.
        if self.hypervisor.lower() in ['hyperv', 'lxc']:
            self.skipTest("Snapshots feature is not supported on Hyper-V and LXC")
        self.debug(
            "Updating snapshot resource limits for domain: %s" %
                                        self.account.domainid)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              3, # Snapshot
                              domainid=self.account.domainid,
                              max=1
                              )

        self.debug("Deploying VM for account: %s" % self.account.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        # Get the Root disk of VM
        volumes = list_volumes(
                            self.apiclient,
                            virtualmachineid=virtual_machine_1.id,
                            type='ROOT',
                            listall=True
                            )
        self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
        volume = volumes[0]

        self.debug("Creating snapshot from volume: %s" % volumes[0].id)
        # Create a snapshot from the ROOTDISK
        snapshot_1 = Snapshot.create(self.apiclient,
                            volume.id,
                            account=self.account.name,
                            domainid=self.account.domainid,
                            )
        self.cleanup.append(snapshot_1)
        # Verify Snapshot state
        self.assertEqual(
                            snapshot_1.state in [
                                                 'BackedUp',
                                                 'CreatedOnPrimary',
                                                 'Allocated'
                                                 ],
                            True,
                            "Snapshot state is not valid, it is %s" % snapshot_1.state
                        )

        # Exception should be raised for second snapshot
        with self.assertRaises(Exception):
            Snapshot.create(self.apiclient,
                            volume.id,
                            account=self.account.name,
                            domainid=self.account.domainid,
                            )
        return

    @attr(tags=["advanced", "advancedns", "simulator"], required_hardware="false")
    def test_04_volumes_per_domain(self):
        """Test Volumes limit per domain
        """

        # Validate the following
        # 1. set max no of volume per domain to 1.
        # 2. Create an account in this domain
        # 3. Create 1 VM in this domain
        # 4. Try to Create another VM in the domain. It should give the user an
        #    appropriate error that Volume limit is exhausted and an alert
        #    should be generated.

        self.debug(
            "Updating volume resource limits for domain: %s" %
                                        self.account.domainid)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
                              self.apiclient,
                              2, # Volume
                              domainid=self.account.domainid,
                              max=1
                              )

        self.debug("Deploying VM for account: %s" % self.account.name)
        virtual_machine_1 = VirtualMachine.create(
                                self.apiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(virtual_machine_1)
        # Verify VM state
        self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )

        # Exception should be raised for second volume
        with self.assertRaises(Exception):
            Volume.create(
                          self.apiclient,
                          self.services["volume"],
                          zoneid=self.zone.id,
                          account=self.account.name,
                          domainid=self.account.domainid,
                          diskofferingid=self.disk_offering.id
                        )
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_05_templates_per_domain(self):
        """Test Templates limit per domain
        """

        # Validate the following
        # 1. set max no of templates per domain to 2.
        # 2. Create an account in this domain
        # 3. Create 2 templates in this domain. Both template should be in
        #    ready state
        # 4. Try create 3rd template in the domain. It should give the user an
        #    appropriate error and an alert should be generated.
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("Template feature is not supported on LXC")


        try:
            userapiclient = self.testClient.getUserApiClient(
                                      UserName=self.account.name,
                                      DomainName=self.account.domain)

            # Set usage_vm=1 for Account 1
            update_resource_limit(
                              self.apiclient,
                              2, # Volume
                              domainid=self.account.domainid,
                              max=5
                              )

            # Set usage_vm=1 for Account 1
            update_resource_limit(
                              self.apiclient,
                              4, # Template
                              domainid=self.account.domainid,
                              max=2
                              )
            virtual_machine_1 = VirtualMachine.create(
                                userapiclient,
                                self.services["server"],
                                templateid=self.template.id,
                                accountid=self.account.name,
                                domainid=self.account.domainid,
                                serviceofferingid=self.service_offering.id
                                )
            self.cleanup.append(virtual_machine_1)
            # Verify VM state
            self.assertEqual(
                            virtual_machine_1.state,
                            'Running',
                            "Check VM state is Running or not"
                        )
            virtual_machine_1.stop(userapiclient)
            # Get the Root disk of VM
            volumes = list_volumes(
                            userapiclient,
                            virtualmachineid=virtual_machine_1.id,
                            type='ROOT',
                            listall=True
                            )
            self.assertEqual(
                        isinstance(volumes, list),
                        True,
                        "Check for list volume response return valid data"
                        )
            volume = volumes[0]

            self.debug("Creating template from volume: %s" % volume.id)
            # Create a template from the ROOTDISK
            template_1 = Template.create(
                            userapiclient,
                            self.services["template"],
                            volumeid=volume.id,
                            account=self.account.name,
                            domainid=self.account.domainid,
                            )

            self.cleanup.append(template_1)
            # Verify Template state
            self.assertEqual(
                            template_1.isready,
                            True,
                            "Check Template is in ready state or not"
                        )
            self.debug("Creating template from volume: %s" % volume.id)
            # Create a template from the ROOTDISK
            template_2 = Template.create(
                            userapiclient,
                            self.services["template"],
                            volumeid=volume.id,
                            account=self.account.name,
                            domainid=self.account.domainid,
                            )

            self.cleanup.append(template_2)
            # Verify Template state
            self.assertEqual(
                            template_2.isready,
                            True,
                            "Check Template is in ready state or not"
                        )
        except Exception as e:
            self.fail("Exception occured: %s" % e)

        # Exception should be raised for second template
        with self.assertRaises(Exception):
            Template.create(
                            userapiclient,
                            self.services["template"],
                            volumeid=volume.id,
                            account=self.account.name,
                            domainid=self.account.domainid,
                            )
        return


class TestMaxAccountNetworks(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestMaxAccountNetworks, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.domain = get_domain(cls.api_client)
        cls.services['mode'] = cls.zone.networktype
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.network_offering = NetworkOffering.create(
                                            cls.api_client,
                                            cls.services["network_offering"],
                                            conservemode=True
                                            )
        # Enable Network offering
        cls.network_offering.update(cls.api_client, state='Enabled')

        cls._cleanup = [
                        cls.service_offering,
                        cls.network_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.account.delete(self.apiclient)
            interval = list_configurations(
                                    self.apiclient,
                                    name='account.cleanup.interval'
                                    )
            # Sleep to ensure that all resources are deleted
            time.sleep(int(interval[0].value) * 2)
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "simulator",
                "api"])
    def test_maxAccountNetworks(self):
        """Test Limit number of guest account specific networks
        """

        # Steps for validation
        # 1. Fetch max.account.networks from configurations
        # 2. Create an account. Create account more that max.accout.network
        # 3. Create network should fail

        config = Configurations.list(
                                    self.apiclient,
                                    name='max.account.networks',
                                    listall=True
                                    )
        self.assertEqual(
                isinstance(config, list),
                True,
                "List configurations should have max.account.networks"
                )

        config_value = int(config[0].value)
        self.debug("max.account.networks: %s" % config_value)

        for ctr in range(config_value):
            # Creating network using the network offering created
            self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
            network = Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
            self.debug("Created network with ID: %s" % network.id)
        self.debug(
            "Creating network in account already having networks : %s" %
                                                            config_value)

        with self.assertRaises(Exception):
            Network.create(
                                    self.apiclient,
                                    self.services["network"],
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    networkofferingid=self.network_offering.id,
                                    zoneid=self.zone.id
                                    )
        self.debug('Create network failed (as expected)')
        return
