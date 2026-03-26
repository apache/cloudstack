#!/cygdrive/c/Python27
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

import os.path
import sys
from xml.dom import minidom
from xml.parsers.expat import ExpatError
import difflib


ROOT_ADMIN = 'r'

user_to_func = {
    ROOT_ADMIN: 'populateForApi',
    }


user_to_cns = {
    ROOT_ADMIN: 'allCommandNames',
    }


dirname_to_user = {
    'apis': ROOT_ADMIN,
    }


dirname_to_dirname = {
    'apis': 'apis',
    }


known_categories = {
    # Category definition format: api keyword or api name: category
    'Cisco' : 'External Device',
    'SystemVm': 'System VM',
    'VirtualMachine': 'Virtual Machine',
    'VM': 'Virtual Machine',
    'Vnf': 'Virtual Network Functions',
    'VnfTemplate': 'Virtual Network Functions',
    'GuestSubnet': 'Routing',
    'GuestOs': 'Guest OS',
    'Domain': 'Domain',
    'Template': 'Template',
    'Iso': 'ISO',
    'Volume': 'Volume',
    'Vlan': 'VLAN',
    'IpAddress': 'Address',
    'PortForwarding': 'Firewall',
    'Firewall': 'Firewall',
    'StaticNat': 'NAT',
    'IpForwarding': 'NAT',
    'Host': 'Host',
    'HostTags': 'Host',
    'cancelHostMaintenance': 'Host',
    'prepareHostForMaintenance': 'Host',
    'startRollingMaintenance': 'Host',
    'OutOfBandManagement': 'Out-of-band Management',
    'Cluster': 'Cluster',
    'Account': 'Account',
    'Role': 'Role',
    'VMSnapshot': 'VM Snapshot',
    'Snapshot': 'Snapshot',
    'User': 'User',
    'UserData': 'User Data',
    'Os': 'Guest OS',
    'ServiceOffering': 'Service Offering',
    'DiskOffering': 'Disk Offering',
    'LoadBalancer': 'Load Balancer',
    'SslCert': 'SSL Certificates',
    'Router': 'Router',
    'Configuration': 'Configuration',
    'Capabilities': 'Configuration',
    'Pod': 'Pod',
    'ManagementNetworkIpRange': 'Pod',
    'PublicIpRange': 'Network',
    'Zone': 'Zone',
    'VmwareDc' : 'Zone',
    'NetworkOffering': 'Network Offering',
    'NetworkACL': 'Network ACL',
    'Network': 'Network',
    'CiscoNexusVSM': 'Cisco Nexus VSM',
    'OpenDaylightController': 'OpenDaylight Controller',
    'createServiceInstance': 'Network',
    'addGloboDnsHost': 'Network',
    'TungstenFabric': 'Tungsten',
    'NsxController': 'NSX',
    'NetrisProvider': 'Netris',
    'Vpn': 'VPN',
    'Limit': 'Resource Limit',
    'Netscaler': 'Netscaler',
    'SolidFire': 'SolidFire',
    'PaloAlto': 'Palo Alto',
    'ResourceCount': 'Resource Limit',
    'CloudIdentifier': 'Cloud Identifier',
    'InstanceGroup': 'VM Group',
    'StorageMaintenance': 'Storage Pool',
    'StoragePool': 'Storage Pool',
    'StorageTags': 'Storage Pool',
    'StorageProvider': 'Storage Pool',
    'StorageScope' : 'Storage Pool',
    'updateStorageCapabilities' : 'Storage Pool',
    'SecurityGroup': 'Security Group',
    'SSHKeyPair': 'SSH KeyPair',
    'AsyncJob': 'Async Job',
    'Certificate': 'Certificate',
    'Hypervisor': 'Configuration',
    'Alert': 'Alert',
    'Event': 'Event',
    'login': 'Authentication',
    'logout': 'Authentication',
    'saml': 'Authentication',
    'getSPMetadata': 'Authentication',
    'listIdps': 'Authentication',
    'oauthlogin': 'Authentication',
    'OauthProvider': 'OAuth',
    'quota': 'Quota',
    'emailTemplate': 'Quota',
    'Capacity': 'System Capacity',
    'NetworkDevice': 'Network Device',
    'ExternalLoadBalancer': 'Ext Load Balancer',
    'ExternalFirewall': 'Ext Firewall',
    'Usage': 'Usage',
    'TrafficMonitor': 'Network',
    'TrafficType': 'Network',
    'Product': 'Product',
    'LB': 'Load Balancer',
    'Ldap': 'LDAP',
    'Swift': 'Image Store',
    'S3' : 'S3',
    'SecondaryStorage': 'Image Store',
    'Project': 'Project',
    'Lun': 'Storage',
    'Pool': 'Pool',
    'VPC': 'VPC',
    'VPCOffering': 'VPC Offering',
    'PrivateGateway': 'VPC',
    'migrateVpc': 'VPC',
    'Simulator': 'Simulator',
    'StaticRoute': 'VPC',
    'Tags': 'Resource Tags',
    'Icon': 'Resource Icon',
    'NiciraNvpDevice': 'Nicira NVP',
    'BrocadeVcsDevice': 'Brocade VCS',
    'BigSwitchBcfDevice': 'BigSwitch BCF',
    'AutoScale': 'AutoScale',
    'Counter': 'AutoScale',
    'Condition': 'AutoScale',
    'Api': 'API Discovery',
    'ApiLimit': 'Configuration',
    'Region': 'Region',
    'Detail': 'Resource Metadata',
    'addIpToNic': 'NIC',
    'removeIpFromNic': 'NIC',
    'updateVmNicIp': 'NIC',
    'listNics':'NIC',
    'AffinityGroup': 'Affinity Group',
    'ImageStore': 'Image Store',
    'SecondaryStagingStore': 'Image Store',
    'InternalLoadBalancer': 'Internal LB',
	'DeploymentPlanners': 'Configuration',
	'ObjectStore': 'Image Store',
    'PortableIp': 'Portable IP',
    'dedicateHost': 'Dedicate Resources',
    'releaseDedicatedHost': 'Dedicate Resources',
    'Baremetal' : 'Baremetal',
    'UCS' : 'UCS',
    'CacheStore' : 'Cache Store',
    'OvsElement' : 'Ovs Element',
    'StratosphereSsp' : 'Stratosphere SSP',
    'Metrics' : 'Metrics',
    'VpnUser': 'VPN',
    'Infrastructure' : 'Metrics',
    'listRegisteredServicePackages': 'Load Balancer',
    'NsVpx' : 'Load Balancer',
    'NetscalerVpx' : 'Load Balancer',
    'deleteServicePackageOffering' : 'Load Balancer',
    'Annotation' : 'Annotations',
    'CA': 'Certificate',
    'Elastistor': 'Elastistor',
    'cloudian': 'Cloudian',
    'Sioc' : 'Sioc',
    'Diagnostics': 'Diagnostics',
    'ManagementServer': 'Management Server',
    'Backup' : 'Backup and Recovery',
    'Restore' : 'Backup and Recovery',
    'UnmanagedInstance': 'Virtual Machine',
    'Kubernetes': 'Kubernetes Service',
    'Rolling': 'Rolling Maintenance',
    'vsphereStoragePolicy' : 'vSphere Storage Policies',
    'vsphereStoragePolicies' : 'vSphere Storage Policies',
    'createConsoleEndpoint': 'Console Session',
    'listConsoleSessions': 'Console Session',
    'importVm': 'Virtual Machine',
    'ImportVmTask': 'Virtual Machine',
    'listVmsForImport': 'Virtual Machine',
    'revertToVMSnapshot': 'Virtual Machine',
    'QuarantinedIp': 'IP Quarantine',
    'Shutdown': 'Management Server',
    'Maintenance': 'Management Server',
    'ObjectStoragePool': 'Object Store',
    'Bucket': 'Object Store',
    'SharedFS': 'Shared FileSystem',
    'SharedFileSystem': 'Shared FileSystem',
    'Webhook': 'Webhook',
    'purgeExpungedResources': 'Resource',
    'forgotPassword': 'Authentication',
    'resetPassword': 'Authentication',
    'BgpPeer': 'BGP Peer',
    'changeBgpPeersForNetwork': 'BGP Peer',
    'ASNRange': 'AS Number Range',
    'ASNumber': 'AS Number',
    'StorageAccess': 'Storage Access Groups',
    'GuiTheme': 'GUI Theme',
    'GPU': 'GPU',
    'vGPU': 'GPU',
    'GPUCard': 'GPU',
    'Extension' : 'Extension',
    'CustomAction' : 'Extension'
}

categories = {}

def choose_category(fn):
    possible_known_categories = []
    for k, v in known_categories.items():
        if k.lower() in fn.lower():
            possible_known_categories.append(k)

    if len(possible_known_categories) > 0:
        close_matches = difflib.get_close_matches(fn, possible_known_categories, n=1, cutoff=0.1)
        if len(close_matches) > 0:
            return known_categories[close_matches[0]]
        else:
            return known_categories[possible_known_categories[0]]
    raise Exception('Need to add a category for %s to %s:known_categories' %
                    (fn, __file__))


for f in sys.argv:
    dirname, fn = os.path.split(f)
    if not fn.endswith('.xml'):
        continue
    if fn.endswith('Summary.xml') and fn != 'quotaSummary.xml':
        continue
    if fn.endswith('SummarySorted.xml'):
        continue
    if fn == 'alert_types.xml':
        continue
    if dirname.startswith('./'):
        dirname = dirname[2:]
    try:
        with open(f) as data:
            dom = minidom.parse(data)
        name = dom.getElementsByTagName('name')[0].firstChild.data
        isAsync = dom.getElementsByTagName('isAsync')[0].firstChild.data
        isDeprecated = dom.getElementsByTagName('isDeprecated')[0].firstChild.data
        category = choose_category(fn)
        if category not in categories:
            categories[category] = []
        categories[category].append({
            'name': name,
            'dirname': dirname_to_dirname[dirname],
            'async': isAsync == 'true',
            'deprecated': isDeprecated == 'true',
            'user': dirname_to_user[dirname],
            })
    except ExpatError as e:
        pass
    except IndexError as e:
        print(fn)


def xml_for(command):
    name = command['name']
    isAsync = command['async'] and ' (A)' or ''
    isDeprecated = command['deprecated'] and ' (D)' or ''
    dirname = command['dirname']
    return '''<xsl:if test="name=\'%(name)s\'">
<li><a href="%(dirname)s/%(name)s.html"><xsl:value-of select="name"/>%(isAsync)s %(isDeprecated)s</a></li>
</xsl:if>
''' % locals()


def write_xml(out, user):
    with open(out, 'w') as f:
        cat_strings = []
        for category in categories.keys():
            strings = []
            for command in categories[category]:
                if command['user'] == user:
                    strings.append(xml_for(command))
            if strings:
                all_strings = ''.join(strings)
                cat_strings.append((len(strings), category, all_strings))

        cat_strings.sort(reverse=True)

        i = 0
        for _1, category, all_strings in cat_strings:
            if i == 0:
                f.write('<div class="apismallsections">\n')
            f.write('''<div class="apismallbullet_box">
<h5>%(category)s</h5>
<ul>
<xsl:for-each select="commands/command">
%(all_strings)s
</xsl:for-each>
</ul>
</div>

''' % locals())
            if i == 3:
                f.write('</div>\n')
                i = 0
            else:
                i += 1
        if i != 0:
            f.write('</div>\n')


def java_for(command, user):
    name = command['name']
    cns = user_to_cns[user]
    return '''%(cns)s.add("%(name)s");
''' % locals()


def java_for_user(user):
    strings = []
    for category in categories.keys():
        for command in categories[category]:
            if command['user'] == user:
                strings.append(java_for(command, user))
    func = user_to_func[user]
    all_strings = ''.join(strings)
    return '''
    public void %(func)s() {
        %(all_strings)s
    }
''' % locals()


def write_java(out):
    with open(out, 'w') as f:
        f.write('''/* Generated using gen_toc.py.  Do not edit. */

import java.util.HashSet;
import java.util.Set;

public class XmlToHtmlConverterData {
    Set<String> allCommandNames = new HashSet<String>();
''')
        f.write(java_for_user(ROOT_ADMIN) + "\n")

        f.write('''
}

''')


write_xml('generatetoc_include.xsl', ROOT_ADMIN)
write_java('XmlToHtmlConverterData.java')
