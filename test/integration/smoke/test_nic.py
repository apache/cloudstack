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
""" NIC tests for VM """
import marvin
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.remoteSSHClient import remoteSSHClient
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from nose.plugins.attrib import attr

import signal
import sys
import time

class Services:
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
                "username": "test",
                # Random characters are appended in create account to
                # ensure unique username generated each time
                "password": "password",
            },
            # Create a small virtual machine instance with disk offering
            "small": {
                "displayname": "testserver",
                "username": "root", # VM creds for SSH
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "service_offerings": {
                "tiny": {
                    "name": "Tiny Instance",
                    "displaytext": "Tiny Instance",
                    "cpunumber": 1,
                    "cpuspeed": 100, # in MHz
                    "memory": 128, # In MBs
                },
            },
            "network_offering": {
                "name": 'Test Network offering',
                "displaytext": 'Test Network offering',
                "guestiptype": 'Isolated',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "serviceProviderList" : {
                    "Dhcp": 'VirtualRouter',
                    "Dns": 'VirtualRouter',
                    "SourceNat": 'VirtualRouter',
                    "PortForwarding": 'VirtualRouter',
                },
            },
            "network": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "acltype": "Account",
            },
            # ISO settings for Attach/Detach ISO tests
            "iso": {
                "displaytext": "Test ISO",
                "name": "testISO",
                "url": "http://people.apache.org/~tsp/dummy.iso",
                 # Source URL where ISO is located
                "ostype": 'CentOS 5.3 (64-bit)',
                "mode": 'HTTP_DOWNLOAD', # Downloading existing ISO 
            },
            "template": {
                "displaytext": "Cent OS Template",
                "name": "Cent OS Template",
                "passwordenabled": True,
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

class TestDeployVM(cloudstackTestCase):

    def setUp(self):
        self.cleanup = []
        self.cleaning_up = 0

        def signal_handler(signal, frame):
            self.tearDown()
            sys.exit(0)

        # assign the signal handler immediately
        signal.signal(signal.SIGINT, signal_handler)

        try:
            self.apiclient = self.testClient.getApiClient()
            self.dbclient  = self.testClient.getDbConnection()
            self.services  = Services().services

            # Get Zone, Domain and templates
            domain = get_domain(self.apiclient, self.services)
            zone = get_zone(self.apiclient, self.services)
            self.services['mode'] = zone.networktype

            if self.services['mode'] != 'Advanced':
                self.debug("Cannot run this test with a basic zone, please use advanced!")
                return

            #if local storage is enabled, alter the offerings to use localstorage
            #this step is needed for devcloud
            if zone.localstorageenabled == True:
                self.services["service_offerings"]["tiny"]["storagetype"] = 'local'

            template = get_template(
                                self.apiclient,
                                zone.id,
                                self.services["ostype"]
                                )
            # Set Zones and disk offerings
            self.services["small"]["zoneid"] = zone.id
            self.services["small"]["template"] = template.id

            self.services["iso"]["zoneid"] = zone.id
            self.services["network"]["zoneid"] = zone.id

            # Create Account, VMs, NAT Rules etc
            self.account = Account.create(
                                self.apiclient,
                                self.services["account"],
                                domainid=domain.id
                                )
            self.cleanup.insert(0, self.account)

            self.service_offering = ServiceOffering.create(
                                        self.apiclient,
                                        self.services["service_offerings"]["tiny"]
                                        )
            self.cleanup.insert(0, self.service_offering)

            ####################
            ### Network offering
            self.network_offering = NetworkOffering.create(
                                        self.apiclient,
                                        self.services["network_offering"],
                                        )
            self.cleanup.insert(0, self.network_offering)
            self.network_offering.update(self.apiclient, state='Enabled') # Enable Network offering
            self.services["network"]["networkoffering"] = self.network_offering.id

            ################
            ### Test Network
            self.test_network = Network.create(
                                                 self.apiclient,
                                                 self.services["network"],
                                                 self.account.account.name,
                                                 self.account.account.domainid,
                                                 )
            self.cleanup.insert(0, self.test_network)
        except Exception as ex:
            self.debug("Exception during NIC test SETUP!: " + str(ex))
            self.assertEqual(True, False, "Exception during NIC test SETUP!: " + str(ex))

    @attr(tags = ["devcloud", "advanced", "advancedns", "smoke", "basic", "sg"])
    def test_01_nic(self):
        if self.services['mode'] != 'Advanced':
            self.debug("Cannot run this test with a basic zone, please use advanced!")
            return
        try:
            self.virtual_machine = VirtualMachine.create(
                                        self.apiclient,
                                        self.services["small"],
                                        accountid=self.account.account.name,
                                        domainid=self.account.account.domainid,
                                        serviceofferingid=self.service_offering.id,
                                        mode=self.services['mode']
                                    )
            self.cleanup.insert(0, self.virtual_machine)

            list_vm_response = list_virtual_machines(
                                                     self.apiclient,
                                                     id=self.virtual_machine.id
                                                     )

            self.debug(
                    "Verify listVirtualMachines response for virtual machine: %s" \
                    % self.virtual_machine.id
                )

            self.assertEqual(
                                isinstance(list_vm_response, list),
                                True,
                                "Check list response returns a valid list"
                            )

            self.assertNotEqual(
                                len(list_vm_response),
                                0,
                                "Check VM available in List Virtual Machines"
                            )
            vm_response = list_vm_response[0]

            self.assertEqual(

                                vm_response.id,
                                self.virtual_machine.id,
                                "Check virtual machine id in listVirtualMachines"
                            )

            self.assertEqual(
                        vm_response.name,
                        self.virtual_machine.name,
                        "Check virtual machine name in listVirtualMachines"
                        )

            self.assertEqual(
                        len(vm_response.nic),
                        1,
                        "Verify we only start with one nic"
                        )

            self.assertEqual(
                vm_response.nic[0].isdefault,
                True,
                "Verify initial adapter is set to default"
            )
            existing_nic_ip = vm_response.nic[0].ipaddress
            existing_nic_id = vm_response.nic[0].id

            # 1. add a nic
            add_response = self.virtual_machine.add_nic(self.apiclient, self.test_network.id)

            time.sleep(5)
            # now go get the vm list?

            list_vm_response = list_virtual_machines(
                                                self.apiclient,
                                                id=self.virtual_machine.id
                                                )

            self.assertEqual(
                        len(list_vm_response[0].nic),
                        2,
                        "Verify we have 2 NIC's now"
                        )

            new_nic_id = ""
            for nc in list_vm_response[0].nic:
                if nc.ipaddress != existing_nic_ip:
                    new_nic_id = nc.id

            self.virtual_machine.update_default_nic(self.apiclient, new_nic_id)

            time.sleep(5)

            list_vm_response = list_virtual_machines(
                                                self.apiclient,
                                                id=self.virtual_machine.id
                                                )

            # iterate as we don't know for sure what order our NIC's will be returned to us.
            for nc in list_vm_response[0].nic:
                if nc.ipaddress == existing_nic_ip:
                    self.assertEqual(
                        nc.isdefault,
                        False,
                        "Verify initial adapter is NOT set to default"
                    )
                else:
                    self.assertEqual(
                        nc.isdefault,
                        True,
                        "Verify second adapter is set to default"
                    )

            sawException = False
            try:
                self.virtual_machine.remove_nic(self.apiclient, new_nic_id)
            except Exception as ex:
                sawException = True

            self.assertEqual(sawException, True, "Make sure we cannot delete the default NIC")

            self.virtual_machine.remove_nic(self.apiclient, existing_nic_id)
            time.sleep(5)

            list_vm_response = list_virtual_machines(
                                                self.apiclient,
                                                id=self.virtual_machine.id
                                                )

            self.assertEqual(
                        len(list_vm_response[0].nic),
                        1,
                        "Verify we are back to a signle NIC"
                        )

            return
        except Exception as ex:
            self.debug("Exception during NIC test!: " + str(ex))
            self.assertEqual(True, False, "Exception during NIC test!: " + str(ex))

    def tearDown(self):
        if self.services['mode'] != 'Advanced':
            self.debug("Cannot run this test with a basic zone, please use advanced!")
            return

        if self.cleaning_up == 1:
            return

        self.cleaning_up = 1
        try:
            for obj in self.cleanup:
                try:
                    obj.delete(self.apiclient)
                    time.sleep(10)
                except Exception as ex:
                    self.debug("Error deleting: " + str(obj) + ", exception: " + str(ex))

        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)
        self.cleaning_up = 0

