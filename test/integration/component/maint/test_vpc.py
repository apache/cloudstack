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

""" Component tests for VPC functionality
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources, validateList, get_process_status
from marvin.lib.base import (Account,
                             VPC,
                             VpcOffering,
                             VirtualMachine,
                             ServiceOffering,
                             Network,
                             NetworkOffering,
                             Configurations,
                             Router)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template)
from marvin.sshClient import SshClient
from marvin.codes import PASS
import re
import time

class Services:

    """Test VPC services
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
            "domain_admin": {
                "email": "domain@admin.com",
                "firstname": "Domain",
                "lastname": "Admin",
                "username": "DoA",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,
                "memory": 128,
            },
            "network_offering": {
                "name": 'VPC Network offering',
                "displaytext": 'VPC Network off',
                "guestiptype": 'Isolated',
                "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,Lb,UserData,StaticNat,NetworkACL',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Vpn": 'VpcVirtualRouter',
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "Lb": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
            },
            "network_offering_no_lb": {
                "name": 'VPC Network offering',
                "displaytext": 'VPC Network off',
                "guestiptype": 'Isolated',
                "supportedservices": 'Vpn,Dhcp,Dns,SourceNat,PortForwarding,UserData,StaticNat,NetworkACL',
                "traffictype": 'GUEST',
                "availability": 'Optional',
                "useVpc": 'on',
                "serviceProviderList": {
                    "Vpn": 'VpcVirtualRouter',
                    "Dhcp": 'VpcVirtualRouter',
                    "Dns": 'VpcVirtualRouter',
                    "SourceNat": 'VpcVirtualRouter',
                    "PortForwarding": 'VpcVirtualRouter',
                    "UserData": 'VpcVirtualRouter',
                    "StaticNat": 'VpcVirtualRouter',
                    "NetworkACL": 'VpcVirtualRouter'
                },
            },
            "vpc_offering": {
                "name": 'VPC off',
                "displaytext": 'VPC off',
                "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Lb,UserData,StaticNat,NetworkACL',
            },
            "vpc": {
                "name": "TestVPC",
                "displaytext": "TestVPC",
                "cidr": '10.0.0.1/24'
            },
            "vpc_no_name": {
                "displaytext": "TestVPC",
                "cidr": '10.0.0.1/24'
            },
            "network": {
                "name": "Test Network",
                "displaytext": "Test Network",
                "netmask": '255.255.255.0'
            },
            "lbrule": {
                "name": "SSH",
                "alg": "leastconn",
                # Algorithm used for load balancing
                "privateport": 22,
                "publicport": 2222,
                "openfirewall": False,
                "startport": 22,
                "endport": 2222,
                "protocol": "TCP",
                "cidrlist": '0.0.0.0/0',
            },
            "natrule": {
                "privateport": 22,
                "publicport": 22,
                "startport": 22,
                "endport": 22,
                "protocol": "TCP",
                "cidrlist": '0.0.0.0/0',
            },
            "fw_rule": {
                "startport": 1,
                "endport": 6000,
                "cidr": '0.0.0.0/0',
                # Any network (For creating FW rule)
                "protocol": "TCP"
            },
            "icmp_rule": {
                "icmptype": -1,
                "icmpcode": -1,
                "cidrlist": '0.0.0.0/0',
                "protocol": "ICMP"
            },
            "virtual_machine": {
                "displayname": "Test VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "domain": {
                "name": "TestDomain"
            },
            "vpn_customer_gw": {
                "ipsecpsk": "s2svpn",
                "ikepolicy": "3des-md5;modp1536",
                "ikelifetime": "86400",
                "esppolicy": "3des-md5",
                "esplifetime": "3600",
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "sleep": 90,
            "timeout": 10,
            "mode": 'advanced'
        }


class TestVPC(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVPC, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.mgtSvrDetails = cls.config.__dict__["mgtSvr"][0].__dict__
        cls.unsupportedHypervisor = False
        if cls.hypervisor.lower() == 'hyperv':
            cls._cleanup = []
            cls.unsupportedHypervisor = True
            return
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.vpc_off = VpcOffering.create(
            cls.api_client,
            cls.services["vpc_offering"]
        )
        cls.vpc_off.update(cls.api_client, state='Enabled')
        cls._cleanup = [
            cls.service_offering,
        ]
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
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = []
        self.cleanup.insert(0, self.account)
        if self.unsupportedHypervisor:
            self.skipTest("not supported on %s" % self.hypervisor)
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.debug("Warning: Exception during cleanup : %s" % e)
        return

    def updateConfigurAndRestart(self, name, value):
        Configurations.update(self.apiclient, name, value)
        self.RestartServers()
        time.sleep(self.services["sleep"])

    def RestartServers(self):
        """ Restart management
        server and usage server """
        sshClient = SshClient(
            self.mgtSvrDetails["mgtSvrIp"],
            22,
            self.mgtSvrDetails["user"],
            self.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management restart"
        sshClient.execute(command)
        return

    @attr(tags=["advanced", "intervlan", "dvs", "test"], required_hardware="true")
    def test_01_create_tier_Vmxnet3(self):
        """
            Test to create vpc tier with nic type as Vmxnet3
            #1.Set global setting parameter "vmware.systemvm.nic.device.type"
            to "Vmxnet3"
            #2.Create VPC
            #3.Create one tier
            #4.Deploy one guest vm in the tier created in step3
        """
        if self.hypervisor.lower() not in ['vmware']:
            self.skipTest("This test can only run on vmware setup")

        nic_types = Configurations.list(
            self.apiclient,
            name="vmware.systemvm.nic.device.type"
        )
        self.assertEqual(validateList(nic_types)[0], PASS, "Invalid list config")
        nic_type = nic_types[0].value
        reset = False
        if nic_type.lower() != "vmxnet3":
            self.updateConfigurAndRestart("vmware.systemvm.nic.device.type", "Vmxnet3")
            reset = True

        self.services["vpc"]["cidr"] = "10.1.1.1/16"
        self.debug("creating a VPC network in the account: %s" %
                   self.account.name)
        try:
            vpc = VPC.create(
                self.apiclient,
                self.services["vpc"],
                vpcofferingid=self.vpc_off.id,
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
            )
            vpc_res = VPC.list(self.apiclient, id=vpc.id)
            self.assertEqual(validateList(vpc_res)[0], PASS, "Invalid response from listvpc")

            self.network_offering = NetworkOffering.create(
                self.apiclient,
                self.services["network_offering"],
                conservemode=False
            )
            # Enable Network offering
            self.network_offering.update(self.apiclient, state='Enabled')
            self.cleanup.append(self.network_offering)

            gateway = vpc.cidr.split('/')[0]
            # Split the cidr to retrieve gateway
            # for eg. cidr = 10.0.0.1/24
            # Gateway = 10.0.0.1
            # Creating network using the network offering created
            self.debug("Creating network with network offering: %s" %
                       self.network_offering.id)
            network = Network.create(
                self.apiclient,
                self.services["network"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                networkofferingid=self.network_offering.id,
                zoneid=self.zone.id,
                gateway=gateway,
                vpcid=vpc.id
            )
            self.debug("Created network with ID: %s" % network.id)
            vm = VirtualMachine.create(
                self.apiclient,
                self.services["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                networkids=[str(network.id)]
            )
            self.assertIsNotNone(vm, "VM creation failed")
            self.debug("Deployed VM in network: %s" % network.id)
            vm_res = VirtualMachine.list(self.apiclient, id=vm.id)
            self.assertEqual(
                validateList(vm_res)[0],
                PASS,
                "list vm returned invalid response"
            )
            vr_res = Router.list(
                self.apiclient,
                vpcid=vpc.id,
                listall="true"
            )
            self.assertEqual(validateList(vr_res)[0], PASS, "list vrs failed for vpc")
            vr_linklocal_ip = vr_res[0].linklocalip
            result = get_process_status(
                self.apiclient.connection.mgtSvr,
                22,
                self.apiclient.connection.user,
                self.apiclient.connection.passwd,
                vr_linklocal_ip,
                'lspci | grep "Ethernet controller"',
                hypervisor=self.hypervisor
            )
            self.assertEqual(
                validateList(result)[0],
                PASS,
                "We didn't find NICS with adapter type VMXNET3"
            )
            reg = re.compile("VMware VMXNET3")
            count = 0
            for line in result:
                if reg.search(line):
                    count += 1
            self.assertEqual(
                count,
                3,
                "Not all NICs on VR are of type VMXNET3"
            )
        except Exception as e:
            self.fail("NIC creation failed for vpc tier with systemvm nic \
                        adapter type as Vmxnet3: %s" % e)
        finally:
            if reset:
                self.updateConfigurAndRestart("vmware.systemvm.nic.device.type", nic_type)
        return
