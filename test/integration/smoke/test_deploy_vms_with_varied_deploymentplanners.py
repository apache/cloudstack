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

from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import Account, VirtualMachine, ServiceOffering, Host, Cluster
from marvin.lib.common import get_zone, get_domain, get_template
from marvin.lib.utils import cleanup_resources
from nose.plugins.attrib import attr

class TestDeployVmWithVariedPlanners(cloudstackTestCase):
    """ Test to create services offerings for deployment planners
	- firstfit, userdispersing
    """

    @classmethod
    def setUpClass(cls):
        testClient = super(TestDeployVmWithVariedPlanners, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"]
        )

        if cls.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % cls.services["ostype"]

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.hosts = Host.list(cls.apiclient, type='Routing')
        cls.clusters = Cluster.list(cls.apiclient)
        cls.cleanup = [
            cls.account
        ]

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_deployvm_firstfit(self):
        """Test to deploy vm with a first fit offering
        """
        #FIXME: How do we know that first fit actually happened?
        self.service_offering_firstfit = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"],
            deploymentplanner='FirstFitPlanner'
        )

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_firstfit.id,
            templateid=self.template.id
        )

        list_vms = VirtualMachine.list(self.apiclient, id=self.virtual_machine.id)
        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"\
            % self.virtual_machine.id
        )
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
            vm.state,
            "Running",
            msg="VM is not in Running state"
        )

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_deployvm_userdispersing(self):
        """Test deploy VMs using user dispersion planner
        """
        self.service_offering_userdispersing = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"],
            deploymentplanner='UserDispersingPlanner'
        )

        self.virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_userdispersing.id,
            templateid=self.template.id
        )
        self.virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_userdispersing.id,
            templateid=self.template.id
        )

        list_vm_1 = VirtualMachine.list(self.apiclient, id=self.virtual_machine_1.id)
        list_vm_2 = VirtualMachine.list(self.apiclient, id=self.virtual_machine_2.id)
        self.assertEqual(
            isinstance(list_vm_1, list),
            True,
            "List VM response was not a valid list"
        )
        self.assertEqual(
            isinstance(list_vm_2, list),
            True,
            "List VM response was not a valid list"
        )
        vm1 = list_vm_1[0]
        vm2 = list_vm_2[0]
        self.assertEqual(
            vm1.state,
            "Running",
            msg="VM is not in Running state"
        )
        self.assertEqual(
            vm2.state,
            "Running",
            msg="VM is not in Running state"
        )
        vm1clusterid = filter(lambda c: c.id == vm1.hostid, self.hosts)[0].clusterid
        vm2clusterid = filter(lambda c: c.id == vm2.hostid, self.hosts)[0].clusterid
        if vm1clusterid == vm2clusterid:
            self.debug("VMs (%s, %s) meant to be dispersed are deployed in the same cluster %s" % (
            vm1.id, vm2.id, vm1clusterid))

    @attr(tags=["advanced", "basic", "sg"], required_hardware="false")
    def test_deployvm_userconcentrated(self):
        """Test deploy VMs using user concentrated planner
        """
        self.service_offering_userconcentrated = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"],
            deploymentplanner='UserConcentratedPodPlanner'
        )

        self.virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_userconcentrated.id,
            templateid=self.template.id
        )
        self.virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_userconcentrated.id,
            templateid=self.template.id
        )

        list_vm_1 = VirtualMachine.list(self.apiclient, id=self.virtual_machine_1.id)
        list_vm_2 = VirtualMachine.list(self.apiclient, id=self.virtual_machine_2.id)
        self.assertEqual(
            isinstance(list_vm_1, list),
            True,
            "List VM response was not a valid list"
        )
        self.assertEqual(
            isinstance(list_vm_2, list),
            True,
            "List VM response was not a valid list"
        )
        vm1 = list_vm_1[0]
        vm2 = list_vm_2[0]
        self.assertEqual(
            vm1.state,
            "Running",
            msg="VM is not in Running state"
        )
        self.assertEqual(
            vm2.state,
            "Running",
            msg="VM is not in Running state"
        )
        vm1clusterid = filter(lambda c: c.id == vm1.hostid, self.hosts)[0].clusterid
        vm2clusterid = filter(lambda c: c.id == vm2.hostid, self.hosts)[0].clusterid

        vm1podid = filter(lambda p: p.id == vm1clusterid, self.clusters)[0].podid
        vm2podid = filter(lambda p: p.id == vm2clusterid, self.clusters)[0].podid
        self.assertEqual(
            vm1podid,
            vm2podid,
            msg="VMs (%s, %s) meant to be pod concentrated are deployed on different pods (%s, %s)" % (vm1.id, vm2.id, vm1clusterid, vm2clusterid)
        )

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
