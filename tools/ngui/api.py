#Licensed to the Apache Software Foundation (ASF) under one
#or more contributor license agreements.  See the NOTICE file
#distributed with this work for additional information
#regarding copyright ownership.  The ASF licenses this file
#to you under the Apache License, Version 2.0 (the
#"License"); you may not use this file except in compliance
#with the License.  You may obtain a copy of the License at
#http://www.apache.org/licenses/LICENSE-2.0
#Unless required by applicable law or agreed to in writing,
#software distributed under the License is distributed on an
#"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#KIND, either express or implied.  See the License for the
#specific language governing permissions and limitations
#under the License.

from requester import make_request
from precache import apicache
from config import *
import re

def get_error_code(error):
    return int(re.findall("\d{3}",error)[0]) #Find the error code by regular expression
    #    return int(error[11:14]) #Ugly

def get_command(verb, subject):
    commandlist = apicache.get(verb, None)
    if commandlist is not None:
        command = commandlist.get(subject, None)
        if command is not None:
            return command["name"]
    return None

def apicall(command, data ):
    response, error = make_request(command, data, None, host, port, apikey, secretkey, protocol, path)
    if error is not None:
        return error, get_error_code(error)
    return response
