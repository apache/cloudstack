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
import logging
# These tests need to be run separately and not in parallel with other tests.
# Because it disables the host
# host_id column of op_host_capacity refers to host_id or a storage pool id
#
# This test is to make sure that Disable host only disables the capacities of type
# CPU and MEMORY
#
# TEST:
# Base Condition: There exists a host and storage pool with same id
#
# Steps:
# 1. Find a host and storage pool having same id
# 2. Disable the host
# 3. verify that the CPU(1) and MEMORY(0) capacity in op_host_capacity for above host
#    is disabled
# 4. verify that the STORAGE(3) capacity in op_host_capacity for storage pool with id
#    same as above host is not disabled
#

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


def check_db(self, host_state):
    """
    Function to check capacity_state in op_host_capacity table
    """
    capacity_state = None
    if self.host_db_id and self.host_db_id[0]:
        capacity_state = self.dbclient.execute(
            "select capacity_state from op_host_capacity where host_id='%s' and capacity_type in (0,1) order by capacity_type asc;" %
            self.host_db_id[0][0])

    if capacity_state and len(capacity_state)==2:
        if capacity_state[0]:
            self.assertEqual(
                capacity_state[0][0],
                host_state +
                "d",
                "Invalid db query response for capacity_state %s" %
                capacity_state[0][0])

        if capacity_state[1]:
            self.assertEqual(
                capacity_state[1][0],
                host_state +
                "d",
                "Invalid db query response for capacity_state %s" %
                capacity_state[1][0])
    else:
        self.logger.debug("Could not find capacities of type 1 and 0. Does not have necessary data to run this test")

    capacity_state = None
    if self.host_db_id and self.host_db_id[0]:
        capacity_state = self.dbclient.execute(
            "select capacity_state from op_host_capacity where host_id='%s' and capacity_type = 3 order by capacity_type asc;" %
            self.host_db_id[0][0])

    if capacity_state and capacity_state[0]:
        self.assertNotEqual(
            capacity_state[0][0],
            host_state +
            "d",
            "Invalid db query response for capacity_state %s" %
            capacity_state[0][0])
    else:
        self.logger.debug("Could not find capacities of type 3. Does not have necessary data to run this test")


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

        cls.logger = logging.getLogger('TestHosts')
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        cls.storage_pool_db_id = None
        # list hosts
        hosts = list_hosts(cls.apiclient, type="Routing")
        i = 0
        while (i < len(hosts)):
            host_id = hosts[i].id
            cls.logger.debug("Trying host id : %s" % host_id)
            host_db_id = cls.dbclient.execute(
                "select id from host where uuid='%s';" %
                host_id)

            if host_db_id and host_db_id[0]:
                cls.logger.debug("found host db id : %s" % host_db_id)
                storage_pool_db_id = cls.dbclient.execute(
                    "select id from storage_pool where id='%s' and removed is null;" %
                    host_db_id[0][0])

                if storage_pool_db_id and storage_pool_db_id[0]:
                    cls.logger.debug("Found storage_pool_db_id  : %s" % storage_pool_db_id[0][0])
                    capacity_state = cls.dbclient.execute(
                        "select count(capacity_state) from op_host_capacity where host_id='%s' and capacity_type in (0,1,3) and capacity_state = 'Enabled'" %
                        host_db_id[0][0])

                    if capacity_state and capacity_state[0]:
                        cls.logger.debug("Check capacity count  : %s" % capacity_state[0][0])

                        if capacity_state[0][0] == 3:
                            cls.logger.debug("found host id : %s, can be used for this test" % host_id)
                            cls.my_host_id = host_id
                            cls.host_db_id = host_db_id
                            cls.storage_pool_db_id = storage_pool_db_id
                            break
            if not cls.storage_pool_db_id:
                i = i + 1


        if cls.storage_pool_db_id is None:
            raise unittest.SkipTest("There is no host and storage pool available in the setup to run this test")



    @classmethod
    def tearDownClass(cls):
        cleanup_resources(cls.apiclient, cls._cleanup)
        return

    def setUp(self):
        self.logger.debug("Capacity check for Disable host")
        self.cleanup = []
        return

    def tearDown(self):
        # Clean up
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags=["advanced", "basic"], required_hardware="false")
    def test_01_op_host_capacity_disable_host(self):

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

        return
