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


"""Upgrades domain router to a new service offering"""
from baseCmd import *
from baseResponse import *
class changeServiceForRouterCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """The ID of the router"""
        """Required"""
        self.id = None
        """the service offering ID to apply to the domain router"""
        """Required"""
        self.serviceofferingid = None
        self.required = ["id","serviceofferingid",]

class changeServiceForRouterResponse (baseResponse):
    def __init__(self):
        """the id of the router"""
        self.id = None
        """the account associated with the router"""
        self.account = None
        """the date and time the router was created"""
        self.created = None
        """the first DNS for the router"""
        self.dns1 = None
        """the second DNS for the router"""
        self.dns2 = None
        """the domain associated with the router"""
        self.domain = None
        """the domain ID associated with the router"""
        self.domainid = None
        """the gateway for the router"""
        self.gateway = None
        """the guest IP address for the router"""
        self.guestipaddress = None
        """the guest MAC address for the router"""
        self.guestmacaddress = None
        """the guest netmask for the router"""
        self.guestnetmask = None
        """the ID of the corresponding guest network"""
        self.guestnetworkid = None
        """the host ID for the router"""
        self.hostid = None
        """the hostname for the router"""
        self.hostname = None
        """the first IPv6 DNS for the router"""
        self.ip6dns1 = None
        """the second IPv6 DNS for the router"""
        self.ip6dns2 = None
        """if this router is an redundant virtual router"""
        self.isredundantrouter = None
        """the link local IP address for the router"""
        self.linklocalip = None
        """the link local MAC address for the router"""
        self.linklocalmacaddress = None
        """the link local netmask for the router"""
        self.linklocalnetmask = None
        """the ID of the corresponding link local network"""
        self.linklocalnetworkid = None
        """the name of the router"""
        self.name = None
        """the network domain for the router"""
        self.networkdomain = None
        """the Pod ID for the router"""
        self.podid = None
        """the project name of the address"""
        self.project = None
        """the project id of the ipaddress"""
        self.projectid = None
        """the public IP address for the router"""
        self.publicip = None
        """the public MAC address for the router"""
        self.publicmacaddress = None
        """the public netmask for the router"""
        self.publicnetmask = None
        """the ID of the corresponding public network"""
        self.publicnetworkid = None
        """the state of redundant virtual router"""
        self.redundantstate = None
        """role of the domain router"""
        self.role = None
        """the version of scripts"""
        self.scriptsversion = None
        """the ID of the service offering of the virtual machine"""
        self.serviceofferingid = None
        """the name of the service offering of the virtual machine"""
        self.serviceofferingname = None
        """the state of the router"""
        self.state = None
        """the template ID for the router"""
        self.templateid = None
        """the version of template"""
        self.templateversion = None
        """VPC the router belongs to"""
        self.vpcid = None
        """the Zone ID for the router"""
        self.zoneid = None
        """the Zone name for the router"""
        self.zonename = None
        """the list of nics associated with the router"""
        self.nic = []

class nic:
    def __init__(self):
        """"the ID of the nic"""
        self.id = None
        """"the broadcast uri of the nic"""
        self.broadcasturi = None
        """"the gateway of the nic"""
        self.gateway = None
        """"the IPv6 address of network"""
        self.ip6address = None
        """"the cidr of IPv6 network"""
        self.ip6cidr = None
        """"the gateway of IPv6 network"""
        self.ip6gateway = None
        """"the ip address of the nic"""
        self.ipaddress = None
        """"true if nic is default, false otherwise"""
        self.isdefault = None
        """"the isolation uri of the nic"""
        self.isolationuri = None
        """"true if nic is default, false otherwise"""
        self.macaddress = None
        """"the netmask of the nic"""
        self.netmask = None
        """"the ID of the corresponding network"""
        self.networkid = None
        """"the name of the corresponding network"""
        self.networkname = None
        """"the Secondary ipv4 addr of nic"""
        self.secondaryip = None
        """"the traffic type of the nic"""
        self.traffictype = None
        """"the type of the nic"""
        self.type = None

