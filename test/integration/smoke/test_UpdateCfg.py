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
""" P1 tests for updating the granular Configuration parameter with scope and resource id provided.
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.remoteSSHClient import remoteSSHClient
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr
#Import System modules
import unittest
import hashlib
import random

class TestUpdateConfigWithScope(cloudstackTestCase):
    """
    This test updates the value of a configuration parameter
    which is at zone level(scope)
    """
    def setUp(self):
        """
        CloudStack internally saves its passwords in md5 form and that is how we
        specify it in the API. Python's hashlib library helps us to quickly hash
        strings as follows
        """
        mdf = hashlib.md5()
        mdf.update('password')
        mdf_pass = mdf.hexdigest()

        self.apiClient = self.testClient.getApiClient() #Get ourselves an API client



    def test_UpdateConfigParamWithScope(self):

        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "use.external.dns"
        updateConfigurationCmd.value = "true"
        updateConfigurationCmd.scope = "zone"
        updateConfigurationCmd.id = 1

        updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)
        self.debug("updated the parameter %s with value %s"%(updateConfigurationResponse.name, updateConfigurationResponse.value))

        listConfigurationsCmd = listConfigurations.listConfigurationsCmd()
        listConfigurationsCmd.cfgName = updateConfigurationResponse.name
        listConfigurationsCmd.scope = "zone"
        listConfigurationsCmd.id = 1
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)

        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list API \
                            returns a non-empty response")

        configParam = listConfigurationsResponse[0]

        self.assertEqual(configParam.value, updateConfigurationResponse.value, "Check if the update API returned \
                         is the same as the one we got in the list API")


    def tearDown(self):

        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "use.external.dns"
        updateConfigurationCmd.value = "false"
        updateConfigurationCmd.scope = "zone"
        updateConfigurationCmd.id = 1
        self.apiClient.updateConfiguration(updateConfigurationCmd)
