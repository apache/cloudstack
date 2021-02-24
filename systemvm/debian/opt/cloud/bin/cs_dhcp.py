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

import logging
from ipaddress import *


def merge(dbag, data):
    # A duplicate ip address wil clobber the old value
    # This seems desirable ....
    if "add" in data and data['add'] is False and "ipv4_address" in data:
        if data['ipv4_address'] in dbag:
            del(dbag[data['ipv4_address']])
    else:
        remove_keys = set()
        for key, entry in list(dbag.items()):
            if key != 'id' and entry['mac_address'] == data['mac_address'] and data['remove']:
                remove_keys.add(key)
                break

        for remove_key in remove_keys:
            del(dbag[remove_key])

        dbag[data['ipv4_address']] = data

    return dbag
