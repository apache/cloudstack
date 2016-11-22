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

""" Custom base class for Nuage VSP SDN plugin specific Marvin tests
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (EgressFireWallRule,
                             FireWallRule,
                             Host,
                             Hypervisor,
                             Network,
                             NetworkACL,
                             NetworkACLList,
                             NetworkOffering,
                             NetworkServiceProvider,
                             Nuage,
                             PhysicalNetwork,
                             PublicIPAddress,
                             Router,
                             ServiceOffering,
                             StaticNATRule,
                             VirtualMachine,
                             VPC,
                             VpcOffering)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone)
from marvin.lib.utils import cleanup_resources
from marvin.cloudstackAPI import restartVPC
# Import System Modules
import importlib
import logging
import socket
import sys
import time


class nuageTestCase(cloudstackTestCase):

    @classmethod
    def setUpClass(cls, zone=None):
        cls.debug("setUpClass nuageTestCase")

        # We want to fail quicker, if it's a failure
        socket.setdefaulttimeout(60)

        test_client = super(nuageTestCase, cls).getClsTestClient()
        cls.api_client = test_client.getApiClient()
        cls.db_client = test_client.getDbConnection()
        cls.test_data = test_client.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client,
                            zone_name=zone.name if zone else None,
                            zone_id=zone.id if zone else None
                            )
        cls.domain = get_domain(cls.api_client)
        cls.template = get_template(cls.api_client,
                                    cls.zone.id,
                                    cls.test_data["ostype"]
                                    )
        cls.test_data["virtual_machine"]["zoneid"] = cls.zone.id
        cls.test_data["virtual_machine"]["template"] = cls.template.id

        # Create service offering
        cls.service_offering = ServiceOffering.create(cls.api_client,
                                                      cls.test_data["service_offering"]
                                                      )
        cls._cleanup = [cls.service_offering]

        # Check if the host hypervisor type is simulator
        cls.isSimulator = Hypervisor.list(cls.api_client, zoneid=cls.zone.id)[0].name == "Simulator"

        # Get configured Nuage VSP device details
        try:
            physical_networks = PhysicalNetwork.list(cls.api_client, zoneid=cls.zone.id)
            for pn in physical_networks:
                if pn.isolationmethods == "VSP":
                    cls.vsp_physical_network = pn
                    break
            cls.nuage_vsp_device = Nuage.list(cls.api_client,
                                              physicalnetworkid=cls.vsp_physical_network.id
                                              )[0]
            pns = cls.config.zones[0].physical_networks
            providers = filter(lambda physical_network: "VSP" in physical_network.isolationmethods, pns)[0].providers
            devices = filter(lambda provider: provider.name == "NuageVsp", providers)[0].devices
            cls.nuage_vsp_device.username = devices[0].username
            cls.nuage_vsp_device.password = devices[0].password
            cls.cms_id = cls.nuage_vsp_device.cmsid
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Warning: Could not get configured Nuage VSP device details - %s" % e)

        # VSD is a programmable policy and analytics engine of Nuage VSP SDN platform
        # vspk is a Python SDK for Nuage VSP's VSD
        # libVSD is a library that wraps vspk package
        try:
            vspk_module = "vspk." + cls.nuage_vsp_device.apiversion if int(cls.nuage_vsp_device.apiversion[1]) >= 4 \
                else "vspk.vsdk." + cls.nuage_vsp_device.apiversion
            cls.vsdk = importlib.import_module(vspk_module)
            from libVSD import ApiClient, VSDHelpers
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Warning: vspk (and/or) libVSD package import failure - %s" % e)

        # Configure VSD session
        cls._session = cls.vsdk.NUVSDSession(username=cls.nuage_vsp_device.username,
                                             password=cls.nuage_vsp_device.password,
                                             enterprise="csp",
                                             api_url="https://%s:%d" % (cls.nuage_vsp_device.hostname,
                                                                        cls.nuage_vsp_device.port)
                                             )
        cls._session.start()

        # Configure libVSD session
        root = logging.getLogger()
        log_handler = logging.StreamHandler(sys.stdout)
        formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
        log_handler.setFormatter(formatter)
        root.addHandler(log_handler)
        vsd_info = cls.nuage_vsp_device.__dict__
        cls.debug("Nuage VSP device (VSD) details - %s" % vsd_info)
        vsd_api_client = ApiClient(address=vsd_info["hostname"],
                                   user=vsd_info["username"],
                                   password=vsd_info["password"],
                                   version=vsd_info["apiversion"][1] + "." + vsd_info["apiversion"][3]
                                   )
        vsd_api_client.new_session()
        cls.vsd = VSDHelpers(vsd_api_client)

        cls.debug("setUpClass nuageTestCase [DONE]")

    def setUp(self):
        self.cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        try:
            # Cleanup resources used
            cleanup_resources(cls.api_client, cls._cleanup)
        except Exception as e:
            cls.debug("Warning: Exception during cleanup: %s" % e)
        return

    def tearDown(self):
        # Cleanup resources used
        self.debug("Cleaning up the resources")
        for obj in reversed(self.cleanup):
            try:
                if isinstance(obj, VirtualMachine):
                    obj.delete(self.api_client, expunge=True)
                else:
                    obj.delete(self.api_client)
            except Exception as e:
                self.error("Failed to cleanup %s, got %s" % (obj, e))
        # cleanup_resources(self.api_client, self.cleanup)
        self.cleanup = []
        self.debug("Cleanup complete!")
        return

    # create_VpcOffering - Creates VPC offering
    def create_VpcOffering(self, vpc_offering, suffix=None):
        self.debug("Creating VPC offering")
        if suffix:
            vpc_offering["name"] = "VPC_OFF-" + str(suffix)
        vpc_off = VpcOffering.create(self.api_client,
                                     vpc_offering
                                     )
        # Enable VPC offering
        vpc_off.update(self.api_client, state="Enabled")
        self.cleanup.append(vpc_off)
        self.debug("Created and Enabled VPC offering")
        return vpc_off

    # create_Vpc - Creates VPC with the given VPC offering
    def create_Vpc(self, vpc_offering, cidr='10.1.0.0/16', testdata=None, account=None, networkDomain=None,
                   cleanup=True):
        if not account:
            account = self.account
        self.debug("Creating a VPC in the account - %s" % account.name)
        if not testdata:
            testdata = self.test_data["vpc"]
            testdata["name"] = "TestVPC-" + cidr + "-" + str(vpc_offering.name)
            testdata["displaytext"] = "Test VPC"
            testdata["cidr"] = cidr
        vpc = VPC.create(self.api_client,
                         testdata,
                         vpcofferingid=vpc_offering.id,
                         zoneid=self.zone.id,
                         account=account.name,
                         domainid=account.domainid,
                         networkDomain=networkDomain
                         )
        self.debug("Created VPC with ID - %s" % vpc.id)
        if cleanup:
            self.cleanup.append(vpc)
        return vpc

    # restart_Vpc - Restarts the given VPC with/without cleanup
    def restart_Vpc(self, vpc, cleanup=None):
        self.debug("Restarting VPC with ID - %s" % vpc.id)
        cmd = restartVPC.restartVPCCmd()
        cmd.id = vpc.id
        cmd.cleanup = cleanup
        cmd.makeredundant = False
        self.api_client.restartVPC(cmd)
        self.debug("Restarted VPC with ID - %s" % vpc.id)

    # create_NetworkOffering - Creates Network offering
    def create_NetworkOffering(self, net_offering, suffix=None, conserve_mode=False):
        self.debug("Creating Network offering")
        if suffix:
            net_offering["name"] = "NET_OFF-" + str(suffix)
        nw_off = NetworkOffering.create(self.api_client,
                                        net_offering,
                                        conservemode=conserve_mode
                                        )
        # Enable Network offering
        nw_off.update(self.api_client, state="Enabled")
        self.cleanup.append(nw_off)
        self.debug("Created and Enabled Network offering")
        return nw_off

    # create_Network - Creates network with the given Network offering
    def create_Network(self, nw_off, gateway="10.1.1.1", netmask="255.255.255.0", vpc=None, acl_list=None,
                       testdata=None, account=None, cleanup=True):
        if not account:
            account = self.account
        self.debug("Creating a network in the account - %s" % account.name)
        if not testdata:
            testdata = self.test_data["network"]
            testdata["name"] = "TestNetwork-" + gateway + "-" + str(nw_off.name)
            testdata["displaytext"] = "Test Network"
            testdata["netmask"] = netmask
        network = Network.create(self.api_client,
                                 testdata,
                                 accountid=account.name,
                                 domainid=account.domainid,
                                 networkofferingid=nw_off.id,
                                 zoneid=self.zone.id,
                                 gateway=gateway,
                                 vpcid=vpc.id if vpc else self.vpc.id if hasattr(self, "vpc") else None,
                                 aclid=acl_list.id if acl_list else None
                                 )
        self.debug("Created network with ID - %s" % network.id)
        if cleanup:
            self.cleanup.append(network)
        return network

    # upgrade_Network - Upgrades the given network
    def upgrade_Network(self, nw_off, network):
        if not hasattr(nw_off, "id"):
            nw_off = self.create_NetworkOffering(nw_off, network.gateway)
        self.debug("Updating Network with ID - %s" % network.id)
        network.update(self.api_client,
                       networkofferingid=nw_off.id,
                       changecidr=False
                       )
        self.debug("Updated network with ID - %s" % network.id)

    # delete_Network - Deletes the given network
    def delete_Network(self, network):
        self.debug("Deleting Network with ID - %s" % network.id)
        network.delete(self.api_client)
        if network in self.cleanup:
            self.cleanup.remove(network)
        self.debug("Deleted Network with ID - %s" % network.id)

    # create_VM - Creates VM in the given network(s)
    def create_VM(self, network_list, host_id=None, start_vm=True, testdata=None, account=None, cleanup=True):
        network_ids = []
        if isinstance(network_list, list):
            for network in network_list:
                network_ids.append(str(network.id))
        else:
            network_ids.append(str(network_list.id))
        if not account:
            account = self.account
        self.debug("Creating VM in network(s) with ID(s) - %s in the account - %s" % (network_ids, account.name))
        if not testdata:
            testdata = self.test_data["virtual_machine"]
        vm = VirtualMachine.create(self.api_client,
                                   testdata,
                                   accountid=account.name,
                                   domainid=account.domainid,
                                   serviceofferingid=self.service_offering.id,
                                   templateid=self.template.id,
                                   zoneid=self.zone.id,
                                   networkids=network_ids,
                                   startvm=start_vm,
                                   hostid=host_id
                                   )
        self.debug("Created VM with ID - %s in network(s) with ID(s) - %s" % (vm.id, network_ids))
        if cleanup:
            self.cleanup.append(vm)
        return vm

    # nic_operation_VM - Performs NIC operations such as add, remove, and update default NIC in the given VM and network
    def nic_operation_VM(self, vm, network, operation="update"):
        self.debug("Performing %s NIC operation in VM with ID - %s and network with ID - %s" %
                   (operation, vm.id, network.id))
        for nic in vm.nic:
            if nic.networkid == network.id:
                nic_id = nic.id
        if operation is "update":
            vm.update_default_nic(self.api_client, nic_id)
            self.debug("Updated default NIC to NIC with ID - %s in VM with ID - %s and network with ID - %s" %
                       (nic_id, vm.id, network.id))
        if operation is "remove":
            vm.remove_nic(self.api_client, nic_id)
            self.debug("Removed NIC with ID - %s in VM with ID - %s and network with ID - %s" %
                       (nic_id, vm.id, network.id))
        if operation is "add":
            vm.add_nic(self.api_client, network.id)
            self.debug("Added NIC with ID - %s in VM with ID - %s and network with ID - %s" %
                       (nic_id, vm.id, network.id))

    # migrate_VM - Migrates VM to another host, if available
    def migrate_VM(self, vm):
        self.debug("Checking if a host is available for migration...")
        hosts = Host.listForMigration(self.api_client, virtualmachineid=vm.id)
        self.assertEqual(isinstance(hosts, list), True,
                         "List hosts should return a valid list"
                         )
        # Remove the host of current VM from the hosts list
        hosts[:] = [host for host in hosts if host.id != vm.hostid]
        if len(hosts) <= 0:
            self.skipTest("No host available for migration. Test requires at-least 2 hosts")
        host = hosts[0]
        self.debug("Migrating VM with ID: %s to Host: %s" % (vm.id, host.id))
        try:
            vm.migrate(self.api_client, hostid=host.id)
        except Exception as e:
            self.fail("Failed to migrate instance, %s" % e)
        self.debug("Migrated VM with ID: %s to Host: %s" % (vm.id, host.id))

    # delete_VM - Deletes the given VM
    def delete_VM(self, vm, expunge=True):
        self.debug("Deleting VM with ID - %s" % vm.id)
        vm.delete(self.api_client, expunge=expunge)
        if vm in self.cleanup:
            self.cleanup.remove(vm)
        self.debug("Deleted VM with ID - %s" % vm.id)

    # get_Router - Returns router for the given network
    def get_Router(self, network):
        self.debug("Finding the virtual router for network with ID - %s" % network.id)
        routers = Router.list(self.api_client,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(isinstance(routers, list), True,
                         "List routers should return a valid virtual router for network"
                         )
        return routers[0]

    # acquire_PublicIPAddress - Acquires public IP address for the given network/VPC
    def acquire_PublicIPAddress(self, network, vpc=None, account=None):
        if not account:
            account = self.account
        self.debug("Associating public IP for network with ID - %s in the account - %s" % (network.id, account.name))
        public_ip = PublicIPAddress.create(self.api_client,
                                           accountid=account.name,
                                           domainid=account.domainid,
                                           zoneid=self.zone.id,
                                           networkid=network.id if vpc is None else None,
                                           vpcid=vpc.id if vpc else self.vpc.id if hasattr(self, "vpc") else None
                                           )
        self.debug("Associated public IP address - %s with network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, network.id))
        return public_ip

    # create_StaticNatRule_For_VM - Creates Static NAT rule on the given public IP for the given VM in the given network
    def create_StaticNatRule_For_VM(self, vm, public_ip, network, vmguestip=None):
        self.debug("Enabling Static NAT rule on public IP - %s for VM with ID - %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))
        static_nat_rule = StaticNATRule.enable(self.api_client,
                                               ipaddressid=public_ip.ipaddress.id,
                                               virtualmachineid=vm.id,
                                               networkid=network.id,
                                               vmguestip=vmguestip
                                               )
        self.debug("Static NAT rule enabled on public IP - %s for VM with ID - %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))
        return static_nat_rule

    # delete_StaticNatRule_For_VM - Deletes Static NAT rule on the given public IP
    def delete_StaticNatRule_For_VM(self, public_ip):
        self.debug("Disabling Static NAT rule on public IP - %s" % public_ip.ipaddress.ipaddress)
        StaticNATRule.disable(self.api_client,
                              ipaddressid=public_ip.ipaddress.id
                              )
        self.debug("Static NAT rule disabled on public IP - %s" % public_ip.ipaddress.ipaddress)

    # create_FirewallRule - Creates (Ingress) Firewall rule on the given Static NAT rule enabled public IP for Isolated
    # networks
    def create_FirewallRule(self, public_ip, rule=None):
        if not rule:
            rule = self.test_data["ingress_rule"]
        self.debug("Adding an (Ingress) Firewall rule to make Guest VMs accessible through Static NAT rule - %s" % rule)
        return FireWallRule.create(self.api_client,
                                   ipaddressid=public_ip.ipaddress.id,
                                   protocol=rule["protocol"],
                                   cidrlist=rule["cidrlist"],
                                   startport=rule["startport"],
                                   endport=rule["endport"]
                                   )

    # create_EgressFirewallRule - Creates Egress Firewall rule in the given Isolated network
    def create_EgressFirewallRule(self, network, rule):
        self.debug("Adding an Egress Firewall rule to allow/deny outgoing traffic from Guest VMs - %s" % rule)
        return EgressFireWallRule.create(self.api_client,
                                         networkid=network.id,
                                         protocol=rule["protocol"],
                                         cidrlist=rule["cidrlist"],
                                         startport=rule["startport"],
                                         endport=rule["endport"]
                                         )

    # create_NetworkAclList - Creates network ACL list in the given VPC
    def create_NetworkAclList(self, name, description, vpc):
        self.debug("Adding NetworkACL list in VPC with ID - %s" % vpc.id)
        return NetworkACLList.create(self.api_client,
                                     services={},
                                     name=name,
                                     description=description,
                                     vpcid=vpc.id
                                     )

    # create_NetworkAclRule - Creates Ingress/Egress Network ACL rule in the given VPC network/acl list
    def create_NetworkAclRule(self, rule, traffic_type="Ingress", network=None, acl_list=None):
        self.debug("Adding NetworkACL rule - %s" % rule)
        if acl_list:
            return NetworkACL.create(self.api_client,
                                     networkid=network.id if network else None,
                                     services=rule,
                                     traffictype=traffic_type,
                                     aclid=acl_list.id
                                     )
        else:
            return NetworkACL.create(self.api_client,
                                     networkid=network.id if network else None,
                                     services=rule,
                                     traffictype=traffic_type
                                     )

    # ssh_into_VM - Gets into the shell of the given VM using its public IP
    def ssh_into_VM(self, vm, public_ip, reconnect=True):
        self.debug("SSH into VM with ID - %s on public IP address - %s" % (vm.id, public_ip.ipaddress.ipaddress))
        tries = 0
        while tries < 3:
            try:
                ssh_client = vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress, reconnect=reconnect)
            except Exception as e:
                self.debug("Failed to SSH into VM: %s" % e)
                self.debug("Waiting for the VM to be fully resolved for SSH connection...")
                time.sleep(120)
                self.debug("Retrying SSH into VM...")
                tries += 1
                continue
            self.debug("Successful to SSH into VM with ID - %s on public IP address - %s" %
                       (vm.id, public_ip.ipaddress.ipaddress))
            return ssh_client

    # execute_cmd - Executes the given command on the given ssh client
    def execute_cmd(self, ssh_client, cmd):
        self.debug("SSH client executing command - %s" % cmd)
        ret_data = ""
        out_list = ssh_client.execute(cmd)
        if out_list is not None:
            ret_data = ' '.join(map(str, out_list)).strip()
            self.debug("SSH client executed command result - %s" % ret_data)
        else:
            self.debug("SSH client executed command result is None")
        return ret_data

    # wget_from_server - Fetches index.html file from a web server listening on the given public IP address and port
    def wget_from_server(self, public_ip, port):
        import urllib
        self.debug("wget index.html file from a http web server listening on public IP address - %s and port - %s" %
                   (public_ip.ipaddress.ipaddress, port))
        filename, headers = urllib.urlretrieve("http://%s:%s/index.html" % (public_ip.ipaddress.ipaddress, port),
                                               filename="index.html"
                                               )
        return filename, headers

    # validate_NetworkServiceProvider - Validates the given Network Service Provider in the Nuage VSP Physical Network,
    # matches the given provider name and state against the list of providers fetched
    def validate_NetworkServiceProvider(self, provider_name, state=None):
        """Validates the Network Service Provider in the Nuage VSP Physical Network"""
        self.debug("Validating the creation and state of Network Service Provider - %s" % provider_name)
        providers = NetworkServiceProvider.list(self.api_client,
                                                name=provider_name,
                                                physicalnetworkid=self.vsp_physical_network.id)
        self.assertEqual(isinstance(providers, list), True,
                         "List Network Service Provider should return a valid list"
                         )
        self.assertEqual(provider_name, providers[0].name,
                         "Name of the Network Service Provider should match with the returned list data"
                         )
        if state:
            self.assertEqual(providers[0].state, state,
                             "Network Service Provider state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of Network Service Provider - %s" % provider_name)

    # validate_VpcOffering - Validates the given VPC offering, matches the given VPC offering name and state against the
    # list of VPC offerings fetched
    def validate_VpcOffering(self, vpc_offering, state=None):
        """Validates the VPC offering"""
        self.debug("Validating the creation and state of VPC offering - %s" % vpc_offering.name)
        vpc_offs = VpcOffering.list(self.api_client,
                                    id=vpc_offering.id
                                    )
        self.assertEqual(isinstance(vpc_offs, list), True,
                         "List VPC offering should return a valid list"
                         )
        self.assertEqual(vpc_offering.name, vpc_offs[0].name,
                         "Name of the VPC offering should match with the returned list data"
                         )
        if state:
            self.assertEqual(vpc_offs[0].state, state,
                             "VPC offering state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of VPC offering - %s" % vpc_offering.name)

    # validate_Vpc - Validates the given VPC, matches the given VPC name and state against the list of VPCs fetched
    def validate_Vpc(self, vpc, state=None):
        """Validates the VPC"""
        self.debug("Validating the creation and state of VPC - %s" % vpc.name)
        vpcs = VPC.list(self.api_client,
                        id=vpc.id
                        )
        self.assertEqual(isinstance(vpcs, list), True,
                         "List VPC should return a valid list"
                         )
        self.assertEqual(vpc.name, vpcs[0].name,
                         "Name of the VPC should match with the returned list data"
                         )
        if state:
            self.assertEqual(vpcs[0].state, state,
                             "VPC state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of VPC - %s" % vpc.name)

    # validate_NetworkOffering - Validates the given Network offering, matches the given network offering name and state
    # against the list of network offerings fetched
    def validate_NetworkOffering(self, net_offering, state=None):
        """Validates the Network offering"""
        self.debug("Validating the creation and state of Network offering - %s" % net_offering.name)
        net_offs = NetworkOffering.list(self.api_client,
                                        id=net_offering.id
                                        )
        self.assertEqual(isinstance(net_offs, list), True,
                         "List Network offering should return a valid list"
                         )
        self.assertEqual(net_offering.name, net_offs[0].name,
                         "Name of the Network offering should match with the returned list data"
                         )
        if state:
            self.assertEqual(net_offs[0].state, state,
                             "Network offering state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of Network offering - %s" % net_offering.name)

    # validate_Network - Validates the given network, matches the given network name and state against the list of
    # networks fetched
    def validate_Network(self, network, state=None):
        """Validates the network"""
        self.debug("Validating the creation and state of Network - %s" % network.name)
        networks = Network.list(self.api_client,
                                id=network.id
                                )
        self.assertEqual(isinstance(networks, list), True,
                         "List network should return a valid list"
                         )
        self.assertEqual(network.name, networks[0].name,
                         "Name of the network should match with with the returned list data"
                         )
        if state:
            self.assertEqual(networks[0].state, state,
                             "Network state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of Network - %s" % network.name)

    # check_VM_state - Checks if the given VM is in the expected state form the list of fetched VMs
    def check_VM_state(self, vm, state=None):
        """Validates the VM state"""
        self.debug("Validating the deployment and state of VM - %s" % vm.name)
        vms = VirtualMachine.list(self.api_client,
                                  id=vm.id,
                                  listall=True
                                  )
        self.assertEqual(isinstance(vms, list), True,
                         "List virtual machine should return a valid list"
                         )
        if state:
            self.assertEqual(vms[0].state, state,
                             "Virtual machine is not in the expected state"
                             )
        self.debug("Successfully validated the deployment and state of VM - %s" % vm.name)

    # check_Router_state - Checks if the given router is in the expected state form the list of fetched routers
    def check_Router_state(self, router, state=None):
        """Validates the Router state"""
        self.debug("Validating the deployment and state of Router - %s" % router.name)
        routers = Router.list(self.api_client,
                              id=router.id,
                              listall=True
                              )
        self.assertEqual(isinstance(routers, list), True,
                         "List router should return a valid list"
                         )
        if state:
            self.assertEqual(routers[0].state, state,
                             "Virtual router is not in the expected state"
                             )
        self.debug("Successfully validated the deployment and state of Router - %s" % router.name)

    # validate_PublicIPAddress - Validates if the given public IP address is in the expected state form the list of
    # fetched public IP addresses
    def validate_PublicIPAddress(self, public_ip, network, static_nat=False, vm=None):
        """Validates the Public IP Address"""
        self.debug("Validating the assignment and state of public IP address - %s" % public_ip.ipaddress.ipaddress)
        public_ips = PublicIPAddress.list(self.api_client,
                                          id=public_ip.ipaddress.id,
                                          networkid=network.id,
                                          isstaticnat=static_nat,
                                          listall=True
                                          )
        self.assertEqual(isinstance(public_ips, list), True,
                         "List public IP for network should return a valid list"
                         )
        self.assertEqual(public_ips[0].ipaddress, public_ip.ipaddress.ipaddress,
                         "List public IP for network should list the assigned public IP address"
                         )
        self.assertEqual(public_ips[0].state, "Allocated",
                         "Assigned public IP is not in the allocated state"
                         )
        if static_nat and vm:
            self.assertEqual(public_ips[0].virtualmachineid, vm.id,
                             "Static NAT rule is not enabled for the VM on the assigned public IP"
                             )
        self.debug("Successfully validated the assignment and state of public IP address - %s" %
                   public_ip.ipaddress.ipaddress)

    # VSD verifications
    # VSD is a programmable policy and analytics engine of Nuage VSP SDN platform

    # get_externalID_filter - Returns corresponding external ID filter of the given object in VSD
    def get_externalID_filter(self, object_id):
        ext_id = object_id + "@" + self.cms_id
        return self.vsd.set_externalID_filter(ext_id)

    # fetch_by_externalID - Returns VSD object with the given external ID
    def fetch_by_externalID(self, fetcher, *cs_objects):
        """ Fetches a child object by external id using the given fetcher, and uuids of the given cloudstack objects.
        E.G.
          - fetch_by_external_id(vsdk.NUSubnet(id="954de425-b860-410b-be09-c560e7dbb474").vms, cs_vm)
          - fetch_by_external_id(session.user.floating_ips, cs_network, cs_public_ip)
        :param fetcher: VSPK Fetcher to use to find the child entity
        :param cs_objects: Cloudstack objects to take the UUID from.
        :return: the VSPK object having the correct externalID
        """
        return fetcher.get_first(filter="externalID BEGINSWITH '%s'" % ":".join([o.id for o in cs_objects]))

    # verify_vsd_network - Verifies the given domain and network/VPC against the corresponding installed enterprise,
    # domain, zone, and subnet in VSD
    def verify_vsd_network(self, domain_id, network, vpc=None):
        self.debug("Verifying the creation and state of Network - %s in VSD" % network.name)
        vsd_enterprise = self.vsd.get_enterprise(filter=self.get_externalID_filter(domain_id))
        ext_network_filter = self.get_externalID_filter(vpc.id) if vpc else self.get_externalID_filter(network.id)
        vsd_domain = self.vsd.get_domain(filter=ext_network_filter)
        vsd_zone = self.vsd.get_zone(filter=ext_network_filter)
        vsd_subnet = self.vsd.get_subnet(filter=self.get_externalID_filter(network.id))
        self.assertEqual(vsd_enterprise.name, domain_id,
                         "VSD enterprise name should match CloudStack domain uuid"
                         )
        if vpc:
            self.assertEqual(vsd_domain.description, "VPC_" + vpc.name,
                             "VSD domain description should match VPC name in CloudStack"
                             )
            self.assertEqual(vsd_zone.description, "VPC_" + vpc.name,
                             "VSD zone description should match VPC name in CloudStack"
                             )
        else:
            self.assertEqual(vsd_domain.description, network.name,
                             "VSD domain description should match network name in CloudStack"
                             )
            self.assertEqual(vsd_zone.description, network.name,
                             "VSD zone description should match network name in CloudStack"
                             )
        self.assertEqual(vsd_subnet.description, network.name,
                         "VSD subnet description should match network name in CloudStack"
                         )
        self.debug("Successfully verified the creation and state of Network - %s in VSD" % network.name)

    # verify_vsd_vm - Verifies the given VM deployment and state in VSD
    def verify_vsd_vm(self, vm, stopped=None):
        self.debug("Verifying the deployment and state of VM - %s in VSD" % vm.name)
        vsd_vm = self.vsd.get_vm(filter=self.get_externalID_filter(vm.id))
        self.assertNotEqual(vsd_vm, None,
                            "VM data format in VSD should not be of type None"
                            )
        for nic in vm.nic:
            vsd_subnet = self.vsd.get_subnet(filter=self.get_externalID_filter(nic.networkid))
            vsd_vport = self.vsd.get_vport(subnet=vsd_subnet, filter=self.get_externalID_filter(nic.id))
            vsd_vm_interface = self.vsd.get_vm_interface(filter=self.get_externalID_filter(nic.id))
            self.assertEqual(vsd_vport.active, True,
                             "VSD VM vport should be active"
                             )
            self.assertEqual(vsd_vm_interface.ip_address, nic.ipaddress,
                             "VSD VM interface IP address should match VM's NIC IP address in CloudStack"
                             )
        if not self.isSimulator:
            if stopped:
                self.assertEqual(vsd_vm.status, "DELETE_PENDING",
                                 "VM state in VSD should be DELETE_PENDING"
                                 )
            else:
                self.assertEqual(vsd_vm.status, vm.state.upper(),
                                 "VM state in VSD should match its state in CloudStack"
                                 )
        self.debug("Successfully verified the deployment and state of VM - %s in VSD" % vm.name)

    # verify_vsd_router - Verifies the given network router deployment and state in VSD
    def verify_vsd_router(self, router, stopped=None):
        self.debug("Verifying the deployment and state of Router - %s in VSD" % router.name)
        vsd_router = self.vsd.get_vm(filter=self.get_externalID_filter(router.id))
        self.assertNotEqual(vsd_router, None,
                            "Router data format in VSD should not be of type None"
                            )
        if not self.isSimulator:
            if stopped:
                self.assertEqual(vsd_router.status, "DELETE_PENDING",
                                 "Router state in VSD should be DELETE_PENDING"
                                 )
            else:
                self.assertEqual(vsd_router.status, router.state.upper(),
                                 "Router state in VSD should match its state in CloudStack"
                                 )
        self.debug("Successfully verified the deployment and state of Router - %s in VSD" % router.name)

    # verify_vsd_lb_device - Verifies the given LB device deployment and state in VSD
    def verify_vsd_lb_device(self, lb_device, stopped=None):
        self.debug("Verifying the deployment and state of LB device - %s in VSD" % lb_device.name)
        vsd_lb_device = self.vsd.get_vm(filter=self.get_externalID_filter(lb_device.id))
        self.assertNotEqual(vsd_lb_device, None,
                            "LB device data format in VSD should not be of type None"
                            )
        if not self.isSimulator:
            if stopped:
                self.assertEqual(vsd_lb_device.status, "DELETE_PENDING",
                                 "LB device state in VSD should be DELETE_PENDING"
                                 )
            else:
                self.assertEqual(vsd_lb_device.status, lb_device.state.upper(),
                                 "LB device state in VSD should match its state in CloudStack"
                                 )
        self.debug("Successfully verified the deployment and state of LB device - %s in VSD" % lb_device.name)

    # verify_vsd_floating_ip -  Verifies the Static NAT rule on the given public IP of the given VM in the given network
    # against the corresponding installed Floating IP in VSD
    def verify_vsd_floating_ip(self, network, vm, public_ipaddress, vpc=None):
        self.debug("Verifying the assignment and state of public IP address - %s in VSD" % public_ipaddress.ipaddress)
        ext_fip_filter = self.get_externalID_filter(vpc.id + ":" + public_ipaddress.id) if vpc else \
            self.get_externalID_filter(network.id + ":" + public_ipaddress.id)
        vsd_fip = self.vsd.get_floating_ip(filter=ext_fip_filter)
        self.assertEqual(vsd_fip.address, public_ipaddress.ipaddress,
                         "Floating IP address in VSD should match acquired public IP address in CloudStack"
                         )
        self.assertEqual(vsd_fip.assigned, True,
                         "Floating IP in VSD should be assigned"
                         )
        ext_network_filter = self.get_externalID_filter(vpc.id) if vpc else self.get_externalID_filter(network.id)
        vsd_domain = self.vsd.get_domain(filter=ext_network_filter)
        self.assertEqual(vsd_domain.id, vsd_fip.parent_id,
                         "Floating IP in VSD should be associated with the correct VSD domain, which in turn should "
                         "correspond to the correct VPC (or) network in CloudStack"
                         )
        vsd_subnet = self.vsd.get_subnet(filter=self.get_externalID_filter(network.id))
        for nic in vm.nic:
            if nic.networkid == network.id:
                vsd_vport = self.vsd.get_vport(subnet=vsd_subnet, filter=self.get_externalID_filter(nic.id))
        self.assertEqual(vsd_vport.associated_floating_ip_id, vsd_fip.id,
                         "Floating IP in VSD should be associated to the correct VSD vport, which in turn should "
                         "correspond to the correct Static NAT rule enabled VM and network in CloudStack"
                         )
        self.debug("Successfully verified the assignment and state of public IP address - %s in VSD" %
                   public_ipaddress.ipaddress)

    # verify_vsd_firewall_rule - Verifies the given Network Firewall (Ingress/Egress ACL) rule against the corresponding
    # installed firewall rule in VSD
    def verify_vsd_firewall_rule(self, firewall_rule, traffic_type="Ingress"):
        self.debug("Verifying the creation and state of Network Firewall (Ingress/Egress ACL) rule with ID - %s in VSD"
                   % firewall_rule.id)
        ext_fw_rule_filter = self.get_externalID_filter(firewall_rule.id)
        vsd_fw_rule = self.vsd.get_egress_acl_entry(filter=ext_fw_rule_filter) if traffic_type is "Ingress" else \
            self.vsd.get_ingress_acl_entry(filter=ext_fw_rule_filter)
        self.assertEqual(vsd_fw_rule.policy_state, "LIVE",
                         "Ingress/Egress ACL rule's policy state in VSD should be LIVE"
                         )
        dest_port = str(firewall_rule.startport) + "-" + str(firewall_rule.endport)
        self.assertEqual(vsd_fw_rule.destination_port, dest_port,
                         "Ingress/Egress ACL rule's destination port in VSD should match corresponding rule's "
                         "destination port in CloudStack"
                         )
        vsd_protocol = int(vsd_fw_rule.protocol)
        protocol = "tcp"
        if vsd_protocol == 6:
            protocol = "tcp"
        elif vsd_protocol == 1:
            protocol = "icmp"
        elif vsd_protocol == 17:
            protocol = "udp"
        self.assertEqual(protocol, firewall_rule.protocol.lower(),
                         "Ingress/Egress ACL rule's protocol in VSD should match corresponding rule's protocol in "
                         "CloudStack"
                         )
        self.debug("Successfully verified the creation and state of Network Firewall (Ingress/Egress ACL) rule with ID "
                   "- %s in VSD" % firewall_rule.id)
