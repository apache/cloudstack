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

import os
import os.path
import sys
from xml.dom import minidom
from xml.parsers.expat import ExpatError


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
    'Cisco' : 'External Device',
    'SystemVm': 'System VM',
    'VirtualMachine': 'Virtual Machine',
    'VM': 'Virtual Machine',
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
    'OutOfBand': 'Out-of-band Management',
    'Cluster': 'Cluster',
    'Account': 'Account',
    'Role': 'Role',
    'Snapshot': 'Snapshot',
    'User': 'User',
    'Os': 'Guest OS',
    'ServiceOffering': 'Service Offering',
    'DiskOffering': 'Disk Offering',
    'LoadBalancer': 'Load Balancer',
    'SslCert': 'Load Balancer',
    'Router': 'Router',
    'SystemVm': 'System VM',
    'Configuration': 'Configuration',
    'Capabilities': 'Configuration',
    'Pod': 'Pod',
    'PublicIpRange': 'Network',
    'Zone': 'Zone',
    'Vmware' : 'Zone',
    'NetworkOffering': 'Network Offering',
    'NetworkACL': 'Network ACL',
    'Network': 'Network',
    'CiscoNexus': 'Network',
    'OpenDaylight': 'Network',
    'createServiceInstance': 'Network',
    'addGloboDnsHost': 'Network',
    'createTungstenFabricProvider': 'Tungsten',
    'listTungstenFabricProviders': 'Tungsten',
    'configTungstenFabricService': 'Tungsten',
    'createTungstenFabricPublicNetwork': 'Tungsten',
    'synchronizeTungstenFabricData': 'Tungsten',
    'addTungstenFabricPolicyRule': 'Tungsten',
    'createTungstenFabricPolicy': 'Tungsten',
    'deleteTungstenFabricPolicy': 'Tungsten',
    'removeTungstenFabricPolicyRule': 'Tungsten',
    'listTungstenFabricTag': 'Tungsten',
    'listTungstenFabricTagType': 'Tungsten',
    'listTungstenFabricPolicy': 'Tungsten',
    'listTungstenFabricPolicyRule': 'Tungsten',
    'listTungstenFabricNetwork': 'Tungsten',
    'listTungstenFabricVm': 'Tungsten',
    'listTungstenFabricNic': 'Tungsten',
    'createTungstenFabricTag': 'Tungsten',
    'createTungstenFabricTagType': 'Tungsten',
    'deleteTungstenFabricTag': 'Tungsten',
    'deleteTungstenFabricTagType': 'Tungsten',
    'applyTungstenFabricPolicy': 'Tungsten',
    'applyTungstenFabricTag': 'Tungsten',
    'removeTungstenFabricTag': 'Tungsten',
    'removeTungstenFabricPolicy': 'Tungsten',
    'createTungstenFabricApplicationPolicySet': 'Tungsten',
    'createTungstenFabricFirewallPolicy': 'Tungsten',
    'createTungstenFabricFirewallRule': 'Tungsten',
    'createTungstenFabricServiceGroup': 'Tungsten',
    'createTungstenFabricAddressGroup': 'Tungsten',
    'createTungstenFabricLogicalRouter': 'Tungsten',
    'addTungstenFabricNetworkGatewayToLogicalRouter': 'Tungsten',
    'listTungstenFabricApplicationPolicySet': 'Tungsten',
    'listTungstenFabricFirewallPolicy': 'Tungsten',
    'listTungstenFabricFirewallRule': 'Tungsten',
    'listTungstenFabricServiceGroup': 'Tungsten',
    'listTungstenFabricAddressGroup': 'Tungsten',
    'listTungstenFabricLogicalRouter': 'Tungsten',
    'deleteTungstenFabricApplicationPolicySet': 'Tungsten',
    'deleteTungstenFabricFirewallPolicy': 'Tungsten',
    'deleteTungstenFabricFirewallRule': 'Tungsten',
    'deleteTungstenFabricAddressGroup': 'Tungsten',
    'deleteTungstenFabricServiceGroup': 'Tungsten',
    'deleteTungstenFabricLogicalRouter': 'Tungsten',
    'removeTungstenFabricNetworkGatewayFromLogicalRouter': 'Tungsten',
    'updateTungstenFabricLBHealthMonitor': 'Tungsten',
    'listTungstenFabricLBHealthMonitor': 'Tungsten',
    'Vpn': 'VPN',
    'Limit': 'Limit',
    'ResourceCount': 'Limit',
    'CloudIdentifier': 'Cloud Identifier',
    'InstanceGroup': 'VM Group',
    'StorageMaintenance': 'Storage Pool',
    'StoragePool': 'Storage Pool',
    'StorageProvider': 'Storage Pool',
    'updateStorageCapabilities' : 'Storage Pool',
    'SecurityGroup': 'Security Group',
    'SSH': 'SSH',
    'register': 'Registration',
    'AsyncJob': 'Async job',
    'Certificate': 'Certificate',
    'Hypervisor': 'Hypervisor',
    'Alert': 'Alert',
    'Event': 'Event',
    'login': 'Authentication',
    'logout': 'Authentication',
    'saml': 'Authentication',
    'getSPMetadata': 'Authentication',
    'listIdps': 'Authentication',
    'authorizeSamlSso': 'Authentication',
    'listSamlAuthorization': 'Authentication',
    'quota': 'Quota',
    'emailTemplate': 'Quota',
    'Capacity': 'System Capacity',
    'NetworkDevice': 'Network Device',
    'ExternalLoadBalancer': 'Ext Load Balancer',
    'ExternalFirewall': 'Ext Firewall',
    'Usage': 'Usage',
    'TrafficMonitor': 'Usage',
    'TrafficType': 'Usage',
    'Product': 'Product',
    'LB': 'Load Balancer',
    'ldap': 'LDAP',
    'Ldap': 'LDAP',
    'Swift': 'Swift',
    'S3' : 'S3',
    'SecondaryStorage': 'Host',
    'Project': 'Project',
    'Lun': 'Storage',
    'Pool': 'Pool',
    'VPC': 'VPC',
    'PrivateGateway': 'VPC',
    'migrateVpc': 'VPC',
    'Simulator': 'simulator',
    'StaticRoute': 'VPC',
    'Tags': 'Resource tags',
    'Icon': 'Resource Icon',
    'NiciraNvpDevice': 'Nicira NVP',
    'BrocadeVcsDevice': 'Brocade VCS',
    'BigSwitchBcfDevice': 'BigSwitch BCF',
    'AutoScale': 'AutoScale',
    'Counter': 'AutoScale',
    'Condition': 'AutoScale',
    'Api': 'API Discovery',
    'Region': 'Region',
    'Detail': 'Resource metadata',
    'addIpToNic': 'Nic',
    'removeIpFromNic': 'Nic',
    'updateVmNicIp': 'Nic',
    'listNics':'Nic',
	'AffinityGroup': 'Affinity Group',
    'addImageStore': 'Image Store',
    'listImageStore': 'Image Store',
    'deleteImageStore': 'Image Store',
    'createSecondaryStagingStore': 'Image Store',
    'deleteSecondaryStagingStore': 'Image Store',
    'listSecondaryStagingStores': 'Image Store',
    'updateImageStore': 'Image Store',
    'InternalLoadBalancer': 'Internal LB',
	'DeploymentPlanners': 'Configuration',
	'ObjectStore': 'Image Store',
    'PortableIp': 'Portable IP',
    'dedicateHost': 'Dedicate Resources',
    'releaseDedicatedHost': 'Dedicate Resources',
    'Baremetal' : 'Baremetal',
    'UCS' : 'UCS',
    'Ucs' : 'UCS',
    'CacheStores' : 'Cache Stores',
    'CacheStore' : 'Cache Store',
    'OvsElement' : 'Ovs Element',
    'StratosphereSsp' : ' Stratosphere SSP',
    'Metrics' : 'Metrics',
    'Infrastructure' : 'Metrics',
    'listNetscalerControlCenter' : 'Load Balancer',
    'listRegisteredServicePackages': 'Load Balancer',
    'listNsVpx' : 'Load Balancer',
    'destroyNsVPx': 'Load Balancer',
    'deployNetscalerVpx' : 'Load Balancer',
    'deleteNetscalerControlCenter' : 'Load Balancer',
    'stopNetScalerVpx' : 'Load Balancer',
    'deleteServicePackageOffering' : 'Load Balancer',
    'destroyNsVpx' : 'Load Balancer',
    'startNsVpx' : 'Load Balancer',
    'listAnnotations' : 'Annotations',
    'addAnnotation' : 'Annotations',
    'removeAnnotation' : 'Annotations',
    'updateAnnotationVisibility' : 'Annotations',
    'CA': 'Certificate',
    'listElastistorInterface': 'Misc',
    'cloudian': 'Cloudian',
    'Sioc' : 'Sioc',
    'Diagnostics': 'Diagnostics',
    'Management': 'Management',
    'Backup' : 'Backup and Recovery',
    'Restore' : 'Backup and Recovery',
    'UnmanagedInstance': 'Virtual Machine',
    'KubernetesSupportedVersion': 'Kubernetes Service',
    'KubernetesCluster': 'Kubernetes Service',
    'UnmanagedInstance': 'Virtual Machine',
    'Rolling': 'Rolling Maintenance',
    'importVsphereStoragePolicies' : 'vSphere storage policies',
    'listVsphereStoragePolicies' : 'vSphere storage policies',
    'ConsoleEndpoint': 'Console Endpoint',
    'Shutdown': 'Shutdown'
}


categories = {}


def choose_category(fn):
    for k, v in known_categories.items():
        if k in fn:
            return v
    raise Exception('Need to add a category for %s to %s:known_categories' %
                    (fn, __file__))
    sys.exit(1)


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
