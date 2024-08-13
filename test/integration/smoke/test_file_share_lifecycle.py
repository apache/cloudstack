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
Tests for File Share
"""
from asyncio import sleep

# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackAPI import (createFirewallRule,
                                  createPortForwardingRule)
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import (cleanup_resources,
                              wait_until)
from marvin.lib.base import (Account,
                             VirtualMachine,
                             Network,
                             FileShare,
                             ServiceOffering,
                             NetworkOffering,
                             DiskOffering,
                             PublicIPAddress,
                             )
from marvin.lib.common import (get_domain,
                               get_zone,
                               get_suitable_test_template)

class TestFileShareLifecycle(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestFileShareLifecycle, cls).getClsTestClient()
        cls.apiclient = cls.testClient.getApiClient()
        cls.services = cls.testClient.getParsedTestDataConfig()

        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, cls.testClient.getZoneForTests())
        cls._cleanup = []

        cls.services["service_offering"]["name"] = 'FSVM offering';
        cls.services["service_offering"]["offerha"] = True;
        cls.services["service_offering"]["cpunumber"] = 2;
        cls.services["service_offering"]["cpuspeed"] = 500;
        cls.services["service_offering"]["memory"] = 1024;
        cls.service_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)

        cls.services["disk_offering"]["disksize"] = 1;
        cls.disk_offering = DiskOffering.create(
            cls.apiclient,
            cls.services["disk_offering"],
            custom=True
        )
        cls._cleanup.append(cls.disk_offering)

        cls.useraccount = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.useraccount)

        cls.adminaccount = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=cls.domain.id,
            admin=True
        )
        cls._cleanup.append(cls.adminaccount)

        cls.network_offering_isolated = NetworkOffering.create(
            cls.apiclient,
            cls.services["isolated_network_offering"]
        )
        cls.network_offering_isolated.update(cls.apiclient, state='Enabled')
        cls._cleanup.append(cls.network_offering_isolated)
        cls.services["network"]["name"] = "Test Network Isolated"
        cls.user_network = Network.create(
            cls.apiclient,
            cls.services["network"],
            networkofferingid=cls.network_offering_isolated.id,
            domainid=cls.domain.id,
            accountid=cls.adminaccount.name,
            zoneid=cls.zone.id
        )
        cls._cleanup.insert(0, cls.user_network)
        cls.public_ipaddress = None
        cls.sshpublicport = 1000

        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.template = get_suitable_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor)

        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.services["diskofferingid"] = cls.disk_offering.id
        cls.services["serviceofferingid"] = cls.service_offering.id
        cls.services["networkid"] = cls.user_network.id
        cls.services["account"] = cls.adminaccount.name

        cls.fileshare = FileShare.create(
            cls.apiclient,
            cls.services,
            name='Test File Share 1',
            size=1,
            format='XFS',
            provider='STORAGEFSVM'
        )
        cls._cleanup.insert(0, cls.fileshare)

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    def setUpSNAT(self, network):
        public_ipaddress = PublicIPAddress.list(
            self.apiclient,
            account=self.adminaccount.name,
            domainid=self.domain.id,
            associatednetworkid=network.id
        )
        createFwRule = createFirewallRule.createFirewallRuleCmd()
        createFwRule.cidrlist = "0.0.0.0/0"
        createFwRule.startport = 22
        createFwRule.endport = 22
        createFwRule.ipaddressid = public_ipaddress[0].id
        createFwRule.protocol = "tcp"
        self.apiclient.createFirewallRule(createFwRule)
        return public_ipaddress[0]

    def setUpPortForwarding(self, virtualmachineid):
        createPfRule = createPortForwardingRule.createPortForwardingRuleCmd()
        self.sshpublicport += 1
        createPfRule.publicport = self.sshpublicport
        createPfRule.privateport = 22
        createPfRule.virtualmachineid = virtualmachineid
        createPfRule.ipaddressid = self.public_ipaddress.id
        createPfRule.protocol = "tcp"
        self.apiclient.createPortForwardingRule(createPfRule)
        self.debug("Successfully programmed PF rule for :%s"%self.public_ipaddress.ipaddress)
        return createPfRule.publicport

    def waitForNFSShare(self, ssh_client, fileshare_ip):
        def showmount(ssh_client, fileshare_ip):
            cmd = "showmount -e " + fileshare_ip  + " | grep /mnt/fs/share"
            res = ssh_client.execute(cmd)[0]
            if res == "/mnt/fs/share *":
                sleep(10)
                return True, res
            else:
                return False, None
        retry_interval = 15
        num_tries = 20
        wait_result, return_val = wait_until(retry_interval, num_tries, showmount, ssh_client, fileshare_ip)
        self.assertTrue(return_val)

    def getSSHClient(self, virtual_machine, port):
        try:
            ssh_client = virtual_machine.get_ssh_client(ipaddress=self.public_ipaddress.ipaddress, port=port)
        except Exception as e:
            self.fail("SSH failed for virtual machine: %s - %s" % (virtual_machine.ipaddress, e))
        return ssh_client

    def mountFileShareOnVM(self, ssh_client, fileshare):
        fileshare_ip = fileshare.nic[0].ipaddress
        self.waitForNFSShare(ssh_client, fileshare_ip)
        ssh_client.execute("mkdir /mnt/fs1")
        cmd = "mount -t nfs -o rw,sync " + fileshare_ip  + ":/mnt/fs/share /mnt/fs1"
        ssh_client.execute(cmd)

    @attr( tags=[ "advanced", "advancedns", "smokes"], required_hardware="true")
    def test_mount_file_share(self):
        """Mount File Share on two VMs and match contents
        """

        self.virtual_machine1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=self.user_network.id,
            domainid=self.domain.id,
            accountid=self.adminaccount.name,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.virtual_machine1)

        if self.public_ipaddress == None:
            self.public_ipaddress = self.setUpSNAT(self.user_network)
        self.debug("Public ipaddress: " + self.public_ipaddress.ipaddress)

        port = self.setUpPortForwarding(self.virtual_machine1.id)
        ssh_client = self.getSSHClient(self.virtual_machine1, port)
        self.assertIsNotNone(ssh_client)

        self.mountFileShareOnVM(ssh_client, self.fileshare)
        ssh_client.execute("df -Th /mnt/fs1")
        ssh_client.execute("touch /mnt/fs1/test")

        self.virtual_machine2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            templateid=self.template.id,
            serviceofferingid=self.service_offering.id,
            networkids=self.user_network.id,
            domainid=self.domain.id,
            accountid=self.adminaccount.name,
            zoneid=self.zone.id
        )
        self.cleanup.append(self.virtual_machine2)

        port = self.setUpPortForwarding(self.virtual_machine2.id)
        ssh_client = self.getSSHClient(self.virtual_machine2, port)
        self.assertIsNotNone(ssh_client)

        self.mountFileShareOnVM(ssh_client, self.fileshare)
        ssh_client.execute("df -Th /mnt/fs1")
        result = ssh_client.execute("ls /mnt/fs1/test")
        self.assertEqual(result[0], "/mnt/fs1/test")
