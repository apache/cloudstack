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

import logging
import time
import requests

from nose.plugins.attrib import attr

from marvin.cloudstackTestCase import cloudstackTestCase
from marvin.lib.base import (Network, NetworkOffering, Account, VirtualMachine, ServiceOffering,
                             NATRule, PublicIPAddress, EgressFireWallRule, Host,
                             FireWallRule, LoadBalancerRule, LogicalRouter, ApplicationPolicySet,
                             FirewallPolicy, FirewallRule, TungstenTag, ServiceGroup,
                             NetworkPolicy, PolicyRule)
from marvin.cloudstackAPI import migrateVirtualMachine
from marvin.lib.utils import (is_server_ssh_ready, get_host_credentials, execute_command_in_host)
from marvin.lib.common import get_zone, get_template
from marvin.lib.decoratorGenerators import skipTestIf
from .common import is_object_created, is_object_deleted, get_list_system_vm, cleanup_resources, not_tungsten_fabric_zone

class Services:
    """Test Tungsten Plugin
    """

    def __init__(self):
        self.services = {
            "network_offering": {
                "name": "TungstenNetworkOffering",
                "displaytext": "TungstenNetworkOffering",
                "guestiptype": "Isolated",
                "traffictype": "GUEST",
                "supportedservices": [
                    "Dhcp",
                    "Dns",
                    "Lb",
                    "UserData",
                    "SourceNat",
                    "StaticNat",
                    "PortForwarding",
                    "Connectivity",
                    "Firewall"
                ],
                "serviceProviderList": {
                    "Dhcp": "Tungsten",
                    "Dns": "Tungsten",
                    "Lb": "Tungsten",
                    "UserData": "ConfigDrive",
                    "SourceNat": "Tungsten",
                    "StaticNat": "Tungsten",
                    "PortForwarding": "Tungsten",
                    "Connectivity": "Tungsten",
                    "Firewall": "Tungsten"
                },
                "useTungsten": "on"
            },
            "networks": {
                "network1": {
                    "name": "TF-Network1",
                    "displaytext": "TF-Network1",
                    "gateway": "10.1.1.1",
                    "netmask": "255.255.255.0",
                    "startip": "10.1.1.100",
                    "endip": "10.1.1.200"
                },
                "network2": {
                    "name": "TF-Network2",
                    "displaytext": "TF-Network2",
                    "gateway": "10.1.2.1",
                    "netmask": "255.255.255.0",
                    "startip": "10.1.2.100",
                    "endip": "10.1.2.200"
                }
            },
            "ostype": "112",
            "account": {
                "email": "test@test.com",
                "firstname": "Test",
                "lastname": "User",
                "username": "test",
                "password": "password",
            },
            "virtual_machines": {
                "vm1": {
                    "name": "vm1",
                    "displayname": "vm1",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'KVM',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP'

                },
                "vm2": {
                    "name": "vm2",
                    "displayname": "vm2",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'KVM',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP'
                },
                "vm3": {
                    "name": "vm3",
                    "displayname": "vm3",
                    "username": "root",
                    "password": "password",
                    "ssh_port": 22,
                    "hypervisor": 'KVM',
                    "privateport": 22,
                    "publicport": 22,
                    "protocol": 'TCP'
                }
            },
            "fw_rule": {
                "startport": 80,
                "endport": 80,
                "cidr": '0.0.0.0/0',
                "protocol": "TCP"
            },
            "nat_rule": {
                "privateport": 80,
                "publicport": 80,
                "protocol": 'TCP'
            },
            "lbrule": {
                "name": "SSH",
                "alg": "roundrobin",
                "privateport": 80,
                "publicport": 80,
                "protocol": 'TCP'
            },
            "logicalrouter": {
                "name": "TFLogicalRouter"
            },
            "applicationpolicysets": {
                "aps1": {
                    "name": "aps1"
                }
            },
            "firewallpolicys": {
                "frontend": {
                    "name": "frontend"
                },
                "internet": {
                    "name": "internet"
                },
                "host": {
                    "name": "host"
                }
            },
            "firewallrules": {
                "frontend": {
                    "name": "frontend",
                    "action": "pass",
                    "direction": "oneway"
                },
                "internet": {
                    "name": "internet",
                    "action": "pass",
                    "direction": "oneway"
                },
                "host": {
                    "name": "host",
                    "action": "pass",
                    "direction": "oneway"
                }
            },
            "servicegroups": {
                "any": {
                    "name": "any",
                    "protocol": "any",
                    "startport": -1,
                    "endport": -1
                }
            },
            "network_policy": {
                "policy1": {
                    "name": "policy1",
                    "rules": {
                        "rule1": {
                            "action": "pass",
                            "direction": "twoway",
                            "protocol": "icmp",
                            "srcnetwork": "any",
                            "srcipprefix": "0.0.0.0",
                            "srcipprefixlend": 0,
                            "srcstartport": -1,
                            "srcendport": -1,
                            "destnetwork": "any",
                            "destipprefix": "0.0.0.0",
                            "destipprefixlen": 0,
                            "deststartport": -1,
                            "destendport": -1
                        }
                    }
                }
            },
            "network_route_table": {
                "route_table_1": {
                    "name": "Network Route Table 1",
                    "prefix": "10.1.2.0/24",
                    "nexthoptype": "ip-address",
                    "communities": ""
                },
                "route_table_2": {
                    "name": "Network Route Table 2",
                    "prefix": "10.1.1.0/24",
                    "nexthoptype": "ip-address",
                    "communities": ""
                }
            },
            "interface_route_table": {
                "route_table_1": {
                    "name": "Interface Route Table 1",
                    "prefix": "10.1.2.0/24",
                    "communities": ""
                },
                "route_table_2": {
                    "name": "Interface Route Table 2",
                    "prefix": "10.1.1.0/24",
                    "communities": ""
                }
            },
            "routing_policy": {
                "routing_policy_1": {
                    "name": "Routing Policy 1",
                    "matchall": "true",
                    "protocollist": [""],
                    "prefixlist": [
                        "10.1.2.0/24&exact"
                    ],
                    "termlist": [
                        "reject&action& "
                    ],
                    "communities": [""]
                }
            }
        }


class TestIsolatedZone(cloudstackTestCase):
    @classmethod
    def setUpClass(cls):
        cls.apiclient = cls.clstestclient.getApiClient()
        cls.dbclient = cls.clstestclient.getDbConnection()
        cls.services = Services().services
        cls.testdata = cls.clstestclient.getParsedTestDataConfig()
        cls.zone = get_zone(cls.apiclient, cls.clstestclient.getZoneForTests())
        cls.hypervisor = cls.clstestclient.getHypervisorInfo()
        cls.tfgw = cls.getClsConfig()["zones"][0]["tungstenprovider"]["gateway"]
        cls.config = cls.getClsConfig()
        cls.template = get_template(cls.apiclient, cls.zone.id)
        cls.hosts = Host.list(cls.apiclient, zoneid=cls.zone.id, type="routing")
        for host in cls.hosts:
            Host.reconnect(cls.apiclient, id=host.id)
        cls.not_tungsten_fabric_zone = not_tungsten_fabric_zone(cls.apiclient, cls.zone.id)
        cls.is_single_host = len(cls.hosts) < 2
        cls.can_not_migrate = cls.not_tungsten_fabric_zone or cls.is_single_host
        cls._cleanup = []

        cls.logger = logging.getLogger("TestIsolatedZone")
        cls.stream_handler = logging.StreamHandler()
        cls.logger.setLevel(logging.DEBUG)
        cls.logger.addHandler(cls.stream_handler)

    @classmethod
    def tearDownClass(self):
        try:
            cleanup_resources(self.apiclient, self.zone.id, self._cleanup)
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    def setUp(self):
        self.account = Account.create(self.apiclient, self.services["account"], admin=True)
        self.service_offering = ServiceOffering.create(self.apiclient, self.testdata["service_offering"])
        self.cleanup = [self.account, self.service_offering]
        return

    def tearDown(self):
        try:
            cleanup_resources(self.apiclient, self.zone.id, reversed(self.cleanup))
        except Exception as e:
            raise Exception("Warning: Exception during cleanup : %s" % e)
        return

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_configuration(self):
        # check if public network was created in tungsten fabric when deployed zone with setup/dev/advancedtf.cfg
        list_public_network = Network.list(self.apiclient, zoneid=self.zone.id, issystem=True,
                                           traffictype="Public")
        self.assertEqual(is_object_created(self.apiclient, self.zone.id, "virtual-network",
                                           list_public_network[0].id), True,
                         "Check if public network was created in tungsten")

        # check if management network was created in tungsten fabric when deployed zone with setup/dev/advancedtf.cfg
        list_management_network = Network.list(self.apiclient, zoneid=self.zone.id, issystem=True,
                                               traffictype="Management")
        self.assertEqual(is_object_created(self.apiclient, self.zone.id, "virtual-network",
                                           list_management_network[0].id), True,
                         "Check if management network was created in tungsten")

        # check if ssvm and proxyvm was created in tungsten fabric when deployed zone with setup/dev/advancedtf.cfg
        list_system_vm = get_list_system_vm(self.apiclient, self.zone.id)
        for system_vm in list_system_vm:
            # check if system vm was created in tungsten fabric
            self.assertEqual(
                is_object_created(self.apiclient, self.zone.id, "virtual-machine", system_vm.id),
                True, "Check if system vm was created in tungsten fabric")

        # check if tungsten fabric virtual gateway was created
        username, password = get_host_credentials(self.config, self.tfgw)
        ssh = is_server_ssh_ready(self.tfgw, 22, username, password)
        vgw = ssh.execute("vif --list | grep vgw")
        self.assertEqual(
            str(vgw).count("vgw"), 1, "Check if vgw was created in host"
        )

        return

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_connectivity(self):
        # test create network offering
        network_offering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        # check if network offering was created
        self.logger.info("network offering id is %s" % network_offering.id)
        list_network_offering_response = NetworkOffering.list(self.apiclient,
                                                              id=network_offering.id)
        self.assertEqual(isinstance(list_network_offering_response, list), True,
                         "Check for a valid list network response")

        # create network
        network = Network.create(
            self.apiclient,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )

        # check if network was created in cloudstack
        self.logger.info("network id is %s" % network.id)
        list_network_response = Network.list(self.apiclient, id=network.id, listall=True)
        self.assertEqual(isinstance(list_network_response, list), True,
                         "Check for a valid list network response")

        # create virtual machine 1
        vm1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )

        # create virtual machine 2
        vm2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm2"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )

        # check if vm 1 was created in cloudstack
        self.logger.info("vm id is %s" % vm1.id)
        list_vm_response = VirtualMachine.list(self.apiclient, id=vm1.id, listall=True)
        self.assertEqual(isinstance(list_vm_response, list), True,
                         "Check for a valid list vm response")

        # check if vm 2 was created in cloudstack
        self.logger.info("vm id is %s" % vm2.id)
        list_vm_response = VirtualMachine.list(self.apiclient, id=vm2.id, listall=True)
        self.assertEqual(isinstance(list_vm_response, list), True,
                         "Check for a valid list vm response")

        # wait for vm start
        time.sleep(30)

        # tungsten fabric network was only created when network was implemented
        # check if network was created in tungsten fabric
        self.assertEqual(
            is_object_created(self.apiclient, self.zone.id, "virtual-network", network.id), True,
            "Check if network was created in tungsten")

        # check if vm 1 was created in tungsten fabric
        self.assertEqual(is_object_created(self.apiclient, self.zone.id, "virtual-machine", vm1.id),
                         True, "Check if vm1 was created in tungsten")

        # check if vm 2 was created in tungsten fabric
        self.assertEqual(is_object_created(self.apiclient, self.zone.id, "virtual-machine", vm2.id),
                         True, "Check if vm1 was created in tungsten")

        # check if nic 1 was created in tungsten fabric
        self.assertEqual(
            is_object_created(self.apiclient, self.zone.id, "virtual-machine-interface", vm1.nic[0].id),
            True, "Check if nic 1 was created in tungsten")

        # check if nic 2 was created in tungsten fabric
        self.assertEqual(
            is_object_created(self.apiclient, self.zone.id, "virtual-machine-interface", vm2.nic[0].id),
            True, "Check if nic 2 was created in tungsten")

        # check vm1 vif interface was created in host
        host1 = Host.list(self.apiclient, id=vm1.hostid)
        username, password = get_host_credentials(self.config, host1[0].ipaddress)
        tap1 = "tap" + vm1.nic[0].macaddress.replace(":", "")
        cmd1 = "vif --list | grep %s" % tap1
        vif1 = execute_command_in_host(host1[0].ipaddress, 22, username, password, cmd1)
        self.assertEqual(
            str(vif1).count(tap1), 1, "Check if vm1 tap interface was created in host"
        )

        # check vm2 vif interface was created in host
        host2 = Host.list(self.apiclient, id=vm2.hostid)
        username, password = get_host_credentials(self.config, host2[0].ipaddress)
        tap2 = "tap" + vm2.nic[0].macaddress.replace(":", "")
        cmd2 = "vif --list | grep %s" % tap2
        vif2 = execute_command_in_host(host2[0].ipaddress, 22, username, password, cmd2)
        self.assertEqual(
            str(vif2).count(tap2), 1, "Check if vm2 tap interface was created in host"
        )

        # ssh vm 1 and ping vm2
        ssh1 = vm1.get_ssh_client()
        res_1 = ssh1.execute("ping -c1 %s" % vm2.ipaddress)

        # ssh vm 2 and ping vm1
        ssh2 = vm2.get_ssh_client()
        res_2 = ssh2.execute("ping -c1 %s" % vm1.ipaddress)

        self.assertEqual(
            str(res_1).count("1 received"),
            1,
            "Ping vm_2 from vm_1 should be successful"
        )

        self.assertEqual(
            str(res_2).count("1 received"),
            1,
            "Ping vm_1 from vm_2 should be successful"
        )

        # delete vm 1
        try:
            vm1.delete(self.apiclient, expunge=True)
        except Exception as e:
            raise Exception("Warning: Exception in expunging vms: %s" % e)

        # delete vm 2
        try:
            vm2.delete(self.apiclient, expunge=True)
        except Exception as e:
            raise Exception("Warning: Exception in expunging vms: %s" % e)

        # check if nic 1 was deleted in tungsten fabric
        self.assertEqual(
            is_object_deleted(self.apiclient, self.zone.id, "virtual-machine-interface", vm1.nic[0].id),
            True, "Check if nic 1 was deleted in tungsten")

        # check if nic 2 was deleted in tungsten fabric
        self.assertEqual(
            is_object_deleted(self.apiclient, self.zone.id, "virtual-machine-interface", vm2.nic[0].id),
            True, "Check if nic 2 was deleted in tungsten")

        # check if vm 1 was deleted in tungsten fabric
        self.assertEqual(
            is_object_deleted(self.apiclient, self.zone.id, "virtual-machine", vm1.id),
            True, "Check if vm was deleted in tungsten")

        # check if vm 2 was deleted in tungsten fabric
        self.assertEqual(
            is_object_deleted(self.apiclient, self.zone.id, "virtual-machine", vm2.id),
            True, "Check if vm was deleted in tungsten")

        network.delete(self.apiclient)

        # check if network was deleted in tungsten fabric
        self.assertEqual(
            is_object_deleted(self.apiclient, self.zone.id, "virtual-network", network.id),
            True, "Check if network was deleted in tungsten")

        return

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_sourcenat(self):
        # create network offering
        network_offering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        # create network
        network = Network.create(
            self.apiclient,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # create virtual machine
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm)

        # open firewall rule
        EgressFireWallRule.create(
            apiclient=self.apiclient,
            networkid=network.id,
            protocol='All',
            cidrlist='0.0.0.0/0'
        )

        # wait for vm start
        # time.sleep(30)

        # ssh vm, ping google.com and verified response package
        ssh = vm.get_ssh_client()
        res = ssh.execute("ping -c1 google.com")
        self.assertEqual(
            str(res).count("1 received"),
            1,
            "Ping google.com from vm should be successful"
        )

        return

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_staticnat(self):
        # create network offering
        network_offering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        # create network
        network = Network.create(
            self.apiclient,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # create virtual machine
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm)

        # wait for vm start
        time.sleep(30)

        public_ip = PublicIPAddress.create(
            apiclient=self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            services=self.services,
            networkid=network.id
        )

        FireWallRule.create(
            apiclient=self.apiclient,
            ipaddressid=public_ip.ipaddress.id,
            protocol='TCP',
            cidrlist=['0.0.0.0/0'],
            startport=80,
            endport=80
        )

        NATRule.create(
            apiclient=self.apiclient,
            virtual_machine=vm,
            services=self.services["nat_rule"],
            ipaddressid=public_ip.ipaddress.id
        )

        # ssh vm, start httpd, clear iptables
        ssh = vm.get_ssh_client()
        ssh.execute("service httpd start")
        ssh.execute("iptables -F")
        ssh.execute("echo test > /var/www/html/test.html")

        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36'}
        r = requests.get("http://%s/test.html" % public_ip.ipaddress.ipaddress, headers=headers)
        self.assertEqual(r.status_code, 200, "Static nat was created and applied to vm")

        return

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_loadbalancing(self):
        # create network offering
        network_offering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        # create network
        network = Network.create(
            self.apiclient,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # create virtual machine 1
        vm1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm1)

        # create virtual machine 2
        vm2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm2"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm2)

        public_ip = PublicIPAddress.create(
            apiclient=self.apiclient,
            accountid=self.account.name,
            zoneid=self.zone.id,
            domainid=self.account.domainid,
            services=self.services,
            networkid=network.id
        )

        # open firewall rule
        EgressFireWallRule.create(
            apiclient=self.apiclient,
            networkid=network.id,
            protocol='All',
            cidrlist='0.0.0.0/0'
        )

        firewall_rule = FireWallRule.create(
            apiclient=self.apiclient,
            ipaddressid=public_ip.ipaddress.id,
            protocol='TCP',
            cidrlist=['0.0.0.0/0'],
            startport=80,
            endport=80
        )
        self.cleanup.append(firewall_rule)

        lb_rule = LoadBalancerRule.create(
            self.apiclient,
            self.services["lbrule"],
            ipaddressid=public_ip.ipaddress.id,
            accountid=self.account.name
        )
        self.cleanup.append(lb_rule)
        lb_rule.assign(self.apiclient, [vm1, vm2])

        # ssh vm1 and vm2, start httpd, clear iptables
        ssh1 = vm1.get_ssh_client()
        ssh1.execute("service httpd start")
        ssh1.execute("iptables -F")
        ssh1.execute("echo test1 > /var/www/html/test.html")
        ssh2 = vm2.get_ssh_client()
        ssh2.execute("service httpd start")
        ssh2.execute("iptables -F")
        ssh2.execute("echo test2 > /var/www/html/test.html")

        # wait for tungsten instance service create
        time.sleep(120)

        # check if load balancing was apply and content was load balancing with round robin method
        contents = ["test1", "test2"]
        headers = {'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36'}
        r1 = requests.get("http://%s/test.html" % public_ip.ipaddress.ipaddress, headers=headers)
        self.assertEqual(r1.status_code, 200, "Load balance was created and applied to vms")
        self.logger.info("http response 1 content %s" % r1.text)
        contents.remove(r1.text.rstrip())
        self.assertEqual(len(contents), 1, "Contents was load balancing with test1 or test2")

        r2 = requests.get("http://%s/test.html" % public_ip.ipaddress.ipaddress, headers=headers)
        self.assertEqual(r2.status_code, 200, "Load balance was created and applied to vms")
        self.logger.info("http response 2 content %s" % r2.text)
        contents.remove(r2.text.rstrip())
        self.assertEqual(len(contents), 0, "Contents was load balancing with the rest of contents")

        return

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_logicalrouter(self):
        # create network offering
        network_offering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        # create network 1
        network1 = Network.create(
            self.apiclient,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network1)

        # create virtual machine 1
        vm1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network1.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm1)

        # create network 2
        network2 = Network.create(
            self.apiclient,
            self.services["networks"]["network2"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network2)

        # create virtual machine 2
        vm2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm2"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network2.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm2)

        ssh1 = vm1.get_ssh_client()
        res1 = ssh1.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res1).count("0 received"),
            1,
            "Ping vm2 from vm1 should be not successful"
        )

        logicalrouter = LogicalRouter.create(
            self.apiclient,
            self.zone.id,
            self.services["logicalrouter"]["name"]
        )
        self.cleanup.append(logicalrouter)

        LogicalRouter.add(
            self.apiclient,
            self.zone.id,
            logicalrouter.uuid,
            network1.id)

        LogicalRouter.add(
            self.apiclient,
            self.zone.id,
            logicalrouter.uuid,
            network2.id)

        # open firewall rule for network 1
        EgressFireWallRule.create(
            apiclient=self.apiclient,
            networkid=network1.id,
            protocol='All',
            cidrlist='0.0.0.0/0'
        )

        # open firewall rule for network 2
        EgressFireWallRule.create(
            apiclient=self.apiclient,
            networkid=network2.id,
            protocol='All',
            cidrlist='0.0.0.0/0'
        )

        ssh2 = vm1.get_ssh_client()
        res2 = ssh2.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res2).count("1 received"),
            1,
            "Ping vm2 from vm1 should be successful"
        )

        LogicalRouter.remove(
            self.apiclient,
            self.zone.id,
            logicalrouter.uuid,
            network1.id)

        LogicalRouter.remove(
            self.apiclient,
            self.zone.id,
            logicalrouter.uuid,
            network2.id)

        return

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_firewall(self):
        # create network offering
        network_offering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        # create network
        network = Network.create(
            self.apiclient,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # create virtual machine
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm)

        # ssh vm, ping google.com and verified response package
        ssh1 = vm.get_ssh_client()
        res1 = ssh1.execute("ping -c1 google.com")
        self.assertEqual(
            str(res1).count("0 received"),
            1,
            "Ping google.com from vm should be fail"
        )

        # open firewall rule
        EgressFireWallRule.create(
            apiclient=self.apiclient,
            networkid=network.id,
            protocol='All',
            cidrlist='0.0.0.0/0'
        )

        # wait for vm start
        time.sleep(30)

        # ssh vm, ping google.com and verified response package
        ssh2 = vm.get_ssh_client()
        res2 = ssh2.execute("ping -c1 google.com")
        self.assertEqual(
            str(res2).count("1 received"),
            1,
            "Ping google.com from vm should be successful"
        )

        return

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_network_policy(self):
        # create network offering
        network_offering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        # create network
        network = Network.create(
            self.apiclient,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        networkpolicy = NetworkPolicy.create(
            self.apiclient,
            self.zone.id,
            self.services["network_policy"]["policy1"]["name"]
        )
        self.cleanup.append(networkpolicy)

        self.assertEqual(
            is_object_created(self.apiclient, self.zone.id, "network-policy", networkpolicy.uuid),
            True, "Check if network policy was created in tungsten")

        # create virtual machine
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm)

        ssh = vm.get_ssh_client()
        res = ssh.execute("ping -c1 google.com")
        self.assertEqual(
            str(res).count("0 received"),
            1,
            "Ping google.com from vm should not be successful"
        )

        NetworkPolicy.apply(
            self.apiclient,
            self.zone.id,
            network.id,
            networkpolicy.uuid,
            1,
            1
        )

        policyrule = PolicyRule.create(
            self.apiclient,
            self.zone.id,
            networkpolicy.uuid,
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["action"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["direction"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["protocol"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["srcnetwork"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["srcipprefix"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["srcipprefixlend"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["srcstartport"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["srcendport"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["destnetwork"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["destipprefix"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["destipprefixlen"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["deststartport"],
            self.services["network_policy"]["policy1"]["rules"]["rule1"]["destendport"]
        )

        ssh1 = vm.get_ssh_client()
        res1 = ssh1.execute("ping -c1 google.com")
        self.assertEqual(
            str(res1).count("1 received"),
            1,
            "Ping google.com from vm should be successful"
        )

        PolicyRule.delete(
            self.apiclient,
            self.zone.id,
            networkpolicy.uuid,
            policyrule.uuid
        )

        NetworkPolicy.remove(
            self.apiclient,
            self.zone.id,
            network.id,
            networkpolicy.uuid
        )

        return

    @skipTestIf("not_tungsten_fabric_zone")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_application_policy_set(self):
        # create network offering
        network_offering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        # create network
        network = Network.create(
            self.apiclient,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # create virtual machine 1
        vm1 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm1)

        # create virtual machine 2
        vm2 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm2"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm2)

        # create virtual machine 3
        vm3 = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm3"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced'
        )
        self.cleanup.append(vm3)

        applicationtag = TungstenTag.create(
            self.apiclient,
            zoneid=self.zone.id,
            tagtype="application",
            tagvalue="webapp"
        )
        self.cleanup.append(applicationtag)

        frontendtag = TungstenTag.create(
            self.apiclient,
            zoneid=self.zone.id,
            tagtype="tier",
            tagvalue="frontend"
        )
        self.cleanup.append(frontendtag)

        backendtag = TungstenTag.create(
            self.apiclient,
            zoneid=self.zone.id,
            tagtype="tier",
            tagvalue="backend"
        )
        self.cleanup.append(backendtag)

        aps = ApplicationPolicySet.create(
            self.apiclient,
            self.zone.id,
            self.services["applicationpolicysets"]["aps1"]["name"]
        )
        self.cleanup.append(aps)

        frontendpolicy = FirewallPolicy.create(
            self.apiclient,
            self.zone.id,
            aps.uuid,
            self.services["firewallpolicys"]["frontend"]["name"],
            1
        )
        self.cleanup.append(frontendpolicy)

        internetpolicy = FirewallPolicy.create(
            self.apiclient,
            self.zone.id,
            aps.uuid,
            self.services["firewallpolicys"]["internet"]["name"],
            3
        )
        self.cleanup.append(internetpolicy)

        hostpolicy = FirewallPolicy.create(
            self.apiclient,
            self.zone.id,
            aps.uuid,
            self.services["firewallpolicys"]["host"]["name"],
            4
        )
        self.cleanup.append(hostpolicy)

        anyservicegroup = ServiceGroup.create(
            self.apiclient,
            self.zone.id,
            self.services["servicegroups"]["any"]["name"],
            self.services["servicegroups"]["any"]["protocol"],
            self.services["servicegroups"]["any"]["startport"],
            self.services["servicegroups"]["any"]["endport"]
        )
        self.cleanup.append(anyservicegroup)

        frontendrule = FirewallRule.create(
            self.apiclient,
            self.zone.id,
            frontendpolicy.uuid,
            name=self.services["firewallrules"]["frontend"]["name"],
            action=self.services["firewallrules"]["frontend"]["action"],
            direction=self.services["firewallrules"]["frontend"]["direction"],
            servicegroupuuid=anyservicegroup.uuid,
            srctaguuid=frontendtag.uuid,
            desttaguuid=backendtag.uuid,
            sequence=1
        )
        self.cleanup.append(frontendrule)

        internetrule = FirewallRule.create(
            self.apiclient,
            self.zone.id,
            internetpolicy.uuid,
            name=self.services["firewallrules"]["internet"]["name"],
            action=self.services["firewallrules"]["internet"]["action"],
            direction=self.services["firewallrules"]["internet"]["direction"],
            servicegroupuuid=anyservicegroup.uuid,
            srctaguuid=applicationtag.uuid,
            destnetworkuuid=network.id,
            sequence=1
        )
        self.cleanup.append(internetrule)

        publicnetwork = Network.list(self.apiclient, listall=True, issystem=True, traffictype="Public")
        hostrule = FirewallRule.create(
            self.apiclient,
            self.zone.id,
            hostpolicy.uuid,
            name=self.services["firewallrules"]["host"]["name"],
            action=self.services["firewallrules"]["host"]["action"],
            direction=self.services["firewallrules"]["host"]["direction"],
            servicegroupuuid=anyservicegroup.uuid,
            srcnetworkuuid=publicnetwork[0].id,
            desttaguuid=applicationtag.uuid,
            sequence=1
        )
        self.cleanup.append(hostrule)

        TungstenTag.apply(
            self.apiclient,
            self.zone.id,
            applicationpolicysetuuid=aps.uuid,
            taguuid=applicationtag.uuid
        )

        TungstenTag.apply(
            self.apiclient,
            self.zone.id,
            taguuid=applicationtag.uuid,
            nicuuid=vm1.nic[0].id
        )

        TungstenTag.apply(
            self.apiclient,
            self.zone.id,
            taguuid=frontendtag.uuid,
            nicuuid=vm1.nic[0].id
        )

        TungstenTag.apply(
            self.apiclient,
            self.zone.id,
            taguuid=applicationtag.uuid,
            nicuuid=vm2.nic[0].id
        )

        TungstenTag.apply(
            self.apiclient,
            self.zone.id,
            taguuid=backendtag.uuid,
            nicuuid=vm2.nic[0].id
        )

        # open firewall rule for network
        EgressFireWallRule.create(
            apiclient=self.apiclient,
            networkid=network.id,
            protocol='All',
            cidrlist='0.0.0.0/0'
        )

        ssh1 = vm1.get_ssh_client()
        ssh2 = vm2.get_ssh_client()
        ssh3 = vm3.get_ssh_client()

        # vm1 can access to internet
        res1 = ssh1.execute("ping -c1 google.com")
        self.assertEqual(
            str(res1).count("1 received"),
            1,
            "Ping google from vm1 should be successful"
        )

        # vm2 can access to internet
        res2 = ssh2.execute("ping -c1 google.com")
        self.assertEqual(
            str(res2).count("1 received"),
            1,
            "Ping google from vm2 should be successful"
        )

        # vm3 can access to internet
        res3 = ssh3.execute("ping -c1 google.com")
        self.assertEqual(
            str(res3).count("1 received"),
            1,
            "Ping google from vm3 should be successful"
        )

        # vm3(outside) can not ping to vm1
        res4 = ssh3.execute("ping -c1 %s" % vm1.ipaddress)
        self.assertEqual(
            str(res4).count("0 received"),
            1,
            "Ping vm1 from vm3 should be not successful"
        )

        # vm3(outside) can not ping to vm2
        res5 = ssh3.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res5).count("0 received"),
            1,
            "Ping vm2 from vm3 should be not successful"
        )

        # vm1(frontend) can ping to vm2(backend)
        res6 = ssh1.execute("ping -c1 %s" % vm2.ipaddress)
        self.assertEqual(
            str(res6).count("1 received"),
            1,
            "Ping vm1 from vm2 should be successful"
        )

        TungstenTag.remove(
            self.apiclient,
            self.zone.id,
            applicationpolicysetuuid=aps.uuid,
            taguuid=applicationtag.uuid
        )
        TungstenTag.remove(
            self.apiclient,
            self.zone.id,
            taguuid=applicationtag.uuid,
            nicuuid=vm1.nic[0].id
        )
        TungstenTag.remove(
            self.apiclient,
            self.zone.id,
            taguuid=frontendtag.uuid,
            nicuuid=vm1.nic[0].id
        )
        TungstenTag.remove(
            self.apiclient,
            self.zone.id,
            taguuid=applicationtag.uuid,
            nicuuid=vm2.nic[0].id
        )
        TungstenTag.remove(
            self.apiclient,
            self.zone.id,
            taguuid=backendtag.uuid,
            nicuuid=vm2.nic[0].id
        )
        return

    @skipTestIf("can_not_migrate")
    @attr(tags=["tungstenfabric"], required_hardware="false")
    def test_01_vm_migration(self):
        # create network offering
        network_offering = NetworkOffering.create(self.apiclient, self.services["network_offering"])
        network_offering.update(self.apiclient, state='Enabled')
        self.cleanup.append(network_offering)

        # create network
        network = Network.create(
            self.apiclient,
            self.services["networks"]["network1"],
            networkofferingid=network_offering.id,
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid
        )
        self.cleanup.append(network)

        # create virtual machine
        vm = VirtualMachine.create(
            self.apiclient,
            self.services["virtual_machines"]["vm1"],
            serviceofferingid=self.service_offering.id,
            templateid=self.template.id,
            networkids=[str(network.id)],
            zoneid=self.zone.id,
            accountid=self.account.name,
            domainid=self.account.domainid,
            mode='advanced',
            hostid=self.hosts[0].id
        )
        self.cleanup.append(vm)

        # open firewall rule for network 1
        EgressFireWallRule.create(
            apiclient=self.apiclient,
            networkid=network.id,
            protocol='All',
            cidrlist='0.0.0.0/0'
        )

        ssh = vm.get_ssh_client()
        res = ssh.execute("ping -c1 google.com")
        self.assertEqual(
            str(res).count("1 received"),
            1,
            "Ping google.com from vm1 should be successful"
        )

        cmd = migrateVirtualMachine.migrateVirtualMachineCmd()
        cmd.virtualmachineid = vm.id
        cmd.hostid = self.hosts[1].id
        migrate = self.apiclient.migrateVirtualMachine(cmd)

        ssh = vm.get_ssh_client()
        res = ssh.execute("ping -c1 google.com")
        self.assertEqual(
            str(res).count("1 received"),
            1,
            "Ping google.com from vm1 should be successful"
        )
