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

    search(dbag, data['host_name'])
    # A duplicate ip address wil clobber the old value
    # This seems desirable ....
    if "add" in data and data['add'] is False and \
            "ipv4_adress" in data:
        if data['ipv4_adress'] in dbag:
            del(dbag[data['ipv4_adress']])
        return dbag
    else:
        dbag[data['ipv4_adress']] = data
    return dbag


def search(dbag, name):
    """
    Dirty hack because CS does not deprovision hosts
    """
    hosts = []
    for o in dbag:
        if o == 'id':
            continue
        print "%s %s" % (dbag[o]['host_name'], name)
        if dbag[o]['host_name'] == name:
            hosts.append(o)
    for o in hosts:
        del(dbag[o])
