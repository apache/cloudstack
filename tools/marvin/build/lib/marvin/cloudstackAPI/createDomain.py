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


"""Creates a domain"""
from baseCmd import *
from baseResponse import *
class createDomainCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """creates domain with this name"""
        """Required"""
        self.name = None
        """Domain UUID, required for adding domain from another Region"""
        self.domainid = None
        """Network domain for networks in the domain"""
        self.networkdomain = None
        """assigns new domain a parent domain by domain ID of the parent.  If no parent domain is specied, the ROOT domain is assumed."""
        self.parentdomainid = None
        self.required = ["name",]

class createDomainResponse (baseResponse):
    def __init__(self):
        """the ID of the domain"""
        self.id = None
        """whether the domain has one or more sub-domains"""
        self.haschild = None
        """the level of the domain"""
        self.level = None
        """the name of the domain"""
        self.name = None
        """the network domain"""
        self.networkdomain = None
        """the domain ID of the parent domain"""
        self.parentdomainid = None
        """the domain name of the parent domain"""
        self.parentdomainname = None
        """the path of the domain"""
        self.path = None

