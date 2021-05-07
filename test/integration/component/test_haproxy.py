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
""" P1 tests for VPN users
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (
    Account,
    ServiceOffering,
    VirtualMachine,
    PublicIPAddress,
    Network,
    LoadBalancerRule,
    Alert,
    Router,
    Vpn,
    FireWallRule
)
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_template
                               )
from marvin.lib.utils import (cleanup_resources,
                              random_gen,
                              verifyRouterState)
from marvin.cloudstackAPI import createLBStickinessPolicy
from marvin.codes import PASS
from marvin.sshClient import SshClient


class TestHAProxyStickyness(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestHAProxyStickyness, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["service_offering"]["name"] = "Medium Instance"
        cls.services["service_offering"]["cpuspeed"] = "1024"
        cls.services["service_offering"]["memory"] = "1024"
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
            pass
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            domainid=self.domain.id
        )
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )

        self.virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id
        )
        self.public_ip = PublicIPAddress.create(
            self.apiclient,
            self.virtual_machine.account,
            self.virtual_machine.zoneid,
            self.virtual_machine.domainid,
            self.services["virtual_machine"]
        )
        FireWallRule.create(
            self.apiclient,
            ipaddressid=self.public_ip.ipaddress.id,
            protocol='TCP',
            cidrlist=[self.services["fwrule"]["cidr"]],
            startport=self.services["fwrule"]["startport"],
            endport=self.services["fwrule"]["endport"]
        )
        self.cleanup = [self.account, ]
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def get_Network(self, account):
        """Returns a network for account"""

        networks = Network.list(
            self.apiclient,
            account=account.name,
            domainid=account.domainid,
            listall=True
        )
        self.assertIsInstance(networks,
                              list,
                              "List networks should return a valid response")
        return networks[0]

    def create_LB_Rule(self, public_ip, network, vmarray, services=None):
        """Create and validate the load balancing rule"""

        self.debug("Creating LB rule for IP address: %s" %
                   public_ip.ipaddress.ipaddress)
        objservices = None
        if services:
            objservices = services
        else:
            objservices = self.services["lbrule"]

        self.services["lbrule"]["publicport"] = 22
        self.services["lbrule"]["privateport"] = 22

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            objservices,
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name,
            networkid=network.id,
            domainid=self.account.domainid
        )
        self.debug("Adding virtual machines %s to LB rule" % str(vmarray))
        lb_rule.assign(self.apiclient, vmarray)
        return lb_rule

    def configure_Stickiness_Policy(self, lb_rule, method, paramDict=None):
        """Configure the stickiness policy on lb rule"""
        try:
            result = lb_rule.createSticky(
                self.apiclient,
                methodname=method,
                name="-".join([method, random_gen()]),
                param=paramDict
            )
            self.debug("Response: %s" % result)
            return result
        except Exception as e:
            self.fail("Configure sticky policy failed with exception: %s" % e)

    def validate_Stickiness_Policy(self, lb_rule, method, publicip):
        """Validates the stickiness policy"""

        sticky_policies = lb_rule.listStickyPolicies(self.apiclient,
                                                     lbruleid=lb_rule.id,
                                                     listall=True)
        self.assertIsInstance(
            sticky_policies,
            list,
            "List sticky policies should return a valid list")
        sticky_policy = sticky_policies[0]

        self.debug("Stickiness policy method: %s" %
                   sticky_policy.stickinesspolicy[0].methodname)
        self.assertEqual(
            sticky_policy.stickinesspolicy[0].methodname,
            method,
            "Stickiness policy should have method as - %s" %
            method)

        hostnames = []

        hostnames = self.try_ssh(publicip, hostnames)
        hostnames = self.try_ssh(publicip, hostnames)

        self.debug("hostnames: %s" % hostnames)
        self.debug("set(hostnames): %s" % set(hostnames))

        # For each ssh, host should be the same, else stickiness policy is not
        # working properly
        if len(hostnames) == len(set(hostnames)):
            raise Exception(
                "Stickyness policy: %s not working properly, got hostnames %s" %
                (method, hostnames))
        return

    def delete_Stickiness_policy(self, policy, lb_rule):
        """Deletes the stickiness policy"""

        try:
            lb_rule.deleteSticky(self.apiclient, id=policy.id)
        except Exception as e:
            self.fail("Failed to delete the stickiness policy: %s" % e)

        sticky_policies = lb_rule.listStickyPolicies(self.apiclient,
                                                     lbruleid=lb_rule.id,
                                                     listall=True)
        self.assertIsInstance(
            sticky_policies,
            list,
            "List stickiness policies shall return a valid response")

        policy = sticky_policies[0]

        self.assertEqual(len(policy.stickinesspolicy),
                         0,
                         "List stickiness policy should return nothing")
        return

    def check_stickiness_supported_methods(self, supportedMethods, value):

        for i, dic in enumerate(supportedMethods):
            if dic["methodname"] == value:
                return True
        return False

    def acquire_Public_Ip(self):
        """Acquires the public IP"""

        try:
            self.debug("Acquiring public IP for account: %s" %
                       self.account.name)
            public_ip = PublicIPAddress.create(
                self.apiclient,
                self.virtual_machine.account,
                self.virtual_machine.zoneid,
                self.virtual_machine.domainid,
                self.services["virtual_machine"]
            )
            self.debug("Acquired public IP: %s" %
                       public_ip.ipaddress.ipaddress)

            FireWallRule.create(
                self.apiclient,
                ipaddressid=public_ip.ipaddress.id,
                protocol='TCP',
                cidrlist=[self.services["fwrule"]["cidr"]],
                startport=self.services["fwrule"]["startport"],
                endport=self.services["fwrule"]["endport"]
            )
            return public_ip
        except Exception as e:
            self.fail("Failed to acquire new public IP: %s" % e)

    def get_router(self, account):
        """Returns a default router for account"""

        routers = Router.list(self.apiclient,
                              account=self.account.name,
                              domainid=self.account.domainid,
                              listall=True)
        self.assertIsInstance(routers, list,
                              "List routers should return a valid repsonse")
        return routers[0]

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

    def try_ssh(self, ip_addr, hostnames):
        try:
            self.debug(
                "SSH into (Public IP: %s)" % ip_addr)

            # If Round Robin Algorithm is chosen,
            # each ssh command should alternate between VMs

            ssh_1 = SshClient(
                ip_addr,
                22,
                self.virtual_machine.username,
                self.virtual_machine.password
            )
            hostnames.append(ssh_1.execute("hostname")[0])
            self.debug(hostnames)
        except Exception as e:
            self.fail("%s: SSH failed for VM with IP Address: %s" %
                      (e, ip_addr))
        return hostnames

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    @attr(speed="slow")
    def test_01_create_sticky_policy_default_values(self):
        """Test Configure stickiness policies with default values"""

        # Validate the following
        # 1. Create a LB rule with round robin. listLoadBalancerRules should
        #    show newly created load balancer rule.
        # 2. Configure the Source based, app cookie and lb cookie based policy
        #   listLBStickinessPolicies should show newly created stickiness

        self.debug("Creating a load balancing rule on IP: %s" %
                   self.public_ip.ipaddress.ipaddress)

        lb_rule = self.create_LB_Rule(
            self.public_ip,
            network=self.get_Network(
                self.account),
            vmarray=[
                self.virtual_machine,
                self.virtual_machine_2])

        #TODO: Add code to check the AppCookie and LbCookie Stickiness policies
        methods = ["SourceBased"]
        for method in methods:
            self.debug("Creating stickiness policy for the LB rule: %s" %
                       lb_rule.id)
            policies = self.configure_Stickiness_Policy(lb_rule, method=method)

            policy = policies.stickinesspolicy[0]

            self.debug("Policy: %s" % str(policy))
            self.debug("Validating the stickiness policy")
            self.validate_Stickiness_Policy(
                lb_rule,
                method,
                self.public_ip.ipaddress.ipaddress)
            self.debug("Deleting the stickiness policy for lb rule: %s" %
                       lb_rule.name)
            self.delete_Stickiness_policy(policy, lb_rule)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    @attr(speed="slow")
    def test_02_create_sticky_policy_custom_values(self):
        """Test Configure stickiness policies with custom values"""

        # Validate the following
        # 1. Create a LB rule with roundrobin, leastconn and source.
        #    listLoadBalancerRules should show newly created load balancer rule
        # 2. Configure the Source based, app cookie and lb cookie based policy
        #    with custom parameters
        #   listLBStickinessPolicies should show newly created stickiness

        lb_methods = ["roundrobin", "leastconn", "source"]

        configs = {"SourceBased": {"tablesize": '100k'}}

        #TODO: Add code to check the AppCookie and LbCookie Stickiness policies
        for lb_method in lb_methods:
            self.debug("Creating a load balancing rule on IP %s and algo %s" %
                       (self.public_ip.ipaddress.ipaddress, lb_method))

            services = self.services["lbrule"]
            services["alg"] = lb_method

            lb_rule = self.create_LB_Rule(
                self.public_ip,
                network=self.get_Network(
                    self.account),
                vmarray=[
                    self.virtual_machine,
                    self.virtual_machine_2],
                services=services)

            for method, params in list(configs.items()):
                self.debug("Creating stickiness policy for the LB rule: %s" %
                           lb_rule.id)
                policies = self.configure_Stickiness_Policy(lb_rule,
                                                            method=method,
                                                            paramDict=params)

                policy = policies.stickinesspolicy[0]
                self.debug("Policy: %s" % str(policy))

                self.debug("Validating the stickiness policy")
                self.validate_Stickiness_Policy(
                    lb_rule,
                    method,
                    self.public_ip.ipaddress.ipaddress)
                self.debug("Deleting the stickiness policy for lb rule: %s" %
                           lb_rule.name)
                self.delete_Stickiness_policy(policy, lb_rule)
            self.debug("Deleting the LB rule: %s" % lb_rule.name)
            lb_rule.delete(self.apiclient)
        return

    @attr(tags=["advanced", "advancedns"])
    @attr(speed="slow")
    def test_03_supported_policies_by_network(self):
        """Test listnetworks response to check supported stickiness policies"""

        # Validate the following
        # 1. List networks for the account in advance network mode
        # 2. List of supported sticky methods should be present under
        #    SupportedStickinessMethods tag

        self.debug("List networks for account: %s" % self.account.name)
        networks = Network.list(self.apiclient,
                                account=self.account.name,
                                domainid=self.account.domainid,
                                listall=True)

        self.assertIsInstance(networks,
                              list,
                              "List network should return a valid response")
        network = networks[0]
        self.debug("Network: %s" % network)
        self.assertEqual(
            hasattr(
                network,
                "SupportedStickinessMethods"),
            True,
            "Network should have SupportedStickinessMethods param")

        self.assertEqual(hasattr(network, "LbCookie"),
                         True,
                         "Network should have LbCookie LB method param")

        self.assertEqual(hasattr(network, "AppCookie"),
                         True,
                         "Network should have AppCookie LB method param")

        self.assertEqual(hasattr(network, "SourceBased"),
                         True,
                         "Network should have SourceBased LB method param")

        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    @attr(speed="slow")
    def test_04_delete_lb_rule(self):
        """Test LB rule before/after stickiness policy creation"""

        # Validate the following
        # 1. Create a LB rule with roundrobin, leastconn and source.
        #    listLoadBalancerRules should show newly created load balancer rule
        # 2. Delete the loadbalancer rule. Delete loadbalancer rule should be
        #    successful
        # 3. Configure the Source based, app cookie and lb cookie based policy
        #    with custom parameters listLBStickinessPolicies should show newly
        #    created stickiness
        # 4. Delete load balancer rule. Delete should be successful

        lb_methods = ["roundrobin", "leastconn", "source"]

        #TODO: Add code to check the AppCookie and LbCookie Stickiness policies
        configs = {"SourceBased": {"tablesize": '100k'}}
        for lb_method in lb_methods:
            for method, params in list(configs.items()):
                self.debug("Creating load balancing rule on IP %s & algo %s" %
                           (self.public_ip.ipaddress.ipaddress, lb_method))

                services = self.services["lbrule"]
                services["alg"] = lb_method

                lb_rule = self.create_LB_Rule(
                    self.public_ip,
                    network=self.get_Network(
                        self.account),
                    vmarray=[
                        self.virtual_machine,
                        self.virtual_machine_2],
                    services=services)
                self.debug(
                    "Deleting the LB rule before stickiness policy creation")
                lb_rule.delete(self.apiclient)

                with self.assertRaises(Exception):
                    LoadBalancerRule.list(self.apiclient,
                                          id=lb_rule.id,
                                          listall=True)

                lb_rule = self.create_LB_Rule(
                    self.public_ip,
                    network=self.get_Network(
                        self.account),
                    vmarray=[
                        self.virtual_machine,
                        self.virtual_machine_2],
                    services=services)
                self.debug("Creating stickiness policy for the LB rule: %s" %
                           lb_rule.id)
                policies = self.configure_Stickiness_Policy(lb_rule,
                                                            method=method,
                                                            paramDict=params)

                policy = policies.stickinesspolicy[0]

                self.debug("Policy: %s" % str(policy))
                self.debug("Validating the stickiness policy")
                self.validate_Stickiness_Policy(
                    lb_rule,
                    method,
                    self.public_ip.ipaddress.ipaddress)

                self.debug("Deleting the LB rule: %s" % lb_rule.name)
                lb_rule.delete(self.apiclient)
                with self.assertRaises(Exception):
                    LoadBalancerRule.list(self.apiclient, id=lb_rule.id)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    @attr(speed="slow")
    def test_05_error_alerts_after_create(self):
        """Test error/alerts after creating stickiness policy"""

        # Validate the following
        # 1. Create a LB rule with round Robin/Least connections/Source
        #    listLoadBalancerRules should show newly created load balancer rule
        # 2. Configure the Stickiness policy to above created LB rule.
        #    listLBStickinessPolicies Api should show newly created stickiness
        # 3. update & delete stickiness policy see error related to stickiness
        # 4. No errors should be shown in the logs and alerts

        lb_methods = ["roundrobin", "leastconn", "source"]
        #TODO: Add code to check the AppCookie and LbCookie Stickiness policies
        configs = {"SourceBased": {"tablesize": '100k'}}
        for lb_method in lb_methods:
            for method, params in list(configs.items()):
                self.debug("Creating load balancing rule on IP %s & algo %s" %
                           (self.public_ip.ipaddress.ipaddress, lb_method))

                services = self.services["lbrule"]
                services["alg"] = lb_method

                lb_rule = self.create_LB_Rule(
                    self.public_ip,
                    network=self.get_Network(
                        self.account),
                    vmarray=[
                        self.virtual_machine,
                        self.virtual_machine_2],
                    services=services)

                self.debug("Creating stickiness policy for the LB rule: %s" %
                           lb_rule.id)
                policies = self.configure_Stickiness_Policy(lb_rule,
                                                            method=method,
                                                            paramDict=params)

                policy = policies.stickinesspolicy[0]

                self.debug("Policy: %s" % str(policy))
                self.debug("Validating the stickiness policy")
                self.validate_Stickiness_Policy(
                    lb_rule,
                    method,
                    self.public_ip.ipaddress.ipaddress)

                self.debug("Deleting the LB rule: %s" % lb_rule.name)
                lb_rule.delete(self.apiclient)

                with self.assertRaises(Exception):
                    LoadBalancerRule.list(self.apiclient,
                                          id=lb_rule.id,
                                          listall=True)
                alerts = Alert.list(self.apiclient, keyword="stickiness",
                                    listall=True)
                self.debug(
                    "Create/update/delete should not produce any alert/error")
                self.assertEqual(
                    alerts,
                    None,
                    "Create/update/delete should not produce any alert/error")
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    @attr(speed="slow")
    def test_06_release_ip(self):
        """Test release public IP with stickiness policy"""

        # 1. Configure load balancing rule. Listloadbalancerrule should list
        #    valid list
        # 2. Create stickiness policy. liststickinesspolicy should return valid
        #    response
        # 3. Release public Ip. liststickiness policy should return a valid
        #    response

        lb_methods = ["roundrobin", "leastconn", "source"]

        #TODO: Add code to check the AppCookie and LbCookie Stickiness policies
        configs = {"SourceBased": {"tablesize": '100k'}}

        for lb_method in lb_methods:
            for method, params in list(configs.items()):
                self.debug("Setting up environment - acquire public IP")
                public_ip = self.acquire_Public_Ip()

                self.debug(
                    "Creating a load balancing rule on IP %s and algo %s" %
                    (public_ip.ipaddress.ipaddress, lb_method))

                services = self.services["lbrule"]
                services["alg"] = lb_method

                lb_rule = self.create_LB_Rule(
                    public_ip,
                    network=self.get_Network(
                        self.account),
                    vmarray=[
                        self.virtual_machine,
                        self.virtual_machine_2],
                    services=services)

                policies = self.configure_Stickiness_Policy(lb_rule,
                                                            method=method,
                                                            paramDict=params)
                policy = policies.stickinesspolicy[0]

                self.debug("Policy: %s" % str(policy))
                self.debug("Validating the stickiness policy")
                self.validate_Stickiness_Policy(
                    lb_rule,
                    method,
                    public_ip.ipaddress.ipaddress)

                self.debug("Releasing public Ip: %s" %
                           public_ip.ipaddress.ipaddress)
                public_ip.delete(self.apiclient)

                self.debug("Checking the response of liststickiness policies")

                with self.assertRaises(Exception):
                    lb_rule.listStickyPolicies(self.apiclient,
                                               lbruleid=lb_rule.id,
                                               listall=True)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_07_delete_account(self):
        """Test Delete account  and check the router and its rules"""

        # Validate the following
        # 1. create an account
        # 2. using that account,create an instances
        # 3. select the Source NAT IP  and configure the stikiness policy
        # 4. Delete account
        # 5. The corresponding stikiness policy should be removed
        # listLBStickinessPolicies Api shouldnot show deleted stikiness policy

        self.debug("Creating LB rule for account: %s" %
                   self.account.name)
        lb_rule = self.create_LB_Rule(
            self.public_ip,
            network=self.get_Network(
                self.account),
            vmarray=[
                self.virtual_machine,
                self.virtual_machine_2])

        policies = self.configure_Stickiness_Policy(lb_rule, method="SourceBased")
        policy = policies.stickinesspolicy[0]

        self.debug("Policy: %s" % str(policy))
        self.debug("Validating the stickiness policy")
        self.validate_Stickiness_Policy(
            lb_rule,
            "SourceBased",
            self.public_ip.ipaddress.ipaddress)

        # removing account from cleanup list as we're deleting account
        self.cleanup.pop()
        self.debug("Deleting account: %s" % self.account.name)

        try:
            self.account.delete(self.apiclient)
        except Exception as e:
            self.fail("Failed to delete account: %s" % e)
        self.debug("Checking the response of liststickiness policies")

        with self.assertRaises(Exception):
            lb_rule.listStickyPolicies(self.apiclient,
                                       lbruleid=lb_rule.id,
                                       listall=True)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_08_create_policy_router_stopped(self):
        """Test verify create stickiness policy when router is stopped state"""

        # Validate the following
        # 1. stop the router
        # 2. create stikiness policy from UI
        # 3. start the router. listLBStickinessPolicies Api should show created
        #    stikiness policy

        self.debug("Creating LB rule for account: %s" % self.account.name)
        lb_rule = self.create_LB_Rule(
            self.public_ip,
            network=self.get_Network(
                self.account),
            vmarray=[
                self.virtual_machine,
                self.virtual_machine_2])

        self.debug("Fetching routers for the account: %s" %
                   self.account.name)
        router = self.get_router(self.account)

        self.debug("Stopping the router: %s" % router.name)
        Router.stop(self.apiclient, id=router.id)

        policies = self.configure_Stickiness_Policy(lb_rule, method="SourceBased")
        policy = policies.stickinesspolicy[0]

        self.debug("Starting the router: %s" % router.name)
        Router.start(self.apiclient, id=router.id)

        response = verifyRouterState(self.apiclient,
                                     router.id,
                                     "running")
        self.assertEqual(response[0], PASS, response[1])

        self.debug("Policy: %s" % str(policy))
        self.debug("Validating the stickiness policy")
        self.validate_Stickiness_Policy(
            lb_rule,
            "SourceBased",
            self.public_ip.ipaddress.ipaddress)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_09_create_policy_router_destroy(self):
        """Test check the stickiness policy rules after destroying router"""

        # Validate the following
        # 1. create an account
        # 2. using that account,create an instances
        # 3. select the Source NAT IP  and configure the stikiness policy
        # 4. destroy the router.

        self.debug("Creating LB rule for account: %s" % self.account.name)
        lb_rule = self.create_LB_Rule(
            self.public_ip,
            network=self.get_Network(
                self.account),
            vmarray=[
                self.virtual_machine,
                self.virtual_machine_2])

        self.debug("Fetching routers for the account: %s" %
                   self.account.name)
        router = self.get_router(self.account)

        policies = self.configure_Stickiness_Policy(lb_rule, method="SourceBased")
        policy = policies.stickinesspolicy[0]

        self.debug("Policy: %s" % str(policy))
        self.debug("Validating the stickiness policy")
        self.validate_Stickiness_Policy(
            lb_rule,
            "SourceBased",
            self.public_ip.ipaddress.ipaddress)

        self.debug("Destroying the router: %s" % router.name)
        Router.destroy(self.apiclient, id=router.id)

        return

    @attr(tags=["advanced", "advancedns"], required_hardware="true")
    def test_10_create_policy_enable_disable_vpn(self):
        """Test enable/disable the VPN after applying sticky policy rules"""

        # Validate the following
        # 1. create an account
        # 2. using that account,create an instances
        # 3. select the Source NAT IP  and configure the stikiness policy
        # 4. enable /disable the VPN. It should not impact the ceated rules
        #    listLBStickinessPolicies Api should show created stikiness policy

        self.debug("Creating LB rule for account: %s" % self.account.name)
        lb_rule = self.create_LB_Rule(
            self.public_ip,
            network=self.get_Network(
                self.account),
            vmarray=[
                self.virtual_machine,
                self.virtual_machine_2])

        policies = self.configure_Stickiness_Policy(lb_rule, method="SourceBased")
        policy = policies.stickinesspolicy[0]

        self.debug("Policy: %s" % str(policy))
        self.debug("Validating the stickiness policy")
        self.validate_Stickiness_Policy(
            lb_rule,
            "SourceBased",
            self.public_ip.ipaddress.ipaddress)

        self.debug("Enabling VPN on Public Ip: %s" %
                   self.public_ip.ipaddress.ipaddress)
        self.create_VPN(self.public_ip)

        self.debug("Validating the stickiness policy after enabling VPN")
        self.validate_Stickiness_Policy(
            lb_rule,
            "SourceBased",
            self.public_ip.ipaddress.ipaddress)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_11_invalid_params(self):
        """Test verfify functionality syncronous and asyncronous validations"""

        # Validate the following
        #  verify the validation  while creating or attaching stikiness policy
        #    by doing the following scenaios
        # * by passing the Invlaid parameter
        # * Invalid method name
        # * required parameter not present
        # * passing invalid values to valid paramters.

        self.debug("Creating LB rule for account: %s" % self.account.name)
        lb_rule = self.create_LB_Rule(
            self.public_ip,
            network=self.get_Network(
                self.account),
            vmarray=[
                self.virtual_machine,
                self.virtual_machine_2])

        self.debug("Creating stickiness policy with invalid method")
        with self.assertRaises(Exception):
            self.configure_Stickiness_Policy(lb_rule, method="InvalidMethod")

        self.debug("Creating stickiness policy with invalid params")
        with self.assertRaises(Exception):
            self.configure_Stickiness_Policy(lb_rule, method="LbCookie",
                                             params={"Test": 10})

        self.debug("Passing invalid parameter")
        with self.assertRaises(Exception):
            cmd = createLBStickinessPolicy.createLBStickinessPolicyCmd()
            cmd.lbruleid = lb_rule.id
            cmd.method = "LbCookie"
            cmd.name = "LbCookie"
            self.apiclient.createLBStickinessPolicy(cmd)

        self.debug("Creating stickiness policy not passing required param")
        with self.assertRaises(Exception):
            cmd = createLBStickinessPolicy.createLBStickinessPolicyCmd()
            cmd.lbruleid = lb_rule.id
            cmd.name = "LbCookie"
            self.apiclient.createLBStickinessPolicy(cmd)

        return
