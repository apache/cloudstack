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


"""Lists all public ip addresses"""
from baseCmd import *
from baseResponse import *
class listPublicIpAddressesCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """limits search results to allocated public IP addresses"""
        self.allocatedonly = None
        """lists all public IP addresses associated to the network specified"""
        self.associatednetworkid = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """list only ips used for load balancing"""
        self.forloadbalancing = None
        """the virtual network for the IP address"""
        self.forvirtualnetwork = None
        """lists ip address by id"""
        self.id = None
        """lists the specified IP address"""
        self.ipaddress = None
        """defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves."""
        self.isrecursive = None
        """list only source nat ip addresses"""
        self.issourcenat = None
        """list only static nat ip addresses"""
        self.isstaticnat = None
        """List by keyword"""
        self.keyword = None
        """If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false"""
        self.listall = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """lists all public IP addresses by physical network id"""
        self.physicalnetworkid = None
        """list objects by project"""
        self.projectid = None
        """List resources by tags (key/value pairs)"""
        self.tags = []
        """lists all public IP addresses by VLAN ID"""
        self.vlanid = None
        """List ips belonging to the VPC"""
        self.vpcid = None
        """lists all public IP addresses by Zone ID"""
        self.zoneid = None
        self.required = []

class listPublicIpAddressesResponse (baseResponse):
    def __init__(self):
        """public IP address id"""
        self.id = None
        """the account the public IP address is associated with"""
        self.account = None
        """date the public IP address was acquired"""
        self.allocated = None
        """the ID of the Network associated with the IP address"""
        self.associatednetworkid = None
        """the name of the Network associated with the IP address"""
        self.associatednetworkname = None
        """the domain the public IP address is associated with"""
        self.domain = None
        """the domain ID the public IP address is associated with"""
        self.domainid = None
        """the virtual network for the IP address"""
        self.forvirtualnetwork = None
        """public IP address"""
        self.ipaddress = None
        """is public IP portable across the zones"""
        self.isportable = None
        """true if the IP address is a source nat address, false otherwise"""
        self.issourcenat = None
        """true if this ip is for static nat, false otherwise"""
        self.isstaticnat = None
        """true if this ip is system ip (was allocated as a part of deployVm or createLbRule)"""
        self.issystem = None
        """the ID of the Network where ip belongs to"""
        self.networkid = None
        """the physical network this belongs to"""
        self.physicalnetworkid = None
        """the project name of the address"""
        self.project = None
        """the project id of the ipaddress"""
        self.projectid = None
        """purpose of the IP address. In Acton this value is not null for Ips with isSystem=true, and can have either StaticNat or LB value"""
        self.purpose = None
        """State of the ip address. Can be: Allocatin, Allocated and Releasing"""
        self.state = None
        """virutal machine display name the ip address is assigned to (not null only for static nat Ip)"""
        self.virtualmachinedisplayname = None
        """virutal machine id the ip address is assigned to (not null only for static nat Ip)"""
        self.virtualmachineid = None
        """virutal machine name the ip address is assigned to (not null only for static nat Ip)"""
        self.virtualmachinename = None
        """the ID of the VLAN associated with the IP address. This parameter is visible to ROOT admins only"""
        self.vlanid = None
        """the VLAN associated with the IP address"""
        self.vlanname = None
        """virutal machine (dnat) ip address (not null only for static nat Ip)"""
        self.vmipaddress = None
        """VPC the ip belongs to"""
        self.vpcid = None
        """the ID of the zone the public IP address belongs to"""
        self.zoneid = None
        """the name of the zone the public IP address belongs to"""
        self.zonename = None
        """the list of resource tags associated with ip address"""
        self.tags = []
        """the ID of the latest async job acting on this object"""
        self.jobid = None
        """the current status of the latest async job acting on this object"""
        self.jobstatus = None

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

