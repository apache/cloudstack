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
                                           "startip": "10.147.43.130",
                                           "endip": "10.147.43.135",
                                           "netmask": "255.255.255.192",
                                           "gateway": "10.147.43.129",
                                           "forvirtualnetwork": "false",
                                           "vlan": "untagged",
                        },
                        "server_without_disk": {
                                        "displayname": "Test VM-No Disk",
                                        "username": "root",
                                        "password": "password",
                                        "hypervisor": 'XenServer',
                        },
                        "host": {
				"publicport": 22,
	                        "username": "root",    # Host creds for SSH
				"password": "password",
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
        cls.dbclient = super(TestMultipleIpRanges, cls).getClsTestClient().getDbConnection()
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
        cls.dc_id = cls.dbclient.execute(
                                      "select id from data_center where uuid = '%s';" % str(cls.services["zoneid"])
                                      )
        cls.dc_id = cls.dc_id[0][0]
        cls.ids = cls.dbclient.execute(
                            "select id from user_ip_address where allocated is null and data_center_id = '%s';" % str(cls.dc_id)
                            )
        cls.id_list = []
        for i in range(len(cls.ids)):
            cls.id_list.append(cls.ids[i][0])
        #Check if VR is already present in the setup
        vr_list = Router.list(cls.api_client, listall='true')
        cls.debug("vr list {}".format(vr_list))
        if isinstance(vr_list, list) and len(vr_list) > 0:
            cls.debug("VR is running in the setup")
            cls.vr_state = True
        else:
            cls.debug("VR is not present in the setup")
            cls.vr_state = False
            cls.id_list = cls.id_list[:-2]
        for id in cls.id_list:
            cls.dbclient.execute(
                                 "update user_ip_address set allocated=now() where id = '%s';" % str(id)
                                 )
        #Add IP range in the new CIDR
        cls.services["vlan_ip_range"]["zoneid"] = cls.zone.id
        cls.services["vlan_ip_range"]["podid"] = cls.pod.id
        #create new vlan ip range
        cls.new_vlan = PublicIpRange.create(cls.api_client, cls.services["vlan_ip_range"])
        #Deploy vm in existing subnet if VR is not present
        if cls.vr_state is False :
            cls.vm_res = VirtualMachine.create(
                                            cls.api_client,
                                            cls.services["server_without_disk"],
                                            templateid = cls.template.id,
                                            accountid = cls.account.name,
                                            domainid = cls.services["domainid"],
                                            zoneid = cls.services["zoneid"],
                                            serviceofferingid = cls.service_offering.id,
                                            mode = cls.services["mode"],
                                            )
        cls._cleanup = [
                        cls.new_vlan,
                        cls.account,
                        ]
        return

    @classmethod
    def tearDownClass(cls):
        try:
            for id in cls.id_list:
                cls.dbclient.execute(
                                     "update user_ip_address set allocated=default where id = '%s';" % str(id)
                                     )
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

    @attr(tags=["sg"])
    def test_01_deploy_vm_in_new_cidr(self):
        """Deploy guest vm after adding guest IP range in new CIDR
            1.Deploy guest vm
            2.Verify vm gets the ip address from new cidr
        """
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
        self.vm_response = VirtualMachine.list(
                                               self.apiclient,
                                               id = self.virtual_machine.id
                                               )
        self.assertEqual(
            isinstance(self.vm_response, list),
            True,
            "Check VM list response returned a valid list"
            )
        self.ip_range = list(netaddr.iter_iprange(unicode(self.services["vlan_ip_range"]["startip"]), unicode(self.services["vlan_ip_range"]["endip"])))
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

    @attr(tags=["sg"])
    def test_02_deploy_vm_in_new_cidr(self):
        """Deploy guest vm in new CIDR and verify
            1.Deploy guest vm in new cidr
            2.Verify dns service listens on alias ip in VR
        """
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
        self.vm_response = VirtualMachine.list(
                                               self.apiclient,
                                               id = self.virtual_machine.id
                                               )
        self.assertEqual(
            isinstance(self.vm_response, list),
            True,
            "Check VM list response returned a valid list"
            )
        self.ip_range = list(netaddr.iter_iprange(unicode(self.services["vlan_ip_range"]["startip"]), unicode(self.services["vlan_ip_range"]["endip"])))
        self.nic_ip = netaddr.IPAddress(unicode(self.vm_response[0].nic[0].ipaddress))
        self.debug("vm got {} as ip address".format(self.nic_ip))
        self.assertIn(
              self.nic_ip,
              self.ip_range,
              "VM did not get the ip address from the new ip range"
              )
        ip_alias = self.dbclient.execute(
                              "select ip4_address from nic_ip_alias;"
                              )
        alias_ip = str(ip_alias[0][0])
        self.debug("alias ip : %s" % alias_ip)
        list_router_response = list_routers(
                                    self.apiclient,
                                    zoneid=self.zone.id,
                                    listall=True
                                    )
        self.assertEqual(
                            isinstance(list_router_response, list),
                            True,
                            "Check list response returns a valid list"
                        )
        router = list_router_response[0]
        hosts = list_hosts(
                           self.apiclient,
                           zoneid=router.zoneid,
                           type='Routing',
                           state='Up',
                           id=router.hostid
                           )
        self.assertEqual(
                            isinstance(hosts, list),
                            True,
                            "Check list host returns a valid list"
                        )
        host = hosts[0]
        self.debug("Router ID: %s, state: %s" % (router.id, router.state))
        self.assertEqual(
                            router.state,
                            'Running',
                            "Check list router response for router state"
                        )
        proc = alias_ip+":53"
        result = get_process_status(
                                host.ipaddress,
                                self.services['host']["publicport"],
                                self.services['host']["username"],
                                self.services['host']["password"],
                                router.linklocalip,
                                "netstat -atnp | grep %s" % proc
                                )
        res = str(result)
        self.debug("Dns process status on alias ip: %s" % res)
        self.assertNotEqual(
                         res.find(proc)
                         -1,
                         "dnsmasq service is not running on alias ip"
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

