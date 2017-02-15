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


from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
import cmd
from cmd import Cmd


class TestHaForHost(cloudstackTestCase):
    """ Test cases for configuring HA for Host
    """

    def setUp(self):
        testClient = super(TestHaForHost, self).getClsTestClient()

        self.apiclient = testClient.getApiClient()
        self.dbclient = testClient.getDbConnection()
        self.services = testClient.getParsedTestDataConfig()

        self.zone = get_zone(self.apiclient, testClient.getZoneForTests())
        self.host = None
        self.server = None

        self.cleanup = []

    def tearDown(self):
        try:
            self.dbclient.execute("delete from ha_config where resource_type='Host'")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)


    def getHost(self, hostId=None):
        if self.host and hostId is None:
            return self.host

        response = list_hosts(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing',
            id=hostId
        )
        if len(response) > 0:
            self.host = response[0]
            return self.host
        raise self.skipTest("No hosts found, skipping HA for Host test")


    def getHaProvider(self, host):
        cmd = listHostHAProviders.listHostHAProvidersCmd()
        cmd.hypervisor = host.hypervisor
        response = self.apiclient.listHostHAProviders(cmd)
        return response[0].haprovider


    def configureHaProvider(self):
        cmd = configureHAForHost.configureHAForHostCmd()
        cmd.hostid = self.getHost().id
        cmd.provider = self.getHaProvider(self.getHost())
        return self.apiclient.configureHAForHost(cmd)


    def getHaForHostEnableCmd(self):
        cmd = enableHAForHost.enableHAForHostCmd()
        cmd.hostid = self.getHost().id
        return cmd


    def getHaForHostDisableCmd(self):
        cmd = disableHAForHost.disableHAForHostCmd()
        cmd.hostid = self.getHost().id
        return cmd


    def getListHostHAResources(self):
        cmd = listHostHAResources.listHostHAResourcesCmd()
        cmd.hostid = self.getHost().id
        return cmd


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_enable_ha_for_host(self):
        """
            This test enables HA for a host
        """

        self.configureHaProvider()
        cmd = self.getHaForHostEnableCmd()
        response = self.apiclient.enableHAForHost(cmd)

        self.assertEqual(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, True)


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_enable_ha_for_host_invalid(self):
        """
            This is a negative test for enable HA for a host
        """

        self.configureHaProvider()
        cmd = self.getHaForHostEnableCmd()
        cmd.hostid = -1

        try:
            response = self.apiclient.enableHAForHost(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_disable_ha_for_host(self):
        """
            This test disables HA for a host
        """

        self.configureHaProvider()
        cmd = self.getHaForHostDisableCmd()

        response = self.apiclient.disableHAForHost(cmd)

        self.assertTrue(response.hostid, cmd.hostid)
        self.assertEqual(response.haenable, False)

        response = self.getHost(cmd.hostid)

        self.assertEqual(response.hostha.hastate, "Disabled")


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_disable_ha_for_host_invalid(self):
        """
            This is a negative test for disable HA for a host
        """

        self.configureHaProvider()
        cmd = self.getHaForHostDisableCmd()
        cmd.hostid = -1

        try:
            response = self.apiclient.disableHAForHost(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_list_ha_for_host(self):
        """
            Test that verifies the listHAForHost API
        """
        self.configureHaProvider()
        db_count = self.dbclient.execute("SELECT count(*) FROM cloud.ha_config")

        cmd = self.getListHostHAResources()
        del cmd.hostid
        response = self.apiclient.listHostHAResources(cmd)

        self.assertEqual(db_count[0][0], len(response))


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_list_ha_for_host_valid(self):
        """
            Valid test for listing a specific host HA resources
        """

        self.configureHaProvider()
        cmd = self.getListHostHAResources()
        response = self.apiclient.listHostHAResources(cmd)
        self.assertEqual(response[0].hostid, cmd.hostid)


    @attr(tags=["advanced",
                "advancedns",
                "smoke",
                "basic",
                "sg"],
          required_hardware="false")
    def test_list_ha_for_host_invalid(self):
        """
            Test that listHostHAResources is returning exception when called with invalid data
        """

        self.configureHaProvider()
        cmd = self.getListHostHAResources()
        cmd.hostid = "someinvalidvalue"

        try:
            response = self.apiclient.listHostHAResources(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")
