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

import os
import subprocess
import socket

from nose.plugins.attrib import attr

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account, ServiceOffering, VirtualMachine, NetworkOffering, Network
                             , SecurityGroup)
from marvin.cloudstackAPI import (authorizeSecurityGroupIngress
    , authorizeSecurityGroupEgress, revokeSecurityGroupIngress, revokeSecurityGroupEgress)
from marvin.lib.common import get_zone, get_template
from marvin.lib.decoratorGenerators import skipTestIf
from .common import cleanup_resources, not_tungsten_fabric_zone


class Services:
    """Test Tungsten Plugin
    """

    def __init__(self):
        self.services = {
            "network_offering": {
                "name": "TungstenSharedNetworkOffering",
                "displaytext": "TungstenSharedNetworkOffering",
                "guestiptype": "Shared",
                "traffictype": "GUEST",
                "supportedservices": [
                    "Dhcp",
                    "Dns",
                    "Connectivity",
                    "SecurityGroup"
                ],
                "serviceProviderList": {
                    "Dhcp": "Tungsten",
                    "Dns": "Tungsten",
                    "Connectivity": "Tungsten",
                    "SecurityGroup": "Tungsten"
                },
                "useTungsten": "on",
                "specifyVlan": "true",
                "specifyIpRanges": "true"
            },
            "networks": {
                "network1": {
                    "name": "TF-Network1",
                    "displaytext": "TF-Network1",
                    "gateway": "192.168.6.1",
                    "netmask": "255.255.255.0",
                    "startip": "192.168.6.100",
                    "endip": "192.168.6.200",
                    "vlan": "untagged"
                }
            },
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                "password": "password",
            },
            "virtual_machines": {
                "vm1": {
                    "name": "vm1",
                    "displayname": "vm1",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'KVM',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP'

                },
                "vm2": {
                    "name": "vm2",
                    "displayname": "vm2",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'KVM',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP'
                },
                "vm3": {
                    "name": "vm3",
                    "displayname": "vm3",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'KVM',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP'
                }
            },
            "security_groups": {
                "security_group1": {
                    "name": "sg1"
                },
                "security_group2": {
                    "name": "sg2"
                },
                "security_group3": {
                    "name": "sg3"
                }
            },
            "ostype": 'CentOS 5.6 (64-bit)'
        }


class TestSharedZone(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.api_client = cls.clstestclient.getApiClient()
        cls.services = Services().services
        cls.test_data = cls.clstestclient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.api_client, cls.clstestclient.getZoneForTests())
        cls.template = get_template(cls.api_client, cls.zone.id, cls.test_data["ostype"])
        cls.not_tungsten_fabric_zone = not_tungsten_fabric_zone(cls.api_client, cls.zone.id)
        cls._cleanup = []

    @classmethod
    def tearDownClass(self):
        try:
            cleanup_resources(self.api_client, self.zone.id, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.account = Account.create(self.api_client, self.services["account"], admin=True)
        self.service_offering = ServiceOffering.create(self.api_client, self.test_data["service_offering"])
        self.cleanup = [self.account, self.service_offering]
        return

    def tearDown(self):
        try:
            cleanup_resources(self.api_client, self.zone.id, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def getLocalMachineIpAddress(self):
        """ Get IP address of the machine on which test case is running """
        socket_ = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        socket_.connect(('8.8.8.8', 0))
        return socket_.getsockname()[0]

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_test_default_security_group_rule(self):

        # Validate the following:
        # 1. deploy vm
        # 2. ping vm from test machine ( should fail as default security group rule )
        # 3. create icmp ingress security group rule with test machine ip address cidr
        # 4. ping vm from test machine again ( should success as icmp ingress security group rule)
        # 5. create ssh ingress security group rule with test machine ip address cidr
        # 6. ssh to vm from test machine
        # 7. ping google from vm ( should success as default security group rule )

        network_offering = NetworkOffering.create(self.api_client, self.services["network_offering"])
        network_offering.update(self.api_client, state='Enabled')
        self.cleanup.append(network_offering)

        # create network
        network = Network.create(
            self.api_client,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # create security group
        security_group = SecurityGroup.create(
            self.api_client,
            self.services["security_groups"]["security_group1"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(security_group)

        # create virtual machine
        vm = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[network.id],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            securitygroupids=[security_group.id],
            mode='advanced'
        )
        self.cleanup.append(vm)

        # ping vm from test machine
        if os.name == 'nt':
            result = subprocess.call(
                ['ping', '-n', '1', vm.ipaddress])
        else:
            result = subprocess.call(
                ['ping', '-c', '1', vm.ipaddress])

        self.assertEqual(
            result,
            1,
            "Ping should be fail as default security group deny all incoming traffic"
        )

        test_machine_ip_address = self.getLocalMachineIpAddress()
        cidr = test_machine_ip_address + "/32"

        # create icmp ingress security group rule with test machine ip address cidr
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group.id
        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.cidrlist = cidr
        icmp_ingress_rule = self.api_client.authorizeSecurityGroupIngress(cmd)

        # ping vm from test machine again
        if os.name == 'nt':
            result = subprocess.call(
                ['ping', '-n', '1', vm.ipaddress])
        else:
            result = subprocess.call(
                ['ping', '-c', '1', vm.ipaddress])

        self.assertEqual(
            result,
            0,
            "Ping should be successful"
        )

        # create ssh ingress security group rule with test machine ip address cidr
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = cidr
        ssh_ingress_rule = self.api_client.authorizeSecurityGroupIngress(cmd)

        # ssh to vm from test machine
        ssh = vm.get_ssh_client()

        # ping google from vm
        res = ssh.execute("ping -c1 google.com")
        self.assertEqual(
            str(res).count("1 received"),
            1,
            "Ping google.com from vm should be successful "
            "as default security group permit all outcoming traffic"
        )

        # revoke icmp ingress security group rule
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = icmp_ingress_rule.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

        # revoke ssh ingress security group rule
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = ssh_ingress_rule.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_test_ingress_rule_security_group_with_cidr(self):

        # Validate the following:
        # 1. deploy 2 vm : vm1 and vm2
        # 2. create ssh ingress security group rule with test machine ip address cidr
        # 3. ssh to vm1
        # 4. ssh to vm2 from vm1 ( should fail as ssh ingress security group rule )
        # 5. create ssh ingress security group rule with vm1 ip address cidr
        # 4. ssh to vm2 from vm1 again ( should success as ssh ingress security group rule )

        network_offering = NetworkOffering.create(self.api_client, self.services["network_offering"])
        network_offering.update(self.api_client, state='Enabled')
        self.cleanup.append(network_offering)

        # create network
        network = Network.create(
            self.api_client,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # create security group
        security_group = SecurityGroup.create(
            self.api_client,
            self.services["security_groups"]["security_group1"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(security_group)

        # create virtual machine 1
        vm1 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            securitygroupids=[security_group.id],
            mode='advanced'
        )
        self.cleanup.append(vm1)

        # create virtual machine 2
        vm2 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machines"]["vm2"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            securitygroupids=[security_group.id],
            mode='advanced'
        )
        self.cleanup.append(vm2)

        # create ssh ingress security group rule with test machine ip address cidr
        test_machine_ip_address = self.getLocalMachineIpAddress()
        cidr = test_machine_ip_address + "/32"
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = cidr
        ingress_rule1 = self.api_client.authorizeSecurityGroupIngress(cmd)

        ssh = vm1.get_ssh_client()
        res1 = ssh.execute("ssh %s@%s -v" % (vm2.username, vm2.ssh_ip))
        self.debug("Response is :%s" % res1)
        self.assertFalse("connection established" in str(res1).lower(), "SSH to VM 2 should fail")

        # create ssh ingress security group rule with vm1 ip address cidr
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = vm1.ipaddress + "/32"
        ingress_rule2 = self.api_client.authorizeSecurityGroupIngress(cmd)

        res2 = ssh.execute("ssh %s@%s -v" % (vm2.username, vm2.ssh_ip))
        self.debug("Response is :%s" % res2)
        self.assertTrue("connection established" in str(res2).lower(), "SSH to VM 2 should success")

        # revoke ssh ingress security group rule 1
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = ingress_rule1.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

        # revoke ssh ingress security group rule 2
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = ingress_rule2.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_test_ingress_rule_security_group_with_account(self):

        # Validate the following:
        # 1. create network 1
        # 2. create security group 1
        # 3. create security group 2
        # 4. deploy vm 1
        # 5. deploy vm 2
        # 6. create ssh ingress security group 1 rule with test machine ip address cidr
        # 7. ssh to vm1 from test machine
        # 8. ping vm2 from vm1 ( should fail )
        # 9. create icmp ingress security group 2 rule with account and security group 1
        # 10. ping vm2 from vm1 again ( should success )
        # 11. create ssh ingress security group 2 rule with test machine ip address cidr
        # 12. ssh to vm2 from test machine
        # 13. ping vm1 from vm2 ( should fail )
        # 14. create icmp ingress security group 1 rule with account and security group 2
        # 15. ping vm1 from vm2 again ( should success )
        # 16. revoke ssh ingress security group rule 1
        # 17. revoke ssh ingress security group rule 2
        # 18. revoke icmp ingress security group rule 1
        # 19. revoke icmp ingress security group rule 2

        network_offering = NetworkOffering.create(self.api_client, self.services["network_offering"])
        network_offering.update(self.api_client, state='Enabled')
        self.cleanup.append(network_offering)

        # 1. create network 1
        network = Network.create(
            self.api_client,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # 2. create security group 1
        security_group1 = SecurityGroup.create(
            self.api_client,
            self.services["security_groups"]["security_group1"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(security_group1)

        # 3. create security group 2
        security_group2 = SecurityGroup.create(
            self.api_client,
            self.services["security_groups"]["security_group2"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(security_group2)

        # 4. deploy vm 1
        vm1 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            securitygroupids=[security_group1.id],
            mode='advanced'
        )
        self.cleanup.append(vm1)

        # 5. deploy vm 2
        vm2 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machines"]["vm2"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            securitygroupids=[security_group2.id],
            mode='advanced'
        )
        self.cleanup.append(vm2)

        # 6. create ssh ingress security group 1 rule with test machine ip address cidr
        test_machine_ip_address = self.getLocalMachineIpAddress()
        cidr = test_machine_ip_address + "/32"
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group1.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = cidr
        ssh_ingress_rule1 = self.api_client.authorizeSecurityGroupIngress(cmd)

        # 7. ssh to vm1 from test machine
        ssh1 = vm1.get_ssh_client()

        # 8. ping vm2 from vm1 ( should fail )
        res1 = ssh1.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res1).count("0 received"),
            1,
            "Ping vm2 from vm1 should be fail"
        )

        # 9. create icmp ingress security group 2 rule with account and security group 1
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group2.id
        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.usersecuritygrouplist = [{'account':str(self.account.name),'group': str(security_group1.name)}]
        icmp_ingress_rule1 = self.api_client.authorizeSecurityGroupIngress(cmd)

        # 10. ping vm2 from vm1 again ( should success )
        res2 = ssh1.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res2).count("1 received"),
            1,
            "Ping vm2 from vm1 should be success"
        )

        # 11. create ssh ingress security group 2 rule with test machine ip address cidr
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group2.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = cidr
        ssh_ingress_rule2 = self.api_client.authorizeSecurityGroupIngress(cmd)

        # 12. ssh to vm2 from test machine
        ssh2 = vm2.get_ssh_client()

        # 13. ping vm1 from vm2 ( should fail )
        res3 = ssh2.execute("ping -c1 %s" % vm1.ipaddress)
        self.assertEqual(
            str(res3).count("0 received"),
            1,
            "Ping vm1 from vm2 should be fail"
        )

        # 14. create icmp ingress security group 1 rule with account and security group 2
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group1.id
        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.usersecuritygrouplist = [{'account':str(self.account.name),'group': str(security_group2.name)}]
        icmp_ingress_rule2 = self.api_client.authorizeSecurityGroupIngress(cmd)

        # 15. ping vm1 from vm2 again ( should success )
        res4 = ssh2.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res4).count("1 received"),
            1,
            "Ping vm1 from vm2 should be success"
        )

        # 16. revoke ssh ingress security group rule 1
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = ssh_ingress_rule1.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

        # 17. revoke ssh ingress security group rule 2
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = ssh_ingress_rule2.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

        # 18. revoke icmp ingress security group rule 1
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = icmp_ingress_rule1.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

        # 19. revoke icmp ingress security group rule 2
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = icmp_ingress_rule2.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_test_egress_rule_security_group_with_cidr(self):

        # Validate the following:
        # 1. create network offering
        # 2. create network 1
        # 3. create security group 1
        # 4. deploy vm1
        # 5. create ssh ingress security group 1 rule with test machine ip address cidr
        # 6. ssh to vm1 from test machine
        # 7. ping 8.8.8.8 from vm1 ( should success )
        # 8. ping 4.4.4.4 from vm1 ( should success )
        # 9. create icmp egress security group 1 rule with 8.8.8.8/32 cidr
        # 10. ping 8.8.8.8 from vm 1 ( should success )
        # 11. ping 8.8.4.4 from vm1 ( should fail )
        # 12. create icmp egress security group 1 rule with 8.8.4.4/32 cidr
        # 13. ping 8.8.4.4 from vm1 ( should success )

        # 1. create network offering
        network_offering = NetworkOffering.create(
            self.api_client,
            self.services["network_offering"]
        )
        network_offering.update(self.api_client, state='Enabled')
        self.cleanup.append(network_offering)

        # 2. create network 1
        network = Network.create(
            self.api_client,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # 3. create security group 1
        security_group1 = SecurityGroup.create(
            self.api_client,
            self.services["security_groups"]["security_group1"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(security_group1)

        # 4. deploy vm 1
        vm1 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            securitygroupids=[security_group1.id],
            mode='advanced'
        )
        self.cleanup.append(vm1)

        # 5. create ssh ingress security group 1 rule with test machine ip address cidr
        test_machine_ip_address = self.getLocalMachineIpAddress()
        cidr = test_machine_ip_address + "/32"
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group1.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = cidr
        ssh_ingress_rule1 = self.api_client.authorizeSecurityGroupIngress(cmd)

        # 6. ssh to vm1 from test machine
        ssh1 = vm1.get_ssh_client()

        # 7. ping 8.8.8.8 from vm 1 ( should success )
        res1 = ssh1.execute("ping -c1 8.8.8.8")
        self.assertEqual(
            str(res1).count("1 received"),
            1,
            "Ping 8.8.8.8 from vm1 should be success"
        )

        # 8. ping 8.8.4.4 from vm1 ( should success )
        res2 = ssh1.execute("ping -c1 8.8.4.4")
        self.assertEqual(
            str(res2).count("1 received"),
            1,
            "Ping 8.8.4.4 from vm1 should be success"
        )

        # 9. create icmp egress security group 1 rule with 8.8.8.8/32 cidr
        cmd = authorizeSecurityGroupEgress.authorizeSecurityGroupEgressCmd()
        cmd.securitygroupid = security_group1.id
        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.cidrlist = '8.8.8.8/32'
        icmp_egress_rule1 = self.api_client.authorizeSecurityGroupEgress(cmd)

        # 10. ping 8.8.8.8 from vm 1 ( should success )
        res3 = ssh1.execute("ping -c1 8.8.8.8")
        self.assertEqual(
            str(res3).count("1 received"),
            1,
            "Ping 8.8.8.8 from vm1 should be success"
        )

        # 11. ping 8.8.4.4 from vm1 ( should fail )
        res4 = ssh1.execute("ping -c1 8.8.4.4")
        self.assertEqual(
            str(res4).count("0 received"),
            1,
            "Ping 8.8.4.4 from vm1 should be fail"
        )

        # 12. create icmp egress security group 1 rule with 8.8.4.4 cidr
        cmd = authorizeSecurityGroupEgress.authorizeSecurityGroupEgressCmd()
        cmd.securitygroupid = security_group1.id
        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.cidrlist = "8.8.4.4/32"
        icmp_egress_rule2 = self.api_client.authorizeSecurityGroupEgress(cmd)

        # 13. ping vm2 from vm1 ( should success )
        res5 = ssh1.execute("ping -c1 8.8.4.4")
        self.assertEqual(
            str(res5).count("1 received"),
            1,
            "Ping 8.8.4.4 from vm1 should be success"
        )

        # 14. revoke ssh ingress security group rule 1
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = ssh_ingress_rule1.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

        # 15. revoke icmp egress security group rule 1
        cmd = revokeSecurityGroupEgress.revokeSecurityGroupEgressCmd()
        cmd.id = icmp_egress_rule1.egressrule[0].ruleid
        self.api_client.revokeSecurityGroupEgress(cmd)

        # 16. revoke icmp egress security group rule 2
        cmd = revokeSecurityGroupEgress.revokeSecurityGroupEgressCmd()
        cmd.id = icmp_egress_rule2.egressrule[0].ruleid
        self.api_client.revokeSecurityGroupEgress(cmd)

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_test_egress_rule_security_group_with_account(self):

        # Validate the following:
        # 1. create network offering
        # 2. create network 1
        # 3. create security group 1
        # 4. create security group 2
        # 5. create security group 3
        # 6. deploy vm1
        # 7. deploy vm2
        # 8. deploy vm3
        # 9. create ssh ingress security group 1 rule with test machine ip address cidr
        # 10. create icmp ingress security group 2 rule with security group 1
        # 11. create icmp ingress security group 3 rule with security group 1
        # 12. ssh to vm1 from test machine
        # 13. ping vm2 from vm1 ( should success )
        # 14. ping vm3 from vm1 ( should success )
        # 15. create icmp egress security group 1 rule with security group 2
        # 16. ping vm2 from vm1 ( should success )
        # 17. ping vm3 from vm1 ( should fail )
        # 18. create icmp egress security group 1 rule with security group 3
        # 19. ping vm2 from vm1 ( should success )
        # 20. ping vm3 from vm1 ( should success )
        # 21. revoke ssh ingress security group rule 1
        # 22. revoke icmp ingress security group rule 1
        # 23. revoke icmp ingress security group rule 2
        # 24. revoke icmp egress security group rule 1
        # 25. revoke icmp egress security group rule 2

        # 1. create network offering
        network_offering = NetworkOffering.create(
            self.api_client,
            self.services["network_offering"]
        )
        network_offering.update(self.api_client, state='Enabled')
        self.cleanup.append(network_offering)

        # 2. create network 1
        network = Network.create(
            self.api_client,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # 3. create security group 1
        security_group1 = SecurityGroup.create(
            self.api_client,
            self.services["security_groups"]["security_group1"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(security_group1)

        # 4. create security group 2
        security_group2 = SecurityGroup.create(
            self.api_client,
            self.services["security_groups"]["security_group2"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(security_group2)

        # 5. create security group 3
        security_group3 = SecurityGroup.create(
            self.api_client,
            self.services["security_groups"]["security_group3"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(security_group3)

        # 6. deploy vm1
        vm1 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            securitygroupids=[security_group1.id],
            mode='advanced'
        )
        self.cleanup.append(vm1)

        # 7. deploy vm2
        vm2 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machines"]["vm2"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            securitygroupids=[security_group2.id],
            mode='advanced'
        )
        self.cleanup.append(vm2)

        # 8. deploy vm3
        vm3 = VirtualMachine.create(
            self.api_client,
            self.services["virtual_machines"]["vm3"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            securitygroupids=[security_group3.id],
            mode='advanced'
        )
        self.cleanup.append(vm3)

        # 9. create ssh ingress security group 1 rule with test machine ip address cidr
        test_machine_ip_address = self.getLocalMachineIpAddress()
        cidr = test_machine_ip_address + "/32"
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group1.id
        cmd.protocol = 'TCP'
        cmd.startport = 22
        cmd.endport = 22
        cmd.cidrlist = cidr
        ssh_ingress_rule1 = self.api_client.authorizeSecurityGroupIngress(cmd)

        # 10. create icmp ingress security group 2 rule with security group 1
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group2.id
        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.usersecuritygrouplist = [{'account':str(self.account.name),'group': str(security_group1.name)}]
        icmp_ingress_rule1 = self.api_client.authorizeSecurityGroupIngress(cmd)

        # 11. create icmp ingress security group 3 rule with security group 1
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.securitygroupid = security_group3.id
        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.usersecuritygrouplist = [{'account':str(self.account.name),'group': str(security_group1.name)}]
        icmp_ingress_rule2 = self.api_client.authorizeSecurityGroupIngress(cmd)

        # 12. ssh to vm1 from test machine
        ssh1 = vm1.get_ssh_client()

        # 13. ping vm2 from vm1 ( should success )
        res1 = ssh1.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res1).count("1 received"),
            1,
            "Ping vm2 from vm1 should be success"
        )

        # 14. ping vm3 from vm1 ( should success )
        res2 = ssh1.execute("ping -c1 %s" % vm3.ipaddress)
        self.assertEqual(
            str(res2).count("1 received"),
            1,
            "Ping vm3 from vm1 should be success"
        )

        # 15. create icmp egress security group 1 rule with security group 2
        cmd = authorizeSecurityGroupEgress.authorizeSecurityGroupEgressCmd()
        cmd.securitygroupid = security_group1.id
        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.usersecuritygrouplist = [{'account':str(self.account.name),'group': str(security_group2.name)}]
        icmp_egress_rule1 = self.api_client.authorizeSecurityGroupEgress(cmd)

        # 16. ping vm2 from vm1 ( should success )
        res3 = ssh1.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res3).count("1 received"),
            1,
            "Ping vm2 from vm1 should be success"
        )

        # 17. ping vm3 from vm1 ( should fail )
        res4 = ssh1.execute("ping -c1 %s" % vm3.ipaddress)
        self.assertEqual(
            str(res4).count("0 received"),
            1,
            "Ping vm3 from vm1 should be fail"
        )

        # 18. create icmp egress security group 1 rule with security group 3
        cmd = authorizeSecurityGroupEgress.authorizeSecurityGroupEgressCmd()
        cmd.securitygroupid = security_group1.id
        cmd.protocol = 'ICMP'
        cmd.icmptype = "-1"
        cmd.icmpcode = "-1"
        cmd.usersecuritygrouplist = [{'account':str(self.account.name),'group': str(security_group3.name)}]
        icmp_egress_rule2 = self.api_client.authorizeSecurityGroupEgress(cmd)

        # 19. ping vm2 from vm1 ( should success )
        res5 = ssh1.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res5).count("1 received"),
            1,
            "Ping vm2 from vm1 should be success"
        )

        # 20. ping vm3 from vm1 ( should success )
        res6 = ssh1.execute("ping -c1 %s" % vm3.ipaddress)
        self.assertEqual(
            str(res6).count("1 received"),
            1,
            "Ping vm3 from vm1 should be success"
        )

        # 21. revoke ssh ingress security group rule 1
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = ssh_ingress_rule1.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

        # 22. revoke icmp ingress security group rule 1
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = icmp_ingress_rule1.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

        # 23. revoke icmp ingress security group rule 2
        cmd = revokeSecurityGroupIngress.revokeSecurityGroupIngressCmd()
        cmd.id = icmp_ingress_rule2.ingressrule[0].ruleid
        self.api_client.revokeSecurityGroupIngress(cmd)

        # 24. revoke icmp egress security group rule 1
        cmd = revokeSecurityGroupEgress.revokeSecurityGroupEgressCmd()
        cmd.id = icmp_egress_rule1.egressrule[0].ruleid
        self.api_client.revokeSecurityGroupEgress(cmd)

        # 25. revoke icmp egress security group rule 2
        cmd = revokeSecurityGroupEgress.revokeSecurityGroupEgressCmd()
        cmd.id = icmp_egress_rule2.egressrule[0].ruleid
        self.api_client.revokeSecurityGroupEgress(cmd)
