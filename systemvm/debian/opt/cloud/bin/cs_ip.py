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

import os
from ipaddress import *


def macdevice_map():
    device_map = {}
    for eth in os.listdir('/sys/class/net'):
        if not eth.startswith('eth'):
            continue
        with open('/sys/class/net/%s/address' % eth) as f:
            mac_address = f.read().replace('\n', '')
            device_map[mac_address] = eth[3:]
    return device_map


def merge(dbag, ip):
    nic_dev_id = None
    for dev in dbag:
        if dev == "id":
            continue
        for address in dbag[dev]:
            if address['public_ip'] == ip['public_ip']:
                if 'nic_dev_id' in address:
                    nic_dev_id = address['nic_dev_id']
                dbag[dev].remove(address)

    ipo = ip_network(ip['public_ip'] + '/' + ip['netmask'])
    if 'nic_dev_id' in ip:
        nic_dev_id = ip['nic_dev_id']
    if 'vif_mac_address' in ip:
        mac_address = ip['vif_mac_address']
        device_map = macdevice_map()
        if mac_address in device_map:
            nic_dev_id = device_map[mac_address]
    ip['device'] = 'eth' + str(nic_dev_id)
    ip['broadcast'] = str(ipo.broadcast_address)
    ip['cidr'] = str(ipo.network_address) + '/' + str(ipo.prefixlen)
    ip['size'] = str(ipo.prefixlen)
    ip['network'] = str(ipo.compressed)
    if 'nw_type' not in list(ip.keys()):
        ip['nw_type'] = 'public'
    else:
        ip['nw_type'] = ip['nw_type'].lower()
    if ip['nw_type'] == 'control':
        dbag[ip['device']] = [ip]
    else:
        if 'source_nat' in ip and ip['source_nat'] and ip['device'] in dbag and len(dbag[ip['device']]) > 0:
            dbag[ip['device']].insert(0, ip)  # Make sure the source_nat ip is first (primary) on the device
        else:
            dbag.setdefault(ip['device'], []).append(ip)

    return dbag
