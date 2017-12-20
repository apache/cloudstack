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

from pprint import pprint
from netaddr import *

def merge(dbag, data):
    # A duplicate ip address wil clobber the old value
    # This seems desirable ....
    if "add" in data and data['add'] is False and "ipv4_address" in data:
        if data['ipv4_address'] in dbag:
            del(dbag[data['ipv4_address']])
    else:
        remove_key = None
        for key, entry in dbag.iteritems():
            if key != 'id' and entry['host_name'] == data['host_name']:
                remove_key = key
                break
        if remove_key is not None:
            del(dbag[remove_key])
            
        dbag[data['ipv4_address']] = data

    return dbag
                                                                                                                            
