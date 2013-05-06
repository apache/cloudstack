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
""" Tests for Multiple IP Ranges feature
"""
from marvin.cloudstackTestCase import *
from marvin.cloudstackAPI import *
from marvin.cloudstackException import cloudstackAPIException
from marvin.integration.lib.utils import *
from marvin.integration.lib.base import *
from marvin.integration.lib.common import *
from netaddr import *

from nose.plugins.attrib import attr

class Services:
    """Test Multiple IP Ranges
    """
    def __init__(self):
        self.services = {
                        "account": {
                                    "email": "test@test.com",
                                    "firstname": "Test",
                                    "lastname": "User",
                                    "username": "test",
                                    # Random characters are appended for unique
                                    # username
                                    "password": "password",
                        },
                        "service_offering": {
                                    "name": "Tiny Instance",
                                    "displaytext": "Tiny Instance",
                                    "cpunumber": 1,
                                    "cpuspeed": 200,    # in MHz
                                    "memory": 256,      # In MBs
                        },
                        "disk_offering": {
                                    "displaytext": "Small Disk",
                                    "name": "Small Disk",
                                    "disksize": 1
                        },
                        "templates": {
                                    "displaytext": 'Template',
                                    "name": 'Template',
                                    "ostype": "CentOS 5.3 (64-bit)",
                                    "templatefilter": 'self',
                        },
                         "vlan_ip_range": {
                                           "startip": "",
                                           "endip": "",
                                           "netmask": "",
                                           "gateway": "",
                                           "forvirtualnetwork": "false",
                                           "vlan": "untagged",
                                           }
          }

class TestMultipleIpRanges(cloudstackTestCase):
    """Test Multiple IP Ranges for guest network
    """


    @classmethod
    def setUpClass(cls):
        cls.api_client = super(TestMultipleIpRanges, cls).getClsTestClient().getApiClient()
        cls.services = Services().services
        # Get Zone, Domain and templates
        cls.domain = get_domain(cls.api_client, cls.services)
        cls.zone = get_zone(cls.api_client, cls.services)
        cls.pod = get_pod(cls.api_client, cls.zone.id, cls.services)
        cls.services['mode'] = cls.zone.networktype
        cls.services["domainid"] = cls.domain.id
        cls.services["zoneid"] = cls.zone.id
        cls.account = Account.create(
                            cls.api_client,
                            cls.services["account"],
                            domainid=cls.domain.id
                            )
        cls.services["account"] = cls.account.name
        cls._cleanup = [
                        cls.account,
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
        self.cleanup = [ ]
        return

    def tearDown(self):
        try:
            #Clean up, terminate the resources created
            cleanup_resources(self.apiclient, self.cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def increment_cidr(self):
        """Takes CIDR as input and will increment by one and returns the new CIDR
        """
        publicIpRange = PublicIpRange.list(self.apiclient)
        self.startIp = publicIpRange[0].startip
        self.endIp = publicIpRange[0].endip
        self.gateway = publicIpRange[0].gateway
        self.netmask = publicIpRange[0].netmask
        #Pass ip address and mask length to IPNetwork to findout the CIDR
        ip = IPNetwork(self.startIp+"/"+self.netmask)
        new_cidr = ip.__iadd__(1)
        ip2 = IPNetwork(new_cidr)
        return ip2

    def verify_vlan_range(self,vlan,services):
        #compare vlan_list response with configured values
        self.assertEqual(
                         isinstance(vlan, list),
                         True,
                         "Check list response returned a valid list"
                         )
        self.assertNotEqual(
                             len(vlan),
                             0,
                             "check list vlan response"
                             )
        self.assertEqual(
                         str(vlan[0].startip),
                         str(services["startip"]),
                         "Start IP in vlan ip range is not matched with the configured start ip"
                         )
        self.assertEqual(
                         str(vlan[0].endip),
                         str(services["endip"]),
                         "End IP in vlan ip range is not matched with the configured end ip"
                        )
        self.assertEqual(
                         str(vlan[0].gateway),
                         str(services["gateway"]),
                         "gateway in vlan ip range is not matched with the configured gateway"
                         )
        self.assertEqual(
                         str(vlan[0].netmask),
                         str(services["netmask"]),
                         "netmask in vlan ip range is not matched with the configured netmask"
                         )
        return

    @attr(tags=["advanced_sg", "sg"])
    def test_01_add_ip_same_cidr(self):
        """Test add guest ip range in the existing cidr
        """
        #call increment_cidr function to get exiting cidr from the setup and increment it
        ip2 = self.increment_cidr()
        test_nw = ip2.network
        ip = IPAddress(test_nw)
        #Add IP range(5 IPs) in the new CIDR
        test_gateway = ip.__add__(1)
        test_startIp = ip.__add__(3)
        test_endIp = ip.__add__(10)
        test_startIp2= ip.__add__(11)
        test_endIp2 = ip.__add__(15)
        #Populating services with new IP range
        self.services["vlan_ip_range"]["startip"] = test_startIp
        self.services["vlan_ip_range"]["endip"] = test_endIp
        self.services["vlan_ip_range"]["gateway"] = test_gateway
        self.services["vlan_ip_range"]["netmask"] = self.netmask
        self.services["vlan_ip_range"]["zoneid"] = self.zone.id
        self.services["vlan_ip_range"]["podid"] = self.pod.id
        #create new vlan ip range
        new_vlan = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"])
        self.debug("Created new vlan range with startip:%s and endip:%s" %(test_startIp,test_endIp))
        self.cleanup.append(new_vlan)
        new_vlan_res = new_vlan.list(self.apiclient,id=new_vlan.vlan.id)
        #Compare list output with configured values
        self.verify_vlan_range(new_vlan_res,self.services["vlan_ip_range"])
        #Add few more ips in the same CIDR
        self.services["vlan_ip_range"]["startip"] = test_startIp2
        self.services["vlan_ip_range"]["endip"] = test_endIp2
        new_vlan2 = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"])
        self.debug("Created new vlan range with startip:%s and endip:%s" %(test_startIp2,test_endIp2))
        self.cleanup.append(new_vlan2)
        #list new vlan ip range
        new_vlan2_res = new_vlan2.list(self.apiclient,id=new_vlan2.vlan.id)
        #Compare list output with configured values
        self.verify_vlan_range(new_vlan2_res,self.services["vlan_ip_range"])
        return

    @attr(tags=["advanced_sg", "sg"])
    def test_02_add_ip_diff_cidr(self):
        """Test add ip range in a new cidr

           Steps:
           1.Get public vlan range (guest cidr) from the setup
           2.Add IP range to a new cidr
        """
        #call increment_cidr function to get exiting cidr from the setup and increment it
        ip2 = self.increment_cidr()
        test_nw = ip2.network
        ip = IPAddress(test_nw)
        #Add IP range(5 IPs) in the new CIDR
        test_gateway = ip.__add__(1)
        test_startIp = ip.__add__(3)
        test_endIp = ip.__add__(10)
        #Populating services with new IP range
        self.services["vlan_ip_range"]["startip"] = test_startIp
        self.services["vlan_ip_range"]["endip"] = test_endIp
        self.services["vlan_ip_range"]["gateway"] = test_gateway
        self.services["vlan_ip_range"]["netmask"] = self.netmask
        self.services["vlan_ip_range"]["zoneid"] = self.zone.id
        self.services["vlan_ip_range"]["podid"] = self.pod.id
        #create new vlan ip range
        new_vlan = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"])
        self.debug("Created new vlan range with startip:%s and endip:%s" %(test_startIp,test_endIp))
        self.cleanup.append(new_vlan)
        new_vlan_res = new_vlan.list(self.apiclient,id=new_vlan.vlan.id)
        #Compare list output with configured values
        self.verify_vlan_range(new_vlan_res,self.services["vlan_ip_range"])
        return

    @attr(tags=["advanced-sg", "sg"])
    def test_03_del_ip_range(self):
        """Test delete ip range

           Steps:
           1.Add ip range in same/new cidr
           2.delete the ip range added at step1
           3.Verify the ip range deletion using list APIs
        """
        #call increment_cidr function to get exiting cidr from the setup and increment it
        ip2 = self.increment_cidr()
        test_nw = ip2.network
        ip = IPAddress(test_nw)
        #Add IP range(5 IPs) in the new CIDR
        test_gateway = ip.__add__(1)
        test_startIp = ip.__add__(3)
        test_endIp = ip.__add__(10)
        #Populating services with new IP range
        self.services["vlan_ip_range"]["startip"] = test_startIp
        self.services["vlan_ip_range"]["endip"] = test_endIp
        self.services["vlan_ip_range"]["gateway"] = test_gateway
        self.services["vlan_ip_range"]["netmask"] = self.netmask
        self.services["vlan_ip_range"]["zoneid"] = self.zone.id
        self.services["vlan_ip_range"]["podid"] = self.pod.id
        #create new vlan ip range
        new_vlan = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"])
        self.debug("Created new vlan range with startip:%s and endip:%s" %(test_startIp,test_endIp))
        new_vlan_res = new_vlan.list(self.apiclient,id=new_vlan.vlan.id)
        #Compare list output with configured values
        self.verify_vlan_range(new_vlan_res,self.services["vlan_ip_range"])
        #Delete the above IP range
        new_vlan.delete(self.apiclient)
        #listing vlan ip ranges with the id should through exception , if not mark the test case as failed
        try:
            new_vlan.list(self.apiclient, id=new_vlan.vlan.id)
        except cloudstackAPIException as cs:
            self.debug(cs.errorMsg)
            self.assertTrue(cs.errorMsg.find("entity does not exist")>0, msg="Failed to delete IP range")
        return

    @attr(tags=["advanced-sg", "sg"])
    def test_04_add_noncontiguous_ip_range(self):
        """Test adding non-contiguous ip range in existing cidr

            1.Add ip range in new cidr
            1.Add non-contigous ip range in cidr added at step1
            2.Verify the ip range using list APIs
        """
        #call increment_cidr function to get exiting cidr from the setup and increment it
        ip2 = self.increment_cidr()
        test_nw = ip2.network
        ip = IPAddress(test_nw)
        #Add IP range(5 IPs) in the new CIDR
        test_gateway = ip.__add__(1)
        test_startIp = ip.__add__(50)
        test_endIp = ip.__add__(60)
        #Populating services with new IP range
        self.services["vlan_ip_range"]["startip"] = test_startIp
        self.services["vlan_ip_range"]["endip"] = test_endIp
        self.services["vlan_ip_range"]["gateway"] = test_gateway
        self.services["vlan_ip_range"]["netmask"] = self.netmask
        self.services["vlan_ip_range"]["zoneid"] = self.zone.id
        self.services["vlan_ip_range"]["podid"] = self.pod.id
        #create new vlan ip range
        new_vlan = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"])
        self.debug("Created new vlan range with startip:%s and endip:%s" %(test_startIp,test_endIp))
        self.cleanup.append(new_vlan)
        new_vlan_res = new_vlan.list(self.apiclient,id=new_vlan.vlan.id)
        #Compare list output with configured values
        self.verify_vlan_range(new_vlan_res,self.services["vlan_ip_range"])
        #Add non-contiguous ip range in exiting cidr
        test_startIp2 = ip.__add__(10)
        test_endIp2 = ip.__add__(20)
        #Populating services with new IP range
        self.services["vlan_ip_range"]["startip"] = test_startIp2
        self.services["vlan_ip_range"]["endip"] = test_endIp2
        #create new vlan ip range
        new_vlan = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"])
        self.debug("Created new vlan range with startip:%s and endip:%s" %(test_startIp,test_endIp))
        self.cleanup.append(new_vlan)
        new_vlan_res = new_vlan.list(self.apiclient,id=new_vlan.vlan.id)
        #Compare list output with configured values
        self.verify_vlan_range(new_vlan_res,self.services["vlan_ip_range"])
        return

    @attr(tags=["advanced-sg", "sg"])
    def test_05_add_overlapped_ip_range(self):
        """Test adding overlapped ip range in existing cidr

            1.Add ip range in new cidr e.g:10.147.40.10-10.147.40.100
            2.Add ip range overlapped with the ip range in step1 e.g.10.147.40.90-150
        """
        #call increment_cidr function to get exiting cidr from the setup and increment it
        ip2 = self.increment_cidr()
        test_nw = ip2.network
        ip = IPAddress(test_nw)
        #Add IP range in the new CIDR
        test_gateway = ip.__add__(1)
        test_startIp = ip.__add__(10)
        test_endIp = ip.__add__(100)
        test_startIp2 = ip.__add__(90)
        test_endIp2 = ip.__add__(150)
        #Populating services with new IP range
        self.services["vlan_ip_range"]["startip"] = test_startIp
        self.services["vlan_ip_range"]["endip"] = test_endIp
        self.services["vlan_ip_range"]["gateway"] = test_gateway
        self.services["vlan_ip_range"]["netmask"] = self.netmask
        self.services["vlan_ip_range"]["zoneid"] = self.zone.id
        self.services["vlan_ip_range"]["podid"] = self.pod.id
        #create new vlan ip range
        new_vlan = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"])
        self.debug("Created new vlan range with startip:%s and endip:%s" %(test_startIp,test_endIp))
        self.cleanup.append(new_vlan)
        new_vlan_res = new_vlan.list(self.apiclient,id=new_vlan.vlan.id)
        #Compare list output with configured values
        self.verify_vlan_range(new_vlan_res,self.services["vlan_ip_range"])
        #Add overlapped ip range
        #Populating services with new IP range
        self.services["vlan_ip_range"]["startip"] = test_startIp2
        self.services["vlan_ip_range"]["endip"] = test_endIp2
        #Try to create ip range overlapped with exiting ip range
        try:
            PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"])
        except cloudstackAPIException as cs:
            self.debug(cs.errorMsg)
            self.assertTrue(cs.errorMsg.find("already has IPs that overlap with the new range")>0, msg="Fail:CS allowed adding overlapped ip ranges in guest cidr")
            return
        #Test will reach here there is a bug in overlap ip range checking
        self.fail("CS should not accept overlapped ip ranges in guest traffic, but it allowed")
        return
