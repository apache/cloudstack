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
#All tests inherit from cloudstackTestCase
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (ServiceOffering,
                             VirtualMachine,
                             Account,
                             UserData,
                             NetworkOffering,
                             Network,
                             Router,
                             EgressFireWallRule,
                             PublicIPAddress,
                             NATRule)
from marvin.lib.common import get_test_template, get_zone, list_virtual_machines
from marvin.lib.utils import (validateList, cleanup_resources)
from nose.plugins.attrib import attr
from marvin.codes import PASS,FAIL


from marvin.lib.common import (get_domain, get_template)

class Services:
    def __init__(self):
        self.services = {
            "virtual_machine": {
                "displayname": "TesVM1",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.5 (64-bit)',
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 256,
            },
            "egress": {
                "name": 'web',
                "protocol": 'TCP',
                "startport": 80,
                "endport": 80,
                "cidrlist": '0.0.0.0/0',
            },
            "natrule": {
                "privateport": 22,
                "publicport": 22,
                "protocol": "TCP"
            },
        }

class TestRegisteredUserdata(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        super(TestRegisteredUserdata, cls)
        cls.api_client = cls.testClient.getApiClient()
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services = Services().services

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.testdata = self.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and Default Built-in template
        self.domain = get_domain(self.apiclient)
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())

        #create a user account
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        self.testdata["mode"] = self.zone.networktype
        self.template = get_template(self.apiclient, self.zone.id, self.testdata["ostype"])

        #create a service offering
        small_service_offering = self.testdata["service_offerings"]["small"]
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            small_service_offering
        )

        self.no_isolate = NetworkOffering.create(
            self.apiclient,
            self.testdata["isolated_network_offering"]
        )
        self.no_isolate.update(self.apiclient, state='Enabled')
        self.isolated_network = Network.create(
            self.apiclient,
            self.testdata["network"],
            networkofferingid=self.no_isolate.id,
            zoneid=self.zone.id,
            accountid="admin",
            domainid=1
        )

        #build cleanup list
        self.cleanup = [
            self.service_offering,
            self.isolated_network,
            self.no_isolate,
            self.account,
        ]


    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_CRUD_operations_userdata(self):

        self.userdata1 = UserData.register(
            self.apiclient,
            name="UserdataName",
            userdata="VGVzdFVzZXJEYXRh", #TestUserData
            account=self.account.name,
            domainid=self.account.domainid
        )

        list_userdata = UserData.list(self.apiclient, id=self.userdata1.userdata.id, listall=True)

        self.assertNotEqual(
            len(list_userdata),
            0,
            "List userdata was empty"
        )

        userdata = list_userdata[0]
        self.assertEqual(
            userdata.id,
            self.userdata1.userdata.id,
            "userdata ids do not match"
        )

        UserData.delete(
            self.apiclient,
            id=self.userdata1.userdata.id
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=True)
    def test_deploy_vm_with_registerd_userdata(self):

        self.userdata2 = UserData.register(
            self.apiclient,
            name="testUserData2",
            userdata="VGVzdFVzZXJEYXRh", #TestUserData
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            zoneid=self.zone.id,
            accountid="admin",
            domainid=1,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[self.isolated_network.id],
            userdataid=self.userdata2.userdata.id
        )
        self.cleanup.append(self.virtual_machine)
        self.cleanup.append(self.userdata2)

        networkid = self.virtual_machine.nic[0].networkid
        src_nat_list = PublicIPAddress.list(
            self.apiclient,
            associatednetworkid=networkid,
            account="admin",
            domainid=1,
            listall=True,
            issourcenat=True,
        )
        src_nat = src_nat_list[0]

        NATRule.create(
            self.apiclient,
            self.virtual_machine,
            self.services["natrule"],
            src_nat.id
        )

        # create egress rule to allow wget of my cloud-set-guest-password script
        if self.zone.networktype.lower() == 'advanced':
            EgressFireWallRule.create(self.api_client,
                                      networkid=networkid,
                                      protocol=self.testdata["egress_80"]["protocol"],
                                      startport=self.testdata["egress_80"]["startport"],
                                      endport=self.testdata["egress_80"]["endport"],
                                      cidrlist=self.testdata["egress_80"]["cidrlist"])

        list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)

        self.assertEqual(
            isinstance(list_vms, list),
            True,
            "List VM response was not a valid list"
        )
        self.assertNotEqual(
            len(list_vms),
            0,
            "List VM response was empty"
        )

        vm = list_vms[0]
        self.assertEqual(
            vm.id,
            self.virtual_machine.id,
            "Virtual Machine ids do not match"
        )
        self.assertEqual(
            vm.state,
            "Running",
            msg="VM is not in Running state"
        )
        self.assertEqual(
            vm.userdataid,
            self.userdata2.userdata.id,
            "Virtual Machine names do not match"
        )

        # Verify the retrieved ip address in listNICs API response
        self.list_nics(vm.id)
        vr_res = Router.list(
            self.apiclient,
            networkid=self.isolated_network.id,
            listAll=True
        )
        self.assertEqual(validateList(vr_res)[0], PASS, "List Routers returned invalid response")
        vr_ip = vr_res[0].guestipaddress
        ssh = self.virtual_machine.get_ssh_client(ipaddress=src_nat.ipaddress)
        cmd = "curl http://%s/latest/user-data" % vr_ip
        res = ssh.execute(cmd)
        self.debug("Verifying userdata in the VR")
        self.assertEqual(
            str(res[0]),
            "TestUserData",
            "Failed to match userdata"
        )

    def list_nics(self, vm_id):
        list_vm_res = VirtualMachine.list(self.apiclient, id=vm_id)
        self.assertEqual(validateList(list_vm_res)[0], PASS, "List vms returned invalid response")
        nics = list_vm_res[0].nic
        for nic in nics:
            if nic.type == "Shared":
                nic_res = NIC.list(
                    self.apiclient,
                    virtualmachineid=vm_id,
                    nicid=nic.id
                )
                nic_ip = nic_res[0].ipaddress
                self.assertIsNotNone(nic_ip, "listNics API response does not have the ip address")
            else:
                continue
        return
