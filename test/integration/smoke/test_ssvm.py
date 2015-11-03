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
""" BVT tests for SSVM
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (stopSystemVm,
                                  rebootSystemVm,
                                  destroySystemVm)
from marvin.lib.utils import (cleanup_resources,
                              get_process_status,
                              get_host_credentials)
from marvin.lib.base import (PhysicalNetwork,
                             NetScaler)
from marvin.lib.common import (get_zone,
                               list_hosts,
                               list_ssvms,
                               list_zones,
                               list_vlan_ipranges)
from nose.plugins.attrib import attr
import telnetlib

# Import System modules
import time
_multiprocess_shared_ = True


class TestSSVMs(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.cleanup = []
        self.services = self.testClient.getParsedTestDataConfig()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())

        # Default sleep is set to 90 seconds, which is too long if the SSVM takes up to 2min to start.
        # Second sleep in the loop will waste test time.
        self.services["sleep"] = 30
        # Default value is 120 seconds. That's just too much.
        self.services["configurableData"]["systemVmDelay"] = 60

        return

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def waitForSystemVMAgent(self, vmname):
        timeout = self.services["timeout"]

        while True:
            list_host_response = list_hosts(
                self.apiclient,
                name=vmname
            )

            if list_host_response and list_host_response[0].state == 'Up':
                break

            if timeout == 0:
                raise Exception("Timed out waiting for SSVM agent to be Up")

            time.sleep(self.services["sleep"])
            timeout = timeout - 1

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="false")
    def test_01_list_sec_storage_vm(self):
        """Test List secondary storage VMs
        """

        # Validate the following:
        # 1. listSystemVM (systemvmtype=secondarystoragevm)
        #    should return only ONE SSVM per zone
        # 2. The returned SSVM should be in Running state
        # 3. listSystemVM for secondarystoragevm should list publicip,
        #    privateip and link-localip
        # 4. The gateway programmed on the ssvm by listSystemVm should be
        #    the same as the gateway returned by listVlanIpRanges
        # 5. DNS entries must match those given for the zone

        list_ssvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='secondarystoragevm',
            state='Running',
        )
        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        # Verify SSVM response
        self.assertNotEqual(
            len(list_ssvm_response),
            0,
            "Check list System VMs response"
        )

        list_zones_response = list_zones(self.apiclient)

        self.assertEqual(
            isinstance(list_zones_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.debug("Number of zones: %s" % len(list_zones_response))
        self.debug("Number of SSVMs: %s" % len(list_ssvm_response))
        # Number of Sec storage VMs = No of Zones
        self.assertEqual(
            len(list_ssvm_response),
            len(list_zones_response),
            "Check number of SSVMs with number of zones"
        )
        # For each secondary storage VM check private IP,
        # public IP, link local IP and DNS
        for ssvm in list_ssvm_response:

            self.debug("SSVM state: %s" % ssvm.state)
            self.assertEqual(
                ssvm.state,
                'Running',
                "Check whether state of SSVM is running"
            )

            self.assertEqual(
                hasattr(ssvm, 'privateip'),
                True,
                "Check whether SSVM has private IP field"
            )

            self.assertEqual(
                hasattr(ssvm, 'linklocalip'),
                True,
                "Check whether SSVM has link local IP field"
            )

            self.assertEqual(
                hasattr(ssvm, 'publicip'),
                True,
                "Check whether SSVM has public IP field"
            )

            # Fetch corresponding ip ranges information from listVlanIpRanges
            ipranges_response = list_vlan_ipranges(
                self.apiclient,
                zoneid=ssvm.zoneid
            )
            self.assertEqual(
                isinstance(ipranges_response, list),
                True,
                "Check list response returns a valid list"
            )
            iprange = ipranges_response[0]

            # Fetch corresponding Physical Network of SSVM's Zone
            listphyntwk = PhysicalNetwork.list(
                self.apiclient,
                zoneid=ssvm.zoneid
            )

            # Execute the following assertion in all zones except EIP-ELB Zones
            if not (
                self.zone.networktype.lower() == 'basic' and isinstance(
                    NetScaler.list(
                        self.apiclient,
                        physicalnetworkid=listphyntwk[0].id),
                    list) is True):
                self.assertEqual(
                    ssvm.gateway,
                    iprange.gateway,
                    "Check gateway with that of corresponding ip range"
                )

            # Fetch corresponding zone information from listZones
            zone_response = list_zones(
                self.apiclient,
                id=ssvm.zoneid
            )
            self.assertEqual(
                isinstance(zone_response, list),
                True,
                "Check list response returns a valid list"
            )
            self.assertEqual(
                ssvm.dns1,
                zone_response[0].dns1,
                "Check DNS1 with that of corresponding zone"
            )

            self.assertEqual(
                ssvm.dns2,
                zone_response[0].dns2,
                "Check DNS2 with that of corresponding zone"
            )
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="false")
    def test_02_list_cpvm_vm(self):
        """Test List console proxy VMs
        """

        # Validate the following:
        # 1. listSystemVM (systemvmtype=consoleproxy) should return
        #    at least ONE CPVM per zone
        # 2. The returned ConsoleProxyVM should be in Running state
        # 3. listSystemVM for console proxy should list publicip, privateip
        #    and link-localip
        # 4. The gateway programmed on the console proxy should be the same
        #    as the gateway returned by listZones
        # 5. DNS entries must match those given for the zone

        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            state='Running',
        )
        self.assertEqual(
            isinstance(list_cpvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        # Verify CPVM response
        self.assertNotEqual(
            len(list_cpvm_response),
            0,
            "Check list System VMs response"
        )
        list_zones_response = list_zones(self.apiclient)
        # Number of Console Proxy VMs = No of Zones

        self.assertEqual(
            isinstance(list_zones_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.debug("Number of zones: %s" % len(list_zones_response))
        self.debug("Number of CPVMs: %s" % len(list_cpvm_response))

        self.assertEqual(
            len(list_cpvm_response),
            len(list_zones_response),
            "Check number of CPVMs with number of zones"
        )
        # For each CPVM check private IP, public IP, link local IP and DNS
        for cpvm in list_cpvm_response:

            self.debug("CPVM state: %s" % cpvm.state)
            self.assertEqual(
                cpvm.state,
                'Running',
                "Check whether state of CPVM is running"
            )

            self.assertEqual(
                hasattr(cpvm, 'privateip'),
                True,
                "Check whether CPVM has private IP field"
            )

            self.assertEqual(
                hasattr(cpvm, 'linklocalip'),
                True,
                "Check whether CPVM has link local IP field"
            )

            self.assertEqual(
                hasattr(cpvm, 'publicip'),
                True,
                "Check whether CPVM has public IP field"
            )
            # Fetch corresponding ip ranges information from listVlanIpRanges
            ipranges_response = list_vlan_ipranges(
                self.apiclient,
                zoneid=cpvm.zoneid
            )
            self.assertEqual(
                isinstance(ipranges_response, list),
                True,
                "Check list response returns a valid list"
            )
            iprange = ipranges_response[0]

            # Fetch corresponding Physical Network of SSVM's Zone
            listphyntwk = PhysicalNetwork.list(
                self.apiclient,
                zoneid=cpvm.zoneid
            )

            # Execute the following assertion in all zones except EIP-ELB Zones
            if not (
                self.zone.networktype.lower() == 'basic' and isinstance(
                    NetScaler.list(
                        self.apiclient,
                        physicalnetworkid=listphyntwk[0].id),
                    list) is True):
                self.assertEqual(
                    cpvm.gateway,
                    iprange.gateway,
                    "Check gateway with that of corresponding ip range"
                )

            # Fetch corresponding zone information from listZones
            zone_response = list_zones(
                self.apiclient,
                id=cpvm.zoneid
            )

            self.assertEqual(
                cpvm.dns1,
                zone_response[0].dns1,
                "Check DNS1 with that of corresponding zone"
            )

            self.assertEqual(
                cpvm.dns2,
                zone_response[0].dns2,
                "Check DNS2 with that of corresponding zone"
            )
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
    def test_03_ssvm_internals(self):
        """Test SSVM Internals"""

        # Validate the following
        # 1. The SSVM check script should not return any
        #    WARN|ERROR|FAIL messages
        # 2. If you are unable to login to the SSVM with the signed key
        #    then test is deemed a failure
        # 3. There should be only one ""cloud"" process running within the SSVM
        # 4. If no process is running/multiple process are running
        #    then the test is a failure

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

        hosts = list_hosts(
            self.apiclient,
            id=ssvm.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )
        host = hosts[0]

        self.debug("Running SSVM check script")

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            # SSH into SSVMs is done via management server for Vmware and
            # Hyper-V
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                ssvm.privateip,
                "/usr/local/cloud/systemvm/ssvm-check.sh |grep -e ERROR -e WARNING -e FAIL",
                hypervisor=self.hypervisor)
        else:
            try:
                host.user, host.passwd = get_host_credentials(
                    self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    ssvm.linklocalip,
                    "/usr/local/cloud/systemvm/ssvm-check.sh |grep -e ERROR -e WARNING -e FAIL")
            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host\
                            credentials to check router services")
        res = str(result)
        self.debug("SSVM script output: %s" % res)

        self.assertEqual(
            res.count("ERROR"),
            1,
            "Check for Errors in tests"
        )

        self.assertEqual(
            res.count("WARNING"),
            1,
            "Check for warnings in tests"
        )

        # Check status of cloud service
        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            # SSH into SSVMs is done via management server for Vmware and
            # Hyper-V
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                ssvm.privateip,
                "service cloud status",
                hypervisor=self.hypervisor
            )
        else:
            try:
                host.user, host.passwd = get_host_credentials(
                    self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    ssvm.linklocalip,
                    "service cloud status"
                )
            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host\
                            credentials to check router services")
        res = str(result)
        self.debug("Cloud Process status: %s" % res)
        # cloud.com service (type=secstorage) is running: process id: 2346
        self.assertEqual(
            res.count("is running"),
            1,
            "Check cloud service is running or not"
        )
        
        linklocal_ip = None
        # Check status of cloud service
        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            # SSH into SSVMs is done via management server for Vmware and
            # Hyper-V
            linklocal_ip = ssvm.privateip
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                ssvm.privateip,
                "cat /var/cache/cloud/cmdline | xargs | sed \"s/ /\\n/g\" | grep eth1ip= | sed \"s/\=/ /g\" | awk '{print $2}'",
                hypervisor=self.hypervisor
            )
        else:
            try:
                linklocal_ip = ssvm.linklocalip
                host.user, host.passwd = get_host_credentials(
                    self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    ssvm.linklocalip,
                    "cat /var/cache/cloud/cmdline | xargs | sed \"s/ /\\n/g\" | grep eth0ip= | sed \"s/\=/ /g\" | awk '{print $2}'"
                )
            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host\
                            credentials to check router services")
        res = result[0]
        self.debug("Cached Link Local IP: %s" % res)
        self.assertEqual(
            linklocal_ip,
            res,
            "The cached Link Local should be the same as the current Link Local IP, but they are different! Current ==> %s; Cached ==> %s " % (linklocal_ip, res)
        )
        
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
    def test_04_cpvm_internals(self):
        """Test CPVM Internals"""

        # Validate the following
        # 1. test that telnet access on 8250 is available to
        #    the management server for the CPVM
        # 2. No telnet access, test FAIL
        # 3. Service cloud status should report cloud agent status to be
        #    running

        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            state='Running',
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_cpvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        cpvm = list_cpvm_response[0]

        hosts = list_hosts(
            self.apiclient,
            id=cpvm.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )
        host = hosts[0]

        try:
            telnetlib.Telnet(
                str(self.apiclient.connection.mgtSvr),
                '8250'
            )
            self.debug("Telnet management server (IP: %s)" %
                       self.apiclient.connection.mgtSvr)
        except Exception as e:
            self.fail(
                "Telnet Access failed for %s: %s" %
                (self.apiclient.connection.mgtSvr, e)
            )

        self.debug("Checking cloud process status")

        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            # SSH into SSVMs is done via management server for Vmware and
            # Hyper-V
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                cpvm.privateip,
                "service cloud status",
                hypervisor=self.hypervisor
            )
        else:
            try:
                host.user, host.passwd = get_host_credentials(
                    self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    cpvm.linklocalip,
                    "service cloud status"
                )
            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host\
                            credentials to check router services")
        res = str(result)
        self.debug("Cloud Process status: %s" % res)
        self.assertEqual(
            res.count("is running"),
            1,
            "Check cloud service is running or not"
        )

        linklocal_ip = None
        # Check status of cloud service
        if self.hypervisor.lower() in ('vmware', 'hyperv'):
            # SSH into SSVMs is done via management server for Vmware and
            # Hyper-V
            linklocal_ip = cpvm.privateip
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                cpvm.privateip,
                "cat /var/cache/cloud/cmdline | xargs | sed \"s/ /\\n/g\" | grep eth1ip= | sed \"s/\=/ /g\" | awk '{print $2}'",
                hypervisor=self.hypervisor
            )
        else:
            try:
                linklocal_ip = cpvm.linklocalip
                host.user, host.passwd = get_host_credentials(
                    self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    cpvm.linklocalip,
                    "cat /var/cache/cloud/cmdline | xargs | sed \"s/ /\\n/g\" | grep eth0ip= | sed \"s/\=/ /g\" | awk '{print $2}'"
                )
            except KeyError:
                self.skipTest(
                    "Marvin configuration has no host\
                            credentials to check router services")
        res = result[0]
        self.debug("Cached Link Local IP: %s" % res)
        self.assertEqual(
            linklocal_ip,
            res,
            "The cached Link Local should be the same as the current Link Local IP, but they are different! Current ==> %s; Cached ==> %s " % (linklocal_ip, res)
        )

        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
    def test_05_stop_ssvm(self):
        """Test stop SSVM
        """

        # Validate the following
        # 1. The SSVM should go to stop state
        # 2. After a brief delay of say one minute, the SSVM should be
        #    restarted once again and return to Running state with previous two
        #    test cases still passing
        # 3. If either of the two above steps fail the test is a failure

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

        hosts = list_hosts(
            self.apiclient,
            id=ssvm.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )

        self.debug("Stopping SSVM: %s" % ssvm.id)
        cmd = stopSystemVm.stopSystemVmCmd()
        cmd.id = ssvm.id
        self.apiclient.stopSystemVm(cmd)

        timeout = self.services["timeout"]
        while True:
            list_ssvm_response = list_ssvms(
                self.apiclient,
                id=ssvm.id
            )
            if isinstance(list_ssvm_response, list):
                if list_ssvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List SSVM call failed!")

            time.sleep(self.services["sleep"])
            timeout = timeout - 1

        self.assertEqual(
            isinstance(list_ssvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        ssvm_response = list_ssvm_response[0]
        self.debug("SSVM state after debug: %s" % ssvm_response.state)
        self.assertEqual(
            ssvm_response.state,
            'Running',
            "Check whether SSVM is running or not"
        )
        # Wait for the agent to be up
        self.waitForSystemVMAgent(ssvm_response.name)

        # Call above tests to ensure SSVM is properly running
        self.test_01_list_sec_storage_vm()

        # Wait for some time before running diagnostic scripts on SSVM
        # as it may take some time to start all service properly
        time.sleep(int(self.services["configurableData"]["systemVmDelay"]))

        self.test_03_ssvm_internals()
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
    def test_06_stop_cpvm(self):
        """Test stop CPVM
        """

        # Validate the following
        # 1. The CPVM should go to stop state
        # 2. After a brief delay of say one minute, the SSVM should be
        #    restarted once again and return to Running state with previous
        #    two test cases still passing
        # 3. If either of the two above steps fail the test is a failure

        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            state='Running',
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_cpvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        cpvm = list_cpvm_response[0]

        hosts = list_hosts(
            self.apiclient,
            id=cpvm.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )

        self.debug("Stopping CPVM: %s" % cpvm.id)
        cmd = stopSystemVm.stopSystemVmCmd()
        cmd.id = cpvm.id
        self.apiclient.stopSystemVm(cmd)

        timeout = self.services["timeout"]
        while True:
            list_cpvm_response = list_ssvms(
                self.apiclient,
                id=cpvm.id
            )
            if isinstance(list_cpvm_response, list):
                if list_cpvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List CPVM call failed!")

            time.sleep(self.services["sleep"])
            timeout = timeout - 1

        cpvm_response = list_cpvm_response[0]

        self.debug("CPVM state after debug: %s" % cpvm_response.state)

        self.assertEqual(
            cpvm_response.state,
            'Running',
            "Check whether CPVM is running or not"
        )

        # Wait for the agent to be up
        self.waitForSystemVMAgent(cpvm_response.name)

        # Call above tests to ensure CPVM is properly running
        self.test_02_list_cpvm_vm()

        # Wait for some time before running diagnostic scripts on SSVM
        # as it may take some time to start all service properly
        time.sleep(int(self.services["configurableData"]["systemVmDelay"]))

        self.test_04_cpvm_internals()
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
    def test_07_reboot_ssvm(self):
        """Test reboot SSVM
        """
        # Validate the following
        # 1. The SSVM should go to stop and return to Running state
        # 2. SSVM's public-ip and private-ip must remain the same
        #    before and after reboot
        # 3. The cloud process should still be running within the SSVM

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

        ssvm_response = list_ssvm_response[0]

        hosts = list_hosts(
            self.apiclient,
            id=ssvm_response.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )

        # Store the public & private IP values before reboot
        old_public_ip = ssvm_response.publicip
        old_private_ip = ssvm_response.privateip

        self.debug("Rebooting SSVM: %s" % ssvm_response.id)
        cmd = rebootSystemVm.rebootSystemVmCmd()
        cmd.id = ssvm_response.id
        self.apiclient.rebootSystemVm(cmd)

        timeout = self.services["timeout"]
        while True:
            list_ssvm_response = list_ssvms(
                self.apiclient,
                id=ssvm_response.id
            )
            if isinstance(list_ssvm_response, list):
                if list_ssvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List SSVM call failed!")

            time.sleep(self.services["sleep"])
            timeout = timeout - 1

        ssvm_response = list_ssvm_response[0]
        self.debug("SSVM State: %s" % ssvm_response.state)
        self.assertEqual(
            'Running',
            str(ssvm_response.state),
            "Check whether CPVM is running or not"
        )

        self.assertEqual(
            ssvm_response.publicip,
            old_public_ip,
            "Check Public IP after reboot with that of before reboot"
        )

        # Private IP Address of System VMs are allowed to change after reboot - CLOUDSTACK-7745

        # Wait for the agent to be up
        self.waitForSystemVMAgent(ssvm_response.name)

        # Wait for some time before running diagnostic scripts on SSVM
        # as it may take some time to start all service properly
        time.sleep(int(self.services["configurableData"]["systemVmDelay"]))

        # Call to verify cloud process is running
        self.test_03_ssvm_internals()
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
    def test_08_reboot_cpvm(self):
        """Test reboot CPVM
        """
        # Validate the following
        # 1. The CPVM should go to stop and return to Running state
        # 2. CPVM's public-ip and private-ip must remain
        #    the same before and after reboot
        # 3. the cloud process should still be running within the CPVM

        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            state='Running',
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_cpvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        cpvm_response = list_cpvm_response[0]

        hosts = list_hosts(
            self.apiclient,
            id=cpvm_response.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )

        # Store the public & private IP values before reboot
        old_public_ip = cpvm_response.publicip
        old_private_ip = cpvm_response.privateip

        self.debug("Rebooting CPVM: %s" % cpvm_response.id)

        cmd = rebootSystemVm.rebootSystemVmCmd()
        cmd.id = cpvm_response.id
        self.apiclient.rebootSystemVm(cmd)

        timeout = self.services["timeout"]
        while True:
            list_cpvm_response = list_ssvms(
                self.apiclient,
                id=cpvm_response.id
            )
            if isinstance(list_cpvm_response, list):
                if list_cpvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List CPVM call failed!")

            time.sleep(self.services["sleep"])
            timeout = timeout - 1

        cpvm_response = list_cpvm_response[0]

        self.debug("CPVM state: %s" % cpvm_response.state)
        self.assertEqual(
            'Running',
            str(cpvm_response.state),
            "Check whether CPVM is running or not"
        )

        self.assertEqual(
            cpvm_response.publicip,
            old_public_ip,
            "Check Public IP after reboot with that of before reboot"
        )

        # Private IP Address of System VMs are allowed to change after reboot - CLOUDSTACK-7745
        
        # Wait for the agent to be up
        self.waitForSystemVMAgent(cpvm_response.name)

        # Wait for some time before running diagnostic scripts on SSVM
        # as it may take some time to start all service properly
        time.sleep(int(self.services["configurableData"]["systemVmDelay"]))

        # Call to verify cloud process is running
        self.test_04_cpvm_internals()
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
    def test_09_destroy_ssvm(self):
        """Test destroy SSVM
        """

        # Validate the following
        # 1. SSVM should be completely destroyed and a new one will spin up
        # 2. listSystemVMs will show a different name for the
        #    systemVM from what it was before
        # 3. new SSVM will have a public/private and link-local-ip
        # 4. cloud process within SSVM must be up and running

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
        ssvm_response = list_ssvm_response[0]

        old_name = ssvm_response.name

        self.debug("Destroying SSVM: %s" % ssvm_response.id)
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = ssvm_response.id
        self.apiclient.destroySystemVm(cmd)

        timeout = self.services["timeout"]
        while True:
            list_ssvm_response = list_ssvms(
                self.apiclient,
                zoneid=self.zone.id,
                systemvmtype='secondarystoragevm'
            )
            if isinstance(list_ssvm_response, list):
                if list_ssvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List SSVM call failed!")

            time.sleep(self.services["sleep"])
            timeout = timeout - 1

        ssvm_response = list_ssvm_response[0]

        # Verify Name, Public IP, Private IP and Link local IP
        # for newly created SSVM
        self.assertNotEqual(
            ssvm_response.name,
            old_name,
            "Check SSVM new name with name of destroyed SSVM"
        )
        self.assertEqual(
            hasattr(ssvm_response, 'privateip'),
            True,
            "Check whether SSVM has private IP field"
        )

        self.assertEqual(
            hasattr(ssvm_response, 'linklocalip'),
            True,
            "Check whether SSVM has link local IP field"
        )

        self.assertEqual(
            hasattr(ssvm_response, 'publicip'),
            True,
            "Check whether SSVM has public IP field"
        )

        # Wait for the agent to be up
        self.waitForSystemVMAgent(ssvm_response.name)

        # Wait for some time before running diagnostic scripts on SSVM
        # as it may take some time to start all service properly
        time.sleep(int(self.services["configurableData"]["systemVmDelay"]))

        # Call to verify cloud process is running
        self.test_03_ssvm_internals()
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "sg"],
        required_hardware="true")
    def test_10_destroy_cpvm(self):
        """Test destroy CPVM
        """

        # Validate the following
        # 1. CPVM should be completely destroyed and a new one will spin up
        # 2. listSystemVMs will show a different name for the systemVM from
        #    what it was before
        # 3. new CPVM will have a public/private and link-local-ip
        # 4. cloud process within CPVM must be up and running

        list_cpvm_response = list_ssvms(
            self.apiclient,
            systemvmtype='consoleproxy',
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(list_cpvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        cpvm_response = list_cpvm_response[0]

        old_name = cpvm_response.name

        self.debug("Destroying CPVM: %s" % cpvm_response.id)
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = cpvm_response.id
        self.apiclient.destroySystemVm(cmd)

        timeout = self.services["timeout"]
        while True:
            list_cpvm_response = list_ssvms(
                self.apiclient,
                systemvmtype='consoleproxy',
                zoneid=self.zone.id
            )
            if isinstance(list_cpvm_response, list):
                if list_cpvm_response[0].state == 'Running':
                    break
            if timeout == 0:
                raise Exception("List CPVM call failed!")

            time.sleep(self.services["sleep"])
            timeout = timeout - 1

        cpvm_response = list_cpvm_response[0]

        # Verify Name, Public IP, Private IP and Link local IP
        # for newly created CPVM
        self.assertNotEqual(
            cpvm_response.name,
            old_name,
            "Check SSVM new name with name of destroyed CPVM"
        )
        self.assertEqual(
            hasattr(cpvm_response, 'privateip'),
            True,
            "Check whether CPVM has private IP field"
        )

        self.assertEqual(
            hasattr(cpvm_response, 'linklocalip'),
            True,
            "Check whether CPVM has link local IP field"
        )

        self.assertEqual(
            hasattr(cpvm_response, 'publicip'),
            True,
            "Check whether CPVM has public IP field"
        )

        # Wait for the agent to be up
        self.waitForSystemVMAgent(cpvm_response.name)

        # Wait for some time before running diagnostic scripts on SSVM
        # as it may take some time to start all service properly
        time.sleep(int(self.services["configurableData"]["systemVmDelay"]))

        # Call to verify cloud process is running
        self.test_04_cpvm_internals()
        return
