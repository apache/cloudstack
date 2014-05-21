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
""" BVT tests for Hosts and Clusters
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.utils import (random_gen)
from nose.plugins.attrib import attr

#Import System modules
import time

_multiprocess_shared_ = True

class TestHosts(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.pod = get_pod(self.apiclient, self.zone.id)
        self.cleanup = []

        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    #@attr(tags=["selfservice"])
    def test_01_clusters(self):
        """Test Add clusters & hosts - simulator


        # Validate the following:
        # 1. Verify hypervisortype returned by API is Simulator/Xen/KVM/VWare
        # 2. Verify that the cluster is in 'Enabled' allocation state
        # 3. Verify that the host is added successfully and in Up state
        #    with listHosts API response

        #Create clusters with Hypervisor type Simulator/XEN/KVM/VWare
        """
        for k, v in self.services["clusters"].items():
            v["clustername"] = v["clustername"] + "-" + random_gen()
            cluster = Cluster.create(
                                     self.apiclient,
                                     v,
                                     zoneid=self.zone.id,
                                     podid=self.pod.id,
                                     hypervisor=v["hypervisor"].lower()
                                     )
            self.debug(
                "Created Cluster for hypervisor type %s & ID: %s" %(
                                                                    v["hypervisor"],
                                                                    cluster.id
                                                                    ))
            self.assertEqual(
                    cluster.hypervisortype.lower(),
                    v["hypervisor"].lower(),
                    "Check hypervisor type is " + v["hypervisor"] + " or not"
                    )
            self.assertEqual(
                    cluster.allocationstate,
                    'Enabled',
                    "Check whether allocation state of cluster is enabled"
                    )

            #If host is externally managed host is already added with cluster
            response = list_hosts(
                           self.apiclient,
                           clusterid=cluster.id
                           )

            if not response:
                hypervisor_type = str(cluster.hypervisortype.lower())
                host = Host.create(
                               self.apiclient,
                               cluster,
                               self.services["hosts"][hypervisor_type],
                               zoneid=self.zone.id,
                               podid=self.pod.id,
                               hypervisor=v["hypervisor"].lower()
                               )
                if host == FAILED:
                    self.fail("Host Creation Failed")
                self.debug(
                    "Created host (ID: %s) in cluster ID %s" %(
                                                                host.id,
                                                                cluster.id
                                                                ))
                #Cleanup Host & Cluster
                self.cleanup.append(host)
            self.cleanup.append(cluster)

            list_hosts_response = list_hosts(
                           self.apiclient,
                           clusterid=cluster.id
                           )
            self.assertEqual(
                            isinstance(list_hosts_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            self.assertNotEqual(
                            len(list_hosts_response),
                            0,
                            "Check list Hosts response"
                        )

            host_response = list_hosts_response[0]
            #Check if host is Up and running
            self.assertEqual(
                            host_response.state,
                            'Up',
                            "Check if state of host is Up or not"
                        )
            #Verify List Cluster Response has newly added cluster
            list_cluster_response = list_clusters(
                                                  self.apiclient,
                                                  id=cluster.id
                                                  )
            self.assertEqual(
                            isinstance(list_cluster_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
            self.assertNotEqual(
                            len(list_cluster_response),
                            0,
                            "Check list Hosts response"
                        )

            cluster_response = list_cluster_response[0]
            self.assertEqual(
                            cluster_response.id,
                            cluster.id,
                            "Check cluster ID with list clusters response"
                        )
            self.assertEqual(
                cluster_response.hypervisortype.lower(),
                cluster.hypervisortype.lower(),
                "Check hypervisor type with is " + v["hypervisor"] + " or not"
                )
        return
