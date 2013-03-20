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
""" Tests for Blocker bugs
"""
import marvin
from nose.plugins.attrib import attr
from marvin.integration.lib.base import *
from marvin.integration.lib.utils import *
from marvin.integration.lib.common import *

#Import Local Modules
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.remoteSSHClient import remoteSSHClient


class Services:
    """Test Services
    """

    def __init__(self):
        self.services = {
                         "domain": {
                                   "name": "Domain",
                        },
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
                                    "memory": 64,       # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small",
                                    "name": "Small",
                                    "disksize": 1
                        },
                        "virtual_machine": {
                                    "displayname": "Test VM",
                                    "username": "root",
                                    "password": "password",
                                    "ssh_port": 22,
                                    "hypervisor": 'XenServer',
                                    "privateport": 22,
                                    "publicport": 22,
                                    "protocol": 'TCP',
                        },
                        "volume": {
                                   "diskname": "APP Data Volume",
                                   "size": 1,   # in GBs
                                   "diskdevice": "/dev/xvdb",   # Data Disk
                        },
                        "templates": {
                                    "displaytext": 'Template from snapshot',
                                    "name": 'Template from snapshot',
                                    "ostype": 'CentOS 5.3 (64-bit)',
                                    "templatefilter": 'self',
                                    "url": "http://download.cloud.com/releases/2.0.0/UbuntuServer-10-04-64bit.vhd.bz2",
                                    "hypervisor": 'XenServer',
                                    "format": 'VHD',
                                    "isfeatured": True,
                                    "ispublic": True,
                                    "isextractable": True,
                                    "passwordenabled": True,
                        },
                        "paths": {
                                    "mount_dir": "/mnt/tmp",
                                    "sub_dir": "test",
                                    "sub_lvl_dir1": "test1",
                                    "sub_lvl_dir2": "test2",
                                    "random_data": "random.data",
                        },
                        "static_nat": {
                                    "startport": 22,
                                    "endport": 22,
                                    "protocol": "TCP"
                        },
                        "ostype": 'CentOS 5.3 (64-bit)',
                        # Cent OS 5.3 (64 bit)
                        "sleep": 60,
                        "mode": 'advanced',
                        # Networking mode, Advanced, Basic
                     }


class TestSnapshots(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestSnapshots, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["volume"]["zoneid"] = cls.zone.id

        cls.services["template"] = cls.template.id
        cls.services["zoneid"] = cls.zone.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.account.name

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.virtual_machine = VirtualMachine.create(
                                cls.api_client,
                                cls.services["virtual_machine"],
                                templateid=cls.template.id,
                                accountid=cls.account.account.name,
                                domainid=cls.account.account.domainid,
                                serviceofferingid=cls.service_offering.id,
                                mode=cls.services["mode"]
                                )

        cls._cleanup = [
                        cls.service_offering,
                        cls.disk_offering,
                        cls.account,
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

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns"])
    def test_01_volume_from_snapshot(self):
        """TS_BUG_001-Test Creating snapshot from volume having spaces in name(KVM)
        """


        # Validate the following
        #1. Create a virtual machine and data volume
        #2. Attach data volume to VM
        #3. Login to machine; create temp/test directories on data volume
        #4. Snapshot the Volume
        #5. Create another Volume from snapshot
        #6. Mount/Attach volume to another server
        #7. Compare data

        random_data_0 = random_gen(100)
        random_data_1 = random_gen(100)

        volume = Volume.create(
                               self.apiclient,
                               self.services["volume"],
                               zoneid=self.zone.id,
                               account=self.account.account.name,
                               domainid=self.account.account.domainid,
                               diskofferingid=self.disk_offering.id
                               )
        self.debug("Created volume with ID: %s" % volume.id)
        self.virtual_machine.attach_volume(
                                           self.apiclient,
                                           volume
                                           )
        self.debug("Attach volume: %s to VM: %s" %
                                (volume.id, self.virtual_machine.id))
        try:
            ssh_client = self.virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail("SSH failed for VM: %s" %
                      self.virtual_machine.ipaddress)

        self.debug("Formatting volume: %s to ext3" % volume.id)
        #Format partition using ext3
        format_volume_to_ext3(
                              ssh_client,
                              self.services["volume"]["diskdevice"]
                              )
        cmds = [
                    "mkdir -p %s" % self.services["paths"]["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["volume"]["diskdevice"],
                                      self.services["paths"]["mount_dir"]
                                      ),
                    "mkdir -p %s/%s/{%s,%s} " % (
                                    self.services["paths"]["mount_dir"],
                                    self.services["paths"]["sub_dir"],
                                    self.services["paths"]["sub_lvl_dir1"],
                                    self.services["paths"]["sub_lvl_dir2"]
                                    ),
                    "echo %s > %s/%s/%s/%s" % (
                                    random_data_0,
                                    self.services["paths"]["mount_dir"],
                                    self.services["paths"]["sub_dir"],
                                    self.services["paths"]["sub_lvl_dir1"],
                                    self.services["paths"]["random_data"]
                                    ),
                    "echo %s > %s/%s/%s/%s" % (
                                    random_data_1,
                                    self.services["paths"]["mount_dir"],
                                    self.services["paths"]["sub_dir"],
                                    self.services["paths"]["sub_lvl_dir2"],
                                    self.services["paths"]["random_data"]
                                    ),
                ]
        for c in cmds:
            self.debug("Command: %s" % c)
            ssh_client.execute(c)

        # Unmount the Sec Storage
        cmds = [
                    "umount %s" % (self.services["paths"]["mount_dir"]),
                ]
        for c in cmds:
            self.debug("Command: %s" % c)
            ssh_client.execute(c)

        list_volume_response = Volume.list(
                                    self.apiclient,
                                    virtualmachineid=self.virtual_machine.id,
                                    type='DATADISK',
                                    listall=True
                                    )

        self.assertEqual(
                         isinstance(list_volume_response, list),
                         True,
                         "Check list volume response for valid data"
                         )
        volume_response = list_volume_response[0]
        #Create snapshot from attached volume
        snapshot = Snapshot.create(
                                   self.apiclient,
                                   volume_response.id,
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
                                   )
        self.debug("Created snapshot: %s" % snapshot.id)
        #Create volume from snapshot
        volume_from_snapshot = Volume.create_from_snapshot(
                                        self.apiclient,
                                        snapshot.id,
                                        self.services["volume"],
                                        account=self.account.account.name,
                                        domainid=self.account.account.domainid
                                        )
        self.debug("Created Volume: %s from Snapshot: %s" % (
                                            volume_from_snapshot.id,
                                            snapshot.id))
        volumes = Volume.list(
                                self.apiclient,
                                id=volume_from_snapshot.id
                                )
        self.assertEqual(
                            isinstance(volumes, list),
                            True,
                            "Check list response returns a valid list"
                        )

        self.assertNotEqual(
                            len(volumes),
                            None,
                            "Check Volume list Length"
                      )
        self.assertEqual(
                        volumes[0].id,
                        volume_from_snapshot.id,
                        "Check Volume in the List Volumes"
                    )
        #Attaching volume to new VM
        new_virtual_machine = VirtualMachine.create(
                                    self.apiclient,
                                    self.services["virtual_machine"],
                                    templateid=self.template.id,
                                    accountid=self.account.account.name,
                                    domainid=self.account.account.domainid,
                                    serviceofferingid=self.service_offering.id,
                                    mode=self.services["mode"]
                                )
        self.debug("Deployed new VM for account: %s" % self.account.account.name)
        self.cleanup.append(new_virtual_machine)

        self.debug("Attaching volume: %s to VM: %s" % (
                                            volume_from_snapshot.id,
                                            new_virtual_machine.id
                                            ))

        cmd = attachVolume.attachVolumeCmd()
        cmd.id = volume_from_snapshot.id
        cmd.virtualmachineid = new_virtual_machine.id
        self.apiclient.attachVolume(cmd)

        try:
            #Login to VM to verify test directories and files
            ssh = new_virtual_machine.get_ssh_client()

            cmds = [
                    "mkdir -p %s" % self.services["paths"]["mount_dir"],
                    "mount %s1 %s" % (
                                      self.services["volume"]["diskdevice"],
                                      self.services["paths"]["mount_dir"]
                                      ),
               ]

            for c in cmds:
                self.debug("Command: %s" % c)
                ssh.execute(c)

            returned_data_0 = ssh.execute(
                            "cat %s/%s/%s/%s" % (
                                    self.services["paths"]["mount_dir"],
                                    self.services["paths"]["sub_dir"],
                                    self.services["paths"]["sub_lvl_dir1"],
                                    self.services["paths"]["random_data"]
                            ))
            returned_data_1 = ssh.execute(
                            "cat %s/%s/%s/%s" % (
                                    self.services["paths"]["mount_dir"],
                                    self.services["paths"]["sub_dir"],
                                    self.services["paths"]["sub_lvl_dir2"],
                                    self.services["paths"]["random_data"]
                            ))
        except Exception as e:
            self.fail("SSH access failed for VM: %s" %
                                new_virtual_machine.ipaddress)
        #Verify returned data
        self.assertEqual(
                random_data_0,
                returned_data_0[0],
                "Verify newly attached volume contents with existing one"
                )
        self.assertEqual(
                random_data_1,
                returned_data_1[0],
                "Verify newly attached volume contents with existing one"
                )
        # Unmount the Sec Storage
        cmds = [
                    "umount %s" % (self.services["paths"]["mount_dir"]),
                ]
        for c in cmds:
            self.debug("Command: %s" % c)
            ssh_client.execute(c)
        return


class TestTemplate(cloudstackTestCase):

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
        cls.api_client = super(TestTemplate, cls).getClsTestClient().getApiClient()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["templates"]["zoneid"] = cls.zone.id

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
            cls.api_client = super(TestTemplate, cls).getClsTestClient().getApiClient()
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(tags = ["advanced", "advancedns", "basic", "sg"])
    def test_01_create_template(self):
        """TS_BUG_002-Test to create and deploy VM using password enabled template
        """


        # Validate the following:
        #1. Create a password enabled template
        #2. Deploy VM using this template
        #3. Deploy VM should return password set in template.

        self.debug("Registering a new template")
        # Register new template
        template = Template.register(
                                    self.apiclient,
                                    self.services["templates"],
                                    zoneid=self.zone.id,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.debug("Registering template with ID: %s" % template.id)
        try:
            # Wait for template to download
            template.download(self.apiclient)
        except Exception as e:
            self.fail("Exception while downloading template %s: %s"\
                      % (template.id, e))

        self.cleanup.append(template)

        # Wait for template status to be changed across
        time.sleep(self.services["sleep"])

        list_template_response = Template.list(
                                            self.apiclient,
                                            templatefilter=\
                                            self.services["templates"]["templatefilter"],
                                            id=template.id,
                                            zoneid=self.zone.id
                                            )

        self.assertEqual(
                            isinstance(list_template_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        #Verify template response to check whether template added successfully
        self.assertNotEqual(
                            len(list_template_response),
                            0,
                            "Check template available in List Templates"
                        )
        template_response = list_template_response[0]

        self.assertEqual(
                            template_response.isready,
                            True,
                            "Check display text of newly created template"
                        )

        # Deploy new virtual machine using template
        virtual_machine = VirtualMachine.create(
                                 self.apiclient,
                                 self.services["virtual_machine"],
                                 templateid=template.id,
                                 accountid=self.account.account.name,
                                 domainid=self.account.account.domainid,
                                 serviceofferingid=self.service_offering.id,
                                 )
        self.debug("Deployed VM with ID: %s " % virtual_machine.id)
        self.assertEqual(
                         hasattr(virtual_machine, "password"),
                         True,
                         "Check if the deployed VM returned a password"
                        )
        return


class TestNATRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = super(TestNATRules, cls).getClsTestClient().getApiClient()
        cls.services = Services().services

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        #Create an account, network, VM and IP addresses
        cls.account = Account.create(
                                cls.api_client,
                                cls.services["account"],
                                admin=True,
                                domainid=cls.domain.id
                                )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.service_offering = ServiceOffering.create(
                                cls.api_client,
                                cls.services["service_offering"]
                                )
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    domainid=cls.account.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                )
        cls.public_ip = PublicIPAddress.create(
                                    cls.api_client,
                                    accountid=cls.account.account.name,
                                    zoneid=cls.zone.id,
                                    domainid=cls.account.account.domainid,
                                    services=cls.services["virtual_machine"]
                                    )
        cls._cleanup = [
                        cls.virtual_machine,
                        cls.account,
                        cls.service_offering
                        ]

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestNATRules, cls).getClsTestClient().getApiClient()
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def tearDown(self):
        cleanup_resources(self.apiclient, self.cleanup)
        return

    @attr(tags = ["advanced"])
    def test_01_firewall_rules_port_fw(self):
        """"Checking firewall rules deletion after static NAT disable"""


        # Validate the following:
        #1. Enable static NAT for a VM
        #2. Open up some ports. At this point there will be new rows in the
        #   firewall_rules table.
        #3. Disable static NAT for the VM.
        #4. Check fire wall rules are deleted from firewall_rules table.

        public_ip = self.public_ip.ipaddress

        # Enable Static NAT for VM
        StaticNATRule.enable(
                             self.apiclient,
                             public_ip.id,
                             self.virtual_machine.id
                            )
        self.debug("Enabled static NAT for public IP ID: %s" %
                                                    public_ip.id)
        #Create Static NAT rule
        nat_rule = StaticNATRule.create(
                        self.apiclient,
                        self.services["static_nat"],
                        public_ip.id
                        )
        self.debug("Created Static NAT rule for public IP ID: %s" %
                                                    public_ip.id)
        list_rules_repsonse = StaticNATRule.list(
                                                 self.apiclient,
                                                 id=nat_rule.id
                                                )
        self.assertEqual(
                            isinstance(list_rules_repsonse, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            len(list_rules_repsonse),
                            0,
                            "Check IP Forwarding Rule is created"
                            )
        self.assertEqual(
                            list_rules_repsonse[0].id,
                            nat_rule.id,
                            "Check Correct IP forwarding Rule is returned"
                        )
        # Verify the entries made in firewall_rules tables
        self.debug(
                   "select id from user_ip_address where uuid = '%s';" \
                    % public_ip.id
                  )
        qresultset = self.dbclient.execute(
                        "select id from user_ip_address where uuid = '%s';" \
                        % public_ip.id
                        )
        self.assertEqual(
                            isinstance(qresultset, list),
                            True,
                            "Check database query returns a valid data"
                        )

        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )
        qresult = qresultset[0]
        public_ip_id = qresult[0]
        # Verify the entries made in firewall_rules tables
        self.debug(
                   "select id, state from firewall_rules where ip_address_id = '%s';" \
                    % public_ip_id
                  )
        qresultset = self.dbclient.execute(
                        "select id, state from firewall_rules where ip_address_id = '%s';" \
                        % public_ip_id
                        )
        self.assertEqual(
                            isinstance(qresultset, list),
                            True,
                            "Check database query returns a valid data for firewall rules"
                        )

        self.assertNotEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )

        for qresult in qresultset:
            self.assertEqual(
                            qresult[1],
                            'Active',
                            "Check state of the static NAT rule in database"
                            )

        nat_rule.delete(self.apiclient)

        list_rules_repsonse = StaticNATRule.list(
                                                    self.apiclient,
                                                    id=nat_rule.id
                                                    )

        self.assertEqual(
                            list_rules_repsonse,
                            None,
                            "Check Port Forwarding Rule is deleted"
                            )

        # Verify the entries made in firewall_rules tables
        self.debug(
                   "select id, state from firewall_rules where ip_address_id = '%s';" \
                    % public_ip.id
                  )
        qresultset = self.dbclient.execute(
                        "select id, state from firewall_rules where ip_address_id = '%s';" \
                        % public_ip.id
                        )

        self.assertEqual(
                            len(qresultset),
                            0,
                            "Check DB Query result set"
                            )
        return


class TestRouters(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestRouters, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )

        # Create an account, domain etc
        cls.domain = Domain.create(
                                   cls.api_client,
                                   cls.services["domain"],
                                   )
        cls.admin_account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            admin=True,
                            domainid=cls.domain.id
                            )

        cls.user_account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )

        cls._cleanup = [
                        cls.service_offering,
                        cls.admin_account,
                        cls.user_account,
                        cls.domain
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

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created instance, users etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags = ["advanced", "advancedns"])
    def test_01_list_routers_admin(self):
        """TS_BUG_007-Check listRouters() using Admin User
        """


        # Validate the following
        # 1. PreReq: have rounters that are owned by other account
        # 2. Create domain and create accounts in that domain
        # 3. Create one VM for each account
        # 4. Using Admin , run listRouters. It should return all the routers

        vm_1 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.admin_account.account.name,
                                  domainid=self.admin_account.account.domainid,
                                  serviceofferingid=self.service_offering.id
                                  )
        self.debug("Deployed VM with ID: %s" % vm_1.id)
        vm_2 = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.user_account.account.name,
                                  domainid=self.user_account.account.domainid,
                                  serviceofferingid=self.service_offering.id
                                  )
        self.debug("Deployed VM with ID: %s" % vm_2.id)
        routers = list_routers(
                               self.apiclient,
                               account=self.admin_account.account.name,
                               domainid=self.admin_account.account.domainid,
                               )
        self.assertEqual(
                            isinstance(routers, list),
                            True,
                            "Check list response returns a valid list"
                        )
        # ListRouter Should return 2 records
        self.assertEqual(
                             len(routers),
                             1,
                             "Check list router response"
                             )
        for router in routers:
            self.assertEqual(
                        router.state,
                        'Running',
                        "Check list router response for router state"
                    )
        return


class TestRouterRestart(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.api_client = super(TestRouterRestart, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id

        #Create an account, network, VM and IP addresses
        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     domainid=cls.domain.id
                                     )
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.vm_1 = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    domainid=cls.account.account.domainid,
                                    serviceofferingid=cls.service_offering.id
                                    )
        cls.cleanup = [
                       cls.vm_1,
                       cls.account,
                       cls.service_offering
                       ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestRouterRestart, cls).getClsTestClient().getApiClient()
            #Clean up, terminate the created templates
            cleanup_resources(cls.api_client, cls.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        return

    @attr(tags = ["advanced", "basic", "sg", "advancedns", "eip"])
    def test_01_restart_network_cleanup(self):
        """TS_BUG_008-Test restart network
        """


        # Validate the following
        # 1. When cleanup = true, router is destroyed and a new one created
        # 2. New router will have new publicIp and linkLocalIp and
        #    all it's services should resume

        # Find router associated with user account
        list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]

        #Store old values before restart
        old_linklocalip = router.linklocalip

        timeout = 10
        # Network should be in Implemented or Setup stage before restart
        while True:
            networks = Network.list(
                                 self.apiclient,
                                 account=self.account.account.name,
                                 domainid=self.account.account.domainid
                                 )
            network = networks[0]
            if network.state in ["Implemented", "Setup"]:
                break
            elif timeout == 0:
                break
            else:
                time.sleep(60)
                timeout = timeout - 1

        self.debug("Restarting network: %s" % network.id)
        cmd = restartNetwork.restartNetworkCmd()
        cmd.id = network.id
        cmd.cleanup = True
        self.apiclient.restartNetwork(cmd)

        # Get router details after restart
        list_router_response = list_routers(
                                    self.apiclient,
                                    account=self.account.account.name,
                                    domainid=self.account.account.domainid
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]

        self.assertNotEqual(
                            router.linklocalip,
                            old_linklocalip,
                            "Check link-local IP after restart"
                        )
        return


class TestTemplates(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.services = Services().services
        cls.api_client = super(TestTemplates, cls).getClsTestClient().getApiClient()

        # Get Zone, templates etc
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)

        template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )

        cls.services["account"] = cls.account.account.name
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                        )

        # create virtual machine
        cls.virtual_machine = VirtualMachine.create(
                                    cls.api_client,
                                    cls.services["virtual_machine"],
                                    templateid=template.id,
                                    accountid=cls.account.account.name,
                                    domainid=cls.account.account.domainid,
                                    serviceofferingid=cls.service_offering.id,
                                    )
        #Stop virtual machine
        cls.virtual_machine.stop(cls.api_client)

        #Wait before server has be successfully stopped
        time.sleep(30)
        list_volume = Volume.list(
                                   cls.api_client,
                                   virtualmachineid=cls.virtual_machine.id,
                                   type='ROOT',
                                   listall=True
                                   )
        try:
            if isinstance(list_volume, list):
                cls.volume = list_volume[0]
        except Exception as e:
            raise Exception("Warning: Exception during setup : %s" % e)

        cls._cleanup = [
                        cls.service_offering,
                        cls.account,
                        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cls.api_client = super(TestTemplates, cls).getClsTestClient().getApiClient()
            #Cleanup created resources such as templates and VMs
            cleanup_resources(cls.api_client, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

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

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "basic", "sg", "eip"])
    def test_01_check_template_size(self):
        """TS_BUG_009-Test the size of template created from root disk
        """


        # Validate the following:
        # 1. Deploy new VM using the template created from Volume
        # 2. VM should be in Up and Running state

        #Create template from volume
        template = Template.create(
                                   self.apiclient,
                                   self.services["templates"],
                                   self.volume.id,
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
                                )
        self.debug("Creating template with ID: %s" % template.id)
        # Volume and Template Size should be same
        self.assertEqual(
                             template.size,
                             self.volume.size,
                             "Check if size of template and volume are same"
                             )
        return

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "basic", "sg", "eip"])
    def test_02_check_size_snapshotTemplate(self):
        """TS_BUG_010-Test check size of snapshot and template
        """


        # Validate the following
        # 1. Deploy VM using default template, small service offering
        #    and small data disk offering.
        # 2. Perform snapshot on the root disk of this VM.
        # 3. Create a template from snapshot.
        # 4. Check the size of snapshot and template

        # Create a snapshot from the ROOTDISK
        snapshot = Snapshot.create(
                                   self.apiclient,
                                   self.volume.id,
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
                                   )
        self.debug("Created snapshot with ID: %s" % snapshot.id)
        snapshots = Snapshot.list(
                                   self.apiclient,
                                   id=snapshot.id
                                   )
        self.assertEqual(
                            isinstance(snapshots, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            snapshots,
                            None,
                            "Check if result exists in list snapshots call"
                            )
        self.assertEqual(
                            snapshots[0].id,
                            snapshot.id,
                            "Check snapshot id in list resources call"
                        )

        # Generate template from the snapshot
        template = Template.create_from_snapshot(
                                    self.apiclient,
                                    snapshot,
                                    self.services["templates"]
                                    )
        self.cleanup.append(template)

        self.debug("Created template from snapshot with ID: %s" % template.id)
        templates = Template.list(
                                self.apiclient,
                                templatefilter=\
                                self.services["templates"]["templatefilter"],
                                id=template.id
                                )
        self.assertEqual(
                            isinstance(templates, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            templates,
                            None,
                            "Check if result exists in list item call"
                            )

        self.assertEqual(
                            templates[0].isready,
                            True,
                            "Check new template state in list templates call"
                        )
        # check size of template with that of snapshot
        self.assertEqual(
                            templates[0].size,
                            self.volume.size,
                            "Check if size of snapshot and template matches"
                        )
        return

    @attr(speed = "slow")
    @attr(tags = ["advanced", "advancedns", "basic", "sg", "eip"])
    def test_03_resuse_template_name(self):
        """TS_BUG_011-Test Reusing deleted template name
        """


        # Validate the following
        # 1. Deploy VM using default template, small service offering
        #    and small data disk offering.
        # 2. Perform snapshot on the root disk of this VM.
        # 3. Create a template from snapshot.
        # 4. Delete the template and create a new template with same name
        # 5. Template should be created succesfully

        # Create a snapshot from the ROOTDISK
        snapshot = Snapshot.create(
                                   self.apiclient,
                                   self.volume.id,
                                   account=self.account.account.name,
                                   domainid=self.account.account.domainid
                                   )
        self.debug("Created snapshot with ID: %s" % snapshot.id)
        snapshots = Snapshot.list(
                                   self.apiclient,
                                   id=snapshot.id
                                   )
        self.assertEqual(
                            isinstance(snapshots, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            snapshots,
                            None,
                            "Check if result exists in list snapshots call"
                            )
        self.assertEqual(
                            snapshots[0].id,
                            snapshot.id,
                            "Check snapshot id in list resources call"
                        )

        # Generate template from the snapshot
        template = Template.create_from_snapshot(
                                    self.apiclient,
                                    snapshot,
                                    self.services["templates"],
                                    random_name=False
                                    )
        self.debug("Created template from snapshot: %s" % template.id)
        templates = Template.list(
                                self.apiclient,
                                templatefilter=\
                                self.services["templates"]["templatefilter"],
                                id=template.id
                                )
        self.assertEqual(
                            isinstance(templates, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            templates,
                            None,
                            "Check if result exists in list item call"
                            )

        self.assertEqual(
                            templates[0].isready,
                            True,
                            "Check new template state in list templates call"
                        )

        self.debug("Deleting template: %s" % template.id)
        template.delete(self.apiclient)

        # Wait for some time to ensure template state is reflected in other calls
        time.sleep(self.services["sleep"])

        # Generate template from the snapshot
        self.debug("Creating template from snapshot: %s with same name" %
                                                                template.id)
        template = Template.create_from_snapshot(
                                    self.apiclient,
                                    snapshot,
                                    self.services["templates"],
                                    random_name=False
                                    )

        templates = Template.list(
                                self.apiclient,
                                templatefilter=\
                                self.services["templates"]["templatefilter"],
                                id=template.id
                                )
        self.assertEqual(
                            isinstance(templates, list),
                            True,
                            "Check list response returns a valid list"
                        )
        self.assertNotEqual(
                            templates,
                            None,
                            "Check if result exists in list item call"
                            )

        self.assertEqual(
                            templates[0].name,
                            self.services["templates"]["name"],
                            "Check the name of the template"
                        )
        return
