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


class TestHostMaintenance(cloudstackTestCase):

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
        self.cleanup = []
        self.services = {
                            "service_offering": {
                                "name": "Ultra Tiny Instance",
                                "displaytext": "Ultra Tiny Instance",
                                "cpunumber": 1,
                                "cpuspeed": 100,
                                "memory": 128,
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
    
    def createVMs(self, hostId, number):
        
        self.template = get_test_template(
            self.apiclient,
            self.zone.id,
            self.hypervisor
        )
            
        if self.template == FAILED:
            assert False, "get_test_template() failed to return template"
            
        self.logger.debug("Using template %s " % self.template.id)
                
        self.service_offering = ServiceOffering.create(
            self.apiclient,
            self.services["service_offering"]
        )
        self.logger.debug("Using service offering %s " % self.service_offering.id)
        
        vms=[]
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
        return vms
    
    def checkVmMigratingOnHost(self, hostId):
        vm_migrating=False
        listVms1 = VirtualMachine.list(
                                   self.apiclient, 
                                   hostid=hostId
                                   )

        if (listVms1 is not None):
            self.logger.debug('Vms found = {} '.format(len(listVms1)))
            for vm in listVms1:
                if (vm.state == "Migrating"):
                    self.logger.debug('VirtualMachine on Hyp id = {} is in {}'.format(vm.id, vm.state))
                    vm_migrating=True
                    break

        return (vm_migrating, None)
    
    def checkNoVmMigratingOnHost(self, hostId):
        no_vm_migrating=True
        listVms1 = VirtualMachine.list(
                                   self.apiclient, 
                                   hostid=hostId
                                   )

        if (listVms1 is not None):
            self.logger.debug('Vms found = {} '.format(len(listVms1)))
            for vm in listVms1:
                if (vm.state == "Migrating"):
                    self.logger.debug('VirtualMachine on Hyp id = {} is in {}'.format(vm.id, vm.state))
                    no_vm_migrating=False
                    break

        return (no_vm_migrating, None)
    
    def noOfVMsOnHost(self, hostId):
        listVms = VirtualMachine.list(
                                       self.apiclient, 
                                       hostid=hostId
                                       )
        no_of_vms=0
        if (listVms is not None):
            for vm in listVms:
                self.logger.debug('VirtualMachine on Hyp 1 = {}'.format(vm.id))
                no_of_vms=no_of_vms+1
             
        return no_of_vms
    
    def hostPrepareAndCancelMaintenance(self, target_host_id, other_host_id, checkVMMigration):
        
        cmd = prepareHostForMaintenance.prepareHostForMaintenanceCmd()
        cmd.id = target_host_id
        response = self.apiclient.prepareHostForMaintenance(cmd)
        
        self.logger.debug('Host with id {} is in prepareHostForMaintenance'.format(target_host_id))
        
        vm_migrating = wait_until(1, 10, checkVMMigration, other_host_id)
        
        cmd = cancelHostMaintenance.cancelHostMaintenanceCmd()
        cmd.id = target_host_id
        response = self.apiclient.cancelHostMaintenance(cmd)
        
        self.logger.debug('Host with id {} is in cancelHostMaintenance'.format(target_host_id) )
        
        return vm_migrating
        
    @attr(
        tags=[
            "advanced",
            "advancedns",
            "smoke",
            "basic",
            "eip",
            "sg"],
        required_hardware="true")
    def test_01_cancel_host_maintenace_with_no_migration_jobs(self):
        listHost = Host.list(
            self.apiclient,
            type='Routing',
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        for host in listHost:
            self.logger.debug('1 Hypervisor = {}'.format(host.id))
            
                  
        if (len(listHost) < 2):
            raise unittest.SkipTest("Cancel host maintenance when VMs are migrating should be tested for 2 or more hosts");
            return

        vm_migrating=False
        
        try:

           vm_migrating = self.hostPrepareAndCancelMaintenance(listHost[0].id, listHost[1].id, self.checkNoVmMigratingOnHost)
           
           vm_migrating = self.hostPrepareAndCancelMaintenance(listHost[1].id, listHost[0].id, self.checkNoVmMigratingOnHost)
           
        except Exception as e:
            self.logger.debug("Exception {}".format(e))
            self.fail("Cancel host maintenance failed {}".format(e[0]))
        

        if (vm_migrating == True):
            raise unittest.SkipTest("VMs are migrating and the test will not be able to check the conditions the test is intended for");
                
            
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
    def test_02_cancel_host_maintenace_with_migration_jobs(self):
        
        listHost = Host.list(
            self.apiclient,
            type='Routing',
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        for host in listHost:
            self.logger.debug('2 Hypervisor = {}'.format(host.id))
            
        if (len(listHost) != 2):
            raise unittest.SkipTest("Cancel host maintenance when VMs are migrating can only be tested with 2 hosts");
            return

        
        no_of_vms = self.noOfVMsOnHost(listHost[0].id)
        
        no_of_vms = no_of_vms + self.noOfVMsOnHost(listHost[1].id)
                
        if no_of_vms < 5:
            self.logger.debug("Create VMs as there are not enough vms to check host maintenance")
            no_vm_req = 5 - no_of_vms
            if (no_vm_req > 0):
                self.logger.debug("Creating vms = {}".format(no_vm_req))
                self.vmlist = self.createVMs(listHost[0].id, no_vm_req)
        
        vm_migrating=False
        
        try:
           
           vm_migrating = self.hostPrepareAndCancelMaintenance(listHost[0].id, listHost[1].id, self.checkVmMigratingOnHost)
           
           vm_migrating = self.hostPrepareAndCancelMaintenance(listHost[1].id, listHost[0].id, self.checkVmMigratingOnHost)
           
        except Exception as e:
            self.logger.debug("Exception {}".format(e))
            self.fail("Cancel host maintenance failed {}".format(e[0]))
        

        if (vm_migrating == False):
            raise unittest.SkipTest("No VM is migrating and the test will not be able to check the conditions the test is intended for");
                
            
        return


