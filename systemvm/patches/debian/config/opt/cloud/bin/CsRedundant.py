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
from cs_databag import CsDataBag, CsCmdLine
import logging
import CsHelper
from CsFile import CsFile

class CsRedundant(object):

    CS_RAMDISK_DIR = "/ramdisk"
    CS_ROUTER_DIR  = "%s/rrouter" % CS_RAMDISK_DIR
    CS_TEMPLATES = [
            "enable_pubip.sh.templ",
            "master.sh.templ", "backup.sh.templ", "fault.sh.templ",
            "primary-backup.sh.templ", "heartbeat.sh.templ", "check_heartbeat.sh.templ",
            "arping_gateways.sh.templ", "check_bumpup.sh", "disable_pubip.sh",
            "services.sh", 
            ]
    CS_TEMPLATES_DIR = "/opt/cloud/templates"

    def __init__(self):
        pass

    def set(self, cl, address):
        logging.debug("Router redundancy status is %s", cl.is_redundant())
        self.cl = cl
        self.address = address
        if cl.is_redundant():
            self._redundant_on()
        else:
            self._redundant_off()

    def _redundant_off(self):
        CsHelper.umount_tmpfs(self.CS_RAMDISK_DIR)
        CsHelper.rmdir(self.CS_RAMDISK_DIR)
        CsHelper.rm("/etc/cron.d/heartbeat")

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
        file.search(" priority ", "    priority %s" % 20)
        file.search(" weight ", "    weight %s" % 2)
        file.greplace("[RROUTER_BIN_PATH]", self.CS_ROUTER_DIR)
        file.section("virtual_ipaddress {", "}", self._collect_ips())
        file.commit()

        # conntrackd configuration
        control = self.address.get_control_if()
        connt = CsFile("/etc/conntrackd/conntrackd.conf")
        connt.search("[\s\t]IPv4_interface ", "\t\tIPv4_interface %s" % control.get_ip())
        connt.search("[\s\t]Interface ", "\t\tInterface %s" % control.get_device())
        connt.section("Address Ignore {", "}", self._collect_ignore_ips())
        connt.commit()

        # FIXME
        # enable/disable_pubip/master/slave etc. will need rewriting to use the new python config

        # Configure heartbeat cron job
        cron = CsFile("/etc/cron.d/heartbeat")
        cron.add("SHELL=/bin/bash", 0)
        cron.add("PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin", 1)
        cron.add("*/1 * * * * root $SHELL %s/check_heartbeat.sh 2>&1 > /dev/null" %  self.CS_ROUTER_DIR, -1)
        cron.commit()

    def _collect_ignore_ips(self):
        """
        This returns a list of ip objects that should be ignored
        by conntrackd
        """
        lines = []
        lines.append("\t\t\tIPv4_address %s\n" % "127.0.0.1")
        lines.append("\t\t\tIPv4_address %s\n" %  self.address.get_control_if().get_ip())
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
                str = "        %s brd %s dev %s\n" % (o.get_cidr(), o.get_broadcast(), o.get_ip())
                lines.append(str)
        return lines
