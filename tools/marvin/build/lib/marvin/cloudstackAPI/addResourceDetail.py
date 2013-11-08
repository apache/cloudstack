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


"""Adds detail for the Resource."""
from baseCmd import *
from baseResponse import *
class addResourceDetailCmd (baseCmd):
    def __init__(self):
        self.isAsync = "true"
        """Map of (key/value pairs)"""
        """Required"""
        self.details = []
        """resource id to create the details for"""
        """Required"""
        self.resourceid = None
        """type of the resource"""
        """Required"""
        self.resourcetype = None
        self.required = ["details","resourceid","resourcetype",]

class addResourceDetailResponse (baseResponse):
    def __init__(self):
        """any text associated with the success or failure"""
        self.displaytext = None
        """true if operation is executed successfully"""
        self.success = None

