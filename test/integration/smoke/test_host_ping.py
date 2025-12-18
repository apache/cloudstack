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
""" Check state transition of host from Alert to Up on Ping
"""

# Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.lib.common import *
from marvin.lib.utils import *
from nose.plugins.attrib import attr

_multiprocess_shared_ = False


class TestHostPing(cloudstackTestCase):

    def setUp(self, handler=logging.StreamHandler()):
        self.logger = logging.getLogger('TestHM')
        self.stream_handler = handler
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(self.stream_handler)
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__
        self.dbConnection = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.pod = get_pod(self.apiclient, self.zone.id)
        self.original_host_state_map = {}
        self.cleanup = []

    def tearDown(self):
        for host_id in self.original_host_state_map:
            state = self.original_host_state_map[host_id]
            sql_query = "UPDATE host SET status = '" + state + "' WHERE uuid = '" + host_id + "'"
            self.dbConnection.execute(sql_query)
        super(TestHostPing, self).tearDown()

    def checkHostStateInCloudstack(self, state, host_id):
        try:
            listHost = Host.list(
                self.apiclient,
                type='Routing',
                zoneid=self.zone.id,
                podid=self.pod.id,
                id=host_id
            )
            self.assertEqual(
                isinstance(listHost, list),
                True,
                "Check if listHost returns a valid response"
            )

            self.assertEqual(
                len(listHost),
                1,
                "Check if listHost returns a host"
            )
            self.logger.debug(" Host state is %s " % listHost[0].state)
            if listHost[0].state == state:
                return True, 1
            else:
                return False, 1
        except Exception as e:
            self.logger.debug("Got exception %s" % e)
            return False, 1

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic"],
        required_hardware="true")
    def test_01_host_ping_on_alert(self):
        listHost = Host.list(
            self.apiclient,
            type='Routing',
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        for host in listHost:
            self.logger.debug('Hypervisor = {}'.format(host.id))

        hostToTest = listHost[0]
        self.original_host_state_map[hostToTest.id] = hostToTest.state
        sql_query = "UPDATE host SET status = 'Alert' WHERE uuid = '" + hostToTest.id + "'"
        self.dbConnection.execute(sql_query)

        hostUpInCloudstack = wait_until(30, 8, self.checkHostStateInCloudstack, "Up", hostToTest.id)

        if not (hostUpInCloudstack):
            raise self.fail("Host is not up %s, in cloudstack so failing test " % (hostToTest.ipaddress))
        return
