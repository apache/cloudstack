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
""" Test cases for checking quota API
"""

#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.utils import (random_gen)
from nose.plugins.attrib import attr

#Import System modules
import time

class TestQuota(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        # Create Account
        testClient = super(TestQuota, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())

        # Create Account
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls._cleanup = [
                        cls.account,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
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
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    #Check quotaTariffList API returning 22 items
    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_01_quota(self):
        if not is_config_suitable(
                apiclient=self.apiclient,
                name='quota.enable.service',
                 value='true'):
             self.skipTest('quota.enable.service should be true. skipping')
             
        cmd = quotaTariffList.quotaTariffListCmd()
        response = self.apiclient.quotaTariffList(cmd)

        self.debug("Number of quota usage types: %s" % len(response))
        self.assertEqual(
                         len(response), 22
                         )
        for quota in response:
            self.debug("Usage Name: %s" % quota.usageName)
            self.assertEqual(
                hasattr(quota, 'usageName'),
                True,
                "Check whether usgaeName field is there"
            )

        return

    #Check quota tariff on a particualr day
    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_02_quota(self):
        if not is_config_suitable(
                apiclient=self.apiclient,
                name='quota.enable.service',
                 value='true'):
             self.skipTest('quota.enable.service should be true. skipping')
             
        cmd = quotaTariffList.quotaTariffListCmd()
        cmd.startdate='2015-07-06'
        response = self.apiclient.quotaTariffList(cmd)

        self.debug("Number of quota usage types: %s" % len(response))
        self.assertEqual(
                         len(response), 22
                         )

        return

    #check quota tariff of a particular item
    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_03_quota(self):
        if not is_config_suitable(
                apiclient=self.apiclient,
                name='quota.enable.service',
                 value='true'):
             self.skipTest('quota.enable.service should be true. skipping')
             
        cmd = quotaTariffList.quotaTariffListCmd()
        cmd.startdate='2015-07-06'
        cmd.usagetype='10'
        response = self.apiclient.quotaTariffList(cmd)

        self.debug("Number of quota usage types: %s" % len(response))
        self.assertEqual(
                         len(response), 1
                         )
        return


    #check quota tariff
    #Change it
    #Check on affective date the new tariff should be applicable
    #check the old tariff it should be same
    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_04_quota(self):
        if not is_config_suitable(
                apiclient=self.apiclient,
                name='quota.enable.service',
                 value='true'):
             self.skipTest('quota.enable.service should be true. skipping')
             
        cmd = quotaTariffList.quotaTariffListCmd()
        cmd.startdate='2015-07-06'
        cmd.usagetype='10'
        response = self.apiclient.quotaTariffList(cmd)

        self.debug("Number of quota usage types: %s" % len(response))
        self.assertEqual(
                         len(response), 1
                         )
        quota = response[0]
        self.debug("Tariff Value for 10: %s" % quota.tariffValue)

        cmd = quotaTariffUpdate.quotaTariffUpdateCmd()
        tomorrow = datetime.date.today() + datetime.timedelta(days=1)
        cmd.startdate=tomorrow
        cmd.usagetype='10'
        cmd.value='2.9'
        response = self.apiclient.quotaTariffUpdate(cmd)

        cmd = quotaTariffList.quotaTariffListCmd()
        cmd.startdate=tomorrow
        cmd.usagetype='10'
        response = self.apiclient.quotaTariffList(cmd)
        self.assertEqual(
                         len(response), 1
                         )
        quota = response[0]
        self.debug("Tariff Value for 10: %s" % quota.tariffValue)

        self.assertEqual( quota.tariffValue, 2.9)


        cmd = quotaTariffList.quotaTariffListCmd()
        cmd.startdate='2015-07-07'
        cmd.usagetype='10'
        response = self.apiclient.quotaTariffList(cmd)
        self.assertEqual(
                         len(response), 1
                         )
        quota = response[0]
        self.debug("Tariff Value for 10: %s" % quota.tariffValue)

        self.assertEqual( quota.tariffValue, 0)

        return


    #Make credit deposit
    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_05_quota(self):
        if not is_config_suitable(
                apiclient=self.apiclient,
                name='quota.enable.service',
                 value='true'):
             self.skipTest('quota.enable.service should be true. skipping')
             
        cmd = quotaCredits.quotaCreditsCmd()
        cmd.domainid = self.account.domainid
        cmd.account = self.account.name
        cmd.value = '10'
        cmd.quota_enforce = '1'
        cmd.min_balance = '9'
        response = self.apiclient.quotaCredits(cmd)

        self.debug("Credit response update on: %s" % response.updated_on)

        return


    #Make credit deposit and check today balance
    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_06_quota(self):
        if not is_config_suitable(
                apiclient=self.apiclient,
                name='quota.enable.service',
                 value='true'):
             self.skipTest('quota.enable.service should be true. skipping')
             
        cmd = quotaBalance.quotaBalanceCmd()
        today = datetime.date.today()
        cmd.domainid = self.account.domainid
        cmd.account = self.account.name
        cmd.startdate = today
        response = self.apiclient.quotaBalance(cmd)

        self.debug("Quota Balance on: %s" % response.startdate)
        self.debug("is: %s" % response.startquota)

        self.assertEqual( response.startquota, 10)
        return

    #make credit deposit and check start and end date balances
    @attr(tags=["smoke", "advanced"], required_hardware="false")
    def test_07_quota(self):
        if not is_config_suitable(
                apiclient=self.apiclient,
                name='quota.enable.service',
                 value='true'):
             self.skipTest('quota.enable.service should be true. skipping')
             
        cmd = quotaBalance.quotaBalanceCmd()
        today = datetime.date.today()
        cmd.domainid = self.account.domainid
        cmd.account = self.account.name
        cmd.startdate = today - datetime.timedelta(days=2)
        cmd.enddate = today 
        response = self.apiclient.quotaBalance(cmd)

        self.debug("Quota Balance on: %s" % response.startdate)
        self.debug("is: %s" % response.startquota)

        self.assertEqual( response.startquota, 0)
        self.assertEqual( response.endquota, 10)
        return
