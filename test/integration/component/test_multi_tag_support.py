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
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import *
from marvin.lib.common import get_zone, get_domain, get_template
from marvin.lib.utils import cleanup_resources


class Services:
    """Test multi tag support in compute offerings
    """

    def __init__(self):
        self.services = {
            "account": {
                "email": "john@doe.de",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "service_offering_0": {
                "name": "NoTag",
                "displaytext": "NoTag",
                "cpunumber": 1,
                "cpuspeed": 100,
                # in MHz
                "memory": 128,
                # In MBs
            },
            "service_offering_1": {
                "name": "OneTag",
                "displaytext": "OneTag",
                "cpunumber": 1,
                "cpuspeed": 100,
                "hosttags": "tag1",
                # in MHz
                "memory": 128,
                # In MBs
            },
            "service_offering_2": {
                "name": "TwoTag",
                "displaytext": "TwoTag",
                "cpunumber": 1,
                "cpuspeed": 100,
                "hosttags": "tag2,tag1",
                # in MHz
                "memory": 128,
                # In MBs
            },
            "service_offering_not_existing_tag": {
                "name": "NotExistingTag",
                "displaytext": "NotExistingTag",
                "cpunumber": 1,
                "cpuspeed": 100,
                "hosttags": "tagX",
                # in MHz
                "memory": 128,
                # In MBs
            },
            "virtual_machine": {
                "name": "TestVM",
                "displayname": "TestVM"
            },
            "template": {
                "displaytext": "Ubuntu",
                "name": "Ubuntu16 x64",
                "ostype": 'Ubuntu 16.04 (64-bit)',
                "templatefilter": 'self',
            },
            "network": {
                "name": "Guest",
            },
            "host": {
                "name": ""
            }
        }


class TestMultiTagSupport(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestMultiTagSupport, cls).getClsTestClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = Services().services
        cls.zone = get_zone(cls.api_client)
        cls.domain = get_domain(cls.api_client)
        cls.network = cls.get_network()
        cls.host = cls.get_host()
        cls.host_tags = cls.host["hosttags"]

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["template"]["ostype"]
        )

        cls.service_offering_list = []
        for x in range(0, 3):
            cls.service_offering_list.append(ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering_" + str(x)]
            ))

        cls.service_offering_ne_tag = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering_not_existing_tag"]
        )

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
        )

        cls._cleanup = [
                           cls.account,
                           cls.service_offering_ne_tag,
                       ] + cls.service_offering_list

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            print("Cleanup resources used")
            Host.update(cls.api_client, id=cls.host["id"], hosttags=cls.host_tags)
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def test_multi_tags(self):
        for x in range(0, len(self.service_offering_list)):

            if hasattr(self.service_offering_list[x], 'hosttags'):
                Host.update(self.api_client, id=self.host["id"], hosttags=self.service_offering_list[x].hosttags)

            vm = VirtualMachine.create(
                self.api_client,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering_list[x].id,
                templateid=self.template.id,
                networkids=self.network.id
            )

            self.assertEqual(
                isinstance(vm, VirtualMachine),
                True,
                "VM %s should be created with service offering %s " %
                (self.services["virtual_machine"]["name"], self.service_offering_list[x].name))

            vm.delete(self.api_client, True)

    def test_no_existing_tag(self):

        with self.assertRaises(Exception):
            VirtualMachine.create(
                self.api_client,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                zoneid=self.zone.id,
                serviceofferingid=self.service_offering_ne_tag.id,
                templateid=self.template.id,
                networkids=self.network.id
            )
            return

    @classmethod
    def get_network(cls):
        networks = Network.list(cls.api_client, zoneid=cls.zone.id)
        if len(networks) == 1:
            return networks[0]

        if len(networks) > 1:
            for network in networks:
                if network.name == cls.services["network"]["name"]:
                    return network
            raise ValueError("No suitable network found, check network name in service object")

        raise ValueError("No network found for zone with id %s" % cls.zone.id)

    @classmethod
    def get_host(cls):
        hosts = Host.list(cls.api_client, zoneid=cls.zone.id)

        if len(hosts) == 1:
            return hosts[0]

        if len(hosts) > 1:
            for host in hosts:
                if host.name == cls.services["host"]["name"]:
                    return host
            raise ValueError("No suitable host found, check host name in service object")

        raise ValueError("No host found for zone with id %s" % cls.zone.id)
