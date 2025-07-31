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
""" Tests for importVolume and unmanageVolume APIs
"""
# Import Local Modules
from marvin.cloudstackAPI import unmanageVolume, listVolumesForImport, importVolume
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.codes import FAILED
from marvin.lib.base import (Account,
                             Domain,
                             Volume,
                             ServiceOffering,
                             DiskOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain, get_zone, get_suitable_test_template)

# Import System modules
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestImportAndUnmanageVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestImportAndUnmanageVolumes, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()

        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.testClient.getHypervisorInfo().lower() != "kvm":
            raise unittest.SkipTest("This is only available for KVM")

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls._cleanup = []

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)

        template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["mode"] = cls.zone.networktype

        cls.disk_offering = DiskOffering.create(cls.apiclient,
                                                cls.services["disk_offering"])
        cls._cleanup.append(cls.disk_offering)

        cls.test_domain = Domain.create(
            cls.apiclient,
            cls.services["domain"])
        cls._cleanup.append(cls.test_domain)

        cls.test_account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.test_domain.id)
        cls._cleanup.append(cls.test_account)

        # Create VM
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.test_account.name,
            domainid=cls.test_account.domainid,
            serviceofferingid=cls.service_offering.id,
            mode=cls.services["mode"]
        )
        cls._cleanup.append(cls.virtual_machine)

        cls.virtual_machine.stop(cls.apiclient, forced=True)

    @classmethod
    def tearDownClass(cls):
        super(TestImportAndUnmanageVolumes, cls).tearDownClass()

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_01_detach_unmanage_import_volume(self):
        """Test attach/detach/unmanage/import volume
        """
        # Create DATA volume
        volume = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.test_account.name,
            domainid=self.test_account.domainid,
            diskofferingid=self.disk_offering.id
        )

        # Attach and Detach volume
        try:
            self.virtual_machine.attach_volume(self.apiclient, volume)
        except Exception as e:
            self.fail("Attach volume failed with Exception: %s" % e)

        self.virtual_machine.detach_volume(self.apiclient, volume)

        # List volume by id
        volumes = Volume.list(self.apiclient,
                              id = volume.id)
        self.assertTrue(isinstance(volumes, list),
                        "listVolumes response should return a valid list"
                        )
        self.assertTrue(len(volumes) > 0,
                        "listVolumes response should return a non-empty list"
                        )
        volume = volumes[0]

        # Unmanage volume
        cmd = unmanageVolume.unmanageVolumeCmd()
        cmd.id = volume.id
        self.apiclient.unmanageVolume(cmd)

        # List VMs for import
        cmd = listVolumesForImport.listVolumesForImportCmd()
        cmd.storageid = volume.storageid
        volumesForImport = self.apiclient.listVolumesForImport(cmd)
        self.assertTrue(isinstance(volumesForImport, list),
            "Check listVolumesForImport response returns a valid list"
        )

        # Import volume
        cmd = importVolume.importVolumeCmd()
        cmd.storageid = volume.storageid
        cmd.path = volume.path
        self.apiclient.importVolume(cmd)

        # List volume by name
        volumes = Volume.list(self.apiclient,
                              storageid = volume.storageid,
                              name=volume.path)
        self.assertTrue(isinstance(volumes, list),
                        "listVolumes response should return a valid list"
                        )
        self.assertTrue(len(volumes) > 0,
                        "listVolumes response should return a non-empty list"
                        )
