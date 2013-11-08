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


"""Lists autoscale vm groups."""
from baseCmd import *
from baseResponse import *
class listAutoScaleVmGroupsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """the ID of the autoscale vm group"""
        self.id = None
        """defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves."""
        self.isrecursive = None
        """List by keyword"""
        self.keyword = None
        """the ID of the loadbalancer"""
        self.lbruleid = None
        """If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false"""
        self.listall = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """the ID of the policy"""
        self.policyid = None
        """list objects by project"""
        self.projectid = None
        """the ID of the profile"""
        self.vmprofileid = None
        """the availability zone ID"""
        self.zoneid = None
        self.required = []

class listAutoScaleVmGroupsResponse (baseResponse):
    def __init__(self):
        """the autoscale vm group ID"""
        self.id = None
        """the account owning the instance group"""
        self.account = None
        """the domain name of the vm profile"""
        self.domain = None
        """the domain ID of the vm profile"""
        self.domainid = None
        """the frequency at which the conditions have to be evaluated"""
        self.interval = None
        """the load balancer rule ID"""
        self.lbruleid = None
        """the maximum number of members in the vmgroup, The number of instances in the vm group will be equal to or less than this number."""
        self.maxmembers = None
        """the minimum number of members in the vmgroup, the number of instances in the vm group will be equal to or more than this number."""
        self.minmembers = None
        """the project name of the vm profile"""
        self.project = None
        """the project id vm profile"""
        self.projectid = None
        """list of scaledown autoscale policies"""
        self.scaledownpolicies = None
        """list of scaleup autoscale policies"""
        self.scaleuppolicies = None
        """the current state of the AutoScale Vm Group"""
        self.state = None
        """the autoscale profile that contains information about the vms in the vm group."""
        self.vmprofileid = None

