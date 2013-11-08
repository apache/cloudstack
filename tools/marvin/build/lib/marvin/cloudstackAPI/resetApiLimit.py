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


"""Reset api count"""
from baseCmd import *
from baseResponse import *
class resetApiLimitCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """the ID of the acount whose limit to be reset"""
        self.account = None
        self.required = []

class resetApiLimitResponse (baseResponse):
    def __init__(self):
        """the account name of the api remaining count"""
        self.account = None
        """the account uuid of the api remaining count"""
        self.accountid = None
        """currently allowed number of apis"""
        self.apiAllowed = None
        """number of api already issued"""
        self.apiIssued = None
        """seconds left to reset counters"""
        self.expireAfter = None

