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
""" P1 tests for Volumes
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (listHypervisorCapabilities,
                                  attachIso,
                                  deleteVolume
                                  )
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (Account,
                             ServiceOffering,
                             VirtualMachine,
                             Volume,
                             Host,
                             Iso,
                             Configurations,
                             DiskOffering,
                             Domain,
                             StoragePool)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               get_pod,
                               find_storage_pool_type,
                               update_resource_limit)
from marvin.codes import PASS
# Import System modules
import time


class TestAttachVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAttachVolume, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                cls.unsupportedStorageType = True
                return
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        # get max data volumes limit based on the hypervisor type and version
        listHost = Host.list(
            cls.api_client,
            type='Routing',
            zoneid=cls.zone.id,
            podid=cls.pod.id,
        )
        ver = listHost[0].hypervisorversion
        hv = listHost[0].hypervisor
        cmd = listHypervisorCapabilities.listHypervisorCapabilitiesCmd()
        cmd.hypervisor = hv
        res = cls.api_client.listHypervisorCapabilities(cmd)
        cls.debug('Hypervisor Capabilities: {}'.format(res))
        for i in range(len(res)):
            if res[i].hypervisorversion == ver:
                break
        cls.max_data_volumes = int(res[i].maxdatavolumeslimit)
        if 'kvm' in cls.hypervisor:
            cls.max_data_volumes = 24
        cls.debug('max data volumes:{}'.format(cls.max_data_volumes))
        cls.testdata["volume"]["max"] = cls.max_data_volumes
        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        update_resource_limit(
                              cls.api_client,
                              2, # Instance
                              account=cls.account.name,
                              domainid=cls.account.domainid,
                              max=cls.max_data_volumes + 1
                              )

        cls._cleanup.append(cls.account)

        cls.debug('max data volumes:{}'.format(cls.max_data_volumes))
        #cls.services["volume"]["max"] = cls.max_data_volumes
        # Create VMs, NAT Rules etc

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.testdata["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
            templateid=cls.template.id,
            zoneid=cls.zone.id
        )

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedStorageType:
            self.skipTest("Skipping because of unsupported storage type")

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
            # raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestAttachVolume,
                cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @attr(tags=["advanced", "advancedns", "needle"])
    def test_01_volume_attach(self):
        """Test Attach volumes (max capacity)
        """
        # Validate the following
        # 1. Deploy a vm and create 5 data disk
        # 2. Attach all the created Volume to the vm.
        # 3. Reboot the VM. VM should be successfully rebooted
        # 4. Stop the VM. Stop VM should be successful
        # 5. Start The VM. Start VM should be successful

        # Create 5 volumes and attach to VM

        for i in range(self.max_data_volumes):
            volume = Volume.create(
                self.apiclient,
                self.testdata["volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering.id
            )
            # Check List Volume response for newly created volume
            list_volume_response = Volume.list(
                self.apiclient,
                id=volume.id
            )
            self.assertNotEqual(
                list_volume_response,
                None,
                "Check if volume exists in ListVolumes"
            )
            # Attach volume to VM
            self.virtual_machine.attach_volume(
                self.apiclient,
                volume
            )
        # Check all volumes attached to same VM
        list_volume_response = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='DATADISK',
            listall=True
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes")
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list volumes response for valid list")
        self.assertEqual(
            len(list_volume_response),
            self.max_data_volumes,
            "Volumes attached to the VM %s. Expected %s" %
            (len(list_volume_response),
             self.max_data_volumes))
        self.debug("Rebooting the VM: %s" % self.virtual_machine.id)
        # Reboot VM
        self.virtual_machine.reboot(self.apiclient)

        vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id,
        )
        # Verify VM response to check whether VM deployment was successful
        self.assertNotEqual(
            len(vm_response),
            0,
            "Check VMs available in List VMs response"
        )
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM response for valid list"
        )
        vm = vm_response[0]
        self.assertEqual(
            vm.state,
            'Running',
            "Check the state of VM"
        )

        # Stop VM
        self.virtual_machine.stop(self.apiclient)

        # Start VM
        self.virtual_machine.start(self.apiclient)
        # Sleep to ensure that VM is in ready state
        time.sleep(self.testdata["sleep"])

        vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id,
        )
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM response for valid list"
        )

        # Verify VM response to check whether VM deployment was successful
        self.assertNotEqual(
            len(vm_response),
            0,
            "Check VMs available in List VMs response"
        )

        vm = vm_response[0]
        self.assertEqual(
            vm.state,
            'Running',
            "Check the state of VM"
        )
        return

    @attr(tags=["advanced", "advancedns"])
    def test_02_volume_attach_max(self):
        """Test attach volumes (more than max) to an instance
        """

        # Validate the following
        # 1. Attach one more data volume to VM (Already 5 attached)
        # 2. Attach volume should fail

        # Create a volume and attach to VM

        # Update limit so that account could create one more volume
        if 'kvm' in self.hypervisor:
            update_resource_limit(
            self.api_client,
            2,  # Instance
            account=self.account.name,
            domainid=self.account.domainid,
            max=32
        )
        # Attach volume to VM
        with self.assertRaises(Exception):
            for i in range(self.max_data_volumes):
                volume = Volume.create(
                    self.apiclient,
                    self.testdata["volume"],
                    zoneid=self.zone.id,
                    account=self.account.name,
                    domainid=self.account.domainid,
                    diskofferingid=self.disk_offering.id
                )
                # Check List Volume response for newly created volume
                list_volume_response = Volume.list(
                    self.apiclient,
                    id=volume.id
                )
                self.assertNotEqual(
                    list_volume_response,
                    None,
                    "Check if volume exists in ListVolumes"
                )
                # Attach volume to VM
                self.virtual_machine.attach_volume(
                    self.apiclient,
                    volume
                )
        return


class TestAttachDetachVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAttachDetachVolume, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                cls.unsupportedStorageType = True
                return
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        # get max data volumes limit based on the hypervisor type and version
        listHost = Host.list(
            cls.api_client,
            type='Routing',
            zoneid=cls.zone.id,
            podid=cls.pod.id,
        )
        ver = listHost[0].hypervisorversion
        hv = listHost[0].hypervisor
        cmd = listHypervisorCapabilities.listHypervisorCapabilitiesCmd()
        cmd.hypervisor = hv  # cls.services["virtual_machine"]["hypervisor"]
        res = cls.api_client.listHypervisorCapabilities(cmd)
        cls.debug('Hypervisor Capabilities: {}'.format(res))
        for i in range(len(res)):
            if res[i].hypervisorversion == ver:
                break
        cls.max_data_volumes = int(res[i].maxdatavolumeslimit)
        if 'kvm' in cls.hypervisor:
            cls.max_data_volumes = 24
        cls.debug('max data volumes:{}'.format(cls.max_data_volumes))
        cls.testdata["volume"]["max"] = cls.max_data_volumes
        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        update_resource_limit(
                              cls.api_client,
                              2, # Instance
                              account=cls.account.name,
                              domainid=cls.account.domainid,
                              max=cls.max_data_volumes + 1
                              )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.testdata["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
        )

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedStorageType:
            self.skipTest("RBD storage type is required for data volumes for LXC")

    def tearDown(self):
        # Clean up, terminate the created volumes
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(
                TestAttachDetachVolume,
                cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    @attr(tags=["advanced", "advancedns"])
    def test_01_volume_attach_detach(self):
        """Test Volume attach/detach to VM (5 data volumes)
        """

        # Validate the following
        # 1. Deploy a vm and create 5 data disk
        # 2. Attach all the created Volume to the vm.
        # 3. Detach all the volumes attached.
        # 4. Reboot the VM. VM should be successfully rebooted
        # 5. Stop the VM. Stop VM should be successful
        # 6. Start The VM. Start VM should be successful

        volumes = []
        # Create 5 volumes and attach to VM
        for i in range(self.max_data_volumes):
            volume = Volume.create(
                self.apiclient,
                self.testdata["volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering.id
            )
            self.cleanup.append(volume)
            volumes.append(volume)

            # Check List Volume response for newly created volume
            list_volume_response = Volume.list(
                self.apiclient,
                id=volume.id
            )
            self.assertNotEqual(
                list_volume_response,
                None,
                "Check if volume exists in ListVolumes")
            self.assertEqual(
                isinstance(list_volume_response, list),
                True,
                "Check list volumes response for valid list")
            # Attach volume to VM
            self.virtual_machine.attach_volume(
                self.apiclient,
                volume
            )

        # Check all volumes attached to same VM
        list_volume_response = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='DATADISK',
            listall=True
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list volumes response for valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            self.max_data_volumes,
            "Volumes attached to the VM %s. Expected %s" %
            (len(list_volume_response),
             self.max_data_volumes))

        # Detach all volumes from VM
        for volume in volumes:
            self.virtual_machine.detach_volume(
                self.apiclient,
                volume
            )
        # Reboot VM
        self.debug("Rebooting the VM: %s" % self.virtual_machine.id)
        self.virtual_machine.reboot(self.apiclient)
        # Sleep to ensure that VM is in ready state
        time.sleep(self.testdata["sleep"])

        vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id,
        )
        # Verify VM response to check whether VM deployment was successful
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM response for valid list"
        )

        self.assertNotEqual(
            len(vm_response),
            0,
            "Check VMs available in List VMs response"
        )
        vm = vm_response[0]
        self.assertEqual(
            vm.state,
            'Running',
            "Check the state of VM"
        )

        # Stop VM
        self.virtual_machine.stop(self.apiclient)

        # Start VM
        self.virtual_machine.start(self.apiclient)
        # Sleep to ensure that VM is in ready state
        time.sleep(self.testdata["sleep"])

        vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id,
        )
        # Verify VM response to check whether VM deployment was successful
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM response for valid list"
        )
        self.assertNotEqual(
            len(vm_response),
            0,
            "Check VMs available in List VMs response"
        )
        vm = vm_response[0]
        self.assertEqual(
            vm.state,
            'Running',
            "Check the state of VM"
        )

        return
    
    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_02_root_volume_attach_detach(self):
        """Test Root Volume attach/detach to VM
        """

        # Validate the following
        # 1. Deploy a VM
        # 2. Verify that we are testing a supported hypervisor
        # 3. Check for root volume
        # 4. Stop VM
        # 5. Detach root volume
        # 6. Verify root volume detached
        # 7. Attach root volume
        # 8. Start VM
        
        # Verify we are using a supported hypervisor
        if (self.hypervisor.lower() == 'vmware'
                or self.hypervisor.lower() == 'kvm'
                or self.hypervisor.lower() == 'simulator'
                or self.hypervisor.lower() == 'xenserver'):

                # Check for root volume
                root_volume_response = Volume.list(
                    self.apiclient,
                    virtualmachineid=self.virtual_machine.id,
                    type='ROOT',
                    listall=True
                )

                self.assertEqual(
                    validateList(root_volume_response)[0],
                    PASS,
                    "Invalid response returned for root volume list"
                )

                # Grab the root volume for later use
                root_volume = root_volume_response[0]

                # Stop VM
                self.debug("Stopping the VM: %s" % self.virtual_machine.id)
                self.virtual_machine.stop(self.apiclient)

                vm_response = VirtualMachine.list(
                    self.apiclient,
                    id=self.virtual_machine.id,
                )

                # Ensure that vm_response is a valid list
                self.assertEqual(
                    validateList(vm_response)[0],
                    PASS,
                    "Invalid response returned for vm_response list"
                )

                vm = vm_response[0]
                self.assertEqual(
                    vm.state,
                    'Stopped',
                    "Check the state of VM"
                )

                # Detach root volume from VM
                self.virtual_machine.detach_volume(
                    self.apiclient,
                    root_volume
                )

                # Verify that root disk is gone
                no_root_volume_response = Volume.list(
                    self.apiclient,
                    virtualmachineid=self.virtual_machine.id,
                    type='ROOT',
                    listall=True
                )

                self.assertEqual(
                    no_root_volume_response,
                    None,
                    "Check if root volume exists in ListVolumes"
                )

                # Attach root volume to VM
                self.virtual_machine.attach_volume(
                    self.apiclient,
                    root_volume,
                    0
                )

                # Check for root volume
                new_root_volume_response = Volume.list(
                    self.apiclient,
                    virtualmachineid=self.virtual_machine.id,
                    type='ROOT',
                    listall=True
                )

                # Ensure that new_root_volume_response is a valid list
                self.assertEqual(
                    validateList(new_root_volume_response)[0],
                    PASS,
                    "Invalid response returned for new_root_volume_response list"
                )

                # Start VM
                self.virtual_machine.start(self.apiclient)

                vm_response = VirtualMachine.list(
                    self.apiclient,
                    id=self.virtual_machine.id,
                )

                # Verify VM response to check whether VM deployment was successful
                self.assertEqual(
                    validateList(vm_response)[0],
                    PASS,
                    "Invalid response returned for vm_response list during VM start up"
                )

                vm = vm_response[0]
                self.assertEqual(
                    vm.state,
                    'Running',
                    "Ensure the state of VM is running"
                )
                
        else:
            self.skipTest("Root Volume attach/detach is not supported on %s " % self.hypervisor)
        return


class TestAttachVolumeISO(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAttachVolumeISO, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(cls.api_client, cls.zone.id)
        cls.testdata['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                cls.unsupportedStorageType = True
                return
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["iso"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        # get max data volumes limit based on the hypervisor type and version
        listHost = Host.list(
            cls.api_client,
            type='Routing',
            zoneid=cls.zone.id,
            podid=cls.pod.id,
        )
        ver = listHost[0].hypervisorversion
        hv = listHost[0].hypervisor
        cmd = listHypervisorCapabilities.listHypervisorCapabilitiesCmd()
        cmd.hypervisor = hv
        res = cls.api_client.listHypervisorCapabilities(cmd)
        cls.debug('Hypervisor Capabilities: {}'.format(res))
        for i in range(len(res)):
            if res[i].hypervisorversion == ver:
                break
        cls.max_data_volumes = int(res[i].maxdatavolumeslimit)
        if 'kvm' in cls.hypervisor:
            cls.max_data_volumes = 24
        cls.debug('max data volumes:{}'.format(cls.max_data_volumes))
        cls.testdata["volume"]["max"] = cls.max_data_volumes
        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        update_resource_limit(
            cls.api_client,
            2,  # Instance
            account=cls.account.name,
            domainid=cls.account.domainid,
            max=cls.max_data_volumes + 1
        )
        cls.debug('max data volumes:{}'.format(cls.max_data_volumes))
        cls.testdata["volume"]["max"] = cls.max_data_volumes
        # Create VMs, NAT Rules etc

        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.testdata["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
        )

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedStorageType:
            self.skipTest("RBD storage type is required for data volumes for LXC")

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_01_volume_iso_attach(self):
        """Test Volumes and ISO attach
        """

        # Validate the following
        # 1. Create and attach 5 data volumes to VM
        # 2. Create an ISO. Attach it to VM instance
        # 3. Verify that attach ISO is successful

        # Create 5 volumes and attach to VM
        if self.hypervisor.lower() in ["lxc"]:
            self.skipTest("attach ISO is not supported on LXC")
        for i in range(self.max_data_volumes):
            volume = Volume.create(
                self.apiclient,
                self.testdata["volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering.id
            )
            self.debug("Created volume: %s for account: %s" % (
                volume.id,
                self.account.name
            ))
            # Check List Volume response for newly created volume
            list_volume_response = Volume.list(
                self.apiclient,
                id=volume.id
            )
            self.assertNotEqual(
                list_volume_response,
                None,
                "Check if volume exists in ListVolumes"
            )
            self.assertEqual(
                isinstance(list_volume_response, list),
                True,
                "Check list volumes response for valid list"
            )
            # Attach volume to VM
            self.virtual_machine.attach_volume(
                self.apiclient,
                volume
            )

        # Check all volumes attached to same VM
        list_volume_response = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='DATADISK',
            listall=True
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list volumes response for valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            self.max_data_volumes,
            "Volumes attached to the VM %s. Expected %s" %
            (len(list_volume_response),
             self.max_data_volumes))
        # Create an ISO and attach it to VM
        iso = Iso.create(
            self.apiclient,
            self.testdata["iso"],
            account=self.account.name,
            domainid=self.account.domainid,
        )
        self.debug("Created ISO with ID: %s for account: %s" % (
            iso.id,
            self.account.name
        ))

        try:
            self.debug("Downloading ISO with ID: %s" % iso.id)
            iso.download(self.apiclient)
        except Exception as e:
            self.fail("Exception while downloading ISO %s: %s"
                      % (iso.id, e))

        # Attach ISO to virtual machine
        self.debug("Attach ISO ID: %s to VM: %s" % (
            iso.id,
            self.virtual_machine.id
        ))
        cmd = attachIso.attachIsoCmd()
        cmd.id = iso.id
        cmd.virtualmachineid = self.virtual_machine.id
        self.apiclient.attachIso(cmd)

        # Verify ISO is attached to VM
        vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id,
        )
        # Verify VM response to check whether VM deployment was successful
        self.assertEqual(
            isinstance(vm_response, list),
            True,
            "Check list VM response for valid list"
        )

        self.assertNotEqual(
            len(vm_response),
            0,
            "Check VMs available in List VMs response"
        )
        vm = vm_response[0]
        self.assertEqual(
            vm.isoid,
            iso.id,
            "Check ISO is attached to VM or not"
        )
        return


class TestVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVolumes, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                cls.unsupportedStorageType = True
                return
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        cls.testdata["virtual_machine"][
            "diskofferingid"] = cls.disk_offering.id

        # Create VMs, VMs etc
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.testdata["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id,
        )

        cls.volume = Volume.create(
            cls.api_client,
            cls.testdata["volume"],
            zoneid=cls.zone.id,
            account=cls.account.name,
            domainid=cls.account.domainid,
            diskofferingid=cls.disk_offering.id
        )

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedStorageType:
            self.skipTest("RBD storage type is required for data volumes for LXC")

    def tearDown(self):
        # Clean up, terminate the created volumes
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_01_attach_volume(self):
        """Attach a created Volume to a Running VM
        """
        # Validate the following
        # 1. Create a data volume.
        # 2. List Volumes should not have vmname and virtualmachineid fields in
        #    response before volume attach (to VM)
        # 3. Attch volume to VM. Attach volume should be successful.
        # 4. List Volumes should have vmname and virtualmachineid fields in
        #    response before volume attach (to VM)

        # Check the list volumes response for vmname and virtualmachineid
        list_volume_response = Volume.list(
            self.apiclient,
            id=self.volume.id
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list volumes response for valid list"
        )
        volume = list_volume_response[0]

        self.assertEqual(
            volume.type,
            'DATADISK',
            "Check volume type from list volume response"
        )

        self.assertEqual(
            hasattr(volume, 'vmname'),
            True,
            "Check whether volume has vmname field"
        )
        self.assertEqual(
            hasattr(volume, 'virtualmachineid'),
            True,
            "Check whether volume has virtualmachineid field"
        )

        # Attach volume to VM
        self.debug("Attach volume: %s to VM: %s" % (
            self.volume.id,
            self.virtual_machine.id
        ))
        self.virtual_machine.attach_volume(self.apiclient, self.volume)

        # Check all volumes attached to same VM
        list_volume_response = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='DATADISK',
            listall=True
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list volumes response for valid list"
        )
        volume = list_volume_response[0]
        self.assertEqual(
            volume.vmname,
            self.virtual_machine.name,
            "Check virtual machine name in list volumes response"
        )
        self.assertEqual(
            volume.virtualmachineid,
            self.virtual_machine.id,
            "Check VM ID in list Volume response"
        )
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_02_detach_volume(self):
        """Detach a Volume attached to a VM
        """

        # Validate the following
        # 1. Data disk should be detached from instance
        # 2. Listvolumes should not have vmname and virtualmachineid fields for
        #    that volume.

        self.debug("Detach volume: %s to VM: %s" % (
            self.volume.id,
            self.virtual_machine.id
        ))
        self.virtual_machine.detach_volume(self.apiclient, self.volume)

        # Sleep to ensure the current state will reflected in other calls
        time.sleep(self.testdata["sleep"])

        list_volume_response = Volume.list(
            self.apiclient,
            id=self.volume.id
        )
        self.assertNotEqual(
            list_volume_response,
            None,
            "Check if volume exists in ListVolumes"
        )
        self.assertEqual(
            isinstance(list_volume_response, list),
            True,
            "Check list volumes response for valid list"
        )
        volume = list_volume_response[0]
        self.assertEqual(
            volume.virtualmachineid,
            None,
            "Check if volume state (detached) is reflected"
        )

        self.assertEqual(
            volume.vmname,
            None,
            "Check if volume state (detached) is reflected"
        )
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_03_delete_detached_volume(self):
        """Delete a Volume unattached to an VM
        """
        # Validate the following
        # 1. volume should be deleted successfully and listVolume should not
        #    contain the deleted volume details.

        self.debug("Deleting volume: %s" % self.volume.id)
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = self.volume.id
        self.apiclient.deleteVolume(cmd)

        # Sleep to ensure the current state will reflected in other calls
        time.sleep(self.testdata["sleep"])

        list_volume_response = Volume.list(
            self.apiclient,
            id=self.volume.id,
        )
        self.assertEqual(
            list_volume_response,
            None,
            "Volume %s was not deleted" % self.volume.id
        )
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "simulator",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_create_volume_under_domain(self):
        """Create a volume under a non-root domain as non-root-domain user

        1. Create a domain under ROOT
        2. Create a user within this domain
        3. As user in step 2. create a volume with standard disk offering
        4. Ensure the volume is created in the domain and available to the
           user in his listVolumes call
        """
        dom = Domain.create(
            self.apiclient,
            services={},
            name="NROOT",
            parentdomainid=self.domain.id
        )
        self.cleanup.append(dom)
        self.assertTrue(dom is not None, msg="Domain creation failed")

        domuser = Account.create(
            apiclient=self.apiclient,
            services=self.testdata["account"],
            admin=False,
            domainid=dom.id
        )
        self.cleanup.insert(-2, domuser)
        self.assertTrue(domuser is not None)

        domapiclient = self.testClient.getUserApiClient(
            UserName=domuser.name,
            DomainName=dom.name)

        diskoffering = DiskOffering.list(self.apiclient)
        self.assertTrue(
            isinstance(
                diskoffering,
                list),
            msg="DiskOffering list is not a list?")
        self.assertTrue(
            len(diskoffering) > 0,
            "no disk offerings in the deployment")

        vol = Volume.create(
            domapiclient,
            services=self.testdata["volume"],
            zoneid=self.zone.id,
            account=domuser.name,
            domainid=dom.id,
            diskofferingid=[x for x in diskoffering if not x.iscustomized][0].id
        )
        self.assertTrue(
            vol is not None, "volume creation fails in domain %s as user %s" %
            (dom.name, domuser.name))

        listed_vol = Volume.list(domapiclient, id=vol.id)
        self.assertTrue(
            listed_vol is not None and isinstance(
                listed_vol,
                list),
            "invalid response from listVolumes for volume %s" %
            vol.id)
        self.assertTrue(
            listed_vol[0].id == vol.id,
            "Volume returned by list volumes %s not matching with queried\
                    volume %s in domain %s" %
            (listed_vol[0].id,
                vol.id,
                dom.name))


class TestDeployVmWithCustomDisk(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestDeployVmWithCustomDisk,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                cls.unsupportedStorageType = True
                return
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"],
            custom=True
        )
        cls._cleanup.append(cls.disk_offering)
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedStorageType:
            self.skipTest("RBD storage type is required for data volumes for LXC")

    @attr(tags=["advanced", "configuration", "advancedns", "simulator",
                "api", "basic", "eip", "sg"])
    def test_deployVmWithCustomDisk(self):
        """Test custom disk sizes beyond range
        """
        # Steps for validation
        # 1. listConfigurations - custom.diskoffering.size.min
        #    and custom.diskoffering.size.max
        # 2. deployVm with custom disk offering size < min
        # 3. deployVm with custom disk offering min< size < max
        # 4. deployVm with custom disk offering size > max
        # Validate the following
        # 2. and 4. of deploy VM should fail.
        #    Only case 3. should succeed.
        #    cleanup all created data disks from the account

        config = Configurations.list(
            self.apiclient,
            name="custom.diskoffering.size.min"
        )
        self.assertEqual(
            isinstance(config, list),
            True,
            "custom.diskoffering.size.min should be present in global config"
        )
        # minimum size of custom disk (in GBs)
        min_size = int(config[0].value)
        self.debug("custom.diskoffering.size.min: %s" % min_size)

        config = Configurations.list(
            self.apiclient,
            name="custom.diskoffering.size.max"
        )
        self.assertEqual(
            isinstance(config, list),
            True,
            "custom.diskoffering.size.min should be present in global config"
        )
        # maximum size of custom disk (in GBs)
        max_size = int(config[0].value)
        self.debug("custom.diskoffering.size.max: %s" % max_size)

        self.debug("Creating a volume with size less than min cust disk size")
        self.testdata["custom_volume"]["customdisksize"] = (min_size - 1)
        self.testdata["custom_volume"]["zoneid"] = self.zone.id
        with self.assertRaises(Exception):
            Volume.create_custom_disk(
                self.apiclient,
                self.testdata["custom_volume"],
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering.id
            )
        self.debug("Create volume failed!")

        self.debug("Creating a volume with size more than max cust disk size")
        self.testdata["custom_volume"]["customdisksize"] = (max_size + 1)
        with self.assertRaises(Exception):
            Volume.create_custom_disk(
                self.apiclient,
                self.testdata["custom_volume"],
                account=self.account.name,
                domainid=self.account.domainid,
                diskofferingid=self.disk_offering.id
            )
        self.debug("Create volume failed!")

        self.debug("Creating a volume with size more than min cust disk " +
                   "but less than max cust disk size"
                   )
        self.testdata["custom_volume"]["customdisksize"] = (min_size + 1)
        Volume.create_custom_disk(
            self.apiclient,
            self.testdata["custom_volume"],
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.debug("Create volume of cust disk size succeeded")

        return


class TestMigrateVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestMigrateVolume, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.testdata['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                cls.unsupportedStorageType = True
                return
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        cls.testdata["virtual_machine"][
            "diskofferingid"] = cls.disk_offering.id

        # Create VMs, VMs etc
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )
        cls.small_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.testdata["virtual_machine"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.small_offering.id,
            mode=cls.testdata["mode"]
        )
        cls._cleanup = [
            cls.small_offering,
            cls.account
        ]
        return

    @classmethod
    def tearDownClass(cls):
        super(TestMigrateVolume,cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        if self.unsupportedStorageType:
            self.skipTest("RBD storage type is required for data volumes for LXC")
        return

    def tearDown(self):
        super(TestMigrateVolume,self).tearDown()

    @attr(tags=["advanced", "sg", "advancedsg"], required_hardware='true')
    def test_01_migrateVolume(self):
        """
        @Desc:Volume is not retaining same uuid when migrating from one
              storage to another.
        Step1:Create a volume/data disk
        Step2:Verify UUID of the volume
        Step3:Migrate the volume to another primary storage within
              the cluster
        Step4:Migrating volume to new primary storage should succeed
        Step5:volume UUID should not change even after migration
        """
        vol = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            diskofferingid=self.disk_offering.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
        )
        self.assertIsNotNone(vol, "Failed to create volume")
        vol_res = Volume.list(
            self.apiclient,
            id=vol.id
        )
        self.assertEqual(
            validateList(vol_res)[0],
            PASS,
            "Invalid response returned for list volumes")
        vol_uuid = vol_res[0].id
        self.virtual_machine.attach_volume(
            self.apiclient,
            vol
        )

        pools = StoragePool.listForMigration(
            self.apiclient,
            id=vol.id
        )
        if not pools:
            self.skipTest(
                "No suitable storage pools found for volume migration.\
                        Skipping")
        self.assertEqual(
            validateList(pools)[0],
            PASS,
            "invalid pool response from findStoragePoolsForMigration")
        pool = pools[0]
        self.debug("Migrating Volume-ID: %s to Pool: %s" % (vol.id, pool.id))
        Volume.migrate(
            self.apiclient,
            volumeid=vol.id,
            storageid=pool.id,
            livemigrate='true'
        )

        migrated_vols = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall='true',
            type='DATADISK'
        )
        self.assertEqual(
            validateList(migrated_vols)[0],
            PASS,
            "invalid volumes response after migration")
        migrated_vol_uuid = migrated_vols[0].id
        self.assertEqual(
            vol_uuid,
            migrated_vol_uuid,
            "Volume is not retaining same uuid when migrating from one\
                    storage to another"
        )
        self.virtual_machine.detach_volume(
            self.apiclient,
            vol
        )
        self.cleanup.append(vol)
        return
