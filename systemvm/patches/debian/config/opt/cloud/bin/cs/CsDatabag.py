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
from merge import DataBag


class CsDataBag(object):

    def __init__(self, key, config=None):
        self.data = {}
        self.db = DataBag()
        self.db.setKey(key)
        self.db.load()
        self.dbag = self.db.getDataBag()
        if config:
            self.fw = config.get_fw()
            self.cl = config.cmdline()
            self.config = config

    def dump(self):
        print self.dbag

    def get_bag(self):
        return self.dbag

    def process(self):
        pass

    def save(self):
        """
        Call to the databag save routine
        Use sparingly!
        """
        self.db.save(self.dbag)


class CsCmdLine(CsDataBag):
    """ Get cmdline config parameters """

    def idata(self):
        if "config" not in self.dbag:
            self.dbag['config'] = {}
        return self.dbag['config']

    def get_priority(self):
        if "router_pr" in self.idata():
            return self.idata()['router_pr']
        return 99

    def set_priority(self, val):
        self.idata()['router_pr'] = val

    def is_redundant(self):
        if "redundant_router" in self.idata():
            return self.idata()['redundant_router'] == "true"
        return False

    def set_redundant(self, val="true"):
        self.idata()['redundant_router'] = val

    def set_guest_gw(self, val):
        self.idata()['guestgw'] = val

    def get_guest_gw(self):
        if "guestgw" in self.idata():
            return self.idata()['guestgw']
        else:
            return "1.2.3.4"

    def get_guest_gw_cidr(self):
        if "guestgw" in self.idata():
            return "%s/%s" % (self.idata()['guestgw'], self.idata()['guestcidrsize'])
        else:
            return "1.2.3.4/8"

    def get_name(self):
        if "name" in self.idata():
            return self.idata()['name']
        else:
            return "unloved-router"

    def get_dns(self):
        dns = []
        names = "dns1 dns2"
        for name in names:
            if name in self.idata():
                dns.append(self.idata()[name])
        return dns

    def get_type(self):
        if "type" in self.idata():
            return self.idata()['type']
        else:
            return "unknown"

    def get_domain(self):
        if "domain" in self.idata():
            return self.idata()['domain']
        else:
            return "cloudnine.internal"

    def get_vpccidr(self):
        if "vpccidr" in self.idata():
            return self.idata()['vpccidr']
        else:
            return "unknown"

    def is_master(self):
        if not self.is_redundant():
            return False
        if "redundant_master" in self.idata():
            return self.idata()['redundant_master'] == "true"
        return False
