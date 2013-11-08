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


"""This command allows a user to register for the developer API, returning a secret key and an API key. This request is made through the integration API port, so it is a privileged command and must be made on behalf of a user. It is up to the implementer just how the username and password are entered, and then how that translates to an integration API request. Both secret key and API key should be returned to the user"""
from baseCmd import *
from baseResponse import *
class registerUserKeysCmd (baseCmd):
    def __init__(self):
        self.isAsync = "false"
        """User id"""
        """Required"""
        self.id = None
        self.required = ["id",]

class registerUserKeysResponse (baseResponse):
    def __init__(self):
        """the api key of the registered user"""
        self.apikey = None
        """the secret key of the registered user"""
        self.secretkey = None

