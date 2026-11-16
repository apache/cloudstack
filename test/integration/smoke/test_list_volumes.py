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
""" Tests for API listing of volumes with different filters
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED
from marvin.lib.base import (Account,
                             Domain,
                             Volume,
                             ServiceOffering,
                             Tag,
                             DiskOffering,
                             VirtualMachine)
from marvin.lib.common import (get_domain, list_accounts,
                               list_zones, list_clusters, list_hosts, get_suitable_test_template)
# Import System modules
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestListVolumes(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestListVolumes, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.domain = get_domain(cls.apiclient)
        cls.zones = list_zones(cls.apiclient)
        cls.zone = cls.zones[0]
        cls.clusters = list_clusters(cls.apiclient)
        cls.cluster = cls.clusters[0]
        cls.hosts = list_hosts(cls.apiclient)
        cls.account = list_accounts(cls.apiclient, name="admin")[0]
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

        cls.services["template"]["ostypeid"] = template.ostypeid
        cls.services["template_2"]["ostypeid"] = template.ostypeid
        cls.services["ostypeid"] = template.ostypeid
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["mode"] = cls.zone.networktype

        cls.disk_offering = DiskOffering.create(cls.apiclient,
                                                cls.services["disk_offering"])
        cls._cleanup.append(cls.disk_offering)

        # Create VM
        cls.virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            clusterid=cls.cluster.id,
            serviceofferingid=cls.service_offering.id,
            mode=cls.services["mode"]
        )

        cls.child_domain = Domain.create(
            cls.apiclient,
            cls.services["domain"])
        cls._cleanup.append(cls.child_domain)

        cls.child_account = Account.create(
            cls.apiclient,
            cls.services["account"],
            admin=True,
            domainid=cls.child_domain.id)
        cls._cleanup.append(cls.child_account)

        cls.vol_1 = Volume.create(cls.apiclient,
                                  cls.services["volume"],
                                  zoneid=cls.zone.id,
                                  account=cls.account.name,
                                  domainid=cls.account.domainid,
                                  diskofferingid=cls.disk_offering.id)
        cls._cleanup.append(cls.vol_1)

        cls.vol_1 = cls.virtual_machine.attach_volume(
            cls.apiclient,
            cls.vol_1
        )
        cls._cleanup.append(cls.virtual_machine)

        Tag.create(cls.apiclient, cls.vol_1.id, "Volume", {"abc": "xyz"})

        cls.vol_2 = Volume.create(cls.apiclient,
                                  cls.services["volume"],
                                  zoneid=cls.zone.id,
                                  account=cls.account.name,
                                  domainid=cls.account.domainid,
                                  diskofferingid=cls.disk_offering.id)

        cls._cleanup.append(cls.vol_2)

        cls.vol_3 = Volume.create(cls.apiclient,
                                  cls.services["volume"],
                                  zoneid=cls.zone.id,
                                  account=cls.child_account.name,
                                  domainid=cls.child_account.domainid,
                                  diskofferingid=cls.disk_offering.id)
        cls._cleanup.append(cls.vol_3)

    @classmethod
    def tearDownClass(cls):
        super(TestListVolumes, cls).tearDownClass()

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_list_volumes_account_domain_filter(self):
        """Test listing Volumes with account & domain filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            3,
            "ListVolumes response expected 3 Volumes, received %s" % len(list_volume_response)
        )

        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            account=self.child_account.name,
            domainid=self.child_account.domainid
        )

        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            1,
            "ListVolumes response expected 1 Volume, received %s" % len(list_volume_response)
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_list_volumes_diskofferingid_filter(self):
        """Test listing Volumes with diskofferingid filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            2,
            "ListVolumes response expected 2 Volumes, received %s" % len(list_volume_response)
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_list_volumes_id_filter(self):
        """Test listing Volumes with id filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            id=self.vol_1.id
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            1,
            "ListVolumes response expected 1 Volume, received %s" % len(list_volume_response)
        )
        self.assertEqual(
            list_volume_response[0].id,
            self.vol_1.id,
            "ListVolumes response expected Volume with id %s, received %s" % (self.vol_1.id, list_volume_response[0].id)
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_04_list_volumes_ids_filter(self):
        """Test listing Volumes with ids filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            ids=[self.vol_1.id, self.vol_2.id, self.vol_3.id]
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            2,
            "ListVolumes response expected 2 Volumes, received %s" % len(list_volume_response)
        )
        self.assertIn(list_volume_response[0].id, [self.vol_1.id, self.vol_2.id],
                      "ListVolumes response Volume 1 not in list")
        self.assertIn(list_volume_response[1].id, [self.vol_1.id, self.vol_2.id],
                      "ListVolumes response Volume 2 not in list")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_list_volumes_isrecursive(self):
        """Test listing Volumes with isrecursive filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            isrecursive=True,
            domainid=self.account.domainid
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len([v for v in list_volume_response if v.state != "Destroy"]),
            4,
            "ListVolumes response expected 4 Volumes, received %s" % len(list_volume_response)
        )

        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            isrecursive=False,
            domainid=self.account.domainid
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len([v for v in list_volume_response if v.state != "Destroy"]),
            3,
            "ListVolumes response expected 3 Volumes, received %s" % len(list_volume_response)
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_06_list_volumes_keyword_filter(self):
        """Test listing Volumes with keyword filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            keyword=self.services["volume"]["diskname"]
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            2,
            "ListVolumes response expected 2 Volumes, received %s" % len(list_volume_response)
        )
        self.assertIn(
            list_volume_response[0].id, [self.vol_1.id, self.vol_2.id],
            "ListVolumes response Volume 1 not in list")
        self.assertIn(list_volume_response[1].id, [self.vol_1.id, self.vol_2.id],
                      "ListVolumes response Volume 2 not in list")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_07_list_volumes_listall(self):
        """Test listing Volumes with listall filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            listall=True
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len([v for v in list_volume_response if v.state != "Destroy"]),
            4,
            "ListVolumes response expected 4 Volumes, received %s" % len(list_volume_response)
        )

        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            listall=False
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len([v for v in list_volume_response if v.state != "Destroy"]),
            3,
            "ListVolumes response expected 3 Volumes, received %s" % len(list_volume_response)
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_08_listsystemvms(self):
        list_volumes_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            listsystemvms=True
        )
        self.assertEqual(
            isinstance(list_volumes_response, list),
            True,
            "List Volume response is not a valid list"
        )
        self.assertGreater(
            len(list_volumes_response),
            3,
            "ListVolumes response expected more than 3 Volumes, received %s" % len(list_volumes_response)
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_09_list_volumes_name_filter(self):
        """Test listing Volumes with name filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            name=self.vol_1.name
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            1,
            "ListVolumes response expected 1 Volumes, received %s" % len(list_volume_response)
        )
        self.assertEqual(
            list_volume_response[0].id,
            self.vol_1.id,
            "ListVolumes response expected Volume with id %s, received %s" % (self.vol_1.id, list_volume_response[0].id)
        )
        self.assertEqual(
            list_volume_response[0].name,
            self.vol_1.name,
            "ListVolumes response expected Volume with name %s, received %s" % (
                self.vol_1.name, list_volume_response[0].name)
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_10_list_volumes_podid_filter(self):
        """Test listing Volumes with podid filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            podid=self.vol_1.podid
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertGreater(
            len(list_volume_response),
            1,
            "ListVolumes response expected more than 1 Volume, received %s" % len(list_volume_response)
        )
        self.assertIn(self.vol_1.id, [volume.id for volume in list_volume_response],
                      "ListVolumes response expected Volume with id %s" % self.vol_1.id)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_11_list_volumes_state_filter(self):
        """Test listing Volumes with state filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            state="Ready"
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            2,
            "ListVolumes response expected 2 Volumes, received %s" % len(list_volume_response)
        )
        self.assertIn(self.vol_1.id, [volume.id for volume in list_volume_response],
                      "ListVolumes response expected Volume with id %s" % self.vol_1.id)

        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            state="Allocated"
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            1,
            "ListVolumes response expected 1 Volumes, received %s" % len(list_volume_response)
        )
        self.assertEqual(self.vol_2.id, list_volume_response[0].id,
                         "ListVolumes response expected Volume with id %s" % self.vol_3.id)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_12_list_volumes_storageid_filter(self):
        """Test listing Volumes with storageid filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            storageid=self.vol_1.storageid
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertGreaterEqual(
            len(list_volume_response),
            1,
            "ListVolumes response expected 1 or more Volumes, received %s" % len(list_volume_response)
        )
        self.assertIn(self.vol_1.id, [volume.id for volume in list_volume_response],
                      "ListVolumes response expected Volume with id %s" % self.vol_1.id)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_13_list_volumes_type_filter(self):
        """Test listing Volumes with type filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            type="DATADISK"
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            2,
            "ListVolumes response expected 2 Volumes, received %s" % len(list_volume_response)
        )
        self.assertIn(self.vol_1.id, [volume.id for volume in list_volume_response],
                      "ListVolumes response expected Volume with id %s" % self.vol_1.id)

        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            type="ROOT"
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            1,
            "ListVolumes response expected 1 Volumes, received %s" % len(list_volume_response)
        )
        self.assertNotIn(list_volume_response[0].id, [self.vol_1.id, self.vol_2.id],
                         "ListVolumes response expected ROOT Volume")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_14_list_volumes_virtualmachineid_filter(self):
        """Test listing Volumes with virtualmachineid filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id,
            virtualmachineid=self.vol_1.virtualmachineid
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            2,
            "ListVolumes response expected 2 Volumes, received %s" % len(list_volume_response)
        )
        self.assertIn(self.vol_1.id, [volume.id for volume in list_volume_response],
                      "ListVolumes response expected Volume with id %s" % self.vol_1.id)

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_15_list_volumes_zoneid_filter(self):
        """Test listing Volumes with zoneid filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zones[0].id
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            3,
            "ListVolumes response expected 3 Volumes, received %s" % len(list_volume_response)
        )

        if len(self.zones) > 1:
            list_volume_response = Volume.list(
                self.apiclient,
                zoneid=self.zones[1].id
            )
            self.assertIsNone(list_volume_response, "List Volume response is not None")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_16_list_volumes_tags_filter(self):
        """Test listing Volumes with tags filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            tags=[{"key": "abc", "value": "xyz"}]
        )

        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertEqual(
            len(list_volume_response),
            1,
            "ListVolumes response expected 1 or more Volumes, received %s" % len(list_volume_response)
        )
        self.assertEqual(
            list_volume_response[0].id,
            self.vol_1.id,
            "ListVolumes response expected Volume with id %s, received %s" % (self.vol_1.id, list_volume_response[0].id)
        )
        self.assertEqual(
            list_volume_response[0].tags[0]["key"],
            "abc",
            "ListVolumes response expected Volume with tag key abc, received %s" % list_volume_response[0].tags[0]["key"]
        )
        self.assertEqual(
            list_volume_response[0].tags[0]["value"],
            "xyz",
            "ListVolumes response expected Volume with tag value xyz, received %s" % list_volume_response[0].tags[0]["value"]
        )

        list_volume_response = Volume.list(
            self.apiclient,
            tags=[{"key": "abc", "value": "xyz1"}]
        )
        self.assertIsNone(list_volume_response, "List Volume response is not None")
        with self.assertRaises(Exception):
            list_volume_response = Volume.list(
                self.apiclient,
                tags=[{"key": None, "value": None}]
            )


    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_17_list_volumes_no_filter(self):
        """Test listing Volumes with no filter
        """
        list_volume_response = Volume.list(
            self.apiclient,
            zoneid=self.zone.id
        )
        self.assertTrue(
            isinstance(list_volume_response, list),
            "List Volume response is not a valid list"
        )
        self.assertGreaterEqual(
            len(list_volume_response),
            3,
            "ListVolumes response expected 3 or more Volumes, received %s" % len(list_volume_response)
        )
        self.assertIn(self.vol_1.id, [volume.id for volume in list_volume_response],
                      "ListVolumes response expected Volume with id %s" % self.vol_1.id)
