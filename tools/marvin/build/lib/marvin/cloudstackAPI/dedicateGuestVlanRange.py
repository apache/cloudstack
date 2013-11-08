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


"""Dedicates a guest vlan range to an account"""
from baseCmd import *
from baseResponse import *
class dedicateGuestVlanRangeCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """account who will own the VLAN"""
        """Required"""
        self.account = None
        """domain ID of the account owning a VLAN"""
        """Required"""
        self.domainid = None
        """physical network ID of the vlan"""
        """Required"""
        self.physicalnetworkid = None
        """guest vlan range to be dedicated"""
        """Required"""
        self.vlanrange = None
        """project who will own the VLAN"""
        self.projectid = None
        self.required = ["account","domainid","physicalnetworkid","vlanrange",]

class dedicateGuestVlanRangeResponse (baseResponse):
    def __init__(self):
        """the ID of the guest VLAN range"""
        self.id = None
        """the account of the guest VLAN range"""
        self.account = None
        """the domain name of the guest VLAN range"""
        self.domain = None
        """the domain ID of the guest VLAN range"""
        self.domainid = None
        """the guest VLAN range"""
        self.guestvlanrange = None
        """the physical network of the guest vlan range"""
        self.physicalnetworkid = None
        """the project name of the guest vlan range"""
        self.project = None
        """the project id of the guest vlan range"""
        self.projectid = None
        """the zone of the guest vlan range"""
        self.zoneid = None

