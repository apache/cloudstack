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
from marvin.sshClient import SshClient
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *

#Import System modules
import time
import subprocess


class Services:
    """Test Security groups Services
    """

    def __init__(self):
        self.services = {
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
                    "memory": 128,       # In MBs
                },
                "security_group": {
                    "name": 'SSH',
                    "protocol": 'TCP',
                    "startport": 22,
                    "endport": 22,
                    "cidrlist": '0.0.0.0/0',
                },
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
            "sleep": 60,
            "timeout": 10,
        }


class TestEgressAfterHostMaintenance(cloudstackTestCase):

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
        cls.testClient = super(TestEgressAfterHostMaintenance, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.pod = get_pod(
                          cls.api_client,
                          zone_id=cls.zone.id
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
        cls.services["account"] = cls.account.name
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
    def test_egress_after_host_maintenance(self):
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
        # 7. Enable maintenance mode for host, cance maintenance mode
        # 8. User should be able to SSH into VM after maintainace

        security_group = SecurityGroup.create(
                                        self.apiclient,
                                        self.services["security_group"],
                                        account=self.account.name,
                                        domainid=self.account.domainid
                                      )
        self.debug("Created security group with ID: %s" % security_group.id)

        # Default Security group should not have any ingress rule
        sercurity_groups = SecurityGroup.list(
                                        self.apiclient,
                                        account=self.account.name,
                                        domainid=self.account.domainid
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
                                        account=self.account.name,
                                        domainid=self.account.domainid
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
                                        account=self.account.name,
                                        domainid=self.account.domainid
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
                                    accountid=self.account.name,
                                    domainid=self.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    securitygroupids=[security_group.id],
                                    mode=self.services["mode"]
                                )
        self.debug("Deploying VM in account: %s" % self.account.name)

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

        self.debug("Enabling host maintenance for ID: %s" % vm.hostid)
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = vm.hostid
        self.apiclient.prepareHostForMaintenance(cmd)

        self.debug("Canceling host maintenance for ID: %s" % vm.hostid)
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
