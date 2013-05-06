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

from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from marvin import remoteSSHClient
from nose.plugins.attrib import attr

class Services:
    """Test Account Services
    """

    def __init__(self):
        self.services = {
            "domain": {
                "name": "Domain",
            },
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                # in MHz
                "memory": 64,
                # In MBs
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            "virtual_machine" : {
                "affinity": {
                    "name": "webvms",
                    "type": "host anti-affinity",
                },
                "hypervisor" : "XenServer",
            }
        }


class TestDeployVmWithAffinityGroup(cloudstackTestCase):
    """
    This test deploys a virtual machine into a user account
    using the small service offering and builtin template
    """

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestDeployVmWithAffinityGroup, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls.ag = AffinityGroup.create(cls.api_client, cls.services["virtual_machine"]["affinity"],
            account=cls.services["account"], domainid=cls.domain.id)

        cls._cleanup = [
            cls.service_offering,
            cls.account,
        ]
        return

    @attr(tags=["simulator", "basic", "advanced", "multihost"])
    def test_DeployVmAntiAffinityGroup(self):
        """
        test DeployVM in anti-affinity groups

        deploy VM1 and VM2 in the same host-anti-affinity groups
        Verify that the vms are deployed on separate hosts
        """
        #deploy VM1 in affinity group created in setUp
        vm1 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            affinitygroupnames=[self.ag.name]
        )

        list_vm1 = list_virtual_machines(
            self.api_client,
            id=vm1.id
        )
        self.assertEqual(
            isinstance(list_vm1, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_vm1),
            0,
            "Check VM available in List Virtual Machines"
        )
        vm1_response = list_vm1[0]
        self.assertEqual(
            vm1_response.state,
            'Running',
            msg="VM is not in Running state"
        )
        host_of_vm1 = vm1_response.hostid

        #deploy VM2 in affinity group created in setUp
        vm2 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            affinitygroupnames=[self.ag.name]
        )
        list_vm2 = list_virtual_machines(
            self.api_client,
            id=vm2.id
        )
        self.assertEqual(
            isinstance(list_vm2, list),
            True,
            "Check list response returns a valid list"
        )
        self.assertNotEqual(
            len(list_vm2),
            0,
            "Check VM available in List Virtual Machines"
        )
        vm2_response = list_vm2[0]
        self.assertEqual(
            vm2_response.state,
            'Running',
            msg="VM is not in Running state"
        )
        host_of_vm2 = vm2_response.hostid

        self.assertNotEqual(host_of_vm1, host_of_vm2,
            msg="Both VMs of affinity group %s are on the same host" % self.ag.name)


        @classmethod
        def tearDown(cls):
            try:
                #cls.api_client = super(TestDeployVmWithAffinityGroup, cls).getClsTestClient().getApiClient()
                #Clean up, terminate the created templates
                cleanup_resources(cls.api_client, cls.cleanup)
            except Exception as e:
                raise Exception("Warning: Exception during cleanup : %s" % e)
