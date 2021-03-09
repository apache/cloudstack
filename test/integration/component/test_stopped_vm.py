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

""" P1 for stopped Virtual Maschine life cycle
"""
# Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Volume,
                             Router,
                             DiskOffering,
                             Host,
                             Iso,
                             Cluster,
                             StoragePool,
                             Template)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               get_builtin_template_info,
                               update_resource_limit,
                               find_storage_pool_type)
from marvin.codes import PASS


class TestDeployVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestDeployVM, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.skip = False

        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.apiclient, storagetype='rbd'):
                cls.skip = True
                return

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        # Create service offerings, disk offerings etc
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        # Cleanup
        cls._cleanup = [
            cls.service_offering,
            cls.disk_offering,
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):

        if self.skip:
            self.skipTest("RBD storage type is required for data volumes for LXC")

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["iso"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    @attr(tags=["advanced", "eip", "advancedns"], required_hardware="false")
    def test_01_deploy_vm_no_startvm(self):
        """Test Deploy Virtual Machine with no startVM parameter
        """

        # Validate the following:
        # 1. deploy Vm  without specifying the startvm parameter
        # 2. Should be able to login to the VM.
        # 3. listVM command should return the deployed VM.State of this VM
        #    should be "Running".

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            mode=self.zone.networktype
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])
        return

    @attr(
        tags=[
              "advanced",
              "eip",
              "advancedns",
              "basic",
              "sg"],
        required_hardware="false")
    def test_02_deploy_vm_startvm_true(self):
        """Test Deploy Virtual Machine with startVM=true parameter
        """

        # Validate the following:
        # 1. deploy Vm  with the startvm=true
        # 2. Should be able to login to the VM.
        # 3. listVM command should return the deployed VM.State of this VM
        #    should be "Running".

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=True,
            mode=self.zone.networktype
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="false")
    def test_03_deploy_vm_startvm_false(self):
        """Test Deploy Virtual Machine with startVM=false parameter
        """

        # Validate the following:
        # 1. deploy Vm  with the startvm=false
        # 2. Should not be able to login to the VM.
        # 3. listVM command should return the deployed VM.State of this VM
        #    should be "Stopped".
        # 4. Check listRouters call for that account. List routers should
        #    return empty response

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False,
            diskofferingid=self.disk_offering.id,
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])
        routers = Router.list(
            self.apiclient,
            account=self.account.name,
            domainid=self.account.domainid,
            listall=True
        )
        self.assertEqual(
            routers,
            None,
            "List routers should return empty response"
        )
        self.debug("Destroying instance: %s" % self.virtual_machine.name)
        self.virtual_machine.delete(self.apiclient, expunge=True)
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="false")
    def test_04_deploy_startvm_false_attach_volume(self):
        """Test Deploy Virtual Machine with startVM=false and attach volume
        """

        # Validate the following:
        # 1. deploy Vm  with the startvm=false. Attach volume to the instance
        # 2. listVM command should return the deployed VM.State of this VM
        #    should be "Stopped".
        # 3. Attach volume should be successful

        # Skipping this test
        self.skipTest("Skipping test as proper implementation seems to be missing")

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False,
            diskofferingid=self.disk_offering.id,
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])
        self.debug("Creating a volume in account: %s" %
                   self.account.name)
        volume = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.debug("Created volume in account: %s" % self.account.name)
        self.debug("Attaching volume to instance: %s" %
                   self.virtual_machine.name)
        try:
            self.virtual_machine.attach_volume(self.apiclient, volume)
        except Exception as e:
            self.fail("Attach volume failed with Exception: %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="false")
    def test_05_deploy_startvm_false_change_so(self):
        """Test Deploy Virtual Machine with startVM=false and change service offering
        """

        # Validate the following:
        # 1. deploy Vm  with the startvm=false. Attach volume to the instance
        # 2. listVM command should return the deployed VM.State of this VM
        #    should be "Stopped".
        # 4. Change service offering

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False,
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])
        medium_service_off = ServiceOffering.create(
            self.apiclient,
            self.testdata["service_offering"]
        )
        self.cleanup.append(medium_service_off)

        self.debug("Changing service offering for instance: %s" %
                   self.virtual_machine.name)
        try:
            self.virtual_machine.change_service_offering(
                self.apiclient,
                medium_service_off.id
            )
        except Exception as e:
            self.fail("Change service offering failed: %s" % e)

        self.debug("Starting the instance: %s" % self.virtual_machine.name)
        self.virtual_machine.start(self.apiclient)
        self.debug("Instance: %s started" % self.virtual_machine.name)

        listedvm = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id)
        self.assertTrue(isinstance(listedvm, list))
        self.assertTrue(len(listedvm) > 0)
        self.assertEqual(
            listedvm[0].serviceofferingid,
            medium_service_off.id,
            msg="VM did not change service offering")

        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="false")
    def test_06_deploy_startvm_attach_detach(self):
        """Test Deploy Virtual Machine with startVM=false and
            attach detach volumes
        """

        # Validate the following:
        # 1. deploy Vm  with the startvm=false. Attach volume to the instance
        # 2. listVM command should return the deployed VM.State of this VM
        #    should be "Stopped".
        # 3. Attach volume should be successful
        # 4. Detach volume from instance. Detach should be successful

        # Skipping this test
        self.skipTest("Skipping test as proper implementation seems to be missing")

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False,
            diskofferingid=self.disk_offering.id,
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])
        self.debug("Creating a volume in account: %s" %
                   self.account.name)
        volume = Volume.create(
            self.apiclient,
            self.testdata["volume"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            diskofferingid=self.disk_offering.id
        )
        self.debug("Created volume in account: %s" % self.account.name)
        self.debug("Attaching volume to instance: %s" %
                   self.virtual_machine.name)
        try:
            self.virtual_machine.attach_volume(self.apiclient, volume)
        except Exception as e:
            self.fail("Attach volume failed with Exception: %s" % e)

        self.debug("Detaching the disk: %s" % volume.name)
        self.virtual_machine.detach_volume(self.apiclient, volume)
        self.debug("Datadisk %s detached!" % volume.name)

        volumes = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='DATADISK',
            id=volume.id,
            listall=True
        )
        self.assertEqual(
            volumes,
            None,
            "List Volumes should not list any volume for instance"
        )
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="true")
    def test_07_deploy_startvm_attach_iso(self):
        """Test Deploy Virtual Machine with startVM=false and attach ISO
        """

        # Validate the following:
        # 1. deploy Vm  with the startvm=false. Attach volume to the instance
        # 2. listVM command should return the deployed VM.State of this VM
        #    should be "Stopped".
        # 3. Attach ISO to the instance. Attach ISO should be successful

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False,
            diskofferingid=self.disk_offering.id,
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])
        self.debug("Registering a ISO in account: %s" %
                   self.account.name)
        iso = Iso.create(
            self.apiclient,
            self.testdata["iso"],
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.debug("Successfully created ISO with ID: %s" % iso.id)
        try:
            iso.download(self.apiclient)

        except Exception as e:
            self.fail("Exception while downloading ISO %s: %s"
                      % (iso.id, e))

        self.debug("Attach ISO with ID: %s to VM ID: %s" % (
            iso.id,
            self.virtual_machine.id
        ))
        try:
            self.virtual_machine.attach_iso(self.apiclient, iso)
        except Exception as e:
            self.fail("Attach ISO failed!")

        vms = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "List vms should return a valid list"
        )
        vm = vms[0]
        self.assertEqual(
            vm.isoid,
            iso.id,
            "The ISO status should be reflected in list Vm call"
        )
        return

    @attr(
        tags=[
              "advanced",
              "eip",
              "advancedns",
              "basic",
              "sg"],
        required_hardware="false")
    def test_08_deploy_attached_volume(self):
        """Test Deploy Virtual Machine with startVM=false and attach volume
           already attached to different machine
        """

        # Validate the following:
        # 1. deploy Vm  with the startvm=false. Attach volume to the instance
        # 2. listVM command should return the deployed VM.State of this VM
        #    should be "Stopped".
        # 3. Create an instance with datadisk attached to it. Detach DATADISK
        # 4. Attach the volume to first virtual machine.


        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False,
        )

        response = self.virtual_machine_1.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id
        )

        self.debug("Deployed instance in account: %s" %
                   self.account.name)
        list_vm_response = VirtualMachine.list(
            self.apiclient,
            id=self.virtual_machine_2.id
        )

        self.debug(
            "Verify listVirtualMachines response for virtual machine: %s"
            % self.virtual_machine_2.id
        )

        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "Check list response returns a valid list"
        )
        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.state,
            "Running",
            "VM should be in Stopped state after deployment with startvm=false"
        )

        self.debug(
            "Fetching DATADISK details for instance: %s" %
            self.virtual_machine_2.name)
        volumes = Volume.list(
            self.apiclient,
            type='DATADISK',
            account=self.account.name,
            domainid=self.account.domainid,
            virtualmachineid=self.virtual_machine_2.id
        )
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "List volumes should return a valid list"
        )
        volume = volumes[0]

        self.debug("Detaching the disk: %s" % volume.name)

        try:
            self.virtual_machine_2.detach_volume(self.apiclient, volume)
            self.debug("Datadisk %s detached!" % volume.name)
        except Exception as e:
            self.fail("Detach volume failed!")

        self.debug("Attaching volume to instance: %s" %
                   self.virtual_machine_1.name)
        try:
            self.virtual_machine_1.attach_volume(self.apiclient, volume)
        except Exception as e:
            self.fail("Attach volume failed with %s!" % e)

        volumes = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine_1.id,
            type='DATADISK',
            id=volume.id,
            listall=True
        )
        self.assertNotEqual(
            volumes,
            None,
            "List Volumes should not list any volume for instance"
        )
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="false")
    def test_09_stop_vm_migrate_vol(self):
        """Test Stopped Virtual Machine's ROOT volume migration
        """

        # Validate the following:
        # 1. deploy Vm with startvm=true
        # 2. Should not be able to login to the VM.
        # 3. listVM command should return the deployed VM.State of this VM
        #    should be "Running".
        # 4. Stop the vm
        # 5.list primary storages in the cluster , should be more than one
        # 6.Migrate voluem to another available primary storage
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest(
                "vm migrate is not supported in %s" %
                self.hypervisor)

        clusters = Cluster.list(
            self.apiclient,
            zoneid=self.zone.id
        )
        self.assertEqual(
            isinstance(clusters, list),
            True,
            "Check list response returns a valid list"
        )
        i = 0
        for cluster in clusters:
            storage_pools = StoragePool.list(
                self.apiclient,
                clusterid=cluster.id
            )
            if len(storage_pools) > 1:
                self.cluster_id = cluster.id
                i += 1
                break
        if i == 0:
            self.skipTest(
                "No cluster with more than one primary storage pool to "
                "perform migrate volume test")

        hosts = Host.list(
            self.apiclient,
            clusterid=self.cluster_id
        )
        self.assertEqual(
            isinstance(hosts, list),
            True,
            "Check list response returns a valid list"
        )
        host = hosts[0]
        self.debug("Deploying instance on host: %s" % host.id)
        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            hostid=host.id,
            mode=self.zone.networktype
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])
        try:
            self.virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("failed to stop instance: %s" % e)
        volumes = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )
        self.assertEqual(
            isinstance(volumes, list),
            True,
            "Check volume list response returns a valid list"
        )
        vol_response = volumes[0]
        # get the storage name in which volume is stored
        storage_name = vol_response.storage
        storage_pools = StoragePool.list(
            self.apiclient,
            clusterid=self.cluster_id
        )
        # Get storage pool to migrate volume
        for spool in storage_pools:
            if spool.name == storage_name:
                continue
            else:
                self.storage_id = spool.id
                self.storage_name = spool.name
                break
        self.debug("Migrating volume to storage pool: %s" % self.storage_name)
        Volume.migrate(
            self.apiclient,
            storageid=self.storage_id,
            volumeid=vol_response.id
        )
        volume = Volume.list(
            self.apiclient,
            virtualmachineid=self.virtual_machine.id,
            type='ROOT',
            listall=True
        )
        self.assertEqual(
            volume[0].storage,
            self.storage_name,
            "Check volume migration response")

        return


class TestDeployHaEnabledVM(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestDeployHaEnabledVM, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.skip = False
        
        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.apiclient, storagetype='rbd'):
                cls.skip = True 
                return

        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        # Create service, disk offerings  etc
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"],
            offerha=True
        )
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        # Cleanup
        cls._cleanup = [
            cls.service_offering,
            cls.disk_offering,
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):

        if self.skip:
            self.skipTest("RBD storage type is required for data volumes for LXC ")

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.testdata["iso"]["zoneid"] = self.zone.id
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="false")
    def test_01_deploy_ha_vm_startvm_false(self):
        """Test Deploy HA enabled Virtual Machine with startvm=false
        """

        # Validate the following:
        # 1. deployHA enabled  Vm  with the startvm parameter = false
        # 2. listVM command should return the deployed VM. State of this VM
        #    should be "Created".

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            startvm=False
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="true")
    def test_02_deploy_ha_vm_from_iso(self):
        """Test Deploy HA enabled Virtual Machine from ISO
        """

        # Validate the following:
        # 1. deployHA enabled Vm using ISO with the startvm parameter=true
        # 2. listVM command should return the deployed VM. State of this VM
        #    should be "Running".
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest(
                "vm deploy from ISO feature is not supported on %s" %
                self.hypervisor.lower())

        self.iso = Iso.create(
            self.apiclient,
            self.testdata["configurableData"]["bootableIso"],
            account=self.account.name,
            domainid=self.account.domainid,
            zoneid=self.zone.id
        )
        try:
            # Download the ISO
            self.iso.download(self.apiclient)

        except Exception as e:
            raise Exception("Exception while downloading ISO %s: %s"
                            % (self.iso.id, e))

        self.debug("Registered ISO: %s" % self.iso.name)
        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            templateid=self.iso.id,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            startvm=True,
            hypervisor=self.hypervisor
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])
        return

    @attr(
        tags=[
            "advanced",
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="false")
    def test_03_deploy_ha_vm_iso_startvm_false(self):
        """Test Deploy HA enabled Virtual Machine from ISO with startvm=false
        """

        # Validate the following:
        # 1. deployHA enabled Vm using ISO with the startvm parameter=false
        # 2. listVM command should return the deployed VM. State of this VM
        #    should be "Stopped".
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm deploy from ISO feature is not supported on %s" %
                self.hypervisor.lower())

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            startvm=False
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])
        return


class TestRouterStateAfterDeploy(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(
            TestRouterStateAfterDeploy,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.skip = False

        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.apiclient, storagetype='rbd'):
                cls.skip = True
                return

        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        # Create service offerings, disk offerings etc
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        # Cleanup
        cls._cleanup = [
            cls.service_offering,
            cls.disk_offering,
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):

        if self.skip:
            self.skipTest("RBD storage type is required for data volumes for LXC")

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.testdata["iso"]["zoneid"] = self.zone.id
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    @attr(tags=["advanced", "eip", "advancedns"], required_hardware="false")
    def test_01_deploy_vm_no_startvm(self):
        """Test Deploy Virtual Machine with no startVM parameter
        """

        # Validate the following:
        # 1. deploy Vm  without specifying the startvm parameter
        # 2. Should be able to login to the VM.
        # 3. listVM command should return the deployed VM.State of this VM
        #    should be "Running".

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine_1 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False
        )

        response = self.virtual_machine_1.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])

        if(self.zone.networktype == "Advanced"):
            self.debug("Checking the router state after VM deployment")
            routers = Router.list(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                listall=True
            )
            self.assertEqual(
                routers,
                None,
                "List routers should return empty response"
            )
        self.debug(
            "Deploying another instance (startvm=true) in the account: %s" %
            self.account.name)
        self.virtual_machine_2 = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            diskofferingid=self.disk_offering.id,
            startvm=True
        )

        response = self.virtual_machine_2.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])
        self.debug("Checking the router state after VM deployment")
        if (self.zone.networktype == "Basic"):
            routers = Router.list(
                                  self.apiclient,
                                  zoneid=self.zone.id,
                                  listall=True
                                 )
        else:
            routers = Router.list(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                listall=True
            )
        self.assertEqual(
            isinstance(routers, list),
            True,
            "List routers should not return empty response"
        )
        for router in routers:
            self.debug("Router state: %s" % router.state)
            self.assertEqual(
                router.state,
                "Running",
                "Router should be in running state when "
                "instance is running in the account")
        self.debug("Destroying the running VM:%s" %
                   self.virtual_machine_2.name)
        self.virtual_machine_2.delete(self.apiclient, expunge=True)
        if(self.zone.networktype == "Advanced"):
            routers = Router.list(
                self.apiclient,
                account=self.account.name,
                domainid=self.account.domainid,
                listall=True
            )
            self.assertNotEqual(
                routers,
                None,
                "Router should get deleted after expunge delay+wait"
            )
        return


class TestDeployVMBasicZone(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestDeployVMBasicZone, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        # Get Zone, Domain and templates
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.skip = False

        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.apiclient, storagetype='rbd'):
                cls.skip = True
                return

        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )

        # Create service offerings, disk offerings etc
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        # Cleanup
        cls._cleanup = [
            cls.service_offering,
            cls.disk_offering,
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):

        if self.skip:
            self.skipTest("RBD storage type is required for data volumes for LXC")

        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.testdata["iso"]["zoneid"] = self.zone.id
        self.testdata["virtual_machine"]["template"] = self.template.id
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)


class TestDeployVMFromTemplate(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(
            TestDeployVMFromTemplate,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        # Create service, disk offerings  etc
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"],
            offerha=True
        )
        cls.disk_offering = DiskOffering.create(
            cls.api_client,
            cls.testdata["disk_offering"]
        )
        # Cleanup
        cls._cleanup = [
            cls.service_offering,
            cls.disk_offering,
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.testdata["virtual_machine"]["zoneid"] = self.zone.id
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            domainid=self.domain.id
        )

        builtin_info = get_builtin_template_info(self.apiclient, self.zone.id)
        self.testdata["privatetemplate"]["url"] = builtin_info[0]
        self.testdata["privatetemplate"]["hypervisor"] = builtin_info[1]
        self.testdata["privatetemplate"]["format"] = builtin_info[2]

        # Register new template
        self.template = Template.register(
            self.apiclient,
            self.testdata["privatetemplate"],
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid,
            hypervisor=self.hypervisor
        )
        self.debug(
            "Registered a template of format: %s with ID: %s" % (
                self.testdata["privatetemplate"]["format"],
                self.template.id
            ))
        try:
            self.template.download(self.apiclient)
        except Exception as e:
            raise Exception("Template download failed: %s" % e)

        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            self.debug("Cleaning up the resources")
            cleanup_resources(self.apiclient, self.cleanup)
            self.debug("Cleanup complete!")
        except Exception as e:
            self.debug("Warning! Exception in tearDown: %s" % e)

    @attr(
        tags=[
              "advanced",
              "eip",
              "advancedns",
              "basic",
              "sg"],
        required_hardware="true")
    def test_deploy_vm_password_enabled(self):
        """Test Deploy Virtual Machine with startVM=false & enabledpassword in
        template
        """

        # Validate the following:
        # 1. Create the password enabled template
        # 2. Deploy Vm with this template and passing startvm=false
        # 3. Start VM. Deploy VM should be successful and it should be in Up
        #    and running state

        self.debug("Deploying instance in the account: %s" %
                   self.account.name)
        self.virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            startvm=False,
        )

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.STOPPED)
        self.assertEqual(response[0], PASS, response[1])
        self.debug("Starting the instance: %s" % self.virtual_machine.name)
        self.virtual_machine.start(self.apiclient)
        self.debug("Started the instance: %s" % self.virtual_machine.name)

        response = self.virtual_machine.getState(
            self.apiclient,
            VirtualMachine.RUNNING)
        self.assertEqual(response[0], PASS, response[1])
        return


class TestVMAccountLimit(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestVMAccountLimit, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id

        # Create Account, VMs etc
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup = [
            cls.service_offering,
            cls.account
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
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="false")
    def test_vm_per_account(self):
        """Test VM limit per account
        """

        # Validate the following
        # 1. Set the resource limit for VM per account.
        # 2. Deploy VMs more than limit in that account.
        # 3. AIP should error out

        self.debug(
            "Updating instance resource limit for account: %s" %
            self.account.name)
        # Set usage_vm=1 for Account 1
        update_resource_limit(
            self.apiclient,
            0,    # Instance
            account=self.account.name,
            domainid=self.account.domainid,
            max=1
        )
        self.debug(
            "Deploying VM instance in account: %s" %
            self.account.name)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False
        )

        # Verify VM state
        self.assertEqual(
            virtual_machine.state,
            'Stopped',
            "Check VM state is Running or not"
        )

        # Exception should be raised for second instance (account_1)
        with self.assertRaises(Exception):
            VirtualMachine.create(
                self.apiclient,
                self.testdata["virtual_machine"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                startvm=False
            )
        return


class TestUploadAttachVolume(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestUploadAttachVolume, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        cls.skip = True

        if cls.hypervisor.lower() == 'lxc':
            if not find_storage_pool_type(cls.apiclient, storagetype='rbd'):
                cls.skip = True

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id

        # Create Account, VMs etc
        cls.account = Account.create(
            cls.api_client,
            cls.testdata["account"],
            domainid=cls.domain.id
        )

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )
        cls._cleanup = [
            cls.service_offering,
            cls.account
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
        if self.skip:
            self.skipTest("Attach operation for uploaded volume to VM which is not started once is not supported")
            # self.skipTest("RBD storage type is required for data volumes for LXC")

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
            "eip",
            "advancedns",
            "basic",
            "sg"],
        required_hardware="false")
    def test_upload_attach_volume(self):
        """Test Upload volume and attach to VM in stopped state
        """

        # Validate the following
        # 1. Upload the volume using uploadVolume API call
        # 2. Deploy VM with startvm=false.
        # 3. Attach the volume to the deployed VM in step 2

        self.debug(
            "Uploading the volume: %s" %
            self.testdata["configurableData"]["upload_volume"]["diskname"])
        try:
            volume = Volume.upload(
                self.apiclient,
                self.testdata["configurableData"]["upload_volume"],
                zoneid=self.zone.id,
                account=self.account.name,
                domainid=self.account.domainid
            )
            self.debug("Uploading the volume: %s" % volume.name)
            volume.wait_for_upload(self.apiclient)
            self.debug("Volume: %s uploaded successfully")
        except Exception as e:
            self.fail("Failed to upload the volume: %s" % e)

        self.debug(
            "Deploying VM instance in account: %s" %
            self.account.name)

        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.testdata["virtual_machine"],
            templateid=self.template.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            startvm=False
        )
        # Verify VM state
        self.assertEqual(
            virtual_machine.state,
            'Stopped',
            "Check VM state is Running or not"
        )
        virtual_machine.attach_volume(self.apiclient, volume)
        return


class TestDeployOnSpecificHost(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(
            TestDeployOnSpecificHost,
            cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.testdata = cls.testClient.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.testdata["ostype"]
        )
        cls.testdata["virtual_machine"]["zoneid"] = cls.zone.id
        cls.testdata["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.testdata["service_offering"]
        )

        cls._cleanup = [
            cls.service_offering,
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
        self.account = Account.create(
            self.apiclient,
            self.testdata["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = []
        return

    def tearDown(self):
        try:
            self.account.delete(self.apiclient)
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(tags=["advanced", "advancedns", "simulator",
                "api", "basic", "eip", "sg"])
    def test_deployVmOnGivenHost(self):
        """Test deploy VM on specific host
        """

        # Steps for validation
        # 1. as admin list available hosts that are Up
        # 2. deployVM with hostid=above host
        # 3. listVirtualMachines
        # 4. destroy VM
        # Validate the following
        # 1. listHosts returns at least one host in Up state
        # 2. VM should be in Running
        # 3. VM should be on the host that it was deployed on

        hosts = Host.list(
            self.apiclient,
            zoneid=self.zone.id,
            type='Routing',
            state='Up',
            listall=True
        )

        self.assertEqual(
            isinstance(hosts, list),
            True,
            "CS should have atleast one host Up and Running"
        )

        host = hosts[0]
        self.debug("Deploting VM on host: %s" % host.name)

        try:
            vm = VirtualMachine.create(
                self.apiclient,
                self.testdata["virtual_machine"],
                templateid=self.template.id,
                accountid=self.account.name,
                domainid=self.account.domainid,
                serviceofferingid=self.service_offering.id,
                hostid=host.id
            )
            self.debug("Deploy VM succeeded")
        except Exception as e:
            self.fail("Deploy VM failed with exception: %s" % e)

        self.debug("Cheking the state of deployed VM")
        vms = VirtualMachine.list(
            self.apiclient,
            id=vm.id,
            listall=True,
            account=self.account.name,
            domainid=self.account.domainid
        )

        self.assertEqual(
            isinstance(vms, list),
            True,
            "List Vm should return a valid response"
        )

        vm_response = vms[0]
        self.assertEqual(
            vm_response.state,
            "Running",
            "VM should be in running state after deployment"
        )
        self.assertEqual(
            vm_response.hostid,
            host.id,
            "Host id where VM is deployed should match"
        )
        return
