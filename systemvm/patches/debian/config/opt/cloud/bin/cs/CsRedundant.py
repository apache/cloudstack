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
import os
import logging
import CsHelper
from CsFile import CsFile
from CsProcess import CsProcess
from CsApp import CsPasswdSvc
from CsAddress import CsDevice
import socket
from time import sleep


class CsRedundant(object):

    CS_RAMDISK_DIR = "/ramdisk"
    CS_PRIO_UP = 1
    CS_PRIO_DOWN = -1
    CS_ROUTER_DIR = "%s/rrouter" % CS_RAMDISK_DIR
    CS_TEMPLATES = [
        "heartbeat.sh.templ", "check_heartbeat.sh.templ",
        "arping_gateways.sh.templ"
    ]
    CS_TEMPLATES_DIR = "/opt/cloud/templates"
    CONNTRACKD_BIN = "/usr/sbin/conntrackd"
    CONNTRACKD_KEEPALIVED_CONFLOCK = "/var/lock/conntrack.lock"
    CONNTRACKD_CONF = "/etc/conntrackd/conntrackd.conf"
    RROUTER_LOG = "/var/log/cloud.log"
    KEEPALIVED_CONF = "/etc/keepalived/keepalived.conf"

    def __init__(self, config):
        self.cl = config.cmdline()
        self.address = config.address()
        self.config = config

    def set(self):
        logging.debug("Router redundancy status is %s", self.cl.is_redundant())
        if self.cl.is_redundant():
            self._redundant_on()
        else:
            self._redundant_off()

    def _redundant_off(self):
        CsHelper.service("conntrackd", "stop")
        CsHelper.service("keepalived", "stop")
        CsHelper.umount_tmpfs(self.CS_RAMDISK_DIR)
        CsHelper.rmdir(self.CS_RAMDISK_DIR)
        CsHelper.rm(self.CONNTRACKD_CONF)
        CsHelper.rm(self.KEEPALIVED_CONF)

    def _redundant_on(self):
        guest = self.address.get_guest_if()
        # No redundancy if there is no guest network
        if self.cl.is_master() or guest is None:
            for obj in [o for o in self.address.get_ips() if o.is_public()]:
                self.check_is_up(obj.get_device())
        if guest is None:
            self._redundant_off()
            return
        CsHelper.mkdir(self.CS_RAMDISK_DIR, 0755, False)
        CsHelper.mount_tmpfs(self.CS_RAMDISK_DIR)
        CsHelper.mkdir(self.CS_ROUTER_DIR, 0755, False)
        for s in self.CS_TEMPLATES:
            d = s
            if s.endswith(".templ"):
                d = s.replace(".templ", "")
            CsHelper.copy_if_needed(
                "%s/%s" % (self.CS_TEMPLATES_DIR, s), "%s/%s" % (self.CS_ROUTER_DIR, d))
        CsHelper.copy_if_needed(
            "%s/%s" % (self.CS_TEMPLATES_DIR, "keepalived.conf.templ"), self.KEEPALIVED_CONF)
        CsHelper.copy_if_needed(
            "%s/%s" % (self.CS_TEMPLATES_DIR, "conntrackd.conf.templ"), self.CONNTRACKD_CONF)
        CsHelper.copy_if_needed(
            "%s/%s" % (self.CS_TEMPLATES_DIR, "checkrouter.sh.templ"), "/opt/cloud/bin/checkrouter.sh")

        CsHelper.execute(
            'sed -i "s/--exec\ \$DAEMON;/--exec\ \$DAEMON\ --\ --vrrp;/g" /etc/init.d/keepalived')
        # checkrouter.sh configuration
        check_router = CsFile("/opt/cloud/bin/checkrouter.sh")
        check_router.greplace("[RROUTER_LOG]", self.RROUTER_LOG)
        check_router.commit()

        # keepalived configuration
        keepalived_conf = CsFile(self.KEEPALIVED_CONF)
        keepalived_conf.search(
            " router_id ", "    router_id %s" % self.cl.get_name())
        keepalived_conf.search(
            " interface ", "    interface %s" % guest.get_device())
        keepalived_conf.search(
            " virtual_router_id ", "    virtual_router_id %s" % self.cl.get_router_id())
        keepalived_conf.greplace("[RROUTER_BIN_PATH]", self.CS_ROUTER_DIR)
        keepalived_conf.section("authentication {", "}", [
                                "        auth_type AH \n", "        auth_pass %s\n" % self.cl.get_router_password()])
        keepalived_conf.section(
            "virtual_ipaddress {", "}", self._collect_ips())
        keepalived_conf.commit()

        # conntrackd configuration
        connt = CsFile(self.CONNTRACKD_CONF)
        if guest is not None:
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

        # Configure heartbeat cron job - runs every 30 seconds
        heartbeat_cron = CsFile("/etc/cron.d/heartbeat")
        heartbeat_cron.add("SHELL=/bin/bash", 0)
        heartbeat_cron.add(
            "PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin", 1)
        heartbeat_cron.add(
            "* * * * * root $SHELL %s/check_heartbeat.sh 2>&1 > /dev/null" % self.CS_ROUTER_DIR, -1)
        heartbeat_cron.add(
            "* * * * * root sleep 30; $SHELL %s/check_heartbeat.sh 2>&1 > /dev/null" % self.CS_ROUTER_DIR, -1)
        heartbeat_cron.commit()

        # Configure KeepaliveD cron job - runs at every reboot
        keepalived_cron = CsFile("/etc/cron.d/keepalived")
        keepalived_cron.add("SHELL=/bin/bash", 0)
        keepalived_cron.add(
            "PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin", 1)
        keepalived_cron.add("@reboot root service keepalived start", -1)
        keepalived_cron.commit()

        # Configure ConntrackD cron job - runs at every reboot
        conntrackd_cron = CsFile("/etc/cron.d/conntrackd")
        conntrackd_cron.add("SHELL=/bin/bash", 0)
        conntrackd_cron.add(
            "PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin", 1)
        conntrackd_cron.add("@reboot root service conntrackd start", -1)
        conntrackd_cron.commit()

        proc = CsProcess(['/usr/sbin/keepalived', '--vrrp'])
        if not proc.find() or keepalived_conf.is_changed():
            CsHelper.service("keepalived", "restart")

    def release_lock(self):
        try:
            os.remove("/tmp/master_lock")
        except OSError:
            pass

    def set_lock(self):
        """
        Make sure that master state changes happen sequentially
        """
        iterations = 10
        time_between = 1

        for iter in range(0, iterations):
            try:
                s = socket.socket(socket.AF_UNIX, socket.SOCK_STREAM)
                s.bind('/tmp/master_lock')
                return s
            except socket.error, e:
                error_code = e.args[0]
                error_string = e.args[1]
                print "Process already running (%d:%s). Exiting" % (error_code, error_string)
                logging.info("Master is already running, waiting")
                sleep(time_between)

    def set_fault(self):
        """ Set fault mode on this router """
        if not self.cl.is_redundant():
            logging.error("Set fault called on non-redundant router")
            return

        self.set_lock()
        logging.info("Router switched to fault mode")
        ads = [o for o in self.address.get_ips() if o.is_public()]
        for o in ads:
            CsHelper.execute("ifconfig %s down" % o.get_device())
        cmd = "%s -C %s" % (self.CONNTRACKD_BIN, self.CONNTRACKD_CONF)
        CsHelper.execute("%s -s" % cmd)
        CsHelper.service("ipsec", "stop")
        CsHelper.service("xl2tpd", "stop")
        CsHelper.service("dnsmasq", "stop")
        ads = [o for o in self.address.get_ips() if o.needs_vrrp()]
        for o in ads:
            CsPasswdSvc(o.get_gateway()).stop()
        self.cl.set_fault_state()
        self.cl.save()
        self.release_lock()
        logging.info("Router switched to fault mode")

    def set_backup(self):
        """ Set the current router to backup """
        if not self.cl.is_redundant():
            logging.error("Set backup called on non-redundant router")
            return

        self.set_lock()
        logging.debug("Setting router to backup")
        ads = [o for o in self.address.get_ips() if o.is_public()]
        dev = ''
        for o in ads:
            if dev == o.get_device():
                continue
            logging.info("Bringing public interface %s down" % o.get_device())
            cmd2 = "ip link set %s up" % o.get_device()
            CsHelper.execute(cmd2)
            dev = o.get_device()
        cmd = "%s -C %s" % (self.CONNTRACKD_BIN, self.CONNTRACKD_CONF)
        CsHelper.execute("%s -d" % cmd)
        CsHelper.service("ipsec", "stop")
        CsHelper.service("xl2tpd", "stop")
        ads = [o for o in self.address.get_ips() if o.needs_vrrp()]
        for o in ads:
            CsPasswdSvc(o.get_gateway()).stop()
        CsHelper.service("dnsmasq", "stop")

        self.cl.set_master_state(False)
        self.cl.save()
        self.release_lock()
        logging.info("Router switched to backup mode")

    def set_master(self):
        """ Set the current router to master """
        if not self.cl.is_redundant():
            logging.error("Set master called on non-redundant router")
            return

        self.set_lock()
        logging.debug("Setting router to master")
        ads = [o for o in self.address.get_ips() if o.is_public()]
        dev = ''
        for o in ads:
            if dev == o.get_device():
                continue
            cmd2 = "ip link set %s up" % o.get_device()
            if CsDevice(o.get_device(), self.config).waitfordevice():
                CsHelper.execute(cmd2)
                dev = o.get_device()
                logging.info("Bringing public interface %s up" %
                             o.get_device())
            else:
                logging.error(
                    "Device %s was not ready could not bring it up" % o.get_device())
        # ip route add default via $gw table Table_$dev proto static
        cmd = "%s -C %s" % (self.CONNTRACKD_BIN, self.CONNTRACKD_CONF)
        CsHelper.execute("%s -c" % cmd)
        CsHelper.execute("%s -f" % cmd)
        CsHelper.execute("%s -R" % cmd)
        CsHelper.execute("%s -B" % cmd)
        CsHelper.service("ipsec", "restart")
        CsHelper.service("xl2tpd", "restart")
        ads = [o for o in self.address.get_ips() if o.needs_vrrp()]
        for o in ads:
            CsPasswdSvc(o.get_gateway()).restart()
        CsHelper.service("dnsmasq", "restart")
        self.cl.set_master_state(True)
        self.cl.save()
        self.release_lock()
        logging.info("Router switched to master mode")

    def _collect_ignore_ips(self):
        """
        This returns a list of ip objects that should be ignored
        by conntrackd
        """
        lines = []
        lines.append("\t\t\tIPv4_address %s\n" % "127.0.0.1")
        lines.append("\t\t\tIPv4_address %s\n" %
                     self.address.get_control_if().get_ip())
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
                str = "        %s brd %s dev %s\n" % (
                    o.get_gateway_cidr(), o.get_broadcast(), o.get_device())
                lines.append(str)
                self.check_is_up(o.get_device())
        return lines

    def check_is_up(self, device):
        """ Ensure device is up """
        cmd = "ip link show %s | grep 'state DOWN'" % device
        for i in CsHelper.execute(cmd):
            if " DOWN " in i:
                cmd2 = "ip link set %s up" % device
                CsHelper.execute(cmd2)
