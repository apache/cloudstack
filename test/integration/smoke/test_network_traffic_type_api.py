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

# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (
    TrafficType,
    PhysicalNetwork,
    Zone,
)
from nose.plugins.attrib import attr


class TestTrafficTypeApi(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.test_client = super(TestTrafficTypeApi, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        cls._cleanup = []
        cls.services["advanced_sg"]["zone"]["name"] = "TestTrafficTypeApi-zone"
        cls.zone = Zone.create(cls.api_client, cls.services["advanced_sg"]["zone"])
        cls._cleanup.append(cls.zone)

        cls.physical_network = PhysicalNetwork.create(
            cls.api_client,
            cls.services["l2-network"],
            isolationmethods="VLAN",
            zoneid=cls.zone.id,
        )
        cls._cleanup.append(cls.physical_network)

    @classmethod
    def tearDownClass(cls):
        super(TestTrafficTypeApi, cls).tearDownClass()


    @attr(tags=["advanced"], required_hardware="false")
    def test_list_api_fields(self):
        traffic_type = TrafficType.add(
            self.api_client,
            physicalnetworkid=self.physical_network.id,
            kvmnetworklabel="kvm",
            traffictype="Public",
            vlan="100",
        ).traffictype

        traffic_types = TrafficType.list(
            self.api_client,
            physicalnetworkid=self.physical_network.id
        )

        assert len(traffic_types) == 1
        response = traffic_types[0]
        self.assertEqual(response.id, traffic_type.id)
        self.assertEqual(response.kvmnetworklabel, "kvm")
        self.assertEqual(response.traffictype, "Public")
        self.assertEqual(response.vlan, "100")
        self.assertEqual(response.isolationmethods, "VLAN")
