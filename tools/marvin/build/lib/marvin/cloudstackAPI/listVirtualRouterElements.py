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


"""Lists all available virtual router elements."""
from baseCmd import *
from baseResponse import *
class listVirtualRouterElementsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list network offerings by enabled state"""
        self.enabled = None
        """list virtual router elements by id"""
        self.id = None
        """List by keyword"""
        self.keyword = None
        """list virtual router elements by network service provider id"""
        self.nspid = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        self.required = []

class listVirtualRouterElementsResponse (baseResponse):
    def __init__(self):
        """the id of the router"""
        self.id = None
        """the account associated with the provider"""
        self.account = None
        """the domain associated with the provider"""
        self.domain = None
        """the domain ID associated with the provider"""
        self.domainid = None
        """Enabled/Disabled the service provider"""
        self.enabled = None
        """the physical network service provider id of the provider"""
        self.nspid = None
        """the project name of the address"""
        self.project = None
        """the project id of the ipaddress"""
        self.projectid = None

