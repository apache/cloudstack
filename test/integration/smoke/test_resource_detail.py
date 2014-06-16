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
""" P1 tests for Scaling up Vm
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.sshClient import SshClient
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True

class TestResourceDetail(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestResourceDetail, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        domain = get_domain(cls.apiclient)
        zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = zone.networktype

        # Set Zones and disk offerings ??

        # Create account, service offerings, vm.
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=domain.id
                            )

 
        cls.disk_offering = DiskOffering.create(
                                    cls.apiclient,
                                    cls.services["disk_offering"]
                                    )

        #create a volume
        cls.volume = Volume.create(
                                   cls.apiclient,
                                   { "diskname" : "ndm"},
                                   zoneid=zone.id,
                                   account=cls.account.name,
                                   domainid=cls.account.domainid,
                                   diskofferingid=cls.disk_offering.id
                                   )
        #how does it work ??
        cls._cleanup = [
                        cls.volume,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        cls.apiclient = super(TestResourceDetail, cls).getClsTestClient().getApiClient()
        cleanup_resources(cls.apiclient, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags = ["advanced", "xenserver"], required_hardware="false")
    def test_01_updatevolumedetail(self):
        """Test volume detail 
        """
        # Validate the following
      
        
        #remove detail
        self.debug("Testing REMOVE volume detail Volume-ID: %s " % (
                                        self.volume.id
                                        ))
        cmd = removeResourceDetail.removeResourceDetailCmd()
        cmd.resourcetype = "Volume"
        cmd.resourceid = self.volume.id
        self.apiclient.removeResourceDetail(cmd)  
        
        listResourceDetailCmd = listResourceDetails.listResourceDetailsCmd()
        listResourceDetailCmd.resourceid = self.volume.id
        listResourceDetailCmd.resourcetype = "Volume"
        listResourceDetailResponse = self.apiclient.listResourceDetails(listResourceDetailCmd)

        self.assertEqual(listResourceDetailResponse, None, "Check if the list API \
                            returns an empty response")
        
        #TODO - add detail. Map as input

        return
