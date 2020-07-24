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
import hashlib
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
        print(self.dbag)

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

    def set_guest_gw(self, val):
        self.idata()['guestgw'] = val

    def get_guest_gw(self):
        if "guestgw" in self.idata():
            return self.idata()['guestgw']
        return False

    def is_redundant(self):
        if "redundant_router" in self.idata():
            return self.idata()['redundant_router'] == "true"
        return False

    def set_redundant(self, val="true"):
        self.idata()['redundant_router'] = val

    def get_name(self):
        if "name" in self.idata():
            return self.idata()['name']
        else:
            return "unloved-router"

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

    def get_eth2_ip(self):
        if "eth2ip" in self.idata():
            return self.idata()['eth2ip']
        else:
            return "unknown"

    def is_master(self):
        if not self.is_redundant():
            return False
        if "redundant_state" in self.idata():
            return self.idata()['redundant_state'] == "MASTER"
        return False

    def set_fault_state(self):
        self.idata()['redundant_state'] = "FAULT"
        self.idata()['redundant_master'] = False

    def set_master_state(self, value):
        if value:
            self.idata()['redundant_state'] = "MASTER"
        else:
            self.idata()['redundant_state'] = "BACKUP"
        self.idata()['redundant_master'] = value

    def get_router_id(self):
        if "router_id" in self.idata():
            return self.idata()['router_id']
        return 1

    def get_router_password(self):
        if "router_password" in self.idata():
            return self.idata()['router_password']

        '''
        Generate a password based on the router id just to avoid hard-coded passwd.
        Remark: if for some reason 1 router gets configured, the other one will have a different password.
        This is slightly difficult to happen, but if it does, destroy the router with the password generated with the
        code below and restart the VPC with out the clean up option.
        '''
        if self.get_type() == 'router':
            passwd = "%s-%s" % (self.get_eth2_ip(), self.get_router_id())
        else:
            passwd = "%s-%s" % (self.get_vpccidr(), self.get_router_id())
        md5 = hashlib.md5()
        md5.update(passwd)
        return md5.hexdigest()

    def get_gateway(self):
        if "gateway" in self.idata():
            return self.idata()['gateway']
        return False

    def get_use_ext_dns(self):
        if "useextdns" in self.idata():
            return self.idata()['useextdns']
        return False

    def get_advert_int(self):
        if 'advert_int' in self.idata():
            return self.idata()['advert_int']
        return 1
