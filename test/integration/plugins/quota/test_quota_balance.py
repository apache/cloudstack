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
"""
Test cases for validating the Quota balance of accounts
"""

from marvin.cloudstackTestCase import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr


class TestQuotaBalance(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestQuotaBalance, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone

        # Create Account
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup = [
            cls.account,
        ]

        cls.services["account"] = cls.account.name

        if not is_config_suitable(apiclient=cls.apiclient, name='quota.enable.service', value='true'):
            cls.debug("Quota service is not enabled, therefore the configuration `quota.enable.service` will be set to `true` and the management server will be restarted.")
            Configurations.update(cls.apiclient, "quota.enable.service", "true")
            cls.restartServer()

        return

    @classmethod
    def restartServer(cls):
        """Restart management server"""

        cls.debug("Restarting management server")
        sshClient = SshClient(
            cls.mgtSvrDetails["mgtSvrIp"],
            22,
            cls.mgtSvrDetails["user"],
            cls.mgtSvrDetails["passwd"]
        )

        command = "service cloudstack-management restart"
        sshClient.execute(command)

        # Waits for management to come up in 5 mins, when it's up it will continue
        timeout = time.time() + 300
        while time.time() < timeout:
            if cls.isManagementUp() is True:
                time.sleep(30)
                return
            time.sleep(5)
        return cls.fail("Management server did not come up, failing")

    @classmethod
    def isManagementUp(cls):
        try:
            cls.apiclient.listInfrastructure(listInfrastructure.listInfrastructureCmd())
            return True
        except Exception:
            return False

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.tariffs = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
            self.delete_tariffs()
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def delete_tariffs(self):
        for tariff in self.tariffs:
            cmd = quotaTariffDelete.quotaTariffDeleteCmd()
            cmd.id = tariff.uuid
            self.apiclient.quotaTariffDelete(cmd)

    @attr(tags=["advanced", "smoke", "quota"], required_hardware="false")
    def test_quota_balance(self):
        """
        Test Quota balance

        Validate the following
        1. Add credits to an account
        2. Create Quota tariff for the usage type 21 (VM_DISK_IO_READ)
        3. Simulate quota usage by inserting a row in the `cloud_usage` table
        4. Update the balance of the account by calling the API quotaUpdate
        5. Verify the balance of the account according to the tariff created
        """

        # Create quota tariff for the usage type 21 (VM_DISK_IO_READ)
        cmd = quotaTariffCreate.quotaTariffCreateCmd()
        cmd.name = 'Tariff'
        cmd.value = '10'
        cmd.usagetype = '21'
        self.tariffs.append(self.apiclient.quotaTariffCreate(cmd))

        # Add credits to the account
        cmd = quotaCredits.quotaCreditsCmd()
        cmd.account = self.account.name
        cmd.domainid = self.domain.id
        cmd.value = 100
        self.apiclient.quotaCredits(cmd)

        # Fetch account ID from account_uuid
        account_id_select = f"SELECT id FROM account WHERE uuid = '{self.account.id}';"
        self.debug(account_id_select)
        qresultset = self.dbclient.execute(account_id_select)
        account_id = qresultset[0][0]

        # Fetch domain ID from domain_uuid
        domain_id_select = f"SELECT id FROM `domain` d WHERE uuid = '{self.domain.id}';"
        self.debug(domain_id_select)
        qresultset = self.dbclient.execute(domain_id_select)
        domain_id = qresultset[0][0]

        # Fetch zone ID from zone_uuid
        zone_id_select = f"SELECT id from data_center dc where dc.uuid = '{self.zone.id}';"
        self.debug(zone_id_select)
        qresultset = self.dbclient.execute(zone_id_select)
        zone_id = qresultset[0][0]

        start_date = datetime.datetime.now() + datetime.timedelta(seconds=1)
        end_date = datetime.datetime.now() + datetime.timedelta(hours=1)

        # Manually insert a usage regarding the usage type 21 (VM_DISK_IO_READ)
        sql_query = (f"INSERT INTO cloud_usage.cloud_usage (zone_id,account_id,domain_id,description,usage_display,usage_type,raw_usage,vm_instance_id,vm_name,offering_id,template_id,"
                     f"usage_id,`type`,`size`,network_id,start_date,end_date,virtual_size,cpu_speed,cpu_cores,memory,quota_calculated,is_hidden,state)"
                     f" VALUES ('{zone_id}','{account_id}','{domain_id}','Test','1 Hrs',21,1,NULL,NULL,NULL,NULL,NULL,'VirtualMachine',NULL,NULL,'{start_date}','{end_date}',NULL,NULL,NULL,NULL,0,0,NULL);")
        self.debug(sql_query)
        self.dbclient.execute(sql_query)

        # Update quota to calculate the balance of the account
        cmd = quotaUpdate.quotaUpdateCmd()
        self.apiclient.quotaUpdate(cmd)

        # Retrieve the quota balance of the account
        cmd = quotaBalance.quotaBalanceCmd()
        cmd.domainid = self.account.domainid
        cmd.account = self.account.name
        response = self.apiclient.quotaBalance(cmd)

        self.debug(f"The quota balance for the account {self.account.name} is {response.balance}.")
        self.assertEqual(response.balance.startquota, 90, f"The `startQuota` response field is supposed to be 90 but was {response.balance.startquota}.")

        return
