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
from builtins import False
""" BVT tests for Hosts Maintenance
"""

# Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr

from time import sleep

_multiprocess_shared_ = False


class TestHostHA(cloudstackTestCase):

    def setUp(self):
        self.logger = logging.getLogger('TestHM')
        self.stream_handler = logging.StreamHandler()
        self.logger.setLevel(logging.DEBUG)
        self.logger.addHandler(self.stream_handler)
        self.apiclient = self.testClient.getApiClient()
        self.hypervisor = self.testClient.getHypervisorInfo()
        self.dbclient = self.testClient.getDbConnection()
        self.services = self.testClient.getParsedTestDataConfig()
        self.zone = get_zone(self.apiclient, self.testClient.getZoneForTests())
        self.pod = get_pod(self.apiclient, self.zone.id)
        self.hostConfig = self.config.__dict__["zones"][0].__dict__["pods"][0].__dict__["clusters"][0].__dict__["hosts"][0].__dict__

        self.cleanup = []
        self.services = {
                            "service_offering": {
                                "name": "Ultra Tiny Instance",
                                "displaytext": "Ultra Tiny Instance",
                                "cpunumber": 1,
                                "cpuspeed": 100,
                                "memory": 128,
                            },
                            "service_offering_local": {
                                "name": "Ultra Tiny Local Instance",
                                "displaytext": "Ultra Tiny Local Instance",
                                "cpunumber": 1,
                                "cpuspeed": 100,
                                "memory": 128,
                                "storagetype": "local"
                            },
                            "vm": {
                                "username": "root",
                                "password": "password",
                                "ssh_port": 22,
                                # Hypervisor type should be same as
                                # hypervisor type of cluster
                                "privateport": 22,
                                "publicport": 22,
                                "protocol": 'TCP',
                            },
                            "natrule": {
                                "privateport": 22,
                                "publicport": 22,
                                "startport": 22,
                                "endport": 22,
                                "protocol": "TCP",
                                "cidrlist": '0.0.0.0/0',
                            },
                         "ostype": 'CentOS 5.3 (64-bit)',
                         "sleep": 60,
                         "timeout": 10,
                         }


    def tearDown(self):
        try:
            # Clean up, terminate the created templates
            cleanup_resources(self.apiclient, self.cleanup)

        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)

        return


    def createVMs(self, hostId, number, local):

        self.template = get_template(
            self.apiclient,
            self.zone.id,
            self.services["ostype"]
        )

        if self.template == FAILED:
            assert False, "get_template() failed to return template with description %s" % self.services["ostype"]

        self.logger.debug("Using template %s " % self.template.id)

        if local:
            self.service_offering = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering_local"]
            )
        else:
            self.service_offering = ServiceOffering.create(
                self.apiclient,
                self.services["service_offering"]
            )

        self.logger.debug("Using service offering %s " % self.service_offering.id)

        vms = []
        for i in range(0, number):
            self.services["vm"]["zoneid"] = self.zone.id
            self.services["vm"]["template"] = self.template.id
            self.services["vm"]["displayname"] = 'vm' + str(i)
            self.services["vm"]["hypervisor"] = self.hypervisor
            vm = VirtualMachine.create(
                self.apiclient,
                self.services["vm"],
                serviceofferingid=self.service_offering.id,
                hostid=hostId
            )
            vms.append(vm)
            self.cleanup.append(vm)
            self.logger.debug("VM create = {}".format(vm.id))
        return vm

    def noOfVMsOnHost(self, hostId):
        listVms = VirtualMachine.list(
                       self.apiclient,
                       hostid=hostId
                       )
        vmnos = 0
        if (listVms is not None):
            for vm in listVms:
                self.logger.debug('VirtualMachine on Hyp 1 = {}'.format(vm.id))
                vmnos = vmnos + 1

        return vmnos

    def checkHostDown(self, fromHostIp, testHostIp):
        try:
            ssh = SshClient(fromHostIp, 22, self.hostConfig["username"],
                    self.hostConfig["password"])
            res = ssh.execute("ping -c 1 %s" % testHostIp)
            result = str(res)
            if result.count("100% packet loss") == 1:
                return True, 1
            else:
                return False, 1
        except Exception as e:
            self.logger.debug("Got exception %s" % e)
            return False, 1

    def checkHostUp(self, fromHostIp, testHostIp):
        try:
            ssh = SshClient(fromHostIp, 22, self.hostConfig["username"],
                    self.hostConfig["password"])
            res = ssh.execute("ping -c 1 %s" % testHostIp)
            result = str(res)
            if result.count(" 0% packet loss") == 1:
                return True, 1
            else:
                return False, 1
        except Exception as e:
            self.logger.debug("Got exception %s" % e)
            return False, 1


    def isOnlyNFSStorageAvailable(self):
        if self.zone.localstorageenabled:
            return False
        storage_pools = StoragePool.list(
                   self.apiclient,
                   zoneid=self.zone.id,
                   listall=True
                    )
        self.assertEqual(
                           isinstance(storage_pools, list),
                           True,
                           "Check if listStoragePools returns a valid response"
                           )
        for storage_pool in storage_pools:
            if storage_pool.type == 'NetworkFilesystem':
                return True

        return False

    def isOnlyLocalStorageAvailable(self):
        if not(self.zone.localstorageenabled):
            return False

        storage_pools = StoragePool.list(
                   self.apiclient,
                   zoneid=self.zone.id,
                   listall=True
                    )
        self.assertEqual(
                           isinstance(storage_pools, list),
                           True,
                           "Check if listStoragePools returns a valid response"
                           )
        for storage_pool in storage_pools:
            if storage_pool.type == 'NetworkFilesystem':
                return False

        return True

    def isLocalAndNFSStorageAvailable(self):
        if not(self.zone.localstorageenabled):
            return False

        storage_pools = StoragePool.list(
                   self.apiclient,
                   zoneid=self.zone.id,
                   listall=True
                    )
        self.assertEqual(
                           isinstance(storage_pools, list),
                           True,
                           "Check if listStoragePools returns a valid response"
                           )
        for storage_pool in storage_pools:
            if storage_pool.type == 'NetworkFilesystem':
                return True

        return False


    def checkHostStateInCloudstack(self, state, hostId):
        try:
            listHost = Host.list(
                   self.apiclient,
                   type='Routing',
                   zoneid=self.zone.id,
                   podid=self.pod.id,
                   id=hostId
               )
            self.assertEqual(
                           isinstance(listHost, list),
                           True,
                           "Check if listHost returns a valid response"
                           )

            self.assertEqual(
                           len(listHost),
                           1,
                           "Check if listHost returns a host"
                           )
            self.logger.debug(" Host state is %s " % listHost[0].state)
            if listHost[0].state == state:
                return True, 1
            else:
                return False, 1
        except Exception as e:
            self.logger.debug("Got exception %s" % e)
            return False, 1


    def disconnectHostfromNetwork(self, hostIp, timeout):
        srcFile = os.path.dirname(os.path.realpath(__file__)) + "/test_host_ha.sh"
        if not(os.path.isfile(srcFile)):
            self.logger.debug("File %s not found" % srcFile)
            raise unittest.SkipTest("Script file %s required for HA not found" % srcFile)

        ssh = SshClient(hostIp, 22, self.hostConfig["username"],
                    self.hostConfig["password"])
        ssh.scp(srcFile, "/root/test_host_ha.sh")
        ssh.execute("nohup sh /root/test_host_ha.sh -t %s -d all > /dev/null 2>&1 &\n" % timeout)
        return

    def stopAgentOnHost(self, hostIp, timeout):
        srcFile = os.path.dirname(os.path.realpath(__file__)) + "/test_host_ha.sh"
        if not(os.path.isfile(srcFile)):
            self.logger.debug("File %s not found" % srcFile)
            raise unittest.SkipTest("Script file %s required for HA not found" % srcFile)

        ssh = SshClient(hostIp, 22, self.hostConfig["username"], self.hostConfig["password"])
        ssh.scp(srcFile, "/root/test_host_ha.sh")
        ssh.execute("nohup sh /root/test_host_ha.sh -t %s -d agent > /dev/null 2>&1 &\n" % timeout)
        return

    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="true")
    def test_01_host_ha_with_nfs_storagepool_with_vm(self):

        if not(self.isOnlyNFSStorageAvailable()):
            raise unittest.SkipTest("Skipping this test as this is for NFS store only.")

        listHost = Host.list(
            self.apiclient,
            type='Routing',
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        for host in listHost:
            self.logger.debug('Hypervisor = {}'.format(host.id))


        if len(listHost) != 2:
            self.logger.debug("Host HA can be tested with two host only %s, found" % len(listHost))
            raise unittest.SkipTest("Host HA can be tested with two host only %s, found" % len(listHost))


        no_of_vms = self.noOfVMsOnHost(listHost[0].id)

        no_of_vms = no_of_vms + self.noOfVMsOnHost(listHost[1].id)

        self.logger.debug("Number of VMS on hosts = %s" % no_of_vms)


        if no_of_vms < 5:
            self.logger.debug("test_01: Create VMs as there are not enough vms to check host ha")
            no_vm_req = 5 - no_of_vms
            if (no_vm_req > 0):
                self.logger.debug("Creating vms = {}".format(no_vm_req))
                self.vmlist = self.createVMs(listHost[0].id, no_vm_req, False)

        ha_host = listHost[1]
        other_host = listHost[0]
        if self.noOfVMsOnHost(listHost[0].id) > self.noOfVMsOnHost(listHost[1].id):
            ha_host = listHost[0]
            other_host = listHost[1]

        self.disconnectHostfromNetwork(ha_host.ipaddress, 400)

        hostDown = wait_until(10, 10, self.checkHostDown, other_host.ipaddress, ha_host.ipaddress)
        if not(hostDown):
            raise unittest.SkipTest("Host %s is not down, cannot proceed with test" % (ha_host.ipaddress))

        hostDownInCloudstack = wait_until(40, 10, self.checkHostStateInCloudstack, "Down", ha_host.id)
        #the test could have failed here but we will try our best to get host back in consistent state

        no_of_vms = self.noOfVMsOnHost(ha_host.id)
        no_of_vms = no_of_vms + self.noOfVMsOnHost(other_host.id)
        self.logger.debug("Number of VMS on hosts = %s" % no_of_vms)
            #
        hostUp = wait_until(10, 10, self.checkHostUp, other_host.ipaddress, ha_host.ipaddress)
        if not(hostUp):
            self.logger.debug("Host is down %s, though HA went fine, the environment is not consistent " % (ha_host.ipaddress))


        hostUpInCloudstack = wait_until(40, 10, self.checkHostStateInCloudstack, "Up", ha_host.id)

        if not(hostDownInCloudstack):
            raise self.fail("Host is not down %s, in cloudstack so failing test " % (ha_host.ipaddress))
        if not(hostUpInCloudstack):
            raise self.fail("Host is not up %s, in cloudstack so failing test " % (ha_host.ipaddress))

        return


    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="true")
    def test_02_host_ha_with_local_storage_and_nfs(self):

        if not(self.isLocalAndNFSStorageAvailable()):
            raise unittest.SkipTest("Skipping this test as this is for Local storage and NFS storage only.")

        listHost = Host.list(
            self.apiclient,
            type='Routing',
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        for host in listHost:
            self.logger.debug('Hypervisor = {}'.format(host.id))


        if len(listHost) != 2:
            self.logger.debug("Host HA can be tested with two host only %s, found" % len(listHost))
            raise unittest.SkipTest("Host HA can be tested with two host only %s, found" % len(listHost))

        no_of_vms = self.noOfVMsOnHost(listHost[0].id)

        no_of_vms = no_of_vms + self.noOfVMsOnHost(listHost[1].id)

        self.logger.debug("Number of VMS on hosts = %s" % no_of_vms)


        if no_of_vms < 5:
            self.logger.debug("test_02: Create VMs as there are not enough vms to check host ha")
            no_vm_req = 5 - no_of_vms
            if (no_vm_req > 0):
                self.logger.debug("Creating vms = {}".format(no_vm_req))
                self.vmlist = self.createVMs(listHost[0].id, no_vm_req, True)

        ha_host = listHost[1]
        other_host = listHost[0]
        if self.noOfVMsOnHost(listHost[0].id) > self.noOfVMsOnHost(listHost[1].id):
            ha_host = listHost[0]
            other_host = listHost[1]

        self.disconnectHostfromNetwork(ha_host.ipaddress, 400)

        hostDown = wait_until(10, 10, self.checkHostDown, other_host.ipaddress, ha_host.ipaddress)
        if not(hostDown):
            raise unittest.SkipTest("Host %s is not down, cannot proceed with test" % (ha_host.ipaddress))

        hostDownInCloudstack = wait_until(40, 10, self.checkHostStateInCloudstack, "Down", ha_host.id)
        #the test could have failed here but we will try our best to get host back in consistent state

        no_of_vms = self.noOfVMsOnHost(ha_host.id)
        no_of_vms = no_of_vms + self.noOfVMsOnHost(other_host.id)
        self.logger.debug("Number of VMS on hosts = %s" % no_of_vms)
            #
        hostUp = wait_until(10, 10, self.checkHostUp, other_host.ipaddress, ha_host.ipaddress)
        if not(hostUp):
            self.logger.debug("Host is down %s, though HA went fine, the environment is not consistent " % (ha_host.ipaddress))


        hostUpInCloudstack = wait_until(40, 10, self.checkHostStateInCloudstack, "Up", ha_host.id)

        if not(hostDownInCloudstack):
            raise self.fail("Host is not down %s, in cloudstack so failing test " % (ha_host.ipaddress))
        if not(hostUpInCloudstack):
            raise self.fail("Host is not up %s, in cloudstack so failing test " % (ha_host.ipaddress))

        return



    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="true")
    def test_03_host_ha_with_only_local_storage(self):

        if not(self.isOnlyLocalStorageAvailable()):
            raise unittest.SkipTest("Skipping this test as this is for Local storage only.")

        listHost = Host.list(
            self.apiclient,
            type='Routing',
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        for host in listHost:
            self.logger.debug('Hypervisor = {}'.format(host.id))


        if len(listHost) != 2:
            self.logger.debug("Host HA can be tested with two host only %s, found" % len(listHost))
            raise unittest.SkipTest("Host HA can be tested with two host only %s, found" % len(listHost))

        no_of_vms = self.noOfVMsOnHost(listHost[0].id)

        no_of_vms = no_of_vms + self.noOfVMsOnHost(listHost[1].id)

        self.logger.debug("Number of VMS on hosts = %s" % no_of_vms)

        if no_of_vms < 5:
            self.logger.debug("test_03: Create VMs as there are not enough vms to check host ha")
            no_vm_req = 5 - no_of_vms
            if (no_vm_req > 0):
                self.logger.debug("Creating vms = {}".format(no_vm_req))
                self.vmlist = self.createVMs(listHost[0].id, no_vm_req, True)

        ha_host = listHost[1]
        other_host = listHost[0]
        if self.noOfVMsOnHost(listHost[0].id) > self.noOfVMsOnHost(listHost[1].id):
            ha_host = listHost[0]
            other_host = listHost[1]

        self.disconnectHostfromNetwork(ha_host.ipaddress, 400)

        hostDown = wait_until(10, 10, self.checkHostDown, other_host.ipaddress, ha_host.ipaddress)
        if not(hostDown):
            raise unittest.SkipTest("Host %s is not down, cannot proceed with test" % (ha_host.ipaddress))

        hostDownInCloudstack = wait_until(40, 10, self.checkHostStateInCloudstack, "Alert", ha_host.id)
        #the test could have failed here but we will try our best to get host back in consistent state

        no_of_vms = self.noOfVMsOnHost(ha_host.id)
        no_of_vms = no_of_vms + self.noOfVMsOnHost(other_host.id)
        self.logger.debug("Number of VMS on hosts = %s" % no_of_vms)
            #
        hostUp = wait_until(10, 10, self.checkHostUp, other_host.ipaddress, ha_host.ipaddress)
        if not(hostUp):
            self.logger.debug("Host is down %s, though HA went fine, the environment is not consistent " % (ha_host.ipaddress))


        hostUpInCloudstack = wait_until(40, 10, self.checkHostStateInCloudstack, "Up", ha_host.id)

        if not(hostDownInCloudstack):
            raise self.fail("Host is not in alert %s, in cloudstack so failing test " % (ha_host.ipaddress))
        if not(hostUpInCloudstack):
            raise self.fail("Host is not up %s, in cloudstack so failing test " % (ha_host.ipaddress))

        return


    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="true")
    def test_04_host_ha_vmactivity_check(self):

        if not(self.isOnlyNFSStorageAvailable()):
            raise unittest.SkipTest("Skipping this test as this is for NFS store only.")

        listHost = Host.list(
            self.apiclient,
            type='Routing',
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        for host in listHost:
            self.logger.debug('Hypervisor = {}'.format(host.id))


        if len(listHost) != 2:
            self.logger.debug("Host HA can be tested with two host only %s, found" % len(listHost))
            raise unittest.SkipTest("Host HA can be tested with two host only %s, found" % len(listHost))

        no_of_vms = self.noOfVMsOnHost(listHost[0].id)

        no_of_vms = no_of_vms + self.noOfVMsOnHost(listHost[1].id)

        self.logger.debug("Number of VMS on hosts = %s" % no_of_vms)


        if no_of_vms < 5:
            self.logger.debug("test_01: Create VMs as there are not enough vms to check host ha")
            no_vm_req = 5 - no_of_vms
            if (no_vm_req > 0):
                self.logger.debug("Creating vms = {}".format(no_vm_req))
                self.vmlist = self.createVMs(listHost[0].id, no_vm_req, False)

        ha_host = listHost[1]
        other_host = listHost[0]
        if self.noOfVMsOnHost(listHost[0].id) > self.noOfVMsOnHost(listHost[1].id):
            ha_host = listHost[0]
            other_host = listHost[1]

        self.stopAgentOnHost(ha_host.ipaddress, 150)

        hostDisconnectedInCloudstack = wait_until(40, 10, self.checkHostStateInCloudstack, "Disconnected", ha_host.id)
        #the test could have failed here but we will try our best to get host back in consistent state

        no_of_vms = self.noOfVMsOnHost(ha_host.id)
        no_of_vms = no_of_vms + self.noOfVMsOnHost(other_host.id)
        self.logger.debug("Number of VMS on hosts = %s" % no_of_vms)
            #

        hostUpInCloudstack = wait_until(40, 10, self.checkHostStateInCloudstack, "Up", ha_host.id)

        if not(hostDisconnectedInCloudstack):
            raise self.fail("Host is not disconnected %s, in cloudstack so failing test " % (ha_host.ipaddress))
        if not(hostUpInCloudstack):
            raise self.fail("Host is not up %s, in cloudstack so failing test " % (ha_host.ipaddress))

        return
