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


"""Updates ACL Item with specified Id"""
from baseCmd import *
from baseResponse import *
class updateNetworkACLItemCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the ID of the network ACL Item"""
        """Required"""
        self.id = None
        """scl entry action, allow or deny"""
        self.action = None
        """the cidr list to allow traffic from/to"""
        self.cidrlist = []
        """the ending port of ACL"""
        self.endport = None
        """error code for this icmp message"""
        self.icmpcode = None
        """type of the icmp message being sent"""
        self.icmptype = None
        """The network of the vm the ACL will be created for"""
        self.number = None
        """the protocol for the ACL rule. Valid values are TCP/UDP/ICMP/ALL or valid protocol number"""
        self.protocol = None
        """the starting port of ACL"""
        self.startport = None
        """the traffic type for the ACL,can be Ingress or Egress, defaulted to Ingress if not specified"""
        self.traffictype = None
        self.required = ["id",]

class updateNetworkACLItemResponse (baseResponse):
    def __init__(self):
        """the ID of the ACL Item"""
        self.id = None
        """the ID of the ACL this item belongs to"""
        self.aclid = None
        """Action of ACL Item. Allow/Deny"""
        self.action = None
        """the cidr list to forward traffic from"""
        self.cidrlist = None
        """the ending port of ACL's port range"""
        self.endport = None
        """error code for this icmp message"""
        self.icmpcode = None
        """type of the icmp message being sent"""
        self.icmptype = None
        """Number of the ACL Item"""
        self.number = None
        """the protocol of the ACL"""
        self.protocol = None
        """the starting port of ACL's port range"""
        self.startport = None
        """the state of the rule"""
        self.state = None
        """the traffic type for the ACL"""
        self.traffictype = None
        """the list of resource tags associated with the network ACLs"""
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

