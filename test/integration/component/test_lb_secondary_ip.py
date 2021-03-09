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
    P1 tests for Load balancing rules for secondary IP feature, this is an
    enhancement to the Multiple IPs per NIC feature

    Test Plan: https://cwiki.apache.org/confluence/display/CLOUDSTACK/
    TestPlan+%3AConfiguring+load+balancing+rules+for+VM+nic+secondary+ips

    Issue Link: https://issues.apache.org/jira/browse/CLOUDSTACK-4840

    Feature Specifications: https://cwiki.apache.org/confluence/display/
    CLOUDSTACK/Configuring+load+balancing+rules+for+VM+nic+secondary+ips
"""

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.lib.base import (
    Account,
    ServiceOffering,
    VirtualMachine,
    LoadBalancerRule,
    NIC,
    PublicIPAddress,
    Router,
    NetworkOffering,
    Network,
    FireWallRule,
    NATRule
)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template,
                               verifyNetworkState,
                               wait_for_cleanup,
                               add_netscaler
                               )

from marvin.lib.utils import (validateList,
                              random_gen,
                              cleanup_resources)
from marvin.cloudstackAPI import (restartNetwork)
from marvin.sshClient import SshClient
from marvin.codes import PASS


def IsIpAssignedToLoadBalancerRule(self, lbrule, iplist):
    """List Load Balancer Rule Instances and verify that
    IP is present in the list"""

    exceptionMessage = ""
    isIpAssignedToLbRule = True
    try:
        lbruleinstances = LoadBalancerRule.listLoadBalancerRuleInstances(
            self.apiclient,
            id=lbrule.id,
            lbvmips=True
        )

        self.assertEqual(validateList(lbruleinstances)[0], PASS,
                         "lbruleinstances list validation failed")

        lbruleips = [str(ipaddress) for lbruleinstance in lbruleinstances
                     for ipaddress in lbruleinstance.lbvmipaddresses]

        for ip in iplist:
            self.assertTrue(ip in lbruleips,
                            "IP address in lbruleinstances list not matching\
                          with ips assigned to lb rule")
    except Exception as e:
        isIpAssignedToLbRule = False
        exceptionMessage = e
    return [isIpAssignedToLbRule, exceptionMessage]


class TestAssignLBRule(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestAssignLBRule,
                                     cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()

        cls.testdata = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())

        template = get_template(cls.api_client,
                                cls.zone.id,
                                cls.testdata["ostype"])
        # Set Zones and disk offerings
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        cls._cleanup = []
        try:
            cls.hypervisor = cloudstackTestClient.getHypervisorInfo()
            # As Hyperv is GUI based VM, it requires more resources to be
            # able to SSH properly to it
            if cls.hypervisor.lower() == 'hyperv':
                cls.testdata["service_offering"]["name"] = "Medium Instance"
                cls.testdata["service_offering"]["memory"] = "1024"
                cls.testdata["service_offering"]["cpuspeed"] = "1024"

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"])
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []
        try:
            self.account = Account.create(self.apiclient,
                                          self.testdata["account"],
                                          domainid=self.domain.id)
            self.cleanup.append(self.account)
            self.virtual_machine = VirtualMachine.create(
                self.api_client,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.zone.networktype)
        except Exception as e:
            self.tearDown()
            raise e

    def tearDown(self):
        try:
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_01_lb_rule_for_primary_ip(self):
        """Create LB rule for primary IP

        steps:
        # 1. Add secondary IP to the VM
        # 2. Associate a public IP for the network
        # 3. Create load balancer rule for this IP
        # 4. Assign load balancer rule for primary ip address of VM
        # 5. List and verify that load balancer rule is successfully added
        """

        NIC.addIp(self.apiclient, id=self.virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)}]

        lb_rule.assign(
            self.apiclient,
            vmidipmap=vmidipmap
        )

        response = IsIpAssignedToLoadBalancerRule(
            self, lb_rule, [
                self.virtual_machine.nic[0].ipaddress])
        self.assertTrue(response[0], response[1])

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(validateList(lbrules)[0], PASS,
                         "lb rules list validation failed")

        self.assertEqual(lb_rule.id, lbrules[0].id, "LB rules does not match")
        return

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_02_lb_rule_for_primary_ip(self):
        """Create LB rule for secondary IP

        steps:
        # 1. Add secondary IP to the VM
        # 2. Associate a public IP for the network
        # 3. Create load balancer rule for this IP
        # 4. Assign load balancer rule for secondary ip address of VM
        # 5. List and verify that load balancer rule is successfully added"""

        secondaryip = NIC.addIp(
            self.apiclient,
            id=self.virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(secondaryip.ipaddress)}]

        lb_rule.assign(
            self.apiclient,
            vmidipmap=vmidipmap
        )

        response = IsIpAssignedToLoadBalancerRule(self, lb_rule,
                                                  [secondaryip.ipaddress])
        self.assertTrue(response[0], response[1])

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(validateList(lbrules)[0], PASS,
                         "lb rules list validation failed")

        self.assertEqual(lb_rule.id, lbrules[0].id, "LB rules does not match")
        return

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_03_lb_rule_for_primary_and_secondary_ip(self):
        """Create LB rule for primary and secondary IP

        steps:
        # 1. Add secondary IP to the VM
        # 2. Associate a public IP for the network
        # 3. Create load balancer rule for this IP
        # 4. Assign load balancer rule to both primary
        #    and secondary ip address of VM
        # 5. List and verify that load balancer rule is successfully added"""

        secondaryip = NIC.addIp(
            self.apiclient,
            id=self.virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)},
                     {"vmid": str(self.virtual_machine.id),
                      "vmip": str(secondaryip.ipaddress)}]

        lb_rule.assign(
            self.apiclient,
            vmidipmap=vmidipmap
        )

        response = IsIpAssignedToLoadBalancerRule(
            self, lb_rule, [
                self.virtual_machine.nic[0].ipaddress, secondaryip.ipaddress])
        self.assertTrue(response[0], response[1])

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(validateList(lbrules)[0], PASS,
                         "lb rules list validation failed")

        self.assertEqual(lb_rule.id, lbrules[0].id, "LB rules does not match")
        return

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_04_lb_rule_primary_secondary_multiple_vms(self):
        """Create LB rule for primary and secondary IPs of multiple VMs

        steps:
        # 1. Deploy two VMs in same network and add secondary IP for both
        # 2. Associate a public IP for the network
        # 3. Create load balancer rule for this IP
        # 4. Assign load balancer rule to both primary and
        #    secondary ip address of both VMs
        # 5. List and verify that load balancer rule is successfully added"""

        secondaryip_vm1 = NIC.addIp(self.apiclient,
                                    id=self.virtual_machine.nic[0].id
                                    )

        self.virtual_machine2 = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.zone.networktype,
            networkids=[self.virtual_machine.nic[0].networkid, ])

        secondaryip_vm2 = NIC.addIp(self.apiclient,
                                    id=self.virtual_machine2.nic[0].id
                                    )

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)},
                     {"vmid": str(self.virtual_machine.id),
                      "vmip": str(secondaryip_vm1.ipaddress)},
                     {"vmid": str(self.virtual_machine2.id),
                      "vmip": str(self.virtual_machine2.nic[0].ipaddress)},
                     {"vmid": str(self.virtual_machine2.id),
                      "vmip": str(secondaryip_vm2.ipaddress)}]

        lb_rule.assign(
            self.apiclient,
            vmidipmap=vmidipmap
        )

        iplist = [self.virtual_machine.nic[0].ipaddress,
                  secondaryip_vm1.ipaddress,
                  self.virtual_machine2.nic[0].ipaddress,
                  secondaryip_vm2.ipaddress]

        response = IsIpAssignedToLoadBalancerRule(self, lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)

        self.assertEqual(validateList(lbrules)[0], PASS,
                         "lb rules list validation failed")

        self.assertEqual(lb_rule.id, lbrules[0].id, "LB rules does not match")

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine2.id,
            listall=True)

        self.assertEqual(validateList(lbrules)[0], PASS,
                         "lb rules list validation failed")

        self.assertEqual(lb_rule.id, lbrules[0].id, "LB rules does not match")
        return


class TestFailureScenarios(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestFailureScenarios,
                                     cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.testdata = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())

        template = get_template(cls.api_client,
                                cls.zone.id,
                                cls.testdata["ostype"])
        # Set Zones and disk offerings
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        cls._cleanup = []

        try:
            cls.hypervisor = cloudstackTestClient.getHypervisorInfo()
            # As Hyperv is GUI based VM, it requires more resources to be
            # able to SSH properly to it
            if cls.hypervisor.lower() == 'hyperv':
                cls.testdata["service_offering"]["name"] = "Medium Instance"
                cls.testdata["service_offering"]["memory"] = "1024"
                cls.testdata["service_offering"]["cpuspeed"] = "1024"

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"])
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

        try:
            self.account = Account.create(self.apiclient,
                                          self.testdata["account"],
                                          domainid=self.domain.id)
            self.cleanup.append(self.account)
            self.virtual_machine = VirtualMachine.create(
                self.api_client,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.zone.networktype)
        except Exception as e:
            self.tearDown()
            raise e

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_05_lb_rule_wrong_vm_id(self):
        """Try to assign LB rule to secondary IP by giving wrong vm id

        steps:
        # 1. Add secondary IP for VM
        # 2. Associate a public IP for the network
        # 3. Create load balancer rule for this IP
        # 4. Try to assign load balancer rule for
        #    secondary IP of VM by specifying invalid vm id, it should fail"""

        secondaryip = NIC.addIp(
            self.apiclient,
            id=self.virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id) + random_gen(),
                      "vmip": str(secondaryip.ipaddress)}]

        with self.assertRaises(Exception):
            lb_rule.assign(self.apiclient,
                           vmidipmap=vmidipmap)
        return

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_06_lb_rule_wrong_vm_ip(self):
        """Try to assign LB rule to secondary IP by giving wrong ip

        steps:
        # 1. Add secondary IP for VM
        # 2. Associate a public IP for the network
        # 3. Create load balancer rule for this IP
        # 4. Try to assign load balancer rule by specifying
        #    wrong ip and correct vm id, the task should fail"""

        secondaryip = NIC.addIp(
            self.apiclient,
            id=self.virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(secondaryip.ipaddress) + random_gen()}]

        with self.assertRaises(Exception):
            lb_rule.assign(self.apiclient,
                           vmidipmap=vmidipmap)
        return

    @unittest.skip("Failing-WIP")
    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_07_lb_rule_used_ip(self):
        """Try to assign secondary IP to lb rule which is already assigned to another
           LB rule

        steps:
        # 1. Add secondary IP for VM
        # 2. Associate two public IPs for the network
        # 3. Create load balancer rule for both the IPs
        # 4. Assign load balancer rule 1 to secondary IP
        # 5. Try to assign load balancer rule 2 to secondary IP, the operation
             should fail"""

        secondaryip = NIC.addIp(
            self.apiclient,
            id=self.virtual_machine.nic[0].id)

        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule1 = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip1.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        public_ip2 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule2 = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip2.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(secondaryip.ipaddress)}]

        lb_rule1.assign(self.apiclient,
                        vmidipmap=vmidipmap)

        iplist = [secondaryip.ipaddress]

        response = IsIpAssignedToLoadBalancerRule(self, lb_rule1, iplist)
        self.assertTrue(response[0], response[1])

        with self.assertRaises(Exception):
            lb_rule2.assign(self.apiclient,
                            vmidipmap=vmidipmap)
        return

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_08_lb_rule_remove_used_ip(self):
        """Try to remove secondary IP with load balancer rule configured for it

        steps:
        # 1. Add secondary IP for VM
        # 2. Associate public IP for the network
        # 3. Create load balancer rule for IP
        # 4. Assign load balancer rule to secondary IP
        # 5. Try to remove the secondary IP form NIC,
             the operation should fail"""

        secondaryip = NIC.addIp(
            self.apiclient,
            id=self.virtual_machine.nic[0].id)

        public_ip1 = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule1 = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip1.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(secondaryip.ipaddress)}]

        lb_rule1.assign(self.apiclient,
                        vmidipmap=vmidipmap)

        iplist = [secondaryip.ipaddress]

        response = IsIpAssignedToLoadBalancerRule(self, lb_rule1, iplist)
        self.assertTrue(response[0], response[1])

        with self.assertRaises(Exception):
            NIC.removeIp(self.apiclient, ipaddressid=secondaryip.id)
        return


class TestListLBRuleInstances(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestListLBRuleInstances,
                                     cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.testdata = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())

        template = get_template(cls.api_client,
                                cls.zone.id,
                                cls.testdata["ostype"])
        # Set Zones and disk offerings
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        cls._cleanup = []

        try:
            cls.hypervisor = cloudstackTestClient.getHypervisorInfo()
            # As Hyperv is GUI based VM, it requires more resources to be
            # able to SSH properly to it
            if cls.hypervisor.lower() == 'hyperv':
                cls.testdata["service_offering"]["name"] = "Medium Instance"
                cls.testdata["service_offering"]["memory"] = "1024"
                cls.testdata["service_offering"]["cpuspeed"] = "1024"

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"])
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

        try:
            self.account = Account.create(self.apiclient,
                                          self.testdata["account"],
                                          domainid=self.domain.id)
            self.cleanup.append(self.account)
            self.virtual_machine = VirtualMachine.create(
                self.api_client,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.zone.networktype)
        except Exception as e:
            self.tearDown()
            raise e

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_09_lbvmips_true(self):
        """List load balancer instances by passing lbvmips flag True

        steps:
        # 1. Add secondary IP for VM
        # 2. Associate public IP for the network
        # 3. Create load balancer rule for IP
        # 4. Assign load balancer rule to secondary IP
        # 5. List loadbalancer instances by specifying lbvmips flag as True
        # 6. List of load balancer instances should contain the secondary
             IP of VM used to assign lb rule to"""

        secondaryip = NIC.addIp(self.apiclient,
                                id=self.virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(secondaryip.ipaddress)}]

        lb_rule.assign(self.apiclient,
                       vmidipmap=vmidipmap)

        iplist = [secondaryip.ipaddress]

        response = IsIpAssignedToLoadBalancerRule(self, lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        lbruleinstances = LoadBalancerRule.listLoadBalancerRuleInstances(
            self.apiclient,
            id=lb_rule.id,
            lbvmips=True
        )

        self.assertEqual(validateList(lbruleinstances)[0], PASS,
                         "lbruleinstances list validation failed")

        self.assertEqual(str(lbruleinstances[0].lbvmipaddresses[
                         0]), secondaryip.ipaddress, "IP address in lbruleinstances list not matching\
                          with secondary ip assigned to lb rule")

        return

    @attr(tags=["advanced", "selfservice"], required_hardware="false")
    def test_10_lbvmips_false(self):
        """List load balancer instances by passing lbvmips flag False

        steps:
        # 1. Add secondary IP for VM
        # 2. Associate public IP for the network
        # 3. Create load balancer rule for IP
        # 4. Assign load balancer rule to secondary IP
        # 5. List loadbalancer instances by specifying lbvmips flag as False
        # 6. List of load balancer instances should not list\
             the vm ip addresses to which load balancer rule
             has been assigned"""

        secondaryip = NIC.addIp(self.apiclient,
                                id=self.virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(secondaryip.ipaddress)}]

        lb_rule.assign(self.apiclient,
                       vmidipmap=vmidipmap)

        iplist = [secondaryip.ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        lbruleinstances = LoadBalancerRule.listLoadBalancerRuleInstances(
            self.apiclient,
            id=lb_rule.id,
            lbvmips=False
        )

        self.assertEqual(validateList(lbruleinstances)[0], PASS,
                         "lbruleinstances list validation failed")

        self.assertTrue(lbruleinstances[0].lbvmipaddresses is None,
                        "IP address in lbruleinstances list not matching\
                          with secondary ip assigned to lb rule")

        return


class TestLbRuleFunctioning(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestLbRuleFunctioning,
                                     cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.testdata = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())

        template = get_template(cls.api_client,
                                cls.zone.id,
                                cls.testdata["ostype"])
        # Set Zones and disk offerings
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        cls._cleanup = []

        try:
            cls.hypervisor = cloudstackTestClient.getHypervisorInfo()
            # As Hyperv is GUI based VM, it requires more resources to be
            # able to SSH properly to it
            if cls.hypervisor.lower() == 'hyperv':
                cls.testdata["service_offering"]["name"] = "Medium Instance"
                cls.testdata["service_offering"]["memory"] = "1024"
                cls.testdata["service_offering"]["cpuspeed"] = "1024"

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"])
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

        try:
            self.account = Account.create(self.apiclient,
                                          self.testdata["account"],
                                          domainid=self.domain.id)
            self.cleanup.append(self.account)
            self.virtual_machine = VirtualMachine.create(
                self.api_client,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.zone.networktype)

            self.secondaryip = NIC.addIp(self.apiclient,
                                         id=self.virtual_machine.nic[0].id)

            self.public_ip = PublicIPAddress.create(
                self.apiclient,
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                networkid=self.virtual_machine.nic[0].networkid)

            FireWallRule.create(
                self.apiclient,
                ipaddressid=self.public_ip.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.testdata["fwrule"]["cidr"]],
                startport=self.testdata["fwrule"]["startport"],
                endport=self.testdata["fwrule"]["endport"])

            # To make secondary IP working for VM, we have to configure it on
            # VM after acquiring it
            # Hence create NAT rule for public IP and SSH to it
            # using the primary IP of VM
            # After SSH is successful, configure the secondary IP on VM,
            # delete the NAT rule and then create LB rule for secondary IP

            nat_rule = NATRule.create(
                self.apiclient,
                self.virtual_machine,
                self.testdata["natrule"],
                ipaddressid=self.public_ip.ipaddress.id,
                networkid=self.virtual_machine.nic[0].networkid)

            sshClient = SshClient(self.public_ip.ipaddress.ipaddress,
                                  self.testdata['natrule']["publicport"],
                                  self.virtual_machine.username,
                                  self.virtual_machine.password
                                  )
            response = sshClient.execute("netstat -i")
            # Sample Reponse:
            # [u'Kernel Interface table',
            # u'Iface MTU Met RX-OK RX-ERR RX-DRP RX-OVR TX-OK TX-ERR TX-DRP TX-OVR Flg',
            # u'eth2 1500  0   29     0     0       0      38    0       0     0    BMRU',
            # u'lo   16436 0   4      0     0       0       4    0       0
            # 0    LRU']}

            # We need to extract default interface out of the response
            defaultEthernetDevice = str(response[2].split()[0])

            cmd = "ip addr add {0}/24 broadcast {0} dev {1}".format(
                self.secondaryip.ipaddress, defaultEthernetDevice)
            sshClient.execute(cmd)
            sshClient.execute("ip addr show")

            # Deleting NAT rule after configuring secondary IP
            nat_rule.delete(self.apiclient)

            self.testdata["lbrule"]["publicport"] = 22
            self.testdata["lbrule"]["privateport"] = 22

            self.lb_rule = LoadBalancerRule.create(
                self.apiclient,
                self.testdata["lbrule"],
                ipaddressid=self.public_ip.ipaddress.id,
                accountid=self.account.name,
                networkid=self.virtual_machine.nic[0].networkid,
                domainid=self.account.domainid)
        except Exception as e:
            self.tearDown()
            raise e

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_11_ssh_to_secondary_ip(self):
        """SSH to VM using LB rule assigned to secondary IP of VM

        steps:
        # 1. Add secondary IP for VM
        # 2. Associate public IP for the network
        # 3. Create load balancer rule for IP
        # 4. Assign load balancer rule to secondary IP
        # 5. Try to SSH to VM using the public IP"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)
        return

    @unittest.skip("Failing-WIP")
    @attr(tags=["advanced"], required_hardware="true")
    def test_12_ssh_to_primary_secondary_ip(self):
        """SSH to VM using LB rule assigned to primary and secondary IP of VM

        steps:
        # 1. Add secondary IP for VM
        # 2. Associate public IP for the network
        # 3. Create load balancer rule for IP
        # 4. Assign load balancer rule to primary and secondary IP of VM
        # 5. Try to SSH to VM using the public IP
        # 6. Delete the primary IP
        # 7. Try to list load balancer rule, the operation should fail
        # 8. Try to SSH to VM using the public IP, the operation should fail"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        self.public_ip.delete(self.apiclient)

        with self.assertRaises(Exception):
            LoadBalancerRule.list(self.apiclient, id=self.lb_rule.id)

        with self.assertRaises(Exception):
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_13_delete_lb_rule(self):
        """SSH to VM after deleting LB rule

        steps:
        # 1. Add secondary IP for VM
        # 2. Associate public IP for the network
        # 3. Create load balancer rule for IP
        # 4. Assign load balancer rule to primary and secondary IP of VM
        # 5. Try to SSH to VM using the public IP, it should succeed
        # 6. Delete the LB rule
        # 7. Try to SSH to VM using the public IP, operation should fail"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        self.lb_rule.delete(self.apiclient)

        with self.assertRaises(Exception):
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_14_remove_lb_rule_secondary_ip(self):
        """ssh to vm after removing secondary ip from load balancer rule

        steps:
        # 1. add secondary ip for vm
        # 2. associate public ip for the network
        # 3. create load balancer rule for ip
        # 4. assign load balancer rule to primary and secondary ip of vm
        # 5. try to ssh to vm using the public ip, it should succeed
        # 6. remove the secondary ip of vm from the load balancer rule
        # 7. try to ssh to vm using the public ip, it should succeed"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)},
                     {"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress,
                  self.virtual_machine.nic[0].ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)}]

        self.lb_rule.remove(self.apiclient,
                            vmidipmap=vmidipmap)

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_15_remove_lb_rule_primary_ip(self):
        """ssh to vm after removing secondary ip from load balancer rule

        steps:
        # 1. Add secondary ip for vm
        # 2. Associate public ip for the network
        # 3. Create load balancer rule for ip
        # 4. Assign load balancer rule to primary and secondary ip of vm
        # 5. Try to ssh to vm using the public ip, it should succeed
        # 6. Remove the primary ip of vm from the load balancer rule
        # 7. Try to ssh to vm using the public ip, it should succeed"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)},
                     {"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress,
                  self.virtual_machine.nic[0].ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)}]

        self.lb_rule.remove(self.apiclient,
                            vmidipmap=vmidipmap)

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_16_delete_vm_from_lb_rule(self):
        """ssh to vm after removing secondary ip from load balancer rule

        steps:
        # 1. Add secondary ip for vm
        # 2. Associate public ip for the network
        # 3. Create load balancer rule for ip
        # 4. Assign load balancer rule to secondary ip of vm
        # 5. Try to ssh to vm using the public ip, it should succeed
        # 6. Remove the VM from load balancer rule
        # 7. Try to list the load balancer rules for the VM,
        #    list should be empty
        # 8. Try to SSH to VM using the public IP, the opeation should fail"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        self.lb_rule.remove(self.apiclient, vms=[self.virtual_machine])

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            listall=True)
        self.assertTrue(lbrules is None,
                        "LB rules list should be empty")

        with self.assertRaises(Exception):
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_20_destroy_recover_vm(self):
        """Verify LB rules after destroying and recovering VM

        steps:
        # 1. Add secondary ip for vm
        # 2. Associate public ip for the network
        # 3. Create load balancer rule for ip
        # 4. Assign load balancer rule to secondary ip of vm
        # 5. Try to ssh to vm using the public ip, it should succeed
        # 6. Destroy VM
        # 7 Recover the VM
        # 8.Try to list the LB rule, it should be present"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        # Destroy VM
        self.virtual_machine.delete(self.apiclient, expunge=False)
        # Recover VM
        self.virtual_machine.recover(self.apiclient)
        self.virtual_machine.start(self.apiclient)

        lbruleinstances = LoadBalancerRule.listLoadBalancerRuleInstances(
            self.apiclient,
            id=self.lb_rule.id,
            lbvmips=True
        )
        self.assertEqual(validateList(lbruleinstances)[0], PASS,
                         "lbruleinstances list validation failed")

        self.assertEqual(str(lbruleinstances[0].lbvmipaddresses[
                         0]), self.secondaryip.ipaddress, "IP address in lbruleinstances list not matching\
                          with secondary ip assigned to lb rule")
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_21_modify_lb_rule_algorithm(self):
        """Verify LB rule functioning with different algorithm

        steps:
        # 1. Add secondary ip for vm
        # 2. Associate public ip for the network
        # 3. Create load balancer rule for ip using roundrobin algorithm
        # 4. Assign load balancer rule to secondary ip of vm
        # 5. Try to ssh to vm using the public ip, it should succeed
        # 6. Update the algorithm of LB rule as leastconn
        # 7. Try to SSH to VM, it should succeed"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        self.lb_rule.update(self.apiclient, algorithm="leastconn")

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)
        return


class TestNetworkOperations(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestNetworkOperations,
                                     cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.testdata = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())

        template = get_template(cls.api_client,
                                cls.zone.id,
                                cls.testdata["ostype"])
        # Set Zones and disk offerings
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        cls._cleanup = []

        try:
            cls.hypervisor = cloudstackTestClient.getHypervisorInfo()
            # As Hyperv is GUI based VM, it requires more resources to be
            # able to SSH properly to it
            if cls.hypervisor.lower() == 'hyperv':
                cls.testdata["service_offering"]["name"] = "Medium Instance"
                cls.testdata["service_offering"]["memory"] = "1024"
                cls.testdata["service_offering"]["cpuspeed"] = "1024"

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"])
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

        try:
            self.account = Account.create(self.apiclient,
                                          self.testdata["account"],
                                          domainid=self.domain.id)
            self.cleanup.append(self.account)
            self.virtual_machine = VirtualMachine.create(
                self.api_client,
                self.testdata["virtual_machine"],
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                mode=self.zone.networktype)
            self.secondaryip = NIC.addIp(self.apiclient,
                                         id=self.virtual_machine.nic[0].id)

            self.public_ip = PublicIPAddress.create(
                self.apiclient,
                accountid=self.account.name,
                zoneid=self.zone.id,
                domainid=self.account.domainid,
                networkid=self.virtual_machine.nic[0].networkid)

            FireWallRule.create(
                self.apiclient,
                ipaddressid=self.public_ip.ipaddress.id,
                protocol='TCP',
                cidrlist=[
                    self.testdata["fwrule"]["cidr"]],
                startport=self.testdata["fwrule"]["startport"],
                endport=self.testdata["fwrule"]["endport"])

            # To make secondary IP working for VM, we have to configure it
            # on VM after acquiring it
            # Hence create NAT rule for public IP and SSH to it
            # using the primary IP of VM
            # After SSH is successful, configure the secondary IP on VM,
            # delete the NAT rule  and then create LB rule for secondary IP

            nat_rule = NATRule.create(
                self.apiclient,
                self.virtual_machine,
                self.testdata["natrule"],
                ipaddressid=self.public_ip.ipaddress.id,
                networkid=self.virtual_machine.nic[0].networkid)

            sshClient = SshClient(self.public_ip.ipaddress.ipaddress,
                                  self.testdata['natrule']["publicport"],
                                  self.virtual_machine.username,
                                  self.virtual_machine.password
                                  )
            response = sshClient.execute("netstat -i")
            # Sample Reponse:
            # [u'Kernel Interface table',
            # u'Iface MTU Met RX-OK RX-ERR RX-DRP RX-OVR TX-OK TX-ERR TX-DRP TX-OVR Flg',
            # u'eth2 1500  0   29     0     0       0      38    0       0     0    BMRU',
            # u'lo   16436 0   4      0     0       0       4    0       0
            # 0    LRU']}

            # We need to extract default interface out of the response
            defaultEthernetDevice = str(response[2].split()[0])

            cmd = "ip addr add {0}/24 broadcast {0} dev {1}".format(
                self.secondaryip.ipaddress, defaultEthernetDevice)
            sshClient.execute(cmd)
            sshClient.execute("ip addr show")

            # Deleting NAT rule after configuring secondary IP
            nat_rule.delete(self.apiclient)

            self.testdata["lbrule"]["publicport"] = 22
            self.testdata["lbrule"]["privateport"] = 22

            self.lb_rule = LoadBalancerRule.create(
                self.apiclient,
                self.testdata["lbrule"],
                ipaddressid=self.public_ip.ipaddress.id,
                accountid=self.account.name,
                networkid=self.virtual_machine.nic[0].networkid,
                domainid=self.account.domainid)
        except Exception as e:
            self.tearDown()
            raise e

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_17_restart_router(self):
        """Verify LB rules after restarting router VM

        steps:
        # 1. Add secondary ip for vm
        # 2. Associate public ip for the network
        # 3. Create load balancer rule for ip using roundrobin algorithm
        # 4. Assign load balancer rule to secondary ip of vm
        # 5. Try to ssh to vm using the public ip, it should succeed
        # 6. List the router associated with the network and restart the router
        # 7. List the LB rule, it should be present
        # 8. Try to SSH to VM, it should succeed"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)},
                     {"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress,
                  self.virtual_machine.nic[0].ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        routers = Router.list(
            self.apiclient,
            networkid=self.virtual_machine.nic[0].networkid,
            listall=True)
        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "routers list validation failed")

        Router.reboot(self.apiclient, id=routers[0].id)

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            id=self.lb_rule.id,
            listll=True)
        self.assertEqual(
            validateList(lbrules)[0],
            PASS,
            "lbrules list validation failed")

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_18_restart_network_cleanup_true(self):
        """Verfy LB rules after restarting the network with cleanup flag set to True

        steps:
        # 1. Add secondary ip for vm
        # 2. Associate public ip for the network
        # 3. Create load balancer rule for ip using roundrobin algorithm
        # 4. Assign load balancer rule to secondary ip of vm
        # 5. Try to ssh to vm using the public ip, it should succeed
        # 6. Restart the network with cleanup flag set to True
        # 7. List the LB rule, it should be present
        # 8. Try to SSH to VM, it should succeed"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)},
                     {"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress,
                  self.virtual_machine.nic[0].ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        cmd = restartNetwork.restartNetworkCmd()
        cmd.id = self.virtual_machine.nic[0].networkid
        cmd.cleanup = True
        self.apiclient.restartNetwork(cmd)

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            id=self.lb_rule.id,
            listll=True)
        self.assertEqual(
            validateList(lbrules)[0],
            PASS,
            "lbrules list validation failed")

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_19_restart_network_cleanup_false(self):
        """Verfy LB rules after restarting the network with cleanup flag set to False

        steps:
        # 1. Add secondary ip for vm
        # 2. Associate public ip for the network
        # 3. Create load balancer rule for ip using roundrobin algorithm
        # 4. Assign load balancer rule to secondary ip of vm
        # 5. Try to ssh to vm using the public ip, it should succeed
        # 6. Restart the network with cleanup flag set to False
        # 7. List the LB rule, it should be present
        # 8. Try to SSH to VM, it should succeed"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)},
                     {"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress,
                  self.virtual_machine.nic[0].ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        cmd = restartNetwork.restartNetworkCmd()
        cmd.id = self.virtual_machine.nic[0].networkid
        cmd.cleanup = False
        self.apiclient.restartNetwork(cmd)

        lbrules = LoadBalancerRule.list(
            self.apiclient,
            id=self.lb_rule.id,
            listll=True)
        self.assertEqual(
            validateList(lbrules)[0],
            PASS,
            "lbrules list validation failed")

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)
        return

    @attr(tags=["advanced"], required_hardware="true")
    def test_22_network_gc(self):
        """Verify LB rule functioning to secondary IP after network GC

        steps:
        # 1. Add secondary ip for vm
        # 2. Associate public ip for the network
        # 3. Create load balancer rule for ip using roundrobin algorithm
        # 4. Assign load balancer rule to secondary ip of vm
        # 5. Try to ssh to vm using the public ip, it should succeed
        # 6. Stop the VM
        # 7. Wait for the network.gc time interval
        # 7. Check the state of the network, it should be allocated
        # 8. Check the state of the related router VM, it should be stopped
        # 9. Start the virtual machine
        # 8. Check the state of the network, it should be implemented now
        # 11.Check the state of the router, it should be running
        # 12.Try to SSH to the VM using public IP, it should be successful"""

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.secondaryip.ipaddress)},
                     {"vmid": str(self.virtual_machine.id),
                      "vmip": str(self.virtual_machine.nic[0].ipaddress)}]

        self.lb_rule.assign(self.apiclient,
                            vmidipmap=vmidipmap)

        iplist = [self.secondaryip.ipaddress,
                  self.virtual_machine.nic[0].ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, self.lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        self.virtual_machine.stop(self.apiclient)

        wait_for_cleanup(self.apiclient, ["network.gc.wait",
                                          "network.gc.interval"])

        response = verifyNetworkState(
            self.api_client,
            self.virtual_machine.nic[0].networkid,
            "allocated")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)

        routers = Router.list(
            self.apiclient,
            networkid=self.virtual_machine.nic[0].networkid,
            listall=True)
        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "routers list validation failed")
        self.assertEqual(
            str(routers[0].state).lower(),
            "stopped",
            "Router should be in stopped state"
        )

        self.virtual_machine.start(self.apiclient)

        response = verifyNetworkState(
            self.api_client,
            self.virtual_machine.nic[0].networkid,
            "implemented")
        exceptionOccured = response[0]
        isNetworkInDesiredState = response[1]
        exceptionMessage = response[2]

        if (exceptionOccured or (not isNetworkInDesiredState)):
            self.fail(exceptionMessage)

        routers = Router.list(
            self.apiclient,
            networkid=self.virtual_machine.nic[0].networkid,
            listall=True)
        self.assertEqual(
            validateList(routers)[0],
            PASS,
            "routers list validation failed")
        self.assertEqual(
            str(routers[0].state).lower(),
            "running",
            "Router should be in running state")

        try:
            SshClient(self.public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)
        return


class TestExternalLoadBalancer(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cloudstackTestClient = super(TestExternalLoadBalancer,
                                     cls).getClsTestClient()
        cls.api_client = cloudstackTestClient.getApiClient()
        cls.testdata = cloudstackTestClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(
            cls.api_client,
            cloudstackTestClient.getZoneForTests())

        template = get_template(cls.api_client,
                                cls.zone.id,
                                cls.testdata["ostype"])
        # Set Zones and disk offerings
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = template.id
        cls._cleanup = []
        cls.testdata["configurableData"]\
            ["netscaler"]["lbdevicededicated"] = False

        try:
            cls.netscaler = add_netscaler(
                cls.api_client,
                cls.zone.id,
                cls.testdata["configurableData"]["netscaler"])
            cls._cleanup.append(cls.netscaler)
        except Exception as e:
            raise unittest.SkipTest("Failed to add netscaler device: %s" % e)

        try:
            cls.hypervisor = cloudstackTestClient.getHypervisorInfo()
            # As Hyperv is GUI based VM, it requires more resources to be
            # able to SSH properly to it
            if cls.hypervisor.lower() == 'hyperv':
                cls.testdata["service_offering"]["name"] = "Medium Instance"
                cls.testdata["service_offering"]["memory"] = "1024"
                cls.testdata["service_offering"]["cpuspeed"] = "1024"

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.testdata["service_offering"])
            cls._cleanup.append(cls.service_offering)
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest(e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.cleanup = []

        try:
            self.account = Account.create(self.apiclient,
                                          self.testdata["account"],
                                          domainid=self.domain.id)
            self.cleanup.append(self.account)
        except Exception as e:
            self.tearDown()
            raise e

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advancedns", "provisioning"], required_hardware="true")
    def test_23_lb_rule_functioning_with_netscaler(self):
        """Verify LB rule functioning for secondary IP with LB handled
           through Netscaler device

        steps:
        # 1. Add secondary ip for vm
        # 2. Associate public ip for the network
        # 3. Create load balancer rule for ip using roundrobin algorithm
        # 4. Assign load balancer rule to secondary ip of vm
        # 5. Try to ssh to vm using the public ip, it should succeed
        # 6. Update the algorithm of LB rule as leastconn
        # 7. Try to SSH to VM, it should succeed"""

        # Creating Network Offerign with LB as Netscalar
        nwoff_netscaler = NetworkOffering.create(
            self.apiclient,
            self.testdata["nw_off_isolated_netscaler"]
        )
        # Enable Network offering
        nwoff_netscaler.update(self.apiclient, state='Enabled')
        # Creating a Network Using the Network Offering
        network = Network.create(
            self.apiclient,
            self.testdata["network"],
            accountid=self.account.name,
            domainid=self.domain.id,
            networkofferingid=nwoff_netscaler.id,
            zoneid=self.zone.id
        )

        self.virtual_machine = VirtualMachine.create(
            self.api_client,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.zone.networktype,
            networkids=[network.id])

        secondaryip = NIC.addIp(self.apiclient,
                                id=self.virtual_machine.nic[0].id)

        public_ip = PublicIPAddress.create(
            self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            networkid=self.virtual_machine.nic[0].networkid)

        FireWallRule.create(
            self.apiclient,
            ipaddressid=public_ip.ipaddress.id,
            protocol='TCP',
            cidrlist=[
                self.testdata["fwrule"]["cidr"]],
            startport=self.testdata["fwrule"]["startport"],
            endport=self.testdata["fwrule"]["endport"])

        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine,
            self.testdata["natrule"],
            ipaddressid=public_ip.ipaddress.id,
            networkid=self.virtual_machine.nic[0].networkid)

        sshClient = SshClient(public_ip.ipaddress.ipaddress,
                              self.testdata['natrule']["publicport"],
                              self.virtual_machine.username,
                              self.virtual_machine.password
                              )
        response = sshClient.execute("netstat -i")
        # Sample Reponse:
        # [u'Kernel Interface table',
        # u'Iface MTU Met RX-OK RX-ERR RX-DRP RX-OVR TX-OK TX-ERR TX-DRP TX-OVR Flg',
        # u'eth2 1500  0   29     0     0       0      38    0       0     0    BMRU',
        # u'lo   16436 0   4      0     0       0       4    0       0     0
        # LRU']}

        # We need to extract default interface out of the response
        defaultEthernetDevice = str(response[2].split()[0])

        cmd = "ip addr add {0}/24 broadcast {0} dev {1}".format(
            secondaryip.ipaddress, defaultEthernetDevice)
        sshClient.execute(cmd)
        sshClient.execute("ip addr show")

        # Deleting NAT rule after configuring secondary IP
        nat_rule.delete(self.apiclient)

        self.testdata["lbrule"]["publicport"] = 22
        self.testdata["lbrule"]["privateport"] = 22

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.testdata["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=self.virtual_machine.nic[0].networkid,
            domainid=self.account.domainid)

        vmidipmap = [{"vmid": str(self.virtual_machine.id),
                      "vmip": str(secondaryip.ipaddress)}]

        lb_rule.assign(self.apiclient,
                       vmidipmap=vmidipmap)

        iplist = [secondaryip.ipaddress]
        response = IsIpAssignedToLoadBalancerRule(self, lb_rule, iplist)
        self.assertTrue(response[0], response[1])

        try:
            SshClient(public_ip.ipaddress.ipaddress,
                      self.testdata['lbrule']["publicport"],
                      self.virtual_machine.username,
                      self.virtual_machine.password
                      )
        except Exception as e:
            self.fail("Exception during SSH : %s" % e)

        return
