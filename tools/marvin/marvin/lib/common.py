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

# Import Local Modules
from marvin.config.test_data import test_data
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
                                  listVPCOfferings,
                                  listManagementServers,
                                  migrateSystemVm)
from marvin.sshClient import SshClient
from marvin.codes import (PASS, FAILED, ISOLATED_NETWORK, VPC_NETWORK,
                          BASIC_ZONE, FAIL, NAT_RULE, STATIC_NAT_RULE,
                          RESOURCE_PRIMARY_STORAGE, RESOURCE_SECONDARY_STORAGE,
                          RESOURCE_USER_VM, RESOURCE_PUBLIC_IP, RESOURCE_VOLUME,
                          RESOURCE_SNAPSHOT, RESOURCE_TEMPLATE, RESOURCE_PROJECT,
                          RESOURCE_NETWORK, RESOURCE_VPC,
                          RESOURCE_CPU, RESOURCE_MEMORY, PUBLIC_TRAFFIC,
                          GUEST_TRAFFIC, MANAGEMENT_TRAFFIC, STORAGE_TRAFFIC,
                          VMWAREDVS)
from marvin.lib.utils import (validateList,
                              xsplit,
                              get_process_status,
                              random_gen,
                              format_volume_to_ext3)
from marvin.lib.base import (PhysicalNetwork,
                             PublicIPAddress,
                             NetworkOffering,
                             NATRule,
                             StaticNATRule,
                             Volume,
                             Template,
                             Account,
                             Project,
                             Snapshot,
                             NetScaler,
                             VirtualMachine,
                             FireWallRule,
                             Template,
                             Network,
                             Host,
                             Resources,
                             Configurations,
                             Router,
                             PublicIpRange,
                             StorageNetworkIpRange,
                             TrafficType)
from marvin.lib.vcenter import Vcenter
from netaddr import IPAddress
import random
import re
import itertools
import random
import hashlib

# Import System modules
import time


def is_config_suitable(apiclient, name, value):
    """
    Ensure if the deployment has the expected `value` for the global setting `name'
    @return: true if value is set, else false
    """
    configs = Configurations.list(apiclient, name=name)
    assert(
        configs is not None and isinstance(
            configs,
            list) and len(
            configs) > 0)
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
    cmd.physicalnetworkid = physical_network.id
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
        cmd.state = 'Enabled'
        apiclient.updateNetworkServiceProvider(cmd)

    return netscaler


def get_region(apiclient, region_id=None, region_name=None):
    '''
    @name : get_region
    @Desc : Returns the Region Information for a given region  id or region name
    @Input : region_name: Name of the Region
             region_id : Id of the region
    @Output : 1. Region  Information for the passed inputs else first Region
              2. FAILED In case the cmd failed
    '''
    cmd = listRegions.listRegionsCmd()
    if region_name is not None:
        cmd.name = region_name
    if region_id is not None:
        cmd.id = region_id
    cmd_out = apiclient.listRegions(cmd)
    return FAILED if validateList(cmd_out)[0] != PASS else cmd_out[0]


def get_domain(apiclient, domain_id=None, domain_name=None):
    '''
    @name : get_domain
    @Desc : Returns the Domain Information for a given domain id or domain name
    @Input : domain id : Id of the Domain
             domain_name : Name of the Domain
    @Output : 1. Domain  Information for the passed inputs else first Domain
              2. FAILED In case the cmd failed
    '''
    cmd = listDomains.listDomainsCmd()

    if domain_name is not None:
        cmd.name = domain_name
    if domain_id is not None:
        cmd.id = domain_id
    cmd_out = apiclient.listDomains(cmd)
    if validateList(cmd_out)[0] != PASS:
        return FAILED
    return cmd_out[0]


def find_storage_pool_type(apiclient, storagetype='NetworkFileSystem'):
    """
    @name : find_storage_pool_type
    @Desc : Returns true if the given storage pool type exists
    @Input : type : type of the storage pool[NFS, RBD, etc.,]
    @Output : True : if the type of storage is found
              False : if the type of storage is not found
              FAILED In case the cmd failed
    """
    cmd = listStoragePools.listStoragePoolsCmd()
    cmd_out = apiclient.listStoragePools(cmd)
    if validateList(cmd_out)[0] != PASS:
        return FAILED
    for storage_pool in cmd_out:
        if storage_pool.type.lower() == storagetype:
            return True
    return False


def get_zone(apiclient, zone_name=None, zone_id=None):
    '''
    @name : get_zone
    @Desc :Returns the Zone Information for a given zone id or Zone Name
    @Input : zone_name: Name of the Zone
             zone_id : Id of the zone
    @Output : 1. Zone Information for the passed inputs else first zone
              2. FAILED In case the cmd failed
    '''
    cmd = listZones.listZonesCmd()
    if zone_name is not None:
        cmd.name = zone_name
    if zone_id is not None:
        cmd.id = zone_id

    cmd_out = apiclient.listZones(cmd)

    if validateList(cmd_out)[0] != PASS:
        return FAILED
    '''
    Check if input zone name and zone id is None,
    then return first element of List Zones command
    '''
    return cmd_out[0]

def get_physical_networks(apiclient, zoneid):
    '''
     @name : get_physical_networks
     @Desc :Returns A list of the Physical Networks in the given Zone
     @Input : zoneid: The Zone ID
     @Output : 1. A list containing the Physical Networks
    '''
    cmd = listPhysicalNetworks.listPhysicalNetworksCmd()
    cmd.zoneid = zoneid
    physical_networks = apiclient.listPhysicalNetworks(cmd)
    return physical_networks

def get_pod(apiclient, zone_id=None, pod_id=None, pod_name=None):
    '''
    @name : get_pod
    @Desc :  Returns the Pod Information for a given zone id or Zone Name
    @Input : zone_id: Id of the Zone
             pod_name : Name of the Pod
             pod_id : Id of the Pod
    @Output : 1. Pod Information for the pod
              2. FAILED In case the cmd failed
    '''
    cmd = listPods.listPodsCmd()

    if pod_name is not None:
        cmd.name = pod_name
    if pod_id is not None:
        cmd.id = pod_id
    if zone_id is not None:
        cmd.zoneid = zone_id

    cmd_out = apiclient.listPods(cmd)

    if validateList(cmd_out)[0] != PASS:
        return FAILED
    return cmd_out[0]

def get_template(
        apiclient, zone_id=None, ostype_desc=None, template_filter="featured", template_type='BUILTIN',
        template_id=None, template_name=None, account=None, domain_id=None, project_id=None,
        hypervisor=None):
    '''
    @Name : get_template
    @Desc : Retrieves the template Information based upon inputs provided
            Template is retrieved based upon either of the inputs matched
            condition
    @Input : returns a template"
    @Output : FAILED in case of any failure
              template Information matching the inputs
    '''
    cmd = listTemplates.listTemplatesCmd()
    cmd.templatefilter = template_filter
    if domain_id is not None:
        cmd.domainid = domain_id
    if zone_id is not None:
        cmd.zoneid = zone_id
    if template_id is not None:
        cmd.id = template_id
    if template_name is not None:
        cmd.name = template_name
    if hypervisor is not None:
        cmd.hypervisor = hypervisor
    if project_id is not None:
        cmd.projectid = project_id
    if account is not None:
        cmd.account = account

    '''
    Get the Templates pertaining to the inputs provided
    '''
    list_templatesout = apiclient.listTemplates(cmd)
    if validateList(list_templatesout)[0] != PASS:
        return FAILED

    for template in list_templatesout:
        if template.isready and template.templatetype == template_type:
            return template
    '''
    Return default first template, if no template matched
    '''
    return list_templatesout[0]


def get_test_template(apiclient, zone_id=None, hypervisor=None, test_templates=None, deploy_as_is=False):
    """
    @Name : get_test_template
    @Desc : Retrieves the test template used to running tests. When the template
            is missing it will be download at most one in a zone for a hypervisor.
    @Input : returns a template
    """

    if test_templates is None:
        test_templates = test_data["test_templates"]

    if hypervisor is None:
        return FAILED

    hypervisor = hypervisor.lower()

    # Return built-in template for simulator
    if hypervisor == 'simulator':
        return get_template(apiclient, zone_id)

    if hypervisor not in list(test_templates.keys()):
        print("Provided hypervisor has no test template")
        return FAILED

    test_template = test_templates[hypervisor]
    if deploy_as_is:
        test_template['deployasis'] = True

    cmd = listTemplates.listTemplatesCmd()
    cmd.name = test_template['name']
    cmd.templatefilter = 'all'
    if zone_id is not None:
        cmd.zoneid = zone_id
    if hypervisor is not None:
        cmd.hypervisor = hypervisor
    templates = apiclient.listTemplates(cmd)

    if validateList(templates)[0] != PASS:
        template = Template.register(apiclient, test_template, zoneid=zone_id, hypervisor=hypervisor.lower(), randomize_name=False)
        template.download(apiclient)
        return template

    for template in templates:
        if template.isready and template.ispublic:
            return template

    return FAILED


def get_test_ovf_templates(apiclient, zone_id=None, test_ovf_templates=None, hypervisor=None):
    """
    @Name : get_test_ovf_templates
    @Desc : Retrieves the list of test ovf templates used to running tests. When the template
            is missing it will be download at most one in a zone for a hypervisor.
    @Input : returns a list of templates
    """
    result = []

    if test_ovf_templates is None:
        test_ovf_templates = test_data["test_ovf_templates"]
    if test_ovf_templates is None:
        return result
    if hypervisor is None:
        return result
    hypervisor = hypervisor.lower()
    if hypervisor != 'vmware':
        return result

    for test_template in test_ovf_templates:

        cmd = listTemplates.listTemplatesCmd()
        cmd.name = test_template['name']
        cmd.templatefilter = 'all'
        if zone_id is not None:
            cmd.zoneid = zone_id
        if hypervisor is not None:
            cmd.hypervisor = hypervisor
        templates = apiclient.listTemplates(cmd)

        if validateList(templates)[0] != PASS:
            template = Template.register(apiclient, test_template, zoneid=zone_id, hypervisor=hypervisor.lower(), randomize_name=False)
            template.download(apiclient)
            retries = 3
            while (not hasattr(template, 'deployasisdetails') or len(template.deployasisdetails.__dict__) == 0) and retries > 0:
                time.sleep(10)
                template_list = Template.list(apiclient, id=template.id, zoneid=zone_id, templatefilter='all')
                if isinstance(template_list, list):
                    template = Template(template_list[0].__dict__)
                retries = retries - 1
            if not hasattr(template, 'deployasisdetails') or len(template.deployasisdetails.__dict__) == 0:
                template.delete(apiclient)
            else:
                result.append(template)

        if templates:
            for template in templates:
                if template.isready and template.ispublic and template.deployasisdetails and len(template.deployasisdetails.__dict__) > 0:
                    result.append(template)

    return result

def get_vm_vapp_configs(apiclient, config, setup_zone, vm_name):
    zoneDetailsInConfig = [zone for zone in config.zones
                           if zone.name == setup_zone.name][0]
    vcenterusername = zoneDetailsInConfig.vmwaredc.username
    vcenterpassword = zoneDetailsInConfig.vmwaredc.password
    vcenterip = zoneDetailsInConfig.vmwaredc.vcenter
    vcenterObj = Vcenter(
        vcenterip,
        vcenterusername,
        vcenterpassword)

    vms = vcenterObj.get_vms(vm_name)
    if vms:
        return vms[0]['vm']['properties']


def get_windows_template(
        apiclient, zone_id=None, ostype_desc=None, template_filter="featured", template_type='USER',
        template_id=None, template_name=None, account=None, domain_id=None, project_id=None,
        hypervisor=None):
    '''
    @Name : get_template
    @Desc : Retrieves the template Information based upon inputs provided
            Template is retrieved based upon either of the inputs matched
            condition
    @Input : returns a template"
    @Output : FAILED in case of any failure
              template Information matching the inputs
    '''
    cmd = listTemplates.listTemplatesCmd()
    cmd.templatefilter = template_filter
    if domain_id is not None:
        cmd.domainid = domain_id
    if zone_id is not None:
        cmd.zoneid = zone_id
    if template_id is not None:
        cmd.id = template_id
    if template_name is not None:
        cmd.name = template_name
    if hypervisor is not None:
        cmd.hypervisor = hypervisor
    if project_id is not None:
        cmd.projectid = project_id
    if account is not None:
        cmd.account = account


    '''
    Get the Templates pertaining to the inputs provided
    '''
    list_templatesout = apiclient.listTemplates(cmd)
    #print("template result is %s"%(list_templatesout))
    if list_templatesout is None:
        return FAILED
    if validateList(list_templatesout[0]) == FAIL :
            return FAILED

    for template in list_templatesout:
        if template.isready and template.templatetype == "USER" and template.ostypename == ostype_desc:
            return template
    '''
    Return default first template, if no template matched
    '''

    return FAILED

def get_suitable_test_template(apiclient, zoneid, ostypeid, hypervisor, deploy_as_is=False):
    '''
    @Name : get_suitable_test_template
    @Desc : Retrieves the test template information based upon inputs provided
            For Xenserver, get_test_template is used for retrieving the template
            while get_template is used for other hypervisors or when
            get_test_template fails
    @Input : returns a template"
    @Output : FAILED in case of any failure
              template Information matching the inputs
    '''
    template = FAILED
    if hypervisor.lower() in ["xenserver"] or (hypervisor.lower() in ["vmware"] and deploy_as_is):
        template = get_test_template(
            apiclient,
            zoneid,
            hypervisor,
            deploy_as_is=deploy_as_is)
    if template == FAILED:
        template = get_template(
            apiclient,
            zoneid,
            ostypeid)
    return template

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
        raise Exception("SSH access failed for server with IP address: %s" %
                        server["ipaddress"])
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
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listOsTypes(cmd))


def list_routers(apiclient, **kwargs):
    """List all Routers matching criteria"""

    cmd = listRouters.listRoutersCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listRouters(cmd))


def list_zones(apiclient, **kwargs):
    """List all Zones matching criteria"""

    cmd = listZones.listZonesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listZones(cmd))


def list_networks(apiclient, **kwargs):
    """List all Networks matching criteria"""

    cmd = listNetworks.listNetworksCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listNetworks(cmd))


def list_clusters(apiclient, **kwargs):
    """List all Clusters matching criteria"""

    cmd = listClusters.listClustersCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listClusters(cmd))


def list_ssvms(apiclient, **kwargs):
    """List all SSVMs matching criteria"""

    cmd = listSystemVms.listSystemVmsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listSystemVms(cmd))


def list_storage_pools(apiclient, **kwargs):
    """List all storage pools matching criteria"""

    cmd = listStoragePools.listStoragePoolsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listStoragePools(cmd))


def list_virtual_machines(apiclient, **kwargs):
    """List all VMs matching criteria"""

    cmd = listVirtualMachines.listVirtualMachinesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listVirtualMachines(cmd))


def list_hosts(apiclient, **kwargs):
    """List all Hosts matching criteria"""

    cmd = listHosts.listHostsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listHosts(cmd))


def list_configurations(apiclient, **kwargs):
    """List configuration with specified name"""

    cmd = listConfigurations.listConfigurationsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listConfigurations(cmd))


def list_publicIP(apiclient, **kwargs):
    """List all Public IPs matching criteria"""

    cmd = listPublicIpAddresses.listPublicIpAddressesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listPublicIpAddresses(cmd))


def list_nat_rules(apiclient, **kwargs):
    """List all NAT rules matching criteria"""

    cmd = listPortForwardingRules.listPortForwardingRulesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listPortForwardingRules(cmd))


def list_lb_rules(apiclient, **kwargs):
    """List all Load balancing rules matching criteria"""

    cmd = listLoadBalancerRules.listLoadBalancerRulesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listLoadBalancerRules(cmd))


def list_lb_instances(apiclient, **kwargs):
    """List all Load balancing instances matching criteria"""

    cmd = listLoadBalancerRuleInstances.listLoadBalancerRuleInstancesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listLoadBalancerRuleInstances(cmd))


def list_firewall_rules(apiclient, **kwargs):
    """List all Firewall Rules matching criteria"""

    cmd = listFirewallRules.listFirewallRulesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listFirewallRules(cmd))


def list_volumes(apiclient, **kwargs):
    """List all volumes matching criteria"""

    cmd = listVolumes.listVolumesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listVolumes(cmd))


def list_isos(apiclient, **kwargs):
    """Lists all available ISO files."""

    cmd = listIsos.listIsosCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listIsos(cmd))


def list_snapshots(apiclient, **kwargs):
    """List all snapshots matching criteria"""

    cmd = listSnapshots.listSnapshotsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listSnapshots(cmd))


def list_templates(apiclient, **kwargs):
    """List all templates matching criteria"""

    cmd = listTemplates.listTemplatesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listTemplates(cmd))


def list_domains(apiclient, **kwargs):
    """Lists domains"""

    cmd = listDomains.listDomainsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listDomains(cmd))


def list_accounts(apiclient, **kwargs):
    """Lists accounts and provides detailed account information for
    listed accounts"""

    cmd = listAccounts.listAccountsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listAccounts(cmd))


def list_users(apiclient, **kwargs):
    """Lists users and provides detailed account information for
    listed users"""

    cmd = listUsers.listUsersCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listUsers(cmd))


def list_snapshot_policy(apiclient, **kwargs):
    """Lists snapshot policies."""

    cmd = listSnapshotPolicies.listSnapshotPoliciesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listSnapshotPolicies(cmd))


def list_events(apiclient, **kwargs):
    """Lists events"""

    cmd = listEvents.listEventsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listEvents(cmd))


def list_disk_offering(apiclient, **kwargs):
    """Lists all available disk offerings."""

    cmd = listDiskOfferings.listDiskOfferingsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listDiskOfferings(cmd))


def list_service_offering(apiclient, **kwargs):
    """Lists all available service offerings."""

    cmd = listServiceOfferings.listServiceOfferingsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listServiceOfferings(cmd))


def list_vlan_ipranges(apiclient, **kwargs):
    """Lists all VLAN IP ranges."""

    cmd = listVlanIpRanges.listVlanIpRangesCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listVlanIpRanges(cmd))


def list_usage_records(apiclient, **kwargs):
    """Lists usage records for accounts"""

    cmd = listUsageRecords.listUsageRecordsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listUsageRecords(cmd))


def list_nw_service_prividers(apiclient, **kwargs):
    """Lists Network service providers"""

    cmd = listNetworkServiceProviders.listNetworkServiceProvidersCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listNetworkServiceProviders(cmd))


def list_virtual_router_elements(apiclient, **kwargs):
    """Lists Virtual Router elements"""

    cmd = listVirtualRouterElements.listVirtualRouterElementsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listVirtualRouterElements(cmd))


def list_network_offerings(apiclient, **kwargs):
    """Lists network offerings"""

    cmd = listNetworkOfferings.listNetworkOfferingsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listNetworkOfferings(cmd))


def list_resource_limits(apiclient, **kwargs):
    """Lists resource limits"""

    cmd = listResourceLimits.listResourceLimitsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listResourceLimits(cmd))


def list_vpc_offerings(apiclient, **kwargs):
    """ Lists VPC offerings """

    cmd = listVPCOfferings.listVPCOfferingsCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listVPCOfferings(cmd))

def list_mgmt_servers(apiclient, **kwargs):
    """ Lists Management Servers """

    cmd = listManagementServers.listManagementServersCmd()
    [setattr(cmd, k, v) for k, v in list(kwargs.items())]
    if 'account' in list(kwargs.keys()) and 'domainid' in list(kwargs.keys()):
        cmd.listall=True
    return(apiclient.listManagementServers(cmd))

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

def findSuitableHostForMigration(apiclient, vmid):
    """Returns a suitable host for VM migration"""
    suitableHost = None
    try:
        hosts = Host.listForMigration(apiclient, virtualmachineid=vmid,
                )
    except Exception as e:
        raise Exception("Exception while getting hosts list suitable for migration: %s" % e)

    suitablehosts = []
    if isinstance(hosts, list) and len(hosts) > 0:
        suitablehosts = [host for host in hosts if (str(host.resourcestate).lower() == "enabled"\
                and str(host.state).lower() == "up")]
        if len(suitablehosts)>0:
            suitableHost = suitablehosts[0]

    return suitableHost


def get_resource_type(resource_id):
    """Returns resource type"""

    lookup = {0: "VM",
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
    assert len(
        list_physical_networks_response) > 0, "No physical networks found in zone %s" % zoneid

    physical_network = list_physical_networks_response[0]

    networks = list_networks(apiclient, zoneid=zoneid)
    usedVlanIds = []

    if isinstance(networks, list) and len(networks) > 0:
        usedVlanIds = [int(nw.broadcasturi.split("//")[-1])
                       for nw in networks if (nw.broadcasturi and nw.broadcasturi.lower() != "vlan://untagged")]

    ipranges = list_vlan_ipranges(apiclient, zoneid=zoneid)
    if isinstance(ipranges, list) and len(ipranges) > 0:
        usedVlanIds += [int(iprange.vlan.split("/")[-1])
                        for iprange in ipranges if (iprange.vlan and iprange.vlan.split("/")[-1].lower() != "untagged")]

    if not hasattr(physical_network, "vlan"):
        while True:
            shared_ntwk_vlan = random.randrange(1, 4095)
            if shared_ntwk_vlan in usedVlanIds:
                continue
            else:
                break
    else:
        vlans = xsplit(physical_network.vlan, ['-', ','])

        assert len(vlans) > 0
        assert int(vlans[0]) < int(
            vlans[-1]), "VLAN range  %s was improperly split" % physical_network.vlan

        # Assuming random function will give different integer each time
        retriesCount = 20

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
    assert len(
        list_physical_networks_response) > 0, "No physical networks found in zone %s" % zoneid

    for physical_network in list_physical_networks_response:
        if not hasattr(physical_network, 'vlan'):
            continue

        vlans = xsplit(physical_network.vlan, ['-', ','])

        assert len(vlans) > 0
        assert int(vlans[0]) < int(
            vlans[-1]), "VLAN range  %s was improperly split" % physical_network.vlan

        # Keep some gap between existing vlan and the new vlans which we are going to add
        # So that they are non contiguous

        non_contig_end_vlan_id = int(vlans[-1]) + 6
        non_contig_start_vlan_id = int(vlans[0]) - 6

        # Form ranges which are consecutive to existing ranges but not immediately contiguous
        # There should be gap in between existing range and new non contiguous
        # ranage

        # If you can't add range after existing range, because it's crossing 4095, then
        # select VLAN ids before the existing range such that they are greater than 0, and
        # then add this non contiguoud range
        vlan = {"partial_range": ["", ""], "full_range": ""}

        if non_contig_end_vlan_id < 4095:
            vlan["partial_range"][0] = str(
                non_contig_end_vlan_id - 4) + '-' + str(non_contig_end_vlan_id - 3)
            vlan["partial_range"][1] = str(
                non_contig_end_vlan_id - 1) + '-' + str(non_contig_end_vlan_id)
            vlan["full_range"] = str(
                non_contig_end_vlan_id - 4) + '-' + str(non_contig_end_vlan_id)
            NonContigVlanIdsAcquired = True

        elif non_contig_start_vlan_id > 0:
            vlan["partial_range"][0] = str(
                non_contig_start_vlan_id) + '-' + str(non_contig_start_vlan_id + 1)
            vlan["partial_range"][1] = str(
                non_contig_start_vlan_id + 3) + '-' + str(non_contig_start_vlan_id + 4)
            vlan["full_range"] = str(
                non_contig_start_vlan_id) + '-' + str(non_contig_start_vlan_id + 4)
            NonContigVlanIdsAcquired = True

        else:
            NonContigVlanIdsAcquired = False

        # If failed to get relevant vlan ids, continue to next physical network
        # else break from loop as we have hot the non contiguous vlan ids for
        # the test purpose

        if not NonContigVlanIdsAcquired:
            continue
        else:
            break

    # If even through looping from all existing physical networks, failed to get relevant non
    # contiguous vlan ids, then fail the test case

    if not NonContigVlanIdsAcquired:
        return None, None

    return physical_network, vlan

def isIpInDesiredState(apiclient, ipaddressid, state):
    """ Check if the given IP is in the correct state (given)
    and return True/False accordingly"""
    retriesCount = 10
    ipInDesiredState = False
    exceptionOccurred = False
    exceptionMessage = ""
    try:
        while retriesCount >= 0:
            portableips = PublicIPAddress.list(apiclient, id=ipaddressid)
            assert validateList(
                portableips)[0] == PASS, "IPs list validation failed"
            if str(portableips[0].state).lower() == state:
                ipInDesiredState = True
                break
            retriesCount -= 1
            time.sleep(60)
    except Exception as e:
        exceptionOccurred = True
        exceptionMessage = e
        return [exceptionOccurred, ipInDesiredState, e]
    if not ipInDesiredState:
        exceptionMessage = "Ip should be in %s state, it is in %s" %\
                            (state, portableips[0].state)
    return [False, ipInDesiredState, exceptionMessage]

def setSharedNetworkParams(networkServices, range=20):
    """Fill up the services dictionary for shared network using random subnet"""

    # @range: range decides the endip. Pass the range as "x" if you want the difference between the startip
    # and endip as "x"
    # Set the subnet number of shared networks randomly prior to execution
    # of each test case to avoid overlapping of ip addresses
    shared_network_subnet_number = random.randrange(1,254)

    networkServices["gateway"] = "172.16."+str(shared_network_subnet_number)+".1"
    networkServices["startip"] = "172.16."+str(shared_network_subnet_number)+".2"
    networkServices["endip"] = "172.16."+str(shared_network_subnet_number)+"."+str(range+1)
    networkServices["netmask"] = "255.255.255.0"
    return networkServices

def createEnabledNetworkOffering(apiclient, networkServices):
    """Create and enable network offering according to the type

       @output: List, containing [ Result,Network Offering,Reason ]
                 Ist Argument('Result') : FAIL : If exception or assertion error occurs
                                          PASS : If network offering
                                          is created and enabled successfully
                 IInd Argument(Net Off) : Enabled network offering
                                                In case of exception or
                                                assertion error, it will be None
                 IIIrd Argument(Reason) :  Reason for failure,
                                              default to None
    """
    try:
        resultSet = [FAIL, None, None]
        # Create network offering
        network_offering = NetworkOffering.create(apiclient, networkServices, conservemode=False)

        # Update network offering state from disabled to enabled.
        NetworkOffering.update(network_offering, apiclient, id=network_offering.id,
                               state="enabled")
    except Exception as e:
        resultSet[2] = e
        return resultSet
    return [PASS, network_offering, None]

def shouldTestBeSkipped(networkType, zoneType):
    """Decide which test to skip, according to type of network and zone type"""

    # If network type is isolated or vpc and zone type is basic, then test should be skipped
    skipIt = False
    if ((networkType.lower() == str(ISOLATED_NETWORK).lower() or networkType.lower() == str(VPC_NETWORK).lower())
            and (zoneType.lower() == BASIC_ZONE)):
        skipIt = True
    return skipIt

def verifyNetworkState(apiclient, networkid, state, listall=True):
    """List networks and check if the network state matches the given state"""
    retriesCount = 10
    isNetworkInDesiredState = False
    exceptionOccurred = False
    exceptionMessage = ""
    try:
        while retriesCount >= 0:
            networks = Network.list(apiclient, id=networkid, listall=listall)
            assert validateList(
                networks)[0] == PASS, "Networks list validation failed"
            if str(networks[0].state).lower() == state:
                isNetworkInDesiredState = True
                break
            retriesCount -= 1
            time.sleep(60)
        if not isNetworkInDesiredState:
            exceptionMessage = "Network state should be %s, it is %s" %\
                                (state, networks[0].state)
    except Exception as e:
        exceptionOccurred = True
        exceptionMessage = e
        return [exceptionOccurred, isNetworkInDesiredState, exceptionMessage]
    return [exceptionOccurred, isNetworkInDesiredState, exceptionMessage]

def verifyComputeOfferingCreation(apiclient, computeofferingid):
    """List Compute offerings by ID and verify that the offering exists"""

    cmd = listServiceOfferings.listServiceOfferingsCmd()
    cmd.id = computeofferingid
    serviceOfferings = None
    try:
        serviceOfferings = apiclient.listServiceOfferings(cmd)
    except Exception:
       return FAIL
    if not (isinstance(serviceOfferings, list) and len(serviceOfferings) > 0):
       return FAIL
    return PASS

def createNetworkRulesForVM(apiclient, virtualmachine, ruletype,
                            account, networkruledata):
    """Acquire IP, create Firewall and NAT/StaticNAT rule
        (associating it with given vm) for that IP"""

    try:
        public_ip = PublicIPAddress.create(
                apiclient,accountid=account.name,
                zoneid=virtualmachine.zoneid,domainid=account.domainid,
                networkid=virtualmachine.nic[0].networkid)

        FireWallRule.create(
            apiclient,ipaddressid=public_ip.ipaddress.id,
            protocol='TCP', cidrlist=[networkruledata["fwrule"]["cidr"]],
            startport=networkruledata["fwrule"]["startport"],
            endport=networkruledata["fwrule"]["endport"]
            )

        if ruletype == NAT_RULE:
            # Create NAT rule
            NATRule.create(apiclient, virtualmachine,
                                 networkruledata["natrule"],ipaddressid=public_ip.ipaddress.id,
                                 networkid=virtualmachine.nic[0].networkid)
        elif ruletype == STATIC_NAT_RULE:
            # Enable Static NAT for VM
            StaticNATRule.enable(apiclient,public_ip.ipaddress.id,
                                     virtualmachine.id, networkid=virtualmachine.nic[0].networkid)
    except Exception as e:
        [FAIL, e]
    return [PASS, public_ip]

def getPortableIpRangeServices(config):
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
        services = FAILED

    return services


def uploadVolume(apiclient, zoneid, account, services):
    try:
        # Upload the volume
        volume = Volume.upload(apiclient, services["volume"],
                                   zoneid=zoneid, account=account.name,
                                   domainid=account.domainid, url=services["url"])

        volume.wait_for_upload(apiclient)

        # Check List Volume response for newly created volume
        volumes = Volume.list(apiclient, id=volume.id,
                                  zoneid=zoneid, listall=True)
        validationresult = validateList(volumes)
        assert validationresult[0] == PASS,\
                            "volumes list validation failed: %s" % validationresult[2]
        assert str(volumes[0].state).lower() == "uploaded",\
                    "Volume state should be 'uploaded' but it is %s" % volumes[0].state
    except Exception as e:
        return [FAIL, e]
    return [PASS, volume]

def matchResourceCount(apiclient, expectedCount, resourceType,
                              accountid=None, projectid=None):
    """Match the resource count of account/project with the expected
    resource count"""
    expected = int(expectedCount)  # initialise as int to make sure floats passed are acceptable
    try:
        resourceholderlist = None
        if accountid:
            resourceholderlist = Account.list(apiclient, id=accountid)
        elif projectid:
            resourceholderlist = Project.list(apiclient, id=projectid, listall=True)
        validationresult = validateList(resourceholderlist)
        assert validationresult[0] == PASS,\
               "accounts list validation failed"
        if resourceType == RESOURCE_PRIMARY_STORAGE:
            resourceCount = resourceholderlist[0].primarystoragetotal
        elif resourceType == RESOURCE_SECONDARY_STORAGE:
            resourceCount = resourceholderlist[0].secondarystoragetotal
            expected = expectedCount  # as the exception, an original value is needed here (should be of type float)
        elif resourceType == RESOURCE_CPU:
            resourceCount = resourceholderlist[0].cputotal
        elif resourceType == RESOURCE_MEMORY:
            resourceCount = resourceholderlist[0].memorytotal
        elif resourceType == RESOURCE_USER_VM:
            resourceCount = resourceholderlist[0].vmtotal
        elif resourceType == RESOURCE_PUBLIC_IP:
            resourceCount = resourceholderlist[0].iptotal
        elif resourceType == RESOURCE_VOLUME:
            resourceCount = resourceholderlist[0].volumetotal
        elif resourceType == RESOURCE_SNAPSHOT:
            resourceCount = resourceholderlist[0].snapshottotal
        elif resourceType == RESOURCE_TEMPLATE:
            resourceCount = resourceholderlist[0].templatetotal
        elif resourceType == RESOURCE_NETWORK:
            resourceCount = resourceholderlist[0].networktotal
        elif resourceType == RESOURCE_VPC:
            resourceCount = resourceholderlist[0].vpctotal
        assert str(resourceCount) == str(expected),\
                "Resource count %s should match with the expected resource count %s" %\
                (resourceCount, expectedCount)
    except Exception as e:
        return [FAIL, e]
    return [PASS, None]

def createSnapshotFromVirtualMachineVolume(apiclient, account, vmid):
    """Create snapshot from volume"""

    try:
        volumes = Volume.list(apiclient, account=account.name,
                              domainid=account.domainid, virtualmachineid=vmid)
        validationresult = validateList(volumes)
        assert validateList(volumes)[0] == PASS,\
                            "List volumes should return a valid response"
        snapshot = Snapshot.create(apiclient, volume_id=volumes[0].id,
                                   account=account.name, domainid=account.domainid)
        snapshots = Snapshot.list(apiclient, id=snapshot.id,
                                      listall=True)
        validationresult = validateList(snapshots)
        assert validationresult[0] == PASS,\
               "List snapshot should return a valid list"
    except Exception as e:
       return[FAIL, e]
    return [PASS, snapshot]

def isVmExpunged(apiclient, vmid, projectid=None, timeout=600):
    """Verify if VM is expunged or not"""
    vmExpunged= False
    while timeout>=0:
        try:
            vms = VirtualMachine.list(apiclient, id=vmid, projectid=projectid)
            if vms is None:
                vmExpunged = True
                break
            timeout -= 60
            time.sleep(60)
        except Exception:
            vmExpunged = True
            break
     #end while
    return vmExpunged

def isDomainResourceCountEqualToExpectedCount(apiclient, domainid, expectedcount,
                                              resourcetype):
    """Get the resource count of specific domain and match
    it with the expected count
    Return list [isExceptionOccurred, reasonForException, isResourceCountEqual]"""
    isResourceCountEqual = False
    isExceptionOccurred = False
    reasonForException = None
    try:
        response = Resources.updateCount(apiclient, domainid=domainid,
                                         resourcetype=resourcetype)
    except Exception as e:
        reasonForException = "Failed while updating resource count: %s" % e
        isExceptionOccurred = True
        return [isExceptionOccurred, reasonForException, isResourceCountEqual]

    if resourcetype == RESOURCE_PRIMARY_STORAGE or resourcetype == RESOURCE_SECONDARY_STORAGE:
        resourcecount = (response[0].resourcecount / (1024**3))
    else:
        resourcecount = response[0].resourcecount

    if resourcecount == expectedcount:
        isResourceCountEqual = True
    return [isExceptionOccurred, reasonForException, isResourceCountEqual]

def isAccountResourceCountEqualToExpectedCount(apiclient, domainid, account, expectedcount,
                                              resourcetype):
    """Get the resource count of specific account and match
    it with the expected count
    Return list [isExceptionOccurred, reasonForException, isResourceCountEqual]"""
    isResourceCountEqual = False
    isExceptionOccurred = False
    reasonForException = None
    try:
        response = Resources.updateCount(apiclient, domainid=domainid, account=account,
                                         resourcetype=resourcetype)
    except Exception as e:
        reasonForException = "Failed while updating resource count: %s" % e
        isExceptionOccurred = True
        return [isExceptionOccurred, reasonForException, isResourceCountEqual]

    if resourcetype == RESOURCE_PRIMARY_STORAGE or resourcetype == RESOURCE_SECONDARY_STORAGE:
        resourcecount = (response[0].resourcecount / (1024**3))
    else:
        resourcecount = response[0].resourcecount

    if resourcecount == expectedcount:
        isResourceCountEqual = True
    return [isExceptionOccurred, reasonForException, isResourceCountEqual]

def isNetworkDeleted(apiclient, networkid, timeout=600):
    """ List the network and check that the list is empty or not"""
    networkDeleted = False
    while timeout >= 0:
        networks = Network.list(apiclient, id=networkid)
        if networks is None:
            networkDeleted = True
            break
        timeout -= 60
        time.sleep(60)
    #end while
    return networkDeleted


def createChecksum(service=None,
                   virtual_machine=None,
                   disk=None,
                   disk_type=None):

    """ Calculate the MD5 checksum of the disk by writing \
		data on the disk where disk_type is either root disk or data disk
	@return: returns the calculated checksum"""

    random_data_0 = random_gen(size=100)
    # creating checksum(MD5)
    m = hashlib.md5()
    m.update(random_data_0)
    ckecksum_random_data_0 = m.hexdigest()
    try:
        ssh_client = SshClient(
            virtual_machine.ssh_ip,
            virtual_machine.ssh_port,
            virtual_machine.username,
            virtual_machine.password
        )
    except Exception:
        raise Exception("SSH access failed for server with IP address: %s" %
                    virtual_machine.ssh_ip)

    # Format partition using ext3

    format_volume_to_ext3(
        ssh_client,
        service["volume_write_path"][
            virtual_machine.hypervisor.lower()][disk_type]
    )
    cmds = ["fdisk -l",
            "mkdir -p %s" % service["data_write_paths"]["mount_dir"],
            "mount -t ext3 %s1 %s" % (
                service["volume_write_path"][
                    virtual_machine.hypervisor.lower()][disk_type],
                service["data_write_paths"]["mount_dir"]
            ),
            "mkdir -p %s/%s/%s " % (
                service["data_write_paths"]["mount_dir"],
                service["data_write_paths"]["sub_dir"],
                service["data_write_paths"]["sub_lvl_dir1"],
            ),
            "echo %s > %s/%s/%s/%s" % (
                random_data_0,
                service["data_write_paths"]["mount_dir"],
                service["data_write_paths"]["sub_dir"],
                service["data_write_paths"]["sub_lvl_dir1"],
                service["data_write_paths"]["random_data"]
            ),
            "cat %s/%s/%s/%s" % (
                service["data_write_paths"]["mount_dir"],
                service["data_write_paths"]["sub_dir"],
                service["data_write_paths"]["sub_lvl_dir1"],
                service["data_write_paths"]["random_data"]
            )
            ]

    for c in cmds:
        ssh_client.execute(c)

    # Unmount the storage
    cmds = [
        "umount %s" % (service["data_write_paths"]["mount_dir"]),
    ]

    for c in cmds:
        ssh_client.execute(c)

    return ckecksum_random_data_0


def compareChecksum(
        apiclient,
        service=None,
        original_checksum=None,
        disk_type=None,
        virt_machine=None
        ):
    """
    Create md5 checksum of the data present on the disk and compare
    it with the given checksum
    """
    if virt_machine.state != "Running":
        virt_machine.start(apiclient)

    try:
        # Login to VM to verify test directories and files
        ssh = SshClient(
            virt_machine.ssh_ip,
            virt_machine.ssh_port,
            virt_machine.username,
            virt_machine.password
        )
    except Exception:
        raise Exception("SSH access failed for server with IP address: %s" %
                    virt_machine.ssh_ip)

    # Mount datadiskdevice_1 because this is the first data disk of the new
    # virtual machine
    cmds = ["blkid",
            "fdisk -l",
            "mkdir -p %s" % service["data_write_paths"]["mount_dir"],
            "mount -t ext3 %s1 %s" % (
                service["volume_write_path"][
                    virt_machine.hypervisor.lower()][disk_type],
                service["data_write_paths"]["mount_dir"]
            ),
            ]

    for c in cmds:
        ssh.execute(c)

    returned_data_0 = ssh.execute(
        "cat %s/%s/%s/%s" % (
            service["data_write_paths"]["mount_dir"],
            service["data_write_paths"]["sub_dir"],
            service["data_write_paths"]["sub_lvl_dir1"],
            service["data_write_paths"]["random_data"]
        ))

    n = hashlib.md5()
    n.update(returned_data_0[0])
    ckecksum_returned_data_0 = n.hexdigest()

    # Verify returned data
    assert original_checksum == ckecksum_returned_data_0, \
        "Cheskum does not match with checksum of original data"

    # Unmount the Sec Storage
    cmds = [
        "umount %s" % (service["data_write_paths"]["mount_dir"]),
    ]

    for c in cmds:
        ssh.execute(c)

    return



def verifyRouterState(apiclient, routerid, state, listall=True):
    """List router and check if the router state matches the given state"""
    retriesCount = 10
    isRouterInDesiredState = False
    exceptionOccurred = False
    exceptionMessage = ""
    try:
        while retriesCount >= 0:
            routers = Router.list(apiclient, id=routerid, listall=listall)
            assert validateList(
                routers)[0] == PASS, "Routers list validation failed"
            if str(routers[0].state).lower() == state:
                isRouterInDesiredState = True
                break
            retriesCount -= 1
            time.sleep(60)
        if not isRouterInDesiredState:
            exceptionMessage = "Router state should be %s, it is %s" %\
                                (state, routers[0].state)
    except Exception as e:
        exceptionOccurred = True
        exceptionMessage = e
        return [exceptionOccurred, isRouterInDesiredState, exceptionMessage]
    return [exceptionOccurred, isRouterInDesiredState, exceptionMessage]

def isIpRangeInUse(api_client, publicIpRange):
    ''' Check that if any Ip in the IP Range is in use
        currently
    '''

    vmList = VirtualMachine.list(api_client,
                                 zoneid=publicIpRange.zoneid,
                                 listall=True)
    if not vmList:
        return False

    for vm in vmList:
        for nic in vm.nic:
            publicIpAddresses = PublicIPAddress.list(api_client,
                                                 associatednetworkid=nic.networkid,
                                                 listall=True)
            if validateList(publicIpAddresses)[0] == PASS:
                for ipaddress in publicIpAddresses:
                    if IPAddress(publicIpRange.startip) <=\
                        IPAddress(ipaddress.ipaddress) <=\
                        IPAddress(publicIpRange.endip):
                        return True
    return False

def verifyGuestTrafficPortGroups(api_client, config, setup_zone):

    """ This function matches the given zone with
    the zone in config file used to deploy the setup and
    retrieves the corresponding vcenter details and forms
    the vcenter connection object. It makes call to
    verify the guest traffic for given zone """

    try:
        zoneDetailsInConfig = [zone for zone in config.zones
                if zone.name == setup_zone.name][0]
        vcenterusername = zoneDetailsInConfig.vmwaredc.username
        vcenterpassword = zoneDetailsInConfig.vmwaredc.password
        vcenterip = zoneDetailsInConfig.vmwaredc.vcenter
        vcenterObj = Vcenter(
                vcenterip,
                vcenterusername,
                vcenterpassword)
        response = verifyVCenterPortGroups(
                api_client,
                vcenterObj,
                traffic_types_to_validate=[
                    GUEST_TRAFFIC],
                zoneid=setup_zone.id,
                switchTypes=[VMWAREDVS])
        assert response[0] == PASS, response[1]
    except Exception as e:
        return [FAIL, e]
    return [PASS, None]

def analyzeTrafficType(trafficTypes, trafficTypeToFilter, switchTypes=None):
    """ Analyze traffic types for given type and return
        switch name and vlan Id from the
        vmwarenetworklabel string of trafficTypeToFilter
    """

    try:
        filteredList = [trafficType for trafficType in trafficTypes
                        if trafficType.traffictype.lower() ==
                        trafficTypeToFilter]

        if not filteredList:
            return [PASS, filteredList, None, None]

        # Split string with , so as to extract the  switch Name and
        # vlan ID
        splitString = str(
            filteredList[0].vmwarenetworklabel).split(",")
        switchName = splitString[0]
        vlanSpecified = splitString[1]
        availableSwitchType = splitString[2]

        if switchTypes and availableSwitchType.lower() not in switchTypes:
            return [PASS, None, None, None]

        return [PASS, filteredList, switchName, vlanSpecified]
    except Exception as e:
        return [FAIL, e, None, None]


def getExpectedPortGroupNames(
        api_client,
        physical_network,
        network_rate,
        switch_name,
        traffic_types,
        switch_dict,
        vcenter_conn,
        specified_vlan,
        traffic_type):
    """ Return names of expected port groups that should be
        present in vcenter

        Parameters:
        @physical_network: Physical Network of the @traffic_type
        @network_rate:     as defined by network.throttling.rate
        @switch_name:      Name of the switch used by the traffic in
                           vcenter
        @traffic_types:    List of all traffic types present in the physical
                           network
        @switch_dict:      Dictionary containing switch information in vcenter
        @vcenter_conn:     vcenter connection object used to fetch information
                           from vcenter
        @specified_vlan:   The vlan for @traffic_type
        @traffic_type:     Traffic type for which the port names are to be
                           returned

        Return value:
        [PASS/FAIL, exception object if FAIL else expected port group names
         for @traffic_type]
        """

    try:
        expectedDVPortGroupNames = []

        if traffic_type == PUBLIC_TRAFFIC:
            publicIpRanges = PublicIpRange.list(
                api_client,
                physicalnetworkid=physical_network.id
            )
            if publicIpRanges is not None:
                for publicIpRange in publicIpRanges:
                    vlanInIpRange = re.findall(
                        '\d+',
                        str(publicIpRange.vlan))
                    vlanId = "untagged"
                    if len(vlanInIpRange) > 0:
                        vlanId = vlanInIpRange[0]
                    ipRangeInUse = isIpRangeInUse(api_client, publicIpRange)
                    if ipRangeInUse:
                        expectedDVPortGroupName = "cloud" + "." + \
                                    PUBLIC_TRAFFIC + "." + vlanId + "." + \
                                    network_rate + "." + "1" + "-" + \
                                    switch_name
                        expectedDVPortGroupNames.append(
                                    expectedDVPortGroupName)

                    expectedDVPortGroupName = "cloud" + "." + PUBLIC_TRAFFIC + "." + \
                            vlanId + "." + "0" + "." + "1" + "-" + switch_name
                    expectedDVPortGroupNames.append(expectedDVPortGroupName)

        if traffic_type == GUEST_TRAFFIC:

            networks = Network.list(
                api_client,
                physicalnetworkid=physical_network.id,
                listall=True
            )
            if networks is not None:
                for network in networks:
                    networkVlan = re.findall(
                        '\d+', str(network.vlan))
                    if len(networkVlan) > 0:
                        vlanId = networkVlan[0]
                        expectedDVPortGroupName = "cloud" + "." + GUEST_TRAFFIC + "." + \
                            vlanId + "." + network_rate + "." + "1" + "-" + \
                            switch_name
                        expectedDVPortGroupNames.append(
                            expectedDVPortGroupName)

        if traffic_type == STORAGE_TRAFFIC:
            vlanId = ""
            storageIpRanges = StorageNetworkIpRange.list(
                api_client,
                zoneid=physical_network.zoneid
            )
            if storageIpRanges is not None:
                for storageIpRange in storageIpRanges:
                    vlanInIpRange = re.findall(
                        '\d+',
                        str(storageIpRange.vlan))
                    if len(vlanInIpRange) > 0:
                        vlanId = vlanInIpRange[0]
                    else:
                        vlanId = "untagged"
                    expectedDVPortGroupName = "cloud" + "." + STORAGE_TRAFFIC + \
                        "." + vlanId + "." + "0" + "." + "1" + "-" + \
                        switch_name
                    expectedDVPortGroupNames.append(
                        expectedDVPortGroupName)

            else:
                response = analyzeTrafficType(
                    traffic_types, MANAGEMENT_TRAFFIC)
                assert response[0] == PASS, response[1]
                filteredList, switchName, vlanSpecified =\
                    response[1], response[2], response[3]

                if not filteredList:
                    raise Exception("No Management traffic present and\
                                Storage traffic does not have any IP range,\
                                Invalid zone setting")

                if switchName not in switch_dict:
                    dvswitches = vcenter_conn.get_dvswitches(
                        name=switchName)
                    switch_dict[switchName] = dvswitches[0][
                        'dvswitch']['portgroupNameList']

                if vlanSpecified:
                    vlanId = vlanSpecified
                else:
                    vlanId = "untagged"
                expectedDVPortGroupName = "cloud" + "." + STORAGE_TRAFFIC + \
                    "." + vlanId + "." + "0" + "." + "1" + "-" + switchName
                expectedDVPortGroupNames.append(expectedDVPortGroupName)

        if traffic_type == MANAGEMENT_TRAFFIC:
            vlanId = "untagged"
            if specified_vlan:
                vlanId = specified_vlan
            expectedDVPortGroupName = "cloud" + "." + "private" + "." + \
                vlanId + "." + "0" + "." + "1" + "-" + switch_name
            expectedDVPortGroupNames.append(expectedDVPortGroupName)

    except Exception as e:
        return [FAIL, e]
    return [PASS, expectedDVPortGroupNames]


def verifyVCenterPortGroups(
        api_client,
        vcenter_conn,
        zoneid,
        traffic_types_to_validate,
        switchTypes):

    """ Generate expected port groups for given traffic types and
        verify they are present in the vcenter

        Parameters:
        @api_client:    API client of root admin account
        @vcenter_conn:  connection object for vcenter used to fetch data
                        using vcenterAPI
        @zone_id:       Zone for which port groups are to be verified
        @traffic_types_to_validate:
                        Traffic types (public, guest, management, storage) for
                        which verification is to be done
        @switchTypes:   The switch types for which port groups
                        are to be verified e.g vmwaredvs

        Return value:
        [PASS/FAIL, exception message if FAIL else None]
    """
    try:
        expectedDVPortGroupNames = []
        vcenterPortGroups = []
        config = Configurations.list(
            api_client,
            name="network.throttling.rate"
        )
        networkRate = config[0].value
        switchDict = {}
        physicalNetworks = PhysicalNetwork.list(
                api_client,
                zoneid=zoneid
            )

        # If there are no physical networks in zone, return as PASS
        # as there are no validations to make
        if validateList(physicalNetworks)[0] != PASS:
            return [PASS, None]

        for physicalNetwork in physicalNetworks:
            trafficTypes = TrafficType.list(
                    api_client,
                    physicalnetworkid=physicalNetwork.id)

            for trafficType in traffic_types_to_validate:
                response = analyzeTrafficType(
                        trafficTypes, trafficType, switchTypes)
                assert response[0] == PASS, response[1]
                filteredList, switchName, vlanSpecified=\
                        response[1], response[2], response[3]

                if not filteredList:
                    continue

                if switchName not in switchDict:
                    dvswitches = vcenter_conn.get_dvswitches(
                            name=switchName)
                    switchDict[switchName] = dvswitches[0][
                            'dvswitch']['portgroupNameList']

                response = getExpectedPortGroupNames(
                        api_client,
                        physicalNetwork,
                        networkRate,
                        switchName,
                        trafficTypes,
                        switchDict,
                        vcenter_conn,
                        vlanSpecified,
                        trafficType)
                assert response[0] == PASS, response[1]
                dvPortGroups = response[1]
                expectedDVPortGroupNames.extend(dvPortGroups)

        vcenterPortGroups = list(itertools.chain(*(list(switchDict.values()))))

        for expectedDVPortGroupName in expectedDVPortGroupNames:
            assert expectedDVPortGroupName in vcenterPortGroups,\
                "Port group %s not present in VCenter DataCenter" %\
                expectedDVPortGroupName

    except Exception as e:
        return [FAIL, e]
    return [PASS, None]

def migrate_router(apiclient, router_id, host_id):
    cmd = migrateSystemVm.migrateSystemVmCmd()
    cmd.hostid = host_id
    cmd.virtualmachineid = router_id

    apiclient.migrateSystemVm(cmd)
