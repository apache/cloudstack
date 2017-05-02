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
from marvin.cloudstackAPI import *
from marvin.lib.utils import *
from marvin.lib.base import *
from marvin.lib.common import *
from marvin.lib.utils import (random_gen)
from nose.plugins.attrib import attr
import cmd

class TestContainerClusterProvisioning(cloudstackTestCase):
    """
        Tests for container cluster provisioning 
    """
    
    def setUp(self):
        testClient = super(TestContainerClusterProvisioning, self).getClsTestClient()

        self.apiclient = testClient.getApiClient()
        self.dbclient = testClient.getDbConnection()

        self.zone = get_zone(self.apiclient, testClient.getZoneForTests())
        
        self.service_offering = self.get_service_offering()
        
        self.ids_to_clean = []

    
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
        cmd.name = "TestCluester"
        cmd.serviceofferingid = self.service_offering.id
        cmd.zoneid = self.zone.id
        cmd.size = 1
                
        return cmd
    

    def getListContainerClusterCmd(self):
        cmd = listContainerCluster.listContainerClusterCmd()
        
        return cmd

    
    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")   
    def test_create_container_cluster_valid(self):
        """
            This is a valid scenario of containing a container cluster
        """
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
        self.ids_to_clean.append("afb8541b-c461-4afe-b9d9-a64e2444f73c")
        self.ids_to_clean.append(response.id)
        
        self.assertEqual(response.state, "Running")
        
        

    
    
    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")   
    def test_create_cc_with_name_emptyString(self):
        """
            This tests calls the CC API with missing name parameter
            Should result in error
        """
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()
        cmd.name = ''
        
        #Execute the API Command
        try:
            response = self.apiclient.createContainerCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")


    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")   
    def test_create_cc_without_name(self):
        """
            This tests calls the CC API with missing name parameter
            Should result in error
        """
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()
        cmd.name = None
        
        #Execute the API Command
        try:
            response = self.apiclient.createContainerCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")
        
    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")       
    def test_create_cc_without_zoneid(self):
        """
            This tests calls the CC API with missing name parameter
            Should result in error
        """
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()
        cmd.zoneid = None
        
        #Execute the API Command
        try:
            response = self.apiclient.createContainerCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")
                
    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")        
    def test_create_cc_without_serviceoffering(self):
        """
            This tests calls the CC API with missing name parameter
            Should result in error
        """
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()
        cmd.serviceofferingid = None
        
        #Execute the API Command
        try:
            response = self.apiclient.createContainerCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")
     
     
    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")        
    def test_create_cc_without_clustersize(self):
        """
            This tests calls the CC API with missing name parameter
            Should result in error
        """
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()
        cmd.size = None
        
        #Execute the API Command
        try:
            response = self.apiclient.createContainerCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")
        
   
    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")        
    def test_create_cc_invalid_clustersize(self):
        """
            This tests calls the CC API with missing name parameter
            Should result in error
        """
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()
        cmd.size = "Invalid-cluster-size"
        
        #Execute the API Command
        try:
            response = self.apiclient.createContainerCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")
            
            
    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")        
    def test_create_cc_invalid_zoneid(self):
        """
            This tests calls the CC API with missing name parameter
            Should result in error
        """
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()
        cmd.zoneid = "Some-invalid-zoneid"
        
        #Execute the API Command
        try:
            response = self.apiclient.createContainerCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")


    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")        
    def test_create_cc_invalid_serviceofferingid(self):
        """
            This tests calls the CC API with missing name parameter
            Should result in error
        """
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()
        cmd.serviceofferingid = "Some-invalid-service-offering_id"
        
        #Execute the API Command
        try:
            response = self.apiclient.createContainerCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")


    @attr(tags=["advanced", "advancedns","smoke","basic", "sg"], required_hardware="false")       
    def test_create_cc_with_size_0(self):
        """
            This tests calls the CC API with missing name parameter
            Should result in error
        """
        #Create API Command
        cmd = self.getCreateContainerClusterCmd()
        cmd.size = 0
        
        #Execute the API Command
        try:
            response = self.apiclient.createContainerCluster(cmd)
        except Exception:
            pass
        else:
            self.fail("Expected an exception to be thrown, failing")
                
