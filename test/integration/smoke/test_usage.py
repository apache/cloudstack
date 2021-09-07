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
""" P1 tests for Snapshots
"""
# Import Local Modules
from marvin.codes import FAILED
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import deleteVolume
from marvin.lib.utils import (cleanup_resources,get_hypervisor_type)
from marvin.lib.base import (Account,
                             ServiceOffering,
                             NATRule,
                             VirtualMachine,
                             Snapshot,
                             Iso,
                             ImageStore,
                             LoadBalancerRule,
                             PublicIPAddress,
                             DiskOffering,
                             Template,
                             VpnUser,
                             Vpn,
                             Volume)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_suitable_test_template,
                               find_storage_pool_type)



class Services:

    """Test Snapshots Services
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
            "service_offering": {
                "name": "Tiny Instance",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100,  # in MHz
                "memory": 128,  # In MBs
            },
            "disk_offering": {
                "displaytext": "Small",
                "name": "Small",
                "disksize": 1
            },
            "volume": {
                "diskname": "TestDiskServ",
            },
            "server": {
                "displayname": "TestVM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "templates": {
                "displaytext": 'Template',
                "name": 'Template',
                "ostype": 'CentOS 5.3 (64-bit)',
                "templatefilter": 'self',
                "url": "http://download.cloudstack.org/releases/2.0.0/UbuntuServer-10-04-64bit.qcow2.bz2"
            },
            "iso": {
                "displaytext": "Test ISO",
                "name": "Test ISO",
                "url": "http://people.apache.org/~tsp/dummy.iso",
                # Source URL where ISO is located
                "isextractable": True,
                "isfeatured": True,
                "ispublic": True,
                "ostype": 'CentOS 5.3 (64-bit)',
            },
            "lbrule": {
                "name": "SSH",
                "alg": "roundrobin",
                # Algorithm used for load balancing
                "openfirewall":"false",
                "privateport": 22,
                "publicport": 2222,
            },
            "natrule": {
                "privateport": 22,
                "publicport": 22,
                "protocol": "TCP"
            },
            "vpn_user": {
                "username": "test",
                "password": "test",
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            # Cent OS 5.3 (64 bit)
            "sleep": 60,
            "timeout": 10,
        }


class TestVmUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVmUsage, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        template = get_suitable_test_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id

        # Create Account, VMs etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["server"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup = [
            cls.service_offering,
            cls.account,
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_01_vm_usage(self):
        """Test Create/Destroy VM and verify usage calculation
        """
        # Validate the following
        # 1. Create a VM. Verify usage_events table contains VM .create,
        #    VM.start , Network.offering.assign , Volume.create events
        # 2. Stop the VM. Verify usage_events table contains
        #    network.offerings.remove ,VM .stop Events for the created account.
        # 3. Destroy the VM after some time. Verify usage_events table contains
        #    VM.Destroy and volume .delete Event for the created account
        # 4. Delete the account

        try:
            self.debug("Stopping the VM: %s" % self.virtual_machine.id)
            # Stop the VM
            self.virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)

        try:
            # Destroy the VM
            self.debug("Destroying the VM: %s" % self.virtual_machine.id)
            self.virtual_machine.delete(self.apiclient, expunge=True)
        except Exception as e:
            self.fail("Failed to destroy VM: %s" % e)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)
        # Check if VM.CREATE, VM.DESTROY events present in usage_event table
        self.assertEqual(
            qresult.count('VM.START'),
            1,
            "Check VM.START event in events table"
        )

        self.assertEqual(
            qresult.count('NETWORK.OFFERING.ASSIGN'),
            1,
            "Check NETWORK.OFFERING.ASSIGN in events table"
        )
        self.assertEqual(
            qresult.count('VM.CREATE'),
            1,
            "Check VM.CREATE in list events"
        )

        self.assertEqual(
            qresult.count('VOLUME.CREATE'),
            1,
            "Check VOLUME.CREATE in events table"
        )

        self.assertEqual(
            qresult.count('VM.STOP'),
            1,
            "Check VM.STOP in events table"
        )

        self.assertEqual(
            qresult.count('NETWORK.OFFERING.REMOVE'),
            1,
            "Check NETWORK.OFFERING.REMOVE in list events"
        )

        self.assertEqual(
            qresult.count('VM.DESTROY'),
            1,
            "Check VM.DESTROY in events table"
        )

        self.assertEqual(
            qresult.count('VOLUME.DELETE'),
            1,
            "Check VOLUME.DELETE in events table"
        )
        return


class TestPublicIPUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestPublicIPUsage, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        template = get_suitable_test_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id

        # Create VMs, Assign Public IP etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["server"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )

        cls.public_ip = PublicIPAddress.create(
            cls.api_client,
            accountid=cls.virtual_machine.account,
            zoneid=cls.virtual_machine.zoneid,
            domainid=cls.virtual_machine.domainid,
            services=cls.services["server"]
        )
        cls._cleanup = [
            cls.service_offering,
            cls.account,
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_01_public_ip_usage(self):
        """Test Assign new IP and verify usage calculation
        """
        # Validate the following
        # 1. Aquire a IP for the network of this account. Verify usage_event
        #    table has  Acquire IP event for  the IP for this account
        # 2. Release one of the IP of this account. Verify usage_event table
        #    has IP.Release event for released IP for this account
        # 3. Delete the newly created account

        self.debug("Deleting public IP: %s" %
                   self.public_ip.ipaddress)

        # Release one of the IP
        self.public_ip.delete(self.apiclient)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = str(qresultset)
        # Check if NET.IPASSIGN, NET.IPRELEASE events present in usage_event
        # table
        self.assertEqual(
            qresult.count('NET.IPASSIGN') > 0,
            True,
            "Check NET.IPASSIGN event in events table"
        )

        self.assertEqual(
            qresult.count('NET.IPRELEASE') > 0,
            True,
            "Check NET.IPRELEASE in events table"
        )
        return


class TestVolumeUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVolumeUsage, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedStorageType = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.api_client, storagetype='rbd'):
                cls.unsupportedStorageType = True
                return
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.services["disk_offering"]
        )
        cls._cleanup.append(cls.disk_offering)

        template = get_suitable_test_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["server"]["zoneid"] = cls.zone.id
        cls.services["server"]["diskoffering"] = cls.disk_offering.id
        cls.services["template"] = template.id

        # Create Account, VMs etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["server"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
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
        self.cleanup = []

        if self.unsupportedStorageType:
            self.skipTest("Skipping because of unsupported storage type")
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, volumes
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns"],
        required_hardware="true")
    def test_01_volume_usage(self):
        """Test Create/delete a volume and verify correct usage is recorded
        """
        # Validate the following
        # 1. Volume.create event for both root and data disk is there for the
        #    created account in cloud.usage_event table
        # 2. Stop the VM
        # 3. Detach the data disk from this VM
        # 4. Destroy the Data disk. Volume.delete event is generated for data
        #    disk of the destroyed VM
        # Upload a volume
        # Verify Volume.upload event in usage table
        # attach the uploaded volume to a VM
        # Verify Volume.create Event is there for the attached volume


        try:
            # Stop VM
            self.debug("Stopping VM with ID: %s" % self.virtual_machine.id)
            self.virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop instance: %s" % e)

        volume_response = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='DATADISK',
            listall=True)
        self.assertEqual(
            isinstance(volume_response, list),
            True,
            "Check for valid list volumes response"
        )
        data_volume = volume_response[0]

        # Detach data Disk
        self.debug("Detaching volume ID: %s VM with ID: %s" % (
            data_volume.id,
            self.virtual_machine.id
        ))
        self.virtual_machine.detach_volume(self.apiclient, data_volume)

        # Delete Data disk
        self.debug("Delete volume ID: %s" % data_volume.id)
        cmd = deleteVolume.deleteVolumeCmd()
        cmd.id = data_volume.id
        self.apiclient.deleteVolume(cmd)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)
        # Check VOLUME.CREATE, VOLUME.DESTROY events in cloud.usage_event table
        self.assertEqual(
            qresult.count('VOLUME.CREATE'),
            2,
            "Check VOLUME.CREATE event in events table"
        )

        self.assertEqual(
            qresult.count('VOLUME.DELETE'),
            1,
            "Check VOLUME.DELETE in events table"
        )
        self.hypervisor = str(get_hypervisor_type(self.apiclient)).lower()
        volume_template = self.testdata["test_templates"][self.hypervisor]
        volume_template["diskname"] = "Volume_tiny"
        volume_template["format"] = volume_template["format"].upper()
        volume_uploaded = Volume.upload(
            self.apiclient,
            volume_template,
            self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid)
        self.assertIsNotNone(volume_uploaded, "Volume creation failed")
        volume_uploaded.wait_for_upload(self.apiclient)
        # Fetch volume ID from volume_uuid
        self.debug("select id from volumes where uuid = '%s';"
                   % volume_uploaded.id)

        qresultset = self.dbclient.execute(
            "select id from volumes where uuid = '%s';"
            % volume_uploaded.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        volume_id = qresult[0]

        self.debug("select type from usage_event where volume_id = '%s';"
                   % volume_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where resource_id = '%s';"
            % volume_id
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)
        # Check VOLUME.UPLOAD event in cloud.usage_event table
        self.assertEqual(
            qresult.count('VOLUME.UPLOAD'),
            1,
            "Check VOLUME.UPLOAD event in events table"
        )
        self.virtual_machine.start(self.apiclient)
        vms = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return the valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.state,
            "Running",
            "VM state should be running after deployment"
        )
        self.virtual_machine.attach_volume(self.apiclient,volume_uploaded)
        self.debug("select type from usage_event where offering_id = 6 and volume_id = '%s';"
                   % volume_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where offering_id = 6 and resource_id = '%s';"
            % volume_id
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)
        # Check VOLUME.CREATE event in cloud.usage_event table
        self.assertEqual(
            qresult.count('VOLUME.CREATE'),
            1,
            "Check VOLUME.CREATE event in events table"
        )


        return


class TestTemplateUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestTemplateUsage, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.services["server"]["zoneid"] = cls.zone.id

        template = get_suitable_test_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["server"]["zoneid"] = cls.zone.id
        cls._cleanup = []
        try:
            cls.account = Account.create(
                cls.api_client,
                cls.services["account"],
                domainid=cls.domain.id
            )
            cls._cleanup.append(cls.account)
            cls.userapiclient = cls.testClient.getUserApiClient(
                UserName=cls.account.name,
                DomainName=cls.account.domain)
            cls.services["account"] = cls.account.name

            cls.service_offering = ServiceOffering.create(
                cls.api_client,
                cls.services["service_offering"]
            )
            cls._cleanup.append(cls.service_offering)
            # create virtual machine
            cls.virtual_machine = VirtualMachine.create(
                cls.api_client,
                cls.services["server"],
                templateid=template.id,
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                mode=cls.services["mode"]
            )

            # Stop virtual machine
            cls.virtual_machine.stop(cls.api_client)

            list_volume = Volume.list(
                cls.api_client,
                virtualmachineid=cls.virtual_machine.id,
                type='ROOT',
                listall=True)
            if isinstance(list_volume, list):
                cls.volume = list_volume[0]
            else:
                raise Exception("List Volumes failed!")
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Exception in setUpClass: %s" % e)
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, templates
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns"],
        required_hardware="false")
    def test_01_template_usage(self):
        """Test Upload/ delete a template and verify correct usage is generated
            for the template uploaded
        """
        # Validate the following
        # 1. Create a account
        # 2. Upload a template from this account. template.create event is
        #    recorded in cloud.usage_event table for this account
        # 3. Delete the template. template.delete event is recorded in
        #    cloud.usage_event tables for this account
        # 4. Destroy the account

        # Create template from Virtual machine and Volume ID
        if self.hypervisor.lower() == 'lxc':
            self.skipTest(
                "template create from volume is not supported on %s . Hence, skipping the test" % self.hypervisor)
        self.template = Template.create(
            self.userapiclient,
            self.services["templates"],
            self.volume.id,
            TestTemplateUsage.account.name,
            TestTemplateUsage.account.domainid
        )
        self.debug("Created template with ID: %s" % self.template.id)
        # Delete template
        self.template.delete(self.userapiclient)
        self.debug("Deleted template with ID: %s" % self.template.id)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)

        # Check for TEMPLATE.CREATE, TEMPLATE.DELETE in cloud.usage_event table
        self.assertEqual(
            qresult.count('TEMPLATE.CREATE'),
            1,
            "Check TEMPLATE.CREATE event in events table"
        )

        self.assertEqual(
            qresult.count('TEMPLATE.DELETE'),
            1,
            "Check TEMPLATE.DELETE in events table"
        )
        return


class TestISOUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestISOUsage, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.services["server"]["zoneid"] = cls.zone.id
        cls.services["iso"]["zoneid"] = cls.zone.id
        # Create Account, ISO image etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )
        cls.services["account"] = cls.account.name
        cls.iso = Iso.create(
            cls.api_client,
            cls.services["iso"],
            account=cls.account.name,
            domainid=cls.account.domainid
        )
        try:
            # Wait till ISO gets downloaded
            cls.iso.download(cls.api_client)
        except Exception as e:
            raise Exception("%s: Failed to download ISO: %s" % (
                e,
                cls.iso.id
            ))
        cls._cleanup = [
            cls.account,
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created ISO images
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns"],
        required_hardware="true")
    def test_01_ISO_usage(self):
        """Test Create/Delete a ISO and verify its usage is generated correctly
        """
        # Validate the following
        # 1. Create a account
        # 2. Upload a ISO from this account. ISO.create event is recorded in
        #    cloud.usage_event table for this account
        # 3. Delete the ISO. ISO.delete event is recorded in cloud.usage_event
        #    tables for this account
        # 4. Destroy the account

        # Delete the ISO
        self.debug("Deleting ISO with ID: %s" % self.iso.id)
        self.iso.delete(self.apiclient)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)
        imageStores = ImageStore.list(self.api_client, zoneid=self.zone.id)
        # Check for ISO.CREATE, ISO.DELETE events in cloud.usage_event table
        self.assertEqual(
            qresult.count('ISO.CREATE'),
            len(imageStores),
            "Check ISO.CREATE event in events table"
        )

        self.assertEqual(
            qresult.count('ISO.DELETE'),
            len(imageStores),
            "Check ISO.DELETE in events table"
        )
        return


class TestLBRuleUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestLBRuleUsage, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        template = get_suitable_test_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id

        # Create VMs, LB Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["server"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls.public_ip_1 = PublicIPAddress.create(
            cls.api_client,
            accountid=cls.virtual_machine.account,
            zoneid=cls.virtual_machine.zoneid,
            domainid=cls.virtual_machine.domainid,
            services=cls.services["server"]
        )
        cls._cleanup = [
            cls.service_offering,
            cls.account,
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, LB rules
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_01_lb_usage(self):
        """Test Create/Delete a LB rule and verify correct usage is recorded
        """
        # Validate the following
        # 1. Acquire a IP for this account. lb.rule.create event is registered
        #    for this account in cloud.usage_event table
        # 2. Create a LB rule on the IP associated with this account
        # 3. Delete the created LB rule from the account. lb.rule.delete event
        #    is registered for this account in cloud.usage_event table
        # 4. Delete this account.

        self.debug(
            "Creating load balancer rule for public IP: %s" %
            self.public_ip_1.ipaddress.id)
        # Create Load Balancer rule and assign VMs to rule
        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            self.public_ip_1.ipaddress.id,
            accountid=self.account.name
        )
        # Delete LB Rule
        self.debug("Deleting LB rule with ID: %s" % lb_rule.id)
        lb_rule.delete(self.apiclient)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)

        # Check for LB.CREATE, LB.DELETE in cloud.usage_event table
        self.assertEqual(
            qresult.count('LB.CREATE'),
            1,
            "Check LB.CREATE event in events table"
        )

        self.assertEqual(
            qresult.count('LB.DELETE'),
            1,
            "Check LB.DELETE in events table"
        )
        return


class TestSnapshotUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestSnapshotUsage, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls._cleanup = []
        cls.unsupportedHypervisor = False
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['hyperv', 'lxc']:
            cls.unsupportedHypervisor = True
            return

        template = get_suitable_test_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id

        # Create Account, VMs etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["server"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls._cleanup = [
            cls.service_offering,
            cls.account,
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
        self.cleanup = []

        if self.unsupportedHypervisor:
            self.skipTest("Snapshots are not supported on %s" %
                    self.hypervisor)
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance and snapshots
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(speed="slow")
    @attr(
        tags=[
            "advanced",
            "basic",
            "sg",
            "eip",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_01_snapshot_usage(self):
        """Test Create/Delete a manual snap shot and verify
        correct usage is recorded
        """
        # Validate the following
        # 1. Create snapshot of the root disk  for this account.Snapshot.create
        #    event is there for the created account in cloud.usage_event table
        # 2. Destroy the snapshot after some time. Snapshot.delete event is
        #    generated for the destroyed Snapshot
        # 3. Delete the account

        # Get the Root disk of VM

        if self.hypervisor.lower() in ['hyperv']:
            self.skipTest("Snapshots feature is not supported on Hyper-V")

        volumes = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True)
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check if list volumes return a valid data"
        )

        # Create a snapshot from the ROOTDISK
        self.debug("Creating snapshot from volume: %s" % volumes[0].id)
        snapshot = Snapshot.create(self.apiclient, volumes[0].id)

        # Delete snapshot Rule
        self.debug("Deleting snapshot: %s" % snapshot.id)
        snapshot.delete(self.apiclient)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check if database query returns a valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = str(qresultset)
        self.debug("Query Result: %s" % qresult)

        # Check for SNAPSHOT.CREATE, SNAPSHOT.DELETE events in
        # cloud.usage_event table
        self.assertEqual(
            qresult.count('SNAPSHOT.CREATE'),
            1,
            "Check SNAPSHOT.CREATE event in events table"
        )

        self.assertEqual(
            qresult.count('SNAPSHOT.DELETE'),
            1,
            "Check SNAPSHOT.DELETE in events table"
        )
        return


class TestNatRuleUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestNatRuleUsage, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        template = get_suitable_test_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id

        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            domainid=cls.domain.id
        )

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["server"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls.public_ip_1 = PublicIPAddress.create(
            cls.api_client,
            accountid=cls.virtual_machine.account,
            zoneid=cls.virtual_machine.zoneid,
            domainid=cls.virtual_machine.domainid,
            services=cls.services["server"]
        )
        cls._cleanup = [
            cls.service_offering,
            cls.account,
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, NAT rules
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "simulator"],
        required_hardware="false")
    def test_01_nat_usage(self):
        """Test Create/Delete a PF rule and verify correct usage is recorded
        """
        # Validate the following
        # 1. Acquire a IP for this account
        # 2. Create a PF rule on the IP associated with this account.
        #    NET.RULEADD event is registered for this account in
        #    cloud.usage_event table
        # 3. Delete the created PF rule from the account. NET.RULEDelete event
        #    is registered for this account in cloud.usage_event table
        # 4. Delete this account.

        self.debug("Creating NAT rule with public IP: %s" %
                   self.public_ip_1.ipaddress.id)
        # Create NAT rule
        nat_rule = NATRule.create(
            self.apiclient,
            self.virtual_machine,
            self.services["natrule"],
            self.public_ip_1.ipaddress.id
        )

        # Delete NAT Rule
        self.debug("Deleting NAT rule: %s" % nat_rule.id)
        nat_rule.delete(self.apiclient)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)

        # Check for NET.RULEADD, NET.RULEDELETE in cloud.usage_event table
        self.assertEqual(
            qresult.count('NET.RULEADD'),
            1,
            "Check NET.RULEADD event in events table"
        )

        self.assertEqual(
            qresult.count('NET.RULEDELETE'),
            1,
            "Check NET.RULEDELETE in events table"
        )
        return


class TestVpnUsage(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVpnUsage, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype
        cls.hypervisor = cls.testClient.getHypervisorInfo()

        template = get_suitable_test_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"],
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_suitable_test_template() failed to return template with description %s" % cls.services["ostype"]
        cls.services["server"]["zoneid"] = cls.zone.id

        cls.services["template"] = template.id
        cls._cleanup = []

        # Create Service offerings, VMs etc
        cls.account = Account.create(
            cls.api_client,
            cls.services["account"],
            admin=True,
            domainid=cls.domain.id
        )
        cls._cleanup.append(cls.account)

        cls.services["account"] = cls.account.name

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.virtual_machine = VirtualMachine.create(
            cls.api_client,
            cls.services["server"],
            templateid=template.id,
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.service_offering.id
        )
        cls.public_ip = PublicIPAddress.create(
            cls.api_client,
            accountid=cls.virtual_machine.account,
            zoneid=cls.virtual_machine.zoneid,
            domainid=cls.virtual_machine.domainid,
            services=cls.services["server"]
        )
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
        self.cleanup = []
        return

    def tearDown(self):
        try:
            # Clean up, terminate the created instance, VPN users
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns"], required_hardware="false")
    def test_01_vpn_usage(self):
        """Test Create/Delete a VPN and verify correct usage is recorded
        """
        # Validate the following
        # 1. Enable VPN for this IP. vpn.add.user event is registered for this
        #    account in cloud.usage_event table
        # 2. Add user to this vpn
        # 3. Delete user for this VPN. vpn.user.delete event is registered for
        #    this account in cloud.usage_event table
        # 4. Delete this account.

        self.debug("Created VPN with public IP: %s" %
                   self.public_ip.ipaddress.id)
        # Assign VPN to Public IP
        vpn = Vpn.create(
            self.apiclient,
            self.public_ip.ipaddress.id,
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.debug("Created VPN user for account: %s" %
                   self.account.name)

        vpnuser = VpnUser.create(
            self.apiclient,
            self.services["vpn_user"]["username"],
            self.services["vpn_user"]["password"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        # Remove VPN user
        self.debug("Deleting VPN user: %s" % vpnuser.id)
        vpnuser.delete(self.apiclient)

        # Delete VPN access
        self.debug("Deleting VPN: %s" % vpn.publicipid)
        vpn.delete(self.apiclient)

        # Fetch account ID from account_uuid
        self.debug("select id from account where uuid = '%s';"
                   % self.account.id)

        qresultset = self.dbclient.execute(
            "select id from account where uuid = '%s';"
            % self.account.id
        )
        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )

        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )
        qresult = qresultset[0]

        account_id = qresult[0]
        self.debug("select type from usage_event where account_id = '%s';"
                   % account_id)

        qresultset = self.dbclient.execute(
            "select type from usage_event where account_id = '%s';"
            % account_id
        )

        self.assertEqual(
            isinstance(qresultset, list),
            True,
            "Check DB query result set for valid data"
        )
        self.assertNotEqual(
            len(qresultset),
            0,
            "Check DB Query result set"
        )

        qresult = str(qresultset)
        self.debug("Query result: %s" % qresult)

        # Check for VPN user related events
        self.assertEqual(
            qresult.count('VPN.USER.ADD'),
            1,
            "Check VPN.USER.ADD event in events table"
        )

        self.assertEqual(
            qresult.count('VPN.USER.ADD'),
            1,
            "Check VPN.USER.ADD in events table"
        )
        return
