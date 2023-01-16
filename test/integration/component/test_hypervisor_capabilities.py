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
""" Test cases for Testing Hypervisor Capabilities
"""
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              validateList)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             DiskOffering,
                             VirtualMachine,
                             Volume,
                             Host,
                             VmSnapshot)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_virtual_machines,
                               list_ssvms,
                               list_routers)
from marvin.lib.decoratorGenerators import skipTestIf


from marvin.cloudstackAPI import (updateHypervisorCapabilities,
                                  listHypervisorCapabilities)

from marvin.codes import PASS


class TestHypervisorCapabilities(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestHypervisorCapabilities, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.domain = get_domain(cls.apiclient)

        cls.hypervisor = cls.testClient.getHypervisorInfo()
        # Get Zone, Domain and templates

        cls.notSupported = True
        cls._cleanup = []
        cls.hosts = Host.list(cls.apiclient, zoneid=cls.zone.id, type='Routing')
        if isinstance(cls.hosts, list) and len(cls.hosts) > 0:
            cls.host = cls.hosts[0]
            cls.notSupported = False

        cls.hypervisorversion = "default"
        if hasattr(cls.host, 'hypervisorversion'):
            cls.hypervisorversion = cls.host.hypervisorversion

        if cls.notSupported == False:
            cls.notSupported = True
            cmdList = listHypervisorCapabilities.listHypervisorCapabilitiesCmd()
            cmdList.hypervisor = cls.hypervisor
            capabilities = cls.apiclient.listHypervisorCapabilities(cmdList)
            for capability in capabilities:
                if capability.hypervisorversion == cls.hypervisorversion:
                   cls.hostCapability = capability
                   cls.notSupported = False
                   break

            if cls.notSupported == True:
                cls.hypervisorversion = "default"
                for capability in capabilities:
                    if capability.hypervisorversion == cls.hypervisorversion:
                       cls.hostCapability = capability
                       cls.notSupported = False
                       break

        if cls.notSupported == False:
            cls.template = get_template(
                cls.apiclient,
                cls.zone.id,
                cls.services["ostype"])

            # Create an account
            cls.account = Account.create(
                cls.apiclient,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)

            # Create user api client of the account
            cls.userapiclient = testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain
            )
            # Create Service offering
            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.services["service_offering"],
                hosttags="host1"
            )
            cls._cleanup.append(cls.service_offering)

            cls.disk_offering = DiskOffering.create(
                cls.apiclient,
                cls.services["disk_offering"]
            )
            cls._cleanup.append(cls.disk_offering)

        return

    @classmethod
    def tearDownClass(cls):
        super(TestHypervisorCapabilities, cls).tearDownClass()

    def setUp(self):
        self.cleanup = []

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()

    def tearDown(self):
        try:
            if self.notSupported == False:
                Host.update(self.apiclient, id=self.host.id, hosttags="")
                self.updateHostHypervisorCapability(self.hostCapability.id,
                    self.hostCapability.maxdatavolumeslimit,
                    self.hostCapability.vmsnapshotenabled)

            super(TestHypervisorCapabilities, self).tearDown()
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @skipTestIf("notSupported")
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_check_hypervisor_max_data_volume_limit(self):
        """ Test hypervisor maxdatavolumeslimit effect

        # 1. Set maxdatavolumeslimit to 1 for hypervisor
        # 2. List capabilities and verify value
        # 3. Deploy a VM and attach a volume to it
        # 4. Try attach another volume, it should fail
        # 5. Set maxdatavolumeslimit to 32 for hypervisor
        # 6. Try attach second volume, it should succeed
        """
        self.updateHostHypervisorCapability(self.hostCapability.id, 1)
        capabilities = self.listHostHypervisorCapabilities(self.hostCapability.id)
        self.assertTrue(isinstance(capabilities, list), "listHypervisorCapabilities response not a valid list")
        self.assertEqual(len(capabilities), 1, "listHypervisorCapabilities response not valid")
        self.assertEqual(capabilities[0].maxdatavolumeslimit,
            1,
            "listHypervisorCapabilities response maxdatavolumeslimit value not 1")

        Host.update(self.apiclient, id=self.host.id, hosttags="host1")
        volume_created1 = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )
        self.cleanup.append(volume_created1)
        volume_created2 = Volume.create(
            self.userapiclient,
            self.services["volume"],
            zoneid=self.zone.id,
            diskofferingid=self.disk_offering.id
        )
        self.cleanup.append(volume_created2)
        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(vm)

        vm.attach_volume(
            self.userapiclient,
            volume_created1
        )

        try:
            vm.attach_volume(
                self.userapiclient,
                volume_created2
            )
            vm.detach_volume(self.userapiclient, volume_created1)
            vm.detach_volume(self.userapiclient, volume_created2)
            self.fail("Successful to attach 2 DATA disks when max DATA disk limit was set to 1")
        except Exception as e:
            self.debug("Failed to attach 2nd DATA disk when max DATA disk limit was set to 1: %s" % e)

        self.updateHostHypervisorCapability(self.hostCapability.id, 32)

        vm.attach_volume(
            self.userapiclient,
            volume_created2
        )
        vm.stop(self.userapiclient, forced=True)
        vm.detach_volume(self.userapiclient, volume_created1)
        vm.detach_volume(self.userapiclient, volume_created2)

    @skipTestIf("notSupported")
    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_02_check_hypervisor_vm_snapshot(self):
        """ Test hypervisor vmsnapshotenabled effect

        # 1. Set vmsnapshotenabled to false for hypervisor
        # 2. List capabilities and verify value
        # 3. Deploy a VM
        # 4. Try VM snapshot, it should fail
        # 5. Set vmsnapshotenabled to true for hypervisor
        # 6. Try VM snapshot again, it should succeed
        """
        if self.hypervisor == "KVM":
            self.skipTest("Skipping test: Reason -  VM Snapshot of running VM is not supported for KVM")
        self.updateHostHypervisorCapability(self.hostCapability.id, None, False)
        capabilities = self.listHostHypervisorCapabilities(self.hostCapability.id)
        self.assertTrue(isinstance(capabilities, list), "listHypervisorCapabilities response not a valid list")
        self.assertEqual(len(capabilities), 1, "listHypervisorCapabilities response not valid")
        self.assertEqual(capabilities[0].vmsnapshotenabled,
            False,
            "listHypervisorCapabilities response vmsnapshotenabled value not False")

        Host.update(self.apiclient, id=self.host.id, hosttags="host1")
        vm = VirtualMachine.create(
            self.userapiclient,
            self.services["small"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            zoneid=self.zone.id
        )
        self.cleanup.append(vm)

        try:
            fail_snapshot = VmSnapshot.create(
                self.userapiclient,
                vmid=vm.id,
                snapshotmemory="false",
                name="Test Snapshot",
                description="Test Snapshot Desc"
            )
            self.cleanup.append(fail_snapshot)
            self.fail("Successful to take VM snapshot even when vmsnapshotenabled was set to False")
        except Exception as e:
            self.debug("Failed to take VM snapshot even vmsnapshotenabled was set to False: %s" % e)

        self.updateHostHypervisorCapability(self.hostCapability.id, None, True)

        vm_snapshot = VmSnapshot.create(
            self.userapiclient,
            vmid=vm.id,
            snapshotmemory="false",
            name="Test Snapshot",
            description="Test Snapshot Desc"
        )
        self.cleanup.append(vm_snapshot)

    def updateHostHypervisorCapability(self, id, maxDataVolumes, vmSnapshotEnabled=None):
        cmd = updateHypervisorCapabilities.updateHypervisorCapabilitiesCmd()
        cmd.id = id
        if maxDataVolumes != None:
            cmd.maxdatavolumeslimit = maxDataVolumes
        if vmSnapshotEnabled != None:
            cmd.vmsnapshotenabled = vmSnapshotEnabled
        self.apiclient.updateHypervisorCapabilities(cmd)

    def listHostHypervisorCapabilities(self, id, hypervisor=None):
        cmd = listHypervisorCapabilities.listHypervisorCapabilitiesCmd()
        cmd.id = id
        return self.apiclient.listHypervisorCapabilities(cmd)
