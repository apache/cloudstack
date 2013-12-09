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
"""
@Desc     : This module defines all codes, constants maintained globally \
            and used across marvin and its test features.The main purpose \
            is to maintain readability, maintain one common place for \
            all codes used or reused across test features. It enhances \
            maintainability and readability. Users just import statement \
            to receive all the codes mentioned here. EX: Here, we define \
            a code viz., ENABLED  with value "Enabled",then using \
            this code in a sample feature say test_a.py as below. \

            from codes import *
            if obj.getvalue() == ENABLED

@DateAdded: 20th October 2013
"""

RUNNING = "Running"
RECURRING = "RECURRING"
ENABLED = "Enabled"
NETWORK_OFFERING = "network_offering"
ROOT = "ROOT"
INVALID_INPUT = "INVALID INPUT"
EMPTY_LIST = "EMPTY_LIST"
FAIL = 0
PASS = 1
MATCH_NOT_FOUND = "ELEMENT NOT FOUND IN THE INPUT"
SUCCESS = "SUCCESS"
EXCEPTION_OCCURRED = "Exception Occurred"
NO = "no"
YES = "yes"
FAILED = "FAILED"
UNKNOWN_ERROR = "Unknown Error"
EXCEPTION = "EXCEPTION"
