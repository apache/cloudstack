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
                             Domain)
from marvin.lib.common import (get_domain,
                               get_zone)

class TestDedicatePublicIPRange(cloudstackTestCase):

    @classmethod
    def setUpClass(self):
        self.testClient = super(
            TestDedicatePublicIPRange,
            self).getClsTestClient()
        self.apiclient = self.testClient.getApiClient()
        self.testdata = self.testClient.getParsedTestDataConfig()
        self.hypervisor = self.testClient.getHypervisorInfo()
        # Get Zone, Domain
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self._cleanup = []
        return

    @classmethod
    def tearDownClass(self):
        try:
            # Cleanup resources used
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

        return

    def tearDown(self):
        try:
            # Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_01_disable_account_settings_for_domain(self):
        """Disable account settings for domain

        # Validate the following:
        # (1) Change global setting enable.account.settings.for.domain to false (default value)
        # (2) create domain/account
        # (3) list global settings vmsnapshot.expire.interval and get value (original value)
        # (4) list account settings with name=(3), value should be same as (3)
        # (5) list domain settings with name=(3), should return null/empty
        # (6) change global settings (3) to original +10
        # (7) list account settings with name=(3), value should be same as (6)
        # (8) change account settings (4) to original +20
        # (9) list account settings with name=(3), value should be same as (8)
        # (10) update domain settings with name=(3), should get exception
        # (11) reset vmsnapshot.expire.interval to original value (3)
        """

        config_name = "vmsnapshot.expire.interval"

        # (1) Change global setting enable.account.settings.for.domain to false (default value)
        Configurations.update(
            self.apiclient,
            name="enable.account.settings.for.domain",
            value="false"
        )
        Configurations.update(
            self.apiclient,
            name="enable.domain.settings.for.child.domain",
            value="false"
        )

        # (2) create domain/account
        user_domain = Domain.create(
            self.apiclient,
            services=self.testdata["acl"]["domain2"],
            parentdomainid=self.domain.id)
        account = Account.create(
            self.apiclient,
            self.testdata["acl"]["accountD2"],
            domainid=user_domain.id
        )
        self.cleanup.append(account)
        self.cleanup.append(user_domain)

        # (3) list global settings vmsnapshot.expire.interval and get value (original value)
        configs = Configurations.list(
            self.apiclient,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get global setting %s " % config_name)
        orig_value = int(configs[0].value)

        # (4) list account settings with name=(3), value should be same as (3)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(orig_value, account_value, "Account setting is not equal to global setting")

        # (5) list domain settings with name=(3), should return null/empty
        configs = Configurations.list(
            self.apiclient,
            domainid=user_domain.id,
            name=config_name)
        self.assertIsNone(configs, "Domain setting %s should not exist" % config_name)

        # (6) change global settings (3) to original +10
        new_value = orig_value + 10
        Configurations.update(
            self.apiclient,
            name=config_name,
            value=new_value
        )

        # (7) list account settings with name=(3), value should be same as (6)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(new_value, account_value, "Account setting is not equal to new value of global setting")

        # (8) change account settings (4) to original +20
        new_value = orig_value + 20
        Configurations.update(
            self.apiclient,
            accountid=account.id,
            name=config_name,
            value=new_value
        )

        # (9) list account settings with name=(3), value should be same as (8)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(new_value, account_value, "Account setting is not equal to new value of account setting")

        # (10) update domain settings with name=(3), should get exception
        try:
            Configurations.update(
                self.apiclient,
                domainid=user_domain.id,
                name=config_name,
                value=new_value
            )
            self.fail("Updating domain setting should fail")
        except Exception as e:
            self.debug("Updating domain setting failed as expected with Exception %s" % e)

        # (11) reset vmsnapshot.expire.interval to original value (3)
        Configurations.update(
            self.apiclient,
            name=config_name,
            value=orig_value
        )

        return

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_02_enable_account_settings_for_domain(self):
        """Enable account settings for domain

        # Validate the following:
        # (1) Change global setting enable.account.settings.for.domain to true
        # (2) create domain/account
        # (3) list global settings vmsnapshot.expire.interval and get value (original value)
        # (4) list domain settings with name=(3), value should be same as (3)
        # (5) list account settings with name=(3), value should be same as (4) = (5)
        # (6) change global settings (3) to original +10
        # (7) list domain settings with name=(3), value should be same as (6)
        # (8) list account settings with name=(3), value should be same as (6)=(7)
        # (9) change ROOT domain settings (4) to original +20
        # (10) list domain settings with name=(3), value should be same as (9)
        # (11) list account settings with name=(3), value should be same as (9)=(10)
        # (12) change domain settings (4) to original +30
        # (13) list domain settings with name=(3), value should be same as (12)
        # (14) list account settings with name=(3), value should be same as (12)=(13)
        # (15) change account settings (4) to original +40
        # (16) list account settings with name=(3), value should be same as (15)
        # (17) reset vmsnapshot.expire.interval to original value (3)
        """

        config_name = "vmsnapshot.expire.interval"

        # (1) Change global setting enable.account.settings.for.domain to true
        Configurations.update(
            self.apiclient,
            name="enable.account.settings.for.domain",
            value="true"
        )

        # (2) create domain/account
        user_domain = Domain.create(
            self.apiclient,
            services=self.testdata["acl"]["domain2"],
            parentdomainid=self.domain.id)
        account = Account.create(
            self.apiclient,
            self.testdata["acl"]["accountD2"],
            domainid=user_domain.id
        )
        self.cleanup.append(account)
        self.cleanup.append(user_domain)

        # (3) list global settings vmsnapshot.expire.interval and get value (original value)
        configs = Configurations.list(
            self.apiclient,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get global setting %s " % config_name)
        orig_value = int(configs[0].value)

        # (4) list domain settings with name=(3), value should be same as (3)
        configs = Configurations.list(
            self.apiclient,
            domainid=user_domain.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get domain setting %s " % config_name)
        domain_value = int(configs[0].value)
        self.assertEqual(orig_value, domain_value, "Domain setting is not equal to global setting")

        # (5) list account settings with name=(3), value should be same as (4) = (5)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(orig_value, account_value, "Account setting is not equal to global setting")

        # (6) change global settings (3) to original +10
        new_value = orig_value + 10
        Configurations.update(
            self.apiclient,
            name=config_name,
            value=new_value
        )

        # (7) list domain settings with name=(3), value should be same as (6)
        configs = Configurations.list(
            self.apiclient,
            domainid=user_domain.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get domain setting %s " % config_name)
        domain_value = int(configs[0].value)
        self.assertEqual(new_value, domain_value, "Domain setting is not equal to new value of global setting")

        # (8) list account settings with name=(3), value should be same as (6)=(7)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(new_value, account_value, "Account setting is not equal to new value of global setting")

        old_domain_value = new_value

        # (9) change ROOT domain settings (4) to original +20
        new_value = orig_value + 20
        Configurations.update(
            self.apiclient,
            domainid=self.domain.id,
            name=config_name,
            value=new_value
        )

        # (10) list domain settings with name=(3), value should be same as (9)
        configs = Configurations.list(
            self.apiclient,
            domainid=user_domain.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get domain setting %s " % config_name)
        domain_value = int(configs[0].value)
        self.assertEqual(old_domain_value, domain_value, "Domain setting is not equal to new value of ROOT domain setting")

        # (11) list account settings with name=(3), value should be same as (9)=(10)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(old_domain_value, account_value, "Account setting is not equal to new value of ROOT domain setting")

        # (12) change domain settings (4) to original +30
        new_value = orig_value + 30
        Configurations.update(
            self.apiclient,
            domainid=user_domain.id,
            name=config_name,
            value=new_value
        )

        # (13) list domain settings with name=(3), value should be same as (12)
        configs = Configurations.list(
            self.apiclient,
            domainid=user_domain.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get domain setting %s " % config_name)
        domain_value = int(configs[0].value)
        self.assertEqual(new_value, domain_value, "Domain setting is not equal to new value of domain setting")

        # (14) list account settings with name=(3), value should be same as (12)=(13)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(old_domain_value, account_value, "Account setting is not equal to new value of domain setting")

        # (15) change account settings (4) to original +40
        new_value = orig_value + 40
        Configurations.update(
            self.apiclient,
            accountid=account.id,
            name=config_name,
            value=new_value
        )

        # (16) list account settings with name=(3), value should be same as (15)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(new_value, account_value, "Account setting is not equal to new value of account setting")

        # (17) reset vmsnapshot.expire.interval to original value (3)
        Configurations.update(
            self.apiclient,
            name=config_name,
            value=orig_value
        )

        Configurations.update(
            self.apiclient,
            name="enable.account.settings.for.domain",
            value="false"
        )

        return

    @attr(tags=["advanced", "advancedsg"], required_hardware="false")
    def test_03_enable_account_settings_for_domain(self):
        """Enable account settings for domain

        # Validate the following:
        # (1) Change global setting enable.account.settings.for.domain to true
        # (2) create domain/account
        # (3) list global settings vmsnapshot.expire.interval and get value (original value)
        # (4) list domain settings with name=(3), value should be same as (3)
        # (5) list account settings with name=(3), value should be same as (4) = (5)
        # (6) change global settings (3) to original +10
        # (7) change global setting enable.domain.settings.for.child.domain to true
        # (8) change domain setting (3) to original +30
        # (9) list domain settings with name=(3), value should be same as (8)
        # (10) list account settings with name=(3), value should be same as (9)=(8)
        # (11) change acount setting (3) to original +50
        # (12) list account settings with name=(3), value should be same as (10)
        """

        config_name = "vmsnapshot.expire.interval"

        # (1) Change global setting enable.account.settings.for.domain to true
        Configurations.update(
            self.apiclient,
            name="enable.account.settings.for.domain",
            value="true"
        )

        # (2) create domain/account
        user_domain = Domain.create(
            self.apiclient,
            services=self.testdata["acl"]["domain2"],
            parentdomainid=self.domain.id)
        account = Account.create(
            self.apiclient,
            self.testdata["acl"]["accountD2"],
            domainid=user_domain.id
        )
        self.cleanup.append(account)
        self.cleanup.append(user_domain)

        # (3) list global settings vmsnapshot.expire.interval and get value (original value)
        configs = Configurations.list(
            self.apiclient,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get global setting %s " % config_name)
        orig_value = int(configs[0].value)

        # (4) list domain settings with name=(3), value should be same as (3)
        configs = Configurations.list(
            self.apiclient,
            domainid=user_domain.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get domain setting %s " % config_name)
        domain_value = int(configs[0].value)
        self.assertEqual(orig_value, domain_value, "Domain setting is not equal to global setting")

        # (5) list account settings with name=(3), value should be same as (4) = (5)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(orig_value, account_value, "Account setting is not equal to global setting")

        # (6) change global settings (3) to original +10
        new_value = orig_value + 10
        Configurations.update(
            self.apiclient,
            name=config_name,
            value=new_value
        )

        # (7) change global setting enable.domain.settings.for.child.domain to true
        Configurations.update(
            self.apiclient,
            name="enable.domain.settings.for.child.domain",
            value="true"
        )

        # (8) change domain setting (3) to original +30
        new_domain_value = domain_value + 30
        Configurations.update(
            self.apiclient,
            name=config_name,
            domainid=user_domain.id,
            value=new_domain_value
        )

        # (9) list domain settings with name=(3), value should be same as (8)
        configs = Configurations.list(
            self.apiclient,
            domainid=user_domain.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get domain setting %s " % config_name)
        domain_value = int(configs[0].value)
        self.assertEqual(new_domain_value, domain_value, "Domain setting is not equal to new value of global setting")

        # (10) list account settings with name=(3), value should be same as (9)=(8)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(new_domain_value, account_value, "Account setting is not equal to new value of global setting")

        # (11) change acount setting (3) to original +50
        new_account_value = account_value + 50
        Configurations.update(
            self.apiclient,
            accountid=account.id,
            name=config_name,
            value=new_account_value
        )

        # (12) list account settings with name=(3), value should be same as (10)
        configs = Configurations.list(
            self.apiclient,
            accountid=account.id,
            name=config_name)
        self.assertIsNotNone(configs, "Fail to get account setting %s " % config_name)
        account_value = int(configs[0].value)
        self.assertEqual(new_account_value, account_value, "Account setting is not equal to new value of global setting")

        Configurations.update(
            self.apiclient,
            name="enable.account.settings.for.domain",
            value="false"
        )
        Configurations.update(
            self.apiclient,
            name="enable.domain.settings.for.child.domain",
            value="false"
        )
