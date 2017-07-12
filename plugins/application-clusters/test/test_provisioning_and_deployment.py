# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import marvin

from marvin.cloudstackTestCase import *
from marvin.sshClient import SshClient
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.utils import (random_gen)
from nose.plugins.attrib import attr
import cmd


class TestProvisioningAndDeployment(cloudstackTestCase):
    """
    Test
    """

    def setUp(self):
        testClient = super(TestProvisioningAndDeployment, self).getClsTestClient()
        self.apiclient = testClient.getApiClient()
        self.mgtSvrDetails = self.config.__dict__["mgtSvr"][0].__dict__        

        self.zone = get_zone(self.apiclient, testClient.getZoneForTests())
        
        self.service_offering = self.get_service_offering()
        
        self.cluster_endpoint = ""
        self.cluster_password = ""
        
        self.ids_to_clean = []
        #self.ids_to_clean.append("9e89d5ef-b31a-415e-b71d-2342cf69d308")

    
    def tearDown(self):
        if self.ids_to_clean is not None:
            for id in self.ids_to_clean:
                try:
                   self.delete_cc(id)
                except Exception:
                    pass
                else:
                    print("Could not managed to delete Container Cluster with ID: " + id)    
                    
    def delete_cc(self, id):
        cmd = self.getDeleteCCCmd(id)
        response = self.apiclient.deleteContainerCluster(cmd)
        self.assertEqual(response.success, True)
        return
      
    def getDeleteCCCmd(self, id):
        cmd = deleteContainerCluster.deleteContainerClusterCmd()
        cmd.id = id
        
        return cmd
        
    def get_service_offering(self):
        response = list_service_offering(self.apiclient)
        
        if len(response) > 0:
            self.service_offering = response[0]
            return self.service_offering
        else:
            raise self.skipTest("No service offering found, skipping test")
    
    
    def getCreateContainerClusterCmd(self):
        cmd = createContainerCluster.createContainerClusterCmd()
        
        cmd.name = "TestCluster"
        cmd.serviceofferingid = self.service_offering.id
        cmd.zoneid = self.zone.id
        cmd.size = 1
                
        return cmd
    
    def stopCluster(self):
        cmd = stopContainerCluster.stopContainerClusterCmd()
        cmd.id = self.ids_to_clean[0]
        print(cmd.id)
        
        response = ContainerCluster.stop(self.apiclient, cmd)
        return response.success
            
    def startCluster(self):
        cmd = startContainerCluster.startContainerClusterCmd()
        cmd.id = self.ids_to_clean[0]
        print(cmd.id)

        response = ContainerCluster.start(self.apiclient, cmd)
        return response.success
    
    def createContainerCluster(self):
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()

        #Execute the API Command
        response = ContainerCluster.create(
                                           self.apiclient,
                                           cmd.name, 
                                           cmd.zoneid, 
                                           cmd.serviceofferingid, 
                                           cmd.size)
        
        #Check if job is successful
        self.ids_to_clean.append(response.id)
        
        self.assertEqual(response.state, "Running")
        
        self.cluster_endpoint = response.endpoint
        self.cluster_password = response.password
        
        return response.success
    
    
    def deployRedis(self):
        sshClient = SshClient(host=self.mgtSvrDetails["mgtSvrIp"], port=22, user=self.mgtSvrDetails["user"], passwd=self.mgtSvrDetails["passwd"]) 
        
        sshClient.execute("mkdir redis-example ")
        sshClient.execute("wget https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/redis/redis-controller.yaml " +
                                                                "https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/redis/redis-master.yaml " +
                                                                "https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/redis/redis-proxy.yaml " +
                                                                "https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/redis/redis-sentinel-controller.yaml " +
                                                                "https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/redis/redis-sentinel-service.yaml " +
                                                                "-P /root/redis-example/")
        sshClient.execute("sed -i -e 's/redis:v2/redis:v1/g' /root/redis-example/*.yaml")
        sshClient.execute("mkdir redis-example/image")
        sshClient.execute("wget https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/redis/image/Dockerfile " +
                                                        "https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/redis/image/redis-master.conf " +
                                                        "https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/redis/image/redis-slave.conf " + 
                                                        "https://raw.githubusercontent.com/kubernetes/kubernetes/master/examples/redis/image/run.sh -P /root/redis-example/image/")
        
        sshClient.execute("kubectl create -f /root/redis-example/redis-master.yaml -s " + self.cluster_endpoint + " --insecure-skip-tls-verify=true --username=admin --password=" + self.cluster_password)
        sshClient.execute("kubectl create -f /root/redis-example/redis-sentinel-service.yaml -s " + self.cluster_endpoint + " --insecure-skip-tls-verify=true --username=admin --password=" + self.cluster_password)
        sshClient.execute("kubectl create -f /root/redis-example/redis-controller.yaml -s " + self.cluster_endpoint + " --insecure-skip-tls-verify=true --username=admin --password=" + self.cluster_password)
        sshClient.execute("kubectl create -f /root/redis-example/redis-sentinel-controller.yaml -s " + self.cluster_endpoint + " --insecure-skip-tls-verify=true --username=admin --password=" + self.cluster_password)

        res = sshClient.execute("kubectl scale rc redis --replicas=3 -s " + self.cluster_endpoint + " --insecure-skip-tls-verify=true --username=admin --password=" + self.cluster_password)
        self.assertEqual(res[0], "replicationcontroller \"redis\" scaled")

        res = sshClient.execute("kubectl scale rc redis-sentinel --replicas=3 -s " + self.cluster_endpoint + " --insecure-skip-tls-verify=true --username=admin --password=" + self.cluster_password)
        self.assertEqual(res[0], "replicationcontroller \"redis-sentinel\" scaled")

        res = sshClient.execute("kubectl delete pods redis-master -s " + self.cluster_endpoint + " --insecure-skip-tls-verify=true --username=admin --password=" + self.cluster_password )
        self.assertEqual(res[0], "pod \"redis-master\" deleted")

        sshClient.execute("rm -rf /root/redis-example/")
       
    
    
    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")   
    def test_create_container_cluster_valid(self):
        """
            This is a valid scenario of containing a container cluster
        """
        
        self.deployRedis()
       
        
   
    @attr(tags=["advanced", "advancedns","smoke","basic", "sg", "custom"], required_hardware="false")   
    def test_stop_start_cc(self):
        """
            Test scenario for stopping and starting a running cluster
        """
        self.createContainerCluster()
        
        self.assertEqual(True, self.stopCluster())
        
        self.assertEqual(True, self.startCluster())
        
        