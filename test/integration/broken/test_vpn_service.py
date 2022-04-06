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

""" P1 tests for VPN service
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackException import CloudstackAPIException
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (
                                        Account,
                                        ServiceOffering,
                                        VirtualMachine,
                                        PublicIPAddress,
                                        Vpn,
                                        VpnUser,
                                        Configurations,
                                        NATRule
                                        )
from marvin.lib.common import (get_domain,
                                        get_zone,
                                        get_template
                                        )
from marvin.lib.utils import cleanup_resources
import subprocess

class Services:
    """Test VPN Service
    """

    def __init__(self):
        self.services = {
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
                                    "cpuspeed": 100,    # in MHz
                                    "memory": 128,    # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small Disk Offering",
                                    "name": "Small Disk Offering",
                                    "disksize": 1
                        },
                        "virtual_machine": {
                                    "displayname": "TestVM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'KVM',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                                },
                         "vpn_user": {
                                   "username": "test",
                                   "password": "test",
                                },
                         "natrule": {
                                   "privateport": 1701,
                                   "publicport": 1701,
                                   "protocol": "UDP"
                                },
                        "ostype": 'CentOS 5.5 (64-bit)',
                        "sleep": 60,
                        "timeout": 10,
                        # Networking mode: Advanced, Basic
                    }


class TestVPNService(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPNService, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.services["mode"] = cls.zone.networktype

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )

        cls._cleanup = [cls.service_offering, ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        try:
            self.apiclient = self.testClient.getApiClient()
            self.dbclient = self.testClient.getDbConnection()
            self.account = Account.create(
                                self.apiclient,
                                self.services["account"],
                                domainid=self.domain.id
                                )
            self.cleanup = [
                            self.account,
                            ]
            self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.template.id,
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                    )
            self.public_ip = PublicIPAddress.create(
                                               self.apiclient,
                                               accountid=self.virtual_machine.account,
                                               zoneid=self.virtual_machine.zoneid,
                                               domainid=self.virtual_machine.domainid,
                                               services=self.services["virtual_machine"]
                                               )
            return
        except CloudstackAPIException as e:
                self.tearDown()
                raise e

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def create_VPN(self, public_ip):
        """Creates VPN for the network"""

        self.debug("Creating VPN with public IP: %s" % public_ip.ipaddress.id)
        try:
            # Assign VPN to Public IP
            vpn = Vpn.create(self.apiclient,
                         self.public_ip.ipaddress.id,
                         account=self.account.name,
                         domainid=self.account.domainid)

            self.debug("Verifying the remote VPN access")
            vpns = Vpn.list(self.apiclient,
                        publicipid=public_ip.ipaddress.id,
                        listall=True)
            self.assertEqual(
                         isinstance(vpns, list),
                         True,
                         "List VPNs shall return a valid response"
                         )
            return vpn
        except Exception as e:
            self.fail("Failed to create remote VPN access: %s" % e)


    @attr(tags=["advanced", "advancedns"])
    def test_01_VPN_service(self):
        """Tests if VPN service is running"""

        # Validate if IPSEC is running on the public
        # IP by using ike-scan

        self.create_VPN(self.public_ip)

        cmd = ['ike-scan', self.public_ip, '-s', '4534'] # Random port

        stdout = subprocess.check_output(cmd)

        if "1 returned handshake" not in stdout:
            self.fail("Unable to connect to VPN service")

        return
