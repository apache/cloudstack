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


"""Lists vm groups"""
from baseCmd import *
from baseResponse import *
class listInstanceGroupsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """list instance groups by ID"""
        self.id = None
        """defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves."""
        self.isrecursive = None
        """List by keyword"""
        self.keyword = None
        """If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false"""
        self.listall = None
        """list instance groups by name"""
        self.name = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """list objects by project"""
        self.projectid = None
        self.required = []

class listInstanceGroupsResponse (baseResponse):
    def __init__(self):
        """the id of the instance group"""
        self.id = None
        """the account owning the instance group"""
        self.account = None
        """time and date the instance group was created"""
        self.created = None
        """the domain name of the instance group"""
        self.domain = None
        """the domain ID of the instance group"""
        self.domainid = None
        """the name of the instance group"""
        self.name = None
        """the project name of the group"""
        self.project = None
        """the project id of the group"""
        self.projectid = None

