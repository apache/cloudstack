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
""" P1 tests for Dedicating Public IP addresses
"""
#Import Local Modules
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
import datetime
from socket import inet_aton
from struct import unpack

class TestDedicatePublicIPRange(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestDedicatePublicIPRange, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services =  cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.services["zoneid"] = cls.zone.id
        cls.pod = get_pod(cls.apiclient, cls.zone.id)
        # Create Account
        cls.account = Account.create(
                            cls.apiclient,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls._cleanup = [
                        cls.account,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "publiciprange", "dedicate", "release"], required_hardware="false")
    def test_dedicatePublicIpRange(self):
        """Test public IP range dedication
        """

        # Validate the following:
        # 1. Create a Public IP range
        # 2. Created IP range should be present, verify with listVlanIpRanges
        # 3. Dedicate the created IP range to user account
        # 4. Verify IP range is dedicated, verify with listVlanIpRanges
        # 5. Release the dedicated Public IP range back to the system
        # 6. Verify IP range has been released, verify with listVlanIpRanges
        # 7. Delete the Public IP range

        self.debug("Creating Public IP range")
        self.public_ip_range = PublicIpRange.create(
                                    self.apiclient,
                                    self.services
                               )
        list_public_ip_range_response = PublicIpRange.list(
                                            self.apiclient,
                                            id=self.public_ip_range.vlan.id
                                        )
        self.debug(
                "Verify listPublicIpRanges response for public ip ranges: %s" \
                % self.public_ip_range.vlan.id
            )
        self.assertEqual(
                         isinstance(list_public_ip_range_response, list),
                         True,
                         "Check for list Public IP range response"
                         )
        public_ip_response = list_public_ip_range_response[0]
        self.assertEqual(
                            public_ip_response.id,
                            self.public_ip_range.vlan.id,
                            "Check public ip range response id is in listVlanIpRanges"
                        )

        self.debug("Dedicating Public IP range");
        dedicate_public_ip_range_response = PublicIpRange.dedicate(
                                                self.apiclient,
                                                self.public_ip_range.vlan.id,
                                                account=self.account.name,
                                                domainid=self.account.domainid
                                            )
        list_public_ip_range_response = PublicIpRange.list(
                                            self.apiclient,
                                            id=self.public_ip_range.vlan.id
                                        )
        public_ip_response = list_public_ip_range_response[0]
        self.assertEqual(
                            public_ip_response.account,
                            self.account.name,
                            "Check account name is in listVlanIpRanges as the account public ip range is dedicated to"
                        )

        self.debug("Releasing Public IP range");
        self.public_ip_range.release(self.apiclient)
        list_public_ip_range_response = PublicIpRange.list(
                                            self.apiclient,
                                            id=self.public_ip_range.vlan.id
                                        )
        public_ip_response = list_public_ip_range_response[0]
        self.assertEqual(
                            public_ip_response.account,
                            "system",
                            "Check account name is system account in listVlanIpRanges"
                        )

        self.debug("Deleting Public IP range");
        self.public_ip_range.delete(self.apiclient)

        return

    @attr(tags = ["advanced", "publiciprange", "dedicate", "release"], required_hardware="false")
    def test_dedicate_public_ip_range_for_system_vms(self):
        """Test public IP range dedication for SSVM and CPVM
        """

        # Validate the following:
        # 1. Create a Public IP range for system vms
        # 2. Created IP range should be present and marked as forsystemvms=true, verify with listVlanIpRanges
        # 7. Delete the Public IP range
        
        services = {
            "gateway":"192.168.99.1",
            "netmask":"255.255.255.0",
            "startip":"192.168.99.2",
            "endip":"192.168.99.200",
            "forvirtualnetwork":self.services["forvirtualnetwork"],
            "zoneid":self.services["zoneid"],
            "vlan":self.services["vlan"]
        }
        public_ip_range = PublicIpRange.create(
            self.apiclient,
            services,
            forsystemvms = True
        )
        created_ip_range_response = PublicIpRange.list(
            self.apiclient,
            id = public_ip_range.vlan.id
        )
        self.assertEqual(
            len(created_ip_range_response),
            1,
            "Check listVlanIpRanges response"
        )
        self.assertTrue(
            created_ip_range_response[0].forsystemvms,
            "Check forsystemvms parameter in created vlan ip range"
        )
        
        # Delete range
        public_ip_range.delete(self.apiclient)
        
    def get_ip_as_number(self, ip_string):
        """ Return numeric value for ip (passed as a string)
        """
        packed_ip = inet_aton(ip_string)
        return unpack(">L", packed_ip)[0]
    
    def is_ip_in_range(self, start_ip, end_ip, ip_to_test):
        """ Check whether ip_to_test belongs to IP range between start_ip and end_ip
        """
        start = self.get_ip_as_number(start_ip)
        end = self.get_ip_as_number(end_ip)
        ip = self.get_ip_as_number(ip_to_test)
        return start <= ip and ip <= end
    
    def wait_for_system_vm_start(self, domain_id, srv_timeout, srv_sleep, systemvmtype):
        """ Wait until system vm is Running
        """
        timeout = srv_timeout
        while True:
            list_systemvm_response = list_ssvms(
                self.apiclient,
                systemvmtype=systemvmtype,
                domainid=domain_id
            )
            if isinstance(list_systemvm_response, list):
                if list_systemvm_response[0].state == 'Running':
                    return list_systemvm_response[0].id
            if timeout == 0:
                raise Exception("List System VM call failed!")

            time.sleep(srv_sleep)
            timeout = timeout - 1
        return None

    def base_system_vm(self, services, systemvmtype):
        """
        Base for CPVM or SSVM depending on systemvmtype parameter
        """

        # Create range for system vms
        self.debug("Creating Public IP range for system vms")
        public_ip_range = PublicIpRange.create(
            self.apiclient,
            services,
            forsystemvms = True
        )

        # List Running System VM
        list_systemvm_response = list_ssvms(
            self.apiclient,
            systemvmtype=systemvmtype,
            state='Running',
            domainid=public_ip_range.vlan.domainid
        )
        self.assertTrue(
            isinstance(list_systemvm_response, list),
            "Check list response returns a valid list"
        )
        self.assertEqual(
            len(list_systemvm_response),
            1,
            "Check list response size"
        )

        # Delete System VM
        systemvm = list_systemvm_response[0]
        self.debug("Destroying System VM: %s" % systemvm.id)
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = systemvm.id
        self.apiclient.destroySystemVm(cmd)

        # Wait for CPVM to start
        systemvm_id = self.wait_for_system_vm_start(
            public_ip_range.vlan.domainid,
            self.services["timeout"],
            self.services["sleep"],
            systemvmtype
        )
        self.assertNotEqual(
            systemvm_id,
            None,
            "Check CPVM id is not none"
        )
        list_systemvm_response = list_ssvms(
            self.apiclient,
            id=systemvm_id
        )
        self.assertEqual(
            isinstance(list_systemvm_response, list),
            True,
            "Check list response returns a valid list"
        )
        systemvm_response = list_systemvm_response[0]
        self.debug("System VM state after debug: %s" % systemvm_response.state)
        self.assertEqual(
            systemvm_response.state,
            'Running',
            "Check whether System VM is running or not"
        )

        # Verify System VM got IP in the created range
        startip = services["startip"]
        endip = services["endip"]
        cpvm_ip = systemvm_response.publicip

        self.assertTrue(
            self.is_ip_in_range(startip, endip, cpvm_ip),
            "Check whether System VM Public IP is in range dedicated to system vms"
        )

        # Delete System VM and IP range, so System VM can get IP from original ranges
        self.debug("Destroying System VM: %s" % systemvm_id)
        cmd = destroySystemVm.destroySystemVmCmd()
        cmd.id = systemvm_id
        self.apiclient.destroySystemVm(cmd)

        domain_id = public_ip_range.vlan.domainid
        public_ip_range.delete(self.apiclient)

        # Wait for System VM to start and check System VM public IP
        systemvm_id = self.wait_for_system_vm_start(
            domain_id,
            self.services["timeout"],
            self.services["sleep"],
            systemvmtype
        )
        list_systemvm_response = list_ssvms(
            self.apiclient,
            id=systemvm_id
        )
        self.assertFalse(
            self.is_ip_in_range(startip, endip, list_systemvm_response[0].publicip),
            "Check System VM Public IP is not in range dedicated to system vms"
        )

        return True

    def exists_public_ip_range_for_system_vms(self, zoneid):
        """
        Return True if there exists a public IP range dedicated for system vms in zoneid
        """
        existing_ip_ranges_response = PublicIpRange.list(
            self.apiclient,
            zoneid=zoneid
        )
        for r in existing_ip_ranges_response:
            if r.forsystemvms:
                return True
        return False

    @attr(tags = ["advanced", "publiciprange", "dedicate", "release"], required_hardware="false")
    def test_dedicate_public_ip_range_for_system_vms_cpvm(self):
        """Test CPVM Public IP
        """
        self.debug("Precondition: No public IP range dedicated for system vms in the environment")
        if self.exists_public_ip_range_for_system_vms(self.services["zoneid"]):
            self.skipTest("An existing IP range defined for system vms, aborting test")
        
        services = {
            "gateway":"192.168.100.1",
            "netmask":"255.255.255.0",
            "startip":"192.168.100.2",
            "endip":"192.168.100.200",
            "forvirtualnetwork":self.services["forvirtualnetwork"],
            "zoneid":self.services["zoneid"],
            "vlan":self.services["vlan"]
        }

        self.base_system_vm(
            services,
            'consoleproxy'
        )
        return

    @attr(tags = ["advanced", "publiciprange", "dedicate", "release"], required_hardware="false")
    def test_dedicate_public_ip_range_for_system_vms_ssvm(self):
        """Test SSVM Public IP
        """
        self.debug("Precondition: No public IP range dedicated for system vms in the environment")
        if self.exists_public_ip_range_for_system_vms(self.services["zoneid"]):
            self.skipTest("An existing IP range defined for system vms, aborting test")

        services = {
            "gateway":"192.168.200.1",
            "netmask":"255.255.255.0",
            "startip":"192.168.200.2",
            "endip":"192.168.200.200",
            "forvirtualnetwork":self.services["forvirtualnetwork"],
            "zoneid":self.services["zoneid"],
            "vlan":self.services["vlan"]
        }

        self.base_system_vm(
            services,
            'secondarystoragevm'
        )
        return