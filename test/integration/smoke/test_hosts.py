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
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr

#Import System modules
import time

_multiprocess_shared_ = True

class Services:
    """Test Hosts & Clusters Services
    """

    def __init__(self):
        self.services = {
                       "clusters": {
                                   0: {
                                        "clustername": "Xen Cluster",
                                        "clustertype": "CloudManaged",
                                        # CloudManaged or ExternalManaged"
                                        "hypervisor": "XenServer",
                                        # Hypervisor type
                                    },
                                   1: {
                                        "clustername": "KVM Cluster",
                                        "clustertype": "CloudManaged",
                                        # CloudManaged or ExternalManaged"
                                        "hypervisor": "KVM",
                                        # Hypervisor type
                                        },
                                   2: {
                                        "hypervisor": 'VMware',
                                        # Hypervisor type
                                        "clustertype": 'ExternalManaged',
                                        # CloudManaged or ExternalManaged"
                                        "username": 'administrator',
                                        "password": 'fr3sca',
                                        "url": 'http://192.168.100.17/CloudStack-Clogeny-Pune/Pune-1',
                                        # Format:http://vCenter Host/Datacenter/Cluster
                                        "clustername": 'VMWare Cluster',
                                        },
                                    },
                       "hosts": {
                                 "xenserver": {
                                # Must be name of corresponding Hypervisor type
                                # in cluster in small letters
                                          "hypervisor": 'XenServer',
                                          # Hypervisor type
                                          "clustertype": 'CloudManaged',
                                          # CloudManaged or ExternalManaged"
                                          "url": 'http://192.168.100.211',
                                          "username": "root",
                                          "password": "fr3sca",
                                          },
                                 "kvm": {
                                          "hypervisor": 'KVM',
                                          # Hypervisor type
                                          "clustertype": 'CloudManaged',
                                          # CloudManaged or ExternalManaged"
                                          "url": 'http://192.168.100.212',
                                          "username": "root",
                                          "password": "fr3sca",
                                          },
                                 "vmware": {
                                          "hypervisor": 'VMware',
                                          # Hypervisor type
                                          "clustertype": 'ExternalManaged',
                                          # CloudManaged or ExternalManaged"
                                          "url": 'http://192.168.100.203',
                                          "username": "administrator",
                                          "password": "fr3sca",
                                         },
                                 },
                       }

class TestHosts(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.services = Services().services
        self.zone = get_zone(self.apiclient, self.services)
        self.pod = get_pod(self.apiclient, self.zone.id, self.services)
        self.cleanup = []

        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @unittest.skip("skipped - our environments will not add hosts")
    def test_01_clusters(self):
        """Test Add clusters & hosts - XEN, KVM, VWARE
        """

        # Validate the following:
        # 1. Verify hypervisortype returned by API is Xen/KVM/VWare
        # 2. Verify that the cluster is in 'Enabled' allocation state
        # 3. Verify that the host is added successfully and in Up state
        #    with listHosts API response

        #Create clusters with Hypervisor type XEN/KVM/VWare
        for k, v in self.services["clusters"].items():
            cluster = Cluster.create(
                                     self.apiclient,
                                     v,
                                     zoneid=self.zone.id,
                                     podid=self.pod.id
                                     )
            self.debug(
                "Created Cluster for hypervisor type %s & ID: %s" %(
                                                                    v["hypervisor"],
                                                                    cluster.id     
                                                                    ))
            self.assertEqual(
                    cluster.hypervisortype,
                    v["hypervisor"],
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
                               podid=self.pod.id
                               )
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
                cluster_response.hypervisortype,
                cluster.hypervisortype,
                "Check hypervisor type with is " + v["hypervisor"] + " or not"
                )
        return
