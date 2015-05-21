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
Tests primary storage limits during upload volume
"""
import unittest

from ddt import ddt, data
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.codes import PASS, RESOURCE_PRIMARY_STORAGE, FAIL, USER_ACCOUNT
from marvin.lib.base import Domain, Account, VirtualMachine, DiskOffering, ServiceOffering, Volume
from marvin.lib.common import get_domain, get_zone, update_resource_limit, uploadVolume, matchResourceCount, get_template
from marvin.lib.utils import cleanup_resources, validateList
from nose.plugins.attrib import attr


@ddt
class TestPrimaryResourceLimitsVolume(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cloudstacktestclient = super(TestPrimaryResourceLimitsVolume,
                                     cls).getClsTestClient()
        cls.api_client = cloudstacktestclient.getApiClient()
        cls.hypervisor = cloudstacktestclient.getHypervisorInfo()
        # Fill services from the external config file
        cls.services = cloudstacktestclient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cloudstacktestclient.getZoneForTests())
        cls.services["mode"] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        cls.template = get_template(cls.api_client, cls.zone.id, cls.services["ostype"])
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id
        cls.services["volume"]["zoneid"] = cls.zone.id
        try:
            cls.service_offering = ServiceOffering.create(cls.api_client, cls.services["service_offering"])
            cls.services["disk_offering"]["disksize"] = 2
            cls.disk_offering = DiskOffering.create(cls.api_client, cls.services["disk_offering"])
            cls._cleanup.append(cls.service_offering)
            cls._cleanup.append(cls.disk_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Exception in setUpClass: %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
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
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setupNormalAccount(self):
        """Setup the account required for the test"""

        try:
            self.domain = Domain.create(self.apiclient,
                                        services=self.services["domain"],
                                        parentdomainid=self.domain.id)

            self.account = Account.create(self.apiclient, self.services["account"],
                                          domainid=self.domain.id, admin=False)
            self.cleanup.append(self.account)
            self.cleanup.append(self.domain)

            self.virtualMachine = VirtualMachine.create(self.api_client, self.services["virtual_machine"],
                                                        accountid=self.account.name, domainid=self.account.domainid,
                                                        diskofferingid=self.disk_offering.id,
                                                        serviceofferingid=self.service_offering.id)

            accounts = Account.list(self.apiclient, id=self.account.id)

            self.assertEqual(validateList(accounts)[0], PASS,
                             "accounts list validation failed")

            self.initialResourceCount = int(accounts[0].primarystoragetotal)

            primarystoragelimit = self.initialResourceCount
            update_resource_limit(self.api_client, RESOURCE_PRIMARY_STORAGE, account=self.account.name, domainid=self.account.domainid, max=primarystoragelimit)

        except Exception as e:
            return [FAIL, e]
        return [PASS, None]

    # @data(USER_ACCOUNT)
    @attr(tags=["advanced","basic"], required_hardware="true")
    def test_attach_volume_exceeding_primary_limits(self):
        """
        # do
        # 1. create a normal user account and update primary store limits to the current resource count
        # 2. Upload a volume of any size
        # 3. Verify that upload volume succeeds
        # 4. Verify that primary storage count doesnt change
        # 6. Try attaching volume to VM and verify that the attach fails (as the resource limits exceed)
        # 7. Verify that primary storage count doesnt change
        # done
        """
        # create an account, launch a vm with default template and custom disk offering, update the primary store limits to the current primary store resource count
        response = self.setupNormalAccount()
        self.assertEqual(response[0], PASS, response[1])

        # upload volume and verify that the volume is uploaded
        volume = Volume.upload(self.apiclient, self.services["configurableData"]["upload_volume"],
                               zoneid=self.zone.id, account=self.account.name,
                               domainid=self.account.domainid, url="http://people.apache.org/~sanjeev/rajani-thin-volume.vhd")

        volume.wait_for_upload(self.apiclient)
        volumes = Volume.list(self.apiclient, id=volume.id,
                              zoneid=self.zone.id, listall=True)
        validationresult = validateList(volumes)
        assert validationresult[0] == PASS, "volumes list validation failed: %s" % validationresult[2]
        assert str(volumes[0].state).lower() == "uploaded", "Volume state should be 'uploaded' but it is %s" % volumes[0].state

        # verify that the resource count didnt change due to upload volume
        response = matchResourceCount(
            self.apiclient, self.initialResourceCount,
            RESOURCE_PRIMARY_STORAGE,
            accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        # attach the above volume to the vm
        try:
            self.virtualMachine.attach_volume(self.apiclient, volume=volume)
        except Exception as e:
            if "Maximum number of resources of type \'primary_storage\' for account name="+self.account.name in e.message:
                self.assertTrue(True, "there should be primary store resource limit reached exception")
            else:
                self.fail("only resource limit reached exception is expected. some other exception occurred. Failing the test case.")

        # resource count should match as the attach should fail due to reaching resource limits
        response = matchResourceCount(
                self.apiclient, self.initialResourceCount,
                RESOURCE_PRIMARY_STORAGE,
                accountid=self.account.id)
        self.assertEqual(response[0], PASS, response[1])

        return

