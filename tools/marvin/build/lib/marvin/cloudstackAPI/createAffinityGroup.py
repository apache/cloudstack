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


"""Creates an affinity/anti-affinity group"""
from baseCmd import *
from baseResponse import *
class createAffinityGroupCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """name of the affinity group"""
        """Required"""
        self.name = None
        """Type of the affinity group from the available affinity/anti-affinity group types"""
        """Required"""
        self.type = None
        """an account for the affinity group. Must be used with domainId."""
        self.account = None
        """optional description of the affinity group"""
        self.description = None
        """domainId of the account owning the affinity group"""
        self.domainid = None
        self.required = ["name","type",]

class createAffinityGroupResponse (baseResponse):
    def __init__(self):
        """the ID of the affinity group"""
        self.id = None
        """the account owning the affinity group"""
        self.account = None
        """the description of the affinity group"""
        self.description = None
        """the domain name of the affinity group"""
        self.domain = None
        """the domain ID of the affinity group"""
        self.domainid = None
        """the name of the affinity group"""
        self.name = None
        """the type of the affinity group"""
        self.type = None
        """virtual machine Ids associated with this affinity group"""
        self.virtualmachineIds = None

