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
""" BVT tests for Disk offerings"""

#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr


_multiprocess_shared_ = True

class TestCreateDiskOffering(cloudstackTestCase):

    def setUp(self):
        self.services = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke", "diskencrypt"], required_hardware="false")
    def test_01_create_disk_offering(self):
        """Test to create disk offering

        # Validate the following:
        # 1. createDiskOfferings should return valid info for new offering
        # 2. The Cloud Database contains the valid information
        """
        disk_offering = DiskOffering.create(
                                        self.apiclient,
                                        self.services["disk_offering"]
                                        )
        self.cleanup.append(disk_offering)

        self.debug("Created Disk offering with ID: %s" % disk_offering.id)

        list_disk_response = list_disk_offering(
                                                self.apiclient,
                                                id=disk_offering.id
                                                )
        self.assertEqual(
                            isinstance(list_disk_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_disk_response),
                            0,
                            "Check Disk offering is created"
                        )
        disk_response = list_disk_response[0]

        self.assertEqual(
                            disk_response.displaytext,
                            self.services["disk_offering"]["displaytext"],
                            "Check server id in createServiceOffering"
                        )
        self.assertEqual(
                            disk_response.name,
                            self.services["disk_offering"]["name"],
                            "Check name in createServiceOffering"
                        )
        self.assertEqual(
            disk_response.encrypt,
            False,
            "Ensure disk encryption is false by default"
        )
        return

    @attr(hypervisor="kvm")
    @attr(tags = ["advanced", "basic", "eip", "sg", "advancedns", "simulator", "smoke"])
    def test_02_create_sparse_type_disk_offering(self):
        """Test to create  a sparse type disk offering"""

        # Validate the following:
        # 1. createDiskOfferings should return valid info for new offering
        # 2. The Cloud Database contains the valid information

        disk_offering = DiskOffering.create(
                                        self.apiclient,
                                        self.services["sparse"]
                                        )
        self.cleanup.append(disk_offering)

        self.debug("Created Disk offering with ID: %s" % disk_offering.id)

        list_disk_response = list_disk_offering(
                                                self.apiclient,
                                                id=disk_offering.id
                                                )
        self.assertEqual(
                            isinstance(list_disk_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_disk_response),
                            0,
                            "Check Disk offering is created"
                        )
        disk_response = list_disk_response[0]

        self.assertEqual(
                            disk_response.provisioningtype,
                            self.services["sparse"]["provisioningtype"],
                            "Check provisionig type in createServiceOffering"
                        )
        return


    @attr(hypervisor="kvm")
    @attr(tags = ["advanced", "basic", "eip", "sg", "advancedns", "simulator", "smoke"])
    def test_04_create_fat_type_disk_offering(self):
        """Test to create a sparse type disk offering"""

        # Validate the following:
        # 1. createDiskOfferings should return valid info for new offering
        # 2. The Cloud Database contains the valid information

        disk_offering = DiskOffering.create(
                                        self.apiclient,
                                        self.services["fat"]
                                        )
        self.cleanup.append(disk_offering)

        self.debug("Created Disk offering with ID: %s" % disk_offering.id)

        list_disk_response = list_disk_offering(
                                                self.apiclient,
                                                id=disk_offering.id
                                                )
        self.assertEqual(
                            isinstance(list_disk_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_disk_response),
                            0,
                            "Check Disk offering is created"
                        )
        disk_response = list_disk_response[0]

        self.assertEqual(
                            disk_response.provisioningtype,
                            self.services["fat"]["provisioningtype"],
                            "Check provisionig type in createServiceOffering"
                        )
        return

    @attr(hypervisor="kvm")
    @attr(tags = ["advanced", "basic", "eip", "sg", "advancedns", "simulator", "smoke"])
    def test_05_create_burst_type_disk_offering(self):
        """Test to create a disk offering with io bursting enabled"""

        # Validate the following:
        # 1. createDiskOfferings should return valid info for new offering with io burst settings
        # 2. The Cloud Database contains the valid information

        burstBits = {}
        for key in self.services["ioburst"]:
            if str(key).startswith("bytes") or str(key).startswith("iops"):
                burstBits[key] = self.services["ioburst"][key]

        disk_offering = DiskOffering.create(
            self.apiclient,
            self.services["ioburst"],
            None,
            False,
            None,
            **burstBits
        )
        self.cleanup.append(disk_offering)

        self.debug("Created Disk offering with ID: %s" % disk_offering.id)

        list_disk_response = list_disk_offering(
            self.apiclient,
            id=disk_offering.id
        )
        self.assertEqual(
            isinstance(list_disk_response, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_disk_response),
            0,
            "Check Disk offering is created"
        )
        disk_response = list_disk_response[0]

        for key in burstBits:
            k = str(key)
            mapped = 'disk' + k[:1].upper() + k[1:]
            self.assertEqual(
                disk_response[mapped],
                self.services["ioburst"][key],
                "Check " + str(key) + " in createServiceOffering"
            )
        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke"], required_hardware="false")
    def test_06_create_disk_offering_with_cache_mode_type(self):
        """Test to create disk offering with each one of the valid cache mode types : none, writeback and writethrough

        # Validate the following:
        # 1. createDiskOfferings should return valid info for new offering
        # 2. The Cloud Database contains the valid information
        """
        cache_mode_types=["none", "writeback", "writethrough"]
        for i in range(3):
            disk_offering = DiskOffering.create(
                self.apiclient,
                self.services["disk_offering"],
                cacheMode=cache_mode_types[i]
            )
            self.cleanup.append(disk_offering)

            self.debug("Created Disk offering with valid cacheMode param with ID: %s" % disk_offering.id)

            list_disk_response = list_disk_offering(
                self.apiclient,
                id=disk_offering.id
            )
            self.assertEqual(
                isinstance(list_disk_response, list),
                True,
                "Check list response returns a valid list"
            )
            self.assertNotEqual(
                len(list_disk_response),
                0,
                "Check Disk offering is created"
            )
            disk_response = list_disk_response[0]

            self.assertEqual(
                disk_response.displaytext,
                self.services["disk_offering"]["displaytext"],
                "Check server id in createServiceOffering"
            )
            self.assertEqual(
                disk_response.name,
                self.services["disk_offering"]["name"],
                "Check name in createServiceOffering"
            )
            self.assertEqual(
                disk_response.cacheMode,
                cache_mode_types[i],
                "Check cacheMode in createServiceOffering"
            )

        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke"], required_hardware="false")
    def test_07_create_disk_offering_with_invalid_cache_mode_type(self):
        """Test to create disk offering with invalid cacheMode type

        # Validate the following:
        # 1. createDiskOfferings should return valid info for new offering
        # 2. The Cloud Database contains the valid information
        """

        with self.assertRaises(Exception):
            disk_offering = DiskOffering.create(
                self.apiclient,
                self.services["disk_offering"],
                cacheMode="invalid_cache_mode_type"
            )


        return

    @attr(tags = ["advanced", "basic", "eip", "sg", "advancedns", "simulator", "smoke", "diskencrypt"])
    def test_08_create_encrypted_disk_offering(self):
        """Test to create an encrypted type disk offering"""

        # Validate the following:
        # 1. createDiskOfferings should return valid info for new offering
        # 2. The Cloud Database contains the valid information

        disk_offering = DiskOffering.create(
            self.apiclient,
            self.services["disk_offering"],
            name="disk-encrypted",
            encrypt="true"
        )
        self.cleanup.append(disk_offering)

        self.debug("Created Disk offering with ID: %s" % disk_offering.id)

        list_disk_response = list_disk_offering(
            self.apiclient,
            id=disk_offering.id
        )

        self.assertEqual(
            isinstance(list_disk_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_disk_response),
            0,
            "Check Disk offering is created"
        )
        disk_response = list_disk_response[0]

        self.assertEqual(
            disk_response.encrypt,
            True,
            "Check if encrypt is set after createServiceOffering"
        )
        return

class TestDiskOfferings(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):

        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDiskOfferings, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        cls.disk_offering_1 = DiskOffering.create(
                                                  cls.apiclient,
                                                  cls.services["disk_offering"]
                                                  )
        cls.disk_offering_2 = DiskOffering.create(
                                                  cls.apiclient,
                                                  cls.services["disk_offering"]
                                                  )
        cls._cleanup = [cls.disk_offering_1]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(TestDiskOfferings, cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns",  "smoke"], required_hardware="false")
    def test_02_edit_disk_offering(self):
        """Test to update existing disk offering

        # Validate the following:
        # 1. updateDiskOffering should return
        #    a valid information for newly created offering

        #Generate new name & displaytext from random data
        """
        random_displaytext = random_gen()
        random_name = random_gen()

        self.debug("Updating Disk offering with ID: %s" %
                                    self.disk_offering_1.id)

        cmd = updateDiskOffering.updateDiskOfferingCmd()
        cmd.id = self.disk_offering_1.id
        cmd.displaytext = random_displaytext
        cmd.name = random_name

        self.apiclient.updateDiskOffering(cmd)

        list_disk_response = list_disk_offering(
                                                self.apiclient,
                                                id=self.disk_offering_1.id
                                                )
        self.assertEqual(
                            isinstance(list_disk_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_disk_response),
                            0,
                            "Check disk offering is updated"
                        )

        disk_response = list_disk_response[0]

        self.assertEqual(
                        disk_response.displaytext,
                        random_displaytext,
                        "Check service displaytext in updateServiceOffering"
                        )
        self.assertEqual(
                        disk_response.name,
                        random_name,
                        "Check service name in updateServiceOffering"
                        )
        return

    @attr(tags=["advanced", "basic", "eip", "sg", "advancedns", "smoke"], required_hardware="false")
    def test_03_delete_disk_offering(self):
        """Test to delete disk offering

        # Validate the following:
        # 1. deleteDiskOffering should return
        #    a valid information for newly created offering
        """
        self.disk_offering_2.delete(self.apiclient)

        self.debug("Deleted Disk offering with ID: %s" %
                                    self.disk_offering_2.id)
        list_disk_response = list_disk_offering(
                                                self.apiclient,
                                                id=self.disk_offering_2.id
                                                )

        self.assertEqual(
                        list_disk_response,
                        None,
                        "Check if disk offering exists in listDiskOfferings"
                        )
        return
