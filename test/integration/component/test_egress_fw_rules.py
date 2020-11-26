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
"""
#Import Local Modules
from nose.plugins.attrib           import attr
from marvin.cloudstackTestCase     import cloudstackTestCase
import unittest
from marvin.lib.base   import (Account,
                                           Domain,
                                           Router,
                                           Network,
                                           ServiceOffering,
                                           NetworkOffering,
                                           VirtualMachine,
                                           FireWallRule,
                                           NATRule,
                                           PublicIPAddress)
from marvin.lib.common import (get_domain,
                                           get_zone,
                                           get_template,
                                           list_routers,
                                           list_virtual_machines
                                           )
from marvin.lib.utils import cleanup_resources, validateList
from marvin.cloudstackAPI import rebootRouter
from marvin.cloudstackAPI.createEgressFirewallRule import createEgressFirewallRuleCmd
from marvin.cloudstackAPI.deleteEgressFirewallRule import deleteEgressFirewallRuleCmd
from marvin.codes import PASS
import time

class Services:
    """Test service data: Egress Firewall rules Tests for Advance Zone.
    """
    def __init__(self):
        self.services = {
                         "host"             : {"username": 'root', # Credentials for SSH
                                               "password": 'password',
                                               "publicport": 22},
                         "domain"           : {"name": "Domain",},
                         "account"          : {"email"     : "test@test.com",
                                               "firstname" : "Test",
                                               "lastname"  : "User",
                                               "username"  : "test",
                                               # Random characters are appended in create account to
                                               # ensure unique username generated each time
                                               "password"  : "password",},
                         "user"             : {"email"    : "user@test.com",
                                               "firstname": "User",
                                               "lastname" : "User",
                                               "username" : "User",
                                               # Random characters are appended for unique
                                               # username
                                               "password" : "password",},
                         "project"          : {"name"        : "Project",
                                               "displaytext" : "Test project",},
                         "volume"           : {"diskname" : "TestDiskServ",
                                               "max"      : 6,},
                         "disk_offering"    : {"displaytext" : "Small",
                                               "name"        : "Small",
                                               "disksize"    : 1},
                         "virtual_machine"  : {"displayname" : "testserver",
                                               "username"    : "root",# VM creds for SSH
                                               "password"    : "password",
                                               "ssh_port"    : 22,
                                               "hypervisor"  : 'XenServer',
                                               "privateport" : 22,
                                               "publicport"  : 22,
                                               "protocol"    : 'TCP',},
                         "service_offering" : {"name"        : "Medium Instance",
                                               "displaytext" : "Medium Instance",
                                               "cpunumber"   : 2,
                                               "cpuspeed"    : 128,# in MHz
                                               "memory"      : 256},
                         "network_offering":  {
                                               "name": 'Network offering-VR services',
                                               "displaytext": 'Network offering-VR services',
                                               "guestiptype": 'Isolated',
                                               "supportedservices": 'Dhcp,Dns,SourceNat,PortForwarding,Vpn,Firewall,Lb,UserData,StaticNat',
                                               "traffictype": 'GUEST',
                                               "availability": 'Optional',
                                               "specifyVlan": 'False',
                                               "serviceProviderList": {
                                                            "Dhcp": 'VirtualRouter',
                                                            "Dns": 'VirtualRouter',
                                                            "SourceNat": 'VirtualRouter',
                                                            "PortForwarding": 'VirtualRouter',
                                                            "Vpn": 'VirtualRouter',
                                                            "Firewall": 'VirtualRouter',
                                                            "Lb": 'VirtualRouter',
                                                            "UserData": 'VirtualRouter',
                                                            "StaticNat": 'VirtualRouter',
                                                        },
                                               "serviceCapabilityList": {
                                                                            "SourceNat": {
                                                                            "SupportedSourceNatTypes": "peraccount",
                                                                            }
                                                                        },
                                             },
                          "network"   :   {
                                          "name": "Test Network",
                                          "displaytext": "Test Network",
                                         },
                          "natrule"   :   {
                                          "privateport": 22,
                                          "publicport": 22,
                                          "protocol": "TCP"
                                          },
                         "sleep" :  30,
                         "ostype":  'CentOS 5.3 (64-bit)',
                         "host_password": 'password',
                        }

class TestEgressFWRules(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls._cleanup = []
        cls.testClient = super(TestEgressFWRules, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        # Get and set template id for VM creation.
        cls.template = get_template(cls.api_client,
                                    cls.zone.id,
                                    cls.services["ostype"])
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.hypervisor = cls.testClient.getHypervisorInfo()

        # As Hyperv is GUI based VM, it requires more resources to be
        # able to SSH properly to it
        if cls.hypervisor.lower()  == 'hyperv':
            cls.services["service_offering"]["name"] = "Medium Instance"
            cls.services["service_offering"]["memory"] = "1024"
            cls.services["service_offering"]["cpuspeed"] = "1024"

        # Create service offerings.
        cls.service_offering = ServiceOffering.create(cls.api_client,
                                                      cls.services["service_offering"])
        # Cleanup
        cls._cleanup.append(cls.service_offering)


    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, reversed(cls._cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.api_client
        self.dbclient = self.testClient.getDbConnection()
        self.egressruleid = None
        self.cleanup   = []
        self.domain  =  Domain.create(self.apiclient,
                                      self.services["domain"])
        # Create an Account associated with domain
        self.account = Account.create(self.apiclient,
                                      self.services["account"],
                                      domainid=self.domain.id)
        self.cleanup.append(self.account)
        self.cleanup.append(self.domain)
        return

    def create_network_offering(self, egress_policy=True, RR=False):
        if egress_policy:
            self.services["network_offering"]["egress_policy"] = "true"
        else:
            self.services["network_offering"]["egress_policy"] = "false"

        if RR:
            self.debug("Redundant Router Enabled")
            self.services["network_offering"]["serviceCapabilityList"]["SourceNat"]["RedundantRouter"] = "true"

        self.network_offering = NetworkOffering.create(self.apiclient,
                                                       self.services["network_offering"],
                                                       conservemode=True)

        # Cleanup
        self.cleanup.append(self.network_offering)

        # Enable Network offering
        self.network_offering.update(self.apiclient, state='Enabled')

    def create_vm(self, pfrule=False, egress_policy=True, RR=False):
        self.create_network_offering(egress_policy, RR)
         # Creating network using the network offering created
        self.debug("Creating network with network offering: %s" %
                                                    self.network_offering.id)
        self.network = Network.create(self.apiclient,
                                      self.services["network"],
                                      accountid=self.account.name,
                                      domainid=self.account.domainid,
                                      networkofferingid=self.network_offering.id,
                                      zoneid=self.zone.id)
        self.debug("Created network with ID: %s" % self.network.id)
        self.debug("Deploying instance in the account: %s" % self.account.name)

        project = None
        self.virtual_machine = VirtualMachine.create(self.apiclient,
                                                         self.services["virtual_machine"],
                                                         accountid=self.account.name,
                                                         domainid=self.domain.id,
                                                         serviceofferingid=self.service_offering.id,
                                                         mode=self.zone.networktype if pfrule else 'basic',
                                                         networkids=[str(self.network.id)],
                                                         projectid=project.id if project else None)
        self.debug("Deployed instance %s in account: %s" % (self.virtual_machine.id,self.account.name))

        # Checking if VM is running or not, in case it is deployed in error state, test case fails
        self.vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)

        self.assertEqual(validateList(self.vm_list)[0], PASS, "vm list validation failed, vm list is %s" % self.vm_list)
        self.assertEqual(str(self.vm_list[0].state).lower(),'running',"VM state should be running, it is %s" % self.vm_list[0].state)

        self.public_ip = PublicIPAddress.create(
                                    self.apiclient,
                                    accountid=self.account.name,
                                    zoneid=self.zone.id,
                                    domainid=self.account.domainid,
                                    networkid=self.network.id
                                    )

        # Open up firewall port for SSH
        FireWallRule.create(
                            self.apiclient,
                            ipaddressid=self.public_ip.ipaddress.id,
                            protocol=self.services["natrule"]["protocol"],
                            cidrlist=['0.0.0.0/0'],
                            startport=self.services["natrule"]["publicport"],
                            endport=self.services["natrule"]["publicport"]
                            )

        self.debug("Creating NAT rule for VM ID: %s" % self.virtual_machine.id)
        #Create NAT rule
        NATRule.create(
                        self.apiclient,
                        self.virtual_machine,
                        self.services["natrule"],
                        self.public_ip.ipaddress.id
                        )
        return

    def exec_script_on_user_vm(self, script, exec_cmd_params, expected_result, negative_test=False):
        try:
            exec_success = False

            self.debug("getting SSH client for Vm: %s with ip %s" % (self.virtual_machine.id,self.public_ip.ipaddress.ipaddress))
            sshClient = self.virtual_machine.get_ssh_client(ipaddress=self.public_ip.ipaddress.ipaddress)
            result = sshClient.execute(script+exec_cmd_params)

            self.debug("script: %s" % script+exec_cmd_params)
            self.debug("result: %s" % result)

            if isinstance(result, list):
                str_result = str([str(x) for x in result])
            else:
                str_result = str(result)
            str_expected_result = str(expected_result).strip()
            if str_result == str_expected_result:
                exec_success = True

            if negative_test:
                self.assertEqual(exec_success,
                                 True,
                                 "Script result is %s matching with %s" % (str_result, str_expected_result))
            else:
                self.assertEqual(exec_success,
                                 True,
                                 "Script result is %s is not matching with %s" % (str_result, str_expected_result))

        except Exception as e:
            self.debug('Error=%s' % e)
            raise e

    def reboot_Router(self):
        vm_network_id = self.virtual_machine.nic[0].networkid
        list_routers_response = list_routers(self.apiclient,
                                             account=self.account.name,
                                             domainid=self.account.domainid,
                                             networkid=vm_network_id)
        self.assertEqual(isinstance(list_routers_response, list),
                         True,
                         "Check for list routers response return valid data")
        router = list_routers_response[0]
        #Reboot the router
        cmd = rebootRouter.rebootRouterCmd()
        cmd.id = router.id
        self.apiclient.rebootRouter(cmd)

        #List routers to check state of router
        router_response = list_routers(self.apiclient,
                                       id=router.id)
        self.assertEqual(isinstance(router_response, list),
                         True,
                         "Check list response returns a valid list")
        #List router should have router in running state and same public IP
        self.assertEqual(router_response[0].state,
                         'Running',
                         "Check list router response for router state")

    def createEgressRule(self, protocol='ICMP', cidr='10.1.1.0/24', start_port=None, end_port=None):
        nics = self.virtual_machine.nic
        self.debug('Creating Egress FW rule for networkid=%s networkname=%s' % (nics[0].networkid, nics[0].networkname))
        cmd = createEgressFirewallRuleCmd()
        cmd.networkid = nics[0].networkid
        cmd.protocol  = protocol
        if cidr:
            cmd.cidrlist  = [cidr]
        if start_port:
            cmd.startport = start_port
        if end_port:
            cmd.endport   = end_port
        rule = self.apiclient.createEgressFirewallRule(cmd)
        self.debug('Created rule=%s' % rule.id)
        self.egressruleid = rule.id

    def deleteEgressRule(self):
        cmd = deleteEgressFirewallRuleCmd()
        cmd.id = self.egressruleid
        self.apiclient.deleteEgressFirewallRule(cmd)
        self.egressruleid = None

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            self.fail("Warning! Cleanup failed: %s" % e)

    def create_another_vm(self):
        self.debug("Deploying instance in the account: %s and network: %s" % (self.account.name, self.network.id))

        project = None
        self.virtual_machine1 = VirtualMachine.create(self.apiclient,
                                                         self.services["virtual_machine"],
                                                         accountid=self.account.name,
                                                         domainid=self.domain.id,
                                                         serviceofferingid=self.service_offering.id,
                                                         mode=self.zone.networktype,
                                                         networkids=[str(self.network.id)],
                                                         projectid=project.id if project else None)
        self.debug("Deployed instance %s in account: %s" % (self.virtual_machine.id,self.account.name))

        # Checking if VM is running or not, in case it is deployed in error state, test case fails
        self.vm_list = list_virtual_machines(self.apiclient, id=self.virtual_machine.id)

        self.assertEqual(validateList(self.vm_list)[0], PASS, "vm list validation failed, vm list is %s" % self.vm_list)
        self.assertEqual(str(self.vm_list[0].state).lower(),'running',"VM state should be running, it is %s" % self.vm_list[0].state)

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_egress_fr1(self):
        """Test By-default the communication from guest n/w to public n/w is allowed.
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. login to VM.
        # 3. ping public network.
        # 4. public network should be reachable from the VM.
        # 5. Reboot the router
        # 6. Ping public network from VM, it should be reachable
        self.create_vm()
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)
        self.reboot_Router()
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_1_egress_fr1(self):
        """Test By-default the communication from guest n/w to public n/w is NOT allowed.
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 2. login to VM.
        # 3. ping public network.
        # 4. public network should not be reachable from the VM.
        self.create_vm(egress_policy=False)
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['100']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_01_2_egress_fr1(self):
        """Test egress rule with /32 CIDR of a VM, and check other VM in the
           network does not have public access
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 2. deploy another VM into the network created in step #1
        # 3. create egress rule with /32 CIDR of the second VM
        # 4. login to first VM.
        # 5. ping public network.
        # 6. public network should not be reachable from the first VM.
        self.create_vm(egress_policy=False)
        self.create_another_vm()
        self.createEgressRule(protocol='all', cidr=self.virtual_machine1.ipaddress+"/32")
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['100']",
                                    negative_test=False)
        # If failed probably we've hit this issue: CLOUDSTACK-10075

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_egress_fr2(self):
        """Test Allow Communication using Egress rule with CIDR + Port Range + Protocol.
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create egress rule with specific CIDR + port range.
        # 3. login to VM.
        # 4. ping public network.
        # 5. public network should not be reachable from the VM.
        self.create_vm()
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['100']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_1_egress_fr2(self):
        """Test Allow Communication using Egress rule with CIDR + Port Range + Protocol.
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 3. create egress rule with specific CIDR + port range.
        # 4. login to VM.
        # 5. ping public network.
        # 6. public network should be reachable from the VM.
        self.create_vm(egress_policy=False)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_02_2_egress_fr2(self):
        """Test Allow Communication using Egress rule with /32 CIDR + Port Range + Protocol.
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 3. create egress rule with specific /32 CIDR + port range.
        # 4. login to VM.
        # 5. ping public network.
        # 6. public network should be reachable from the VM.
        self.create_vm(egress_policy=False)
        self.createEgressRule(cidr=self.virtual_machine.ipaddress+"/32")
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_egress_fr3(self):
        """Test Communication blocked with network that is other than specified
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 3. create egress rule with specific CIDR + port range.
        # 4. login to VM.
        # 5. Try to reach to public network with other protocol/port range
        self.create_vm()
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        #Egress rule is set for ICMP other traffic is allowed
        self.exec_script_on_user_vm(' wget -t1 http://apache.claz.org/favicon.ico 2>&1',
                                    "| grep -oP 'failed:'",
                                    "[]",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_03_1_egress_fr3(self):
        """Test Communication blocked with network that is other than specified
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 3. create egress rule with specific CIDR + port range.
        # 4. login to VM.
        # 5. Try to reach to public network with other protocol/port range
        self.create_vm(egress_policy=False)
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['100']",
                                    negative_test=False)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        #Egress rule is set for ICMP other traffic is not allowed
        self.exec_script_on_user_vm(' wget -t1 http://apache.claz.org/favicon.ico 2>&1',
                                    "| grep -oP 'failed:'",
                                    "['failed:']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="false")
    def test_04_egress_fr4(self):
        """Test Create Egress rule and check the Firewall_Rules DB table
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create egress rule with specific CIDR + port range.
        # 3. check the table Firewall_Rules, Firewall and Traffic_type should be "Egress".
        self.create_vm()
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        qresultset = self.dbclient.execute("select purpose, traffic_type from firewall_rules where uuid='%s';" % self.egressruleid)
        self.assertEqual(isinstance(qresultset, list),
                         True,
                         "Check DB query result set for valid data")

        self.assertNotEqual(len(qresultset),
                            0,
                            "Check DB Query result set")
        self.assertEqual(qresultset[0][0],
                         "Firewall",
                         "DB results not matching, expected: Firewall found: %s " % qresultset[0][0])
        self.assertEqual(qresultset[0][1],
                         "Egress",
                         "DB results not matching, expected: Egress, found: %s" % qresultset[0][1])
        qresultset = self.dbclient.execute("select egress_default_policy from network_offerings where name='%s';" % self.network_offering.name)
        self.assertEqual(isinstance(qresultset, list),
                         True,
                         "Check DB query result set for valid data")

        self.assertNotEqual(len(qresultset),
                            0,
                            "Check DB Query result set")
        self.assertEqual(qresultset[0][0],
                         1,
                         "DB results not matching, expected: 1, found: %s" % qresultset[0][0])


    @attr(tags=["advanced"], required_hardware="false")
    def test_04_1_egress_fr4(self):
        """Test Create Egress rule and check the Firewall_Rules DB table
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 2. create egress rule with specific CIDR + port range.
        # 3. check the table Firewall_Rules, Firewall and Traffic_type should be "Egress".
        self.create_vm(egress_policy=False)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        qresultset = self.dbclient.execute("select purpose, traffic_type from firewall_rules where uuid='%s';" % self.egressruleid)
        self.assertEqual(isinstance(qresultset, list),
                         True,
                         "Check DB query result set for valid data")

        self.assertNotEqual(len(qresultset),
                            0,
                            "Check DB Query result set")
        self.assertEqual(qresultset[0][0],
                         "Firewall",
                         "DB results not matching, expected: Firewall found: %s " % qresultset[0][0])
        self.assertEqual(qresultset[0][1],
                         "Egress",
                         "DB results not matching, expected: Egress, found: %s" % qresultset[0][1])
        qresultset = self.dbclient.execute("select egress_default_policy from network_offerings where name='%s';" % self.network_offering.name)
        self.assertEqual(isinstance(qresultset, list),
                         True,
                         "Check DB query result set for valid data")

        self.assertNotEqual(len(qresultset),
                            0,
                            "Check DB Query result set")
        self.assertEqual(qresultset[0][0],
                         0,
                         "DB results not matching, expected: 0, found: %s" % qresultset[0][0])

    @unittest.skip("Skip")
    @attr(tags=["advanced", "NotRun"])
    def test_05_egress_fr5(self):
        """Test Create Egress rule and check the IP tables
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create egress rule with specific CIDR + port range.
        # 3. login to VR.
        # 4. Check iptables for rules settings.
        #  -A FW_OUTBOUND -j FW_EGRESS_RULES
        #  -A FW_EGRESS_RULES -m state --state RELATED,ESTABLISHED -j ACCEPT
        #  -A FW_EGRESS_RULES -d 10.147.28.0/24 -p tcp -m tcp --dport 22 -j ACCEPT
        #  -A FW_EGRESS_RULES -j DROP
        self.create_vm()
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        #TODO: Query VR for expected route rules.


    @unittest.skip("Skip")
    @attr(tags=["advanced", "NotRun"])
    def test_05_1_egress_fr5(self):
        """Test Create Egress rule and check the IP tables
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 2. create egress rule with specific CIDR + port range.
        # 3. login to VR.
        # 4. Check iptables for rules settings.
        #  -A FW_OUTBOUND -j FW_EGRESS_RULES
        #  -A FW_EGRESS_RULES -m state --state RELATED,ESTABLISHED -j ACCEPT
        #  -A FW_EGRESS_RULES -d 10.147.28.0/24 -p tcp -m tcp --dport 22 -j ACCEPT
        #  -A FW_EGRESS_RULES -j DROP
        self.create_vm(egress_policy=False)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        #TODO: Query VR for expected route rules.


    @attr(tags=["advanced"], required_hardware="true")
    def test_06_egress_fr6(self):
        """Test Create Egress rule without CIDR
        """
        # Validate the following:
        # 1. deploy VM.using network offering with egress policy true.
        # 2. create egress rule without specific CIDR.
        # 3. login to VM.
        # 4. access to public network should not be successfull.
        self.create_vm()
        self.createEgressRule(cidr=None)
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['100']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_06_1_egress_fr6(self):
        """Test Create Egress rule without CIDR
        """
        # Validate the following:
        # 1. deploy VM.using network offering with egress policy false.
        # 2. create egress rule without specific CIDR.
        # 3. login to VM.
        # 4. access to public network should be successfull.
        self.create_vm(egress_policy=False)
        self.createEgressRule(cidr=None)
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_07_egress_fr7(self):
        """Test Create Egress rule without End Port
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create egress rule without specific end port.
        # 3. login to VM.
        # 4. access to public network should not be successfull.
        self.create_vm()
        self.createEgressRule(protocol='tcp', start_port=80, cidr=TestEgressFWRules.zone.guestcidraddress)
        self.exec_script_on_user_vm(' wget -t1 http://apache.claz.org/favicon.ico 2>&1',
                                    "| grep -oP 'failed:'",
                                    "['failed:']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_07_1_egress_fr7(self):
        """Test Create Egress rule without End Port
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create egress rule without specific end port.
        # 3. login to VM.
        # 4. access to public network for tcp port 80 is blocked.
        self.create_vm()
        self.createEgressRule(protocol='tcp', start_port=80, cidr=TestEgressFWRules.zone.guestcidraddress)
        self.exec_script_on_user_vm(' wget -t1 http://apache.claz.org/favicon.ico 2>&1',
                                    "| grep -oP 'failed:'",
                                    "['failed:']",
                                    negative_test=False)

    @unittest.skip("Skip")
    @attr(tags=["advanced", "NotRun"])
    def test_08_egress_fr8(self):
        """Test Port Forwarding and Egress Conflict
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true and pf on public ip.
        # 2. create egress rule with specific CIDR + port range.
        # 3. Egress should not impact pf rule.
        self.create_vm(pfrule=True)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)

    @unittest.skip("Skip")
    @attr(tags=["advanced", "NotRun"])
    def test_08_1_egress_fr8(self):
        """Test Port Forwarding and Egress Conflict
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false and pf on public ip.
        # 2. create egress rule with specific CIDR + port range.
        # 3. Egress should not impact pf rule.
        self.create_vm(pfrule=True, egress_policy=False)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)


    @attr(tags=["advanced"], required_hardware="true")
    def test_09_egress_fr9(self):
        """Test Delete Egress rule
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create egress rule with specific CIDR + port range.
        # 3. login to VM.
        # 4. ping public network.
        # 3. public network should not be reachable from the VM.
        # 4. delete egress rule.
        # 5. connection to public network should be reachable.
        self.create_vm()
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['100']",
                                    negative_test=False)
        self.deleteEgressRule()
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_09_1_egress_fr9(self):
        """Test Delete Egress rule
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 2. create egress rule with specific CIDR + port range.
        # 3. login to VM.
        # 4. ping public network.
        # 3. public network should be reachable from the VM.
        # 4. delete egress rule.
        # 5. connection to public network should not be reachable.
        self.create_vm(egress_policy=False)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)
        self.deleteEgressRule()
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['100']",
                                    negative_test=False)


    @attr(tags=["advanced"], required_hardware="false")
    def test_10_egress_fr10(self):
        """Test Invalid CIDR and Invalid Port ranges
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create egress rule invalid cidr invalid port range.
        # 3. egress rule creation should fail.
        self.create_vm()
        self.assertRaises(Exception, self.createEgressRule, cidr='10.2.2.0/24')

    @attr(tags=["advanced"], required_hardware="false")
    def test_10_1_egress_fr10(self):
        """Test Invalid CIDR and Invalid Port ranges
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 2. create egress rule invalid cidr invalid port range.
        # 3. egress rule creation should fail.
        self.create_vm(egress_policy=False)
        self.assertRaises(Exception, self.createEgressRule, cidr='10.2.2.0/24')


    @attr(tags=["advanced"], required_hardware="false")
    def test_11_egress_fr11(self):
        """Test Regression on Firewall + PF + LB + SNAT
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create PF/SNAT/LB.
        # 3. All should work fine.
        self.create_vm(pfrule=True)

    @attr(tags=["advanced"], required_hardware="false")
    def test_11_1_egress_fr11(self):
        """Test Regression on Firewall + PF + LB + SNAT
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 2. create PF/SNAT/LB.
        # 3. All should work fine.
        self.create_vm(pfrule=True, egress_policy=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_12_egress_fr12(self):
        """Test Reboot Router
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create egress rule valid cidr and port range.
        # 3. reboot router.
        # 4. access to public network should not be successfull.
        self.create_vm()
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        self.reboot_Router()
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['100']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_12_1_egress_fr12(self):
        """Test Reboot Router
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 2. create egress rule valid  cidr port range.
        # 3. reboot router.
        # 4. access to public network should be successfull.
        self.create_vm(egress_policy=False)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        self.reboot_Router()
        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_13_egress_fr13(self):
        """Test Redundant Router : Master failover
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy true.
        # 2. create egress rule valid cidr valid port range.
        # 3. redundant router
        # 3. All should work fine.
        #TODO: setup network with RR
        self.create_vm(RR=True)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        vm_network_id = self.virtual_machine.nic[0].networkid
        self.debug("Listing routers for network: %s" % vm_network_id)
        routers = Router.list(self.apiclient,
                              networkid=vm_network_id,
                              listall=True)
        self.assertEqual(isinstance(routers, list),
                         True,
                         "list router should return Master and backup routers")
        self.assertEqual(len(routers),
                         2,
                         "Length of the list router should be 2 (Backup & master)")

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Redundant states: %s, %s" % (master_router.redundantstate,
                                                backup_router.redundantstate))
        self.debug("Stopping the Master router")
        try:
            Router.stop(self.apiclient, id=master_router.id)
        except Exception as e:
            self.fail("Failed to stop master router: %s" % e)

        # wait for VR update state
        time.sleep(60)

        self.debug("Checking state of the master router in %s" % self.network.name)
        routers = Router.list(self.apiclient,
                              id=master_router.id,
                              listall=True)
        self.assertEqual(isinstance(routers, list),
                         True,
                         "list router should return Master and backup routers")

        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['100']",
                                    negative_test=False)

    @attr(tags=["advanced"], required_hardware="true")
    def test_13_1_egress_fr13(self):
        """Test Redundant Router : Master failover
        """
        # Validate the following:
        # 1. deploy VM using network offering with egress policy false.
        # 2. create egress rule valid cidr valid port range.
        # 3. redundant router
        # 3. All should work fine.
        #TODO: setup network with RR
        self.create_vm(RR=True, egress_policy=False)
        self.createEgressRule(cidr=TestEgressFWRules.zone.guestcidraddress)
        vm_network_id = self.virtual_machine.nic[0].networkid
        self.debug("Listing routers for network: %s" % vm_network_id)
        routers = Router.list(self.apiclient,
                              networkid=vm_network_id,
                              listall=True)
        self.assertEqual(isinstance(routers, list),
                         True,
                         "list router should return Master and backup routers")
        self.assertEqual(len(routers),
                         2,
                         "Length of the list router should be 2 (Backup & master)")

        if routers[0].redundantstate == 'MASTER':
            master_router = routers[0]
            backup_router = routers[1]
        else:
            master_router = routers[1]
            backup_router = routers[0]

        self.debug("Redundant states: %s, %s" % (master_router.redundantstate,
                                                backup_router.redundantstate))
        self.debug("Stopping the Master router")
        try:
            Router.stop(self.apiclient, id=master_router.id)
        except Exception as e:
            self.fail("Failed to stop master router: %s" % e)

        # wait for VR update state
        time.sleep(60)

        self.debug("Checking state of the master router in %s" % self.network.name)
        routers = Router.list(self.apiclient,
                              id=master_router.id,
                              listall=True)
        self.assertEqual(isinstance(routers, list),
                         True,
                         "list router should return Master and backup routers")

        self.exec_script_on_user_vm('ping -c 1 www.google.com',
                                    "| grep -oP \'\d+(?=% packet loss)\'",
                                    "['0']",
                                    negative_test=False)
