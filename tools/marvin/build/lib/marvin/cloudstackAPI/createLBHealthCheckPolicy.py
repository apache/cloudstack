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


"""Creates a Load Balancer healthcheck policy"""
from baseCmd import *
from baseResponse import *
class createLBHealthCheckPolicyCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the ID of the load balancer rule"""
        """Required"""
        self.lbruleid = None
        """the description of the load balancer HealthCheck policy"""
        self.description = None
        """Number of consecutive health check success before declaring an instance healthy"""
        self.healthythreshold = None
        """Amount of time between health checks (1 sec - 20940 sec)"""
        self.intervaltime = None
        """HTTP Ping Path"""
        self.pingpath = None
        """Time to wait when receiving a response from the health check (2sec - 60 sec)"""
        self.responsetimeout = None
        """Number of consecutive health check failures before declaring an instance unhealthy"""
        self.unhealthythreshold = None
        self.required = ["lbruleid",]

class createLBHealthCheckPolicyResponse (baseResponse):
    def __init__(self):
        """the account of the HealthCheck policy"""
        self.account = None
        """the domain of the HealthCheck policy"""
        self.domain = None
        """the domain ID of the HealthCheck policy"""
        self.domainid = None
        """the LB rule ID"""
        self.lbruleid = None
        """the id of the zone the HealthCheck policy belongs to"""
        self.zoneid = None
        """the list of healthcheckpolicies"""
        self.healthcheckpolicy = []

class healthcheckpolicy:
    def __init__(self):
        """"the LB HealthCheck policy ID"""
        self.id = None
        """"the description of the healthcheck policy"""
        self.description = None
        """"Amount of time between health checks"""
        self.healthcheckinterval = None
        """"Number of consecutive health check success before declaring an instance healthy"""
        self.healthcheckthresshold = None
        """"the pingpath  of the healthcheck policy"""
        self.pingpath = None
        """"Time to wait when receiving a response from the health check"""
        self.responsetime = None
        """"the state of the policy"""
        self.state = None
        """"Number of consecutive health check failures before declaring an instance unhealthy."""
        self.unhealthcheckthresshold = None

