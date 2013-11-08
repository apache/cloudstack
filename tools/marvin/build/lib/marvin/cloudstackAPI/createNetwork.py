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


"""Creates a network"""
from baseCmd import *
from baseResponse import *
class createNetworkCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the display text of the network"""
        """Required"""
        self.displaytext = None
        """the name of the network"""
        """Required"""
        self.name = None
        """the network offering id"""
        """Required"""
        self.networkofferingid = None
        """the Zone ID for the network"""
        """Required"""
        self.zoneid = None
        """account who will own the network"""
        self.account = None
        """Network ACL Id associated for the network"""
        self.aclid = None
        """Access control type; supported values are account and domain. In 3.0 all shared networks should have aclType=Domain, and all Isolated networks - Account. Account means that only the account owner can use the network, domain - all accouns in the domain can use the network"""
        self.acltype = None
        """an optional field, whether to the display the network to the end user or not."""
        self.displaynetwork = None
        """domain ID of the account owning a network"""
        self.domainid = None
        """the ending IP address in the network IP range. If not specified, will be defaulted to startIP"""
        self.endip = None
        """the ending IPv6 address in the IPv6 network range"""
        self.endipv6 = None
        """the gateway of the network. Required for Shared networks and Isolated networks when it belongs to VPC"""
        self.gateway = None
        """the CIDR of IPv6 network, must be at least /64"""
        self.ip6cidr = None
        """the gateway of the IPv6 network. Required for Shared networks and Isolated networks when it belongs to VPC"""
        self.ip6gateway = None
        """the isolated private vlan for this network"""
        self.isolatedpvlan = None
        """the netmask of the network. Required for Shared networks and Isolated networks when it belongs to VPC"""
        self.netmask = None
        """network domain"""
        self.networkdomain = None
        """the Physical Network ID the network belongs to"""
        self.physicalnetworkid = None
        """an optional project for the ssh key"""
        self.projectid = None
        """the beginning IP address in the network IP range"""
        self.startip = None
        """the beginning IPv6 address in the IPv6 network range"""
        self.startipv6 = None
        """Defines whether to allow subdomains to use networks dedicated to their parent domain(s). Should be used with aclType=Domain, defaulted to allow.subdomain.network.access global config if not specified"""
        self.subdomainaccess = None
        """the ID or VID of the network"""
        self.vlan = None
        """the VPC network belongs to"""
        self.vpcid = None
        self.required = ["displaytext","name","networkofferingid","zoneid",]

class createNetworkResponse (baseResponse):
    def __init__(self):
        """the id of the network"""
        self.id = None
        """the owner of the network"""
        self.account = None
        """ACL Id associated with the VPC network"""
        self.aclid = None
        """acl type - access type to the network"""
        self.acltype = None
        """Broadcast domain type of the network"""
        self.broadcastdomaintype = None
        """broadcast uri of the network. This parameter is visible to ROOT admins only"""
        self.broadcasturi = None
        """list networks available for vm deployment"""
        self.canusefordeploy = None
        """Cloudstack managed address space, all CloudStack managed VMs get IP address from CIDR"""
        self.cidr = None
        """an optional field, whether to the display the network to the end user or not."""
        self.displaynetwork = None
        """the displaytext of the network"""
        self.displaytext = None
        """the first DNS for the network"""
        self.dns1 = None
        """the second DNS for the network"""
        self.dns2 = None
        """the domain name of the network owner"""
        self.domain = None
        """the domain id of the network owner"""
        self.domainid = None
        """the network's gateway"""
        self.gateway = None
        """the cidr of IPv6 network"""
        self.ip6cidr = None
        """the gateway of IPv6 network"""
        self.ip6gateway = None
        """true if network is default, false otherwise"""
        self.isdefault = None
        """list networks that are persistent"""
        self.ispersistent = None
        """true if network is system, false otherwise"""
        self.issystem = None
        """the name of the network"""
        self.name = None
        """the network's netmask"""
        self.netmask = None
        """the network CIDR of the guest network configured with IP reservation. It is the summation of CIDR and RESERVED_IP_RANGE"""
        self.networkcidr = None
        """the network domain"""
        self.networkdomain = None
        """availability of the network offering the network is created from"""
        self.networkofferingavailability = None
        """true if network offering is ip conserve mode enabled"""
        self.networkofferingconservemode = None
        """display text of the network offering the network is created from"""
        self.networkofferingdisplaytext = None
        """network offering id the network is created from"""
        self.networkofferingid = None
        """name of the network offering the network is created from"""
        self.networkofferingname = None
        """the physical network id"""
        self.physicalnetworkid = None
        """the project name of the address"""
        self.project = None
        """the project id of the ipaddress"""
        self.projectid = None
        """related to what other network configuration"""
        self.related = None
        """the network's IP range not to be used by CloudStack guest VMs and can be used for non CloudStack purposes"""
        self.reservediprange = None
        """true network requires restart"""
        self.restartrequired = None
        """true if network supports specifying ip ranges, false otherwise"""
        self.specifyipranges = None
        """state of the network"""
        self.state = None
        """true if users from subdomains can access the domain level network"""
        self.subdomainaccess = None
        """the traffic type of the network"""
        self.traffictype = None
        """the type of the network"""
        self.type = None
        """The vlan of the network. This parameter is visible to ROOT admins only"""
        self.vlan = None
        """VPC the network belongs to"""
        self.vpcid = None
        """zone id of the network"""
        self.zoneid = None
        """the name of the zone the network belongs to"""
        self.zonename = None
        """the list of services"""
        self.service = []
        """the list of resource tags associated with network"""
        self.tags = []

class capability:
    def __init__(self):
        """"can this service capability value can be choosable while creatine network offerings"""
        self.canchooseservicecapability = None
        """"the capability name"""
        self.name = None
        """"the capability value"""
        self.value = None

class provider:
    def __init__(self):
        """"uuid of the network provider"""
        self.id = None
        """"true if individual services can be enabled/disabled"""
        self.canenableindividualservice = None
        """"the destination physical network"""
        self.destinationphysicalnetworkid = None
        """"the provider name"""
        self.name = None
        """"the physical network this belongs to"""
        self.physicalnetworkid = None
        """"services for this provider"""
        self.servicelist = None
        """"state of the network provider"""
        self.state = None

class service:
    def __init__(self):
        """"the service name"""
        self.name = None
        """"the list of capabilities"""
        self.capability = []
        """"can this service capability value can be choosable while creatine network offerings"""
        self.canchooseservicecapability = None
        """"the capability name"""
        self.name = None
        """"the capability value"""
        self.value = None
        """"the service provider name"""
        self.provider = []
        """"uuid of the network provider"""
        self.id = None
        """"true if individual services can be enabled/disabled"""
        self.canenableindividualservice = None
        """"the destination physical network"""
        self.destinationphysicalnetworkid = None
        """"the provider name"""
        self.name = None
        """"the physical network this belongs to"""
        self.physicalnetworkid = None
        """"services for this provider"""
        self.servicelist = None
        """"state of the network provider"""
        self.state = None

class tags:
    def __init__(self):
        """"the account associated with the tag"""
        self.account = None
        """"customer associated with the tag"""
        self.customer = None
        """"the domain associated with the tag"""
        self.domain = None
        """"the ID of the domain associated with the tag"""
        self.domainid = None
        """"tag key name"""
        self.key = None
        """"the project name where tag belongs to"""
        self.project = None
        """"the project id the tag belongs to"""
        self.projectid = None
        """"id of the resource"""
        self.resourceid = None
        """"resource type"""
        self.resourcetype = None
        """"tag value"""
        self.value = None

