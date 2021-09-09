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
# Unless   by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Test from the Marvin - Testing in Python wiki

# All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest

# Import Integration Libraries

# base - contains all resources as entities and defines create, delete,
# list operations on them
from marvin.lib.base import Host, Cluster, Zone, Pod

# utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources

# common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, list_hosts, get_pod

from nose.plugins.attrib import attr

import time

# These tests need to be run separately and not in parallel with other tests.
# Because it disables the infrastructure for brief periods


def update_host(apiclient, state, host_id):
    """
    Function to Enable/Disable Host
    """
    host_status = Host.update(
        apiclient,
        id=host_id,
        allocationstate=state
    )
    return host_status.resourcestate


def update_cluster(apiclient, state, cluster_id, managed_state):
    """
    Function to Enable/Disable cluster
    """
    cluster_status = Cluster.update(
        apiclient,
        id=cluster_id,
        allocationstate=state,
        managedstate=managed_state
    )
    return cluster_status.managedstate, cluster_status.allocationstate


def update_pod(apiclient, state, pod_id):
    """
    Function to Enable/Disable pod
    """
    pod_status = Pod.update(
        apiclient,
        id=pod_id,
        allocationstate=state
    )
    return pod_status.allocationstate


def update_zone(apiclient, state, zone):
    """
    Function to Enable/Disable zone
    """
    zone_status = zone.update(
        apiclient,
        allocationstate=state
    )
    return zone_status.allocationstate


def check_db(self, host_state):
    """
    Function to check capacity_state in op_host_capacity table
    """
    capacity_state = self.dbclient.execute(
        "select capacity_state from op_host_capacity where host_id='%s';" %
        self.host_db_id[0][0])
    self.assertEqual(
        capacity_state[0][0],
        host_state +
        "d",
        "Invalid db query response for capacity_state %s" %
        self.host_db_id[0][0])
    return capacity_state[0][0]


class TestHosts(cloudstackTestCase):

    """
    Testing Hosts
    """
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestHosts, cls).getClsTestClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.apiclient = cls.testClient.getApiClient()
        cls.dbclient = cls.testClient.getDbConnection()
        cls._cleanup = []

        # get zone, domain etc
        cls.zone = Zone(get_zone(cls.apiclient, cls.testClient.getZoneForTests()).__dict__)
        cls.domain = get_domain(cls.apiclient)
        cls.pod = get_pod(cls.apiclient, cls.zone.id)

        # list hosts
        hosts = list_hosts(cls.apiclient, type="Routing")
        if len(hosts) > 0:
            cls.my_host_id = hosts[0].id
            cls.host_db_id = cls.dbclient.execute(
                "select id from host where uuid='%s';" %
                cls.my_host_id)
            cls.my_cluster_id = hosts[0].clusterid
        else:
            raise unittest.SkipTest("There is no host available in the setup")

    @classmethod
    def tearDownClass(cls):
        cleanup_resources(cls.apiclient, cls._cleanup)
        return

    def setUp(self):
        self.cleanup = []
        return

    def tearDown(self):
        # Clean up
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_op_host_capacity_disable_cluster(self):
        """
        Disable the host and it's cluster,
        make sure that capacity_state is not affected by enabling/disabling
        of cluster in the op_host_capacity table
        """
        # disable the host and check op_host_capacity table

        host_state = "Disable"
        host_resourcestate = update_host(
            self.apiclient,
            host_state,
            self.my_host_id)
        self.assertEqual(
            host_resourcestate,
            host_state + "d",
            "Host state not correct"
        )
        check_db(self, host_state)
        # disable the cluster and check op_host_capacity table
        cluster_state = "Disabled"
        managed_state = "Managed"
        cluster_managedstate, cluster_allocationstate = update_cluster(
            self.apiclient, cluster_state, self.my_cluster_id, managed_state)
        self.assertEqual(
            cluster_allocationstate,
            cluster_state,
            "Not able to enable/disable the cluster"
        )
        self.assertEqual(
            cluster_managedstate,
            managed_state,
            "Not able to managed/unmanage the cluster"
        )

        check_db(self, host_state)
        # enable the cluster and check op_host_capacity table
        cluster_state = "Enabled"
        cluster_managedstate, cluster_allocationstate = update_cluster(
            self.apiclient, cluster_state, self.my_cluster_id, managed_state)
        self.assertEqual(
            cluster_allocationstate,
            cluster_state,
            "Not able to enable/disable the cluster"
        )
        self.assertEqual(
            cluster_managedstate,
            managed_state,
            "Not able to managed/unmanage the cluster"
        )
        check_db(self, host_state)
        # enable the host and check op_host_capacity table

        host_state = "Enable"
        host_resourcestate = update_host(
            self.apiclient,
            host_state,
            self.my_host_id)
        self.assertEqual(
            host_resourcestate,
            host_state + "d",
            "Host state not correct"
        )
        check_db(self, host_state)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_02_op_host_capacity_disable_pod(self):
        """
        Disable the host and it's pod,
        make sure that capacity_state is not affected by enabling/disabling
        of pod in the op_host_capacity table
        """
        # disable the host and check op_host_capacity table

        host_state = "Disable"
        host_resourcestate = update_host(
            self.apiclient,
            host_state,
            self.my_host_id)
        self.assertEqual(
            host_resourcestate,
            host_state + "d",
            "Host state not correct"
        )
        check_db(self, host_state)
        # disable the pod and check op_host_capacity table
        pod_state = "Disabled"
        pod_allocationstate = update_pod(
            self.apiclient,
            pod_state,
            self.pod.id)
        self.assertEqual(
            pod_allocationstate,
            pod_state,
            "Not able to enable/disable the pod"
        )
        check_db(self, host_state)
        # enable the pod and check op_host_capacity table
        pod_state = "Enabled"
        pod_allocationstate = update_pod(
            self.apiclient,
            pod_state,
            self.pod.id)
        self.assertEqual(
            pod_allocationstate,
            pod_state,
            "Not able to enable/disable the pod"
        )
        check_db(self, host_state)
        # enable the host and check op_host_capacity table

        host_state = "Enable"
        host_resourcestate = update_host(
            self.apiclient,
            host_state,
            self.my_host_id)
        self.assertEqual(
            host_resourcestate,
            host_state + "d",
            "Host state not correct"
        )
        check_db(self, host_state)
        return

    @attr(tags=["advanced", "basic", "tag1"], required_hardware="false")
    def test_03_op_host_capacity_disable_zone(self):
        """
        Disable the host and it's zone,
        make sure that capacity_state is not affected by enabling/disabling
        of zone in the op_host_capacity table
        """
        # disable the host and check op_host_capacity table

        host_state = "Disable"
        host_resourcestate = update_host(
            self.apiclient,
            host_state,
            self.my_host_id)
        self.assertEqual(
            host_resourcestate,
            host_state + "d",
            "Host state not correct"
        )
        check_db(self, host_state)
        # disbale the zone and check op_host_capacity table
        zone_state = "Disabled"
        zone_allocationstate = update_zone(
            self.apiclient,
            zone_state,
            self.zone)
        self.assertEqual(
            zone_allocationstate,
            zone_state,
            "Not able to enable/disable the zone"
        )
        check_db(self, host_state)
        # enable the zone and check op_host_capacity table
        zone_state = "Enabled"
        zone_allocationstate = update_zone(
            self.apiclient,
            zone_state,
            self.zone)
        self.assertEqual(
            zone_allocationstate,
            zone_state,
            "Not able to enable/disable the zone"
        )
        check_db(self, host_state)
        # enable the host and check op_host_capacity table

        host_state = "Enable"
        host_resourcestate = update_host(
            self.apiclient,
            host_state,
            self.my_host_id)
        self.assertEqual(
            host_resourcestate,
            host_state + "d",
            "Host state not correct"
        )
        check_db(self, host_state)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_04_disable_host_unmanage_cluster_check_hosts_status(self):
        """
        Disable the host then unmanage the cluster,
        make sure that the host goes to Disconnected state
        """
        # disable host
        host_state = "Disable"
        host_resourcestate = update_host(
            self.apiclient,
            host_state,
            self.my_host_id)
        self.assertEqual(
            host_resourcestate,
            host_state + "d",
            "Host state not correct"
        )
        # unmanage cluster
        cluster_state = "Enabled"
        managed_state = "Unmanaged"
        cluster_managedstate, cluster_allocationstate = update_cluster(
            self.apiclient, cluster_state, self.my_cluster_id, managed_state)
        self.assertEqual(
            cluster_allocationstate,
            cluster_state,
            "Not able to enable/disable the cluster"
        )
        self.assertEqual(
            cluster_managedstate,
            managed_state,
            "Not able to managed/unmanage the cluster"
        )
        # check host state now
        time.sleep(30)
        host_list = list_hosts(self.apiclient, id=self.my_host_id)

        self.assertEqual(
            host_list[0].state,
            "Disconnected",
            " Host is not in Disconnected state after unmanaging cluster"
        )
        # manage the cluster again and let the hosts come back to Up state.
        managed_state = "Managed"
        cluster_managedstate, cluster_allocationstate = update_cluster(
            self.apiclient, cluster_state, self.my_cluster_id, managed_state)
        self.assertEqual(
            cluster_allocationstate,
            cluster_state,
            "Not able to enable/disable the cluster"
        )
        self.assertEqual(
            cluster_managedstate,
            managed_state,
            "Not able to managed/unmanage the cluster"
        )
        # check host state now
        time.sleep(90)
        host_list = list_hosts(self.apiclient, id=self.my_host_id)

        self.assertEqual(
            host_list[0].state,
            "Up",
            " Host is not in Up state after managing cluster"
        )
        # enable the host
        host_state = "Enable"
        host_resourcestate = update_host(
            self.apiclient,
            host_state,
            self.my_host_id)
        self.assertEqual(
            host_resourcestate,
            host_state + "d",
            "Host state not correct"
        )
        return
