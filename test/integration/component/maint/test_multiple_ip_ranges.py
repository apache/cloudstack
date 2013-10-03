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
#from netaddr import *
import netaddr

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
                        },
                        "server_without_disk": {
                                        "displayname": "Test VM-No Disk",
                                        "username": "root",
                                        "password": "password",
                                        "hypervisor": 'XenServer',
                        },
     			"cidr": {
                                  "name": "cidr1 -Test",
                                  "gateway" :"10.147.43.1",
                                  "netmask" :"255.255.255.128",
                                  "startip" :"10.147.43.3",
                                  "endip" :"10.147.43.10",
                        },
                        "ostype": "CentOS 5.3 (64-bit)",
                        "sleep": 60,
                        "timeout": 10,
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
        cls.disk_offering = DiskOffering.create(
                                    cls.api_client,
                                    cls.services["disk_offering"]
                                    )
        cls.service_offering = ServiceOffering.create(
                                            cls.api_client,
                                            cls.services["service_offering"]
                                            )
        cls.template = get_template(
                            cls.api_client,
                            cls.zone.id,
                            cls.services["ostype"]
                            )
        cls.services["templates"]["ostypeid"] = cls.template.ostypeid
        cls.services["diskoffering"] = cls.disk_offering.id
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

    def list_Routers(self):
        """Check if any VR is already present in the setup
           Will return True if yes else return False
        """
        list_zone = Zone.list(self.apiclient)
        network_type = list_zone[0].networktype
        sg_enabled = list_zone[0].securitygroupsenabled
        if network_type == "Basic":
            vr_list = Router.list(self.apiclient, listall='true')
            self.debug("vr list {}".format(vr_list))
            if isinstance(vr_list,list) and len(vr_list) > 0:
                self.debug("VR is running in the setup")
                return True
            else:
                self.debug("VR is not present in the setup")
                return False
        elif network_type == "Advanced" and sg_enabled == True:
            nw_list = Network.list(
                                   self.apiclient,
                                   supportedservices='SecurityGroup',
                                   )
            nw_id = nw_list[0].id
            vr_list = Router.list(
                                   self.apiclient,
                                   networkid = nw_id,
                                   listall = 'true',
                                 )
            if isinstance(vr_list, list) and len(vr_list) > 0:
                self.debug("VR is present in the setup")
                return True
            else :
                self.debug("VR is not present in the setup")
                return False
        else :
            self.debug("Network type is not shared")
            return None

    def test_01_deploy_vm_in_new_cidr(self):
        """Deploy guest vm after adding guest IP range in new CIDR

            1.Add IP range in new CIDR
            2.Deploy guest vm
        """
        dc_id = self.dbclient.execute(
                                      "select id from data_center where uuid = '%s';" % str(self.services["zoneid"])
                                      )
        dc_id = dc_id[0][0]
        id_list = self.dbclient.execute(
                            "select id from user_ip_address where allocated is null and data_center_id = '%s';" % str(dc_id)
                            )
        ip_list = []
        for i in range(len(id_list)):
            ip_list.append(id_list[i][0])
        #Check if VR is already present in the setup
        vr_state = self.list_Routers();
        if vr_state is True :
            for id in ip_list:
                self.dbclient.execute(
                        "update user_ip_address set allocated=now() where id = '%s';" % str(id)
                        )
        else :
            ip_list = ip_list[:-2]
            for id in ip_list:
                self.dbclient.execute(
                        "update user_ip_address set allocated=now() where id = '%s';" % str(id)
                        )
        #Add IP range in the new CIDR
        test_gateway = self.services["cidr"]["gateway"]
        test_startIp = self.services["cidr"]["startip"]
        test_endIp = self.services["cidr"]["endip"]
        test_netmask = self.services["cidr"]["netmask"]
        #Populating services with new IP range
        self.services["vlan_ip_range"]["startip"] = test_startIp
        self.services["vlan_ip_range"]["endip"] = test_endIp
        self.services["vlan_ip_range"]["gateway"] = test_gateway
        self.services["vlan_ip_range"]["netmask"] = test_netmask
        self.services["vlan_ip_range"]["zoneid"] = self.zone.id
        self.services["vlan_ip_range"]["podid"] = self.pod.id
        #create new vlan ip range
        new_vlan = PublicIpRange.create(self.apiclient, self.services["vlan_ip_range"])
        self.debug("Created new vlan range with startip:%s and endip:%s" %(test_startIp,test_endIp))
        self.cleanup.append(new_vlan)
        new_vlan_res = new_vlan.list(self.apiclient,id=new_vlan.vlan.id)
        #Compare list output with configured values
        self.verify_vlan_range(new_vlan_res,self.services["vlan_ip_range"])
        #Deploy vm in existing subnet if VR is not present
        if vr_state is False :
            vm_res = VirtualMachine.create(
                                            self.apiclient,
                                            self.services["server_without_disk"],
                                            templateid = self.template.id,
                                            accountid = self.account.name,
                                            domainid = self.services["domainid"],
                                            zoneid = self.services["zoneid"],
                                            serviceofferingid = self.service_offering.id,
                                            mode = self.services["mode"],
                                            )
            self.cleanup.append(vm_res)
        #Deploy guest vm
        try :
            self.virtual_machine = VirtualMachine.create(
                                            self.apiclient,
                                            self.services["server_without_disk"],
                                            templateid = self.template.id,
                                            accountid = self.account.name,
                                            domainid = self.services["domainid"],
                                            zoneid = self.services["zoneid"],
                                            serviceofferingid = self.service_offering.id,
                                            mode = self.services["mode"],
                                            )
        except Exception as e :
            raise Exception("Warning: Exception during vm deployment: {}".format(e))
        finally :
            #Mark ip_Adddresses allocated state to Null which were marked as allocated at the beginning of the test
            for id in ip_list :
                self.dbclient.execute(
                                      "update user_ip_address set allocated=default where id = '%s';" % str(id)
                                      )
        self.vm_response = VirtualMachine.list(
                                               self.apiclient,
                                               id = self.virtual_machine.id
                                               )
        self.assertEqual(
            isinstance(self.vm_response, list),
            True,
            "Check VM list response returned a valid list"
            )
        self.ip_range = list(netaddr.iter_iprange(unicode(self.services["cidr"]["startip"]), unicode(self.services["cidr"]["endip"])))
        self.nic_ip = netaddr.IPAddress(unicode(self.vm_response[0].nic[0].ipaddress))
        self.debug("vm got {} as ip address".format(self.nic_ip))
        self.assertIn(
              self.nic_ip,
              self.ip_range,
              "VM did not get the ip address from the new ip range"
              )
        self.virtual_machine.delete(self.apiclient)
        expunge_del = Configurations.list(
                                          self.apiclient,
                                          name = 'expunge.delay'
                                         )
        expunge_int = Configurations.list(
                                          self.apiclient,
                                          name = 'expunge.interval'
                                         )
        wait_time = int(expunge_del[0].value) + int(expunge_int[0].value) + int(30)

        self.debug("Waiting for {} seconds for the vm to expunge".format(wait_time))
        #wait for the vm to expunge
        time.sleep(wait_time)
        return

