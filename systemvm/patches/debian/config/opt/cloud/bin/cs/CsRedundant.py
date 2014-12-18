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
# -------------------------------------------------------------------- #
# Notes
# -------------------------------------------------------------------- #
# Vrouter
#
# eth0 router gateway IP for isolated network
# eth1 Control IP for hypervisor
# eth2 public ip(s)
#
# VPC Router
#
# eth0 control interface
# eth1 public ip
# eth2+ Guest networks
# -------------------------------------------------------------------- #
import sys
import os
from pprint import pprint
from CsDatabag import CsDataBag, CsCmdLine
import logging
import CsHelper
from CsFile import CsFile
from CsConfig import CsConfig


class CsRedundant(object):

    CS_RAMDISK_DIR = "/ramdisk"
    CS_ROUTER_DIR = "%s/rrouter" % CS_RAMDISK_DIR
    CS_TEMPLATES = [
        "heartbeat.sh.templ", "check_heartbeat.sh.templ",
        "arping_gateways.sh.templ"
    ]
    CS_TEMPLATES_DIR = "/opt/cloud/templates"
    CONNTRACKD_BIN = "/usr/sbin/conntrackd"
    CONNTRACKD_LOCK = "/var/lock/conntrack.lock"
    CONNTRACKD_CONFIG = "/etc/conntrackd/conntrackd.conf"

    def __init__(self, config):
        self.cl = config.cmdline()
        self.address = config.address()

    def set(self):
        logging.debug("Router redundancy status is %s", self.cl.is_redundant())
        guest = self.address.get_guest_if()
        if self.cl.is_redundant() and guest:
            self._redundant_on()
        else:
            self._redundant_off()

    def _redundant_off(self):
        CsHelper.service("conntrackd", "stop")
        CsHelper.service("keepalived", "stop")
        CsHelper.umount_tmpfs(self.CS_RAMDISK_DIR)
        CsHelper.rmdir(self.CS_RAMDISK_DIR)
        CsHelper.rm("/etc/conntrackd/conntrackd.conf")
        CsHelper.rm("/etc/keepalived/keepalived.conf")

    def _redundant_on(self):
        CsHelper.mkdir(self.CS_RAMDISK_DIR, 0755, False)
        CsHelper.mount_tmpfs(self.CS_RAMDISK_DIR)
        CsHelper.mkdir(self.CS_ROUTER_DIR, 0755, False)
        for s in self.CS_TEMPLATES:
            d = s
            if s.endswith(".templ"):
                d = s.replace(".templ", "")
            CsHelper.copy_if_needed("%s/%s" % (self.CS_TEMPLATES_DIR, s), "%s/%s" % (self.CS_ROUTER_DIR, d))
        CsHelper.copy_if_needed("%s/%s" % (self.CS_TEMPLATES_DIR, "keepalived.conf.templ"), "/etc/keepalived/keepalived.conf")
        CsHelper.copy_if_needed("%s/%s" % (self.CS_TEMPLATES_DIR, "conntrackd.conf.templ"), "/etc/conntrackd/conntrackd.conf")
        CsHelper.copy_if_needed("%s/%s" % (self.CS_TEMPLATES_DIR, "checkrouter.sh.templ"), "/opt/cloud/bin/checkrouter.sh")

        # keepalived configuration
        file = CsFile("/etc/keepalived/keepalived.conf")
        file.search(" router_id ", "    router_id %s" % self.cl.get_name())
        file.search(" priority ", "    priority %s" % self.cl.get_priority())
        file.search(" weight ", "    weight %s" % 2)
        file.greplace("[RROUTER_BIN_PATH]", self.CS_ROUTER_DIR)
        file.section("virtual_ipaddress {", "}", self._collect_ips())
        file.commit()

        # conntrackd configuration
        guest = self.address.get_guest_if()
        connt = CsFile("/etc/conntrackd/conntrackd.conf")
        connt.section("Multicast {", "}", [
                      "IPv4_address 225.0.0.50\n",
                      "Group 3780\n",
                      "IPv4_interface %s\n" % guest.get_ip(),
                      "Interface %s\n" % guest.get_device(),
                      "SndSocketBuffer 1249280\n",
                      "RcvSocketBuffer 1249280\n",
                      "Checksum on\n"])
        connt.section("Address Ignore {", "}", self._collect_ignore_ips())
        connt.commit()
        if connt.is_changed():
            CsHelper.service("conntrackd", "restart")

        if file.is_changed():
            CsHelper.service("keepalived", "restart")

        # FIXME
        # enable/disable_pubip/master/slave etc. will need rewriting to use the new python config

        # Configure heartbeat cron job
        cron = CsFile("/etc/cron.d/heartbeat")
        cron.add("SHELL=/bin/bash", 0)
        cron.add("PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin", 1)
        cron.add("*/1 * * * * root $SHELL %s/check_heartbeat.sh 2>&1 > /dev/null" % self.CS_ROUTER_DIR, -1)
        cron.commit()

    def set_fault(self):
        """ Set fault mode on this router """
        if not self.cl.is_redundant():
            logging.error("Set fault called on non-redundant router")
            return
        logging.info("Router switched to fault mode")
        ads = [o for o in self.address.get_ips() if o.needs_vrrp()]
        for o in ads:
            CsHelper.execute("ifconfig %s down" % o.get_device())
        cmd = "%s -C %s" % (self.CONNTRACKD_BIN, self.CONNTRACKD_CONFIG)
        CsHelper.execute("%s -s" % cmd)
        CsHelper.service("ipsec", "stop")
        CsHelper.service("xl2tpd", "stop")
        CsHelper.service("cloud-passwd-srvr", "stop")
        CsHelper.service("dnsmasq", "stop")
        cl.dbag['config']['redundant_master'] = "false"
        cl.save()

    def set_backup(self):
        """ Set the current router to backup """
        if not self.cl.is_redundant():
            logging.error("Set backup called on non-redundant router")
            return
        """
        if not self.cl.is_master():
            logging.error("Set backup called on node that is already backup")
            return
        """
        logging.info("Router switched to backup mode")
        ads = [o for o in self.address.get_ips() if o.is_public()]
        for o in ads:
            CsHelper.execute("ifconfig %s down" % o.get_device())
        cmd = "%s -C %s" % (self.CONNTRACKD_BIN, self.CONNTRACKD_CONFIG)
        CsHelper.execute("%s -d" % cmd)
        CsHelper.service("ipsec", "stop")
        CsHelper.service("xl2tpd", "stop")
        CsHelper.service("cloud-passwd-srvr", "stop")
        CsHelper.service("dnsmasq", "stop")
        self.cl.dbag['config']['redundant_master'] = "false"
        CsHelper.service("keepalived", "restart")
        self.cl.save()

    def set_master(self):
        """ Set the current router to master """
        if not self.cl.is_redundant():
            logging.error("Set master called on non-redundant router")
            return
        """
        if self.cl.is_master():
            logging.error("Set master called on master node")
            return
        """
        ads = [o for o in self.address.get_ips() if o.is_public()]
        for o in ads:
            # cmd2 = "ip link set %s up" % self.getDevice()
            CsHelper.execute("ifconfig %s down" % o.get_device())
            CsHelper.execute("ifconfig %s up" % o.get_device())
            CsHelper.execute("arping -I %s -A %s -c 1" % (o.get_device(), o.get_ip()))
        # FIXME Need to add in the default routes but I am unsure what the gateway is
        # ip route add default via $gw table Table_$dev proto static
        cmd = "%s -C %s" % (self.CONNTRACKD_BIN, self.CONNTRACKD_CONFIG)
        CsHelper.execute("%s -c" % cmd)
        CsHelper.execute("%s -f" % cmd)
        CsHelper.execute("%s -R" % cmd)
        CsHelper.execute("%s -B" % cmd)
        CsHelper.service("ipsec", "restart")
        CsHelper.service("xl2tpd", "restart")
        CsHelper.service("cloud-passwd-srvr", "restart")
        CsHelper.service("dnsmasq", "restart")
        self.cl.dbag['config']['redundant_master'] = "true"
        self.cl.save()
        CsHelper.service("keepalived", "restart")
        logging.info("Router switched to master mode")

    def _collect_ignore_ips(self):
        """
        This returns a list of ip objects that should be ignored
        by conntrackd
        """
        lines = []
        lines.append("\t\t\tIPv4_address %s\n" % "127.0.0.1")
        lines.append("\t\t\tIPv4_address %s\n" % self.address.get_control_if().get_ip())
        # FIXME - Do we need to also add any internal network gateways?
        return lines

    def _collect_ips(self):
        """
        Construct a list containing all the ips that need to be looked afer by vrrp
        This is based upon the address_needs_vrrp method in CsAddress which looks at
        the network type and decides if it is an internal address or an external one

        In a DomR there will only ever be one address in a VPC there can be many
        The new code also gives the possibility to cloudstack to have a hybrid device
        thet could function as a router and VPC router at the same time
        """
        lines = []
        for o in self.address.get_ips():
            if o.needs_vrrp():
                str = "        %s brd %s dev %s\n" % (o.get_gateway_cidr(), o.get_broadcast(), o.get_device())
                lines.append(str)
        return lines
