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

# Import Local Modules
import tools.marvin.marvin
from tools.marvin.marvin.cloudstackTestCase import *
from tools.marvin.marvin.cloudstackAPI import *
from tools.marvin.marvin.lib.utils import *
from tools.marvin.marvin.lib.base import *
from tools.marvin.marvin.lib.common import *
from nose.plugins.attrib import attr

# Import System modules
import time


class TestQuotaTariffOrder(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestQuotaTariffOrder, cls).getClsTestClient()
        cls.api_client = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls._cleanup = []
        # Create Account
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name

        return

    @classmethod
    def tearDownClass(cls):
        super(TestQuotaTariffOrder, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.tariffs = []
        return

    def tearDown(self):
        self.delete_tariffs()
        super(TestQuotaTariffOrder, self).tearDown()

    def delete_tariffs(self):
        for tariff in self.tariffs:
            cmd = quotaTariffDelete.quotaTariffDeleteCmd()
            cmd.id = tariff.uuid
            self.api_client.quotaTariffDelete(cmd)

    @attr(
        tags=[
            "advanced",
            "smoke"],
        required_hardware="false")
    def test_01_quota_tariff_order(self):
        """Test Quota Tariff Order
        """

        cmd = quotaTariffCreate.quotaTariffCreateCmd()
        cmd.name = 'tf1'
        cmd.value = '1'
        cmd.activationrule = '10'
        cmd.usagetype = '22'
        cmd.position = '2'
        self.tariffs.append(self.api_client.quotaTariffCreate(cmd))

        cmd = quotaTariffCreate.quotaTariffCreateCmd()
        cmd.name = 'tf2'
        cmd.value = '1'
        cmd.activationrule = 'lastTariffs[lastTariffs.length -1].value + 7'
        cmd.usagetype = '22'
        cmd.position = '3'
        self.tariffs.append(self.api_client.quotaTariffCreate(cmd))

        cmd = quotaTariffCreate.quotaTariffCreateCmd()
        cmd.name = 'tf3'
        cmd.value = '1'
        cmd.activationrule = 'lastTariffs[lastTariffs.length -2].value + lastTariffs[lastTariffs.length -1].value'
        cmd.usagetype = '22'
        cmd.position = '4'
        self.tariffs.append(self.api_client.quotaTariffCreate(cmd))

        cmd = quotaCredits.quotaCreditsCmd()
        cmd.account = self.account.name
        cmd.domainid = self.domain.id
        cmd.value = 54
        self.api_client.quotaCredits(cmd)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )

        account_id = qresultset[0][0]

        self.debug("SELECT id from `domain` d WHERE uuid = '%s';"
                   % self.domain.id)

        qresultset = self.dbclient.execute(
            "SELECT id from `domain` d WHERE uuid = '%s';"
            % self.domain.id
        )

        domain_id = qresultset[0][0]

        self.debug("SELECT id from data_center dc where dc.uuid = '%s';"
                   % self.zone.id)

        qresultset = self.dbclient.execute(
            "SELECT id from data_center dc where dc.uuid = '%s';"
            % self.zone.id
        )

        zone_id = qresultset[0][0]

        start = datetime.datetime.now() + datetime.timedelta(seconds=1)
        end = datetime.datetime.now() + datetime.timedelta(hours=1)

        query = "INSERT INTO cloud_usage.cloud_usage (zone_id,account_id,domain_id,description,usage_display,"
        "usage_type,raw_usage,vm_instance_id,vm_name,offering_id,template_id,usage_id,`type`,`size`,"
        "network_id,start_date,end_date,virtual_size,cpu_speed,cpu_cores,memory,quota_calculated,"
        "is_hidden,state) VALUES ('{}','{}','{}','Test','1 Hrs',22,1,NULL,NULL,NULL,NULL,NULL,"
        "'VirtualMachine',NULL,NULL,'{}','{}',NULL,NULL,NULL,NULL,0,0,NULL);".format(zone_id, account_id, domain_id, start, end)

        self.debug(query)

        self.dbclient.execute(
            query)

        cmd = quotaUpdate.quotaUpdateCmd()
        self.api_client.quotaUpdate(cmd)

        cmd = quotaBalance.quotaBalanceCmd()
        cmd.domainid = self.account.domainid
        cmd.account = self.account.name
        response = self.apiclient.quotaBalance(cmd)

        self.debug(f"Quota Balance: {response.balance}")

        self.assertEqual(response.balance.startquota, 0, f"startQuota is supposed to be 0 but was {response.balance.startquota}")

        return
