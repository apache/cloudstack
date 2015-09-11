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
""" Test for baremetal
"""
#Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.base import (Template,
                             Host,
                             ServiceOffering,
                             Account,
                             VirtualMachine)
from marvin.lib.common import get_domain, get_zone
from marvin.codes import PASS, HOSTTAG1
from nose.plugins.attrib import attr

#Import System modules
_multiprocess_shared_ = True

class TestBaremetal(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(TestBaremetal, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        #Register baremetal template
        cls.template = Template.register(
            cls.apiclient,
            cls.services["baremetal_template"],
            zoneid=cls.zone.id,
            account=cls.account.name,
            domainid=cls.account.domainid,
        )
        cls.debug(
            "Registered a template of format: %s with ID: %s" % (
                cls.services["baremetal_template"]["format"], cls.template.id
            )
        )
        #Update host with host tag
        cls.hosts = Host.list(
            cls.apiclient,
            zoneid=cls.zone.id,
            hypervisor=cls.hypervisor
        )
        if not validateList(cls.hosts)[0]:
            assert False, "Invalid response for list host"
        host_res = Host.update(
            cls.apiclient,
            id=cls.hosts[0].id,
            hosttags=HOSTTAG1
        )
        if host_res is None:
            assert False, "Failed to update host with host tag"
        #Create compute offering matching with host resources
        host_details = cls.config.__dict__["zones"][0].__dict__["pods"][0].\
            __dict__["clusters"][0].__dict__["hosts"][0].__dict__
        cpu_cores = host_details["cpunumber"]
        cpu_speed = host_details["cpuspeed"]
        memory = host_details["memory"]
        cls.services["service_offering"]["displaytext"] = "BMOffering"
        cls.services["service_offering"]["name"] = "BMOffering"
        cls.services["service_offering"]["cpunumber"] = cpu_cores
        cls.services["service_offering"]["cpuspeed"] = cpu_speed
        cls.services["service_offering"]["memory"] = memory
        cls.services["service_offering"]["hosttags"] = HOSTTAG1
        cls.compute_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"],
        )
        cls._cleanup = [cls.compute_offering, cls.account]

    @classmethod
    def tearDownClass(cls):
        try:
            Host.update(
                cls.apiclient,
                id=cls.hosts[0].id,
                hosttags=""
            )
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            #Clean up, terminate the created network offerings
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["baremetal"])
    def test_deploy_vm(self):
        """
        @desc: Test vm deployment on Baremetal host
        """
        self.debug("Test VM deployment in baremetal")
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine2"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id,
            serviceofferingid=self.compute_offering.id,
            mode=self.zone.networktype
        )
        self.cleanup.append(vm)
        vm_res = VirtualMachine.list(
            self.apiclient,
            id=vm.id
        )
        self.assertEqual(validateList(vm_res)[0], PASS, "Invalid response for list vms")
        self.assertEqual(
            vm.id,
            vm_res[0].id,
            "Check virtual machine id in listVirtualMachines"
        )
        self.assertEqual(
            vm_res[0].state,
            "Running",
            "VM is not in Running state"
        )
        return
