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
import sys
import traceback
class CloudRuntimeException(Exception):
    def __init__(self, errMsg):
        self.errMsg = errMsg

        value = sys.exc_info()[1]
        if value is not None:
            self.errMsg += ", due to:" + str(value)
      
        self.details = formatExceptionInfo()
    def __str__(self):
        return self.errMsg
    def getDetails(self):
        return self.details

class CloudInternalException(Exception):
    def __init__(self, errMsg):
        self.errMsg = errMsg
    def __str__(self):
        return self.errMsg

def formatExceptionInfo(maxTBlevel=5):
    cla, exc, trbk = sys.exc_info()
    excTb = traceback.format_tb(trbk, maxTBlevel)
    msg = str(exc) + "\n"
    for tb in excTb:
        msg += tb
    return msg
