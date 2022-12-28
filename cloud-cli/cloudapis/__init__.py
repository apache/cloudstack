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
Created on Aug 2, 2010

'''

import os,pkgutil

def get_all_apis():
    apis = []
    for x in pkgutil.walk_packages([os.path.dirname(__file__)]):
        loader = x[0].find_module(x[1])
        try: module = loader.load_module("cloudapis." + x[1])
        except ImportError: continue
        apis.append(module)
    return apis

def lookup_api(api_name):
    api = None
    matchingapi = [ x for x in get_all_apis() if api_name.replace("-","_") == x.__name__.split(".")[-1] ]
    if not matchingapi: api = None
    else: api = matchingapi[0]
    if api: api = getattr(api,"implementor")
    return api
