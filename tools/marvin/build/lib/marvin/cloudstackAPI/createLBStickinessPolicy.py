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


"""Creates a Load Balancer stickiness policy"""
from baseCmd import *
from baseResponse import *
class createLBStickinessPolicyCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the ID of the load balancer rule"""
        """Required"""
        self.lbruleid = None
        """name of the LB Stickiness policy method, possible values can be obtained from ListNetworks API"""
        """Required"""
        self.methodname = None
        """name of the LB Stickiness policy"""
        """Required"""
        self.name = None
        """the description of the LB Stickiness policy"""
        self.description = None
        """param list. Example: param[0].name=cookiename&param[0].value=LBCookie"""
        self.param = []
        self.required = ["lbruleid","methodname","name",]

class createLBStickinessPolicyResponse (baseResponse):
    def __init__(self):
        """the account of the Stickiness policy"""
        self.account = None
        """the description of the Stickiness policy"""
        self.description = None
        """the domain of the Stickiness policy"""
        self.domain = None
        """the domain ID of the Stickiness policy"""
        self.domainid = None
        """the LB rule ID"""
        self.lbruleid = None
        """the name of the Stickiness policy"""
        self.name = None
        """the state of the policy"""
        self.state = None
        """the id of the zone the Stickiness policy belongs to"""
        self.zoneid = None
        """the list of stickinesspolicies"""
        self.stickinesspolicy = []

class stickinesspolicy:
    def __init__(self):
        """"the LB Stickiness policy ID"""
        self.id = None
        """"the description of the Stickiness policy"""
        self.description = None
        """"the method name of the Stickiness policy"""
        self.methodname = None
        """"the name of the Stickiness policy"""
        self.name = None
        """"the params of the policy"""
        self.params = None
        """"the state of the policy"""
        self.state = None

