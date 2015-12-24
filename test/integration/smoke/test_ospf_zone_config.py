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
""" Test cases for checking quagga API
"""

# Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.utils import (random_gen)
from nose.plugins.attrib import attr

# Import System modules
import time

class TestQuagga(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # Create Account
        testClient = super(TestQuagga, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())

        return

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
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    # Check vpcQuaggaConfigCmd positive test case
    #VPCQuaggaConfigUpdateCmd [zoneid=1, quaggaProtocol=null, ospfArea=null, quaggaHelloInterval=null, quaggaDeadInterval=null, quaggaRetransmitInterval=null, quaggaTransitDelay=null, quaggaAuthentication=null, quaggaPassword=junk, ospfSuperCIDR=null, quaggaEnabled=null]
    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_01_quaggaConfig(self):
        
        cmd = vpcQuaggaConfig.vpcQuaggaConfigCmd()
        cmd.zoneid = self.zone.id
        response = self.apiclient.vpcQuaggaConfig(cmd)
        self.debug("Response quaggaEnabled: %s" % response.quaggaEnabled)
        self.debug("Response quaggaPassword: %s" % response.quaggaPassword)
        self.debug("Response ospfSuperCIDR: %s" % response.ospfSuperCIDR)
        
        
        cmd = vpcQuaggaConfigUpdate.vpcQuaggaConfigUpdateCmd()
        cmd.zoneid = self.zone.id
        cmd.ospfSuperCIDR = "200.100.0.0/18,216.100.0.0/20"
        cmd.quaggaEnabled = "true"
        cmd.quaggaPassword = "6P33lsadEbmiJ7ZN7gK8vylOY2dTza3rAuY8UFboH7I=:Xb1G4VhzBkSXmTa8vDqExQQo8PiIUUSNtpxmdOkX1z8="
        response = self.apiclient.vpcQuaggaConfigUpdate(cmd)
        self.debug("Response quaggaEnabled: %s" % response.quaggaEnabled)
        self.debug("Response quaggaPassword: %s" % response.quaggaPassword)
        self.debug("Response ospfSuperCIDR: %s" % response.ospfSuperCIDR)
        
        
        cmd = vpcQuaggaConfig.vpcQuaggaConfigCmd()
        cmd.zoneid = self.zone.id
        response = self.apiclient.vpcQuaggaConfig(cmd)
        self.debug("Response quaggaEnabled: %s" % response.quaggaEnabled)
        self.debug("Response quaggaPassword: %s" % response.quaggaPassword)
        self.debug("Response ospfSuperCIDR: %s" % response.ospfSuperCIDR)
        
        cmd.zoneid = self.zone.id
        
        
        self.assertEqual(
                         response.quaggaEnabled, "true"
                         )
        
        
        self.assertEqual(
                         response.quaggaPassword, "6P33lsadEbmiJ7ZN7gK8vylOY2dTza3rAuY8UFboH7I=:Xb1G4VhzBkSXmTa8vDqExQQo8PiIUUSNtpxmdOkX1z8="
                         )
        
        self.assertEqual(
                         response.ospfSuperCIDR, "200.100.0.0/18,216.100.0.0/20"
                         )
        
        return

        
    # Check vpcQuaggaConfigCmd positive test case
    #VPCQuaggaConfigUpdateCmd [zoneid=1, quaggaProtocol=null, ospfArea=null, quaggaHelloInterval=null, quaggaDeadInterval=null, quaggaRetransmitInterval=null, quaggaTransitDelay=null, quaggaAuthentication=null, quaggaPassword=junk, ospfSuperCIDR=null, quaggaEnabled=null]
    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_02_quaggaConfig(self):
        
        cmd = vpcQuaggaConfig.vpcQuaggaConfigCmd()
        cmd.zoneid = self.zone.id
        response = self.apiclient.vpcQuaggaConfig(cmd)
        self.debug("Response quaggaEnabled: %s" % response.quaggaEnabled)
        self.debug("Response quaggaPassword: %s" % response.quaggaPassword)
        self.debug("Response ospfSuperCIDR: %s" % response.ospfSuperCIDR)
        
        
        cmd = vpcQuaggaConfigUpdate.vpcQuaggaConfigUpdateCmd()
        cmd.zoneid = self.zone.id
        cmd.ospfSuperCIDR = "200.100.0.0/18,216.100.0.0/20"
        cmd.quaggaEnabled = "false"
        cmd.quaggaPassword = "6P33lsadEbmiJ7ZN7gK8vylOY2dTza3rAuY8UFboH7I=:Xb1G4VhzBkSXmTa8vDqExQQo8PiIUUSNtpxmdOkX1z8="
        response = self.apiclient.vpcQuaggaConfigUpdate(cmd)
        self.debug("Response quaggaEnabled: %s" % response.quaggaEnabled)
        self.debug("Response quaggaPassword: %s" % response.quaggaPassword)
        self.debug("Response ospfSuperCIDR: %s" % response.ospfSuperCIDR)
        
        
        cmd = vpcQuaggaConfig.vpcQuaggaConfigCmd()
        cmd.zoneid = self.zone.id
        response = self.apiclient.vpcQuaggaConfig(cmd)
        self.debug("Response quaggaEnabled: %s" % response.quaggaEnabled)
        self.debug("Response quaggaPassword: %s" % response.quaggaPassword)
        self.debug("Response ospfSuperCIDR: %s" % response.ospfSuperCIDR)
        
        cmd.zoneid = self.zone.id
        
        
        self.assertEqual(
                         response.quaggaEnabled, "false"
                         )
        
        
        self.assertEqual(
                         response.quaggaPassword, "6P33lsadEbmiJ7ZN7gK8vylOY2dTza3rAuY8UFboH7I=:Xb1G4VhzBkSXmTa8vDqExQQo8PiIUUSNtpxmdOkX1z8="
                         )
        
        self.assertEqual(
                         response.ospfSuperCIDR, "200.100.0.0/18,216.100.0.0/20"
                         )
        
        return

       