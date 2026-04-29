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
""" Test for baremetal
"""
#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import createVlanIpRange
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (NetworkOffering,
                             NetworkServiceProvider,
                             PhysicalNetwork,
                             Network,
                             Pod)
#from marvin.lib.common import *
from nose.plugins.attrib import attr

#Import System modules
_multiprocess_shared_ = True

class Services:
    """Test Baremetal
    """

    def __init__(self):
        self.services = {
                         "network_offering": {
                                    "name": 'Baremetal_network_offering',
                                    "displaytext": 'Baremetal_network_offering',
                                    "guestiptype": 'Shared',
                                    "supportedservices": 'Dhcp,UserData,BaremetalPxeService',
                                    "specifyVlan": "true",
                                    "specifyIpRanges": "true",
                                    "traffictype": 'GUEST',
                                    "availability": 'Optional',
                                    "conservemode": 'false',
                                    "serviceProviderList": {
                                            "Dhcp": 'BaremetalDhcpProvider',
                                            "UserData": 'BaremetalUserdataProvider',
                                            "BaremetalPxeService": 'BaremetalPxeProvider',
                                        },
                                    },
                         "network" :{
                                     "name" : "defaultBaremetalNetwork",
                                     "displaytext" : "defaultBaremetalNetwork",
                                     },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         # Cent OS 5.3 (64 bit)
                         "sleep": 60,
                         "timeout": 10,
                    }
class TestBaremetal(cloudstackTestCase):
    zoneid = 1

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.services = Services().services
        self.cleanup = []

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["baremetal", "invalid"])
    def test_baremetal(self):
        self.debug("Test create baremetal network offering")
        networkoffering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        networkoffering.update(self.apiclient, state="Enabled")
        self.cleanup.append(networkoffering)

        physical_network = PhysicalNetwork.list(self.apiclient, zoneid=self.zoneid)[0];
        dhcp_provider = NetworkServiceProvider.list(self.apiclient, name="BaremetalDhcpProvider", physical_network_id=physical_network.id)[0]
        NetworkServiceProvider.update(
                                          self.apiclient,
                                          id=dhcp_provider.id,
                                          state='Enabled'
                                          )
        pxe_provider = NetworkServiceProvider.list(self.apiclient, name="BaremetalPxeProvider", physical_network_id=physical_network.id)[0]
        NetworkServiceProvider.update(
                                          self.apiclient,
                                          id=pxe_provider.id,
                                          state='Enabled'
                                          )
        userdata_provider = NetworkServiceProvider.list(self.apiclient, name="BaremetalUserdataProvider", physical_network_id=physical_network.id)[0]
        NetworkServiceProvider.update(
                                          self.apiclient,
                                          id=userdata_provider.id,
                                          state='Enabled'
                                          )

        network = Network.create(self.apiclient, self.services["network"], zoneid=self.zoneid, networkofferingid=networkoffering.id)
        self.cleanup.insert(0, network)

        pod = Pod.list(self.apiclient)[0]
        cmd = createVlanIpRange.createVlanIpRangeCmd()
        cmd.podid = pod.id
        cmd.networkid = network.id
        cmd.gateway = "10.1.1.1"
        cmd.netmask = "255.255.255.0"
        cmd.startip = "10.1.1.20"
        cmd.endip = "10.1.1.40"
        cmd.forVirtualNetwork="false"
        self.apiclient.createVlanIpRange(cmd)
