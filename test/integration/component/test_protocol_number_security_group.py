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

"""
Tests protocol number support for security groups
"""

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import authorizeSecurityGroupIngress, revokeSecurityGroupIngress, authorizeSecurityGroupEgress, revokeSecurityGroupEgress
from marvin.sshClient import SshClient
from marvin.lib.utils import (validateList,
                              cleanup_resources,
                              get_host_credentials)
from marvin.lib.base import (Account,
                             Host,
                             Domain,
                             VirtualMachine,
                             ServiceOffering,
                             Zone,
                             SecurityGroup)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               list_hosts)
import logging

class TestProtocolNumberSecurityGroup(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestProtocolNumberSecurityGroup,
            cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.services = cls.testClient.getParsedTestDataConfig()

        zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls.zone = Zone(zone.__dict__)
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls._cleanup = []

        if str(cls.zone.securitygroupsenabled) != "True":
            sys.exit(1)

        cls.logger = logging.getLogger("TestProtocolNumberSecurityGroup")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.apiclient)
        testClient = super(TestProtocolNumberSecurityGroup, cls).getClsTestClient()
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        # Create new domain, account, network and VM
        cls.user_domain = Domain.create(
            cls.apiclient,
            services=cls.testdata["acl"]["domain2"],
            parentdomainid=cls.domain.id)

        # Create account
        cls.account = Account.create(
            cls.apiclient,
            cls.testdata["acl"]["accountD2"],
            admin=True,
            domainid=cls.user_domain.id
        )

        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.testdata["service_offering"]
        )

        cls.testdata["domainid"] = cls.domain.id
        cls.testdata["virtual_machine_userdata"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine_userdata"]["template"] = cls.template.id

        cls._cleanup.append(cls.service_offering)
        cls._cleanup.append(cls.account)
        cls._cleanup.append(cls.user_domain)

    @classmethod
    def tearDownClass(self):
        try:
            cleanup_resources(self.apiclient, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_01_add_valid_protocol_number(self):
        # Validate the following
        #
        # 1. Create a security group
        # 2. Try to add a new ingress rule by specifying a protocol number
        # 3. New rule should be added successfully
        # 4. Try to add a new egress rule by specifying a protocol number
        # 5. New rule should be added successfully

        self.security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % self.security_group.id)

        # Add ingress rule
        self.createIngressRule("111")

        # Add egress rule
        self.createEgressRule("111")

        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 1, 1)

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_02_add_invalid_protocol_number(self):
        # Validate the following
        #
        # 1. Create a security group
        # 2. Try to add a new ingress rule by specifying an invalid (> 255) protocol number
        # 3. Exception should be thrown successfully
        # 4. Try to add a new egreess rule by specifying an invalid (> 255) protocol number
        # 5. Exception should be thrown successfully

        self.security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % self.security_group.id)

        # Create ingress rule with invalid protocol number. Exception should be thrown
        with self.assertRaises(Exception):
            self.createIngressRule("555")

        # Create egress rule with invalid protocol number. Exception should be thrown
        with self.assertRaises(Exception):
            self.createEgressRule("555")

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_03_add_duplicate_protocol_number(self):
        # Validate the following
        #
        # 1. Create a security group
        # 2. Try to add a new ingress rule by specifying a protocol number
        # 3. Try to add one more ingress rule by specifying the same protocol number
        # 4. Exception should be thrown successfully
        # 5. Try to add a new egress rule by specifying a protocol number
        # 6. Try to add one more egress rule by specifying the same protocol number
        # 7. Exception should be thrown successfully

        self.security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % self.security_group.id)

        # Add ingress rule
        self.createIngressRule("111")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 1, 0)

        # Try to add another ingress with same protocol number. Exception is thrown
        with self.assertRaises(Exception):
            self.createIngressRule("111")

        # Add egress rule
        self.createEgressRule("111")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 1, 1)

        # Try to add another ingress with same protocol number. Exception is thrown
        with self.assertRaises(Exception):
            self.createEgressRule("111")

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_04_add_duplicate_protocol_number(self):
        # Validate the following
        #
        # 1. Create a security group
        # 2. Try to add a new ingress rule by using "all" as the protocol string
        # 3. Try to add one more ingress rule by specifying the same protocol
        # 4. Exception should be thrown successfully
        # 5. Try to add a new egress rule by using "all" as the protocol string
        # 6. Try to add one more egress rule by specifying the same protocol
        # 7. Exception should be thrown successfully

        self.security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % self.security_group.id)

        # Add ingress rule
        self.createIngressRule("111")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 1, 0)

        # Try to add another ingress with same protocol number. Exception is thrown
        with self.assertRaises(Exception):
            self.createIngressRule("111")

        # Add egress rule
        self.createEgressRule("111")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 1, 1)

        # Try to add another ingress with same protocol number. Exception is thrown
        with self.assertRaises(Exception):
            self.createEgressRule("111")

    @attr(tags=["advancedsg"], required_hardware="false")
    def test_05_invalid_protocol_string(self):
        # Validate the following
        #
        # 1. Create a security group
        # 2. Try to add ingress rule with invalid protocol name
        # 3. Exception should be thrown
        # 4. Try to add egressrule with invalid protocol name
        # 5. Exception should be thrown

        security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created security group with ID: %s" % security_group.id)

        with self.assertRaises(Exception):
            self.createIngressRule("randomprotocol")

        with self.assertRaises(Exception):
            self.createEgressRule("randomprotocol")

    @attr(tags=["advancedsg"])
    def test_06_create_virtual_machine(self):
        # Validate the following
        #
        # 1. Create a security group
        # 2. Create a virtual machine
        # 3. Try to add a new ingress rule
        # 4. Check if ingress rule is applied successfully on host
        # 5. Throw exception if it's not applied
        # 6. Try to add a new egress rule
        # 7. Check if egress rule is applied successfully on host
        # 8. Throw exception if it's not applied

        self.security_group = SecurityGroup.create(
            self.apiclient,
            self.testdata["security_group"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine_userdata"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            securitygroupids=[self.security_group.id]
        )

        # Get the virtual machine
        virtial_machine = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id
        )

        vm = virtial_machine[0]

        # get the host on which the vm is running
        hosts = list_hosts(
            self.apiclient,
            id=vm.hostid
        )

        host = hosts[0]
        if host.hypervisor.lower() not in "kvm":
            return

        host.user, host.passwd = get_host_credentials(self.config, host.ipaddress)

        # Add ingress rule
        self.createIngressRule("tcp", "1.1.1.1/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 1, 0)
        # Check if the ingress rule if applied successfully on host
        rule = "-A %s -s 1.1.1.1/32 -p tcp -m tcp --dport 1:65535 -m state --state NEW -j ACCEPT" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

        # Add ingress rule
        self.createIngressRule("udp", "2.2.2.2/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 2, 0)
        # Check if the ingress rule if applied successfully on host
        rule = "-A %s -s 2.2.2.2/32 -p udp -m udp --dport 1:65535 -m state --state NEW -j ACCEPT" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

        # Add ingress rule
        self.createIngressRule("icmp", "3.3.3.3/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 3, 0)
        # Check if the ingress rule if applied successfully on host
        rule = "-A %s -s 3.3.3.3/32 -p icmp -m icmp --icmp-type any -j ACCEPT" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

        # Add ingress rule
        self.createIngressRule("all", "4.4.4.4/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 4, 0)
        # Check if the ingress rule if applied successfully on host
        rule = "-A %s -s 4.4.4.4/32 -m state --state NEW -j ACCEPT" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

        # Add ingress rule
        self.createIngressRule("47", "5.5.5.5/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 5, 0)
        # Check if the ingress rule if applied successfully on host
        rule = "-A %s -s 5.5.5.5/32 -p gre -j ACCEPT" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

        # Add egress rule
        self.createEgressRule("tcp", "11.11.11.11/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 5, 1)
        # Check if the egress rule if applied successfully on host
        rule = "-A %s-eg -d 11.11.11.11/32 -p tcp -m tcp --dport 1:65535 -m state --state NEW -j RETURN" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

        # Add egress rule
        self.createEgressRule("udp", "12.12.12.12/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 5, 2)
        # Check if the egress rule if applied successfully on host
        rule = "-A %s-eg -d 12.12.12.12/32 -p udp -m udp --dport 1:65535 -m state --state NEW -j RETURN" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

        # Add egress rule
        self.createEgressRule("icmp", "13.13.13.13/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 5, 3)
        # Check if the egress rule if applied successfully on host
        rule = "-A %s-eg -d 13.13.13.13/32 -p icmp -m icmp --icmp-type any -j RETURN" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

        # Add egress rule
        self.createEgressRule("all", "14.14.14.14/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 5, 4)
        # Check if the egress rule if applied successfully on host
        rule = "-A %s-eg -d 14.14.14.14/32 -m state --state NEW -j RETURN" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

        # Add egress rule
        self.createEgressRule("47", "15.15.15.15/32")
        # verify number of ingress rules and egress rules
        self.verify_security_group_rules(self.security_group.id, 5, 5)
        # Check if the egress rule if applied successfully on host
        rule = "-A %s-eg -d 15.15.15.15/32 -p gre -j RETURN" % vm.instancename
        self.verify_rule_on_host(host.ipaddress, host.user, host.passwd, rule)

    def createIngressRule(self, protocol, cidrlist=None):
        cmd = authorizeSecurityGroupIngress.authorizeSecurityGroupIngressCmd()
        cmd.account=self.account.name
        cmd.domainid=self.account.domainid
        cmd.securitygroupid=self.security_group.id
        cmd.cidrlist="99.99.99.99/32"
        if cidrlist:
            cmd.cidrlist=cidrlist
        cmd.protocol=protocol
        if protocol == "tcp" or protocol == "udp":
            cmd.startport = 1
            cmd.endport = 65535
        elif protocol == "icmp":
            cmd.icmptype = -1
            cmd.icmpcode = -1
        self.apiclient.authorizeSecurityGroupIngress(cmd)
        cmd = None

    def createEgressRule(self, protocol, cidrlist=None):
        cmd = authorizeSecurityGroupEgress.authorizeSecurityGroupEgressCmd()
        cmd.account=self.account.name
        cmd.domainid=self.account.domainid
        cmd.securitygroupid=self.security_group.id
        cmd.cidrlist="88.88.88.88/32"
        if cidrlist:
            cmd.cidrlist=cidrlist
        cmd.protocol=protocol
        if protocol == "tcp" or protocol == "udp":
            cmd.startport = 1
            cmd.endport = 65535
        elif protocol == "icmp":
            cmd.icmptype = -1
            cmd.icmpcode = -1
        self.apiclient.authorizeSecurityGroupEgress(cmd)
        cmd = None

    def verify_security_group_rules(self, securitygroupid, numIngress, numEgress):
        security_groups = SecurityGroup.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            id=securitygroupid
        )
        ingressrule = security_groups[0].ingressrule
        if len(ingressrule) != numIngress:
            raise Exception("Failed to verify ingress rule for security group %s" % security_groups[0].name)
        egressrule = security_groups[0].egressrule
        if len(egressrule) != numEgress:
            raise Exception("Failed to verify egress rule for security group %s" % security_groups[0].name)

    def verify_rule_on_host(self, ipaddress, user, password, rule):
        self.logger.debug("Verifying rule '%s' in host %s" % (rule, ipaddress))
        try:
            ssh = SshClient(ipaddress, 22, user, password)
            result = ssh.execute("iptables-save |grep \"\\%s\"" % rule)
            if len(result) == 0 or result[0] != rule:
                raise Exception("Unable to apply security group rule")
        except KeyError:
            self.skipTest(
                "Provide a marvin config file with host credentials to run %s" % self._testMethodName)
