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


"""Lists autoscale vm profiles."""
from baseCmd import *
from baseResponse import *
class listAutoScaleVmProfilesCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """list resources by account. Must be used with the domainId parameter."""
        self.account = None
        """list only resources belonging to the domain specified"""
        self.domainid = None
        """the ID of the autoscale vm profile"""
        self.id = None
        """defaults to false, but if true, lists all resources from the parent specified by the domainId till leaves."""
        self.isrecursive = None
        """List by keyword"""
        self.keyword = None
        """If set to false, list only resources belonging to the command's caller; if set to true - list resources that the caller is authorized to see. Default value is false"""
        self.listall = None
        """the otherdeployparameters of the autoscale vm profile"""
        self.otherdeployparams = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """list objects by project"""
        self.projectid = None
        """the templateid of the autoscale vm profile"""
        self.templateid = None
        self.required = []

class listAutoScaleVmProfilesResponse (baseResponse):
    def __init__(self):
        """the autoscale vm profile ID"""
        self.id = None
        """the account owning the instance group"""
        self.account = None
        """the ID of the user used to launch and destroy the VMs"""
        self.autoscaleuserid = None
        """the time allowed for existing connections to get closed before a vm is destroyed"""
        self.destroyvmgraceperiod = None
        """the domain name of the vm profile"""
        self.domain = None
        """the domain ID of the vm profile"""
        self.domainid = None
        """parameters other than zoneId/serviceOfferringId/templateId to be used while deploying a virtual machine"""
        self.otherdeployparams = None
        """the project name of the vm profile"""
        self.project = None
        """the project id vm profile"""
        self.projectid = None
        """the service offering to be used while deploying a virtual machine"""
        self.serviceofferingid = None
        """the template to be used while deploying a virtual machine"""
        self.templateid = None
        """the availability zone to be used while deploying a virtual machine"""
        self.zoneid = None

