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


"""Adds Traffic Monitor Host for Direct Network Usage"""
from baseCmd import *
from baseResponse import *
class addTrafficMonitorCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """URL of the traffic monitor Host"""
        """Required"""
        self.url = None
        """Zone in which to add the external firewall appliance."""
        """Required"""
        self.zoneid = None
        """Traffic going into the listed zones will not be metered"""
        self.excludezones = None
        """Traffic going into the listed zones will be metered"""
        self.includezones = None
        self.required = ["url","zoneid",]

class addTrafficMonitorResponse (baseResponse):
    def __init__(self):
        """the ID of the external firewall"""
        self.id = None
        """the management IP address of the external firewall"""
        self.ipaddress = None
        """the number of times to retry requests to the external firewall"""
        self.numretries = None
        """the timeout (in seconds) for requests to the external firewall"""
        self.timeout = None
        """the zone ID of the external firewall"""
        self.zoneid = None

