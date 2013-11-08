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


"""Lists affinity groups"""
from baseCmd import *
from baseResponse import *
class listAffinityGroupsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """list the affinity group by the id provided"""
        self.id = None
        """defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves."""
        self.isrecursive = None
        """List by keyword"""
        self.keyword = None
        """If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false"""
        self.listall = None
        """lists affinity groups by name"""
        self.name = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """lists affinity groups by type"""
        self.type = None
        """lists affinity groups by virtual machine id"""
        self.virtualmachineid = None
        self.required = []

class listAffinityGroupsResponse (baseResponse):
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

