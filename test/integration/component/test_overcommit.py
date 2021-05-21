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

""" test for feature "CPU and MEM OVERCOMMIT"
"""

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Account,
                             Capacities,
                             Cluster,
                             Configurations,
                             FAILED,
                             Host,
                             PASS,
                             ServiceOffering,
                             time,
                             VirtualMachine,)
from marvin.lib.utils import cleanup_resources, validateList
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template)
from marvin.sshClient import SshClient
from nose.plugins.attrib import attr


def ssh_xen_host(password, username, ipaddr, instance_name):
    """Ssh into xen host and get vm mem details"""
    mem = []
    sshClient = SshClient(
        ipaddr,
        22,
        username,
        password
    )
    command = "xe vm-list params=all name-label=%s" % instance_name
    vm_detail = sshClient.execute(command)
    max_str = vm_detail[17].split(":")
    min_str = vm_detail[20].split(":")
    max = int(max_str[1])
    min = int(min_str[1])
    mem.append(max)
    mem.append(min)
    return mem


def ssh_kvm_host(password, user, ipaddr, instance_name):
    """Ssh into kvm host and get vm mem details"""
    mem = []

    sshClient = SshClient(
        ipaddr,
        22,
        user,
        password
    )

    command = "virsh dominfo %s" % instance_name
    vm_detail = sshClient.execute(command)
    max = vm_detail[7].split()
    min = vm_detail[8].split()
    mem.append(int(max[2]))
    mem.append(int(min[2]))
    return mem


def capacity_parser(capacity):
    cpu = []
    mem = []
    cpu.append(capacity[0].capacitytotal)
    cpu.append(capacity[0].capacityused)
    cpu.append(capacity[0].percentused)
    mem.append(capacity[1].capacitytotal)
    mem.append(capacity[1].capacityused)
    mem.append(capacity[1].percentused)
    return cpu, mem


class Overcommit (cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        testClient = super(Overcommit, cls).getClsTestClient()
        cls.apiclient = testClient.getApiClient()
        cls.testdata = testClient.getParsedTestDataConfig()
        # Get Zone,Domain and templates
        cls.domain = get_domain(cls.apiclient)
        cls.zone = get_zone(cls.apiclient)
        cls.testdata["mode"] = cls.zone.networktype
        cls.hypervisor = testClient.getHypervisorInfo()
        cls.hostConfig = cls.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        cls.template = get_template(
            cls.apiclient,
            cls.zone.id,
            cls.testdata["ostype"])
        cls.testdata["template"]["ostypeid"] = cls.template.ostypeid
        list_conf = Configurations.list(cls.apiclient,
                                        name="capacity.check.period"
                                        )
        cls.wait_time = 5 + int(list_conf[0].value) / 1000
        if cls.template == FAILED:
            cls.fail(
                "get_template() failed to return template with description \
                %s" %
                cls.testdata["ostype"])
        cls._cleanup = []
        try:
            cls.account = Account.create(cls.apiclient,
                                         cls.testdata["account"],
                                         domainid=cls.domain.id
                                         )
            cls._cleanup.append(cls.account)

            cls.service_offering = ServiceOffering.create(
                cls.apiclient,
                cls.testdata["service_offerings"]["small"])

            cls._cleanup.append(cls.service_offering)

            cls.deployVmResponse = VirtualMachine.create(
                cls.apiclient,
                services=cls.testdata["virtual_machine"],
                accountid=cls.account.name,
                domainid=cls.account.domainid,
                serviceofferingid=cls.service_offering.id,
                templateid=cls.template.id,
                zoneid=cls.zone.id,
            )

        except Exception as e:
            cls.tearDownClass()
            raise e

        return

    @classmethod
    def tearDownClass(cls):
        try:
            cleanup_resources(cls.apiclient, cls._cleanup)
        except Exception as e:
            raise Exception("Warning:Exception during cleanup: %s" % e)

    @attr(
        tags=[
            "simulator",
            "devcloud",
            "basic",
            "advanced"],
        required_hardware="false")
    def test_01_cluster_settings(self):
        """change cpu/mem.overprovisioning.factor at cluster level and
         verify the change """
        listHost = Host.list(self.apiclient,
                             id=self.deployVmResponse.hostid
                             )
        self.assertEqual(
            validateList(listHost)[0],
            PASS,
            "check list host response for host id %s" %
            self.deployVmResponse.hostid)
        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="mem.overprovisioning.factor",
                              value=2)

        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="cpu.overprovisioning.factor",
                              value=3)

        list_cluster = Cluster.list(self.apiclient,
                                    id=listHost[0].clusterid)
        self.assertEqual(
            validateList(list_cluster)[0],
            PASS,
            "check list cluster response for cluster id %s" %
            listHost[0].clusterid)
        self.assertEqual(int(list_cluster[0].cpuovercommitratio),
                         3,
                         "check the cpu overcommit value at cluster level ")

        self.assertEqual(int(list_cluster[0].memoryovercommitratio),
                         2,
                         "check memory overcommit value at cluster level")

        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="mem.overprovisioning.factor",
                              value=1)

        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="cpu.overprovisioning.factor",
                              value=1)
        list_cluster1 = Cluster.list(self.apiclient,
                                     id=listHost[0].clusterid)
        self.assertEqual(
            validateList(list_cluster1)[0],
            PASS,
            "check the list cluster response for id %s" %
            listHost[0].clusterid)
        self.assertEqual(int(list_cluster1[0].cpuovercommitratio),
                         1,
                         "check the cpu overcommit value at cluster level ")

        self.assertEqual(int(list_cluster1[0].memoryovercommitratio),
                         1,
                         "check memory overcommit value at cluster level")

    @attr(
        tags=["simulator",
              "devcloud",
              "basic",
              "advanced"],
        required_hardware="true")
    def test_02_Overcommit_factor(self):
        """change mem.overprovisioning.factor and verify vm memory """

        listHost = Host.list(self.apiclient,
                             id=self.deployVmResponse.hostid
                             )
        self.assertEqual(
            validateList(listHost)[0],
            PASS,
            "check list host for host id %s" %
            self.deployVmResponse.hostid)
        if listHost[0].hypervisor.lower() not in ['kvm', 'xenserver']:
            self.skipTest(
                "Skiping test because of not supported hypervisor type %s" %
                listHost[0].hypervisor)

        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="mem.overprovisioning.factor",
                              value=1)

        self.deployVmResponse.stop(self.apiclient)
        self.deployVmResponse.start(self.apiclient)

        if listHost[0].hypervisor.lower() == 'xenserver':

            k = ssh_xen_host(
                self.hostConfig["password"],
                self.hostConfig["username"],
                listHost[0].ipaddress,
                self.deployVmResponse.instancename)

        elif listHost[0].hypervisor.lower() == 'kvm':

            k = ssh_kvm_host(
                self.hostConfig["password"],
                self.hostConfig["username"],
                listHost[0].ipaddress,
                self.deployVmResponse.instancename)

        self.assertEqual(k[0],
                         k[1],
                         "Check static max ,min on host for overcommit 1 ")

        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="mem.overprovisioning.factor",
                              value=2)

        self.deployVmResponse.stop(self.apiclient)
        self.deployVmResponse.start(self.apiclient)

        if listHost[0].hypervisor.lower() == 'xenserver':
            k1 = ssh_xen_host(
                self.hostConfig["password"],
                self.hostConfig["username"],
                listHost[0].ipaddress,
                self.deployVmResponse.instancename)

        elif listHost[0].hypervisor.lower() == 'kvm':
            time.sleep(200)
            k1 = ssh_kvm_host(
                self.hostConfig["password"],
                self.hostConfig["username"],
                listHost[0].ipaddress,
                self.deployVmResponse.instancename)
        self.assertEqual(k1[0],
                         2 * k1[1],
                         "Check static max ,min on  host for overcommit 2")

    @attr(
        tags=[
            "simulator",
            "devcloud",
            "basic",
            "advanced"],
        required_hardware="false")
    def test_03_cluste_capacity_check(self):
        """change cpu/mem.overprovisioning.factor at cluster level and
           verify cluster capacity """

        listHost = Host.list(self.apiclient,
                             id=self.deployVmResponse.hostid
                             )
        self.assertEqual(
            validateList(listHost)[0],
            PASS,
            "check list host for host id %s" %
            self.deployVmResponse.hostid)

        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="mem.overprovisioning.factor",
                              value=1)
        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="cpu.overprovisioning.factor",
                              value=1)

        time.sleep(self.wait_time)

        capacity = Capacities.list(self.apiclient,
                                   clusterid=listHost[0].clusterid)
        self.assertEqual(
            validateList(capacity)[0],
            PASS,
            "check list capacity response for cluster id %s" %
            listHost[0].clusterid)
        cpu, mem = capacity_parser(capacity)

        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="mem.overprovisioning.factor",
                              value=2)
        Configurations.update(self.apiclient,
                              clusterid=listHost[0].clusterid,
                              name="cpu.overprovisioning.factor",
                              value=2)

        time.sleep(self.wait_time)

        capacity1 = Capacities.list(self.apiclient,
                                    clusterid=listHost[0].clusterid)
        self.assertEqual(
            validateList(capacity1)[0],
            PASS,
            "check list capacity response for cluster id %s" %
            listHost[0].clusterid)
        cpu1, mem1 = capacity_parser(capacity1)
        self.assertEqual(2 * cpu[0],
                         cpu1[0],
                         "check total capacity ")
        self.assertEqual(2 * cpu[1],
                         cpu1[1],
                         "check capacity used")
        self.assertEqual(cpu[2],
                         cpu1[2],
                         "check capacity % used")

        self.assertEqual(2 * mem[0],
                         mem1[0],
                         "check mem total capacity ")
        self.assertEqual(2 * mem[1],
                         mem1[1],
                         "check mem capacity used")
        self.assertEqual(mem[2],
                         mem1[2],
                         "check mem capacity % used")

    @attr(
        tags=[
            "simulator",
            "devcloud",
            "basic",
            "advanced"],
        required_hardware="false")
    def test_04_zone_capacity_check(self):
        """change cpu/mem.overprovisioning.factor at cluster level for
           all cluster in a zone  and  verify capacity at zone level """
        list_cluster = Cluster.list(self.apiclient,
                                    zoneid=self.zone.id)
        self.assertEqual(
            validateList(list_cluster)[0],
            PASS,
            "check list cluster response for zone id  %s" %
            self.zone.id)
        k = len(list_cluster)
        for id in range(k):
            Configurations.update(self.apiclient,
                                  clusterid=list_cluster[id].id,
                                  name="mem.overprovisioning.factor",
                                  value=1)
            Configurations.update(self.apiclient,
                                  clusterid=list_cluster[id].id,
                                  name="cpu.overprovisioning.factor",
                                  value=1)

        time.sleep(self.wait_time)

        capacity = Capacities.list(self.apiclient,
                                   zoneid=self.zone.id)
        self.assertEqual(
            validateList(capacity)[0],
            PASS,
            "check list capacity response for zone id %s" %
            self.zone.id)
        cpu, mem = capacity_parser(capacity)
        for id in range(k):
            Configurations.update(self.apiclient,
                                  clusterid=list_cluster[id].id,
                                  name="mem.overprovisioning.factor",
                                  value=2)
            Configurations.update(self.apiclient,
                                  clusterid=list_cluster[id].id,
                                  name="cpu.overprovisioning.factor",
                                  value=2)

        time.sleep(self.wait_time)

        capacity1 = Capacities.list(self.apiclient,
                                    zoneid=self.zone.id)
        self.assertEqual(validateList(capacity1)[0],
                         PASS,
                         "check list capacity for zone id %s" % self.zone.id)

        cpu1, mem1 = capacity_parser(capacity1)
        self.assertEqual(2 * cpu[0],
                         cpu1[0],
                         "check total capacity ")
        self.assertEqual(2 * cpu[1],
                         cpu1[1],
                         "check capacity used")
        self.assertEqual(cpu[2],
                         cpu1[2],
                         "check capacity % used")

        self.assertEqual(2 * mem[0],
                         mem1[0],
                         "check mem total capacity ")
        self.assertEqual(2 * mem[1],
                         mem1[1],
                         "check mem capacity used")
        self.assertEqual(mem[2],
                         mem1[2],
                         "check mem capacity % used")
        for id in range(k):
            Configurations.update(self.apiclient,
                                  clusterid=list_cluster[id].id,
                                  name="mem.overprovisioning.factor",
                                  value=1)
            Configurations.update(self.apiclient,
                                  clusterid=list_cluster[id].id,
                                  name="cpu.overprovisioning.factor",
                                  value=1)
