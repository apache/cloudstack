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

""" P1 tests for dedicated Host high availability
"""
#Import Local Modules
from nose.plugins.attrib import attr
from marvin.cloudstackTestCase import cloudstackTestCase
import unittest
from marvin.cloudstackAPI import (migrateVirtualMachine,
                                  prepareHostForMaintenance,
                                  cancelHostMaintenance)
from marvin.lib.utils import cleanup_resources
from marvin.lib.base import (Account,
                             VirtualMachine,
                             ServiceOffering,
                             Cluster,
                             Host,
                             Configurations)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_hosts,
                               list_virtual_machines,
                               list_service_offering)
import time


class Services:
    """ Dedicated host HA test cases """

    def __init__(self):
        self.services = {
            "account": {
                "email": "test@test.com",
                "firstname": "HA",
                "lastname": "HA",
                "username": "HA",
                # Random characters are appended for unique
                # username
                "password": "password",
            },
            "service_offering_with_ha": {
                "name": "Tiny Instance With HA Enabled",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100, # in MHz
                "memory": 128, # In MBs
            },
            "service_offering_without_ha": {
                "name": "Tiny Instance Without HA",
                "displaytext": "Tiny Instance",
                "cpunumber": 1,
                "cpuspeed": 100, # in MHz
                "memory": 128, # In MBs
            },
            "virtual_machine": {
                "displayname": "VM",
                "username": "root",
                "password": "password",
                "ssh_port": 22,
                "hypervisor": 'XenServer',
                # Hypervisor type should be same as
                # hypervisor type of cluster
                "privateport": 22,
                "publicport": 22,
                "protocol": 'TCP',
            },
            "ostype": 'CentOS 5.3 (64-bit)',
            "timeout": 100,
        }


class TestHostHighAvailability(cloudstackTestCase):
    """ Dedicated host HA test cases """

    @classmethod
    def setUpClass(cls):
        cls.testClient = super(TestHostHighAvailability, cls).getClsTestClient()
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())

        cls.template = get_template(
            cls.api_client,
            cls.zone.id,
            cls.services["ostype"]
        )
        cls.hypervisor = cls.testClient.getHypervisorInfo()
        if cls.hypervisor.lower() in ['lxc']:
            raise unittest.SkipTest("Template creation from root volume is not supported in LXC")


        clusterWithSufficientHosts = None
        clusters = Cluster.list(cls.api_client, zoneid=cls.zone.id)
        for cluster in clusters:
            cls.hosts = Host.list(cls.api_client, clusterid=cluster.id, type="Routing")
            if len(cls.hosts) >= 3:
                clusterWithSufficientHosts = cluster
                break

        if clusterWithSufficientHosts is None:
            raise unittest.SkipTest("No Cluster with 3 hosts found")

        configs = Configurations.list(
                                      cls.api_client,
                                      name='ha.tag'
                                      )

        assert isinstance(configs, list), "Config list not\
                retrieved for ha.tag"

        if configs[0].value != "ha":
            raise unittest.SkipTest("Please set the global config\
                    value for ha.tag as 'ha'")

        Host.update(cls.api_client, id=cls.hosts[2].id, hosttags="ha")

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering_with_ha = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering_with_ha"],
            offerha=True
        )

        cls.service_offering_without_ha = ServiceOffering.create(
            cls.api_client,
            cls.services["service_offering_without_ha"],
            offerha=False
        )

        cls._cleanup = [
            cls.service_offering_with_ha,
            cls.service_offering_without_ha,
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Remove the host from HA
            Host.update(cls.api_client, id=cls.hosts[2].id, hosttags="")
            #Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.account = Account.create(
            self.apiclient,
            self.services["account"],
            admin=True,
            domainid=self.domain.id
        )
        self.cleanup = [self.account]
        return

    def tearDown(self):
        try:
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(configuration="ha.tag")
    @attr(tags=["advanced", "advancedns", "sg", "basic", "eip", "simulator"])
    def test_01_vm_deployment_with_compute_offering_with_ha_enabled(self):
        """ Test VM deployments (Create HA enabled Compute Service Offering and VM) """

        # Steps,
        #1. Create a Compute service offering with the 'Offer HA' option selected.
        #2. Create a Guest VM with the compute service offering created above.
        # Validations,
        #1. Ensure that the offering is created and that in the UI the 'Offer HA' field is enabled (Yes)
        #The listServiceOffering API should list 'offerha' as true.
        #2. Select the newly created VM and ensure that the Compute offering field value lists the compute service offering that was selected.
        #    Also, check that the HA Enabled field is enabled 'Yes'.

        #list and validate above created service offering with Ha enabled
        list_service_response = list_service_offering(
            self.apiclient,
            id=self.service_offering_with_ha.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "listServiceOfferings returned invalid object in response."
        )
        self.assertNotEqual(
            len(list_service_response),
            0,
            "listServiceOfferings returned empty list."
        )
        self.assertEqual(
            list_service_response[0].offerha,
            True,
            "The service offering is not HA enabled"
        )

        #create virtual machine with the service offering with Ha enabled
        virtual_machine = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_with_ha.id
        )
        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine.id,
            listall=True
        )
        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )
        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )
        self.debug("Deployed VM on host: %s" % vms[0].hostid)
        self.assertEqual(
            vms[0].haenable,
            True,
            "VM not created with HA enable tag"
        )

    @attr(configuration="ha.tag")
    @attr(tags=["advanced", "advancedns", "sg", "basic", "eip", "simulator", "multihost"])
    def test_02_no_vm_creation_on_host_with_haenabled(self):
        """ Verify you can not create new VMs on hosts with an ha.tag """

        # Steps,
        #1. Fresh install CS (Bonita) that supports this feature
        #2. Create Basic zone, pod, cluster, add 3 hosts to cluster (host1, host2, host3), secondary & primary Storage
        #3. When adding host3, assign the HA host tag.
        #4. You should already have a compute service offering with HA already create from above. If not, create one for HA.
        #5. Create VMs with the service offering with and without the HA tag
        # Validations,
        #Check to make sure the newly created VM is not on any HA enabled hosts
        #The  VM should be created only on host1 or host2 and never host3 (HA enabled)

        #create and verify virtual machine with HA enabled service offering
        virtual_machine_with_ha = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_with_ha.id
        )

        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_with_ha.id,
            listall=True
        )

        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )

        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        vm = vms[0]

        self.debug("Deployed VM on host: %s" % vm.hostid)

        #validate the virtual machine created is host Ha enabled
        list_hosts_response = list_hosts(
            self.apiclient,
            id=vm.hostid
        )
        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "listHosts returned invalid object in response."
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "listHosts retuned empty list in response."
        )

        self.assertEqual(
            list_hosts_response[0].hahost,
            False,
            "VM created on HA enabled host."
        )

        #create and verify virtual machine with Ha disabled service offering
        virtual_machine_without_ha = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_without_ha.id
        )

        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_without_ha.id,
            listall=True
        )

        self.assertEqual(
            isinstance(vms, list),
            True,
            "listVirtualMachines returned invalid object in response."
        )

        self.assertNotEqual(
            len(vms),
            0,
            "listVirtualMachines returned empty list."
        )

        vm = vms[0]

        self.debug("Deployed VM on host: %s" % vm.hostid)

        #verify that the virtual machine created on the host is Ha disabled
        list_hosts_response = list_hosts(
            self.apiclient,
            id=vm.hostid
        )
        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "listHosts returned invalid object in response."
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "listHosts returned empty list."
        )

        host = list_hosts_response[0]

        self.assertEqual(
            host.hahost,
            False,
            "VM migrated to HA enabled host."
        )

    @attr(configuration="ha.tag")
    @attr(tags=["advanced", "advancedns", "sg", "basic", "eip", "simulator", "multihost"])
    def test_03_cant_migrate_vm_to_host_with_ha_positive(self):
        """ Verify you can not migrate VMs to hosts with an ha.tag (positive) """

        # Steps,
        # 1. Create a Compute service offering with the 'Offer HA' option selected.
        # 2. Create a Guest VM with the compute service offering created above.
        # 3. Select the VM and migrate VM to another host. Choose a 'Suitable' host (i.e. host2)
        # Validations
        # The option from the 'Migrate instance to another host' dialog box' should list host3 as 'Not Suitable' for migration.
        # Confirm that the VM is migrated to the 'Suitable' host you selected
        # (i.e. host2)

        # create and verify the virtual machine with HA enabled service
        # offering
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)

        virtual_machine_with_ha = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_with_ha.id
        )

        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_with_ha.id,
            listall=True,
        )

        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )

        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )

        vm = vms[0]

        self.debug("Deployed VM on host: %s" % vm.hostid)

        # Find out a Suitable host for VM migration
        list_hosts_response = list_hosts(
            self.apiclient,
            virtualmachineid = vm.id
        )
        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "The listHosts API returned the invalid list"
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "The listHosts returned nothing."
        )
        suitableHost = None
        for host in list_hosts_response:
            if host.suitableformigration and host.hostid != vm.hostid:
                suitableHost = host
                break

        self.assertTrue(
            suitableHost is not None,
            "suitablehost should not be None")

        # Migration of the VM to a suitable host
        self.debug(
            "Migrating VM-ID: %s to Host: %s" %
            (vm.id, suitableHost.id))

        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.hostid = suitableHost.id
        cmd.virtualmachineid = vm.id
        self.apiclient.migrateVirtualMachine(cmd)

        # Verify that the VM migrated to a targeted Suitable host
        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=vm.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "The listVirtualMachines returned the invalid list."
        )

        self.assertNotEqual(
            list_vm_response,
            None,
            "The listVirtualMachines API returned nothing."
        )

        vm_response = list_vm_response[0]

        self.assertEqual(
            vm_response.id,
            vm.id,
            "The virtual machine id and the the virtual machine from listVirtualMachines is not matching."
        )

        self.assertEqual(
            vm_response.hostid,
            suitableHost.id,
            "The VM is not migrated to targeted suitable host."
        )

    @attr(configuration="ha.tag")
    @attr(tags=["advanced", "advancedns", "sg", "basic", "eip", "simulator", "multihost"])
    def test_04_cant_migrate_vm_to_host_with_ha_negative(self):
        """ Verify you can not migrate VMs to hosts with an ha.tag (negative) """

        # Steps,
        #1. Create a Compute service offering with the 'Offer HA' option selected.
        #2. Create a Guest VM with the compute service offering created above.
        #3. Select the VM and migrate VM to another host. Choose a 'Not Suitable' host.
        # Validations,
        #The option from the 'Migrate instance to another host' dialog box should list host3 as 'Not Suitable' for migration.
        #By design, The Guest VM can STILL can be migrated to host3 if the admin chooses to do so.

        #create and verify virtual machine with HA enabled service offering
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)
        virtual_machine_with_ha = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_with_ha.id
        )

        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_with_ha.id,
            listall=True
        )

        self.assertEqual(
            isinstance(vms, list),
            True,
            "The listVirtualMachines returned invalid object in response."
        )

        self.assertNotEqual(
            len(vms),
            0,
            "The listVirtualMachines returned empty response."
        )

        vm = vms[0]

        self.debug("Deployed VM on host: %s" % vm.hostid)

        #Find out Non-Suitable host for VM migration
        list_hosts_response = list_hosts(
            self.apiclient,
            type="Routing"
        )
        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "listHosts returned invalid object in response."
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "listHosts returned empty response."
        )

        notSuitableHost = None
        for host in list_hosts_response:
            if not host.suitableformigration and host.id != vm.hostid:
                notSuitableHost = host
                break

        self.assertTrue(notSuitableHost is not None, "notsuitablehost should not be None")

        #Migrate VM to Non-Suitable host
        self.debug("Migrating VM-ID: %s to Host: %s" % (vm.id, notSuitableHost.id))

        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.hostid = notSuitableHost.id
        cmd.virtualmachineid = vm.id
        self.apiclient.migrateVirtualMachine(cmd)

        #Verify that the virtual machine got migrated to targeted Non-Suitable host
        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=vm.id
        )
        self.assertEqual(
            isinstance(list_vm_response, list),
            True,
            "listVirtualMachine returned invalid object in response."
        )

        self.assertNotEqual(
            len(list_vm_response),
            0,
            "listVirtualMachines returned empty response."
        )

        self.assertEqual(
            list_vm_response[0].id,
            vm.id,
            "Virtual machine id with the virtual machine from listVirtualMachine is not matching."
        )

        self.assertEqual(
            list_vm_response[0].hostid,
            notSuitableHost.id,
            "The detination host id of migrated VM is not matching."
        )

    @attr(configuration="ha.tag")
    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns", "sg", "basic", "eip", "simulator", "multihost"])
    def test_05_no_vm_with_ha_gets_migrated_to_ha_host_in_live_migration(self):
        """ Verify that none of the VMs with HA enabled migrate to an ha tagged host during live migration """

        # Steps,
        #1. Fresh install CS that supports this feature
        #2. Create Basic zone, pod, cluster, add 3 hosts to cluster (host1, host2, host3), secondary & primary Storage
        #3. When adding host3, assign the HA host tag.
        #4. Create VMs with and without the Compute Service Offering with the HA tag.
        #5. Note the VMs on host1 and whether any of the VMs have their 'HA enabled' flags enabled.
        #6. Put host1 into maintenance mode.
        # Validations,
        #1. Make sure the VMs are created on either host1 or host2 and not on host3
        #2. Putting host1 into maintenance mode should trigger a live migration. Make sure the VMs are not migrated to HA enabled host3.

        # create and verify virtual machine with HA disabled service offering
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)
        virtual_machine_with_ha = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_with_ha.id
        )

        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_with_ha.id,
            listall=True
        )

        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )

        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )

        vm_with_ha_enabled = vms[0]

        #Verify the virtual machine got created on non HA host
        list_hosts_response = list_hosts(
            self.apiclient,
            id=vm_with_ha_enabled.hostid
        )
        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "Check Host is available"
        )

        self.assertEqual(
            list_hosts_response[0].hahost,
            False,
            "The virtual machine is not ha enabled so check if VM is created on host which is also not ha enabled"
        )

        #put the Host in maintenance mode
        self.debug("Enabling maintenance mode for host %s" % vm_with_ha_enabled.hostid)
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = vm_with_ha_enabled.hostid
        self.apiclient.prepareHostForMaintenance(cmd)

        timeout = self.services["timeout"]

        #verify the VM live migration happened to another running host
        self.debug("Waiting for VM to come up")
        time.sleep(timeout)

        vms = VirtualMachine.list(
            self.apiclient,
            id=vm_with_ha_enabled.id,
            listall=True,
        )

        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )

        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )

        vm_with_ha_enabled1 = vms[0]

        list_hosts_response = list_hosts(
            self.apiclient,
            id=vm_with_ha_enabled1.hostid
        )
        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "Check Host is available"
        )

        self.assertEqual(
            list_hosts_response[0].hahost,
            False,
            "The virtual machine is not ha enabled so check if VM is created on host which is also not ha enabled"
        )

        self.debug("Disabling the maintenance mode for host %s" % vm_with_ha_enabled.hostid)
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = vm_with_ha_enabled.hostid
        self.apiclient.cancelHostMaintenance(cmd)

    @attr(configuration="ha.tag")
    @attr(speed="slow")
    @attr(tags=["advanced", "advancedns", "sg", "basic", "eip", "simulator", "multihost"])
    def test_06_no_vm_without_ha_gets_migrated_to_ha_host_in_live_migration(self):
        """ Verify that none of the VMs without HA enabled migrate to an ha tagged host during live migration """

        # Steps,
        #1. Fresh install CS that supports this feature
        #2. Create Basic zone, pod, cluster, add 3 hosts to cluster (host1, host2, host3), secondary & primary Storage
        #3. When adding host3, assign the HA host tag.
        #4. Create VMs with and without the Compute Service Offering with the HA tag.
        #5. Note the VMs on host1 and whether any of the VMs have their 'HA enabled' flags enabled.
        #6. Put host1 into maintenance mode.
        # Validations,
        #1. Make sure the VMs are created on either host1 or host2 and not on host3
        #2. Putting host1 into maintenance mode should trigger a live migration. Make sure the VMs are not migrated to HA enabled host3.

        # create and verify virtual machine with HA disabled service offering
        self.hypervisor = self.testClient.getHypervisorInfo()
        if self.hypervisor.lower() in ['lxc']:
            self.skipTest("vm migrate is not supported in %s" % self.hypervisor)
        virtual_machine_without_ha = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machine"],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering_without_ha.id
        )

        vms = VirtualMachine.list(
            self.apiclient,
            id=virtual_machine_without_ha.id,
            listall=True
        )

        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )

        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )

        vm_with_ha_disabled = vms[0]

        #Verify the virtual machine got created on non HA host
        list_hosts_response = list_hosts(
            self.apiclient,
            id=vm_with_ha_disabled.hostid
        )
        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "Check Host is available"
        )

        self.assertEqual(
            list_hosts_response[0].hahost,
            False,
            "The virtual machine is not ha enabled so check if VM is created on host which is also not ha enabled"
        )

        #put the Host in maintenance mode
        self.debug("Enabling maintenance mode for host %s" % vm_with_ha_disabled.hostid)
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = vm_with_ha_disabled.hostid
        self.apiclient.prepareHostForMaintenance(cmd)

        timeout = self.services["timeout"]

        #verify the VM live migration happened to another running host
        self.debug("Waiting for VM to come up")
        time.sleep(timeout)

        vms = VirtualMachine.list(
            self.apiclient,
            id=vm_with_ha_disabled.id,
            listall=True
        )

        self.assertEqual(
            isinstance(vms, list),
            True,
            "List VMs should return valid response for deployed VM"
        )

        self.assertNotEqual(
            len(vms),
            0,
            "List VMs should return valid response for deployed VM"
        )

        list_hosts_response = list_hosts(
            self.apiclient,
            id=vms[0].hostid
        )
        self.assertEqual(
            isinstance(list_hosts_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_hosts_response),
            0,
            "Check Host is available"
        )

        self.assertEqual(
            list_hosts_response[0].hahost,
            False,
            "The virtual machine is not ha enabled so check if VM is created on host which is also not ha enabled"
        )

        self.debug("Disabling the maintenance mode for host %s" % vm_with_ha_disabled.hostid)
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = vm_with_ha_disabled.hostid
        self.apiclient.cancelHostMaintenance(cmd)
