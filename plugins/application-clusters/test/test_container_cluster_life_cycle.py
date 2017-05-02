# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.utils import (random_gen)
from nose.plugins.attrib import attr
import cmd

class TestContainerClusterLifeCycle(cloudstackTestCase):
    """
        Tests for container cluster life cycle operations
    """

    @classmethod
    def setUpClass(cls):
        testClient = super(TestContainerClusterLifeCycle, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]
        )
        cls.container_cluster = ContainerCluster.create(
            cls.apiclient,
            name="TestContainerCluster",
            zoneid=cls.zone.id,
            serviceofferingid=cls.service_offering.id,
            size=2)

        cls._cleanup = [cls.small_offering]

    @classmethod
    def tearDownClass(cls):
        cls.apiclient = super(TestContainerClusterLifeCycle, cls).getClsTestClient().getApiClient()
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            #Clean up, terminate the created ISOs
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "smoke"], required_hardware="false")
    def test_01_created_vm_state(self):
        """Test state of container cluster is running state after creation
        """
        self.assertEqual(self.container_cluster.state, "Running")

    @attr(tags = ["advanced", "smoke"], required_hardware="false")
    def test_02_stop_container_cluster(self):
        """Test state of container cluster is stopped state after performing stop
        """
        try:
            self.container_cluster.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop container cluster: %s" % e)
            return

        list_container_cluster_response = ContainerCluster.list(
                                            self.apiclient,
                                            id=self.container_cluster.id
                                            )
        self.assertEqual(
                            isinstance(list_container_cluster_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_container_cluster_response),
                            0,
                            "Check container cluster in the list"
                        )

        self.assertEqual(
                            list_container_cluster_response[0].state,
                            "Stopped",
                            "Check Container Cluster is in Stopped state"
                        )
        return

    @attr(tags = ["advanced", "smoke"], required_hardware="false")
    def test_03_start_container_cluster(self):
        """Test state of container cluster is Running state after performing start
        """
        try:
            self.container_cluster.start(self.apiclient)
        except Exception as e:
            self.fail("Failed to start container cluster: %s" % e)
            return

        list_container_cluster_response = ContainerCluster.list(
                                            self.apiclient,
                                            id=self.container_cluster.id
                                            )
        self.assertEqual(
                            isinstance(list_container_cluster_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_container_cluster_response),
                            0,
                            "Check container cluster in the list"
                        )

        self.assertEqual(
                            list_container_cluster_response[0].state,
                            "Running",
                            "Check Container Cluster is in Stopped state"
                        )
        return

    @attr(tags = ["advanced", "smoke"], required_hardware="false")
    def test_04_destroy_container_cluster(self):
        """Test destroy container cluster
        """
        try:
            self.container_cluster.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete container cluster: %s" % e)
            return

        list_container_cluster_response = ContainerCluster.list(
                                            self.apiclient,
                                            id=self.container_cluster.id
                                            )
        self.assertEqual(
                            isinstance(list_container_cluster_response, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(list_container_cluster_response),
                            0,
                            "Check container cluster in the list"
                        )

        self.assertEqual(
                            list_container_cluster_response[0].state,
                            "Destroyed",
                            "Check Container Cluster is in Destroyed state"
                        )
        return