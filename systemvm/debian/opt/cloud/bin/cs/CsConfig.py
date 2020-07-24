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

from .CsDatabag import CsCmdLine
from .CsAddress import CsAddress
import logging


class CsConfig(object):
    """
    A class to cache all the stuff that the other classes need
    """
    __LOG_FILE = "/var/log/cloud.log"
    __LOG_LEVEL = "INFO"
    __LOG_FORMAT = "%(asctime)s %(levelname)-8s %(message)s"
    cl = None

    def __init__(self):
        self.fw = []

    def set_address(self):
        self.ips = CsAddress("ips", self)

    @classmethod
    def get_cmdline_instance(cls):
        if cls.cl is None:
            cls.cl = CsCmdLine("cmdline")
        return cls.cl

    def cmdline(self):
        return self.get_cmdline_instance()

    def address(self):
        return self.ips

    def get_fw(self):
        return self.fw

    def get_logger(self):
        return self.__LOG_FILE

    def get_level(self):
        return self.__LOG_LEVEL

    def is_vpc(self):
        return self.cl.get_type() == 'vpcrouter'

    def is_router(self):
        return self.cl.get_type() == 'router'

    def is_dhcp(self):
        return self.cl.get_type() == 'dhcpsrvr'

    def has_dns(self):
        return not self.use_extdns()

    def has_metadata(self):
        return any((self.is_vpc(), self.is_router(), self.is_dhcp()))

    def get_domain(self):
        return self.cl.get_domain()

    def use_extdns(self):
        return self.cmdline().idata().get('useextdns', 'false') == 'true'

    def expose_dns(self):
        return self.cmdline().idata().get('exposedns', 'false') == 'true'

    def get_dns(self):
        conf = self.cmdline().idata()
        dns = []
        if not self.use_extdns():
            if not self.is_vpc() and self.cl.is_redundant() and self.cl.get_guest_gw():
                dns.append(self.cl.get_guest_gw())
            else:
                dns.append(self.address().get_guest_ip())

        for name in ('dns1', 'dns2'):
            if name in conf:
                dns.append(conf[name])
        return dns

    def get_format(self):
        return self.__LOG_FORMAT

    def get_ingress_chain(self, device, ip):
        if self.is_vpc():
            return "ACL_INBOUND_%s" % device
        else:
            return "FIREWALL_%s" % ip

    def get_egress_chain(self, device, ip):
        if self.is_vpc():
            return "ACL_OUTBOUND_%s" % device
        else:
            return "FW_EGRESS_RULES"

    def get_egress_table(self):
        if self.is_vpc():
            return 'mangle'
        else:
            return ""
