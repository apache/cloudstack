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

    def get_guest_ip6gateway(self):
        if "guestgw6" in self.idata():
            return self.idata()['guestgw6']
        return False

    def get_guest_ip6cidr_size(self):
        if "guestcidr6size" in self.idata():
            return self.idata()['guestcidr6size']
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

    def get_eth0_ip(self):
        if "eth0ip" in self.idata():
            return self.idata()['eth0ip']
        else:
            return False

    def get_cidr_size(self):
        if "cidrsize" in self.idata():
            return self.idata()['cidrsize']
        else:
            return False

    def get_eth2_ip(self):
        if "eth2ip" in self.idata():
            return self.idata()['eth2ip']
        else:
            return "unknown"

    def is_primary(self):
        if not self.is_redundant():
            return False
        if "redundant_state" in self.idata():
            return self.idata()['redundant_state'] == "PRIMARY"
        return False

    def set_fault_state(self):
        self.idata()['redundant_state'] = "FAULT"
        self.idata()['redundant_primary'] = False

    def set_primary_state(self, value):
        if value:
            self.idata()['redundant_state'] = "PRIMARY"
        else:
            self.idata()['redundant_state'] = "BACKUP"
        self.idata()['redundant_primary'] = value

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
        md5.update(passwd.encode())
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

    def get_ip6gateway(self):
        if "ip6gateway" in self.idata():
            return self.idata()['ip6gateway']
        return False

    def get_dev_ip6prelen(self, devname):
        ipkey = devname + "ip6"
        prelenkey = devname + "ip6prelen"
        if ipkey not in self.idata() or prelenkey not in self.idata():
            return False
        return "%s/%s" % (self.idata()[ipkey], self.idata()[prelenkey])

    def get_source_nat_ip(self):
        if "source_nat_ip" in self.idata():
            return self.idata()['source_nat_ip']
        return False


class CsGuestNetwork(CsDataBag):
    """ Get guestnetwork config parameters """

    def get_dev_data(self, devname):
        if devname in self.dbag and isinstance(self.dbag[devname], list) and len(self.dbag[devname]) > 0:
            return self.dbag[devname][0]
        return {}

    def get_dev_ip6gateway(self, devname):
        nw = self.get_dev_data(devname)
        gatewaykey = "router_guest_ip6_gateway"
        if gatewaykey not in nw:
            return False
        return nw[gatewaykey]

    def get_dev_ip6cidr(self, devname):
        nw = self.get_dev_data(devname)
        cidrkey = "cidr6"
        if cidrkey not in nw:
            return False
        return nw[cidrkey]

    def __get_device_router_ip6prelen(self, devname):
        nw = self.get_dev_data(devname)
        ip6key = "router_ip6"
        ip6cidrkey = "router_ip6_cidr"
        if ip6key not in nw or ip6cidrkey not in nw:
            return False
        ip6 = nw[ip6key]
        ip6prelen = nw[ip6cidrkey].split("/")[1]
        return "%s/%s" % (ip6, ip6prelen)

    def get_router_ip6prelen(self, devname=None):
        if devname:
            return self.__get_device_router_ip6prelen(devname)
        else:
            for key in list(self.dbag.keys()):
                ip6prelen = self.__get_device_router_ip6prelen(key)
                if ip6prelen:
                    return ip6prelen
        return False

    def __get_device_router_ip6gateway(self, devname):
        nw = self.get_dev_data(devname)
        ip6gatewaykey = "router_ip6_gateway"
        if ip6gatewaykey not in nw:
            return False
        return nw[ip6gatewaykey]

    def get_router_ip6gateway(self, devname=None):
        if devname:
            return self.__get_device_router_ip6gateway(devname)
        else:
            for key in list(self.dbag.keys()):
                ip6gateway = self.__get_device_router_ip6gateway(key)
                if ip6gateway:
                    return ip6gateway
        return False
