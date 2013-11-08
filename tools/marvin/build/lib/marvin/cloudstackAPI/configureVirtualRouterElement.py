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


"""Configures a virtual router element."""
from baseCmd import *
from baseResponse import *
class configureVirtualRouterElementCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the ID of the virtual router provider"""
        """Required"""
        self.id = None
        """Enabled/Disabled the service provider"""
        """Required"""
        self.enabled = None
        self.required = ["id","enabled",]

class configureVirtualRouterElementResponse (baseResponse):
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

