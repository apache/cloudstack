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


"""Lists all alerts."""
from baseCmd import *
from baseResponse import *
class listAlertsCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the ID of the alert"""
        self.id = None
        """List by keyword"""
        self.keyword = None
        """"""
        self.page = None
        """"""
        self.pagesize = None
        """list by alert type"""
        self.type = None
        self.required = []

class listAlertsResponse (baseResponse):
    def __init__(self):
        """the id of the alert"""
        self.id = None
        """description of the alert"""
        self.description = None
        """the date and time the alert was sent"""
        self.sent = None
        """One of the following alert types: MEMORY = 0, CPU = 1, STORAGE = 2, STORAGE_ALLOCATED = 3, PUBLIC_IP = 4, PRIVATE_IP = 5, HOST = 6, USERVM = 7, DOMAIN_ROUTER = 8, CONSOLE_PROXY = 9, ROUTING = 10: lost connection to default route (to the gateway), STORAGE_MISC = 11: lost connection to default route (to the gateway), USAGE_SERVER = 12: lost connection to default route (to the gateway), MANAGMENT_NODE = 13: lost connection to default route (to the gateway), DOMAIN_ROUTER_MIGRATE = 14, CONSOLE_PROXY_MIGRATE = 15, USERVM_MIGRATE = 16, VLAN = 17, SSVM = 18, USAGE_SERVER_RESULT = 19"""
        self.type = None

