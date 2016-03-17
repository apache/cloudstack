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

""" Custom base class for NuageVsp SDN Plugin specific Marvin tests
"""
# Import Local Modules
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (NetworkServiceProvider,
                             ServiceOffering,
                             NetworkOffering,
                             Network,
                             Router,
                             Nuage,
                             VPC,
                             VpcOffering,
                             PublicIPAddress,
                             VirtualMachine,
                             StaticNATRule,
                             NetworkACLList,
                             NetworkACL,
                             FireWallRule,
                             EgressFireWallRule,
                             Host)
from marvin.lib.common import (get_zone,
                               get_domain,
                               get_template,
                               list_templates,
                               wait_for_cleanup)
from marvin.lib.utils import cleanup_resources
from marvin.cloudstackAPI import (listPhysicalNetworks,
                                  updateConfiguration,
                                  updateTemplate,
                                  listConfigurations,
                                  listHypervisors,
                                  stopRouter,
                                  startRouter)
# Import System Modules
import socket
import importlib
import logging


class nuageTestCase(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.debug("setUpClass nuageTestCase")

        # We want to fail quicker, if it's a failure
        socket.setdefaulttimeout(60)

        test_client = super(nuageTestCase, cls).getClsTestClient()
        cls.api_client = test_client.getApiClient()
        cls.db_client = test_client.getDbConnection()
        cls.test_data = test_client.getParsedTestDataConfig()

        # Get Zone, Domain and templates
        cls.zone = get_zone(cls.api_client)
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

        # Get configured Nuage Vsp device details
        try:
            resp = listPhysicalNetworks.listPhysicalNetworksCmd()
            resp.zoneid = cls.zone.id
            physical_networks = cls.api_client.listPhysicalNetworks(resp)
            for pn in physical_networks:
                if pn.isolationmethods == 'VSP':
                    cls.vsp_physical_network = pn
                    break
            cls.nuage_vsp_device = Nuage.list(cls.api_client,
                                              physicalnetworkid=cls.vsp_physical_network.id
                                              )[0]
            pns = cls.config.zones[0].physical_networks
            providers = filter(lambda physical_network: 'VSP' in physical_network.isolationmethods, pns)[0].providers
            devices = filter(lambda provider: provider.name == 'NuageVsp', providers)[0].devices
            cls.nuage_vsp_device.username = devices[0].username
            cls.nuage_vsp_device.password = devices[0].password
            listConfigurationsCmd = listConfigurations.listConfigurationsCmd()
            listConfigurationsCmd.name = "nuagevsp.cms.id"
            listConfigurationsCmd.scopename = "global"
            cs_config_dict = cls.api_client.listConfigurations(listConfigurationsCmd)
            cls.cms_id = str(cs_config_dict[0].value).split(":")[1]
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Warning: Couldn't get configured Nuage Vsp device details: %s" % e)

        # Check if the host hypervisor type is simulator
        resp = listHypervisors.listHypervisorsCmd()
        resp.zoneid = cls.zone.id
        cls.isSimulator = cls.api_client.listHypervisors(resp)[0].name == 'Simulator'

        # VSD is a Python SDK for Nuage Vsp
        try:
            vspk_module = "vspk." + cls.nuage_vsp_device.apiversion if int(cls.nuage_vsp_device.apiversion[1]) >= 4 \
                else "vspk.vsdk." + cls.nuage_vsp_device.apiversion
            vsdk = importlib.import_module(vspk_module)
            vspk_utils_module = "vspk.utils" if int(cls.nuage_vsp_device.apiversion[1]) >= 4 \
                else "vspk.vsdk." + cls.nuage_vsp_device.apiversion + ".utils"
            vsdk_utils = importlib.import_module(vspk_utils_module)
            set_log_level = getattr(vsdk_utils, "set_log_level")
            from cms_vspk_wrapper.cms_vspk_wrapper import Cms_vspk_wrapper
        except:
            raise unittest.SkipTest("vspk (and/or) cms_vspk_wrapper import failure")

        # Configure VSD session
        cls._session = vsdk.NUVSDSession(username=cls.nuage_vsp_device.username,
                                         password=cls.nuage_vsp_device.password,
                                         enterprise="csp", api_url="https://%s:%d" %
                                                                   (cls.nuage_vsp_device.hostname,
                                                                    cls.nuage_vsp_device.port)
                                         )
        cls._session.start()

        # Configure cms_vspk_wrapper session
        cls.log_handler = logging.getLogger("CSLog").handlers[0]
        vsd_info = cls.nuage_vsp_device.__dict__
        vsd_info["port"] = str(vsd_info["port"])
        cls.vsd = Cms_vspk_wrapper(vsd_info, cls.log_handler)

        set_log_level(logging.INFO)

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
            cls.debug("Warning: Exception during cleanup : %s" % e)
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

    def getConfigurationValue(self, name, scope="global"):
        listConfigurationsCmd = listConfigurations.listConfigurationsCmd()
        listConfigurationsCmd.name = name
        listConfigurationsCmd.scopename = scope
        if scope is "zone":
            listConfigurationsCmd.zoneid = self.zone.id
        return self.api_client.listConfigurations(listConfigurationsCmd)

    def setConfigurationValue(self, name, value, scope="global"):
        cmd = updateConfiguration.updateConfigurationCmd()
        cmd.name = name
        cmd.scopename = scope
        if scope is "zone":
            cmd.zoneid = self.zone.id
        cmd.value = value
        self.api_client.updateConfiguration(cmd)

    def updateTemplate(self, value):
        self.debug("UPDATE TEMPLATE")
        cmd = updateTemplate.updateTemplateCmd()
        cmd.id = self.template.id
        cmd.passwordenabled = value
        self.api_client.updateTemplate(cmd)
        list_template_response = list_templates(self.api_client,
                                                templatefilter="all",
                                                id=self.template.id
                                                )
        self.template = list_template_response[0]

    # Creates the vpc offering
    def create_VpcOffering(self, vpc_offering, suffix=None):
        self.debug('Create VpcOffering')
        if suffix:
            vpc_offering["name"] = "VPC_OFF-" + str(suffix)
        vpc_off = VpcOffering.create(self.api_client,
                                     vpc_offering
                                     )
        # Enable VPC offering
        vpc_off.update(self.api_client, state='Enabled')
        self.cleanup.append(vpc_off)
        self.debug('Created and Enabled VpcOffering')
        return vpc_off

    # create_Vpc - Takes the vpc offering as arguments and creates the VPC
    def create_Vpc(self, vpc_offering, cidr='10.1.1.1/16', cleanup=True):
        self.debug("Creating a VPC network in the account: %s" % self.account.name)
        self.test_data["vpc"]["cidr"] = cidr
        vpc = VPC.create(
            self.api_client,
            self.test_data["vpc"],
            vpcofferingid=vpc_offering.id,
            zoneid=self.zone.id,
            account=self.account.name,
            domainid=self.account.domainid
        )
        self.debug("Created VPC with ID: %s" % vpc.id)
        if cleanup:
            self.cleanup.append(vpc)
        return vpc

    # create_NetworkOffering - Takes the network offering as argument and creates the Network Offering
    def create_NetworkOffering(self, net_offering, suffix=None, conserve_mode=False):
        self.debug('Create NetworkOffering')
        if suffix:
            net_offering["name"] = "NET_OFF-" + str(suffix)
        nw_off = NetworkOffering.create(self.api_client,
                                        net_offering,
                                        conservemode=conserve_mode
                                        )
        # Enable Network offering
        nw_off.update(self.api_client, state='Enabled')
        self.cleanup.append(nw_off)
        self.debug('Created and Enabled NetworkOffering')
        return nw_off

    # create_Network - Takes the network offering as argument and nw_key and creates the network
    def create_Network(self, nw_off, nw_key="network", gateway='10.1.1.1', netmask='255.255.255.0', vpc=None, acl_list=None):
        if not hasattr(nw_off, "id"):
            nw_off = self.create_NetworkOffering(nw_off)
        self.debug('Adding Network=%s' % self.test_data[nw_key])
        self.test_data[nw_key]["netmask"] = netmask
        obj_network = Network.create(self.api_client,
                                     self.test_data[nw_key],
                                     accountid=self.account.name,
                                     domainid=self.account.domainid,
                                     networkofferingid=nw_off.id,
                                     zoneid=self.zone.id,
                                     gateway=gateway,
                                     vpcid=vpc.id if vpc else self.vpc.id if hasattr(self, "vpc") else None,
                                     aclid=acl_list.id if acl_list else None
                                     )
        self.debug("Created network with ID: %s" % obj_network.id)
        self.cleanup.append(obj_network)
        return obj_network

    # upgrade_Network - Upgrades the given network
    def upgrade_Network(self, nw_off, network):
        if not hasattr(nw_off, "id"):
            nw_off = self.create_NetworkOffering(nw_off, network.gateway)
        self.debug('Update Network=%s' % network)
        network.update(
            self.api_client,
            networkofferingid=nw_off.id,
            changecidr=False
        )
        self.debug("Updated network with ID: %s" % network.id)

    # delete_Network - Deletes the given network
    def delete_Network(self, network):
        self.debug('Deleting Network - %s' % network.name)
        # Wait for network garbage collection before network deletion
        wait_for_cleanup(self.api_client,
                         ["network.gc.interval", "network.gc.wait"]
                         )
        network.delete(self.api_client)
        if network in self.cleanup:
            self.cleanup.remove(network)
        self.debug('Deleted Network - %s' % network.name)

    # create_VM_in_Network - Creates a VM in the given network, the vm_key - is the key for the services on the vm.
    def create_VM_in_Network(self, network,  vm_key="virtual_machine", host_id=None, start_vm=True):
        self.debug('Creating VM in network=%s' % network.name)
        self.debug('Passed vm_key=%s' % vm_key)
        self.test_data[vm_key]["zoneid"] = self.zone.id
        self.test_data[vm_key]["template"] = self.template.id
        vm = VirtualMachine.create(
            self.api_client,
            self.test_data[vm_key],
            accountid=self.account.name,
            domainid=self.account.domainid,
            serviceofferingid=self.service_offering.id,
            networkids=[str(network.id)],
            startvm=start_vm,
            hostid=host_id
        )
        self.debug('Created VM=%s in network=%s' % (vm.id, network.name))
        self.cleanup.append(vm)
        return vm

    # delete_VM - Deletes the given VM
    def delete_VM(self, vm):
        self.debug('Deleting VM - %s' % vm.name)
        vm.delete(self.api_client)
        # Wait for expunge interval to cleanup VM
        wait_for_cleanup(self.api_client,
                         ["expunge.delay", "expunge.interval"]
                         )
        if vm in self.cleanup:
            self.cleanup.remove(vm)
        self.debug('Deleted VM - %s' % vm.name)

    # acquire_Public_IP - Acquires a public IP for the given network
    def acquire_Public_IP(self, network, vpc=None):
        self.debug("Associating public IP for network: %s" % network.name)
        public_ip = PublicIPAddress.create(self.api_client,
                                           accountid=self.account.name,
                                           zoneid=self.zone.id,
                                           domainid=self.account.domainid,
                                           networkid=network.id if vpc is None else None,
                                           vpcid=vpc.id if vpc else self.vpc.id if hasattr(self, "vpc") else None
                                           )
        self.debug("Associated %s with network %s" % (public_ip.ipaddress.ipaddress,
                                                      network.id))
        return public_ip

    # create_StaticNatRule_For_VM - Creates static NAT rule for the given network , VM on the given public ip
    def create_StaticNatRule_For_VM(self, vm, public_ip, network, vmguestip=None):
        self.debug("Enabling static NAT for IP: %s" %
                   public_ip.ipaddress.ipaddress)
        StaticNATRule.enable(
            self.api_client,
            ipaddressid=public_ip.ipaddress.id,
            virtualmachineid=vm.id,
            networkid=network.id,
            vmguestip=vmguestip
        )
        self.debug("Static NAT enabled for IP: %s" %
                   public_ip.ipaddress.ipaddress)

    # delete_StaticNatRule_For_VM - Deletes the static NAT rule for the given VM
    def delete_StaticNatRule_For_VM(self, vm, public_ip):
        self.debug("Disabling static NAT for IP: %s" %
                   public_ip.ipaddress.ipaddress)
        StaticNATRule.disable(
            self.api_client,
            ipaddressid=public_ip.ipaddress.id,
            virtualmachineid=vm.id
        )
        self.debug("Static NAT disabled for IP: %s" %
                   public_ip.ipaddress.ipaddress)

    # create_firewall_rule - Creates the Ingress firewall rule on the given public ip
    def create_firewall_rule(self, public_ip, rule=None):
        if not rule:
            rule = self.test_data["ingress_rule"]
        self.debug("Adding an Ingress Firewall rule to make Guest VMs accessible through Static NAT")
        return FireWallRule.create(self.api_client,
                                   ipaddressid=public_ip.ipaddress.id,
                                   protocol=rule["protocol"],
                                   cidrlist=rule["cidrlist"],
                                   startport=rule["startport"],
                                   endport=rule["endport"]
                                   )

    # create_egress_firewall_rule - Creates the Egress firewall rule on the given public ip
    def create_egress_firewall_rule(self, network, rule):
        self.debug("Adding an Egress Firewall rule to allow/deny outgoing traffic from Guest VMs")
        return EgressFireWallRule.create(self.api_client,
                                         networkid=network.id,
                                         protocol=rule["protocol"],
                                         cidrlist=rule["cidrlist"],
                                         startport=rule["startport"],
                                         endport=rule["endport"]
                                         )

    # create_network_acl_list - Creates network ACL list in the given VPC
    def create_network_acl_list(self, name, description, vpc):
        self.debug("Adding NetworkACL list in VPC: %s" % vpc.id)
        return NetworkACLList.create(self.api_client,
                                     services={},
                                     name=name,
                                     description=description,
                                     vpcid=vpc.id
                                     )

    # create_network_acl_rule - Creates network ACL rule Ingree/Egress in the given network
    def create_network_acl_rule(self, rule, traffic_type="Ingress", network=None, acl_list=None):
        self.debug("Adding NetworkACL rule: %s" % rule)
        return NetworkACL.create(self.api_client,
                                 networkid=network.id if network else None,
                                 services=rule,
                                 traffictype=traffic_type,
                                 aclid=acl_list.id if acl_list else None
                                 )

    # migrate_vm - Migrates the VM to a different host if available
    def migrate_vm(self, vm):
        self.debug("Checking if a host is available for migration?")
        hosts = Host.listForMigration(self.api_client)
        self.assertEqual(isinstance(hosts, list), True,
                         "List hosts should return a valid list"
                         )
        # Remove the host of current VM from the hosts list
        hosts[:] = [host for host in hosts if host.id != vm.hostid]
        if len(hosts) <= 0:
            self.skipTest("No host available for migration. Test requires at-least 2 hosts")
        host = hosts[0]
        self.debug("Migrating VM-ID: %s to Host: %s" % (vm.id, host.id))
        try:
            vm.migrate(self.api_client, hostid=host.id)
        except Exception as e:
            self.fail("Failed to migrate instance, %s" % e)

    # get_network_router - returns the router for the given network
    def get_network_router(self, network):
        self.debug("Finding the virtual router for network: %s" % network.name)
        routers = Router.list(self.api_client,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(isinstance(routers, list), True,
                         "List routers should return a valid virtual router for network"
                         )
        return routers[0]

    # stop_network_router - Stops the given network router
    def stop_network_router(self, router):
        self.debug("Stopping Router with ID: %s" % router.id)
        cmd = stopRouter.stopRouterCmd()
        cmd.id = router.id
        self.api_client.stopRouter(cmd)

    # start_network_router - Starts the given network router
    def start_network_router(self, router):
        self.debug("Starting Router with ID: %s" % router.id)
        cmd = startRouter.startRouterCmd()
        cmd.id = router.id
        self.api_client.startRouter(cmd)

    # ssh_into_vm - Gets into the shell of the given VM
    def ssh_into_vm(self, vm, public_ip):
        self.debug("SSH into VM=%s on public_ip=%s" % (vm.name, public_ip.ipaddress.ipaddress))
        ssh_client = vm.get_ssh_client(ipaddress=public_ip.ipaddress.ipaddress)
        return ssh_client

    # execute_cmd - Executes the given command on the given ssh client
    def execute_cmd(self, ssh_client, cmd):
        self.debug("EXECUTE SSH COMMAND: " + cmd)
        ret_data = ""
        out_list = ssh_client.execute(cmd)
        if out_list is not None:
            ret_data = ' '.join(map(str, out_list)).strip()
            self.debug("ssh execute cmd result=" + ret_data)
        else:
            self.debug("ssh execute cmd result is None")
        return ret_data

    # wget_from_server - fetches the index.html file from the given public Ip
    def wget_from_server(self, public_ip):
        import urllib
        self.debug("wget from a http server on public_ip=%s" % public_ip.ipaddress.ipaddress)
        wget_file = urllib.urlretrieve("http://%s/index.html" % public_ip.ipaddress.ipaddress,
                                       filename="index.html"
                                       )
        return wget_file

    # validate_NetworkServiceProvider - Validates the Network Service Provider
    # in the Nuage VSP Physical Network - matches the given provider name
    # against the list of providers fetched
    def validate_NetworkServiceProvider(self, provider_name, state=None):
        """Validates the Network Service Provider in the Nuage VSP Physical Network"""
        self.debug("Check if the Network Service Provider is created successfully ?")
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
        self.debug("Network Service Provider creation successfully validated - %s" % provider_name)

    # validate_vpc_offering - Validates the VPC offering, matches the given VPC off name against the list of VPC offerings fetched
    def validate_vpc_offering(self, vpc_offering, state=None):
        """Validates the VPC offering"""
        self.debug("Check if the VPC offering is created successfully ?")
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
        self.debug("VPC offering creation successfully validated - %s" % vpc_offering.name)

    # validate_vpc - Validates the given VPC matches, the given VPC name against the list of VPCs fetched
    def validate_vpc(self, vpc, state=None):
        """Validates the VPC"""
        self.debug("Check if the VPC is created successfully ?")
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
        self.debug("VPC creation successfully validated - %s" % vpc.name)

    # validate_network_offering - Validates the given  Network offering
    # matches the given network offering name against the list of network
    # offerings fetched
    def validate_network_offering(self, net_offering, state=None):
        """Validates the Network offering"""
        self.debug("Check if the Network offering is created successfully ?")
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
        self.debug("Network offering creation successfully validated - %s" % net_offering.name)

    # validate_network - Validates the network - matches the given network name against the list of networks fetched
    def validate_network(self, network, state=None):
        """Validates the network"""
        self.debug("Check if the network is created successfully ?")
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
        self.debug("Network creation successfully validated - %s" % network.name)

    # check_router_state - Fetches the list of routers and their states and matches the given router's state
    def check_router_state(self, router, state=None):
        self.debug("Check if the virtual router is in state - %s" % state)
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
        self.debug("Virtual router is in the expected state - %s" % state)

    #   check_vm_state - Fetches the list of VMs and their states and matches the given VM's state
    def check_vm_state(self, vm, state=None):
        self.debug("Check if the VM instance is in state - %s" % state)
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
        self.debug("Virtual machine is in the expected state - %s" % state)

    # validate_Public_IP - Looks if the given public ip is in the allocated state form the list of fetched public IPs
    def validate_Public_IP(self, public_ip, network, static_nat=False, vm=None):
        """Validates the Public IP"""
        self.debug("Check if the Public IP is successfully assigned to the network ?")
        public_ips = PublicIPAddress.list(self.api_client,
                                          id=public_ip.ipaddress.id,
                                          networkid=network.id,
                                          isstaticnat=static_nat,
                                          listall=True
                                          )
        self.assertEqual(isinstance(public_ips, list), True,
                         "List public Ip for network should return a valid list"
                         )
        self.assertEqual(public_ips[0].ipaddress, public_ip.ipaddress.ipaddress,
                         "List public Ip for network should list the assigned public Ip address"
                         )
        self.assertEqual(public_ips[0].state, "Allocated",
                         "Assigned public Ip is not in the allocated state"
                         )
        if static_nat and vm:
            self.assertEqual(public_ips[0].virtualmachineid, vm.id,
                             "Static NAT Rule not enabled for the VM using the assigned public Ip"
                             )
        self.debug("Assigned Public IP is successfully validated - %s" % public_ip.ipaddress.ipaddress)

    # VSD verifications

    def fetch_by_external_id(self, fetcher, *cs_objects):
        """ Fetches a child object by external id using the given fetcher, and uuids of the given cloudstack objects.
        E.G.
          - fetch_by_external_id(vsdk.NUSubnet(id="954de425-b860-410b-be09-c560e7dbb474").vms, cs_vm)
          - fetch_by_external_id(session.user.floating_ips, cs_network, cs_public_ip)
        :param fetcher: VSPK Fetcher to use to find the child entity
        :param cs_objects: Cloudstack objects to take the UUID from.
        :return: the VSPK object having the correct externalID
        """
        return fetcher.get_first(filter="externalID BEGINSWITH '%s'" % ":".join([o.id for o in cs_objects]))

    # VSD verifications using cms_vspk_wrapper

    def get_externalID(self, object_id):
        return object_id + "@" + self.cms_id

    # verify_vsp_network - Fetches the vsd domain, vsd zone and vsd subnet and Verifies the given network/VPC values match the fetched values
    def verify_vsp_network(self, domain_id, network, vpc=None):
        vsd_enterprise = self.vsd.get_enterprise(name=domain_id)
        if vpc:
            ext_network_id = self.get_externalID(vpc.id)
        else:
            ext_network_id = self.get_externalID(network.id)
        ext_subnet_id = self.get_externalID(network.id)
        vsd_domain = self.vsd.get_domain(externalID=ext_network_id)
        vsd_zone = self.vsd.get_zone(externalID=ext_network_id)
        vsd_subnet = self.vsd.get_subnet(externalID=ext_subnet_id)
        self.debug("SHOW ENTERPRISE DATA FORMAT IN VSD")
        self.debug(vsd_enterprise)
        self.assertNotEqual(vsd_enterprise, None,
                            "VSD Enterprise data format should not be a None type"
                            )
        self.debug("SHOW NETWORK DATA FORMAT IN VSD")
        self.debug(vsd_domain)
        self.debug(vsd_zone)
        self.debug(vsd_subnet)
        if vpc:
            self.assertEqual(vsd_domain['description'], "VPC_" + vpc.name,
                             "VSD domain description should match VPC name in CloudStack"
                             )
            self.assertEqual(vsd_zone['description'], "VPC_" + vpc.name,
                             "VSD zone description should match VPC name in CloudStack"
                             )
        else:
            self.assertEqual(vsd_domain['description'], network.name,
                             "VSD domain description should match Isolated Network name in CloudStack"
                             )
            self.assertEqual(vsd_zone['description'], network.name,
                             "VSD zone description should match Isolated Network name in CloudStack"
                             )
        self.assertEqual(vsd_subnet['description'], network.name,
                         "VSD subnet description should match Isolated Network name in CloudStack"
                         )

    # verify_vsp_vm - Fetches the vsd vport, vsd vm and interface and Verifies the given VM values match the fetched values
    def verify_vsp_vm(self, vm, stopped=None):
        ext_vm_id = self.get_externalID(vm.id)
        for nic in vm.nic:
            ext_network_id = self.get_externalID(nic.networkid)
            ext_nic_id = self.get_externalID(nic.id)
            vsd_vport = self.vsd.get_vport(subnet_externalID=ext_network_id, vport_externalID=ext_nic_id)
            vsd_vm_interface = self.vsd.get_vm_interface(externalID=ext_nic_id)
            self.debug("SHOW VPORT and VM INTERFACE DATA FORMAT IN VSD")
            self.debug(vsd_vport)
            self.debug(vsd_vm_interface)
            self.assertEqual(vsd_vport['active'], True,
                             "VSD VM vport should be active"
                             )
            self.assertEqual(vsd_vm_interface['IPAddress'], nic.ipaddress,
                             "VSD VM interface IP address should match VM's NIC IP address in CloudStack"
                             )
        vsd_vm = self.vsd.get_vm(externalID=ext_vm_id)
        self.debug("SHOW VM DATA FORMAT IN VSD")
        self.debug(vsd_vm)
        if not self.isSimulator:
            if stopped:
                self.assertEqual(vsd_vm['status'], "DELETE_PENDING",
                                 "VM state in VSD should be DELETE_PENDING"
                                 )
            else:
                self.assertEqual(vsd_vm['status'], vm.state.upper(),
                                 "VM state in VSD should match its state in CloudStack"
                                 )
    # verify_vsp_router - Fetches the vsd router and Verifies the given router status match the fetched status

    def verify_vsp_router(self, router, stopped=None):
        ext_router_id = self.get_externalID(router.id)
        vsd_router = self.vsd.get_vm(externalID=ext_router_id)
        self.debug("SHOW VIRTUAL ROUTER DATA FORMAT IN VSD")
        self.debug(vsd_router)
        if not self.isSimulator:
            if stopped:
                self.assertEqual(vsd_router['status'], "DELETE_PENDING",
                                 "Router state in VSD should be DELETE_PENDING"
                                 )
            else:
                self.assertEqual(vsd_router['status'], router.state.upper(),
                                 "Router state in VSD should match its state in CloudStack"
                                 )

    # verify_vsp_floating_ip -  Verifies the floating IPs on the given public IP against the VSD FIP
    def verify_vsp_floating_ip(self, network, vm, public_ipaddress, vpc=None):
        ext_fip_id = self.get_externalID(network.id + ":" + public_ipaddress.id)
        vsd_fip = self.vsd.get_floating_ip(externalID=ext_fip_id)
        self.debug("SHOW FLOATING IP DATA FORMAT IN VSD")
        self.debug(vsd_fip)
        self.assertEqual(vsd_fip['address'], public_ipaddress.ipaddress,
                         "Floating IP address in VSD should match acquired Public IP address in CloudStack"
                         )
        if vpc:
            ext_network_id = self.get_externalID(vpc.id)
        else:
            ext_network_id = self.get_externalID(network.id)
        vsd_domain = self.vsd.get_domain(externalID=ext_network_id)
        self.debug("SHOW NETWORK DATA FORMAT IN VSD")
        self.debug(vsd_domain)
        self.assertEqual(vsd_domain['ID'], vsd_fip['parentID'],
                         "Floating IP in VSD should be associated with the correct VSD domain, "
                         "which in turn should correspond to the correct VPC (or) Isolated network in CloudStack"
                         )
        ext_subnet_id = self.get_externalID(network.id)
        vsd_subnet = self.vsd.get_subnet(externalID=ext_subnet_id)
        for nic in vm.nic:
            if nic.networkname == vsd_subnet['description']:
                ext_network_id = self.get_externalID(nic.networkid)
                ext_nic_id = self.get_externalID(nic.id)
                vsd_vport = self.vsd.get_vport(subnet_externalID=ext_network_id, vport_externalID=ext_nic_id)
                self.debug("SHOW VM VPORT DATA FORMAT IN VSD")
                self.debug(vsd_vport)
        self.assertEqual(vsd_vport['associatedFloatingIPID'], vsd_fip['ID'],
                         "Floating IP in VSD should be associated to the correct VSD vport, "
                         "which in turn should correspond to the correct Static NAT enabled VM "
                         "and Isolated Network in CloudStack"
                         )

    # verify_vsp_firewall_rule - Verifies the start port, destination port,
    # protocol of the given firewall rule Ingress/Egress against the VSD
    # firewall rule
    def verify_vsp_firewall_rule(self, firewall_rule, traffic_type="Ingress"):
        ext_fw_id = self.get_externalID(firewall_rule.id)
        if traffic_type is "Ingress":
            vsd_fw_rule = self.vsd.get_egress_acl_entry(externalID=ext_fw_id)
        else:
            vsd_fw_rule = self.vsd.get_ingress_acl_entry(externalID=ext_fw_id)
        self.debug("SHOW ACL ENTRY IN VSD")
        self.debug(vsd_fw_rule)
        dest_port = str(firewall_rule.startport) + "-" + str(firewall_rule.endport)
        self.assertEqual(vsd_fw_rule['destinationPort'], dest_port,
                         "Destination Port in VSD should match Destination Port in CloudStack"
                         )
        vsd_protocol = str(vsd_fw_rule['protocol'])
        self.debug("vsd protocol " + vsd_protocol)
        protocol = "tcp"
        if vsd_protocol == 6:
            protocol = "tcp"
        elif vsd_protocol == 1:
            protocol = "icmp"
        elif vsd_protocol == 17:
            protocol = "udp"
        self.assertEqual(protocol, firewall_rule.protocol.lower(),
                         "Protocol in VSD should match Protocol in CloudStack"
                         )
