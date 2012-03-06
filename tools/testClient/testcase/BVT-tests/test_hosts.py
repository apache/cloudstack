# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Hosts and Clusters
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from testcase.libs.utils import *
from testcase.libs.base import *
from testcase.libs.common import *

#Import System modules
import time

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
                                          "url": 'http://192.168.100.210',
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
                         "zoneid": 2,
                         # Optional, if specified the mentioned zone will be
                         # used for tests
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
            self.dbclient.close()
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

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