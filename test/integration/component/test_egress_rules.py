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

""" P1 for Egresss & Ingress rules
"""
#Import Local Modules
import marvin
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.remoteSSHClient import remoteSSHClient
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *

#Import System modules
import time
import subprocess


class Services:
    """Test Security groups Services
    """

    def __init__(self):
        self.services = {
                "disk_offering": {
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
                "virtual_machine": {
                # Create a small virtual machine instance with disk offering
                    "displayname": "Test VM",
                    "username": "root",     # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                    "userdata": 'This is sample data',
                },
                "service_offering": {
                    "name": "Tiny Instance",
                    "displaytext": "Tiny Instance",
                    "cpunumber": 1,
                    "cpuspeed": 100,    # in MHz
                    "memory": 64,       # In MBs
                },
                "security_group": {
                    "name": 'SSH',
                    "protocol": 'TCP',
                    "startport": 22,
                    "endport": 22,
                    "cidrlist": '0.0.0.0/0',
                },
                "sg_invalid_port": {
                    "name": 'SSH',
                    "protocol": 'TCP',
                    "startport": -22,
                    "endport": -22,
                    "cidrlist": '0.0.0.0/0',
                },
                "sg_invalid_cidr": {
                    "name": 'SSH',
                    "protocol": 'TCP',
                    "startport": 22,
                    "endport": 22,
                    "cidrlist": '0.0.0.10'
                },
                "sg_cidr_anywhere": {
                    "name": 'SSH',
                    "protocol": 'TCP',
                    "startport": 22,
                    "endport": 22,
                    "cidrlist": '0.0.0.0/0'
                },
                "sg_cidr_restricted": {
                    "name": 'SSH',
                    "protocol": 'TCP',
                    "startport": 22,
                    "endport": 22,
                    "cidrlist": '10.0.0.1/24',
                },
                "sg_account": {
                    "name": 'SSH',
                    "protocol": 'TCP',
                    "startport": 22,
                    "endport": 22,
                    "cidrlist": '0.0.0.0/0'
                },
                "mgmt_server": {
                    "username": "root",
                    "password": "password",
                    "ipaddress": "192.168.100.21"
                },
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
            "sleep": 60,
            "timeout": 10,
            "mode": 'basic',
            # Networking mode: Basic or Advanced
        }


class TestDefaultSecurityGroupEgress(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestDefaultSecurityGroupEgress,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name

        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_deployVM_InDefaultSecurityGroup(self):
        """Test deploy VM in default security group with no egress rules
        """


        # Validate the following:
        # 1. Deploy a VM.
        # 2. Deployed VM should be running, verify with listVirtualMachiens
        # 3. listSecurityGroups for this account. should list the default
        #    security group with no egress rules
        # 4. listVirtualMachines should show that the VM belongs to default
        #    security group

        self.debug("Deploying VM in account: %s" % self.account.account.name)
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                )
        self.debug("Deployed VM with ID: %s" % self.virtual_machine.id)
        self.cleanup.append(self.virtual_machine)

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
                         "Check for list VM response"
                         )
        vm_response = list_vm_response[0]
        self.assertNotEqual(
                            len(list_vm_response),
                            0,
                            "Check VM available in List Virtual Machines"
                        )

        self.assertEqual(

                            vm_response.id,
                            self.virtual_machine.id,
                            "Check virtual machine id in listVirtualMachines"
                        )

        self.assertEqual(
                    vm_response.state,
                    'Running',
                    "VM state should be running"
                    )
        self.assertEqual(
                    hasattr(vm_response, "securitygroup"),
                    True,
                    "List VM response should have atleast one security group"
                    )

        # Verify listSecurity groups response
        security_groups = SecurityGroup.list(
                                        self.apiclient,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                         isinstance(security_groups, list),
                         True,
                         "Check for list security groups response"
                         )
        self.assertEqual(
                            len(security_groups),
                            1,
                            "Check List Security groups response"
                            )
        self.debug("List Security groups response: %s" %
                                            str(security_groups))
        sec_grp = security_groups[0]
        self.assertEqual(
                        sec_grp.name,
                        'default',
                        "List Sec Group should only list default sec. group"
                        )
        return


class TestAuthorizeIngressRule(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestAuthorizeIngressRule,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_authorizeIngressRule(self):
        """Test authorize ingress rule
        """


        # Validate the following:
        # 1. createaccount of type user
        # 2. createsecuritygroup (ssh) for this account
        # 3. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 4. deployVirtualMachine into this security group (ssh). deployed VM
        #    should be Running
        # 5. listSecurityGroups should show two groups, default and ssh
        # 6. verify that ssh-access into the VM is now allowed
        # 7. verify from within the VM is able to ping outside world
        #    (ping www.google.com)

        security_group = SecurityGroup.create(
                                              self.apiclient,
                                              self.services["security_group"],
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)
        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        ingress_rule = security_group.authorize(
                                self.apiclient,
                                self.services["security_group"],
                                account=self.account.account.name,
                                domainid=self.account.account.domainid
                                )
        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        self.debug("Authorizing ingress rule for sec group ID: %s for ssh access"
                                                            % security_group.id)
        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)
        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.ssh_ip)
            ssh = self.virtual_machine.get_ssh_client()

            # Ping to outsite world
            res = ssh.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )
        return


class TestDefaultGroupEgress(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestDefaultGroupEgress,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_01_default_group_with_egress(self):
        """Test default group with egress rule before VM deploy and ping, ssh
        """


        # Validate the following:
        # 1. createaccount of type user
        # 2. createsecuritygroup (ssh) for this account
        # 3. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 4. authorizeSecurityGroupEgress to allow ssh access only out to
        #    CIDR: 0.0.0.0/0
        # 5. deployVirtualMachine into this security group (ssh)
        # 6. deployed VM should be Running, ssh should be allowed into the VM,
        #    ping out to google.com from the VM should fail,
        #    ssh from within VM to mgt server should pass

        security_group = SecurityGroup.create(
                                              self.apiclient,
                                              self.services["security_group"],
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        self.debug("Authorizing ingress rule for sec group ID: %s for ssh access"
                                                            % security_group.id)
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__

        # Authorize Security group to SSH to VM
        self.debug("Authorizing egress rule for sec group ID: %s for ssh access"
                                                            % security_group.id)
        egress_rule = security_group.authorizeEgress(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(egress_rule, dict),
                          True,
                          "Check egress rule created properly"
                    )
        ssh_egress_rule = (egress_rule["egressrule"][0]).__dict__

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.ssh_ip)

            ssh = self.virtual_machine.get_ssh_client()

            self.debug("Ping to google.com from VM")
            # Ping to outsite world
            res = ssh.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )

        result = str(res)
        self.assertEqual(
                         result.count("0 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        try:
            self.debug("SSHing into management server from VM")
            res = ssh.execute("ssh %s@%s" % (
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["ipaddress"]
                                 ))
            self.debug("SSH result: %s" % str(res))

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )
        result = str(res)
        self.assertNotEqual(
                    result.count("No route to host"),
                    1,
                    "SSH into management server from VM should be successful"
                    )
        return


class TestDefaultGroupEgressAfterDeploy(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestDefaultGroupEgressAfterDeploy,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_01_default_group_with_egress(self):
        """ Test default group with egress rule added after vm deploy and ping,
            ssh test
        """


        # Validate the following:
        # 1. createaccount of type user
        # 2. createsecuritygroup (ssh) for this account
        # 3. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 4. deployVirtualMachine into this security group (ssh)
        # 5. authorizeSecurityGroupEgress to allow ssh access only out to
        #    CIDR: 0.0.0.0/0
        # 6. deployed VM should be Running, ssh should be allowed into the VM,
        #    ping out to google.com from the VM should fail

        security_group = SecurityGroup.create(
                                              self.apiclient,
                                              self.services["security_group"],
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                              self.apiclient,
                                              account=self.account.account.name,
                                              domainid=self.account.account.domainid
                                              )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        self.debug("Authorizing ingress rule for sec group ID: %s for ssh access"
                                                            % security_group.id)
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing egress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        egress_rule = security_group.authorizeEgress(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(egress_rule, dict),
                          True,
                          "Check egress rule created properly"
                    )
        ssh_egress_rule = (egress_rule["egressrule"][0]).__dict__

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.ssh_ip)
            ssh = self.virtual_machine.get_ssh_client()

            self.debug("Ping to google.com from VM")
            # Ping to outsite world
            res = ssh.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
            self.debug("SSH result: %s" % str(res))
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )

        result = str(res)
        self.assertEqual(
                         result.count("0 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )
        return


class TestRevokeEgressRule(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestRevokeEgressRule,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_revoke_egress_rule(self):
        """Test revoke security group egress rule
        """


        # Validate the following:
        # 1. createaccount of type user
        # 2. createsecuritygroup (ssh) for this account
        # 3. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 4. authorizeSecurityGroupEgress to allow ssh access only out to
        #    CIDR: 0.0.0.0/0
        # 5. deployVirtualMachine into this security group (ssh)
        # 6. deployed VM should be Running, ssh should be allowed into the VM,
        #    ping out to google.com from the VM should fail,
        #    ssh from within VM to mgt server should pass
        # 7. Revoke egress rule. Verify ping and SSH access to management server
        #    is restored

        security_group = SecurityGroup.create(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                        self.apiclient,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing ingress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__

        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing egress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        egress_rule = security_group.authorizeEgress(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(egress_rule, dict),
                          True,
                          "Check egress rule created properly"
                    )
        ssh_egress_rule = (egress_rule["egressrule"][0]).__dict__

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.ssh_ip)
            ssh = self.virtual_machine.get_ssh_client()

            self.debug("Ping to google.com from VM")
            # Ping to outsite world
            res = ssh.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
            self.debug("SSH result: %s" % str(res))
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )

        result = str(res)
        self.assertEqual(
                         result.count("0 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        try:
            self.debug("SSHing into management server from VM")
            res = ssh.execute("ssh %s@%s" % (
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["ipaddress"]
                                 ))
            self.debug("SSH result: %s" % str(res))

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )
        result = str(res)
        self.assertNotEqual(
                    result.count("No route to host"),
                    1,
                    "SSH into management server from VM should be successful"
                    )

        self.debug(
            "Revoke Egress Rule for Security Group %s for account: %s" \
                % (
                    security_group.id,
                    self.account.account.name
                ))

        result = security_group.revokeEgress(
                                self.apiclient,
                                id=ssh_egress_rule["ruleid"]
                                )
        self.debug("Revoke egress rule result: %s" % result)

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.ssh_ip)
            ssh = self.virtual_machine.get_ssh_client(reconnect=True)

            self.debug("Ping to google.com from VM")
            # Ping to outsite world
            res = ssh.execute("ping -c 1 www.google.com")
            # res = 64 bytes from maa03s17-in-f20.1e100.net (74.125.236.212):
            # icmp_req=1 ttl=57 time=25.9 ms
            # --- www.l.google.com ping statistics ---
            # 1 packets transmitted, 1 received, 0% packet loss, time 0ms
            # rtt min/avg/max/mdev = 25.970/25.970/25.970/0.000 ms
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )

        result = str(res)
        self.assertEqual(
                         result.count("1 received"),
                         1,
                         "Ping to outside world from VM should be successful"
                         )

        try:
            self.debug("SSHing into management server from VM")
            res = ssh.execute("ssh %s@%s" % (
                                    self.services["mgmt_server"]["username"],
                                    self.services["mgmt_server"]["ipaddress"]
                                 ))
            self.debug("SSH result: %s" % str(res))

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )
        result = str(res)
        self.assertNotEqual(
                    result.count("No route to host"),
                    1,
                    "SSH into management server from VM should be successful"
                    )
        return


class TestInvalidAccountAuthroize(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestInvalidAccountAuthroize,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_invalid_account_authroize(self):
        """Test invalid account authroize
        """


        # Validate the following:
        # 1. createaccount of type user
        # 2. createsecuritygroup (ssh) for this account
        # 3. authorizeSecurityGroupEgress to allow ssh access only out to
        #    non-existent random account and default security group
        # 4. listSecurityGroups should show ssh and default security groups
        # 5. authorizeSecurityGroupEgress API should fail since there is no
        #    account

        security_group = SecurityGroup.create(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                        self.apiclient,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )

        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing egress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        with self.assertRaises(Exception):
            egress_rule = security_group.authorizeEgress(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=random_gen(),
                                        domainid=self.account.account.domainid
                                        )
        return


class TestMultipleAccountsEgressRuleNeg(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestMultipleAccountsEgressRuleNeg,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.accountA = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.accountB = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.accountA.account.name
        cls._cleanup = [
                        cls.accountA,
                        cls.accountB,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_multiple_account_egress_rule_negative(self):
        """Test multiple account egress rules negative case
        """


        # Validate the following:
        # 1. createaccount of type user A
        # 2. createaccount of type user B
        # 3. createsecuritygroup (SSH-A) for account A
        # 4. authorizeSecurityGroupEgress in account A to allow ssh access
        #    only out to VMs in account B's default security group
        # 5. authorizeSecurityGroupIngress in account A to allow ssh incoming
        #    access from anywhere into Vm's of account A. listSecurityGroups
        #    for account A should show two groups (default and ssh-a) and ssh
        #    ingress rule and account based egress rule
        # 6. deployVM in account A into security group SSH-A. deployed VM
        #    should be Running
        # 7. deployVM in account B. deployed VM should be Running
        # 8. ssh into VM  in account A and from there ssh to VM in account B.
        #    ssh should fail

        security_group = SecurityGroup.create(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.accountA.account.name,
                                        domainid=self.accountA.account.domainid
                                        )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                        self.apiclient,
                                        account=self.accountA.account.name,
                                        domainid=self.accountA.account.domainid
                                        )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing egress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        # Authorize to only account not CIDR
        user_secgrp_list = {self.accountB.account.name: 'default'}

        egress_rule = security_group.authorizeEgress(
                                        self.apiclient,
                                        self.services["sg_account"],
                                        account=self.accountA.account.name,
                                        domainid=self.accountA.account.domainid,
                                        user_secgrp_list=user_secgrp_list
                                        )

        self.assertEqual(
                          isinstance(egress_rule, dict),
                          True,
                          "Check egress rule created properly"
                    )
        ssh_egress_rule = (egress_rule["egressrule"][0]).__dict__

        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing ingress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.accountA.account.name,
                                        domainid=self.accountA.account.domainid
                                        )

        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__

        self.virtual_machineA = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.accountA.account.name,
                                    domainid=self.accountA.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.cleanup.append(self.virtual_machineA)
        self.debug("Deploying VM in account: %s" % self.accountA.account.name)
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.virtual_machineA.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VM should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state after deployment should be running"
                         )
        self.debug("VM: %s state: %s" % (vm.id, vm.state))

        self.virtual_machineB = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.accountB.account.name,
                                    domainid=self.accountB.account.domainid,
                                    serviceofferingid=self.service_offering.id
                                )
        self.cleanup.append(self.virtual_machineB)
        self.debug("Deploying VM in account: %s" % self.accountB.account.name)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.virtual_machineB.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VM should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state after deployment should be running"
                         )
        self.debug("VM: %s state: %s" % (vm.id, vm.state))

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machineA.ssh_ip)
            ssh = self.virtual_machineA.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machineA.ipaddress, e)
                      )

        try:
            self.debug("SSHing into VM type B from VM A")
            self.debug("VM IP: %s" % self.virtual_machineB.ssh_ip)
            res = ssh.execute("ssh %s@%s" % (
                                self.services["virtual_machine"]["username"],
                                self.virtual_machineB.ssh_ip
                                ))
            self.debug("SSH result: %s" % str(res))

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machineA.ipaddress, e)
                      )
        result = str(res)
        self.assertEqual(
                    result.count("Connection timed out"),
                    1,
                    "SSH into management server from VM should not be successful"
                    )
        return


class TestMultipleAccountsEgressRule(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestMultipleAccountsEgressRule,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.accountA = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.accountB = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.accountA.account.name
        cls._cleanup = [
                        cls.accountA,
                        cls.accountB,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_multiple_account_egress_rule_positive(self):
        """Test multiple account egress rules positive case
        """


        # Validate the following:
        # 1. createaccount of type user A
        # 2. createaccount of type user B
        # 3. createsecuritygroup (SSH-A) for account A
        # 4. authorizeSecurityGroupEgress in account A to allow ssh access
        #    only out to VMs in account B's default security group
        # 5. authorizeSecurityGroupIngress in account A to allow ssh incoming
        #    access from anywhere into Vm's of account A. listSecurityGroups
        #    for account A should show two groups (default and ssh-a) and ssh
        #    ingress rule and account based egress rule
        # 6. deployVM in account A into security group SSH-A. deployed VM
        #    should be Running
        # 7. deployVM in account B. deployed VM should be Running
        # 8. ssh into VM  in account A and from there ssh to VM in account B.
        #    ssh should fail

        security_groupA = SecurityGroup.create(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.accountA.account.name,
                                        domainid=self.accountA.account.domainid
                                        )
        self.debug("Created security group with ID: %s" % security_groupA.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                        self.apiclient,
                                        account=self.accountA.account.name,
                                        domainid=self.accountA.account.domainid
                                        )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )

        security_groupB = SecurityGroup.create(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.accountB.account.name,
                                        domainid=self.accountB.account.domainid
                                        )
        self.debug("Created security group with ID: %s" % security_groupB.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                    self.apiclient,
                                    account=self.accountB.account.name,
                                    domainid=self.accountB.account.domainid
                                    )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )

        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing egress rule for sec group ID: %s for ssh access"
                                                        % security_groupA.id)
        # Authorize to only account not CIDR
        user_secgrp_list = {self.accountB.account.name: security_groupB.name}

        egress_rule = security_groupA.authorizeEgress(
                                        self.apiclient,
                                        self.services["sg_account"],
                                        account=self.accountA.account.name,
                                        domainid=self.accountA.account.domainid,
                                        user_secgrp_list=user_secgrp_list
                                        )

        self.assertEqual(
                          isinstance(egress_rule, dict),
                          True,
                          "Check egress rule created properly"
                    )
        ssh_egress_rule = (egress_rule["egressrule"][0]).__dict__

        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing ingress rule for sec group ID: %s for ssh access"
                                                        % security_groupA.id)
        ingress_ruleA = security_groupA.authorize(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.accountA.account.name,
                                        domainid=self.accountA.account.domainid
                                        )

        self.assertEqual(
                          isinstance(ingress_ruleA, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        ssh_ruleA = (ingress_ruleA["ingressrule"][0]).__dict__

        self.virtual_machineA = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.accountA.account.name,
                                    domainid=self.accountA.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_groupA.id]
                                )
        self.cleanup.append(self.virtual_machineA)
        self.debug("Deploying VM in account: %s" % self.accountA.account.name)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.virtual_machineA.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VM should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state after deployment should be running"
                         )
        self.debug("VM: %s state: %s" % (vm.id, vm.state))

        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing ingress rule for sec group ID: %s for ssh access"
                                                        % security_groupB.id)
        ingress_ruleB = security_groupB.authorize(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.accountB.account.name,
                                        domainid=self.accountB.account.domainid
                                        )

        self.assertEqual(
                          isinstance(ingress_ruleB, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        ssh_ruleB = (ingress_ruleB["ingressrule"][0]).__dict__

        self.virtual_machineB = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.accountB.account.name,
                                    domainid=self.accountB.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_groupB.id]
                                )
        self.cleanup.append(self.virtual_machineB)
        self.debug("Deploying VM in account: %s" % self.accountB.account.name)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.virtual_machineB.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VM should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state after deployment should be running"
                         )
        self.debug("VM: %s state: %s" % (vm.id, vm.state))

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machineA.ssh_ip)
            ssh = self.virtual_machineA.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machineA.ipaddress, e)
                      )

        try:
            self.debug("SSHing into VB type B from VM A")
            self.debug("VM IP: %s" % self.virtual_machineB.ssh_ip)

            res = ssh.execute("ssh %s@%s" % (
                                self.services["virtual_machine"]["username"],
                                self.virtual_machineB.ssh_ip
                                ))
            self.debug("SSH result: %s" % str(res))

        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machineA.ipaddress, e)
                      )
        result = str(res)
        self.assertNotEqual(
                    result.count("Connection timed out"),
                    1,
                    "SSH into management server from VM should be successful"
                    )
        return


class TestStartStopVMWithEgressRule(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestStartStopVMWithEgressRule,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_start_stop_vm_egress(self):
        """ Test stop start Vm with egress rules
        """


        # Validate the following:
        # 1. createaccount of type user
        # 2. createsecuritygroup (ssh) for this account
        # 3. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 4. authorizeSecurityGroupEgress to allow ssh access only out to
        #    CIDR: 0.0.0.0/0
        # 5. deployVirtualMachine into this security group (ssh)
        # 6. stopVirtualMachine
        # 7. startVirtualMachine
        # 8. ssh in to VM

        security_group = SecurityGroup.create(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                        self.apiclient,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        self.debug(
            "Authorizing ingress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing egress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        egress_rule = security_group.authorizeEgress(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(egress_rule, dict),
                          True,
                          "Check egress rule created properly"
                    )
        ssh_egress_rule = (egress_rule["egressrule"][0]).__dict__

        # Stop virtual machine
        self.debug("Stopping virtual machine: %s" % self.virtual_machine.id)
        self.virtual_machine.stop(self.apiclient)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VM should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Stopped",
                         "VM state should be stopped"
                         )
        self.debug("VM: %s state: %s" % (vm.id, vm.state))

        # Start virtual machine
        self.debug("Starting virtual machine: %s" % self.virtual_machine.id)
        self.virtual_machine.start(self.apiclient)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=self.virtual_machine.id,
                                  listall=True
                                  )
        self.assertEqual(
                         isinstance(vms, list),
                         True,
                         "List VM should return a valid list"
                         )
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "VM state should be stopped"
                         )
        self.debug("VM: %s state: %s" % (vm.id, vm.state))

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.ssh_ip)
            ssh = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )
        return


@unittest.skip("Valid bug- ID: CS-12647")
class TestInvalidParametersForEgress(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestInvalidParametersForEgress,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["sg", "eip"])
    def test_invalid_parameters(self):
        """ Test invalid parameters for egress rules
        """


        # Validate the following:
        # 1. createUserAccount
        # 2. createSecurityGroup (test)
        # 3. authorizeEgressRule (negative port) - Should fail
        # 4. authorizeEgressRule (invalid CIDR) - Should fail
        # 5. authorizeEgressRule (invalid account) - Should fail
        # 6. authorizeEgressRule (22, cidr: anywhere) and
        #    authorizeEgressRule (22, cidr: restricted) - Should pass
        # 7. authorizeEgressRule (21, cidr : 10.1.1.0/24) and
        #    authorizeEgressRule (21, cidr: 10.1.1.0/24) - Should fail

        security_group = SecurityGroup.create(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                        self.apiclient,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )

        # Authorize Security group to SSH to VM
        self.debug(
            "Authorizing egress rule for sec group ID: %s with invalid port"
                                                        % security_group.id)
        with self.assertRaises(Exception):
            egress_rule = security_group.authorizeEgress(
                                    self.apiclient,
                                    self.services["sg_invalid_port"],
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.debug(
            "Authorizing egress rule for sec group ID: %s with invalid cidr"
                                                        % security_group.id)
        with self.assertRaises(Exception):
            egress_rule = security_group.authorizeEgress(
                                    self.apiclient,
                                    self.services["sg_invalid_cidr"],
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.debug(
            "Authorizing egress rule for sec group ID: %s with invalid account"
                                                        % security_group.id)
        with self.assertRaises(Exception):
            egress_rule = security_group.authorizeEgress(
                                    self.apiclient,
                                    self.services["security_group"],
                                    account=random_gen(),
                                    domainid=self.account.account.domainid
                                    )
        self.debug(
            "Authorizing egress rule for sec group ID: %s with cidr: anywhere and port: 22"
                                                        % security_group.id)
        egress_rule_A = security_group.authorizeEgress(
                                    self.apiclient,
                                    self.services["sg_cidr_anywhere"],
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )

        self.assertEqual(
                          isinstance(egress_rule_A, dict),
                          True,
                          "Check egress rule created properly"
                    )

        egress_rule_R = security_group.authorizeEgress(
                                    self.apiclient,
                                    self.services["sg_cidr_restricted"],
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )

        self.assertEqual(
                          isinstance(egress_rule_R, dict),
                          True,
                          "Check egress rule created properly"
                    )

        self.debug(
            "Authorizing egress rule for sec group ID: %s with duplicate port"
                                                        % security_group.id)
        with self.assertRaises(Exception):
            security_group.authorizeEgress(
                                    self.apiclient,
                                    self.services["sg_cidr_restricted"],
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        return


class TestEgressAfterHostMaintainance(cloudstackTestCase):

    def setUp(self):

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @classmethod
    def setUpClass(cls):
        cls.services = Services().services
        cls.api_client = super(
                               TestEgressAfterHostMaintainance,
                               cls
                               ).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.pod = get_pod(
                          cls.api_client,
                          zoneid=cls.zone.id
                          )

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["domainid"] = cls.domain.id
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.account.name
        cls._cleanup = [
                        cls.account,
                        cls.service_offering
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(speed = "slow")
    @attr(tags = ["sg", "eip", "maintenance"])
    def test_egress_after_host_maintainance(self):
        """Test maintenance case for egress
        """

        # Validate the following:
        # 1. createaccount of type user
        # 2. createsecuritygroup (ssh) for this account
        # 3. authorizeSecurityGroupIngress to allow ssh access to the VM
        # 4. authorizeSecurityGroupEgress to allow ssh access only out to
        #    CIDR: 0.0.0.0/0
        # 5. deployVirtualMachine into this security group (ssh)
        # 6. deployed VM should be Running, ssh should be allowed into the VM
        # 7. Enable maintainance mode for host, cance maintainance mode
        # 8. User should be able to SSH into VM after maintainace

        security_group = SecurityGroup.create(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                      )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                        self.apiclient,
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                      )
        self.assertEqual(
                         isinstance(sercurity_groups, list),
                         True,
                         "Check for list security groups response"
                         )

        self.assertEqual(
                            len(sercurity_groups),
                            2,
                            "Check List Security groups response"
                            )
        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing ingress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        ingress_rule = security_group.authorize(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(ingress_rule, dict),
                          True,
                          "Check ingress rule created properly"
                    )

        ssh_rule = (ingress_rule["ingressrule"][0]).__dict__

        # Authorize Security group to SSH to VM
        self.debug(
                "Authorizing egress rule for sec group ID: %s for ssh access"
                                                        % security_group.id)
        egress_rule = security_group.authorizeEgress(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )

        self.assertEqual(
                          isinstance(egress_rule, dict),
                          True,
                          "Check egress rule created properly"
                    )
        ssh_egress_rule = (egress_rule["egressrule"][0]).__dict__

        self.virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id]
                                )
        self.debug("Deploying VM in account: %s" % self.account.account.name)

        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.id)
            ssh = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )
        vms = VirtualMachine.list(
                                    self.apiclient,
                                    id=self.virtual_machine.id,
                                    listall=True
                          )
        self.assertEqual(
                          isinstance(vms, list),
                          True,
                          "Check list VMs response for valid host"
                    )
        vm = vms[0]

        self.debug("Enabling host maintainance for ID: %s" % vm.hostid)
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = vm.hostid
        self.apiclient.prepareHostForMaintenance(cmd)

        self.debug("Canceling host maintainance for ID: %s" % vm.hostid)
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = vm.hostid
        self.apiclient.cancelHostMaintenance(cmd)

        self.debug("Waiting for SSVMs to come up")
        wait_for_ssvms(
                       self.apiclient,
                       zoneid=self.zone.id,
                       podid=self.pod.id,
                      )
        self.debug("Starting VM: %s" % self.virtual_machine.id)

        self.virtual_machine.start(self.apiclient)
        # Should be able to SSH VM
        try:
            self.debug("SSH into VM: %s" % self.virtual_machine.id)
            ssh = self.virtual_machine.get_ssh_client(reconnect=True)
        except Exception as e:
            self.fail("SSH Access failed for %s: %s" % \
                      (self.virtual_machine.ipaddress, e)
                      )
        return
