# -*- encoding: utf-8 -*-
#
# Copyright (c) 2012 Citrix.  All rights reserved.
#
""" BVT tests for Hosts and Clusters
"""
#Import Local Modules
from cloudstackTestCase import *
from cloudstackAPI import *
from utils import *
from base import *

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
                                        "clustertype": "ExternalManaged",
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
                                        "clustername": '192.168.100.17/CloudStack-Clogeny-Pune/Pune-1',
                                        # Format: http://IP_Address/Datacenter/Cluster
                                        },
                                    },
                       "hosts": {
                                 "xenserver": {
                                # Must be name of corresponding Hypervisor type
                                # in cluster in small letters
                                          "clusterid": 16,
                                          "hypervisor": 'XenServer',
                                          # Hypervisor type
                                          "clustertype": 'ExternalManaged',
                                          # CloudManaged or ExternalManaged"
                                          "url": 'http://192.168.100.210',
                                          "username": "administrator",
                                          "password": "fr3sca",
                                          },
                                 "kvm": {
                                          "clusterid": 35,
                                          "hypervisor": 'KVM',
                                          # Hypervisor type
                                          "clustertype": 'CloudManaged',
                                          # CloudManaged or ExternalManaged"
                                          "url": 'http://192.168.100.212',
                                          "username": "root",
                                          "password": "fr3sca",
                                          },
                                 "vmware": {
                                          "clusterid": 16,
                                          "hypervisor": 'VMware',
                                          # Hypervisor type
                                          "clustertype": 'ExternalManaged',
                                          # CloudManaged or ExternalManaged"
                                          "url": 'http://192.168.100.203',
                                          "username": "administrator",
                                          "password": "fr3sca",
                                         },
                                 }
                       }

class TestHosts(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.services = Services().services
        self.cleanup = []

        # Get Zone and pod
        self.zone = get_zone(self.apiclient)
        self.pod = get_pod(self.apiclient, self.zone.id)

        self.services["clusters"][0]["zoneid"] = self.zone.id
        self.services["clusters"][1]["zoneid"] = self.zone.id
        self.services["clusters"][2]["zoneid"] = self.zone.id

        self.services["clusters"][0]["podid"] = self.pod.id
        self.services["clusters"][1]["podid"] = self.pod.id
        self.services["clusters"][2]["podid"] = self.pod.id

        self.services["hosts"]["xenserver"]["zoneid"] = self.zone.id
        self.services["hosts"]["kvm"]["zoneid"] = self.zone.id
        self.services["hosts"]["vmware"]["zoneid"] = self.zone.id

        self.services["hosts"]["xenserver"]["podid"] = self.pod.id
        self.services["hosts"]["kvm"]["podid"] = self.pod.id
        self.services["hosts"]["vmware"]["podid"] = self.pod.id

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
            cluster = Cluster.create(self.apiclient, v)

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
            cmd = listHosts.listHostsCmd()
            cmd.clusterid = cluster.id
            response = self.apiclient.listHosts(cmd)

            if not response:
                hypervisor_type = str(cluster.hypervisortype.lower())
                host = Host.create(
                               self.apiclient,
                               cluster,
                               self.services["hosts"][hypervisor_type]
                               )

            #Cleanup Host & Cluster
            self.cleanup.append(host)
            self.cleanup.append(cluster)

            cmd = listHosts.listHostsCmd()
            cmd.clusterid = cluster.id
            list_hosts_response = self.apiclient.listHosts(cmd)

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
            cmd = listClusters.listClustersCmd()
            cmd.id = cluster.id
            list_cluster_response = self.apiclient.listClusters(cmd)

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
