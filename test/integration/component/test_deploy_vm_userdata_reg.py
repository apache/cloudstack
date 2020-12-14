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


# this script will cover VMdeployment  with Userdata tests

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import *
from marvin.lib.utils import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
from marvin.sshClient import SshClient
import unittest
import random
import string

_multiprocess_shared_ = True
import os

class Services:
    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                "password": "password",
                },
            "virtual_machine": {
                "displayname": "TesVM1",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
                },
            "ostype": 'CentOS 5.3 (64-bit)',
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 256,
                },
            }





class TestDeployVmWithUserData(cloudstackTestCase):
    """Tests for UserData
    """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDeployVmWithUserData, cls).getClsTestClient()
        cls.apiClient = cls.testClient.getApiClient()
        cls.services = Services().services
        cls.zone = get_zone(cls.apiClient, cls.testClient.getZoneForTests())
        if cls.zone.localstorageenabled:
            #For devcloud since localstroage is enabled
            cls.services["service_offering"]["storagetype"] = "local"
        cls.service_offering = ServiceOffering.create(
            cls.apiClient,
            cls.services["service_offering"]
        )
        cls.account = Account.create(cls.apiClient, services=cls.services["account"])
        cls.cleanup = [cls.account]
        cls.template = get_template(
            cls.apiClient,
            cls.zone.id,
            cls.services["ostype"]
        )
        # Generate userdata of 2500 bytes. This is larger than the 2048 bytes limit.
        # CS however allows for upto 4K bytes in the code. So this must succeed.
        # Overall, the query length must not exceed 4K, for then the json decoder
        # will fail this operation at the marvin client side itcls.


        cls.userdata = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(2500))
        cls.user_data_2k= ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(2000))
        cls.user_data_2kl = ''.join(random.choice(string.ascii_uppercase + string.digits) for x in range(1900))


    def setUp(self):
        self.apiClient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()


    @attr(tags=["simulator", "devcloud", "basic", "advanced"], required_hardware="true")
    def test_deployvm_userdata_post(self):
        """Test userdata as POST, size > 2k
        """


        self.userdata=base64.b64encode(self.userdata)
        self.services["virtual_machine"]["userdata"] = self.userdata

        deployVmResponse = VirtualMachine.create(
            self.apiClient,
            services=self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            zoneid=self.zone.id,
            method="POST"

        )

        vms = list_virtual_machines(
            self.apiClient,
            account=self.account.name,
            domainid=self.account.domainid,
            id=deployVmResponse.id
        )
        self.assertTrue(len(vms) > 0, "There are no Vms deployed in the account %s" % self.account.name)
        vm = vms[0]
        self.assertTrue(vm.id == str(deployVmResponse.id), "Vm deployed is different from the test")
        self.assertTrue(vm.state == "Running", "VM is not in Running state")
        ip_addr=deployVmResponse.ipaddress
        if self.zone.networktype == "Basic":
            list_router_response = list_routers(
                self.apiClient,
                listall="true"
            )
        else:
            list_router_response = list_routers(
                self.apiClient,
                account=self.account.name,
                domainid=self.account.domainid
            )
        self.assertEqual(
            isinstance(list_router_response, list),
            True,
            "Check list response returns a valid list"
        )
        router = list_router_response[0]

        hosts = list_hosts(
            self.apiClient,
            zoneid=router.zoneid,
            type='Routing',
            state='Up',
            id=router.hostid
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list host returns a valid list"
        )
        host = hosts[0]
        self.debug("Router ID: %s, state: %s" % (router.id, router.state))

        self.assertEqual(
            router.state,
            'Running',
            "Check list router response for router state"
        )
        cmd="cat /var/www/html/userdata/"+deployVmResponse.ipaddress+"/user-data"

        if self.hypervisor.lower() in ('vmware', 'hyperv'):

            try:
                result = get_process_status(
                    self.apiClient.connection.mgtSvr,
                    22,
                    self.apiClient.connection.user,
                    self.apiClient.connection.passwd,
                    router.linklocalip,
                    cmd,
                    hypervisor=self.hypervisor
                )
                res = str(result)
                self.assertEqual(res.__contains__(self.userdata),True,"Userdata Not applied Check the failures")


            except KeyError:
                self.skipTest("Marvin configuration has no host credentials to check USERDATA")

        else:
            try:
                host.user, host.passwd = get_host_credentials(self.config, host.ipaddress)
                result = get_process_status(
                    host.ipaddress,
                    22,
                    host.user,
                    host.passwd,
                    router.linklocalip,
                    cmd
                )
                res = str(result)
                self.assertEqual(res.__contains__(self.userdata),True,"Userdata Not applied Check the failures")
            except KeyError:
                self.skipTest("Marvin configuration has no host credentials to check router user data")



    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.apiClient, cls.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
