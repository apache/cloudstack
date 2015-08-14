#!/usr/bin/python
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
import sys
import os
import base64

from merge import DataBag
from pprint import pprint
import subprocess
import logging
import re
import time
import shutil
import os.path
from fcntl import flock, LOCK_EX, LOCK_UN

from cs.CsDatabag import CsDataBag, CsCmdLine
import cs.CsHelper
from cs.CsNetfilter import CsNetfilters
from cs.CsDhcp import CsDhcp
from cs.CsRedundant import *
from cs.CsFile import CsFile
from cs.CsApp import CsApache, CsDnsmasq
from cs.CsMonitor import CsMonitor
from cs.CsLoadBalancer import CsLoadBalancer
from cs.CsConfig import CsConfig


class CsPassword(CsDataBag):
    """
      Update the password cache

      A stupid step really as we should just rewrite the password server to
      use the databag
    """
    cache = "/var/cache/cloud/passwords"

    def process(self):
        file = CsFile(self.cache)
        for item in self.dbag:
            if item == "id":
                continue
            self.__update(file, item, self.dbag[item])
        file.commit()

    def __update(self, file, ip, password):
        file.search("%s=" % ip, "%s=%s" % (ip, password))


class CsAcl(CsDataBag):
    """
        Deal with Network acls
    """

    class AclIP():
        """ For type Virtual Router """

        def __init__(self, obj, fw):
            self.fw = fw.get_fw()
            self.direction = 'egress'
            if obj['traffic_type'] == 'Ingress':
                self.direction = 'ingress'
            self.device = ''
            self.ip = obj['src_ip']
            self.rule = obj
            self.rule['type'] = obj['protocol']
            # src_port_range
            if 'src_port_range' in obj:
                self.rule['first_port'] = obj['src_port_range'][0]
                self.rule['last_port'] = obj['src_port_range'][1]
            self.rule['allowed'] = True
            self.rule['cidr'] = obj['source_cidr_list']
            self.rule['action'] = "ACCEPT"

        def create(self):
            for cidr in self.rule['cidr']:
                self.add_rule(cidr)

        def add_rule(self, cidr):
            icmp_type = ''
            rule = self.rule
            icmp_type = "any"
            if "icmp_type" in self.rule.keys() and self.rule['icmp_type'] != -1:
                icmp_type = self.rule['icmp_type']
            if "icmp_code" in self.rule.keys() and rule['icmp_code'] != -1:
                icmp_type = "%s/%s" % (self.rule['icmp_type'], self.rule['icmp_code'])
            rnge = ''
            if "first_port" in self.rule.keys() and \
               self.rule['first_port'] == self.rule['last_port']:
                    rnge = self.rule['first_port']
            if "first_port" in self.rule.keys() and \
               self.rule['first_port'] != self.rule['last_port']:
                    rnge = "%s:%s" % (rule['first_port'], rule['last_port'])
            if self.direction == 'ingress':
                if rule['protocol'] == "icmp":
                    self.fw.append(["mangle", "front",
                                    " -A FIREWALL_%s" % self.ip +
                                    " -s %s " % cidr +
                                    " -p %s " % rule['protocol'] +
                                    " -m %s " % rule['protocol'] +
                                    " --icmp-type %s -j %s" % (icmp_type, self.rule['action'])])
                else:
                    self.fw.append(["mangle", "front",
                                    " -A FIREWALL_%s" % self.ip +
                                    " -s %s " % cidr +
                                    " -p %s " % rule['protocol'] +
                                    " -m %s " % rule['protocol'] +
                                    " --dport %s -j RETURN" % rnge])
            if self.direction == 'egress':
                if rule['protocol'] == "icmp":
                    self.fw.append(["filter", "front",
                                    " -A FIREWALL_EGRESS_RULES" +
                                    " -s %s " % cidr +
                                    " -p %s " % rule['protocol'] +
                                    " -m %s " % rule['protocol'] +
                                    " --icmp-type %s -j %s" % (icmp_type, self.rule['action'])])
                else:
                    fwr = " -A FIREWALL_EGRESS_RULES" + \
                          " -s %s " % cidr
                    if rule['protocol'] != "all":
                        fwr += "-p %s " % rule['protocol'] + \
                               " -m %s " % rule['protocol'] + \
                               " --dport %s" % rnge
                    self.fw.append(["filter", "front", "%s -j %s" % (fwr, rule['action'])])

    class AclDevice():
        """ A little class for each list of acls per device """

        FIXED_RULES_INGRESS = 3
        FIXED_RULES_EGRESS = 3

        def __init__(self, obj, config):
            self.ingess = []
            self.egress = []
            self.device = obj['device']
            self.ip = obj['nic_ip']
            self.netmask = obj['nic_netmask']
            self.config = config
            self.cidr = "%s/%s" % (self.ip, self.netmask)
            if "ingress_rules" in obj.keys():
                self.ingress = obj['ingress_rules']
            if "egress_rules" in obj.keys():
                self.egress = obj['egress_rules']
            self.fw = config.get_fw()

        def create(self):
            self.process("ingress", self.ingress, self.FIXED_RULES_INGRESS)
            self.process("egress", self.egress, self.FIXED_RULES_EGRESS)

        def process(self, direction, rule_list, base):
            count = base
            for i in rule_list:
                r = self.AclRule(direction, self, i, self.config, count)
                r.create()
                count += 1

        class AclRule():

            def __init__(self, direction, acl, rule, config, count):
                self.count = count
                if config.is_vpc():
                    self.init_vpc(direction, acl, rule, config)

            def init_vpc(self, direction, acl, rule, config):
                self.table = ""
                self.device = acl.device
                self.direction = direction
                # acl is an object of the AclDevice type. So, its fw attribute is already a list.
                self.fw = acl.fw
                self.chain = config.get_ingress_chain(self.device, acl.ip)
                self.dest = "-s %s" % rule['cidr']
                if direction == "egress":
                    self.table = config.get_egress_table()
                    self.chain = config.get_egress_chain(self.device, acl.ip)
                    self.dest = "-d %s" % rule['cidr']
                self.type = ""
                self.type = rule['type']
                self.icmp_type = "any"
                self.protocol = self.type
                if "icmp_type" in rule.keys() and rule['icmp_type'] != -1:
                    self.icmp_type = rule['icmp_type']
                if "icmp_code" in rule.keys() and rule['icmp_code'] != -1:
                    self.icmp_type = "%s/%s" % (self.icmp_type, rule['icmp_code'])
                if self.type == "protocol":
                    if rule['protocol'] == 41:
                        rule['protocol'] = "ipv6"
                    self.protocol = rule['protocol']
                self.action = "DROP"
                self.dport = ""
                if 'allowed' in rule.keys() and rule['allowed'] and rule['allowed']:
                    self.action = "ACCEPT"
                if 'first_port' in rule.keys():
                    self.dport = "-m %s --dport %s" % (self.protocol, rule['first_port'])
                if 'last_port' in rule.keys() and self.dport and \
                   rule['last_port'] != rule['first_port']:
                    self.dport = "%s:%s" % (self.dport, rule['last_port'])

            def create(self):
                rstr = ""
                rstr = "%s -A %s -p %s %s" % (rstr, self.chain, self.protocol, self.dest)
                if self.type == "icmp":
                    rstr = "%s -m icmp --icmp-type %s" % (rstr, self.icmp_type)
                rstr = "%s %s -j %s" % (rstr, self.dport, self.action)
                rstr = rstr.replace("  ", " ").lstrip()
                self.fw.append([self.table, self.count, rstr])

    def process(self):
        for item in self.dbag:
            if item == "id":
                continue
            if self.config.is_vpc():
                dev_obj = self.AclDevice(self.dbag[item], self.config).create()
            else:
                self.AclIP(self.dbag[item], self.config).create()


class CsVmMetadata(CsDataBag):

    def process(self):
        for ip in self.dbag:
            if ("id" == ip):
                continue
            logging.info("Processing metadata for %s" % ip)
            for item in self.dbag[ip]:
                folder = item[0]
                file = item[1]
                data = item[2]

                # process only valid data
                if folder != "userdata" and folder != "metadata":
                    continue

                if file == "":
                    continue

                self.__htaccess(ip, folder, file)

                if data == "":
                    self.__deletefile(ip, folder, file)
                else:
                    self.__createfile(ip, folder, file, data)

    def __deletefile(self, ip, folder, file):
        datafile = "/var/www/html/" + folder + "/" + ip + "/" + file

        if os.path.exists(datafile):
            os.remove(datafile)

    def __createfile(self, ip, folder, file, data):
        dest = "/var/www/html/" + folder + "/" + ip + "/" + file
        metamanifestdir = "/var/www/html/" + folder + "/" + ip
        metamanifest = metamanifestdir + "/meta-data"

        # base64 decode userdata
        if folder == "userdata" or folder == "user-data":
            if data is not None:
                data = base64.b64decode(data)

        fh = open(dest, "w")
        self.__exflock(fh)
        if data is not None:
            fh.write(data)
        else:
            fh.write("")
        self.__unflock(fh)
        fh.close()
        os.chmod(dest, 0644)

        if folder == "metadata" or folder == "meta-data":
            try:
                os.makedirs(metamanifestdir, 0755)
            except OSError as e:
                # error 17 is already exists, we do it this way for concurrency
                if e.errno != 17:
                    print "failed to make directories " + metamanifestdir + " due to :" + e.strerror
                    sys.exit(1)
            if os.path.exists(metamanifest):
                fh = open(metamanifest, "r+a")
                self.__exflock(fh)
                if file not in fh.read():
                    fh.write(file + '\n')
                self.__unflock(fh)
                fh.close()
            else:
                fh = open(metamanifest, "w")
                self.__exflock(fh)
                fh.write(file + '\n')
                self.__unflock(fh)
                fh.close()

        if os.path.exists(metamanifest):
            os.chmod(metamanifest, 0644)

    def __htaccess(self, ip, folder, file):
        entry = "RewriteRule ^" + file + "$  ../" + folder + "/%{REMOTE_ADDR}/" + file + " [L,NC,QSA]"
        htaccessFolder = "/var/www/html/latest"
        htaccessFile = htaccessFolder + "/.htaccess"

        CsHelper.mkdir(htaccessFolder, 0755, True)

        if os.path.exists(htaccessFile):
            fh = open(htaccessFile, "r+a")
            self.__exflock(fh)
            if entry not in fh.read():
                fh.write(entry + '\n')
            self.__unflock(fh)
            fh.close()
        else:
            fh = open(htaccessFile, "w")
            self.__exflock(fh)
            fh.write("Options +FollowSymLinks\nRewriteEngine On\n\n")
            fh.write(entry + '\n')
            self.__unflock(fh)
            fh.close()

        entry = "Options -Indexes\nOrder Deny,Allow\nDeny from all\nAllow from " + ip
        htaccessFolder = "/var/www/html/" + folder + "/" + ip
        htaccessFile = htaccessFolder+"/.htaccess"

        try:
            os.makedirs(htaccessFolder, 0755)
        except OSError as e:
            # error 17 is already exists, we do it this way for sake of concurrency
            if e.errno != 17:
                print "failed to make directories " + htaccessFolder + " due to :" + e.strerror
                sys.exit(1)

        fh = open(htaccessFile, "w")
        self.__exflock(fh)
        fh.write(entry + '\n')
        self.__unflock(fh)
        fh.close()

        if folder == "metadata" or folder == "meta-data":
            entry = "RewriteRule ^meta-data/(.+)$  ../" + folder + "/%{REMOTE_ADDR}/$1 [L,NC,QSA]"
            htaccessFolder = "/var/www/html/latest"
            htaccessFile = htaccessFolder + "/.htaccess"

            fh = open(htaccessFile, "r+a")
            self.__exflock(fh)
            if entry not in fh.read():
                fh.write(entry + '\n')

            entry = "RewriteRule ^meta-data/$  ../" + folder + "/%{REMOTE_ADDR}/meta-data [L,NC,QSA]"

            fh.seek(0)
            if entry not in fh.read():
                fh.write(entry + '\n')
            self.__unflock(fh)
            fh.close()

    def __exflock(self, file):
        try:
            flock(file, LOCK_EX)
        except IOError as e:
            print "failed to lock file" + file.name + " due to : " + e.strerror
            sys.exit(1)  # FIXME
        return True

    def __unflock(self, file):
        try:
            flock(file, LOCK_UN)
        except IOError:
            print "failed to unlock file" + file.name + " due to : " + e.strerror
            sys.exit(1)  # FIXME
        return True


class CsSite2SiteVpn(CsDataBag):
    """
    Setup any configured vpns (using swan)
    left is the local machine
    right is where the clients connect from
    """

    VPNCONFDIR = "/etc/ipsec.d"

    def process(self):
        self.confips = []
        # collect a list of configured vpns
        for file in os.listdir(self.VPNCONFDIR):
            m = re.search("^ipsec.vpn-(.*).conf", file)
            if m:
                self.confips.append(m.group(1))

        for public_ip in self.dbag:
            if public_ip == "id":
                continue
            dev = CsHelper.get_device(public_ip)
            if dev == "":
                logging.error("Request for ipsec to %s not possible because ip is not configured", public_ip)
                continue
            CsHelper.start_if_stopped("ipsec")
            self.configure_iptables(dev, self.dbag[public_ip])
            self.configure_ipsec(self.dbag[public_ip])

        # Delete vpns that are no longer in the configuration
        for ip in self.confips:
            self.deletevpn(ip)

    def deletevpn(self, ip):
        logging.info("Removing VPN configuration for %s", ip)
        CsHelper.execute("ipsec auto --down vpn-%s" % ip)
        CsHelper.execute("ipsec auto --delete vpn-%s" % ip)
        vpnconffile = "%s/ipsec.vpn-%s.conf" % (self.VPNCONFDIR, ip)
        vpnsecretsfile = "%s/ipsec.vpn-%s.secrets" % (self.VPNCONFDIR, ip)
        os.remove(vpnconffile)
        os.remove(vpnsecretsfile)
        CsHelper.execute("ipsec auto --rereadall")

    def configure_iptables(self, dev, obj):
        self.fw.append(["", "front", "-A INPUT -i %s -p udp -m udp --dport 500 -s %s -d %s -j ACCEPT" % (dev, obj['peer_gateway_ip'], obj['local_public_ip'])])
        self.fw.append(["", "front", "-A INPUT -i %s -p udp -m udp --dport 4500 -s %s -d %s -j ACCEPT" % (dev, obj['peer_gateway_ip'], obj['local_public_ip'])])
        self.fw.append(["", "front", "-A INPUT -i %s -p esp -s %s -d %s -j ACCEPT" % (dev, obj['peer_gateway_ip'], obj['local_public_ip'])])
        self.fw.append(["nat", "front", "-A POSTROUTING -t nat -o %s -m mark --mark 0x525 -j ACCEPT" % dev])
        for net in obj['peer_guest_cidr_list'].lstrip().rstrip().split(','):
            self.fw.append(["mangle", "front",
                            "-A FORWARD -s %s -d %s -j MARK --set-xmark 0x525/0xffffffff" % (obj['local_guest_cidr'], net)])
            self.fw.append(["mangle", "",
                            "-A OUTPUT -s %s -d %s -j MARK --set-xmark 0x525/0xffffffff" % (obj['local_guest_cidr'], net)])
            self.fw.append(["mangle", "front",
                            "-A FORWARD -s %s -d %s -j MARK --set-xmark 0x524/0xffffffff" % (net, obj['local_guest_cidr'])])
            self.fw.append(["mangle", "",
                            "-A INPUT -s %s -d %s -j MARK --set-xmark 0x524/0xffffffff" % (net, obj['local_guest_cidr'])])

    def configure_ipsec(self, obj):
        leftpeer = obj['local_public_ip']
        rightpeer = obj['peer_gateway_ip']
        peerlist = obj['peer_guest_cidr_list'].lstrip().rstrip().replace(',', ' ')
        vpnconffile = "%s/ipsec.vpn-%s.conf" % (self.VPNCONFDIR, rightpeer)
        vpnsecretsfile = "%s/ipsec.vpn-%s.secrets" % (self.VPNCONFDIR, rightpeer)
        if rightpeer in self.confips:
            self.confips.remove(rightpeer)
        file = CsFile(vpnconffile)
        file.search("conn ", "conn vpn-%s" % rightpeer)
        file.addeq(" left=%s" % leftpeer)
        file.addeq(" leftsubnet=%s" % obj['local_guest_cidr'])
        file.addeq(" leftnexthop=%s" % obj['local_public_gateway'])
        file.addeq(" right=%s" % rightpeer)
        file.addeq(" rightsubnets={%s}" % peerlist)
        file.addeq(" type=tunnel")
        file.addeq(" authby=secret")
        file.addeq(" keyexchange=ike")
        file.addeq(" ike=%s" % obj['ike_policy'])
        file.addeq(" ikelifetime=%s" % self.convert_sec_to_h(obj['ike_lifetime']))
        file.addeq(" esp=%s" % obj['esp_policy'])
        file.addeq(" salifetime=%s" % self.convert_sec_to_h(obj['esp_lifetime']))
        file.addeq(" pfs=%s" % CsHelper.bool_to_yn(obj['dpd']))
        file.addeq(" keyingtries=2")
        file.addeq(" auto=start")
        if obj['dpd']:
            file.addeq("  dpddelay=30")
            file.addeq("  dpdtimeout=120")
            file.addeq("  dpdaction=restart")
        file.commit()
        secret = CsFile(vpnsecretsfile)
        secret.search("%s " % leftpeer, "%s %s: PSK \"%s\"" % (leftpeer, rightpeer, obj['ipsec_psk']))
        secret.commit()
        if secret.is_changed() or file.is_changed():
            logging.info("Configured vpn %s %s", leftpeer, rightpeer)
            CsHelper.execute("ipsec auto --rereadall")
            CsHelper.execute("ipsec --add vpn-%s" % rightpeer)
            if not obj['passive']:
                CsHelper.execute("ipsec --up vpn-%s" % rightpeer)
        os.chmod(vpnsecretsfile, 0o400)

    def convert_sec_to_h(self, val):
        hrs = int(val) / 3600
        return "%sh" % hrs


class CsForwardingRules(CsDataBag):

    def process(self):
        for public_ip in self.dbag:
            if public_ip == "id":
                continue
            for rule in self.dbag[public_ip]:
                if rule["type"] == "forward":
                    self.processForwardRule(rule)
                elif rule["type"] == "staticnat":
                    self.processStaticNatRule(rule)

    def getDeviceByIp(self, ipa):
        for ip in self.config.address().get_ips():
            if ip.ip_in_subnet(ipa):
                return ip.get_device()
        return None

    def getNetworkByIp(self, ipa):
        for ip in self.config.address().get_ips():
            if ip.ip_in_subnet(ipa):
                return ip.get_network()
        return None

    def getGatewayByIp(self, ipa):
        for ip in self.config.address().get_ips():
            if ip.ip_in_subnet(ipa):
                return ip.get_gateway()
        return None

    def portsToString(self, ports, delimiter):
        ports_parts = ports.split(":", 2)
        if ports_parts[0] == ports_parts[1]:
            return str(ports_parts[0])
        else:
            return "%s%s%s" % (ports_parts[0], delimiter, ports_parts[1])

    def processForwardRule(self, rule):
        if self.config.is_vpc():
            self.forward_vpc(rule)
        else:
            self.forward_vr(rule)

    def forward_vr(self, rule):
        fw1 = "-A PREROUTING -d %s/32 -i %s -p %s -m %s --dport %s -j DNAT --to-destination %s:%s" % \
              (
                rule['public_ip'],
                self.getDeviceByIp(rule['public_ip']),
                rule['protocol'],
                rule['protocol'],
                self.portsToString(rule['public_ports'], ':'),
                rule['internal_ip'],
                self.portsToString(rule['internal_ports'], '-')
              )
        fw2 = "-A PREROUTING -d %s/32 -i %s -p %s -m %s --dport %s -j DNAT --to-destination %s:%s" % \
              (
                rule['public_ip'],
                self.getDeviceByIp(rule['internal_ip']),
                rule['protocol'],
                rule['protocol'],
                self.portsToString(rule['public_ports'], ':'),
                rule['internal_ip'],
                self.portsToString(rule['internal_ports'], '-')
              )
        fw3 = "-A OUTPUT -d %s/32 -p %s -m %s --dport %s -j DNAT --to-destination %s:%s" % \
              (
                rule['public_ip'],
                rule['protocol'],
                rule['protocol'],
                self.portsToString(rule['public_ports'], ':'),
                rule['internal_ip'],
                self.portsToString(rule['internal_ports'], '-')
              )
        fw4 = "-j SNAT --to-source %s -A POSTROUTING -s %s -d %s/32 -o %s -p %s -m %s --dport %s" % \
              (
                self.getGatewayByIp(rule['internal_ip']),
                self.getNetworkByIp(rule['internal_ip']),
                rule['internal_ip'],
                self.getDeviceByIp(rule['internal_ip']),
                rule['protocol'],
                rule['protocol'],
                self.portsToString(rule['internal_ports'], ':')
              )
        fw5 = "-A PREROUTING -d %s/32 -i %s -p %s -m %s --dport %s -j MARK --set-xmark %s/0xffffffff" % \
              (
                rule['public_ip'],
                self.getDeviceByIp(rule['public_ip']),
                rule['protocol'],
                rule['protocol'],
                self.portsToString(rule['public_ports'], ':'),
                hex(int(self.getDeviceByIp(rule['public_ip'])[3:]))
              )
        fw6 = "-A PREROUTING -d %s/32 -i %s -p %s -m %s --dport %s -m state --state NEW -j CONNMARK --save-mark --nfmask 0xffffffff --ctmask 0xffffffff" % \
              (
                rule['public_ip'],
                self.getDeviceByIp(rule['public_ip']),
                rule['protocol'],
                rule['protocol'],
                self.portsToString(rule['public_ports'], ':'),
              )
        fw7 = "-A FORWARD -i %s -o %s -p %s -m %s --dport %s -m state --state NEW -j ACCEPT" % \
              (
                self.getDeviceByIp(rule['public_ip']),
                self.getDeviceByIp(rule['internal_ip']),
                rule['protocol'],
                rule['protocol'],
                self.portsToString(rule['internal_ports'], ':')
              )
        self.fw.append(["nat", "", fw1])
        self.fw.append(["nat", "", fw2])
        self.fw.append(["nat", "", fw3])
        self.fw.append(["nat", "", fw4])
        self.fw.append(["nat", "", fw5])
        self.fw.append(["nat", "", fw6])
        self.fw.append(["", "", fw7])

    def forward_vpc(self, rule):
        fw_prerout_rule = "-A PREROUTING -d %s/32 -i %s" % (rule["public_ip"], self.getDeviceByIp(rule['public_ip']))
        if not rule["protocol"] == "any":
            fw_prerout_rule += " -m %s -p %s" % (rule["protocol"], rule["protocol"])
        if not rule["public_ports"] == "any":
            fw_prerout_rule += " --dport %s" % self.portsToString(rule["public_ports"], ":")
        fw_prerout_rule += " -j DNAT --to-destination %s" % rule["internal_ip"]
        if not rule["internal_ports"] == "any":
            fw_prerout_rule += ":" + self.portsToString(rule["internal_ports"], "-")

        fw_postrout_rule = "-A POSTROUTING -d %s/32 " % rule["public_ip"]
        if not rule["protocol"] == "any":
            fw_postrout_rule += " -m %s -p %s" % (rule["protocol"], rule["protocol"])
        if not rule["public_ports"] == "any":
            fw_postrout_rule += " --dport %s" % self.portsToString(rule["public_ports"], ":")
        fw_postrout_rule += " -j SNAT --to-source %s" % rule["internal_ip"]
        if not rule["internal_ports"] == "any":
            fw_postrout_rule += ":" + self.portsToString(rule["internal_ports"], "-")

        fw_output_rule = "-A OUTPUT -d %s/32" % rule["public_ip"]
        if not rule["protocol"] == "any":
            fw_output_rule += " -m %s -p %s" % (rule["protocol"], rule["protocol"])
        if not rule["public_ports"] == "any":
            fw_output_rule += " --dport %s" % self.portsToString(rule["public_ports"], ":")
        fw_output_rule += " -j DNAT --to-destination %s" % rule["internal_ip"]
        if not rule["internal_ports"] == "any":
            fw_output_rule += ":" + self.portsToString(rule["internal_ports"], "-")

        self.fw.append(["nat", "", fw_prerout_rule])
        self.fw.append(["nat", "", fw_postrout_rule])
        self.fw.append(["nat", "", fw_output_rule])

    def processStaticNatRule(self, rule):
        # FIXME this needs ordering with the VPN no nat rule
        device = self.getDeviceByIp(rule["public_ip"])
        if device is None:
            raise Exception("Ip address %s has no device in the ips databag" % rule["public_ip"])
        self.fw.append(["nat", "front",
                        "-A PREROUTING -d %s/32 -j DNAT --to-destination %s" % (rule["public_ip"], rule["internal_ip"])])
        self.fw.append(["nat", "front",
                        "-A POSTROUTING -o %s -s %s/32 -j SNAT --to-source %s" % (device, rule["internal_ip"], rule["public_ip"])])
        self.fw.append(["nat", "front",
                        "-A OUTPUT -d %s/32 -j DNAT --to-destination %s" % (rule["public_ip"], rule["internal_ip"])])


def main(argv):
    config = CsConfig()
    logging.basicConfig(filename=config.get_logger(),
                        level=config.get_level(),
                        format=config.get_format())
    config.set_address()

    # IP configuration
    config.address().compare()
    config.address().process()

    password = CsPassword("vmpassword", config)
    password.process()

    metadata = CsVmMetadata('vmdata', config)
    metadata.process()

    acls = CsAcl('networkacl', config)
    acls.process()

    acls = CsAcl('firewallrules', config)
    acls.process()

    fwd = CsForwardingRules("forwardingrules", config)
    fwd.process()

    nf = CsNetfilters()
    nf.compare(config.get_fw())

    red = CsRedundant(config)
    red.set()

    vpns = CsSite2SiteVpn("site2sitevpn", config)
    vpns.process()

    dhcp = CsDhcp("dhcpentry", config)
    dhcp.process()

    lb = CsLoadBalancer("loadbalancer", config)
    lb.process()

    mon = CsMonitor("monitorservice", config)
    mon.process()

    nf = CsNetfilters()
    nf.compare(config.get_fw())

    # Save iptables configuration - will be loaded on reboot by the iptables-restore that is configured on /etc/rc.local
    CsHelper.save_iptables("iptables-save", "/etc/iptables/router_rules.v4")
    CsHelper.save_iptables("ip6tables-save", "/etc/iptables/router_rules.v6")

if __name__ == "__main__":
    main(sys.argv)
