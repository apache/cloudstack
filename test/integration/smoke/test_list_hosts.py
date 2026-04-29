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
""" Tests for API listing of hosts with different filters
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import FAILED
from marvin.lib.base import (Configurations, Host)
from marvin.lib.common import (get_domain, list_accounts,
                               list_zones, list_clusters)
# Import System modules
from nose.plugins.attrib import attr

_multiprocess_shared_ = True


class TestListHosts(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestListHosts, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.zones = list_zones(cls.apiclient)
        cls.zone = cls.zones[0]
        cls.clusters = list_clusters(cls.apiclient)
        cls.cluster = cls.clusters[0]
        cls.hosts = Host.list(cls.apiclient)


    @classmethod
    def tearDownClass(cls):
        super(TestListHosts, cls).tearDownClass()

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_01_list_hosts_no_filter(self):
        """Test list hosts with no filter"""
        hosts = Host.list(self.apiclient)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should greater than 0"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_02_list_hosts_clusterid_filter(self):
        """Test list hosts with clusterid filter"""
        hosts = Host.list(self.apiclient, clusterid=self.cluster.id)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should greater than 0"
        )
        for host in hosts:
            self.assertEqual(
                host.clusterid,
                self.cluster.id,
                "Host should be in the cluster %s" % self.cluster.id
            )
        with self.assertRaises(Exception):
            hosts = Host.list(self.apiclient, clusterid="invalidclusterid")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_03_list_hosts_hahost_filter(self):
        """Test list hosts with hahost filter"""
        configs = Configurations.list(
            self.apiclient,
            name='ha.tag'
        )
        if isinstance(configs, list) and configs[0].value != "" and configs[0].value is not None:
            hosts = Host.list(self.apiclient, hahost=True)
            if hosts is not None:
                self.assertTrue(
                    isinstance(hosts, list),
                    "Host response type should be a valid list"
                )
                self.assertGreater(
                    len(hosts),
                    0,
                    "Length of host response should greater than 0"
                )
                for host in hosts:
                    self.assertEqual(
                        host.hahost,
                        True,
                        "Host should be a HA host"
                    )

            hosts = Host.list(self.apiclient, hahost=False)
            if hosts is not None:
                self.assertTrue(
                    isinstance(hosts, list),
                    "Host response type should be a valid list"
                )
                self.assertGreater(
                    len(hosts),
                    0,
                    "Length of host response should greater than 0"
                )
                for host in hosts:
                    self.assertTrue(
                        host.hahost is None or host.hahost is False,
                        "Host should not be a HA host"
                    )
        else:
            self.debug("HA is not enabled in the setup")
            hosts = Host.list(self.apiclient, hahost="invalidvalue")
            self.assertTrue(
                isinstance(hosts, list),
                "Host response type should be a valid list"
            )
            self.assertGreater(
                len(hosts),
                0,
                "Length of host response should greater than 0"
            )
            self.assertEqual(
                len(hosts),
                len(self.hosts),
                "Length of host response should be equal to the length of hosts"
            )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_04_list_hosts_hypervisor_filter(self):
        """Test list hosts with hypervisor filter"""
        hosts = Host.list(self.apiclient, hypervisor=self.hypervisor)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should greater than 0"
        )
        for host in hosts:
            self.assertEqual(
                host.hypervisor.lower(),
                self.hypervisor.lower(),
                "Host should be a %s hypervisor" % self.hypervisor
            )

        hosts = Host.list(self.apiclient, hypervisor="invalidhypervisor")
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertEqual(
            len(hosts),
            len(self.hosts),
            "Length of host response should be equal to the length of hosts"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_05_list_hosts_id_filter(self):
        """Test list hosts with id filter"""
        hosts = Host.list(self.apiclient, id=self.hosts[0].id)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertEqual(
            len(hosts),
            1,
            "Length of host response should be 1"
        )
        self.assertEqual(
            hosts[0].id,
            self.hosts[0].id,
            "Host id should match with the host id in the list"
        )

        with self.assertRaises(Exception):
            hosts = Host.list(self.apiclient, id="invalidid")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_06_list_hosts_keyword_filter(self):
        """Test list hosts with keyword filter"""
        hosts = Host.list(self.apiclient, keyword=self.hosts[0].name)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should be greater than 0"
        )
        for host in hosts:
            self.assertIn(
                host.name,
                self.hosts[0].name,
                "Host name should match with the host name in the list"
            )

        hosts = Host.list(self.apiclient, keyword="invalidkeyword")
        self.assertIsNone(
            hosts,
            "Host response should be None"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_07_list_hosts_name_filter(self):
        """Test list hosts with name filter"""
        hosts = Host.list(self.apiclient, name=self.hosts[0].name)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should be greater than 0"
        )
        for host in hosts:
            self.assertIn(
                host.name,
                self.hosts[0].name,
                "Host name should match with the host name in the list"
            )

        hosts = Host.list(self.apiclient, name="invalidname")
        self.assertIsNone(
            hosts,
            "Host response should be None"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_08_list_hosts_podid_filter(self):
        """Test list hosts with podid filter"""
        hosts = Host.list(self.apiclient, podid=self.hosts[0].podid)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should be greater than 0"
        )
        for host in hosts:
            self.assertEqual(
                host.podid,
                self.hosts[0].podid,
                "Host podid should match with the host podid in the list"
            )
        with self.assertRaises(Exception):
            hosts = Host.list(self.apiclient, podid="invalidpodid")

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_09_list_hosts_resourcestate_filter(self):
        """Test list hosts with resourcestate filter"""
        hosts = Host.list(self.apiclient, resourcestate=self.hosts[0].resourcestate)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should be greater than 0"
        )
        for host in hosts:
            self.assertEqual(
                host.resourcestate,
                self.hosts[0].resourcestate,
                "Host resourcestate should match with the host resourcestate in the list"
            )

        hosts = Host.list(self.apiclient, resourcestate="invalidresourcestate")
        self.assertIsNone(
            hosts,
            "Host response should be None"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_10_list_hosts_state_filter(self):
        """Test list hosts with state filter"""
        hosts = Host.list(self.apiclient, state=self.hosts[0].state)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should be greater than 0"
        )
        for host in hosts:
            self.assertEqual(
                host.state,
                self.hosts[0].state,
                "Host state should match with the host state in the list"
            )

        hosts = Host.list(self.apiclient, state="invalidstate")
        self.assertIsNone(
            hosts,
            "Host response should be None"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_11_list_hosts_type_filter(self):
        """Test list hosts with type filter"""
        hosts = Host.list(self.apiclient, type=self.hosts[0].type)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should be greater than 0"
        )
        for host in hosts:
            self.assertEqual(
                host.type,
                self.hosts[0].type,
                "Host type should match with the host type in the list"
            )

        hosts = Host.list(self.apiclient, type="invalidtype")
        self.assertIsNone(
            hosts,
            "Host response should be None"
        )

    @attr(tags=["advanced", "advancedns", "smoke", "basic"], required_hardware="false")
    def test_12_list_hosts_zoneid_filter(self):
        """Test list hosts with zoneid filter"""
        hosts = Host.list(self.apiclient, zoneid=self.zone.id)
        self.assertTrue(
            isinstance(hosts, list),
            "Host response type should be a valid list"
        )
        self.assertGreater(
            len(hosts),
            0,
            "Length of host response should be greater than 0"
        )
        for host in hosts:
            self.assertEqual(
                host.zoneid,
                self.zone.id,
                "Host zoneid should match with the host zoneid in the list"
            )

        with self.assertRaises(Exception):
            hosts = Host.list(self.apiclient, zoneid="invalidzoneid")
