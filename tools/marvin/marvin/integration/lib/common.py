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
"""Common functions
"""

#Import Local Modules
from marvin.cloudstackAPI import (listConfigurations,
                                  listPhysicalNetworks,
                                  listRegions,
                                  addNetworkServiceProvider,
                                  updateNetworkServiceProvider,
                                  listDomains,
                                  listZones,
                                  listPods,
                                  listOsTypes,
                                  listTemplates,
                                  updateResourceLimit,
                                  listRouters,
                                  listNetworks,
                                  listClusters,
                                  listSystemVms,
                                  listStoragePools,
                                  listVirtualMachines,
                                  listLoadBalancerRuleInstances,
                                  listFirewallRules,
                                  listVolumes,
                                  listIsos,
                                  listAccounts,
                                  listSnapshotPolicies,
                                  listDiskOfferings,
                                  listVlanIpRanges,
                                  listUsageRecords,
                                  listNetworkServiceProviders,
                                  listHosts,
                                  listPublicIpAddresses,
                                  listPortForwardingRules,
                                  listLoadBalancerRules,
                                  listSnapshots,
                                  listUsers,
                                  listEvents,
                                  listServiceOfferings,
                                  listVirtualRouterElements,
                                  listNetworkOfferings,
                                  listResourceLimits,
                                  listVPCOfferings)
from marvin.integration.lib.base import (Configurations,
                                         NetScaler,
                                         Template,
                                         Resources,
                                         PhysicalNetwork,
                                         Host)
from marvin.integration.lib.utils import (get_process_status,
                                          xsplit)

from marvin.sshClient import SshClient
import random

#Import System modules
import time


def is_config_suitable(apiclient, name, value):
    """
    Ensure if the deployment has the expected `value` for the global setting `name'
    @return: true if value is set, else false
    """
    configs = Configurations.list(apiclient, name=name)
    assert(configs is not None and isinstance(configs, list) and len(configs) > 0)
    return configs[0].value == value

def wait_for_cleanup(apiclient, configs=None):
    """Sleeps till the cleanup configs passed"""

    # Configs list consists of the list of global configs
    if not isinstance(configs, list):
        return
    for config in configs:
        cmd = listConfigurations.listConfigurationsCmd()
        cmd.name = config
        cmd.listall = True
        try:
            config_descs = apiclient.listConfigurations(cmd)
        except Exception as e:
            raise Exception("Failed to fetch configurations: %s" % e)

        if not isinstance(config_descs, list):
            raise Exception("List configs didn't returned a valid data")

        config_desc = config_descs[0]
        # Sleep for the config_desc.value time
        time.sleep(int(config_desc.value))
    return

def add_netscaler(apiclient, zoneid, NSservice):
    """ Adds Netscaler device and enables NS provider"""

    cmd = listPhysicalNetworks.listPhysicalNetworksCmd()
    cmd.zoneid = zoneid
    physical_networks = apiclient.listPhysicalNetworks(cmd)
    if isinstance(physical_networks, list):
       physical_network = physical_networks[0]

    cmd = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
    cmd.name = 'Netscaler'
    cmd.physicalnetworkid=physical_network.id
    nw_service_providers = apiclient.listNetworkServiceProviders(cmd)

    if isinstance(nw_service_providers, list):
        netscaler_provider = nw_service_providers[0]
    else:
        cmd1 = addNetworkServiceProvider.addNetworkServiceProviderCmd()
        cmd1.name = 'Netscaler'
        cmd1.physicalnetworkid = physical_network.id
        netscaler_provider = apiclient.addNetworkServiceProvider(cmd1)

    netscaler = NetScaler.add(
                    apiclient,
                    NSservice,
                    physicalnetworkid=physical_network.id
                    )
    if netscaler_provider.state != 'Enabled':
      cmd = updateNetworkServiceProvider.updateNetworkServiceProviderCmd()
      cmd.id = netscaler_provider.id
      cmd.state =  'Enabled'
      apiclient.updateNetworkServiceProvider(cmd)

    return netscaler

def get_region(apiclient, services=None):
    "Returns a default region"

    cmd = listRegions.listRegionsCmd()
    if services:
        if "regionid" in services:
            cmd.id = services["regionid"]

    regions = apiclient.listRegions(cmd)

    if isinstance(regions, list):
        assert len(regions) > 0
        return regions[0]
    else:
        raise Exception("Failed to find specified region.")

def get_domain(apiclient, services=None):
    "Returns a default domain"

    cmd = listDomains.listDomainsCmd()
    if services:
        if "domainid" in services:
            cmd.id = services["domainid"]

    domains = apiclient.listDomains(cmd)

    if isinstance(domains, list):
        assert len(domains) > 0
        return domains[0]
    else:
        raise Exception("Failed to find specified domain.")


def get_zone(apiclient, services=None):
    "Returns a default zone"

    cmd = listZones.listZonesCmd()
    if services:
        if "zoneid" in services:
            cmd.id = services["zoneid"]

    zones = apiclient.listZones(cmd)

    if isinstance(zones, list):
        assert len(zones) > 0, "There are no available zones in the deployment"
        return zones[0]
    else:
        raise Exception("Failed to find specified zone.")


def get_pod(apiclient, zoneid, services=None):
    "Returns a default pod for specified zone"

    cmd = listPods.listPodsCmd()
    cmd.zoneid = zoneid

    if services:
        if "podid" in services:
            cmd.id = services["podid"]

    pods = apiclient.listPods(cmd)

    if isinstance(pods, list):
        assert len(pods) > 0, "No pods found for zone %s"%zoneid
        return pods[0]
    else:
        raise Exception("Exception: Failed to find specified pod.")


def get_template(apiclient, zoneid, ostype, services=None,
                 templatefilter='featured',
                 templatetype='BUILTIN'):
    "Returns a template"

    cmd = listOsTypes.listOsTypesCmd()
    cmd.description = ostype
    ostypes = apiclient.listOsTypes(cmd)

    if isinstance(ostypes, list):
        ostypeid = ostypes[0].id
    else:
        raise Exception(
            "Failed to find OS type with description: %s" % ostype)

    cmd = listTemplates.listTemplatesCmd()
    cmd.templatefilter = templatefilter
    cmd.zoneid = zoneid

    if services:
        if "template" in services:
            cmd.id = services["template"]

    list_templates = apiclient.listTemplates(cmd)

    if isinstance(list_templates, list):
        assert len(list_templates) > 0, "received empty response on template of type %s"%ostype
        for template in list_templates:
            if template.ostypeid == ostypeid and template.isready and template.templatetype == templatetype:
                return template

    raise Exception("Exception: Failed to find template of type %s with OSTypeID and which is in "
                                "ready state: %s" %(templatetype, ostypeid))
    return


def download_systemplates_sec_storage(server, services):
    """Download System templates on sec storage"""

    try:
        # Login to management server
        ssh = SshClient(
                                          server["ipaddress"],
                                          server["port"],
                                          server["username"],
                                          server["password"]
                             )
    except Exception:
        raise Exception("SSH access failted for server with IP address: %s" %
                                                            server["ipaddess"])
    # Mount Secondary Storage on Management Server
    cmds = [
            "mkdir -p %s" % services["mnt_dir"],
            "mount -t nfs %s:/%s %s" % (
                                        services["sec_storage"],
                                        services["path"],
                                        services["mnt_dir"]
                                        ),
            "%s -m %s -u %s -h %s -F" % (
                                         services["command"],
                                         services["mnt_dir"],
                                         services["download_url"],
                                         services["hypervisor"]
                                        )
            ]
    for c in cmds:
        result = ssh.execute(c)

    res = str(result)

    # Unmount the Secondary storage
    ssh.execute("umount %s" % (services["mnt_dir"]))

    if res.count("Successfully installed system VM template") == 1:
        return
    else:
        raise Exception("Failed to download System Templates on Sec Storage")
    return


def wait_for_ssvms(apiclient, zoneid, podid, interval=60):
    """After setup wait for SSVMs to come Up"""

    time.sleep(interval)
    timeout = 40
    while True:
            list_ssvm_response = list_ssvms(
                                        apiclient,
                                        systemvmtype='secondarystoragevm',
                                        zoneid=zoneid,
                                        podid=podid
                                        )
            ssvm = list_ssvm_response[0]
            if ssvm.state != 'Running':
                # Sleep to ensure SSVMs are Up and Running
                time.sleep(interval)
                timeout = timeout - 1
            elif ssvm.state == 'Running':
                break
            elif timeout == 0:
                raise Exception("SSVM failed to come up")
                break

    timeout = 40
    while True:
            list_ssvm_response = list_ssvms(
                                        apiclient,
                                        systemvmtype='consoleproxy',
                                        zoneid=zoneid,
                                        podid=podid
                                        )
            cpvm = list_ssvm_response[0]
            if cpvm.state != 'Running':
                # Sleep to ensure SSVMs are Up and Running
                time.sleep(interval)
                timeout = timeout - 1
            elif cpvm.state == 'Running':
                break
            elif timeout == 0:
                raise Exception("CPVM failed to come up")
                break
    return

def get_builtin_template_info(apiclient, zoneid):
    """Returns hypervisor specific infor for templates"""

    list_template_response = Template.list(
                                    apiclient,
                                    templatefilter='featured',
                                    zoneid=zoneid,
                                    )

    for b_template in list_template_response:
            if b_template.templatetype == 'BUILTIN':
                break

    extract_response = Template.extract(apiclient,
                                            b_template.id,
                                            'HTTP_DOWNLOAD',
                                            zoneid)

    return extract_response.url, b_template.hypervisor, b_template.format

def download_builtin_templates(apiclient, zoneid, hypervisor, host,
                                                linklocalip, interval=60):
    """After setup wait till builtin templates are downloaded"""

    # Change IPTABLES Rules
    get_process_status(
                        host["ipaddress"],
                        host["port"],
                        host["username"],
                        host["password"],
                        linklocalip,
                        "iptables -P INPUT ACCEPT"
                    )
    time.sleep(interval)
    # Find the BUILTIN Templates for given Zone, Hypervisor
    list_template_response = list_templates(
                                    apiclient,
                                    hypervisor=hypervisor,
                                    zoneid=zoneid,
                                    templatefilter='self'
                                    )

    if not isinstance(list_template_response, list):
        raise Exception("Failed to download BUILTIN templates")

    # Ensure all BUILTIN templates are downloaded
    templateid = None
    for template in list_template_response:
        if template.templatetype == "BUILTIN":
                templateid = template.id

    # Sleep to ensure that template is in downloading state after adding
    # Sec storage
    time.sleep(interval)
    while True:
        template_response = list_templates(
                                    apiclient,
                                    id=templateid,
                                    zoneid=zoneid,
                                    templatefilter='self'
                                    )
        template = template_response[0]
        # If template is ready,
        # template.status = Download Complete
        # Downloading - x% Downloaded
        # Error - Any other string
        if template.status == 'Download Complete':
            break

        elif 'Downloaded' in template.status:
            time.sleep(interval)

        elif 'Installing' not in template.status:
            raise Exception("ErrorInDownload")

    return


def update_resource_limit(apiclient, resourcetype, account=None,
                                    domainid=None, max=None, projectid=None):
    """Updates the resource limit to 'max' for given account"""

    cmd = updateResourceLimit.updateResourceLimitCmd()
    cmd.resourcetype = resourcetype
    if account:
        cmd.account = account
    if domainid:
        cmd.domainid = domainid
    if max:
        cmd.max = max
    if projectid:
        cmd.projectid = projectid
    apiclient.updateResourceLimit(cmd)
    return


def list_os_types(apiclient, **kwargs):
    """List all os types matching criteria"""

    cmd = listOsTypes.listOsTypesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listOsTypes(cmd))


def list_routers(apiclient, **kwargs):
    """List all Routers matching criteria"""

    cmd = listRouters.listRoutersCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listRouters(cmd))


def list_zones(apiclient, **kwargs):
    """List all Zones matching criteria"""

    cmd = listZones.listZonesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listZones(cmd))


def list_networks(apiclient, **kwargs):
    """List all Networks matching criteria"""

    cmd = listNetworks.listNetworksCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listNetworks(cmd))


def list_clusters(apiclient, **kwargs):
    """List all Clusters matching criteria"""

    cmd = listClusters.listClustersCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listClusters(cmd))


def list_ssvms(apiclient, **kwargs):
    """List all SSVMs matching criteria"""

    cmd = listSystemVms.listSystemVmsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listSystemVms(cmd))


def list_storage_pools(apiclient, **kwargs):
    """List all storage pools matching criteria"""

    cmd = listStoragePools.listStoragePoolsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listStoragePools(cmd))


def list_virtual_machines(apiclient, **kwargs):
    """List all VMs matching criteria"""

    cmd = listVirtualMachines.listVirtualMachinesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listVirtualMachines(cmd))


def list_hosts(apiclient, **kwargs):
    """List all Hosts matching criteria"""

    cmd = listHosts.listHostsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listHosts(cmd))


def list_configurations(apiclient, **kwargs):
    """List configuration with specified name"""

    cmd = listConfigurations.listConfigurationsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listConfigurations(cmd))


def list_publicIP(apiclient, **kwargs):
    """List all Public IPs matching criteria"""

    cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listPublicIpAddresses(cmd))


def list_nat_rules(apiclient, **kwargs):
    """List all NAT rules matching criteria"""

    cmd = listPortForwardingRules.listPortForwardingRulesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listPortForwardingRules(cmd))


def list_lb_rules(apiclient, **kwargs):
    """List all Load balancing rules matching criteria"""

    cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listLoadBalancerRules(cmd))


def list_lb_instances(apiclient, **kwargs):
    """List all Load balancing instances matching criteria"""

    cmd = listLoadBalancerRuleInstances.listLoadBalancerRuleInstancesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listLoadBalancerRuleInstances(cmd))


def list_firewall_rules(apiclient, **kwargs):
    """List all Firewall Rules matching criteria"""

    cmd = listFirewallRules.listFirewallRulesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listFirewallRules(cmd))


def list_volumes(apiclient, **kwargs):
    """List all volumes matching criteria"""

    cmd = listVolumes.listVolumesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listVolumes(cmd))


def list_isos(apiclient, **kwargs):
    """Lists all available ISO files."""

    cmd = listIsos.listIsosCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listIsos(cmd))


def list_snapshots(apiclient, **kwargs):
    """List all snapshots matching criteria"""

    cmd = listSnapshots.listSnapshotsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listSnapshots(cmd))


def list_templates(apiclient, **kwargs):
    """List all templates matching criteria"""

    cmd = listTemplates.listTemplatesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listTemplates(cmd))


def list_domains(apiclient, **kwargs):
    """Lists domains"""

    cmd = listDomains.listDomainsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listDomains(cmd))


def list_accounts(apiclient, **kwargs):
    """Lists accounts and provides detailed account information for
    listed accounts"""

    cmd = listAccounts.listAccountsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listAccounts(cmd))


def list_users(apiclient, **kwargs):
    """Lists users and provides detailed account information for
    listed users"""

    cmd = listUsers.listUsersCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listUsers(cmd))


def list_snapshot_policy(apiclient, **kwargs):
    """Lists snapshot policies."""

    cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listSnapshotPolicies(cmd))


def list_events(apiclient, **kwargs):
    """Lists events"""

    cmd = listEvents.listEventsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listEvents(cmd))


def list_disk_offering(apiclient, **kwargs):
    """Lists all available disk offerings."""

    cmd = listDiskOfferings.listDiskOfferingsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listDiskOfferings(cmd))


def list_service_offering(apiclient, **kwargs):
    """Lists all available service offerings."""

    cmd = listServiceOfferings.listServiceOfferingsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listServiceOfferings(cmd))


def list_vlan_ipranges(apiclient, **kwargs):
    """Lists all VLAN IP ranges."""

    cmd = listVlanIpRanges.listVlanIpRangesCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listVlanIpRanges(cmd))


def list_usage_records(apiclient, **kwargs):
    """Lists usage records for accounts"""

    cmd = listUsageRecords.listUsageRecordsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listUsageRecords(cmd))


def list_nw_service_prividers(apiclient, **kwargs):
    """Lists Network service providers"""

    cmd = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listNetworkServiceProviders(cmd))


def list_virtual_router_elements(apiclient, **kwargs):
    """Lists Virtual Router elements"""

    cmd = listVirtualRouterElements.listVirtualRouterElementsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listVirtualRouterElements(cmd))


def list_network_offerings(apiclient, **kwargs):
    """Lists network offerings"""

    cmd = listNetworkOfferings.listNetworkOfferingsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listNetworkOfferings(cmd))


def list_resource_limits(apiclient, **kwargs):
    """Lists resource limits"""

    cmd = listResourceLimits.listResourceLimitsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listResourceLimits(cmd))

def list_vpc_offerings(apiclient, **kwargs):
    """ Lists VPC offerings """

    cmd = listVPCOfferings.listVPCOfferingsCmd()
    [setattr(cmd, k, v) for k, v in kwargs.items()]
    return(apiclient.listVPCOfferings(cmd))

def update_resource_count(apiclient, domainid, accountid=None,
                          projectid=None, rtype=None):
        """updates the resource count
            0     - VM
            1     - Public IP
            2     - Volume
            3     - Snapshot
            4     - Template
            5     - Projects
            6     - Network
            7     - VPC
            8     - CPUs
            9     - RAM
            10    - Primary (shared) storage (Volumes)
            11    - Secondary storage (Snapshots, Templates & ISOs)
        """

        Resources.updateCount(apiclient,
                              domainid=domainid,
                              account=accountid if accountid else None,
                              projectid=projectid if projectid else None,
                              resourcetype=rtype if rtype else None
                              )
        return

def find_suitable_host(apiclient, vm):
        """Returns a suitable host for VM migration"""

        hosts = Host.list(apiclient,
                          virtualmachineid=vm.id,
                          listall=True)

        if isinstance(hosts, list):
            assert len(hosts) > 0, "List host should return valid response"
        else:
            raise Exception("Exception: List host should return valid response")
        return hosts[0]

def get_resource_type(resource_id):
        """Returns resource type"""

        lookup = {  0: "VM",
                    1: "Public IP",
                    2: "Volume",
                    3: "Snapshot",
                    4: "Template",
                    5: "Projects",
                    6: "Network",
                    7: "VPC",
                    8: "CPUs",
                    9: "RAM",
                    10: "Primary (shared) storage (Volumes)",
                    11: "Secondary storage (Snapshots, Templates & ISOs)"
                 }

        return lookup[resource_id]

def get_portable_ip_range_services(config):
    """ Reads config values related to portable ip and fills up
    services accordingly"""

    services = {}
    attributeError = False

    if config.portableIpRange.startip:
        services["startip"] = config.portableIpRange.startip
    else:
        attributeError = True

    if config.portableIpRange.endip:
        services["endip"] = config.portableIpRange.endip
    else:
        attributeError = True

    if config.portableIpRange.netmask:
        services["netmask"] = config.portableIpRange.netmask
    else:
        attributeError = True

    if config.portableIpRange.gateway:
        services["gateway"] = config.portableIpRange.gateway
    else:
        attributeError = True

    if config.portableIpRange.vlan:
        services["vlan"] = config.portableIpRange.vlan

    if attributeError:
        services = None

    return services

def get_free_vlan(apiclient, zoneid):
    """
    Find an unallocated VLAN outside the range allocated to the physical network.

    @note: This does not guarantee that the VLAN is available for use in
    the deployment's network gear
    @return: physical_network, shared_vlan_tag
    """
    list_physical_networks_response = PhysicalNetwork.list(
            apiclient,
            zoneid=zoneid
        )
    assert isinstance(list_physical_networks_response, list)
    assert len(list_physical_networks_response) > 0, "No physical networks found in zone %s" % zoneid

    physical_network = list_physical_networks_response[0]

    networks = list_networks(apiclient, zoneid= zoneid, type='Shared')
    usedVlanIds = []

    if isinstance(networks, list) and len(networks) > 0:
        usedVlanIds = [int(nw.vlan) for nw in networks if nw.vlan!="untagged"]

    if hasattr(physical_network, "vlan") is False:
        while True:
            shared_ntwk_vlan = random.randrange(1,4095)
            if shared_ntwk_vlan in usedVlanIds:
                continue
            else:
                break
    else:
        vlans = xsplit(physical_network.vlan, ['-', ','])

        assert len(vlans) > 0
        assert int(vlans[0]) < int(vlans[-1]), "VLAN range  %s was improperly split" % physical_network.vlan

        retriesCount = 20 #Assuming random function will give different integer each time

        shared_ntwk_vlan = None

        while True:

            if retriesCount == 0:
                break

            free_vlan = int(vlans[-1]) + random.randrange(1, 20)

            if free_vlan > 4095:
                free_vlan = int(vlans[0]) - random.randrange(1, 20)
            if free_vlan < 0 or (free_vlan in usedVlanIds):
                retriesCount -= 1
                continue
            else:
                shared_ntwk_vlan = free_vlan
                break

    return physical_network, shared_ntwk_vlan

def setNonContiguousVlanIds(apiclient, zoneid):
    """
    Form the non contiguous ranges based on currently assigned range in physical network
    """

    NonContigVlanIdsAcquired = False

    list_physical_networks_response = PhysicalNetwork.list(
        apiclient,
        zoneid=zoneid
    )
    assert isinstance(list_physical_networks_response, list)
    assert len(list_physical_networks_response) > 0, "No physical networks found in zone %s" % zoneid

    for physical_network in list_physical_networks_response:

        vlans = xsplit(physical_network.vlan, ['-', ','])

        assert len(vlans) > 0
        assert int(vlans[0]) < int(vlans[-1]), "VLAN range  %s was improperly split" % physical_network.vlan

        # Keep some gap between existing vlan and the new vlans which we are going to add
        # So that they are non contiguous

        non_contig_end_vlan_id = int(vlans[-1]) + 6
        non_contig_start_vlan_id = int(vlans[0]) - 6

        # Form ranges which are consecutive to existing ranges but not immediately contiguous
        # There should be gap in between existing range and new non contiguous ranage

        # If you can't add range after existing range, because it's crossing 4095, then
        # select VLAN ids before the existing range such that they are greater than 0, and
        # then add this non contiguoud range
        vlan = { "partial_range": ["",""], "full_range": ""}

        if non_contig_end_vlan_id < 4095:
            vlan["partial_range"][0] = str(non_contig_end_vlan_id - 4) + '-' + str(non_contig_end_vlan_id - 3)
            vlan["partial_range"][1] = str(non_contig_end_vlan_id - 1) + '-' + str(non_contig_end_vlan_id)
            vlan["full_range"] = str(non_contig_end_vlan_id - 4) + '-' + str(non_contig_end_vlan_id)
            NonContigVlanIdsAcquired = True

        elif non_contig_start_vlan_id > 0:
            vlan["partial_range"][0] = str(non_contig_start_vlan_id) + '-' + str(non_contig_start_vlan_id + 1)
            vlan["partial_range"][1] = str(non_contig_start_vlan_id + 3) + '-' + str(non_contig_start_vlan_id + 4)
            vlan["full_range"] = str(non_contig_start_vlan_id) + '-' + str(non_contig_start_vlan_id + 4)
            NonContigVlanIdsAcquired = True

        else:
            NonContigVlanIdsAcquired = False

        # If failed to get relevant vlan ids, continue to next physical network
        # else break from loop as we have hot the non contiguous vlan ids for the test purpose

        if not NonContigVlanIdsAcquired:
            continue
        else:
            break

    # If even through looping from all existing physical networks, failed to get relevant non
    # contiguous vlan ids, then fail the test case

    if not NonContigVlanIdsAcquired:
        return None, None

    return physical_network, vlan
