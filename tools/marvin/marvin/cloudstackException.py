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
from marvin.codes import (INVALID_INPUT, EXCEPTION_OCCURRED)


class CloudstackAPIException(Exception):

    def __init__(self, cmd="", result=""):
        self.errorMsg = "Execute cmd: %s failed, due to: %s" % (cmd, result)

    def __str__(self):
        return self.errorMsg


class InvalidParameterException(Exception):

    def __init__(self, msg=''):
        self.errorMsg = msg

    def __str__(self):
        return self.errorMsg


class dbException(Exception):

    def __init__(self, msg=''):
        self.errorMsg = msg

    def __str__(self):
        return self.errorMsg


class internalError(Exception):

    def __init__(self, msg=''):
        self.errorMsg = msg

    def __str__(self):
        return self.errorMsg


def GetDetailExceptionInfo(e):
    if e is not None:
        if type(e) is str:
            return e
        elif type(e) is tuple:
            (exc_type, exc_value, exc_traceback) = e
        else:
            exc_type, exc_value, exc_traceback = sys.exc_info()
        return str(repr(traceback.format_exception(
            exc_type, exc_value, exc_traceback)))
    else:
        return EXCEPTION_OCCURRED

class CloudstackAclException():

    NO_PERMISSION_TO_OPERATE_DOMAIN = "does not have permission to operate within domain"
    UNABLE_TO_USE_NETWORK = "Unable to use network"
    NO_PERMISSION_TO_OPERATE_ACCOUNT = "does not have permission to operate with resource"
    UNABLE_TO_LIST_NETWORK_ACCOUNT = "Can't create/list resources for account"
    NO_PERMISSION_TO_ACCESS_ACCOUNT = "does not have permission to access resource Acct"
    NOT_AVAILABLE_IN_DOMAIN = "not available in domain"

    @staticmethod
    def verifyMsginException(e,message):
        if message in str(e):
            return True
        else:
            return False

    @staticmethod
    def verifyErrorCodeinException(e,errorCode):
        errorString = " errorCode: " + errorCode
        if  errorString in str(e):
            return True
        else:
            return False
