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


"""Lists remote access vpns"""
from baseCmd import *
from baseResponse import *
class listRemoteAccessVpnsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """public ip address id of the vpn server"""
        """Required"""
        self.publicipid = None
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves."""
        self.isrecursive = None
        """List by keyword"""
        self.keyword = None
        """If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false"""
        self.listall = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """list objects by project"""
        self.projectid = None
        self.required = ["publicipid",]

class listRemoteAccessVpnsResponse (baseResponse):
    def __init__(self):
        """the id of the remote access vpn"""
        self.id = None
        """the account of the remote access vpn"""
        self.account = None
        """the domain name of the account of the remote access vpn"""
        self.domain = None
        """the domain id of the account of the remote access vpn"""
        self.domainid = None
        """the range of ips to allocate to the clients"""
        self.iprange = None
        """the ipsec preshared key"""
        self.presharedkey = None
        """the project name of the vpn"""
        self.project = None
        """the project id of the vpn"""
        self.projectid = None
        """the public ip address of the vpn server"""
        self.publicip = None
        """the public ip address of the vpn server"""
        self.publicipid = None
        """the state of the rule"""
        self.state = None

