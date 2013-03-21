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

import marvin
import unittest
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from time import sleep as delay

class TestSetupSuccess(cloudstackTestCase):
    """
    Test to verify if the cloudstack is ready to launch tests upon
    1. Verify that system VMs are up and running in all zones
    2. Verify that built-in templates are Ready in all zones
    """
    @classmethod
    def setUpClass(cls):
        cls.apiClient = super(TestSetupSuccess, cls).getClsTestClient().getApiClient()
        
        zones = listZones.listZonesCmd()
        cls.zones_list = cls.apiClient.listZones(zones)
        cls.retry = 2
        
    def test_systemVmReady(self):
        """
        system VMs need to be ready and Running for each zone in cloudstack
        """
        for z in self.zones_list:
            retry = self.retry
            while retry != 0:
                self.debug("looking for system VMs in zone: %s, %s"%(z.id, z.name))
                sysvms = listSystemVms.listSystemVmsCmd()
                sysvms.zoneid = z.id
                sysvms.state = 'Running'
                sysvms_list = self.apiClient.listSystemVms(sysvms)
                if sysvms_list is not None and len(sysvms_list) == 2:
                    assert len(sysvms_list) == 2
                    self.debug("found %d system VMs running {%s}"%(len(sysvms_list), sysvms_list))
                    break
                retry = retry - 1
                delay(60) #wait a minute for retry
            self.assertNotEqual(retry, 0, "system VMs not Running in zone %s"%z.name)
    
    def test_templateBuiltInReady(self):
        """
        built-in templates CentOS to be ready
        """
        for z in self.zones_list:
            retry = self.retry
            while retry != 0:
                self.debug("Looking for at least one ready builtin template")
                templates = listTemplates.listTemplatesCmd()
                templates.templatefilter = 'featured'
                templates.listall = 'true'
                templates_list = self.apiClient.listTemplates(templates)
                if templates_list is not None:
                    builtins = [tmpl for tmpl in templates_list if tmpl.templatetype == 'BUILTIN' and tmpl.isready == True]
                    if len(builtins) > 0:
                        self.debug("Found %d builtins ready for use %s"%(len(builtins), builtins))
                        break
                retry = retry - 1
                delay(60) #wait a minute for retry
            self.assertNotEqual(retry, 0, "builtIn templates not ready in zone %s"%z.name)

    def test_deployVmWithBuiltIn(self):
        """
        Deploys a VM with the built-in CentOS template
        """

    @classmethod
    def tearDownClass(cls):
        pass
