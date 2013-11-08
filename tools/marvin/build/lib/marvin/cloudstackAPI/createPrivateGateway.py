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


"""Creates a private gateway"""
from baseCmd import *
from baseResponse import *
class createPrivateGatewayCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the gateway of the Private gateway"""
        """Required"""
        self.gateway = None
        """the IP address of the Private gateaway"""
        """Required"""
        self.ipaddress = None
        """the netmask of the Private gateway"""
        """Required"""
        self.netmask = None
        """the network implementation uri for the private gateway"""
        """Required"""
        self.vlan = None
        """the VPC network belongs to"""
        """Required"""
        self.vpcid = None
        """the ID of the network ACL"""
        self.aclid = None
        """the uuid of the network offering to use for the private gateways network connection"""
        self.networkofferingid = None
        """the Physical Network ID the network belongs to"""
        self.physicalnetworkid = None
        """source NAT supported value. Default value false. If 'true' source NAT is enabled on the private gateway 'false': sourcenat is not supported"""
        self.sourcenatsupported = None
        self.required = ["gateway","ipaddress","netmask","vlan","vpcid",]

class createPrivateGatewayResponse (baseResponse):
    def __init__(self):
        """the id of the private gateway"""
        self.id = None
        """the account associated with the private gateway"""
        self.account = None
        """ACL Id set for private gateway"""
        self.aclid = None
        """the domain associated with the private gateway"""
        self.domain = None
        """the ID of the domain associated with the private gateway"""
        self.domainid = None
        """the gateway"""
        self.gateway = None
        """the private gateway's ip address"""
        self.ipaddress = None
        """the private gateway's netmask"""
        self.netmask = None
        """the physical network id"""
        self.physicalnetworkid = None
        """the project name of the private gateway"""
        self.project = None
        """the project id of the private gateway"""
        self.projectid = None
        """Souce Nat enable status"""
        self.sourcenatsupported = None
        """State of the gateway, can be Creating, Ready, Deleting"""
        self.state = None
        """the network implementation uri for the private gateway"""
        self.vlan = None
        """VPC the private gateaway belongs to"""
        self.vpcid = None
        """zone id of the private gateway"""
        self.zoneid = None
        """the name of the zone the private gateway belongs to"""
        self.zonename = None

