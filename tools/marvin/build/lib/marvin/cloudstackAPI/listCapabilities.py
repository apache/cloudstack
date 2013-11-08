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


"""Lists capabilities"""
from baseCmd import *
from baseResponse import *
class listCapabilitiesCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        self.required = []

class listCapabilitiesResponse (baseResponse):
    def __init__(self):
        """true if regular user is allowed to create projects"""
        self.allowusercreateprojects = None
        """time interval (in seconds) to reset api count"""
        self.apilimitinterval = None
        """Max allowed number of api requests within the specified interval"""
        self.apilimitmax = None
        """version of the cloud stack"""
        self.cloudstackversion = None
        """maximum size that can be specified when create disk from disk offering with custom size"""
        self.customdiskofferingmaxsize = None
        """If invitation confirmation is required when add account to project"""
        self.projectinviterequired = None
        """true if region wide secondary is enabled, false otherwise"""
        self.regionsecondaryenabled = None
        """true if security groups support is enabled, false otherwise"""
        self.securitygroupsenabled = None
        """true if region supports elastic load balancer on basic zones"""
        self.supportELB = None
        """true if user and domain admins can set templates to be shared, false otherwise"""
        self.userpublictemplateenabled = None

