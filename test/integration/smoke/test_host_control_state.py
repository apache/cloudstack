#!/usr/bin/env python
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
Tests for host control state
"""

from marvin.cloudstackAPI import (updateHost, updateConfiguration)
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_hosts,
                               list_routers,
                               list_ssvms,
                               list_clusters,
                               list_hosts)
from marvin.lib.base import (Account,
                             Domain,
                             Host,
                             ServiceOffering,
                             VirtualMachine)
from marvin.sshClient import SshClient
from marvin.lib.decoratorGenerators import skipTestIf
from marvin.lib.utils import wait_until
import logging
import time


class TestHostControlState(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestHostControlState, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls._cleanup = []

        cls.domain = Domain.create(
            cls.apiclient,
            cls.services["acl"]["domain1"]
        )
        cls._cleanup.append(cls.domain)
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.vm = VirtualMachine.create(
            cls.apiclient,
            cls.services["virtual_machine"],
            templateid=cls.template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup.append(cls.vm)

    @classmethod
    def tearDownClass(cls):
        super(TestHostControlState, cls).tearDownClass()

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        super(TestHostControlState, self).tearDown()

    def disable_host(self, id):
        cmd = updateHost.updateHostCmd()
        cmd.id = id
        cmd.allocationstate = "Disable"
        response = self.apiclient.updateHost(cmd)
        self.assertEqual(response.resourcestate, "Disabled")

    def enable_host(self, id):
        cmd = updateHost.updateHostCmd()
        cmd.id = id
        cmd.allocationstate = "Enable"
        response = self.apiclient.updateHost(cmd)
        self.assertEqual(response.resourcestate, "Enabled")

    def get_host_ipaddress(self, hostId):
        hosts = list_hosts(
            self.apiclient,
            type='Routing',
            id=hostId
        )
        return hosts[0].ipaddress

    def stop_agent(self, host_ipaddress):
        SshClient(host_ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]).execute\
            ("systemctl stop cloudstack-agent || service cloudstack-agent stop")

    def start_agent(self, host_ipaddress):
        SshClient(host_ipaddress, port=22, user=self.hostConfig["username"], passwd=self.hostConfig["password"]).execute\
            ("systemctl start cloudstack-agent || service cloudstack-agent start")

    def verify_uservm_host_control_state(self, vm_id, state):
        list_vms = VirtualMachine.list(
            self.apiclient,
            id=vm_id
        )
        vm = list_vms[0]
        self.assertEqual(vm.hostcontrolstate,
                         state,
                         msg="host control state should be %s, but it is %s" % (state, vm.hostcontrolstate))

    def verify_ssvm_host_control_state(self, vm_id, state):
        list_ssvm_response = list_ssvms(
            self.apiclient,
            id=vm_id
        )
        vm = list_ssvm_response[0]
        self.assertEqual(vm.hostcontrolstate,
                         state,
                         msg="host control state should be %s, but it is %s" % (state, vm.hostcontrolstate))

    def verify_router_host_control_state(self, vm_id, state):
        list_router_response = list_routers(
            self.apiclient,
            id=vm_id
        )
        vm = list_router_response[0]
        self.assertEqual(vm.hostcontrolstate,
                         state,
                         msg="host control state should be %s, but it is %s" % (state, vm.hostcontrolstate))

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_uservm_host_control_state(self):
        """ Verify host control state for user vm """
        # 1. verify hostcontrolstate = Enabled
        # 2. Disable the host, verify hostcontrolstate = Disabled

        list_vms = VirtualMachine.list(
            self.apiclient,
            id=self.vm.id
        )
        host_id = list_vms[0].hostid

        self.verify_uservm_host_control_state(self.vm.id, "Enabled")

        self.disable_host(host_id)
        self.verify_uservm_host_control_state(self.vm.id, "Disabled")

        if self.hypervisor == "kvm":
            host_ipaddress = self.get_host_ipaddress(host_id)

            self.stop_agent(host_ipaddress)
            time.sleep(5)  # wait for the host to be Disconnected
            self.verify_uservm_host_control_state(self.vm.id, "Offline")

            self.enable_host(host_id)
            self.verify_uservm_host_control_state(self.vm.id, "Offline")

            self.start_agent(host_ipaddress)
            time.sleep(10)  # wait for the host to be Up
            self.verify_uservm_host_control_state(self.vm.id, "Enabled")

        else:
            self.enable_host(host_id)
            self.verify_uservm_host_control_state(self.vm.id, "Enabled")

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_ssvm_host_control_state(self):
        """ Verify host control state for systemvm """
        # 1. verify hostcontrolstate = Enabled
        # 2. Disable the host, verify hostcontrolstate = Disabled

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        ssvm = list_ssvm_response[0]
        host_id = ssvm.hostid

        self.verify_ssvm_host_control_state(ssvm.id, "Enabled")

        self.disable_host(host_id)
        self.verify_ssvm_host_control_state(ssvm.id, "Disabled")

        self.enable_host(host_id)
        self.verify_ssvm_host_control_state(ssvm.id, "Enabled")

    @attr(tags=["basic", "advanced"], required_hardware="false")
    def test_router_host_control_state(self):
        """ Verify host control state for router """
        # 1. verify hostcontrolstate = Enabled
        # 2. Disable the host, verify hostcontrolstate = Disabled

        list_router_response = list_routers(
            self.apiclient,
            state='Running',
            listall=True,
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]
        host_id = router.hostid

        self.verify_router_host_control_state(router.id, "Enabled")

        self.disable_host(host_id)
        self.verify_router_host_control_state(router.id, "Disabled")

        self.enable_host(host_id)
        self.verify_router_host_control_state(router.id, "Enabled")


class TestAutoEnableDisableHost(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestAutoEnableDisableHost, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__
        if cls.hypervisor.lower() not in ['kvm']:
            cls.hypervisorNotSupported = True
            return

        cls.logger = logging.getLogger('TestAutoEnableDisableHost')
        return

    @classmethod
    def tearDownClass(cls):
        super(TestAutoEnableDisableHost, cls).tearDownClass()

    def tearDown(self):
        super(TestAutoEnableDisableHost, self).tearDown()

    def get_ssh_client(self, ip, username, password, retries=10):
        """ Setup ssh client connection and return connection """
        try:
            ssh_client = SshClient(ip, 22, username, password, retries)
        except Exception as e:
            raise unittest.SkipTest("Unable to create ssh connection: " % e)

        self.assertIsNotNone(
            ssh_client, "Failed to setup ssh connection to ip=%s" % ip)

        return ssh_client

    def wait_until_host_is_in_state(self, hostid, resourcestate, interval=3, retries=20):
        def check_resource_state():
            response = Host.list(
                self.apiclient,
                id=hostid
            )
            if isinstance(response, list):
                if response[0].resourcestate == resourcestate:
                    self.logger.debug('Host with id %s is in resource state = %s' % (hostid, resourcestate))
                    return True, None
                else:
                    self.logger.debug("Waiting for host " + hostid +
                                      " to reach state " + resourcestate +
                                      ", with current state " + response[0].resourcestate)
            return False, None

        done, _ = wait_until(interval, retries, check_resource_state)
        if not done:
            raise Exception("Failed to wait for host %s to be on resource state %s" % (hostid, resourcestate))
        return True

    def update_config(self, enable_feature):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = "enable.kvm.host.auto.enable.disable"
        cmd.value = enable_feature

        response = self.apiclient.updateConfiguration(cmd)
        self.debug("updated the parameter %s with value %s" % (response.name, response.value))

    def update_health_check_script(self, ip_address, username, password, exit_code):
        health_check_script_path = "/etc/cloudstack/agent/healthcheck.sh"
        health_check_agent_property = "agent.health.check.script.path"
        agent_properties_file_path = "/etc/cloudstack/agent/agent.properties"

        ssh_client = self.get_ssh_client(ip_address, username, password)
        ssh_client.execute("echo 'exit %s' > %s" % (exit_code, health_check_script_path))
        ssh_client.execute("chmod +x %s" % health_check_script_path)
        ssh_client.execute("echo '%s=%s' >> %s" % (health_check_agent_property, health_check_script_path,
                                                   agent_properties_file_path))
        ssh_client.execute("service cloudstack-agent restart")

    def remove_host_health_check(self, ip_address, username, password):
        health_check_script_path = "/etc/cloudstack/agent/healthcheck.sh"
        ssh_client = self.get_ssh_client(ip_address, username, password)
        ssh_client.execute("rm -f %s" % health_check_script_path)

    def select_host_for_health_checks(self):
        clusters = list_clusters(
            self.apiclient,
            zoneid=self.zone.id
        )
        if not clusters:
            return None

        for cluster in clusters:
            list_hosts_response = list_hosts(
                self.apiclient,
                clusterid=cluster.id,
                type="Routing",
                resourcestate="Enabled"
            )
            assert isinstance(list_hosts_response, list)
            if not list_hosts_response or len(list_hosts_response) < 1:
                continue
            return list_hosts_response[0]
        return None

    def update_host_allocation_state(self, id, enable):
        cmd = updateHost.updateHostCmd()
        cmd.id = id
        cmd.allocationstate = "Enable" if enable else "Disable"
        response = self.apiclient.updateHost(cmd)
        self.assertEqual(response.resourcestate, "Enabled" if enable else "Disabled")

    @attr(tags=["basic", "advanced"], required_hardware="false")
    @skipTestIf("hypervisorNotSupported")
    def test_01_auto_enable_disable_kvm_host(self):
        """Test to auto-enable and auto-disable a KVM host based on health check results

        # Validate the following:
        # 1. Enable the KVM Auto Enable/Disable Feature
        # 2. Set a health check script that fails and observe the host is Disabled
        # 3. Make the health check script succeed and observe the host is Enabled
        """

        selected_host = self.select_host_for_health_checks()
        if not selected_host:
            self.skipTest("Cannot find a KVM host to test the auto-enable-disable feature")

        username = self.hostConfig["username"]
        password = self.hostConfig["password"]

        # Enable the Auto Enable/Disable Configuration
        self.update_config("true")

        # Set health check script for failure
        self.update_health_check_script(selected_host.ipaddress, username, password, 1)
        self.wait_until_host_is_in_state(selected_host.id, "Disabled", 5, 200)

        # Set health check script for success
        self.update_health_check_script(selected_host.ipaddress, username, password, 0)

        self.wait_until_host_is_in_state(selected_host.id, "Enabled", 5, 200)

    @attr(tags=["basic", "advanced"], required_hardware="false")
    @skipTestIf("hypervisorNotSupported")
    def test_02_disable_host_overrides_auto_enable_kvm_host(self):
        """Test to override the auto-enabling of a KVM host by an administrator

        # Validate the following:
        # 1. Enable the KVM Auto Enable/Disable Feature
        # 2. Set a health check script that succeeds and observe the host is Enabled
        # 3. Make the host Disabled
        # 4. Verify the host does not get auto-enabled after the previous step
        """

        selected_host = self.select_host_for_health_checks()
        if not selected_host:
            self.skipTest("Cannot find a KVM host to test the auto-enable-disable feature")

        username = self.hostConfig["username"]
        password = self.hostConfig["password"]

        # Enable the Auto Enable/Disable Configuration
        self.update_config("true")

        # Set health check script for failure
        self.update_health_check_script(selected_host.ipaddress, username, password, 0)
        self.wait_until_host_is_in_state(selected_host.id, "Enabled", 5, 200)

        # Manually disable the host
        self.update_host_allocation_state(selected_host.id, False)

        # Wait for more than the ping interval
        time.sleep(70)

        # Verify the host continues on Disabled state
        self.wait_until_host_is_in_state(selected_host.id, "Disabled", 5, 200)

        # Restore the host to Enabled state
        self.remove_host_health_check(selected_host.ipaddress, username, password)
        self.update_host_allocation_state(selected_host.id, True)

    @attr(tags=["basic", "advanced"], required_hardware="false")
    @skipTestIf("hypervisorNotSupported")
    def test_03_enable_host_does_not_override_auto_disable_kvm_host(self):
        """Test to override the auto-disabling of a KVM host by an administrator

        # Validate the following:
        # 1. Enable the KVM Auto Enable/Disable Feature
        # 2. Set a health check script that fails and observe the host is Disabled
        # 3. Make the host Enabled
        # 4. Verify the host does get auto-disabled after the previous step
        """

        selected_host = self.select_host_for_health_checks()
        if not selected_host:
            self.skipTest("Cannot find a KVM host to test the auto-enable-disable feature")

        username = self.hostConfig["username"]
        password = self.hostConfig["password"]

        # Enable the Auto Enable/Disable Configuration
        self.update_config("true")

        # Set health check script for failure
        self.update_health_check_script(selected_host.ipaddress, username, password, 1)
        self.wait_until_host_is_in_state(selected_host.id, "Disabled", 5, 200)

        # Manually enable the host
        self.update_host_allocation_state(selected_host.id, True)

        # Verify the host goes back to Disabled state
        self.wait_until_host_is_in_state(selected_host.id, "Disabled", 5, 200)

        # Restore the host to Enabled state
        self.remove_host_health_check(selected_host.ipaddress, username, password)
        self.update_host_allocation_state(selected_host.id, True)
