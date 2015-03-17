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

keys = ['eth1', 'eth2', 'eth3', 'eth4', 'eth5', 'eth6', 'eth7', 'eth8', 'eth9']


def merge(dbag, gn):
    device = gn['device']

    if not gn['add'] and device in dbag:

        if dbag[device]:
            device_to_die = dbag[device][0]
            try:
                dbag[device].remove(device_to_die)
            except ValueError, e:
                print "[WARN] cs_guestnetwork.py :: Error occurred removing item from databag. => %s" % device_to_die
                del(dbag[device])
        else:
            del(dbag[device])

    else:
        dbag.setdefault(device, []).append(gn)

    return dbag
