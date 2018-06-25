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
from bambou.nurest_object import NURESTObject
from marvin.cloudstackTestCase import cloudstackTestCase, unittest
from marvin.lib.base import (Domain,
                             EgressFireWallRule,
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
                             ResourceDetails,
                             VirtualMachine,
                             VPC,
                             VpcOffering,
                             Zone)
from marvin.lib.common import (get_domain,
                               get_template,
                               get_zone)
from marvin.cloudstackAPI import (restartVPC,
                                  enableNuageUnderlayVlanIpRange,
                                  disableNuageUnderlayVlanIpRange,
                                  listNuageUnderlayVlanIpRanges)

from nuage_test_data import nuage_test_data
from nuage_vsp_statistics import VsdDataCollector

# Import System Modules
from retry import retry
import importlib
import functools
import logging
import socket
import time
import sys


class needscleanup(object):
    """
    Decorator to add the returned object automatically to the cleanup list.
    """
    def __init__(self, method):
        self.method = method

    def __get__(self, obj=None, objtype=None):
        @functools.wraps(self.method)
        def _wrapper(*args, **kwargs):
            wants_cleanup = True
            if "cleanup" in kwargs:
                wants_cleanup = kwargs["cleanup"]
                del kwargs["cleanup"]
            if obj is not None:
                result = self.method(obj, *args, **kwargs)
                if hasattr(obj, 'cleanup'):
                    cleanup = obj.cleanup
            else:
                result = self.method(objtype, *args, **kwargs)
                cleanup = objtype._cleanup
            if wants_cleanup:
                cleanup.append(result)
            return result
        return _wrapper


class gherkin(object):
    """Decorator to mark a method as Gherkin style.
       Add extra colored logging
    """
    BLACK = "\033[0;30m"
    BLUE = "\033[0;34m"
    GREEN = "\033[0;32m"
    CYAN = "\033[0;36m"
    RED = "\033[0;31m"
    BOLDBLUE = "\033[1;34m"
    NORMAL = "\033[0m"

    def __init__(self, method):
        self.method = method

    def __get__(self, obj=None, objtype=None):
        @functools.wraps(self.method)
        def _wrapper(*args, **kwargs):
            gherkin_step = self.method.__name__.replace("_", " ").capitalize()
            obj.info("=G= %s%s%s" % (self.BOLDBLUE, gherkin_step, self.NORMAL))
            try:
                result = self.method(obj, *args, **kwargs)
                obj.info("=G= %s%s: [SUCCESS]%s" %
                         (self.GREEN, gherkin_step, self.NORMAL))
                return result
            except Exception as e:
                obj.info("=G= %s%s: [FAILED]%s%s" %
                         (self.RED, gherkin_step, self.NORMAL, e))
                raise
        return _wrapper


class nuageTestCase(cloudstackTestCase):

    @classmethod
    def setUpClass(cls):
        cls.debug("setUpClass nuageTestCase")
        cls._cleanup = []

        # We want to fail quicker, if it's a failure
        socket.setdefaulttimeout(60)

        # Get test client and test data
        cls.test_client = super(nuageTestCase, cls).getClsTestClient()
        cls.api_client = cls.test_client.getApiClient()
        cls.db_client = cls.test_client.getDbConnection()
        cls.test_data = cls.test_client.getParsedTestDataConfig()
        cls.test_data.update(nuage_test_data)

        # Get Zones and Domains
        cls.zones = Zone.list(cls.api_client)
        cls.domains = Domain.list(cls.api_client, listall=True)
        cls.domain = get_domain(cls.api_client)
        cls.root_domain = get_domain(cls.api_client, domain_name="ROOT")

        # Get Zone details
        cls.getZoneDetails()

        # Configure VSD sessions
        cls.configureVSDSessions()

        # Create service offering
        cls.service_offering = ServiceOffering.create(
            cls.api_client,
            cls.test_data["service_offering"]
        )
        cls._cleanup.append(cls.service_offering)
        cls.debug("setUpClass nuageTestCase [DONE]")

    @classmethod
    def getZoneDetails(cls, zone=None):
        """Get Zone details"""
        cls.zone = zone if zone else get_zone(
            cls.api_client,
            zone_name=cls.test_client.getZoneForTests()
        )
        cls.template = get_template(cls.api_client,
                                    cls.zone.id,
                                    cls.test_data["ostype"]
                                    )
        cls.test_data["virtual_machine"]["zoneid"] = cls.zone.id
        cls.test_data["virtual_machine"]["template"] = cls.template.id

        # Check if the host hypervisor type is simulator
        hypervisors = Hypervisor.list(cls.api_client, zoneid=cls.zone.id)
        assert hypervisors is not None and len(hypervisors) > 0, \
            "Expected at least one hypervisor"
        cls.isSimulator = any(map(lambda h: h.name == "Simulator",
                                  hypervisors))

        # Get configured Nuage VSP device details
        try:
            physical_networks = PhysicalNetwork.list(
                cls.api_client,
                zoneid=cls.zone.id
            )

            cls.vsp_physical_network = next(pn for pn in physical_networks
                                            if pn.isolationmethods == "VSP")
            cls.nuage_vsp_device = Nuage.list(
                cls.api_client,
                physicalnetworkid=cls.vsp_physical_network.id)[0]

            # Get username and password from the Marvin config file,
            # as they are not returned by the API.
            config_nuage_device = next(device for zone in cls.config.zones
                                       if zone.name == cls.zone.name
                                       for physnet in zone.physical_networks
                                       if "VSP" in physnet.isolationmethods
                                       for provider in physnet.providers
                                       if provider.name == "NuageVsp"
                                       for device in provider.devices)

            cls.nuage_vsp_device.username = config_nuage_device.username
            cls.nuage_vsp_device.password = config_nuage_device.password
            cls.cms_id = cls.nuage_vsp_device.cmsid
        except Exception as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Warning: Could not get configured "
                                    "Nuage VSP device details - %s" % e)

        # Get data center Internet connectivity information from the Marvin
        # config file, which is required to perform Internet connectivity and
        # traffic tests from the guest VMs.
        cls.dcInternetConnectivityInfo()

        # Check if the configured Nuage VSP SDN platform infrastructure
        # supports underlay networking
        cmd = listNuageUnderlayVlanIpRanges.listNuageUnderlayVlanIpRangesCmd()
        cmd.zoneid = cls.zone.id
        cmd.underlay = True
        cls.isNuageInfraUnderlay = isinstance(
            cls.api_client.listNuageUnderlayVlanIpRanges(cmd), list)
        return

    @classmethod
    def dcInternetConnectivityInfo(cls):
        cls.isInternetConnectivityAvailable = False
        cls.http_proxy = None
        cls.https_proxy = None
        zone = next(zone for zone in cls.config.zones
                    if zone.name == cls.zone.name)

        cls.isInternetConnectivityAvailable = \
            hasattr(zone, "dcInternetConnectivityInfo") \
            and hasattr(zone.dcInternetConnectivityInfo, "available") \
            and zone.dcInternetConnectivityInfo.available == "true"

        if cls.isInternetConnectivityAvailable:
            if zone.dcInternetConnectivityInfo.httpProxy:
                cls.http_proxy = zone.dcInternetConnectivityInfo.httpProxy
            if zone.dcInternetConnectivityInfo.httpsProxy:
                cls.https_proxy = zone.dcInternetConnectivityInfo.httpsProxy

    @classmethod
    def configureVSDSessions(cls):
        # VSD is a programmable policy and analytics engine of Nuage VSP SDN
        # platform; vspk is a Python SDK for Nuage VSP's VSD; libVSD is a
        # library that wraps vspk package
        try:
            vspk_module = "vspk." + cls.nuage_vsp_device.apiversion
            cls.vsdk = importlib.import_module(vspk_module)
        except ImportError as e:
            try:
                vspk_module = "vspk.vsdk." + cls.nuage_vsp_device.apiversion
                cls.vsdk = importlib.import_module(vspk_module)
            except ImportError:
                cls.tearDownClass()
                raise unittest.SkipTest("Warning: vspk import failure"
                                        " - %s" % e)

        try:
            from libVSD import ApiClient, VSDHelpers
        except ImportError as e:
            cls.tearDownClass()
            raise unittest.SkipTest("Warning: libVSD package import failure "
                                    "- %s" % e)

        # Configure VSD session
        cls._session = cls.vsdk.NUVSDSession(
            username=cls.nuage_vsp_device.username,
            password=cls.nuage_vsp_device.password,
            enterprise="csp",
            api_url="https://%s:%d" % (cls.nuage_vsp_device.hostname,
                                       cls.nuage_vsp_device.port)
            )
        cls._session.start()

        # Configure libVSD session
        root = logging.getLogger()
        log_handler = logging.StreamHandler(sys.stdout)
        formatter = logging.Formatter('%(asctime)s - %(name)s - '
                                      '%(levelname)s - %(message)s')
        log_handler.setFormatter(formatter)
        root.addHandler(log_handler)
        vsd_info = cls.nuage_vsp_device.__dict__

        cls.debug("Nuage VSP device (VSD) details - %s" % vsd_info)
        vsd_api_client = ApiClient(
            address=cls.nuage_vsp_device.hostname,
            user=cls.nuage_vsp_device.username,
            password=cls.nuage_vsp_device.password,
            version=cls.nuage_vsp_device.apiversion[1] + "." + cls.nuage_vsp_device.apiversion[3]
        )
        vsd_api_client.new_session()
        cls.vsd = VSDHelpers(vsd_api_client)
        return

    def setUp(self):
        self.cleanup = []
        return

    @classmethod
    def tearDownClass(cls):
        # Cleanup resources used
        cls.debug("Cleaning up the class resources")
        for obj in reversed(cls._cleanup):
            try:
                if isinstance(obj, VirtualMachine):
                    obj.delete(cls.api_client, expunge=True)
                else:
                    obj.delete(cls.api_client)
            except Exception as e:
                cls.error("Failed to cleanup %s, got %s" % (obj, e))
        # cleanup_resources(cls.api_client, cls._cleanup)
        cls._cleanup = []
        cls.debug("Cleanup class complete!")
        return

    def tearDown(self):
        # Cleanup resources used
        self.debug("Cleaning up the test resources")
        for obj in reversed(self.cleanup):
            try:
                if isinstance(obj, VirtualMachine):
                    obj.delete(self.api_client, expunge=True)
                elif isinstance(obj, NURESTObject):
                    obj.delete()
                else:
                    obj.delete(self.api_client)
            except Exception as e:
                self.error("Failed to cleanup %s, got %s" % (obj, e))
        # cleanup_resources(self.api_client, self.cleanup)
        self.cleanup = []
        self.debug("Cleanup test complete!")
        return

    def enable_NuageUnderlayPublicIpRange(self, vlanid):
        """Enables/configures underlay networking
           for the given public IP range in Nuage VSP

        :param vlanid: Vlan ID
        :type vlanid: marvin.lib.base.PublicIpRange.vlan
        """
        cmd = enableNuageUnderlayVlanIpRange. \
            enableNuageUnderlayVlanIpRangeCmd()
        cmd.id = vlanid
        self.api_client.enableNuageUnderlayVlanIpRange(cmd)

    def disable_NuageUnderlayPublicIpRange(self, public_ip_range):
        """Disables underlay networking
           for the given public IP range in Nuage VSP

        :param public_ip_range: Public IP range
        :type public_ip_range: marvin.lib.base.PublicIpRange
        """
        cmd = disableNuageUnderlayVlanIpRange. \
            disableNuageUnderlayVlanIpRangeCmd()
        cmd.id = public_ip_range.vlan.id
        self.api_client.enableNuageUnderlayVlanIpRange(cmd)

    # list_NuageUnderlayPublicIpRanges - Lists underlay networking
    # enabled/configured public IP ranges in Nuage VSP
    def list_NuageUnderlayPublicIpRanges(self, public_ip_range=None):
        """Lists Vlan IP ranges that have the nuage underlay flag set to True

        :param public_ip_range: Optionally filter by Public IP range
        :type public_ip_range: marvin.lib.base.PublicIpRange
        """
        cmd = listNuageUnderlayVlanIpRanges.listNuageUnderlayVlanIpRangesCmd()
        if public_ip_range:
            cmd.id = public_ip_range.vlan.id
        cmd.underlay = True
        return self.api_client.listNuageUnderlayVlanIpRanges(cmd)

    # create_VpcOffering - Creates VPC offering
    @needscleanup
    def create_VpcOffering(cls, vpc_offering, suffix=None):
        """Creates VPC offering"""
        cls.debug("Creating VPC offering")
        if suffix:
            vpc_offering["name"] = "VPC_OFF-" + str(suffix)
        vpc_off = VpcOffering.create(cls.api_client,
                                     vpc_offering
                                     )
        # Enable VPC offering
        vpc_off.update(cls.api_client, state="Enabled")
        cls.debug("Created and Enabled VPC offering")
        return vpc_off

    # create_Vpc - Creates VPC with the given VPC offering
    @needscleanup
    def create_Vpc(cls, vpc_offering, cidr='10.1.0.0/16', testdata=None,
                   account=None, networkDomain=None):
        """Creates VPC with the given VPC offering
        :param vpc_offering: vpc offering
        :type vpc_offering: VpcOffering
        :param cidr: CIDR
        :param testdata: vpc details
        :param account: Account which will be the owner.
        :param networkDomain:
        :return: created VPC
        :rtype: VPC
        """
        if not account:
            account = cls.account
        cls.debug("Creating a VPC in the account - %s" % account.name)
        if not testdata:
            testdata = cls.test_data["vpc"]
            testdata["name"] = "TestVPC-" + cidr + "-" + str(vpc_offering.name)
            testdata["displaytext"] = "Test VPC"
            testdata["cidr"] = cidr
        vpc = VPC.create(cls.api_client,
                         testdata,
                         vpcofferingid=vpc_offering.id,
                         zoneid=cls.zone.id,
                         account=account.name,
                         domainid=account.domainid,
                         networkDomain=networkDomain
                         )
        cls.debug("Created VPC with ID - %s" % vpc.id)
        return vpc

    # restart_Vpc - Restarts the given VPC with/without cleanup
    def restart_Vpc(self, vpc, cleanup=False):
        """Restarts the given VPC with/without cleanup
        :param vpc: vpc to restart
        :type vpc: VPC
        :param cleanup: whether to restart with cleanup
        :type cleanup: bool
        """
        self.debug("Restarting VPC with ID - %s" % vpc.id)
        cmd = restartVPC.restartVPCCmd()
        cmd.id = vpc.id
        cmd.cleanup = cleanup
        cmd.makeredundant = False
        self.api_client.restartVPC(cmd)
        self.debug("Restarted VPC with ID - %s" % vpc.id)

    # create_NetworkOffering - Creates Network offering
    @needscleanup
    def create_NetworkOffering(cls, net_offering, suffix=None,
                               conserve_mode=False):
        """Creates a Network Offering
        :param net_offering: offering details
        :type net_offering: object
        :param suffix: string to append to the offering name
        :param conserve_mode:
        :return: created Network Offering
        :rtype: NetworkOffering
        """
        cls.debug("Creating Network offering")
        if suffix:
            net_offering["name"] = "NET_OFF-" + str(suffix)
        nw_off = NetworkOffering.create(cls.api_client,
                                        net_offering,
                                        conservemode=conserve_mode
                                        )
        # Enable Network offering
        nw_off.update(cls.api_client, state="Enabled")
        cls.debug("Created and Enabled Network offering")
        return nw_off

    # create_Network - Creates network with the given Network offering
    @needscleanup
    def create_Network(cls, nw_off, gateway="10.1.1.1",
                       netmask="255.255.255.0", vpc=None, acl_list=None,
                       testdata=None, account=None, vlan=None, externalid=None):
        """Creates Network with the given Network offering
        :param nw_off: Network offering
        :type nw_off: NetworkOffering
        :param gateway: gateway
        :param netmask: netmask
        :param vpc: in case of a VPC tier, the parent VPC
        :type vpc: VPC
        :param acl_list: in case of a VPC tier, the acl list
        :type acl_list: NetworkACLList
        :param testdata: Network details
        :param account: Account which will be the owner.
        :param vlan: vlan id
        :param externalid: external id, in case of VSD managed networks

        :return: created Network
        :rtype: Network
        """
        if not account:
            account = cls.account
        cls.debug("Creating a network in the account - %s" % account.name)
        if not testdata:
            testdata = cls.test_data["network"]
            testdata["name"] = "TestNet-" + gateway + "-" + str(nw_off.name)
            testdata["displaytext"] = "Test Network"
            testdata["gateway"] = gateway
            testdata["netmask"] = netmask
        network = Network.create(cls.api_client,
                                 testdata,
                                 accountid=account.name,
                                 domainid=account.domainid,
                                 networkofferingid=nw_off.id,
                                 zoneid=cls.zone.id,
                                 vlan=vlan,
                                 externalid=externalid,
                                 vpcid=vpc.id if vpc else cls.vpc.id
                                 if hasattr(cls, "vpc") else None,
                                 aclid=acl_list.id if acl_list else None
                                 )
        cls.debug("Created network with ID - %s" % network.id)
        return network

    # upgrade_Network - Upgrades the given network
    def upgrade_Network(self, nw_off, network, forced=True):
        if not hasattr(nw_off, "id"):
            nw_off = self.create_NetworkOffering(nw_off)
        self.debug("Updating Network with ID - %s" % network.id)
        network.update(self.api_client,
                       networkofferingid=nw_off.id,
                       changecidr=False,
                       forced=forced
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
    @needscleanup
    def create_VM(cls, network_list, host_id=None, start_vm=True,
                  testdata=None, account=None, keypair=None):
        network_ids = []
        if isinstance(network_list, list):
            for network in network_list:
                network_ids.append(str(network.id))
        else:
            network_ids.append(str(network_list.id))
        if not account:
            account = cls.account
        cls.debug("Creating VM in network(s) with ID(s) - %s in the "
                  "account - %s" % (network_ids, account.name))
        if not testdata:
            testdata = cls.test_data["virtual_machine"]
        vm = VirtualMachine.create(cls.api_client,
                                   testdata,
                                   accountid=account.name,
                                   domainid=account.domainid,
                                   serviceofferingid=cls.service_offering.id,
                                   templateid=cls.template.id,
                                   zoneid=cls.zone.id,
                                   networkids=network_ids,
                                   startvm=start_vm,
                                   hostid=host_id,
                                   keypair=keypair
                                   )
        cls.debug("Created VM with ID - %s in network(s) with ID(s) - %s"
                  % (vm.id, network_ids))
        return vm

    # nic_operation_VM - Performs NIC operations such as add, remove, and
    # update default NIC in the given VM and network
    def nic_operation_VM(self, vm, network, operation="add"):
        self.debug("Performing %s NIC operation in VM with ID - %s and "
                   "network with ID - %s" % (operation, vm.id, network.id))
        if operation is "add":
            vm.add_nic(self.api_client, network.id)
            self.debug("Added NIC in VM with ID - %s and network with ID - %s"
                       % (vm.id, network.id))
        vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
        for nic in vm_info.nic:
            if nic.networkid == network.id:
                nic_id = nic.id
        if operation is "update":
            vm.update_default_nic(self.api_client, nic_id)
            self.debug("Updated default NIC to NIC with ID - %s in VM with ID "
                       "- %s and network with ID - %s" %
                       (nic_id, vm.id, network.id))
        if operation is "remove":
            vm.remove_nic(self.api_client, nic_id)
            self.debug("Removed NIC with ID - %s in VM with ID - %s and "
                       "network with ID - %s" % (nic_id, vm.id, network.id))

    # migrate_VM - Migrates VM to another host, if available
    def migrate_VM(self, vm):
        self.debug("Checking if a host is available for migration...")
        hosts = Host.listForMigration(self.api_client, virtualmachineid=vm.id)
        if hosts:
            self.assertEqual(isinstance(hosts, list), True,
                             "List hosts should return a valid list"
                             )
            host = hosts[0]
            self.debug("Migrating VM with ID: "
                       "%s to Host: %s" % (vm.id, host.id))
            try:
                vm.migrate(self.api_client, hostid=host.id)
            except Exception as e:
                self.fail("Failed to migrate instance, %s" % e)
            self.debug("Migrated VM with ID: "
                       "%s to Host: %s" % (vm.id, host.id))
        else:
            self.debug("No host available for migration. "
                       "Test requires at-least 2 hosts")

    # delete_VM - Deletes the given VM
    def delete_VM(self, vm, expunge=True):
        self.debug("Deleting VM with ID - %s" % vm.id)
        vm.delete(self.api_client, expunge=expunge)
        if vm in self.cleanup:
            self.cleanup.remove(vm)
        self.debug("Deleted VM with ID - %s" % vm.id)

    # get_Router - Returns router for the given network
    def get_Router(self, network):
        self.debug("Finding the virtual router for network with ID - %s" %
                   network.id)
        routers = Router.list(self.api_client,
                              networkid=network.id,
                              listall=True
                              )
        self.assertEqual(isinstance(routers, list), True,
                         "List routers should return a valid virtual router "
                         "for network"
                         )
        return routers[0]

    # acquire_PublicIPAddress - Acquires public IP address for the given
    # network/VPC
    def acquire_PublicIPAddress(self, network, vpc=None, account=None):
        if not account:
            account = self.account
        self.debug("Associating public IP for network with ID - %s in the "
                   "account - %s" % (network.id, account.name))
        public_ip = PublicIPAddress.create(self.api_client,
                                           accountid=account.name,
                                           domainid=account.domainid,
                                           zoneid=self.zone.id,
                                           networkid=network.id
                                           if vpc is None else None,
                                           vpcid=vpc.id if vpc else self.vpc.id
                                           if hasattr(self, "vpc") else None
                                           )
        self.debug("Associated public IP address - %s with network with ID - "
                   "%s" % (public_ip.ipaddress.ipaddress, network.id))
        return public_ip

    # create_StaticNatRule_For_VM - Creates Static NAT rule on the given
    # public IP for the given VM in the given network
    def create_StaticNatRule_For_VM(self, vm, public_ip, network,
                                    vmguestip=None):
        self.debug("Enabling Static NAT rule on public IP - %s for VM with ID "
                   "- %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))
        static_nat_rule = StaticNATRule.enable(
            self.api_client,
            ipaddressid=public_ip.ipaddress.id,
            virtualmachineid=vm.id,
            networkid=network.id,
            vmguestip=vmguestip
        )
        self.debug("Static NAT rule enabled on public IP - %s for VM with ID "
                   "- %s in network with ID - %s" %
                   (public_ip.ipaddress.ipaddress, vm.id, network.id))
        return static_nat_rule

    # delete_StaticNatRule_For_VM - Deletes Static NAT rule on the given
    # public IP
    def delete_StaticNatRule_For_VM(self, public_ip):
        self.debug("Disabling Static NAT rule on public IP - %s" %
                   public_ip.ipaddress.ipaddress)
        StaticNATRule.disable(self.api_client,
                              ipaddressid=public_ip.ipaddress.id
                              )
        self.debug("Static NAT rule disabled on public IP - %s" %
                   public_ip.ipaddress.ipaddress)

    # create_FirewallRule - Creates (Ingress) Firewall rule on the given
    # Static NAT rule enabled public IP for Isolated networks
    def create_FirewallRule(self, public_ip, rule=None):
        if not rule:
            rule = self.test_data["ingress_rule"]
        self.debug("Adding an (Ingress) Firewall rule to make Guest VMs "
                   "accessible through Static NAT rule - %s" % rule)
        return FireWallRule.create(self.api_client,
                                   ipaddressid=public_ip.ipaddress.id,
                                   protocol=rule["protocol"],
                                   cidrlist=rule["cidrlist"],
                                   startport=rule["startport"],
                                   endport=rule["endport"]
                                   )

    # create_EgressFirewallRule - Creates Egress Firewall rule in the given
    # Isolated network
    def create_EgressFirewallRule(self, network, rule):
        self.debug("Adding an Egress Firewall rule to allow/deny outgoing "
                   "traffic from Guest VMs - %s" % rule)
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

    # create_NetworkAclRule - Creates Ingress/Egress Network ACL rule in the
    # given VPC network/acl list
    def create_NetworkAclRule(self, rule, traffic_type="Ingress", network=None,
                              acl_list=None):
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

    def ssh_into_VM(self, vm, public_ip, reconnect=True, negative_test=False, keypair=None):
        """Creates a SSH connection to the VM

        :returns: the SSH connection
        :rtype: marvin.sshClient.SshClient
        """
        if self.isSimulator:
            self.debug("Simulator Environment: Skipping ssh into VM")
            return

        self.debug("SSH into VM with ID - %s on public IP address - %s" %
                   (vm.id, public_ip.ipaddress.ipaddress))
        tries = 1 if negative_test else 3

        @retry(tries=tries)
        def retry_ssh():
            ssh_client = vm.get_ssh_client(
                ipaddress=public_ip.ipaddress.ipaddress,
                reconnect=reconnect,
                retries=3 if negative_test else 30,
                keyPairFileLocation=keypair.private_key_file if keypair else None
            )
            self.debug("Successful to SSH into VM with ID - %s on "
                       "public IP address - %s" %
                       (vm.id, public_ip.ipaddress.ipaddress))
            return ssh_client

        return retry_ssh()

    def execute_cmd(self, ssh_client, cmd):
        """Executes the given command on the given ssh client

        :param ssh_client: SSH session to the remote machine
        :type ssh_client: marvin.SshClient
        :param cmd: Command to run on the remote machine
        :type cmd: str
        :return: command output
        """
        self.debug("SSH client executing command - %s" % cmd)
        ret_data = ""
        out_list = ssh_client.execute(cmd)
        if out_list is not None:
            ret_data = ' '.join(map(str, out_list)).strip()
            self.debug("SSH client executed command result - %s" % ret_data)
        else:
            self.debug("SSH client executed command result is None")
        return ret_data


    def wget_from_server(self, public_ip, port=80, file_name="index.html",
                         disable_system_proxies=True):
        """Fetches file with the given file name from a web server

        :param public_ip: HTTP server IP
        :type public_ip: PublicIPAddress
        :param port: HTTP server port
        :param file_name: URL path
        :param disable_system_proxies: whether to bypass system proxy
        :return: filename, headers
        """
        import urllib
        if disable_system_proxies:
            urllib.getproxies = lambda: {}
        self.debug("wget file - %s from a http web server listening on "
                   "public IP address - %s and port - %s" %
                   (file_name, public_ip.ipaddress.ipaddress, port))
        filename, headers = urllib.urlretrieve(
            "http://%s:%s/%s" %
            (public_ip.ipaddress.ipaddress, port, file_name),
            filename=file_name
        )
        self.debug("Successful to wget file - %s from a http web server "
                   "listening on public IP address - %s and port - %s" %
                   (file_name, public_ip.ipaddress.ipaddress, port))
        return filename, headers

    def validate_NetworkServiceProvider(self, provider_name, state=None):
        """Validates the Network Service Provider in the Nuage VSP Physical
        Network.

        :param provider_name Provider name
        :param state Expected state
        :raises AssertionError when provider isn't found,
            or has an incorrect state.
        """
        self.debug("Validating the creation and state of Network Service "
                   "Provider - %s" % provider_name)
        providers = NetworkServiceProvider.list(
            self.api_client,
            name=provider_name,
            physicalnetworkid=self.vsp_physical_network.id
        )
        self.assertEqual(isinstance(providers, list), True,
                         "List Network Service Provider should return a "
                         "valid list"
                         )
        self.assertEqual(provider_name, providers[0].name,
                         "Name of the Network Service Provider should match "
                         "with the returned list data"
                         )
        if state:
            self.assertEqual(providers[0].state, state,
                             "Network Service Provider state should be '%s'" %
                             state
                             )
        self.debug("Successfully validated the creation and state of Network "
                   "Service Provider - %s" % provider_name)

    def validate_VpcOffering(self, vpc_offering, state=None):
        """Validates the VPC offering

        Fetches the Vpc offering by id,
        verifies that the name is correct,
        and if en expected state is given, verifies that it is correct.

        :param vpc_offering: cs object
        :type vpc_offering: VpcOffering
        :param state: optional state
        :raise AssertionError when VPC offering isn't found,
            or has an incorrect state.
        """
        self.debug("Validating the creation and state of VPC offering - %s" %
                   vpc_offering.name)
        vpc_offs = VpcOffering.list(self.api_client,
                                    id=vpc_offering.id
                                    )
        self.assertEqual(isinstance(vpc_offs, list), True,
                         "List VPC offering should return a valid list"
                         )
        self.assertEqual(vpc_offering.name, vpc_offs[0].name,
                         "Name of the VPC offering should match with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(vpc_offs[0].state, state,
                             "VPC offering state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of VPC "
                   "offering - %s" % vpc_offering.name)

    def validate_Vpc(self, vpc, state=None):
        """Validates the VPC

        Fetches the vpc by id,
        verifies that the name is correct,
        and if en expected state is given, verifies that it is correct.

        :param vpc: cs object
        :type vpc: Vpc
        :param state: optional state
        :raise AssertionError when vpc isn't found,
            or has an incorrect state."""
        self.debug("Validating the creation and state of VPC - %s" % vpc.name)
        vpcs = VPC.list(self.api_client,
                        id=vpc.id
                        )
        self.assertEqual(isinstance(vpcs, list), True,
                         "List VPC should return a valid list"
                         )
        self.assertEqual(vpc.name, vpcs[0].name,
                         "Name of the VPC should match with the returned "
                         "list data"
                         )
        if state:
            self.assertEqual(vpcs[0].state, state,
                             "VPC state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of VPC - %s"
                   % vpc.name)

    def validate_NetworkOffering(self, net_offering, state=None):
        """Validates the Network offering

        Fetches the Network offering by id,
        verifies that the name is correct,
        and if en expected state is given, verifies that it is correct.

        :param net_offering: cs object
        :type net_offering: NetworkOffering
        :param state: optional state
        :raise AssertionError when network offering isn't found,
            or has an incorrect state."""

        self.debug("Validating the creation and state of Network offering - %s"
                   % net_offering.name)
        net_offs = NetworkOffering.list(self.api_client,
                                        id=net_offering.id
                                        )
        self.assertEqual(isinstance(net_offs, list), True,
                         "List Network offering should return a valid list"
                         )
        self.assertEqual(net_offering.name, net_offs[0].name,
                         "Name of the Network offering should match with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(net_offs[0].state, state,
                             "Network offering state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of Network "
                   "offering - %s" % net_offering.name)

    def validate_Network(self, network, state=None):
        """Validates the network

        Fetches the Network by id,
        verifies that the name is correct,
        and if en expected state is given, verifies that it is correct.

        :param network: cs object
        :type network: Network
        :param state: optional state
        :raise AssertionError when network isn't found,
            or has an incorrect state."""
        self.debug("Validating the creation and state of Network - %s" %
                   network.name)
        networks = Network.list(self.api_client,
                                id=network.id
                                )
        self.assertEqual(isinstance(networks, list), True,
                         "List network should return a valid list"
                         )
        self.assertEqual(network.name, networks[0].name,
                         "Name of the network should match with with the "
                         "returned list data"
                         )
        if state:
            self.assertEqual(networks[0].state, state,
                             "Network state should be '%s'" % state
                             )
        self.debug("Successfully validated the creation and state of Network "
                   "- %s" % network.name)

    def check_VM_state(self, vm, state=None):
        """Validates the VM state
            :param vm: cs object
            :type vm: VirtualMachine
            :param state: optional state
            :raise AssertionError when vm isn't found,
                or has an incorrect state."""

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
        self.debug("Successfully validated the deployment and state of VM - %s"
                   % vm.name)

    def check_Router_state(self, router, state=None):
        """Validates the Router state
            :param router: cs object
            :type router: Router
            :param state: optional state
            :raise AssertionError when router isn't found,
                or has an incorrect state."""

        self.debug("Validating the deployment and state of Router - %s" %
                   router.name)
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
        self.debug("Successfully validated the deployment and state of Router "
                   "- %s" % router.name)

    def validate_PublicIPAddress(self, public_ip, network, static_nat=False,
                                 vm=None):
        """Validates the Public IP Address
            :param public_ip: cs object
            :type public_ip: PublicIPAddress
            :param network: cs object
            :type network: Network
            :param static_nat: optional state
            :type static_nat: bool
            :param vm: Virtual machine the public ip should be forwarding to.
            :type vm: VirtualMachine
            :raise AssertionError when Public IP isn't found, isn't Allocated
                or has an incorrect ip address."""

        self.debug("Validating the assignment and state of public IP address "
                   "- %s" % public_ip.ipaddress.ipaddress)
        public_ips = PublicIPAddress.list(self.api_client,
                                          id=public_ip.ipaddress.id,
                                          networkid=network.id,
                                          isstaticnat=static_nat,
                                          listall=True
                                          )
        self.assertEqual(isinstance(public_ips, list), True,
                         "List public IP for network should return a "
                         "valid list"
                         )
        self.assertEqual(public_ips[0].ipaddress,
                         public_ip.ipaddress.ipaddress,
                         "List public IP for network should list the assigned "
                         "public IP address"
                         )
        self.assertEqual(public_ips[0].state, "Allocated",
                         "Assigned public IP is not in the allocated state"
                         )
        if static_nat and vm:
            self.assertEqual(public_ips[0].virtualmachineid, vm.id,
                             "Static NAT rule is not enabled for the VM on "
                             "the assigned public IP"
                             )
        self.debug("Successfully validated the assignment and state of public "
                   "IP address - %s" % public_ip.ipaddress.ipaddress)

    def verify_VRWithoutPublicIPNIC(self, vr):
        """Verifies that the given Virtual Router has no public IP nor NIC
            :param vr: cs object
            :type vr: Router
            :raise AssertionError when router isn't found,
                has an incorrect name, has a public ip for source nat
                or has a nic in the public network."""

        self.debug("Verifies that there is no public IP and NIC in Virtual "
                   "Router - %s" % vr.name)
        self.assertEqual(vr.publicip, None,
                         "Virtual router has public IP"
                         )
        for nic in vr.nic:
            self.assertNotEqual(nic.traffictype, "Public",
                                "Virtual router has public NIC"
                                )
        self.debug("Successfully verified that there is no public IP and NIC "
                   "in Virtual Router - %s" % vr.name)

    def verify_vpc_has_no_src_nat(self, vpc, account=None):
        """Verifies that the given Vpc has no public IP nor NIC
            :param vpc: cs object
            :type vpc: VPC
            :raise AssertionError when the VPC has a public ip for source nat.
        """
        if not account:
            account = self.account
        self.debug("Verify that there is no src NAT ip address "
                   "allocated for the vpc")
        src_nat_ip = PublicIPAddress.list(
            self.api_client,
            vpcid=vpc.id,
            issourcenat=True,
            account=account.name)
        self.assertEqual(src_nat_ip, None, "VPC has a source NAT ip!")

    # VSD verifications; VSD is a programmable policy and analytics engine of
    # Nuage VSP SDN platform

    def get_externalID_filter(self, object_id):
        """Builds a VSD filter to search by external ID

        :param object_id: Cloudstack UUID
        :type object_id: str
        :rtype: str
        :return: filter
        """
        ext_id = object_id + "@" + self.cms_id
        return self.vsd.set_externalID_filter(ext_id)

    # fetch_by_externalID - Returns VSD object with the given external ID
    def fetch_by_externalID(self, fetcher, *cs_objects):
        """ Fetches a child object by external id using the given fetcher, and
        uuids of the given cloudstack objects.
        E.G.
          - fetch_by_external_id(vsdk.NUSubnet
          (id="954de425-b860-410b-be09-c560e7dbb474").vms, cs_vm)
          - fetch_by_external_id
          (session.user.floating_ips, cs_network, cs_public_ip)
        :param fetcher: VSPK Fetcher to use to find the child entity
        :param cs_objects: Cloudstack objects to take the UUID from.
        :return: the VSPK object having the correct externalID
        """
        object_id = ":".join([o.id for o in cs_objects])
        ext_id = object_id + "@" + self.cms_id
        return fetcher.get_first(filter="externalID is '%s'" % ext_id)

    def fetch_vsd_objects(self, domain_id, network, vpc=None):
        vsd_enterprise = self.vsd.get_enterprise(
            filter=self.get_externalID_filter(domain_id))

        ext_network_filter = self.get_externalID_filter(vpc.id) if vpc \
            else self.get_externalID_filter(network.id)
        vsd_domain = self.vsd.get_domain(filter=ext_network_filter)
        vsd_zone = self.vsd.get_zone(filter=ext_network_filter)
        vsd_subnet = self.vsd.get_subnet(
            filter=self.get_externalID_filter(network.id))

        return [
            ext_network_filter,
            vsd_enterprise,
            vsd_domain,
            vsd_zone,
            vsd_subnet
        ]

    # verify_vsd_network - Verifies the given CloudStack domain and network/VPC
    # against the corresponding installed enterprise, domain, zone, and subnet
    # in VSD
    def verify_vsd_network(self, domain_id, network, vpc=None,
                           domain_template_name=None):
        self.debug("Verifying the creation and state of Network - %s in VSD" %
                   network.name)

        [
            ext_network_filter,
            vsd_enterprise,
            vsd_domain,
            vsd_zone,
            vsd_subnet
        ] = self.fetch_vsd_objects(domain_id, network, vpc)

        self.assertEqual(vsd_enterprise.name, domain_id,
                         "VSD enterprise name should match CloudStack domain "
                         "uuid"
                         )
        if domain_template_name:
            vsd_domain_template = self.vsd.get_domain_template(
                enterprise=vsd_enterprise,
                filter=self.vsd.set_name_filter(domain_template_name))
        else:
            vsd_domain_template = self.vsd.get_domain_template(
                enterprise=vsd_enterprise,
                filter=ext_network_filter)
        self.assertEqual(vsd_domain.template_id, vsd_domain_template.id,
                         "VSD domain should be instantiated from appropriate "
                         "domain template"
                         )
        if vpc:
            self.assertEqual(vsd_domain.description, "VPC_" + vpc.name,
                             "VSD domain description should match VPC name in "
                             "CloudStack"
                             )
            self.assertEqual(vsd_zone.description, "VPC_" + vpc.name,
                             "VSD zone description should match VPC name in "
                             "CloudStack"
                             )
        else:
            self.assertEqual(vsd_domain.description, network.name,
                             "VSD domain description should match network "
                             "name in CloudStack"
                             )
            self.assertEqual(vsd_zone.description, network.name,
                             "VSD zone description should match network name "
                             "in CloudStack"
                             )
        self.assertEqual(vsd_subnet.description, network.name,
                         "VSD subnet description should match network name in "
                         "CloudStack"
                         )
        self.debug("Successfully verified the creation and state of Network "
                   "- %s in VSD" % network.name)

    def verify_vsd_network_not_present(self, network, vpc=None):
        self.debug("Verifying the creation and state of Network - %s in VSD" %
                   network.name)
        ext_network_filter = self.get_externalID_filter(vpc.id) if vpc \
            else self.get_externalID_filter(network.id)

        vsd_domain = self.vsd.get_domain(filter=ext_network_filter)
        if vsd_domain is None:
            return

        vsd_zone = self.vsd.get_zone(filter=ext_network_filter)
        if vsd_zone is None:
            return
        vsd_subnet = self.vsd.get_subnet(
            filter=self.get_externalID_filter(network.id))
        self.assertEqual(vsd_subnet, None, "Network is present on the vsd.")

    def get_subnet_id(self, network_id, gateway):
        """ Calculates the subnet ID in VSD with
            the given CloudStack network ID and subnet gateway

            :param gateway: Gateway
            :type gateway: str
            :type network_id: str
            :rtype: str
            :return: Expected Subnet UUID

        """
        try:
            import uuid

            class NULL_NAMESPACE:
                bytes = b''

            # The UUID of the shared network in ACS
            # The gateway IP of the address range
            network_id = str(network_id)
            bytes = bytearray(network_id)
            ipbytes = bytearray(gateway)
            subnet_id = uuid.uuid3(NULL_NAMESPACE, bytes + ipbytes)
            self.debug("The required subnet id is %s in VSD" % subnet_id)
            return str(subnet_id)
        except Exception as e:
            self.debug("Failed to get the subnet id due to %s" % e)
            self.fail("Unable to get the subnet id, failing the test case")


    def verify_vsd_shared_network(self, domain_id, network,
                                  gateway="10.1.1.1"):
        """Verifies the given CloudStack domain and
            shared network against the corresponding installed enterprise,
            domain, zone, subnet, and shared network resource in VSD"""
        self.debug("Verifying the creation and state of Shared Network - %s "
                   "in VSD" % network.name)
        vsd_enterprise = self.vsd.get_enterprise(
            filter=self.get_externalID_filter(domain_id))
        ext_network_id_filter = self.get_externalID_filter(network.id)
        vsd_domain = self.vsd.get_domain(filter=ext_network_id_filter)
        vsd_zone = self.vsd.get_zone(filter=ext_network_id_filter)
        subnet_id = self.get_subnet_id(network.id, gateway)
        vsd_subnet = self.vsd.get_subnet(
            filter=self.get_externalID_filter(subnet_id))

        self.assertNotEqual(vsd_enterprise, None,
                            "VSD enterprise (CloudStack domain) data format "
                            "should not be of type None"
                            )
        self.assertEqual(vsd_domain.description, network.name,
                         "VSD domain description should match network name in "
                         "CloudStack"
                         )
        self.assertEqual(vsd_zone.description, network.name,
                         "VSD zone description should match network name in "
                         "CloudStack"
                         )
        self.assertEqual(vsd_subnet.description, network.name,
                         "VSD subnet description '%s' should match network "
                         "name in CloudStack" % vsd_subnet.description
                         )
        shared_resource = self.vsd.get_shared_network_resource(
            filter=self.get_externalID_filter(subnet_id))
        self.assertEqual(shared_resource.description, network.name,
                         "VSD shared resources description should match "
                         "network name in CloudStack"
                         )
        self.debug("Successfully verified the creation and state of Shared "
                   "Network - %s in VSD" % network.name)

    def verify_vsd_object_status(self, cs_object, stopped):
        """ Verifies the VM status in VSD for a given Cloudstack VM,
        retrying every 5 seconds for 10 minutes.

        :param cs_object: Cloudstack VM
        :param stopped: boolean: specifying if the vm is stopped.
        """
        vsd_object = self.vsd.get_vm(
            filter=self.get_externalID_filter(cs_object.id))
        expected_status = cs_object.state.upper() if not stopped \
            else "DELETE_PENDING"
        tries = 0
        while (vsd_object.status != expected_status) and (tries < 120):
            self.debug("Waiting for the CloudStack object " + cs_object.name +
                       " to be fully resolved in VSD...")
            time.sleep(5)
            self.debug("Rechecking the CloudStack object " + cs_object.name +
                       " status in VSD...")
            vsd_object = self.vsd.get_vm(
                filter=self.get_externalID_filter(cs_object.id))
            tries += 1
        self.assertEqual(vsd_object.status, expected_status,
                         "Object " + cs_object.name +
                         " state in VSD should match its state in CloudStack"
                         )

    # verify_vsd_vm - Verifies the given CloudStack VM deployment and status in
    # VSD
    def verify_vsd_vm(self, vm, stopped=False):
        self.debug("Verifying the deployment and state of VM - %s in VSD" %
                   vm.name)
        vsd_vm = self.vsd.get_vm(filter=self.get_externalID_filter(vm.id))
        self.assertNotEqual(vsd_vm, None,
                            "VM data format in VSD should not be of type None"
                            )
        vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
        for nic in vm_info.nic:
            vsd_subnet = self.vsd.get_subnet(
                filter=self.get_externalID_filter(nic.networkid))
            vsd_vport = self.vsd.get_vport(
                subnet=vsd_subnet, filter=self.get_externalID_filter(nic.id))
            vsd_vm_interface = self.vsd.get_vm_interface(
                filter=self.get_externalID_filter(nic.id))
            self.assertEqual(vsd_vport.active, True,
                             "VSD VM vport should be active"
                             )
            self.assertEqual(vsd_vm_interface.ip_address, nic.ipaddress,
                             "VSD VM interface IP address should match VM's "
                             "NIC IP address in CloudStack"
                             )
        if not self.isSimulator:
            self.verify_vsd_object_status(vm, stopped)
        self.debug("Successfully verified the deployment and state of VM - %s "
                   "in VSD" % vm.name)

    # verify_vsd_enterprise_vm - Verifies the given CloudStack domain VM
    # deployment and status in the corresponding VSD enterprise
    def verify_vsd_enterprise_vm(self, domain_id, network, vm, vpc=None,
                                 stopped=False, sharedsubnetid=None):
        self.debug("Verifying the creation and state of Network - %s in VSD" %
                   network.name)
        vsd_enterprise = self.vsd.get_enterprise(
            filter=self.get_externalID_filter(domain_id))
        ext_network_id_filter = self.get_externalID_filter(vpc.id) if vpc \
            else self.get_externalID_filter(network.id)
        vsd_domain = self.vsd.get_domain(
            enterprise=vsd_enterprise, filter=ext_network_id_filter)
        vsd_zone = self.vsd.get_zone(
            domain=vsd_domain, filter=ext_network_id_filter)
        if sharedsubnetid:
            vsd_subnet = self.vsd.get_subnet(
                zone=vsd_zone,
                filter=self.get_externalID_filter(sharedsubnetid))
        else:
            vsd_subnet = self.vsd.get_subnet(
                zone=vsd_zone, filter=self.get_externalID_filter(network.id))
        self.assertNotEqual(vsd_enterprise, None,
                            "VSD enterprise (CloudStack domain) data format "
                            "should not be of type None"
                            )
        if vpc:
            self.assertEqual(vsd_domain.description, "VPC_" + vpc.name,
                             "VSD domain description should match VPC name in "
                             "CloudStack"
                             )
            self.assertEqual(vsd_zone.description, "VPC_" + vpc.name,
                             "VSD zone description should match VPC name in "
                             "CloudStack"
                             )
        else:
            self.assertEqual(vsd_domain.description, network.name,
                             "VSD domain description should match network "
                             "name in CloudStack"
                             )
            self.assertEqual(vsd_zone.description, network.name,
                             "VSD zone description should match network name "
                             "in CloudStack"
                             )
        self.assertEqual(vsd_subnet.description, network.name,
                         "VSD subnet description '%s' should match network "
                         "name in CloudStack" % vsd_subnet.description
                         )
        self.debug("Successfully verified the creation and state of Network - "
                   "%s in VSD" % network.name)
        self.debug("Verifying the deployment and state of VM - %s in VSD" %
                   vm.name)
        vsd_vm = self.vsd.get_vm(
            subnet=vsd_subnet, filter=self.get_externalID_filter(vm.id))
        self.assertNotEqual(vsd_vm, None,
                            "VM data format in VSD should not be of type None"
                            )
        vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
        for nic in vm_info.nic:
            if nic.networkid == network.id:
                vsd_vport = self.vsd.get_vport(
                    subnet=vsd_subnet,
                    filter=self.get_externalID_filter(nic.id))
                vsd_vm_interface = self.vsd.get_vm_interface(
                    filter=self.get_externalID_filter(nic.id))
                self.assertEqual(vsd_vport.active, True,
                                 "VSD VM vport should be active"
                                 )
                self.assertEqual(vsd_vm_interface.ip_address, nic.ipaddress,
                                 "VSD VM interface IP address should match "
                                 "VM's NIC IP address in CloudStack"
                                 )
        if not self.isSimulator:
            self.verify_vsd_object_status(vm, stopped)
        self.debug("Successfully verified the deployment and state of VM - %s "
                   "in VSD" % vm.name)

    # verify_vsd_router - Verifies the given CloudStack network router
    # deployment and status in VSD
    def verify_vsd_router(self, router, stopped=False):
        self.debug("Verifying the deployment and state of Router - %s in VSD" %
                   router.name)
        vsd_router = self.vsd.get_vm(
            filter=self.get_externalID_filter(router.id))
        self.assertNotEqual(vsd_router, None,
                            "Router data format in VSD should not be of type "
                            "None"
                            )
        if not self.isSimulator:
            self.verify_vsd_object_status(router, stopped)
        self.debug("Successfully verified the deployment and state of Router "
                   "- %s in VSD" % router.name)

    # verify_vsd_lb_device - Verifies the given CloudStack LB device deployment
    # and status in VSD
    def verify_vsd_lb_device(self, lb_device, stopped=False):
        self.debug("Verifying the deployment and state of LB device - %s in "
                   "VSD" % lb_device.name)
        vsd_lb_device = self.vsd.get_vm(
            filter=self.get_externalID_filter(lb_device.id))
        self.assertNotEqual(vsd_lb_device, None,
                            "LB device data format in VSD should not be of "
                            "type None"
                            )
        if not self.isSimulator:
            self.verify_vsd_object_status(lb_device, stopped)
        self.debug("Successfully verified the deployment and state of LB "
                   "device - %s in VSD" % lb_device.name)

    # verify_vsd_floating_ip -  Verifies the CloudStack Static NAT rule on the
    # given public IP of the given VM in the given network against the
    # corresponding installed Floating IP in VSD
    def verify_vsd_floating_ip(self, network, vm, public_ipaddress, vpc=None):
        self.debug("Verifying the assignment and state of public IP address "
                   "- %s in VSD" % public_ipaddress.ipaddress)
        ext_fip_filter = self.get_externalID_filter(vpc.id + ":" +
                                                    public_ipaddress.id) \
            if vpc else self.get_externalID_filter(network.id + ":" +
                                                   public_ipaddress.id)
        vsd_fip = self.vsd.get_floating_ip(filter=ext_fip_filter)
        self.assertEqual(vsd_fip.address, public_ipaddress.ipaddress,
                         "Floating IP address in VSD should match acquired "
                         "public IP address in CloudStack"
                         )
        self.assertEqual(vsd_fip.assigned, True,
                         "Floating IP in VSD should be assigned"
                         )
        ext_fip_subnet_filter = self.get_externalID_filter(
            public_ipaddress.vlanid)
        vsd_fip_subnet = self.vsd.get_shared_network_resource(
            filter=ext_fip_subnet_filter)

        if self.isNuageInfraUnderlay:
            self.assertEqual(vsd_fip_subnet.underlay, True,
                             "Floating IP subnet in VSD should be underlay "
                             "enabled"
                             )
        else:
            self.assertEqual(vsd_fip_subnet.underlay, False,
                             "Floating IP subnet in VSD should be underlay "
                             "disabled"
                             )

        ext_network_filter = self.get_externalID_filter(vpc.id) if vpc \
            else self.get_externalID_filter(network.id)
        vsd_domain = self.vsd.get_domain(filter=ext_network_filter)
        self.assertEqual(vsd_domain.id, vsd_fip.parent_id,
                         "Floating IP in VSD should be associated with the "
                         "correct VSD domain, which in turn should correspond "
                         "to the correct VPC (or) network in CloudStack"
                         )
        vsd_subnet = self.vsd.get_subnet(
            filter=self.get_externalID_filter(network.id))
        vm_info = VirtualMachine.list(self.api_client, id=vm.id)[0]
        for nic in vm_info.nic:
            if nic.networkid == network.id:
                vsd_vport = self.vsd.get_vport(
                    subnet=vsd_subnet,
                    filter=self.get_externalID_filter(nic.id))
        self.assertEqual(vsd_vport.associated_floating_ip_id, vsd_fip.id,
                         "Floating IP in VSD should be associated to the "
                         "correct VSD vport, which in turn should correspond "
                         "to the correct Static NAT rule enabled VM and "
                         "network in CloudStack"
                         )
        self.debug("Successfully verified the assignment and state of public "
                   "IP address - %s in VSD" % public_ipaddress.ipaddress)

    # verify_vsd_firewall_rule - Verifies the given CloudStack Network Firewall
    # (Ingress/Egress ACL) rule against the corresponding installed firewall
    # rule in VSD
    def verify_vsd_firewall_rule(self, firewall_rule, traffic_type="Ingress"):
        self.debug("Verifying the creation and state of Network Firewall "
                   "(Ingress/Egress ACL) rule with ID - %s in VSD" %
                   firewall_rule.id)
        ext_fw_rule_filter = self.get_externalID_filter(firewall_rule.id)
        vsd_fw_rule = self.vsd.get_egress_acl_entry(
            filter=ext_fw_rule_filter) if traffic_type is "Ingress" \
            else self.vsd.get_ingress_acl_entry(filter=ext_fw_rule_filter)
        self.assertEqual(vsd_fw_rule.policy_state, "LIVE",
                         "Ingress/Egress ACL rule's policy state in VSD "
                         "should be LIVE"
                         )
        dest_port = \
            str(firewall_rule.startport) + "-" + str(firewall_rule.endport)
        self.assertEqual(vsd_fw_rule.destination_port, dest_port,
                         "Ingress/Egress ACL rule's destination port in VSD "
                         "should match corresponding rule's destination port "
                         "in CloudStack"
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
                         "Ingress/Egress ACL rule's protocol in VSD should "
                         "match corresponding rule's protocol in CloudStack"
                         )
        self.debug("Successfully verified the creation and state of Network "
                   "Firewall (Ingress/Egress ACL) rule with ID - %s in VSD" %
                   firewall_rule.id)

    def add_resource_tag(self, resource_id, resource_type, key, value,
                         fordisplay=False):
        details = {key: value}
        return ResourceDetails.create(self.api_client, resourceid=resource_id,
                                      resourcetype=resource_type,
                                      details=details, fordisplay=fordisplay)

    def list_resource_tag(self, resource_id, resource_type, key,
                          fordisplay=False):
        return ResourceDetails.list(self.api_client, resourceid=resource_id,
                                    resourcetype=resource_type, key=key,
                                    fordisplay=fordisplay)

    def delete_resource_tag(self, resource_id, resource_type):
        return ResourceDetails.delete(self.api_client,
                                      resourceid=resource_id,
                                      resourcetype=resource_type)
