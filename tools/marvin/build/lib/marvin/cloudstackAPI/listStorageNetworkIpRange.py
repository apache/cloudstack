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


"""List a storage network IP range."""
from baseCmd import *
from baseResponse import *
class listStorageNetworkIpRangeCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """optional parameter. Storaget network IP range uuid, if specicied, using it to search the range."""
        self.id = None
        """List by keyword"""
        self.keyword = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """optional parameter. Pod uuid, if specicied and range uuid is absent, using it to search the range."""
        self.podid = None
        """optional parameter. Zone uuid, if specicied and both pod uuid and range uuid are absent, using it to search the range."""
        self.zoneid = None
        self.required = []

class listStorageNetworkIpRangeResponse (baseResponse):
    def __init__(self):
        """the uuid of storage network IP range."""
        self.id = None
        """the end ip of the storage network IP range"""
        self.endip = None
        """the gateway of the storage network IP range"""
        self.gateway = None
        """the netmask of the storage network IP range"""
        self.netmask = None
        """the network uuid of storage network IP range"""
        self.networkid = None
        """the Pod uuid for the storage network IP range"""
        self.podid = None
        """the start ip of the storage network IP range"""
        self.startip = None
        """the ID or VID of the VLAN."""
        self.vlan = None
        """the Zone uuid of the storage network IP range"""
        self.zoneid = None

