# -- coding: utf-8 --
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
from pprint import pprint
from netaddr import *


def merge(dbag, ip):
    added = False
    for dev in dbag:
        if dev == "id":
            continue
        for address in dbag[dev]:
            if address['public_ip'] == ip['public_ip']:
                dbag[dev].remove(address)

    ipo = IPNetwork(ip['public_ip'] + '/' + ip['netmask'])
    ip['device'] = 'eth' + str(ip['nic_dev_id'])
    ip['broadcast'] = str(ipo.broadcast)
    ip['cidr'] = str(ipo.ip) + '/' + str(ipo.prefixlen)
    ip['size'] = str(ipo.prefixlen)
    ip['network'] = str(ipo.network) + '/' + str(ipo.prefixlen)
    if 'nw_type' not in ip.keys():
        ip['nw_type'] = 'public'
    if ip['nw_type'] == 'control':
        dbag['eth' + str(ip['nic_dev_id'])] = [ip]
    else:
        dbag.setdefault('eth' + str(ip['nic_dev_id']), []).append(ip)

    return dbag
