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
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
#Import System modules

class TestUpdateConfigWithScope(cloudstackTestCase):
    """
    Test to update a configuration (global setting) at various scopes
    """
    def setUp(self):
        self.apiClient = self.testClient.getApiClient()

    def test_UpdateConfigParamWithScope(self):
        """
        test update configuration setting at zone level scope
        @return:
        """
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "use.external.dns"
        updateConfigurationCmd.value = "true"
        updateConfigurationCmd.scopename = "zone"
        updateConfigurationCmd.scopeid = 1

        updateConfigurationResponse = self.apiClient.updateConfiguration(updateConfigurationCmd)
        self.debug("updated the parameter %s with value %s"%(updateConfigurationResponse.name, updateConfigurationResponse.value))

        listConfigurationsCmd = listConfigurations.listConfigurationsCmd()
        listConfigurationsCmd.cfgName = updateConfigurationResponse.name
        listConfigurationsCmd.scopename = "zone"
        listConfigurationsCmd.scopeid = 1
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)

        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list API \
                            returns a non-empty response")

        configParam = listConfigurationsResponse[7]
        self.assertEqual(configParam.value, updateConfigurationResponse.value, "Check if the update API returned \
                         is the same as the one we got in the list API")


    def tearDown(self):
        """
        Reset the configuration back to false
        @return:
        """
        updateConfigurationCmd = updateConfiguration.updateConfigurationCmd()
        updateConfigurationCmd.name = "use.external.dns"
        updateConfigurationCmd.value = "false"
        updateConfigurationCmd.scopename = "zone"
        updateConfigurationCmd.scopeid = 1
        self.apiClient.updateConfiguration(updateConfigurationCmd)
