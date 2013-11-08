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


"""List Conditions for the specific user"""
from baseCmd import *
from baseResponse import *
class listConditionsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """Counter-id of the condition."""
        self.counterid = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """ID of the Condition."""
        self.id = None
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
        """the ID of the policy"""
        self.policyid = None
        self.required = []

class listConditionsResponse (baseResponse):
    def __init__(self):
        """the id of the Condition"""
        self.id = None
        """the owner of the Condition."""
        self.account = None
        """Details of the Counter."""
        self.counter = None
        """the domain name of the owner."""
        self.domain = None
        """the domain id of the Condition owner"""
        self.domainid = None
        """the project name of the Condition"""
        self.project = None
        """the project id of the Condition."""
        self.projectid = None
        """Relational Operator to be used with threshold."""
        self.relationaloperator = None
        """Threshold Value for the counter."""
        self.threshold = None
        """zone id of counter"""
        self.zoneid = None

