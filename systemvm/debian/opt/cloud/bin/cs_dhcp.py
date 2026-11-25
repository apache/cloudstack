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
from netaddr import *


def merge(dbag, data):
    # A duplicate ip address wil clobber the old value
    # This seems desirable ....
    if "add" in data and data['add'] is False and "ipv4_address" in data:
        if data['ipv4_address'] in dbag:
            del dbag[data['ipv4_address']]
    else:
        # A multi-VLAN trunk nic's one MAC can legitimately hold several simultaneous entries - its
        # primary network's, plus one per associated network. Only ever replace an existing entry for
        # this MAC when the incoming entry is itself not an associated-network entry, and only replace
        # an existing entry that is also not one - otherwise an association's push would delete the
        # nic's own primary entry (or a sibling association's), and vice versa.
        if not data.get('associated_network', False):
            remove_keys = set()
            for key, entry in dbag.items():
                if key != 'id' and entry['mac_address'] == data['mac_address'] and not entry.get('associated_network', False):
                    remove_keys.add(key)
                    break

            for remove_key in remove_keys:
                del dbag[remove_key]

        dbag[data['ipv4_address']] = data

    return dbag
