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
""" BVT tests for Hosts Test
"""

# Import Local Modules
from marvin.codes import FAILED
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from nose.plugins.attrib import attr
from marvin.sshClient import SshClient
import time
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
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__        
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
    
    def checkHostDown(self, fromHostIp, testHostIp):
        try:
            ssh = SshClient(fromHostIp, 22, "root", "password") 
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
            ssh = SshClient(fromHostIp, 22, "root", "password") 
            res = ssh.execute("ping -c 1 %s" % testHostIp)
            result = str(res)
            if result.count(" 0% packet loss") == 1:
                return True, 1
            else:
                return False, 1
        except Exception as e:
            self.logger.debug("Got exception %s" % e)
            return False, 1
      
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

    def updateConfigurAndRestart(self, name, value):
        Configurations.update(self.apiclient, name, value)
        self.RestartServers()
        time.sleep(self.services["sleep"])

    def RestartServers(self):
        """ Restart management
        server and usage server """
        sshClient = SshClient(self.mgtSvrDetails["mgtSvrIp"],
            22,
            self.mgtSvrDetails["user"],
            self.mgtSvrDetails["passwd"]
        )
        command = "service cloudstack-management restart"
        sshClient.execute(command)
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
        Configurations.update(self.apiclient, "ping.timeout", "150")
        self.updateConfigurAndRestart("ping.interval", "150")
        
        listHost = Host.list(
            self.apiclient,
            type='Routing',
            zoneid=self.zone.id,
            podid=self.pod.id,
        )
        for host in listHost:
            self.logger.debug('Hypervisor = {}'.format(host.id))
            

        hostToTest = listHost[0]
        hostUpInCloudstack = wait_until(40, 10, self.checkHostStateInCloudstack, "Up", hostToTest.id)

        if not(hostUpInCloudstack): 
            raise self.fail("Host is not up %s, in cloudstack so failing test " % (hostToTest.ipaddress))
        return        
