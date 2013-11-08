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


"""list the vm nics  IP to NIC"""
from baseCmd import *
from baseResponse import *
class listNicsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the ID of the vm"""
        """Required"""
        self.virtualmachineid = None
        """List by keyword"""
        self.keyword = None
        """the ID of the nic to to list IPs"""
        self.nicid = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        self.required = ["virtualmachineid",]

class listNicsResponse (baseResponse):
    def __init__(self):
        """the ID of the nic"""
        self.id = None
        """the broadcast uri of the nic"""
        self.broadcasturi = None
        """the gateway of the nic"""
        self.gateway = None
        """the IPv6 address of network"""
        self.ip6address = None
        """the cidr of IPv6 network"""
        self.ip6cidr = None
        """the gateway of IPv6 network"""
        self.ip6gateway = None
        """the ip address of the nic"""
        self.ipaddress = None
        """true if nic is default, false otherwise"""
        self.isdefault = None
        """the isolation uri of the nic"""
        self.isolationuri = None
        """true if nic is default, false otherwise"""
        self.macaddress = None
        """the netmask of the nic"""
        self.netmask = None
        """the ID of the corresponding network"""
        self.networkid = None
        """the name of the corresponding network"""
        self.networkname = None
        """the Secondary ipv4 addr of nic"""
        self.secondaryip = None
        """the traffic type of the nic"""
        self.traffictype = None
        """the type of the nic"""
        self.type = None

