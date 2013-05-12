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
""" P1 tests for Scaling up Vm
"""
#Import Local Modules
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.remoteSSHClient import remoteSSHClient
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr
#Import System modules
import time

_multiprocess_shared_ = True
class Services:
    """Test VM Life Cycle Services
    """

    def __init__(self):
        self.services = {

                "account": {
                    "email": "test@test.com",
                    "firstname": "Test",
                    "lastname": "User",
                    "username": "test",
                    # Random characters are appended in create account to 
                    # ensure unique username generated each time
                    "password": "password",
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
                 "small":
                    {
                     # Small service offering ID to for change VM 
                     # service offering from medium to small
                        "name": "SmallInstance",
                        "displaytext": "SmallInstance",
                        "cpunumber": 1,
                        "cpuspeed": 100,
                        "memory": 256,
                    },
                "big":
                    {
                     # Big service offering ID to for change VM 
                        "name": "BigInstance",
                        "displaytext": "BigInstance",
                        "cpunumber": 1,
                        "cpuspeed": 100,
                        "memory": 512,
                    }
                },
                #Change this
                "template": {
                    "displaytext": "xs",
                    "name": "xs",
                    "passwordenabled": False,
                },
            "diskdevice": '/dev/xvdd',
            # Disk device where ISO is attached to instance
            "mount_dir": "/mnt/tmp",
            "sleep": 60,
            "timeout": 10,
            #Migrate VM to hostid
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
        }

class TestScaleVm(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestScaleVm, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        domain = get_domain(cls.api_client, cls.services)
        zone = get_zone(cls.api_client, cls.services)
        cls.services['mode'] = zone.networktype

        template = get_template(
                            cls.api_client,
                            zone.id,
                            cls.services["ostype"]
                            )
        # Set Zones and disk offerings ??
        cls.services["small"]["zoneid"] = zone.id
        cls.services["small"]["template"] = template.id

        # Create account, service offerings, vm.
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=domain.id
                            )

        cls.small_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["small"]
                                    )
        
        cls.big_offering = ServiceOffering.create(
                                    cls.api_client,
                                    cls.services["service_offerings"]["big"]
                                    )

        #create a virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                        cls.api_client,
                                        cls.services["small"],
                                        accountid=cls.account.name,
                                        domainid=cls.account.domainid,
                                        serviceofferingid=cls.small_offering.id,
                                        mode=cls.services["mode"]
                                        )
        #how does it work ??
        cls._cleanup = [
                        cls.small_offering,
                        cls.account
                        ]

    @classmethod
    def tearDownClass(cls):
        cls.api_client = super(TestScaleVm, cls).getClsTestClient().getApiClient()
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

    @attr(tags = ["xenserver", "advanced", "basic"])
    def test_01_scale_vm(self):
        """Test scale virtual machine 
        """
        # Validate the following
        # Scale up the vm and see if it scales to the new svc offering and is finally in running state
        
      
      
        self.debug("Scaling VM-ID: %s to service offering: %s" % (
                                        self.virtual_machine.id,
                                        self.big_offering.id
                                        ))
        
        cmd = scaleVirtualMachine.scaleVirtualMachineCmd()
        cmd.serviceofferingid = self.big_offering.id
        cmd.id = self.virtual_machine.id
        self.apiclient.scaleVirtualMachine(cmd)  

        list_vm_response = list_virtual_machines(
                                            self.apiclient,
                                            id=self.virtual_machine.id
                                            )
        self.assertEqual(
                        isinstance(list_vm_response, list),
                        True,
                        "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            list_vm_response,
                            None,
                            "Check virtual machine is listVirtualMachines"
                            )

        vm_response = list_vm_response[0]

        self.assertEqual(
                        vm_response.id,
                        self.virtual_machine.id,
                        "Check virtual machine ID of scaled VM"
                        )

        self.assertEqual(
                        vm_response.serviceofferingid,
                        self.big_offering.id,
                        "Check service offering of the VM"
                        )

        self.assertEqual(
                        vm_response.state,
                        'Running',
                        "Check the state of VM"
                        )
        return
