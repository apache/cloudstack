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


"""Creates a VLAN IP range."""
from baseCmd import *
from baseResponse import *
class createVlanIpRangeCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """account who will own the VLAN. If VLAN is Zone wide, this parameter should be ommited"""
        self.account = None
        """domain ID of the account owning a VLAN"""
        self.domainid = None
        """the ending IP address in the VLAN IP range"""
        self.endip = None
        """the ending IPv6 address in the IPv6 network range"""
        self.endipv6 = None
        """true if VLAN is of Virtual type, false if Direct"""
        self.forvirtualnetwork = None
        """the gateway of the VLAN IP range"""
        self.gateway = None
        """the CIDR of IPv6 network, must be at least /64"""
        self.ip6cidr = None
        """the gateway of the IPv6 network. Required for Shared networks and Isolated networks when it belongs to VPC"""
        self.ip6gateway = None
        """the netmask of the VLAN IP range"""
        self.netmask = None
        """the network id"""
        self.networkid = None
        """the physical network id"""
        self.physicalnetworkid = None
        """optional parameter. Have to be specified for Direct Untagged vlan only."""
        self.podid = None
        """project who will own the VLAN. If VLAN is Zone wide, this parameter should be ommited"""
        self.projectid = None
        """the beginning IP address in the VLAN IP range"""
        self.startip = None
        """the beginning IPv6 address in the IPv6 network range"""
        self.startipv6 = None
        """the ID or VID of the VLAN. If not specified, will be defaulted to the vlan of the network or if vlan of the network is null - to Untagged"""
        self.vlan = None
        """the Zone ID of the VLAN IP range"""
        self.zoneid = None
        self.required = []

class createVlanIpRangeResponse (baseResponse):
    def __init__(self):
        """the ID of the VLAN IP range"""
        self.id = None
        """the account of the VLAN IP range"""
        self.account = None
        """the description of the VLAN IP range"""
        self.description = None
        """the domain name of the VLAN IP range"""
        self.domain = None
        """the domain ID of the VLAN IP range"""
        self.domainid = None
        """the end ip of the VLAN IP range"""
        self.endip = None
        """the end ipv6 of the VLAN IP range"""
        self.endipv6 = None
        """the virtual network for the VLAN IP range"""
        self.forvirtualnetwork = None
        """the gateway of the VLAN IP range"""
        self.gateway = None
        """the cidr of IPv6 network"""
        self.ip6cidr = None
        """the gateway of IPv6 network"""
        self.ip6gateway = None
        """the netmask of the VLAN IP range"""
        self.netmask = None
        """the network id of vlan range"""
        self.networkid = None
        """the physical network this belongs to"""
        self.physicalnetworkid = None
        """the Pod ID for the VLAN IP range"""
        self.podid = None
        """the Pod name for the VLAN IP range"""
        self.podname = None
        """the project name of the vlan range"""
        self.project = None
        """the project id of the vlan range"""
        self.projectid = None
        """the start ip of the VLAN IP range"""
        self.startip = None
        """the start ipv6 of the VLAN IP range"""
        self.startipv6 = None
        """the ID or VID of the VLAN."""
        self.vlan = None
        """the Zone ID of the VLAN IP range"""
        self.zoneid = None

