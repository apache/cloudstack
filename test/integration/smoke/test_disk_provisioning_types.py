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

from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import DiskOffering, Iso, Account, VirtualMachine, ServiceOffering, Volume
from marvin.codes import FAILED
from marvin.lib.common import list_disk_offering, get_zone, get_suitable_test_template, get_domain
from marvin.cloudstackAPI import listStoragePools, updateStorageCapabilities
from nose.plugins.attrib import attr


class TestDiskProvisioningTypes(cloudstackTestCase):

    def setUp(self):

        if self.testClient.getHypervisorInfo().lower() != "vmware":
            raise unittest.SkipTest("VMWare tests only valid on VMWare hypervisor")

        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.domain = get_domain(self.apiclient)
        self.services['mode'] = self.zone.networktype
        self.hypervisor = self.hypervisor = self.testClient.getHypervisorInfo()

        template = get_suitable_test_template(
            self.apiclient,
            self.zone.id,
            self.services["ostype"],
            self.hypervisor
        )

        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % self.services["ostype"]

        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )

        self.services["small"]["zoneid"] = self.zone.id
        self.services["small"]["template"] = template.id

        self.services["iso1"]["zoneid"] = self.zone.id

        iso = Iso.create(
            self.apiclient,
            self.services["iso1"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.cleanup = [
            self.account
        ]


    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke"], required_hardware="false")
    def test_01_vm_with_thin_disk_offering(self):
        self.runner("thin")

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke"], required_hardware="false")
    def test_02_vm_with_fat_disk_offering(self):
        self.runner("fat")

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke"], required_hardware="false")
    def test_03_vm_with_sparse_disk_offering(self):
        self.runner("sparse")

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke"], required_hardware="false")
    def test_05_update_cmd(self):
        cmd = listStoragePools.listStoragePoolsCmd()
        storagePools = self.apiclient.listStoragePools(cmd)

        for pool in storagePools:
            if pool.type == 'NetworkFilesystem':
                cmd = updateStorageCapabilities.updateStorageCapabilitiesCmd()
                cmd.id = pool.id
                response = self.apiclient.updateStorageCapabilities(cmd)
                acceleration = getattr(response[0].storagecapabilities, "HARDWARE_ACCELERATION")
                self.assertNotEqual(
                    acceleration,
                    None,
                    "Check Updated storage pool capabilities"
                )

    def runner(self, provisioning_type):
        self.services["disk_offering"]['provisioningtype'] = provisioning_type
        self.services["small"]['size'] = "1"
        disk_offering = DiskOffering.create(
            self.apiclient,
            self.services["disk_offering"],
            custom=True,
        )
        self.cleanup.append(disk_offering)

        self.debug("Created Disk offering with ID: %s" % disk_offering.id)

        self.services["service_offerings"]["small"]["provisioningtype"] = provisioning_type
        small_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["small"]
        )

        self.cleanup.append(small_offering)

        self.debug("Created service offering with ID: %s" % small_offering.id)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=small_offering.id,
            diskofferingid=disk_offering.id,
            mode=self.services["mode"]
        )

        self.debug("Created virtual machine with ID: %s" % virtual_machine.id)

        volumes = Volume.list(self.apiclient, virtualMachineId=virtual_machine.id, listAll='true')

        for volume in volumes:
            if volume["type"] == "DATADISK":
                VirtualMachine.detach_volume(virtual_machine, self.apiclient, volume)
                currentVolume = Volume({})
                currentVolume.id = volume.id
                Volume.resize(currentVolume, self.apiclient, size='2')
                VirtualMachine.attach_volume(virtual_machine, self.apiclient, volume)
