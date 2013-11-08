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


"""Dedicates a Public IP range to an account"""
from baseCmd import *
from baseResponse import *
class dedicatePublicIpRangeCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the id of the VLAN IP range"""
        """Required"""
        self.id = None
        """account who will own the VLAN"""
        """Required"""
        self.account = None
        """domain ID of the account owning a VLAN"""
        """Required"""
        self.domainid = None
        """project who will own the VLAN"""
        self.projectid = None
        self.required = ["id","account","domainid",]

class dedicatePublicIpRangeResponse (baseResponse):
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

