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
""" P1 tests for Storage motion
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.sshClient import SshClient
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True
class Services:
    """Test explicit dedication
    """

    def __init__(self):
        self.services = {
                "disk_offering":{
                    "displaytext": "Small",
                    "name": "Small",
                    "disksize": 1
                },
                "account": {
                    "email": "test@test.com",
                    "firstname": "Test",
                    "lastname": "User",
                    "username": "testexplicit",
                    # Random characters are appended in create account to
                    # ensure unique username generated each time
                    "password": "password",
                },
                "virtual_machine" :
                {
                    "affinity": {
                        "name": "explicit",
                        "type": "ExplicitDedication",
                    },
                    "hypervisor" : "XenServer",
                },
                "small":
                # Create a small virtual machine instance with disk offering
                {
                    "displayname": "testserver",
                    "username": "root", # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
                "service_offerings":
                {
                 "for-explicit":
                    {
                     # Small service offering ID to for change VM
                     # service offering from medium to small
                        "name": "For explicit",
                        "displaytext": "For explicit",
                        "cpunumber": 1,
                        "cpuspeed": 500,
                        "memory": 512
                    }
                },
                "template": {
                    "displaytext": "Cent OS Template",
                    "name": "Cent OS Template",
                    "passwordenabled": True,
                },
                "sleep": 60,
                "timeout": 10,
                "ostype": 'CentOS 5.3 (64-bit)'
        }

class TestExplicitDedication(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestExplicitDedication, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = cls.zone.networktype

        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = cls.template.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.small_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["for-explicit"]
                                    )

        #cls.ag = AffinityGroup.create(cls.api_client, cls.services["virtual_machine"]["affinity"],
         #   account=cls.services["account"], domainid=cls.domain.id)

        cls._cleanup = [
                        cls.small_offering,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        cls.api_client = super(TestExplicitDedication, cls).getClsTestClient().getApiClient()
        cleanup_resources(cls.api_client, cls._cleanup)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        #Clean up, terminate the created ISOs
        cleanup_resources(self.apiclient, self.cleanup)
        return

    # This test requires multi host and at least one host which is empty (no vms should
    # be running on that host). It explicitly dedicates empty host to an account, deploys
    # a vm for that account and verifies that the vm gets deployed to the dedicated host.
    @attr(tags = ["advanced", "basic", "multihosts", "explicitdedication"])
    def test_01_deploy_vm_with_explicit_dedication(self):
        """Test explicit dedication is placing vms of an account on dedicated hosts.
        """
        # Validate the following
        # 1. Find and dedicate an empty host to an account.
        # 2. Create an affinity group for explicit dedication.
        # 3. Create a vm deployment by passing the affinity group as a parameter.
        # 4. Validate the vm got deployed on the dedicated host.
        # 5. Cleanup.

        # list and find an empty hosts
        all_hosts = list_hosts(
                           self.apiclient,
                           type='Routing',
                           )

        empty_host = None
        for host in all_hosts:
            vms_on_host = list_virtual_machines(
                                                self.api_client,
                                                hostid=host.id)
            if not vms_on_host:
                empty_host = host
                break

        #If no empty host is found, return
        if empty_host:
           self.skipTest("Did not find any empty hosts, Skipping")

        # Create an affinity group for explicit dedication.
        agCmd = createAffinityGroup.createAffinityGroupCmd()
        agCmd.name = "explicit-affinity"
        agCmd.displayText = "explicit-affinity"
        agCmd.account = self.account.name
        agCmd.domainid = self.account.domainid
        agCmd.type = self.services['virtual_machine']['affinity']['type']
        self.apiclient.createAffinityGroup(agCmd)

        # dedicate the empty host to this account.
        dedicateCmd = dedicateHost.dedicateHostCmd()
        dedicateCmd.hostid = empty_host.id
        dedicateCmd.domainid = self.domain.id
        self.apiclient.dedicateHost(dedicateCmd)

        # deploy vm on the dedicated resource.
        vm = VirtualMachine.create(
            self.api_client,
            self.services["small"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.small_offering.id,
            affinitygroupnames=["explicit-affinity"],
            mode=self.services["mode"]
        )

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=vm.id
                                            )

        vm_response = list_vm_response[0]

        self.assertEqual(
                        vm_response.hostid,
                        empty_host.id,
                        "Check destination hostID of deployed VM"
                        )

        # release the dedicated host to this account.
        releaseCmd = releaseDedicatedHost.releaseDedicatedHostCmd()
        releaseCmd.hostid = empty_host.id
        releaseCmd.domainid = self.domain.id
        self.apiclient.releaseDedicatedHost(releaseCmd)

        #Deletion of the created VM and affinity group is taken care as part of account clean

        return
