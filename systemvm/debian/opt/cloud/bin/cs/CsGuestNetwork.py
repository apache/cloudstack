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
from merge import DataBag
from . import CsHelper


class CsGuestNetwork:
    def __init__(self, device, config):
        self.data = {}
        self.guest = True
        db = DataBag()
        db.setKey("guestnetwork")
        db.load()
        dbag = db.getDataBag()
        self.config = config
        if device in list(dbag.keys()) and len(dbag[device]) != 0:
            self.data = dbag[device][0]
        else:
            self.guest = False

    def is_guestnetwork(self):
        return self.guest

    def get_dns(self):
        if not self.guest:
            return self.config.get_dns()

        dns = []
        if 'router_guest_gateway' in self.data and not self.config.use_extdns():
            dns.append(self.data['router_guest_gateway'])

        if 'dns' in self.data:
            dns.extend(self.data['dns'].split(','))

        return dns or ['']

    def set_dns(self, val):
        self.data['dns'] = val

    def set_router(self, val):
        self.data['router_guest_gateway'] = val

    def get_netmask(self):
        # We need to fix it properly. I just added the if, as Ian did in some other files, to avoid the exception.
        if 'router_guest_netmask' in self.data:
            return self.data['router_guest_netmask']
        return ''

    def get_gateway(self):
        # We need to fix it properly. I just added the if, as Ian did in some other files, to avoid the exception.
        if 'router_guest_gateway' in self.data:
            return self.data['router_guest_gateway']
        return ''

    def get_domain(self):
        domain = "cloudnine.internal"
        if not self.guest:
            return self.config.get_domain()

        if 'domain_name' in self.data:
            return self.data['domain_name']

        return domain
