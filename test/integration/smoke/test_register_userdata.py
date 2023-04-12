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
                             NATRule,
                             Template)
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
        """Test register, list, update operations on userdata
            1. Register a userdata
            2. List the registered userdata
            3. Delete the registered userdata
        """

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
    def test_deploy_vm_with_registered_userdata(self):
        """Test deploy VM with registered userdata
            1. Register a userdata
            2. Deploy a VM by passing the userdata id
            3. Test the VM response
            4. SSH into VM and access the userdata
        """

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

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=True)
    def test_deploy_vm_with_registered_userdata_with_params(self):
        """Test deploy VM with registered userdata with variables
            1. Register a userdata having variables
            2. Deploy a VM by passing the userdata id and custom userdata params map with values
            3. Test the VM response
            4. SSH into VM and access the userdata, check if values got rendered for the decalared variables in userdata
        """
        self.userdata2 = UserData.register(
            self.apiclient,
            name="testUserData2",
            userdata="IyMgdGVtcGxhdGU6IGppbmphCiNjbG91ZC1jb25maWcKcnVuY21kOgogICAgLSBlY2hvICdrZXkge3sgZHMubWV0YV9kYXRhLmtleTEgfX0nID4+IC9yb290L2luc3RhbmNlX21ldGFkYXRh",
            #    ## template: jinja
            #    #cloud-config
            #    runcmd:
            #        - echo 'key {{ ds.meta_data.key1 }}' >> /root/instance_metadata

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
            userdataid=self.userdata2.userdata.id,
            userdatadetails=[{"key1": "value1"}]
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
            "Userdata ids do not match"
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
        cmd = "curl http://%s/latest/meta-data/key1" % vr_ip
        res = ssh.execute(cmd)
        self.debug("Verifying userdata in the VR")
        self.assertEqual(
            str(res[0]),
            "value1",
            "Failed to match userdata"
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=False)
    def test_link_and_unlink_userdata_to_template(self):
        """Test link and unlink of userdata to a template
            1. Register a userdata
            2. Link the registered userdata to a template
            3. Verify the template response and check userdata details in it
            4. Unlink the registered userdata from template
            5. Verify the template response
        """

        self.userdata3 = UserData.register(
            self.apiclient,
            name="testUserData2",
            userdata="VGVzdFVzZXJEYXRh", #TestUserData
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id,
            userdataid=self.userdata3.userdata.id
        )

        self.assertEqual(
            self.userdata3.userdata.id,
            self.template.userdataid,
            "Match userdata id in template response"
        )

        self.assertEqual(
            self.template.userdatapolicy,
            "ALLOWOVERRIDE",
            "Match default userdata override policy in template response"
        )

        self.debug("Verifying unlinking of userdata from template " + self.template.id)

        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id
        )

        self.assertEqual(
            self.template.userdataid,
            None,
            "Check userdata id in template response is None"
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=True)
    def test_deploy_vm_with_registered_userdata_with_override_policy_allow(self):
        """Test deploy VM with registered userdata with variables
            1. Register two userdata, one to link to template and another to pass while deploying VM
            2. Link a userdata to template, default override policy is allow override
            3. Deploy a VM with that template and also by passing another userdata id
            4. Since the override policy is allow override, userdata id passed during VM deployment will be consider.
                Verify the same by SSH into VM.
        """

        self.apiUserdata = UserData.register(
            self.apiclient,
            name="ApiUserdata",
            userdata="QVBJdXNlcmRhdGE=", #APIuserdata
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.templateUserdata = UserData.register(
            self.apiclient,
            name="TemplateUserdata",
            userdata="VGVtcGxhdGVVc2VyRGF0YQ==", #TemplateUserData
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id,
            userdataid=self.templateUserdata.userdata.id,
            userdatapolicy="allowoverride"
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
            userdataid=self.apiUserdata.userdata.id
        )
        self.cleanup.append(self.virtual_machine)
        self.cleanup.append(self.apiUserdata)

        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id
        )

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
            self.apiUserdata.userdata.id,
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
            "APIuserdata",
            "Failed to match userdata"
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg'], required_hardware=True)
    def test_deploy_vm_with_registered_userdata_with_override_policy_append(self):
        """Test deploy VM with registered userdata with variables
            1. Register two userdata, one to link to template and another to pass while deploying VM
            2. Link a userdata to template with override policy is append
            3. Deploy a VM with that template and also by passing another userdata id
            4. Since the override policy is append, userdata passed during VM deployment will be appended to template's
            userdata and configured to VM. Verify the same by SSH into VM.
        """

        self.apiUserdata = UserData.register(
            self.apiclient,
            name="ApiUserdata",
            userdata="QVBJdXNlcmRhdGE=", #APIuserdata
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.templateUserdata = UserData.register(
            self.apiclient,
            name="TemplateUserdata",
            userdata="VGVtcGxhdGVVc2VyRGF0YQ==", #TemplateUserData
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id,
            userdataid=self.templateUserdata.userdata.id,
            userdatapolicy="append"
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
            userdataid=self.apiUserdata.userdata.id
        )
        self.cleanup.append(self.virtual_machine)
        self.cleanup.append(self.apiUserdata)
        self.cleanup.append(self.templateUserdata)

        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id
        )

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
            "TemplateUserDataAPIuserdata",
            "Failed to match userdata"
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg', 'testnow'], required_hardware=True)
    def test_deploy_vm_with_registered_userdata_with_override_policy_deny(self):
        """Test deploy VM with registered userdata with variables
            1. Register two userdata, one to link to template and another to pass while deploying VM
            2. Link a userdata to template with override policy is deny override
            3. Deploy a VM with that template and also by passing another userdata id
            4. Since the override policy is deny override, userdata passed during VM deployment will not be accepted.
            So expect an exception.
        """

        self.apiUserdata = UserData.register(
            self.apiclient,
            name="ApiUserdata",
            userdata="QVBJdXNlcmRhdGE=", #APIuserdata
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.templateUserdata = UserData.register(
            self.apiclient,
            name="TemplateUserdata",
            userdata="VGVtcGxhdGVVc2VyRGF0YQ==", #TemplateUserData
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id,
            userdataid=self.templateUserdata.userdata.id,
            userdatapolicy="denyoverride"
        )

        with self.assertRaises(Exception) as e:
            self.virtual_machine = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                zoneid=self.zone.id,
                accountid="admin",
                domainid=1,
                serviceofferingid=self.service_offering.id,
                templateid=self.template.id,
                networkids=[self.isolated_network.id],
                userdataid=self.apiUserdata.userdata.id
            )
            self.cleanup.append(self.virtual_machine)
            self.debug("Deploy VM with userdata passed during deployment failed as expected because template userdata override policy is deny. Exception here is : %s" %
                       e.exception)

        self.cleanup.append(self.apiUserdata)
        self.cleanup.append(self.templateUserdata)

        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id
        )

    @attr(tags=['advanced', 'simulator', 'basic', 'sg', 'testnow'], required_hardware=True)
    def test_user_userdata_crud(self):
        """Test following operations as a normal user:
            1. Register userdata
            2. List userdata
            3. Link userdata to a template, unlink
            4. Delete userdata.
        """
        self.user = self.account.user[0]
        self.userapiclient = self.testClient.getUserApiClient(
            self.user.username,
            self.domain.name)

        self.userdata = UserData.register(
            self.userapiclient,
            name="UserdataName",
            userdata="VGVzdFVzZXJEYXRh",
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(self.userdata)

        list_userdata = UserData.list(self.apiclient, id=self.userdata.userdata.id, listall=True)
        self.assertNotEqual(
            len(list_userdata),
            0,
            "List userdata was empty"
        )
        userdata = list_userdata[0]
        self.assertEqual(
            userdata.id,
            self.userdata.userdata.id,
            "userdata ids do not match"
        )

        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id,
            userdataid=self.userdata.userdata.id
        )
        self.assertEqual(
            self.userdata.userdata.id,
            self.template.userdataid,
            "Match userdata id in template response"
        )
        self.assertEqual(
            self.template.userdatapolicy,
            "ALLOWOVERRIDE",
            "Match default userdata override policy in template response"
        )
        self.template = Template.linkUserDataToTemplate(
            self.apiclient,
            templateid=self.template.id
        )
        self.assertEqual(
            self.template.userdataid,
            None,
            "Check userdata id in template response is None"
        )

        UserData.delete(
            self.userapiclient,
            id=self.userdata.userdata.id
        )
        self.cleanup.remove(self.userdata)
