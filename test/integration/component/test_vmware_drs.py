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

""" P1 for VMware DRS testing
"""
#Import Local Modules
import marvin
from nose.plugins.attrib import attr

from marvin.cloudstackTestCase import cloudstackTestCase, unittest

from marvin.lib.base import (Account,
                                         AffinityGroup,
                                         Host,
                                         VirtualMachine,
                                         ServiceOffering)

from marvin.lib.common import (get_zone,
                                           get_template,
                                           get_domain,
                                           get_pod
                                           )

from marvin.lib.utils import (validateList,
                                          cleanup_resources,
                                          random_gen)

from marvin.cloudstackAPI import (prepareHostForMaintenance,
                                  cancelHostMaintenance,
                                  migrateVirtualMachine)

from marvin.codes import PASS


#Import System modules
import time


class Services:
    """Test vmware DRS services
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
                "virtual_machine":
                {
                    "displayname": "testserver",
                    "username": "root",     # VM creds for SSH
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'XenServer',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP',
                },
                "service_offering":
                {
                    "name": "Tiny Instance",
                    "displaytext": "Tiny Instance",
                    "cpunumber": 1,
                    "cpuspeed": 100,    # in MHz
                    "memory": 2048,      # In MBs
                },
                "service_offering_max_memory":
                {
                    "name": "Tiny Instance",
                    "displaytext": "Tiny Instance",
                    "cpunumber": 1,
                    "cpuspeed": 100,    # in MHz
                    "memory": 128,      # In MBs
                },
                "host_anti_affinity": {
                    "name": "",
                    "type": "host anti-affinity",
                },
                "host_affinity": {
                    "name": "",
                    "type": "host affinity",
                },
            "sleep": 60,
            "timeout": 10,
            "ostype": 'CentOS 5.3 (64-bit)',
            # CentOS 5.3 (64-bit)
        }

class TestVMPlacement(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestVMPlacement, cls).getClsTestClient()
        if cls.testClient.getHypervisorInfo().lower() != "vmware":
            raise unittest.SkipTest("VMWare tests only valid on VMWare hypervisor")
        cls.api_client = cls.testClient.getApiClient()

        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client)
        cls.zone = get_zone(cls.api_client, cls.testClient.getZoneForTests())
        cls.pod = get_pod(
                          cls.api_client,
                          zone_id=cls.zone.id)
        cls.template = get_template(
                                    cls.api_client,
                                    cls.zone.id,
                                    cls.services["ostype"]
                            )
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"],
                                            offerha=True
                                            )
        cls._cleanup = [
                        cls.service_offering,
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
            #self.testClient.close()
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @unittest.skip("Test case is not testable as the VM is being deployed with\
    too high memory - above account capacity")
    @attr(tags=["invalid"], required_hardware="true")
    def test_vm_creation_in_fully_automated_mode(self):
        """ Test VM Creation in  automation mode = Fully automated
            This test requires following preconditions:
                - DRS Cluster is configured in "Fully automated" mode
        """
        # Validate the following
        # 1. Create a new VM in a host which is almost fully utilized
        # 2 Automatically places VM on the other host
        # 3. VM state is running after deployment

        hosts = Host.list(
                          self.apiclient,
                          zoneid=self.zone.id,
                          resourcestate='Enabled',
                          type='Routing'
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List hosts should return valid host response"
                         )
        self.assertGreaterEqual(
                         len(hosts),
                         2,
                         "There must be two hosts present in a cluster"
                        )

        host_1 = hosts[0]

        #Convert available memory( Keep some margin) into MBs and assign to service offering
        self.services["service_offering_max_memory"]["memory"] = int((int(hosts[0].memorytotal) - int(hosts[0].memoryused))/1048576 - 1024)

        self.debug("max memory: %s" % self.services["service_offering_max_memory"]["memory"])

        service_offering_max_memory = ServiceOffering.create(
                                            self.apiclient,
                                            self.services["service_offering_max_memory"]
                                            )

        VirtualMachine.create(
                              self.apiclient,
                              self.services["virtual_machine"],
                              accountid=self.account.name,
                              domainid=self.account.domainid,
                              serviceofferingid=service_offering_max_memory.id,
                              hostid = host_1.id
                              )

        # Host 1 has only 1024 MB memory available now after deploying the instance
        # We are trying to deploy an instance with 2048 MB memory, this should automatically
        # get deployed on other host which has the enough capacity

        self.debug("Trying to deploy instance with memory requirement more than that is available on\
                     the first host")

        self.debug("Deploying VM in account: %s" % self.account.name)
        # Spawn an instance in that network
        virtual_machine = VirtualMachine.create(
                                  self.apiclient,
                                  self.services["virtual_machine"],
                                  accountid=self.account.name,
                                  domainid=self.account.domainid,
                                  serviceofferingid=self.service_offering.id
                                  )
        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine.id,
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
        vm = vms[0]
        self.assertEqual(
                         vm.state,
                         "Running",
                         "Deployed VM should be in RUnning state"
                         )
        self.assertNotEqual(
                    vm.hostid,
                    host_1.id,
                    "Host Ids of two should not match as one host is full"
                    )

        self.debug("The host ids of two virtual machines are different as expected\
                    they are %s and %s" % (vm.hostid, host_1.id))
        return

@unittest.skip("Skipping... Not tested due to unavailibility of multihosts setup - 3 hosts in a cluster")
class TestAntiAffinityRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestAntiAffinityRules, cls).getClsTestClient()
        if cls.testClient.getHypervisorInfo().lower() != "vmware":
            raise unittest.SkipTest("VMWare tests only valid on VMWare hypervisor")
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
        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"],
                                            offerha=True
                                            )

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     domainid=cls.domain.id
                                     )
        cls._cleanup = [cls.account]
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
            #Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
            #self.testClient.close()
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def create_aff_grp(self, aff_grp=None,
                  acc=None, domainid=None):

        aff_grp["name"] = "aff_grp_" + random_gen(size=6)

        try:
            aff_grp = AffinityGroup.create(self.apiclient,
                                           aff_grp, acc, domainid)
            return aff_grp
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    @attr(tags=["advanced", "vmware", "multihost"], required_hardware="true")
    def test_vmware_anti_affinity(self):
        """ Test Set up anti-affinity rules

            The test requires following pre-requisites
            - VMWare cluster configured in fully automated mode
        """

        # Validate the following
        # 1. Deploy VMs on host 1 and 2
        # 2. Enable maintenance mode for host 1
        # 3. VM should be migrated to 3rd host

        hosts = Host.list(
                          self.apiclient,
                          zoneid=self.zone.id,
                          resourcestate='Enabled',
                          type='Routing'
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List hosts should return valid host response"
                         )

        self.debug(len(hosts))

        self.assertGreaterEqual(
                         len(hosts),
                         3,
                         "There must be at least 3 hosts present in a cluster"
                        )

        aff_grp = self.create_aff_grp(aff_grp=self.services["host_anti_affinity"], acc=self.account.name, domainid=self.domain.id)

        vm_1 = VirtualMachine.create(
                              self.apiclient,
                              self.services["virtual_machine"],
                              accountid=self.account.name,
                              domainid=self.domain.id,
                              serviceofferingid=self.service_offering.id,
                              affinitygroupnames=[aff_grp.name]
                             )

        vm_2 = VirtualMachine.create(
                              self.apiclient,
                              self.services["virtual_machine"],
                              accountid=self.account.name,
                              domainid=self.domain.id,
                              serviceofferingid=self.service_offering.id,
                              affinitygroupnames=[aff_grp.name]
                             )

        host_1 = vm_1.hostid

        host_2 = vm_2.hostid

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=vm_1.id,
                                  listall=True
                                  )
        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[1])

        virtual_machine_1 = vm_list_validation_result[1]

        self.debug("VM State: %s" % virtual_machine_1.state)
        self.assertEqual(
                         virtual_machine_1.state,
                         "Running",
                         "Deployed VM should be in RUnning state"
                         )

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=vm_2.id,
                                  listall=True
                                  )
        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[1])

        virtual_machine_2 = vm_list_validation_result[1]

        self.debug("VM %s  State: %s" % (
                                         virtual_machine_2.name,
                                         virtual_machine_2.state
                                         ))
        self.assertEqual(
                         virtual_machine_2.state,
                         "Running",
                         "Deployed VM should be in RUnning state"
                         )
        self.debug("Enabling maintenance mode on host_1: %s" % host_1)

        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = host_1
        self.apiclient.prepareHostForMaintenance(cmd)

        timeout = self.services["timeout"]
        while True:
            hosts = Host.list(
                          self.apiclient,
                          zoneid=self.zone.id,
                          type='Routing',
                          id=host_1
                          )
            host_list_validation_result = validateList(hosts)

            self.assertEqual(host_list_validation_result[0], PASS, "host list validation failed due to %s"
                            % host_list_validation_result[2])

            host = host_list_validation_result[1]

            if host.resourcestate == 'Maintenance':
                break
            elif timeout == 0:
                self.fail("Failed to put host: %s in maintenance mode" % host.name)

            time.sleep(self.services["sleep"])
            timeout = timeout - 1

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=virtual_machine_1.id,
                                  listall=True
                                  )
        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        vm = vm_list_validation_result[0]

        self.assertEqual(
                         vm.state,
                         "Running",
                         "Deployed VM should be in RUnning state"
                         )
        self.assertNotEqual(
                         vm.hostid,
                         host_2,
                         "The host name should not match with second host name"
                         )

        self.debug("Canceling host maintenance for ID: %s" % host_1.id)
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = host_1.id
        self.apiclient.cancelHostMaintenance(cmd)
        self.debug("Maintenance mode canceled for host: %s" % host_1.id)

        return

@unittest.skip("Skipping...Host Affinity feature not available yet in cloudstack")
class TestAffinityRules(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):

        cls.testClient = super(TestAffinityRules, cls).getClsTestClient()
        if cls.testClient.getHypervisorInfo().lower() != "vmware":
            raise unittest.SkipTest("VMWare tests only valid on VMWare hypervisor")
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

        cls.services["virtual_machine"]["zoneid"] = cls.zone.id
        cls.services["virtual_machine"]["template"] = cls.template.id

        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"],
                                            offerha=True
                                            )

        cls.account = Account.create(
                                     cls.api_client,
                                     cls.services["account"],
                                     domainid=cls.domain.id
                                     )
        cls._cleanup = [cls.account]
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
            # Clean up, terminate the created accounts, domains etc
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def create_aff_grp(self, aff_grp=None,
                  acc=None, domainid=None):

        aff_grp["name"] = "aff_grp_" + random_gen(size=6)

        try:
            aff_grp = AffinityGroup.create(self.apiclient,
                                           aff_grp, acc, domainid)
            return aff_grp
        except Exception as e:
            raise Exception("Error: Creation of Affinity Group failed : %s" %e)

    @attr(tags=["advanced", "vmware", "multihost"], required_hardware="true")
    def test_vmware_affinity(self):
        """ Test Set up affinity rules

            The test requires following pre-requisites
            - VMWare cluster configured in fully automated mode
        """

        # Validate the following
        # 1. Deploy 2 VMs on same hosts
        # 2. Migrate one VM from one host to another
        # 3. The second VM should also get migrated

        hosts = Host.list(
                          self.apiclient,
                          zoneid=self.zone.id,
                          resourcestate='Enabled',
                          type='Routing'
                          )
        self.assertEqual(
                         isinstance(hosts, list),
                         True,
                         "List hosts should return valid host response"
                         )
        self.assertGreaterEqual(
                         len(hosts),
                         2,
                         "There must be two hosts present in a cluster"
                        )

        host_1 = hosts[0].id

        host_2 = hosts[1].id

        aff_grp = self.create_aff_grp(aff_grp=self.services["host_affinity"], acc=self.account.name, domainid=self.domain.id)

        vm_1 = VirtualMachine.create(
                              self.apiclient,
                              self.services["virtual_machine"],
                              accountid=self.account.name,
                              domainid=self.domain.id,
                              serviceofferingid=self.service_offering.id,
                              affinitygroupnames=[aff_grp.name],
                              hostid = host_1
                             )

        vm_2 = VirtualMachine.create(
                              self.apiclient,
                              self.services["virtual_machine"],
                              accountid=self.account.name,
                              domainid=self.domain.id,
                              serviceofferingid=self.service_offering.id,
                              affinitygroupnames=[aff_grp.name]
                             )

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id= vm_1.id,
                                  listall=True
                                  )

        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        virtual_machine_1 = vm_list_validation_result[1]

        self.assertEqual(
                         virtual_machine_1.state,
                         "Running",
                         "Deployed VM should be in RUnning state"
                         )

        self.debug("Deploying VM on account: %s" % self.account.name)

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  id=vm_2.id,
                                  listall=True
                                  )
        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        virtual_machine_2 = vm_list_validation_result[1]

        self.assertEqual(
                         virtual_machine_2.state,
                         "Running",
                         "Deployed VM should be in RUnning state"
                         )

        self.debug("Migrate VM from host_1 to host_2")
        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.virtualmachineid = virtual_machine_2.id
        cmd.hostid = host_2
        self.apiclient.migrateVirtualMachine(cmd)
        self.debug("Migrated VM from host_1 to host_2")

        vms = VirtualMachine.list(
                                  self.apiclient,
                                  hostid=host_2,
                                  listall=True
                                  )
        vm_list_validation_result = validateList(vms)

        self.assertEqual(vm_list_validation_result[0], PASS, "vm list validation failed due to %s" %
                         vm_list_validation_result[2])

        vmids = [vm.id for vm in vms]

        self.assertIn(
                      virtual_machine_1.id,
                      vmids,
                      "VM 1 should be successfully migrated to host 2"
                      )
        self.assertIn(
                      virtual_machine_2.id,
                      vmids,
                      "VM 2 should be automatically migrated to host 2"
                      )
        return
