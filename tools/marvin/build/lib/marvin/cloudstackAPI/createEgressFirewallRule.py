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


"""Creates a egress firewall rule for a given network"""
from baseCmd import *
from baseResponse import *
class createEgressFirewallRuleCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the network id of the port forwarding rule"""
        """Required"""
        self.networkid = None
        """the protocol for the firewall rule. Valid values are TCP/UDP/ICMP."""
        """Required"""
        self.protocol = None
        """the cidr list to forward traffic from"""
        self.cidrlist = []
        """the ending port of firewall rule"""
        self.endport = None
        """error code for this icmp message"""
        self.icmpcode = None
        """type of the icmp message being sent"""
        self.icmptype = None
        """the starting port of firewall rule"""
        self.startport = None
        """type of firewallrule: system/user"""
        self.type = None
        self.required = ["networkid","protocol",]

class createEgressFirewallRuleResponse (baseResponse):
    def __init__(self):
        """the ID of the firewall rule"""
        self.id = None
        """the cidr list to forward traffic from"""
        self.cidrlist = None
        """the ending port of firewall rule's port range"""
        self.endport = None
        """error code for this icmp message"""
        self.icmpcode = None
        """type of the icmp message being sent"""
        self.icmptype = None
        """the public ip address for the firewall rule"""
        self.ipaddress = None
        """the public ip address id for the firewall rule"""
        self.ipaddressid = None
        """the network id of the firewall rule"""
        self.networkid = None
        """the protocol of the firewall rule"""
        self.protocol = None
        """the starting port of firewall rule's port range"""
        self.startport = None
        """the state of the rule"""
        self.state = None
        """the list of resource tags associated with the rule"""
        self.tags = []

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

