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
""" Tests for importVolume and unmanageVolume APIs
"""
# Import Local Modules
from marvin.cloudstackAPI import updateHost
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import Host
from marvin.lib.utils import is_server_ssh_ready, wait_until

# Import System modules
from nose.plugins.attrib import attr

import logging

class TestHostTags(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestHostTags, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.hypervisor = testClient.getHypervisorInfo()
        if cls.testClient.getHypervisorInfo().lower() != "kvm":
            raise unittest.SkipTest("This is only available for KVM")

        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        hosts = Host.list(
            cls.apiclient,
            type = "Routing",
            hypervisor = cls.hypervisor
        )
        if isinstance(hosts, list) and len(hosts) > 0:
            cls.host = hosts[0]
        else:
            raise unittest.SkipTest("No available host for this test")

        cls.logger = logging.getLogger("TestHostTags")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

    @classmethod
    def tearDownClass(cls):
        cls.update_host_tags_via_api(cls.host.hosttags)
        cls.update_implicit_host_tags_via_agent_properties(cls.host.implicithosttags)

    @classmethod
    def update_host_tags_via_api(cls, hosttags):
        cmd = updateHost.updateHostCmd()
        cmd.id = cls.host.id
        cmd.hosttags = hosttags
        cls.apiclient.updateHost(cmd)

    @classmethod
    def update_implicit_host_tags_via_agent_properties(cls, implicithosttags):
        ssh_client = is_server_ssh_ready(
            cls.host.ipaddress,
            22,
            cls.hostConfig["username"],
            cls.hostConfig["password"],
        )
        if implicithosttags:
            command = "sed -i '/host.tags=/d' /etc/cloudstack/agent/agent.properties \
                && echo 'host.tags=%s' >> /etc/cloudstack/agent/agent.properties \
                && systemctl restart cloudstack-agent" % implicithosttags
        else:
            command = "sed -i '/host.tags=/d' /etc/cloudstack/agent/agent.properties \
                && systemctl restart cloudstack-agent"

        ssh_client.execute(command)

    def wait_until_host_is_up_and_verify_hosttags(self, explicithosttags, implicithosttags, interval=3, retries=20):
        def check_host_state():
            hosts = Host.list(
                self.apiclient,
                id=self.host.id
            )
            if isinstance(hosts, list) and len(hosts) > 0:
                host = hosts[0]
                if host.state == "Up":
                    self.logger.debug("Host %s is in Up state" % host.name)
                    self.logger.debug("Host explicithosttags is %s, implicit hosttags is %s" % (host.explicithosttags, host.implicithosttags))
                    if explicithosttags:
                        self.assertEquals(explicithosttags, host.explicithosttags)
                    else:
                        self.assertIsNone(host.explicithosttags)
                    if implicithosttags:
                        self.assertEquals(implicithosttags, host.implicithosttags)
                    else:
                        self.assertIsNone(host.implicithosttags)
                    return True, None
                else:
                    self.logger.debug("Waiting for host %s to be Up state, current state is %s" % (host.name, host.state))
            return False, None

        done, _ = wait_until(interval, retries, check_host_state)
        if not done:
            raise Exception("Failed to wait for host %s to be Up" % self.host.name)
        return True

    @attr(tags=['advanced', 'basic', 'sg'], required_hardware=False)
    def test_01_host_tags(self):
        """Test implicit/explicit host tags
        """

        # update explicit host tags to "s1,s2"
        explicithosttags="s1,s2"
        implicithosttags=self.host.implicithosttags
        self.update_host_tags_via_api(explicithosttags)
        self.wait_until_host_is_up_and_verify_hosttags(explicithosttags, implicithosttags)

        # update implicit host tags to "d1,d2"
        implicithosttags="d1,d2"
        self.update_implicit_host_tags_via_agent_properties(implicithosttags)
        self.wait_until_host_is_up_and_verify_hosttags(explicithosttags, implicithosttags)

        # update explicit host tags to "s3,s4"
        explicithosttags="s3,s4"
        self.update_host_tags_via_api(explicithosttags)
        self.wait_until_host_is_up_and_verify_hosttags(explicithosttags, implicithosttags)

        # update implicit host tags to "d3,d4"
        implicithosttags="d3,d4"
        self.update_implicit_host_tags_via_agent_properties(implicithosttags)
        self.wait_until_host_is_up_and_verify_hosttags(explicithosttags, implicithosttags)

        # update hosttags to ""
        explicithosttags=""
        self.update_host_tags_via_api(explicithosttags)
        self.wait_until_host_is_up_and_verify_hosttags(explicithosttags, implicithosttags)

        # update implicit host tags to ""
        implicithosttags=""
        self.update_implicit_host_tags_via_agent_properties(implicithosttags)
        self.wait_until_host_is_up_and_verify_hosttags(explicithosttags, implicithosttags)

        # update explicit host tags to "s1,s2"
        explicithosttags="s1,s2"
        self.update_host_tags_via_api(explicithosttags)
        self.wait_until_host_is_up_and_verify_hosttags(explicithosttags, implicithosttags)

        # update implicit host tags to "d1,d2"
        implicithosttags="d1,d2"
        self.update_implicit_host_tags_via_agent_properties(implicithosttags)
        self.wait_until_host_is_up_and_verify_hosttags(explicithosttags, implicithosttags)
