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
'''
This will help pass webdriver (Browser instance) across our test cases.
'''



from selenium import webdriver
import sys

DRIVER = None
MS_ip = None


def getOrCreateWebdriver():
    global DRIVER
    DRIVER = DRIVER or webdriver.PhantomJS('phantomjs')  # phantomjs executable must be in PATH.
    return DRIVER

    
def getMSip():
    global MS_ip
    if len(sys.argv) >= 3:
            sys.exit("Only One argument is required .. Enter your Management Server IP")

    if len(sys.argv) == 1:
            sys.exit("Atleast One argument is required .. Enter your Management Server IP")

    for arg in sys.argv[1:]:
        MS_ip = arg
        return MS_ip