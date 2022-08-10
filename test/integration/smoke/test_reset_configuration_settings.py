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

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.utils import (validateList,
                              cleanup_resources)
from marvin.lib.base import (Account,
                             Configurations,
                             Domain,
                             Cluster,
                             StoragePool)
from marvin.lib.common import (get_domain,
                               get_zone)

class TestRestConfigurationSettings(cloudstackTestCase):
    @classmethod
    def setUpClass(self):
        self.testClient = super(
            TestRestConfigurationSettings,
            self).getClsTestClient()
        self.apiclient = self.testClient.getApiClient()
        self.testdata = self.testClient.getParsedTestDataConfig()

        # Get Zone, Domain
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self._cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        super(TestRestConfigurationSettings, cls).tearDownClass()
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        return

    def tearDown(self):
        super(TestRestConfigurationSettings, self).tearDown()
        return

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_01_test_settings_for_domain(self):
        """
        1. Get the default value for the setting in domain scope
        2. Change the default value to new value
        3. Make sure updated value is same as new value
        4. Reset the config value
        5. Make sure that current value is same as default value
        :return:
        """
        config_name="ldap.basedn"
        #1. Get default value
        configs = Configurations.list(
            self.apiclient,
            name=config_name
        )
        self.assertIsNotNone(configs, "Fail to get domain setting %s " % config_name)

        orig_value = str(configs[0].value)
        new_value = "testing"

        #2. Update to new value
        Configurations.update(
            self.apiclient,
            name=config_name,
            value=new_value,
            domainid=self.domain.id
        )

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            domainid=self.domain.id
        )
        self.assertIsNotNone(configs, "Fail to get domain setting %s " % config_name)

        #3. validate they are same
        self.assertEqual(new_value,
                         str(configs[0].value),
                         "Failed to set new config value")

        #4. Reset the value
        Configurations.reset(
            self.apiclient,
            name=config_name,
            domainid=self.domain.id
        )

        #5. Make sure its same as original value
        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            domainid=self.domain.id
        )
        self.assertIsNotNone(configs, "Fail to get domain setting %s " % config_name)

        self.assertEqual(orig_value,
                         str(configs[0].value),
                         "Failed to reset the value")

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_02_test_settings_for_account(self):
        """
        1. Get the default value for the setting in account scope
        2. Change the default value to new value
        3. Make sure updated value is same as new value
        4. Reset the config value
        5. Make sure that current value is same as default value
        :return:
        """
        accounts = Account.list(
            self.apiclient,
            domainid=self.domain.id,
            listall=True
        )

        self.assertIsNotNone(accounts[0],
                             "There should be at least 1 account in the zone")

        config_name = "enable.additional.vm.configuration"
        #1. Get the default value
        configs = Configurations.list(
            self.apiclient,
            name=config_name
        )
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)

        orig_value = str(configs[0].value)
        new_value = "true"

        Configurations.update(
            self.apiclient,
            name=config_name,
            value=new_value,
            accountid=accounts[0].id
        )

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            accountid=accounts[0].id
        )
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)

        self.assertEqual(new_value,
                         str(configs[0].value),
                         "Failed to set new config value")

        Configurations.reset(
            self.apiclient,
            name=config_name,
            accountid=accounts[0].id
        )

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            accountid=accounts[0].id
        )
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)

        self.assertEqual(orig_value,
                         str(configs[0].value),
                         "Failed to reset the value")

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_03_test_settings_for_cluster(self):
        """
        1. Get the default value for the setting in cluster scope
        2. Change the default value to new value
        3. Make sure updated value is same as new value
        4. Reset the config value
        5. Make sure that current value is same as default value
        :return:
        """
        cluster = Cluster.list(
            self.apiclient
        )

        self.assertIsNotNone(cluster[0],
                             "There should be at least 1 cluster in the zone")

        config_name = "cluster.storage.operations.exclude"
        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            clusterid=cluster[0].id
        )
        self.assertIsNotNone(configs, "Fail to get cluster setting %s " % config_name)

        orig_value = str(configs[0].value)
        new_value = "true"

        Configurations.update(
            self.apiclient,
            name=config_name,
            value=new_value,
            clusterid=cluster[0].id
        )

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            clusterid=cluster[0].id
        )
        self.assertIsNotNone(configs, "Fail to get cluster setting %s " % config_name)

        self.assertEqual(new_value,
                         str(configs[0].value),
                         "Failed to set new config value")

        Configurations.reset(
            self.apiclient,
            name=config_name,
            clusterid=cluster[0].id
        )

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            clusterid=cluster[0].id
        )
        self.assertIsNotNone(configs, "Fail to get cluster setting %s " % config_name)

        self.assertEqual(orig_value,
                         str(configs[0].value),
                         "Failed to reset the value")

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_04_test_settings_for_storage(self):
        """
        1. Get the default value for the setting in storage scope
        2. Change the default value to new value
        3. Make sure updated value is same as new value
        4. Reset the config value
        5. Make sure that current value is same as default value
        :return:
        """
        storage = StoragePool.list(
            self.apiclient
        )

        self.assertIsNotNone(storage[0],
                             "There should be at least 1 primary storage pool in the zone")

        config_name = "vmware.create.full.clone"
        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            storageid=storage[0].id
        )
        self.assertIsNotNone(configs, "Fail to get storage pool setting %s " % config_name)

        orig_value = str(configs[0].value)
        new_value = 'false'

        Configurations.update(
            self.apiclient,
            name=config_name,
            value=new_value,
            storageid=storage[0].id
        )

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            storageid=storage[0].id
        )
        self.assertIsNotNone(configs, "Fail to get storage pool setting %s " % config_name)

        self.assertEqual(new_value,
                         (configs[0].value),
                         "Failed to set new config value")

        Configurations.reset(
            self.apiclient,
            name=config_name,
            storageid=storage[0].id
        )

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            storage=storage[0].id
        )
        self.assertIsNotNone(configs, "Fail to get storage pool setting %s " % config_name)

        self.assertEqual(orig_value,
                         (configs[0].value),
                         "Failed to reset the value for storage pool")

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_05_test_settings_for_zone(self):
        """
        1. Get the default value for the setting in zone scope
        2. Change the default value to new value
        3. Make sure updated value is same as new value
        4. Reset the config value
        5. Make sure that current value is same as default value
        :return:
        """
        config_name = "enable.dynamic.scale.vm"
        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            zoneid=self.zone.id
        )
        self.assertIsNotNone(configs, "Fail to get zone setting %s " % config_name)

        orig_value = str(configs[0].value)
        new_value = 'true'

        Configurations.update(
            self.apiclient,
            name=config_name,
            value=new_value,
            zoneid=self.zone.id
        )

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            zoneid=self.zone.id
        )
        self.assertIsNotNone(configs, "Fail to get ol setting %s " % config_name)

        self.assertEqual(new_value,
                         (configs[0].value),
                         "Failed to set new config value")

        Configurations.reset(
            self.apiclient,
            name=config_name,
            zoneid=self.zone.id
        )

        configs = Configurations.list(
            self.apiclient,
            name=config_name,
            zoneid=self.zone.id
        )
        self.assertIsNotNone(configs, "Fail to get zone setting %s " % config_name)

        self.assertEqual(orig_value,
                         (configs[0].value),
                         "Failed to reset the value for zone")
