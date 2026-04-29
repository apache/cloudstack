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

#Test from the Marvin - Testing in Python wiki

import time

#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase

#Import Integration Libraries

#base - contains all resources as entities and defines create, delete, list operations on them
from marvin.lib.base import Account, VirtualMachine, Cluster, Host, ServiceOffering, Configurations, SimulatorMock

#utils - utility classes for common cleanup, external library wrappers etc
from marvin.lib.utils import cleanup_resources, validateList

#common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_template
from marvin.codes import PASS

from nose.plugins.attrib import attr

class TestDeployVMHA(cloudstackTestCase):
    """Test VM HA
    """

    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())

        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])

        self.hosts = []
        suitablecluster = None
        clusters = Cluster.list(self.apiclient)
        self.assertTrue(isinstance(clusters, list) and len(clusters) > 0, msg = "No clusters found")
        for cluster in clusters:
            self.hosts = Host.list(self.apiclient, clusterid=cluster.id, type='Routing')
            if isinstance(self.hosts, list) and len(self.hosts) >= 2:
                suitablecluster = cluster
                break
        self.assertEqual(validateList(self.hosts)[0], PASS, "hosts list validation failed")
        if len(self.hosts) < 2:
            self.skipTest("Atleast 2 hosts required in cluster for VM HA test")
        #update host tags
        for host in self.hosts:
            Host.update(self.apiclient, id=host.id, hosttags=self.testdata["service_offerings"]["hasmall"]["hosttags"])

        #create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        #create a service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offerings"]["hasmall"]
        )
        #deploy ha vm
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id
        )
        list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)
        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"\
            % self.virtual_machine.id
        )
        self.assertTrue(isinstance(list_vms, list) and len(list_vms) == 1, msg = "List VM response was empty")
        self.virtual_machine = list_vms[0]

        self.mock_checkhealth = SimulatorMock.create(
            apiclient=self.apiclient,
            command="CheckHealthCommand",
            zoneid=suitablecluster.zoneid,
            podid=suitablecluster.podid,
            clusterid=suitablecluster.id,
            hostid=self.virtual_machine.hostid,
            value="result:fail")
        self.mock_ping = SimulatorMock.create(
            apiclient=self.apiclient,
            command="PingCommand",
            zoneid=suitablecluster.zoneid,
            podid=suitablecluster.podid,
            clusterid=suitablecluster.id,
            hostid=self.virtual_machine.hostid,
            value="result:fail")
        self.mock_checkvirtualmachine = SimulatorMock.create(
            apiclient=self.apiclient,
            command="CheckVirtualMachineCommand",
            zoneid=suitablecluster.zoneid,
            podid=suitablecluster.podid,
            clusterid=suitablecluster.id,
            hostid=self.virtual_machine.hostid,
            value="result:fail")
        self.mock_pingtest = SimulatorMock.create(
            apiclient=self.apiclient,
            command="PingTestCommand",
            zoneid=suitablecluster.zoneid,
            podid=suitablecluster.podid,
            value="result:fail")
        self.mock_checkonhost_list = []
        for host in self.hosts:
            if host.id != self.virtual_machine.hostid:
                self.mock_checkonhost_list.append(SimulatorMock.create(
                    apiclient=self.apiclient,
                    command="CheckOnHostCommand",
                    zoneid=suitablecluster.zoneid,
                    podid=suitablecluster.podid,
                    clusterid=suitablecluster.id,
                    hostid=host.id,
                    value="result:fail"))
        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account,
            self.mock_checkhealth,
            self.mock_ping,
            self.mock_checkvirtualmachine,
            self.mock_pingtest
        ]
        self.cleanup = self.cleanup + self.mock_checkonhost_list

    @attr(tags = ['advanced'], required_hardware="simulator only")
    def test_vm_ha(self):
        """Test VM HA

        # Validate the following:
        # VM started on other host in cluster
        """

        #wait for VM to HA
        ping_timeout = Configurations.list(self.apiclient, name="ping.timeout")
        ping_interval = Configurations.list(self.apiclient, name="ping.interval")
        total_duration = int(float(ping_timeout[0].value) * float(ping_interval[0].value))
        time.sleep(total_duration)

        duration = 0
        vm = None
        while duration < total_duration:
            list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)
            self.assertTrue(isinstance(list_vms, list) and len(list_vms) == 1, msg = "List VM response was empty")
            vm = list_vms[0]
            if vm.hostid != self.virtual_machine.hostid and vm.state == "Running":
                break
            else:
                time.sleep(10)
                duration = duration + 10

        self.assertEqual(
            vm.id,
            self.virtual_machine.id,
            "VM ids do not match")
        self.assertEqual(
            vm.name,
            self.virtual_machine.name,
            "VM names do not match")
        self.assertEqual(
            vm.state,
            "Running",
            msg="VM is not in Running state")
        self.assertNotEqual(
            vm.hostid,
            self.virtual_machine.hostid,
            msg="VM is not started on another host as part of HA")

    def tearDown(self):
        try:
            for host in self.hosts:
                Host.update(self.apiclient, id=host.id, hosttags="")

            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
