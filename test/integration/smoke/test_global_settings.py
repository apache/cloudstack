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
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
#Import System modules

class TestUpdateConfigWithScope(cloudstackTestCase):
    """
    Test to update a configuration (global setting) at various scopes
    """
    def setUp(self):
        self.apiClient = self.testClient.getApiClient()

    @attr(tags=["devcloud", "basic", "advanced"], required_hardware="false")
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
        listConfigurationsCmd.name = updateConfigurationResponse.name
        listConfigurationsCmd.scopename = "zone"
        listConfigurationsCmd.scopeid = 1
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)

        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list API \
                            returns a non-empty response")

        for item in listConfigurationsResponse:
            if item.name == updateConfigurationResponse.name:
                configParam = item

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

class TestListConfigurations(cloudstackTestCase):
    """
    Test to list configurations (global settings)
    """
    @classmethod
    def setUpClass(cls):
        cls.apiclient = cls.testClient.getApiClient()
        cls._cleanup = []

    @classmethod
    def tearDownClass(cls):
        super(TestListConfigurations, cls).tearDownClass()

    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        super(TestListConfigurations, self).tearDown()

    @attr(tags=["devcloud", "basic", "advanced"], required_hardware="false")
    def test_01_list_configs(self):
        """
        test list configuration setting at global level
        @return:
        """
        listConfigurationsCmd = listConfigurations.listConfigurationsCmd()

        listConfigurationsCmd.name = "agent.lb.enabled"
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)
        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list configurations API returns a non-empty response")
        self.debug("The parameter %s listed with value %s" %(listConfigurationsCmd.name, listConfigurationsResponse[0].value))
        self.assertEqual(listConfigurationsResponse[0].type, 'Boolean', "Wrong type for the config")
        self.assertEqual(listConfigurationsResponse[0].defaultvalue, 'false', "Wrong default value for the config")
        self.assertEqual(listConfigurationsResponse[0].group, 'Management Server', "Check the group for the config")
        self.assertEqual(listConfigurationsResponse[0].subgroup, 'Agent', "Check the subgroup for the config")

        listConfigurationsCmd.name = "storage.cleanup.interval"
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)
        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list configurations API returns a non-empty response")
        self.debug("The parameter %s listed with value %s" % (listConfigurationsCmd.name, listConfigurationsResponse[0].value))
        self.assertEqual(listConfigurationsResponse[0].type, 'Number', "Wrong type for the config")
        self.assertEqual(listConfigurationsResponse[0].defaultvalue, '86400', "Wrong default value for the config")
        self.assertEqual(listConfigurationsResponse[0].group, 'Infrastructure', "Check the group for the config")
        self.assertEqual(listConfigurationsResponse[0].subgroup, 'Primary Storage', "Check the subgroup for the config")

        listConfigurationsCmd.name = "agent.load.threshold"
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)
        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list configurations API returns a non-empty response")
        self.debug("The parameter %s listed with value %s" % (listConfigurationsCmd.name, listConfigurationsResponse[0].value))
        self.assertEqual(listConfigurationsResponse[0].type, 'Range', "Wrong type for the config")
        self.assertEqual(listConfigurationsResponse[0].defaultvalue, '0.7', "Wrong default value for the config")
        self.assertEqual(listConfigurationsResponse[0].group, 'Management Server', "Check the group for the config")
        self.assertEqual(listConfigurationsResponse[0].subgroup, 'Agent', "Check the subgroup for the config")

        listConfigurationsCmd.name = "endpoint.url"
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)
        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list configurations API returns a non-empty response")
        self.debug("The parameter %s listed with value %s" % (listConfigurationsCmd.name, listConfigurationsResponse[0].value))
        self.assertEqual(listConfigurationsResponse[0].type, 'String', "Wrong type for the config")
        self.assertEqual(listConfigurationsResponse[0].defaultvalue, 'http://localhost:8080/client/api', "Wrong default value for the config")

    @attr(tags=["devcloud", "basic", "advanced"], required_hardware="false")
    def test_02_list_config_parent(self):
        """
        test list configuration setting parent
        @return:
        """
        listConfigurationsCmd = listConfigurations.listConfigurationsCmd()

        listConfigurationsCmd.name = "api.throttling.cachesize"
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)
        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list configurations API returns a non-empty response")
        self.assertEqual(listConfigurationsResponse[0].parent, 'api.throttling.enabled', "Wrong parent for the config")

        listConfigurationsCmd.name = "storage.cache.replacement.interval"
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)
        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list configurations API returns a non-empty response")
        self.assertEqual(listConfigurationsResponse[0].parent, 'storage.cache.replacement.enabled', "Wrong parent for the config")

        listConfigurationsCmd.name = "cloud.kubernetes.cluster.max.size"
        listConfigurationsResponse = self.apiClient.listConfigurations(listConfigurationsCmd)
        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list configurations API returns a non-empty response")
        self.assertEqual(listConfigurationsResponse[0].parent, 'cloud.kubernetes.service.enabled', "Wrong parent for the config")

    @attr(tags=["devcloud", "basic", "advanced"], required_hardware="false")
    def test_03_config_groups(self):
        """
        test list configuration groups
        @return:
        """
        listConfigurationGroupsResponse = Configurations.listGroups(self.apiclient)
        self.assertNotEqual(len(listConfigurationGroupsResponse), 0, "Check if the list configurationgroups API returns a non-empty response")

        self.debug("Total %d configuration groups listed" %(len(listConfigurationGroupsResponse)))
        self.debug("Configuration groups: %s" % (str(listConfigurationGroupsResponse)))

        group = listConfigurationGroupsResponse[0].name
        subgroup = listConfigurationGroupsResponse[0].subgroup[0].name

        listConfigurationsResponse = Configurations.list(self.apiclient,
                                         group=group,
                                         subgroup=subgroup)
        self.assertNotEqual(len(listConfigurationsResponse), 0, "Check if the list configurations API returns a non-empty response")
        self.debug("Total %d configurations for group %s, subgroup %s" % (len(listConfigurationsResponse), group, subgroup))
