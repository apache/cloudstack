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


"""Dedicates a zones."""
from baseCmd import *
from baseResponse import *
class dedicateZoneCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """the ID of the containing domain"""
        """Required"""
        self.domainid = None
        """the ID of the zone"""
        """Required"""
        self.zoneid = None
        """the name of the account which needs dedication. Must be used with domainId."""
        self.account = None
        self.required = ["domainid","zoneid",]

class dedicateZoneResponse (baseResponse):
    def __init__(self):
        """the ID of the dedicated resource"""
        self.id = None
        """the Account Id to which the Zone is dedicated"""
        self.accountid = None
        """the Dedication Affinity Group ID of the zone"""
        self.affinitygroupid = None
        """the domain ID to which the Zone is dedicated"""
        self.domainid = None
        """the ID of the Zone"""
        self.zoneid = None
        """the Name of the Zone"""
        self.zonename = None

