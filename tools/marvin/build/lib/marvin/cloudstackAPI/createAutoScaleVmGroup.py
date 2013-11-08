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


"""Creates and automatically starts a virtual machine based on a service offering, disk offering, and template."""
from baseCmd import *
from baseResponse import *
class createAutoScaleVmGroupCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the ID of the load balancer rule"""
        """Required"""
        self.lbruleid = None
        """the maximum number of members in the vmgroup, The number of instances in the vm group will be equal to or less than this number."""
        """Required"""
        self.maxmembers = None
        """the minimum number of members in the vmgroup, the number of instances in the vm group will be equal to or more than this number."""
        """Required"""
        self.minmembers = None
        """list of scaledown autoscale policies"""
        """Required"""
        self.scaledownpolicyids = []
        """list of scaleup autoscale policies"""
        """Required"""
        self.scaleuppolicyids = []
        """the autoscale profile that contains information about the vms in the vm group."""
        """Required"""
        self.vmprofileid = None
        """the frequency at which the conditions have to be evaluated"""
        self.interval = None
        self.required = ["lbruleid","maxmembers","minmembers","scaledownpolicyids","scaleuppolicyids","vmprofileid",]

class createAutoScaleVmGroupResponse (baseResponse):
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

