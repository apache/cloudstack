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
from marvin.lib.utils import cleanup_resources

#common - commonly used methods for all tests are listed here
from marvin.lib.common import get_zone, get_domain, get_template

from nose.plugins.attrib import attr

class TestDeployVMSync(cloudstackTestCase):
    """Test VM Sync
    """

    def setUp(self):
        self.testdata = self.testClient.getParsedTestDataConfig()
        self.apiclient = self.testClient.getApiClient()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())

        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])

        hosts = Host.list(self.apiclient, type='Routing')
        self.assertTrue(isinstance(hosts, list) and len(hosts) > 0, msg = "No hosts found")
        self.host = hosts[0]
        #update host tags
        Host.update(self.apiclient, id=self.host.id, hosttags=self.testdata["service_offerings"]["taggedsmall"]["hosttags"])

        #create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        #create a service offering
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offerings"]["taggedsmall"]
        )
        #deploy vms
        self.vm1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id
        )
        self.vm2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id
        )
        self.vm3 = VirtualMachine.create(
            self.apiclient,
            self.testdata["small"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id
        )
        list_vms = VirtualMachine.list(self.apiclient, ids=[self.vm1.id, self.vm2.id, self.vm3.id], listAll=True)
        self.assertTrue(isinstance(list_vms, list) and len(list_vms) == 3, msg = "List VM response is empty")
        clusters = Cluster.list(self.apiclient, id=self.host.clusterid)
        self.assertTrue(isinstance(clusters, list) and len(clusters) > 0, msg = "Cluster not found")

        json_response = '{"com.cloud.agent.api.PingRoutingWithNwGroupsCommand":{"newGroupStates":{},"newStates":{},"_hostVmStateReport":{"%s":{"state":"PowerOn","host":"%s"},"%s":{"state":"PowerOff","host":"%s"}},"_gatewayAccessible":true,"_vnetAccessible":true,"hostType":"Routing","hostId":0,"contextMap":{},"wait":0}}'
        json_response = json_response%(self.vm1.instancename, self.host.name, self.vm2.instancename, self.host.name)

        #create a mock to simulate vm1 as power-on, vm2 as power-off and vm3 as missing
        self.mock_ping = SimulatorMock.create(
            apiclient=self.apiclient,
            command="PingRoutingWithNwGroupsCommand",
            zoneid=self.zone.id,
            podid=clusters[0].podid,
            clusterid=clusters[0].id,
            hostid=self.host.id,
            value='',
            jsonresponse=json_response,
            method='POST')

        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.account,
            self.mock_ping
        ]

    @attr(tags = ['advanced'], required_hardware="simulator only")
    def test_vm_sync(self):
        """Test VM Sync

        # Validate the following:
        # vm1 should be running, vm2 should be stopped as power report says PowerOff, vm3 should be stopped as missing from power report
        """

        #wait for vmsync to happen
        ping_interval = Configurations.list(self.apiclient, name="ping.interval")
        total_duration = int(float(ping_interval[0].value) * 3.2)
        time.sleep(total_duration)

        list_vms = VirtualMachine.list(self.apiclient, ids=[self.vm1.id, self.vm2.id, self.vm3.id], listAll=True)
        self.assertTrue(isinstance(list_vms, list) and len(list_vms) == 3, msg = "List VM response is empty")
        for vm in list_vms:
            if vm.id == self.vm1.id:
                self.assertTrue(vm.state == "Running", msg = "VM {0} is expected to be in running state".format(vm.name))
            elif vm.id == self.vm2.id or vm.id == self.vm3.id:
                self.assertTrue(vm.state == "Stopped", msg = "VM {0} is expected to be in stopped state".format(vm.name))

    def tearDown(self):
        try:
            Host.update(self.apiclient, id=self.host.id, hosttags="")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)
