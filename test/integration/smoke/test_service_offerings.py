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
""" BVT tests for Service offerings"""

# Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.cloudstackAPI import (changeServiceForVirtualMachine,
                                  updateServiceOffering)
from marvin.lib.utils import (isAlmostEqual,
                              cleanup_resources,
                              random_gen)
from marvin.lib.base import (ServiceOffering,
                             Account,
                             VirtualMachine)
from marvin.lib.common import (list_service_offering,
                               list_virtual_machines,
                               get_domain,
                               get_zone,
                               get_test_template,
                               list_hosts)
from nose.plugins.attrib import attr

import time
from marvin.sshClient import SshClient
from marvin.lib.decoratorGenerators import skipTestIf

_multiprocess_shared_ = True


class TestCreateServiceOffering(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []
        self.services = self.testClient.getParsedTestDataConfig()

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_01_create_service_offering(self):
        """Test to create service offering"""

        # Validate the following:
        # 1. createServiceOfferings should return a valid information
        #    for newly created offering
        # 2. The Cloud Database contains the valid information

        service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offerings"]["tiny"]
        )
        self.cleanup.append(service_offering)

        self.debug(
            "Created service offering with ID: %s" %
            service_offering.id)

        list_service_response = list_service_offering(
            self.apiclient,
            id=service_offering.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_service_response),
            0,
            "Check Service offering is created"
        )

        self.assertEqual(
            list_service_response[0].cpunumber,
            self.services["service_offerings"]["tiny"]["cpunumber"],
            "Check server id in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].cpuspeed,
            self.services["service_offerings"]["tiny"]["cpuspeed"],
            "Check cpuspeed in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].displaytext,
            self.services["service_offerings"]["tiny"]["displaytext"],
            "Check server displaytext in createServiceOfferings"
        )
        self.assertEqual(
            list_service_response[0].memory,
            self.services["service_offerings"]["tiny"]["memory"],
            "Check memory in createServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].name,
            self.services["service_offerings"]["tiny"]["name"],
            "Check name in createServiceOffering"
        )
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_02_create_iops_offering(self):
        """Test to create service io burst offering"""

        # Validate the following:
        # 1. createServiceOfferings should return a valid information
        #    for newly created offering
        # 2. The Cloud Database contains the valid information



        svcs = self.services["service_offerings"]["tiny"]
        kws = {}

        for key in self.services["ioburst"]:
            if str(key).startswith("bytes") or str(key).startswith("iops"):
                kws[key] = self.services["ioburst"][key]
            else:
                svcs[key] = self.services["ioburst"][key]

        service_offering = ServiceOffering.create(
            self.apiclient,
            svcs,
            None,
            None,
            **kws
        )
        self.cleanup.append(service_offering)

        self.debug(
            "Created service offering with ID: %s" %
            service_offering.id)

        list_service_response = list_service_offering(
            self.apiclient,
            id=service_offering.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_service_response),
            0,
            "Check Service offering is created"
        )

        for key in kws:
            k = str(key)
            mapped = 'disk' + k[:1].upper() + k[1:]
            self.assertEqual(
                list_service_response[0][mapped],
                kws[key],
                "Check " + str(key) + " => " + str(mapped) +  " in createServiceOffering"
            )

        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_03_create_service_offering_with_cache_mode_type(self):
        """Test to create service offering with each one of the valid cache mode types : none, writeback and writethrough"""

        # Validate the following:
        # 1. createServiceOfferings should return a valid information
        #    for newly created offering
        # 2. The Cloud Database contains the valid information

        cache_mode_types=["none", "writeback", "writethrough"]
        for i in range(3):
            service_offering = ServiceOffering.create(
                self.apiclient,
                self.services["service_offerings"]["tiny"],
                cacheMode=cache_mode_types[i]
            )
            self.cleanup.append(service_offering)

            self.debug(
                "Created service offering with ID: %s" %
                service_offering.id)

            list_service_response = list_service_offering(
                self.apiclient,
                id=service_offering.id
            )
            self.assertEqual(
                isinstance(list_service_response, list),
                True,
                "Check list response returns a valid list"
            )

            self.assertNotEqual(
                len(list_service_response),
                0,
                "Check Service offering is created"
            )

            self.assertEqual(
                list_service_response[0].cpunumber,
                self.services["service_offerings"]["tiny"]["cpunumber"],
                "Check server id in createServiceOffering"
            )
            self.assertEqual(
                list_service_response[0].cpuspeed,
                self.services["service_offerings"]["tiny"]["cpuspeed"],
                "Check cpuspeed in createServiceOffering"
            )
            self.assertEqual(
                list_service_response[0].displaytext,
                self.services["service_offerings"]["tiny"]["displaytext"],
                "Check server displaytext in createServiceOfferings"
            )
            self.assertEqual(
                list_service_response[0].memory,
                self.services["service_offerings"]["tiny"]["memory"],
                "Check memory in createServiceOffering"
            )
            self.assertEqual(
                list_service_response[0].name,
                self.services["service_offerings"]["tiny"]["name"],
                "Check name in createServiceOffering"
            )
            self.assertEqual(
                list_service_response[0].cacheMode,
                cache_mode_types[i],
                "Check cacheMode in createServiceOffering"
            )
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_04_create_service_offering_with_invalid_cache_mode_type(self):
        """Test to create service offering with invalid cache mode type"""

        # Validate the following:
        # 1. createServiceOfferings should return a valid information
        #    for newly created offering
        # 2. The Cloud Database contains the valid information

        with self.assertRaises(Exception):
            service_offering = ServiceOffering.create(
                self.apiclient,
                self.services["service_offerings"]["tiny"],
                cacheMode="invalid_cache_mode_type"
            )
        return


class TestServiceOfferings(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    @classmethod
    def setUpClass(cls):
        testClient = super(TestServiceOfferings, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()

        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        cls.service_offering_1 = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        cls.service_offering_2 = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["tiny"]
        )
        template = get_test_template(
            cls.apiclient,
            cls.zone.id,
            cls.hypervisor
        )
        if template == FAILED:
            assert False, "get_test_template() failed to return template"

        # Set Zones and disk offerings
        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id


        # Create VMs, NAT Rules etc
        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )

        cls.small_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["small"]
        )

        cls.medium_offering = ServiceOffering.create(
            cls.apiclient,
            cls.services["service_offerings"]["medium"]
        )
        cls.medium_virtual_machine = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.medium_offering.id,
            mode=cls.services["mode"]
        )
        cls._cleanup = [
            cls.small_offering,
            cls.medium_offering,
            cls.account
        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestServiceOfferings,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created templates
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_02_edit_service_offering(self):
        """Test to update existing service offering"""

        # Validate the following:
        # 1. updateServiceOffering should return
        #    a valid information for newly created offering

        # Generate new name & displaytext from random data
        random_displaytext = random_gen()
        random_name = random_gen()

        self.debug("Updating service offering with ID: %s" %
                   self.service_offering_1.id)

        cmd = updateServiceOffering.updateServiceOfferingCmd()
        # Add parameters for API call
        cmd.id = self.service_offering_1.id
        cmd.displaytext = random_displaytext
        cmd.name = random_name
        self.apiclient.updateServiceOffering(cmd)

        list_service_response = list_service_offering(
            self.apiclient,
            id=self.service_offering_1.id
        )
        self.assertEqual(
            isinstance(list_service_response, list),
            True,
            "Check list response returns a valid list"
        )

        self.assertNotEqual(
            len(list_service_response),
            0,
            "Check Service offering is updated"
        )

        self.assertEqual(
            list_service_response[0].displaytext,
            random_displaytext,
            "Check server displaytext in updateServiceOffering"
        )
        self.assertEqual(
            list_service_response[0].name,
            random_name,
            "Check server name in updateServiceOffering"
        )

        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="false")
    def test_03_delete_service_offering(self):
        """Test to delete service offering"""

        # Validate the following:
        # 1. deleteServiceOffering should return
        #    a valid information for newly created offering

        self.debug("Deleting service offering with ID: %s" %
                   self.service_offering_2.id)

        self.service_offering_2.delete(self.apiclient)

        list_service_response = list_service_offering(
            self.apiclient,
            id=self.service_offering_2.id
        )

        self.assertEqual(
            list_service_response,
            None,
            "Check if service offering exists in listDiskOfferings"
        )

        return

    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_04_change_offering_small(self):
        """Test to change service to a small capacity
        """
        # Validate the following
        # 1. Log in to the Vm .We should see that the CPU and memory Info of
        #    this Vm matches the one specified for "Small" service offering.
        # 2. Using  listVM command verify that this Vm
        #    has Small service offering Id.

        if self.hypervisor.lower() == "lxc":
            self.skipTest("Skipping this test for {} due to bug CS-38153".format(self.hypervisor))
        try:
            self.medium_virtual_machine.stop(self.apiclient)
        except Exception as e:
            self.fail("Failed to stop VM: %s" % e)

        cmd = changeServiceForVirtualMachine.changeServiceForVirtualMachineCmd()
        cmd.id = self.medium_virtual_machine.id
        cmd.serviceofferingid = self.small_offering.id
        self.apiclient.changeServiceForVirtualMachine(cmd)

        self.debug("Starting VM - ID: %s" % self.medium_virtual_machine.id)
        self.medium_virtual_machine.start(self.apiclient)
        # Ensure that VM is in running state
        list_vm_response = list_virtual_machines(
            self.apiclient,
            id=self.medium_virtual_machine.id
        )

        if isinstance(list_vm_response, list):
            vm = list_vm_response[0]
            if vm.state == 'Running':
                self.debug("VM state: %s" % vm.state)
            else:
                raise Exception(
                    "Failed to start VM (ID: %s) after changing\
                            service offering" % vm.id)

        try:
            ssh = self.medium_virtual_machine.get_ssh_client()
        except Exception as e:
            self.fail(
                "SSH Access failed for %s: %s" %
                (self.medium_virtual_machine.ipaddress, e)
            )

        cpuinfo = ssh.execute("cat /proc/cpuinfo")
        cpu_cnt = len([i for i in cpuinfo if "processor" in i])
        # 'cpu MHz\t\t: 2660.499'
        cpu_speed = [i for i in cpuinfo if "cpu MHz" in i][0].split()[3]
        meminfo = ssh.execute("cat /proc/meminfo")
        # MemTotal:        1017464 kB
        total_mem = [i for i in meminfo if "MemTotal" in i][0].split()[1]

        self.debug(
            "CPU count: %s, CPU Speed: %s, Mem Info: %s" % (
                cpu_cnt,
                cpu_speed,
                total_mem
            ))
        self.assertAlmostEqual(
            int(cpu_cnt),
            self.small_offering.cpunumber,
            "Check CPU Count for small offering"
        )
        self.assertAlmostEqual(
            list_vm_response[0].cpuspeed,
            self.small_offering.cpuspeed,
            "Check CPU Speed for small offering"
        )

        range = 25
        if self.hypervisor.lower() == "hyperv":
            range = 200
        # TODO: Find the memory allocated to VM on hyperv hypervisor using
        # powershell commands and use that value to equate instead of
        # manipulating range, currently we get the memory count much less
        # because of the UI component
        self.assertTrue(
            isAlmostEqual(int(int(total_mem) / 1024),
                          int(self.small_offering.memory),
                          range=range
                          ),
            "Check Memory(kb) for small offering"
        )
        return

class TestCpuCapServiceOfferings(cloudstackTestCase):

    def setUp(self):
        self.apiclient = self.testClient.getApiClient()
        self.dbclient = self.testClient.getDbConnection()
        self.cleanup = []

    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return

    def get_ssh_client(self, id, public_ip, username, password, retries):
        """ Setup ssh client connection and return connection
        vm requires attributes public_ip, public_port, username, password """

        try:
            ssh_client = SshClient(
                public_ip,
                22,
                username,
                password,
                retries)

        except Exception as e:
            self.fail("Unable to create ssh connection: " % e)

        self.assertIsNotNone(
            ssh_client, "Failed to setup ssh connection to host=%s on public_ip=%s" % (id, public_ip))

        return ssh_client

    @classmethod
    def setUpClass(cls):
        testClient = super(TestCpuCapServiceOfferings, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.services = testClient.getParsedTestDataConfig()
        cls.hypervisor = testClient.getHypervisorInfo()
        cls._cleanup = []
        cls.hypervisorNotSupported = False
        if cls.hypervisor.lower() not in ["kvm"]:
            cls.hypervisorNotSupported = True
            return

        domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient, testClient.getZoneForTests())
        cls.services['mode'] = cls.zone.networktype

        template = get_test_template(cls.apiclient, cls.zone.id, cls.hypervisor)
        if template == FAILED:
            assert False, "get_test_template() failed to return template"

        cls.services["small"]["zoneid"] = cls.zone.id
        cls.services["small"]["template"] = template.id
        cls.services["small"]["hypervisor"] = cls.hypervisor
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        cls.account = Account.create(
            cls.apiclient,
            cls.services["account"],
            domainid=domain.id
        )

        offering_data = {
            'displaytext': 'TestOffering',
            'cpuspeed': 512,
            'cpunumber': 2,
            'name': 'TestOffering',
            'memory': 1024
        }

        cls.offering = ServiceOffering.create(
            cls.apiclient,
            offering_data,
            limitcpuuse=True
        )

        def getHost(self, hostId=None):
            response = list_hosts(
                self.apiclient,
                type='Routing',
                hypervisor='kvm',
                id=hostId
            )
            # Check if more than one kvm hosts are available in order to successfully configure host-ha
            if response and len(response) > 0:
                self.host = response[0]
                return self.host
            raise self.skipTest("Not enough KVM hosts found, skipping host-ha test")

        cls.host = getHost(cls)

        cls.vm = VirtualMachine.create(
            cls.apiclient,
            cls.services["small"],
            accountid=cls.account.name,
            domainid=cls.account.domainid,
            serviceofferingid=cls.offering.id,
            mode=cls.services["mode"],
            hostid=cls.host.id

        )
        cls._cleanup = [
            cls.offering,
            cls.account
        ]

    @classmethod
    def tearDownClass(cls):
        try:
            cls.apiclient = super(
                TestCpuCapServiceOfferings,
                cls).getClsTestClient().getApiClient()
            # Clean up, terminate the created templates
            cleanup_resources(cls.apiclient, cls._cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @skipTestIf("hypervisorNotSupported")
    @attr(tags=["advanced", "advancedns", "smoke"], required_hardware="true")
    def test_01_service_offering_cpu_limit_use(self):
        """
        Test CPU Cap on KVM
        """

        ssh_host = self.get_ssh_client(self.host.id, self.host.ipaddress, self.hostConfig["username"], self.hostConfig["password"], 10)

        #Get host CPU usage from top command before and after VM consuming 100% CPU
        find_pid_cmd = "ps -ax | grep '%s' | head -1 | awk '{print $1}'" % self.vm.id
        pid = ssh_host.execute(find_pid_cmd)[0]
        cpu_usage_cmd = "top -b n 1 p %s | tail -1 | awk '{print $9}'" % pid
        host_cpu_usage_before_str = ssh_host.execute(cpu_usage_cmd)[0]

        host_cpu_usage_before = round(float(host_cpu_usage_before_str))
        self.debug("Host CPU usage before the infinite loop on the VM: " + str(host_cpu_usage_before))

        #Execute loop command in background on the VM
        ssh_vm = self.vm.get_ssh_client(reconnect=True)
        ssh_vm.execute("echo 'while true; do x=$(($x+1)); done' > cputest.sh")
        ssh_vm.execute("sh cputest.sh > /dev/null 2>&1 &")

        time.sleep(5)
        host_cpu_usage_after_str = ssh_host.execute(cpu_usage_cmd)[0]
        host_cpu_usage_after = round(float(host_cpu_usage_after_str))
        self.debug("Host CPU usage after the infinite loop on the VM: " + str(host_cpu_usage_after))

        limit = 95
        self.assertTrue(host_cpu_usage_after < limit, "Host CPU usage after VM usage increased is high")

        return
