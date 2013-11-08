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


"""lists all available apis on the server, provided by the Api Discovery plugin"""
from baseCmd import *
from baseResponse import *
class listApisCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """API name"""
        self.name = None
        self.required = []

class listApisResponse (baseResponse):
    def __init__(self):
        """description of the api"""
        self.description = None
        """true if api is asynchronous"""
        self.isasync = None
        """the name of the api command"""
        self.name = None
        """comma separated related apis"""
        self.related = None
        """version of CloudStack the api was introduced in"""
        self.since = None
        """response field type"""
        self.type = None
        """the list params the api accepts"""
        self.params = []
        """api response fields"""
        self.response = []

class params:
    def __init__(self):
        """"description of the api parameter"""
        self.description = None
        """"length of the parameter"""
        self.length = None
        """"the name of the api parameter"""
        self.name = None
        """"comma separated related apis to get the parameter"""
        self.related = None
        """"true if this parameter is required for the api request"""
        self.required = None
        """"version of CloudStack the api was introduced in"""
        self.since = None
        """"parameter type"""
        self.type = None

class response:
    def __init__(self):
        """"description of the api response field"""
        self.description = None
        """"the name of the api response field"""
        self.name = None
        """"api response fields"""
        self.response = None
        """"response field type"""
        self.type = None

