#!/usr/bin/python3
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
import base64
import logging
import os
import re
import sys
from urllib.request import *
from urllib.parse import *
from urllib.error import *
import time

from collections import OrderedDict
from fcntl import flock, LOCK_EX, LOCK_UN

from cs.CsDatabag import CsDataBag
from cs.CsNetfilter import CsNetfilters
from cs.CsDhcp import CsDhcp
from cs.CsRedundant import *
from cs.CsFile import CsFile
from cs.CsMonitor import CsMonitor
from cs.CsLoadBalancer import CsLoadBalancer
from cs.CsConfig import CsConfig
from cs.CsProcess import CsProcess
from cs.CsStaticRoutes import CsStaticRoutes


class CsPassword(CsDataBag):

    TOKEN_FILE = "/tmp/passwdsrvrtoken"

    def process(self):
        for item in self.dbag:
            if item == "id":
                continue
            self.__update(item, self.dbag[item])

    def __update(self, vm_ip, password):
        token = ""
        try:
            tokenFile = open(self.TOKEN_FILE)
            token = tokenFile.read()
        except IOError:
            logging.debug("File %s does not exist" % self.TOKEN_FILE)

        server_ip = None
        guest_ip = None
        for interface in self.config.address().get_interfaces():
            if interface.ip_in_subnet(vm_ip) and interface.is_added():
                if self.config.cl.is_redundant():
                    server_ip = interface.get_gateway()
                    guest_ip = interface.get_ip()
                else:
                    server_ip = interface.get_ip()
                break

        if server_ip is not None:
            if guest_ip is None:
                proc = CsProcess(['/opt/cloud/bin/passwd_server_ip.py', server_ip])
            else:
                proc = CsProcess(['/opt/cloud/bin/passwd_server_ip.py', server_ip + "," + guest_ip])
            if proc.find():
                url = "http://%s:8080/" % server_ip
                payload = {"ip": vm_ip, "password": password, "token": token}
                data = urllib.parse.urlencode(payload)
                request = urllib.request.Request(url, data=data, headers={"DomU_Request": "save_password"})
                try:
                    resp = urlopen(request, data)
                    logging.debug("Update password server result: http:%s, content:%s" % (resp.code, resp.read()))
                except Exception as e:
                    logging.error("Failed to update password server due to: %s" % e)


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
            self.rule['action'] = "ACCEPT"

            if self.rule['type'] == 'all' and obj['source_cidr_list']:
                self.rule['cidr'] = []
            else:
                self.rule['cidr'] = obj['source_cidr_list']

            if self.direction == 'egress':
                try:
                    if not obj['dest_cidr_list']:
                        self.rule['dcidr'] = []
                    else:
                        self.rule['dcidr'] = obj['dest_cidr_list']
                except Exception:
                    self.rule['dcidr'] = []

            logging.debug("AclIP created for rule ==> %s", self.rule)

        def create(self):
            self.add_rule()

        def add_rule(self):
            CIDR_ALL = '0.0.0.0/0'
            icmp_type = ''
            rule = self.rule
            icmp_type = "any"
            if "icmp_type" in list(self.rule.keys()) and self.rule['icmp_type'] != -1:
                icmp_type = self.rule['icmp_type']
            if "icmp_code" in list(self.rule.keys()) and rule['icmp_code'] != -1:
                icmp_type = "%s/%s" % (self.rule['icmp_type'], self.rule['icmp_code'])
            rnge = ''
            if "first_port" in list(self.rule.keys()) and \
               self.rule['first_port'] == self.rule['last_port']:
                rnge = " --dport %s " % self.rule['first_port']
            if "first_port" in list(self.rule.keys()) and \
               self.rule['first_port'] != self.rule['last_port']:
                rnge = " --dport %s:%s" % (rule['first_port'], rule['last_port'])

            logging.debug("Current ACL IP direction is ==> %s", self.direction)

            if self.direction == 'ingress':
                for cidr in self.rule['cidr']:
                    if rule['protocol'] == "icmp":
                        self.fw.append(["mangle", "front",
                                        " -A FIREWALL_%s" % self.ip +
                                        " -s %s " % cidr +
                                        " -p %s " % rule['protocol'] +
                                        " --icmp-type %s -j %s" % (icmp_type, self.rule['action'])])
                    else:
                        self.fw.append(["mangle", "front",
                                        " -A FIREWALL_%s" % self.ip +
                                        " -s %s " % cidr +
                                        " -p %s " % rule['protocol'] +
                                        "  %s -j %s" % (rnge, self.rule['action'])])

            sflag = False
            dflag = False
            if self.direction == 'egress':
                ruleId = self.rule['id']
                sourceIpsetName = 'sourceCidrIpset-%d' % ruleId
                destIpsetName = 'destCidrIpset-%d' % ruleId

                # Create source cidr ipset
                srcIpset = 'ipset create '+sourceIpsetName + ' hash:net '
                dstIpset = 'ipset create '+destIpsetName + ' hash:net '

                CsHelper.execute(srcIpset)
                CsHelper.execute(dstIpset)
                for cidr in self.rule['cidr']:
                    ipsetAddCmd = 'ipset add ' + sourceIpsetName + ' ' + cidr
                    CsHelper.execute(ipsetAddCmd)
                    sflag = True

                logging.debug("egress   rule  ####==> %s", self.rule)
                for cidr in self.rule['dcidr']:
                    if cidr == CIDR_ALL:
                        continue
                    ipsetAddCmd = 'ipset add ' + destIpsetName + ' ' + cidr
                    CsHelper.execute(ipsetAddCmd)
                    dflag = True

                self.fw.append(["filter", "", " -A FW_OUTBOUND -j FW_EGRESS_RULES"])

                fwr = " -I FW_EGRESS_RULES"
                # In case we have a default rule (accept all or drop all), we have to evaluate the action again.
                if rule['type'] == 'all' and not rule['source_cidr_list']:
                    fwr = " -A FW_EGRESS_RULES"
                    # For default egress ALLOW or DENY, the logic is inverted.
                    # Having default_egress_policy == True, means that the default rule should have ACCEPT,
                    # otherwise DROP. The rule should be appended, not inserted.
                    if self.rule['default_egress_policy']:
                        self.rule['action'] = "ACCEPT"
                    else:
                        self.rule['action'] = "DROP"
                else:
                    # For other rules added, if default_egress_policy == True, following rules should be DROP,
                    # otherwise ACCEPT
                    if self.rule['default_egress_policy']:
                        self.rule['action'] = "DROP"
                    else:
                        self.rule['action'] = "ACCEPT"

                egressIpsetStr = ''
                if sflag and dflag:
                    egressIpsetStr = " -m set --match-set %s src " % sourceIpsetName + \
                                     " -m set --match-set %s dst " % destIpsetName
                elif sflag:
                    egressIpsetStr = " -m set --match-set %s src " % sourceIpsetName
                elif dflag:
                    egressIpsetStr = " -m set --match-set %s dst " % destIpsetName

                if rule['protocol'] == "icmp":
                    fwr += egressIpsetStr + " -p %s " % rule['protocol'] + " -m %s " % rule['protocol'] + \
                                     " --icmp-type %s" % icmp_type
                elif rule['protocol'] != "all":
                    fwr += egressIpsetStr + " -p %s " % rule['protocol'] + " -m %s " % rule['protocol'] + \
                                     " %s" % rnge
                elif rule['protocol'] == "all":
                    fwr += egressIpsetStr

                self.fw.append(["filter", "", "%s -j %s" % (fwr, rule['action'])])
                logging.debug("EGRESS rule configured for protocol ==> %s, action ==> %s", rule['protocol'], rule['action'])

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
            if "ingress_rules" in list(obj.keys()):
                self.ingress = obj['ingress_rules']
            if "egress_rules" in list(obj.keys()):
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
                if "icmp_type" in list(rule.keys()) and rule['icmp_type'] != -1:
                    self.icmp_type = rule['icmp_type']
                if "icmp_code" in list(rule.keys()) and rule['icmp_code'] != -1:
                    self.icmp_type = "%s/%s" % (self.icmp_type, rule['icmp_code'])
                if self.type == "protocol":
                    if rule['protocol'] == 41:
                        rule['protocol'] = "ipv6"
                    self.protocol = rule['protocol']
                self.action = "DROP"
                self.dport = ""
                if 'allowed' in list(rule.keys()) and rule['allowed']:
                    self.action = "ACCEPT"
                if 'first_port' in list(rule.keys()):
                    self.dport = "-m %s --dport %s" % (self.protocol, rule['first_port'])
                if 'last_port' in list(rule.keys()) and self.dport and \
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

    def flushAllowAllEgressRules(self):
        logging.debug("Flush allow 'all' egress firewall rule")
        # Ensure that FW_EGRESS_RULES chain exists
        CsHelper.execute("iptables-save | grep '^:FW_EGRESS_RULES' || iptables -t filter -N FW_EGRESS_RULES")
        CsHelper.execute("iptables-save | grep '^-A FW_EGRESS_RULES -j ACCEPT$' | sed 's/^-A/iptables -t filter -D/g' | bash")
        CsHelper.execute("iptables -F FW_EGRESS_RULES")
        CsHelper.execute("ipset -L | grep Name:  | awk {'print $2'} | ipset flush")
        CsHelper.execute("ipset -L | grep Name:  | awk {'print $2'} | ipset destroy")

    def process(self):
        for item in self.dbag:
            if item == "id":
                continue
            if self.config.is_vpc():
                self.AclDevice(self.dbag[item], self.config).create()
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
                # need to pad data if it is not valid base 64
                if len(data) % 4 != 0:
                    data += (4 - (len(data) % 4)) * "="
                data = base64.b64decode(data)

        fh = open(dest, "w")
        self.__exflock(fh)
        if data is not None:
            fh.write(data)
        else:
            fh.write("")
        self.__unflock(fh)
        fh.close()
        os.chmod(dest, 0o644)

        if folder == "metadata" or folder == "meta-data":
            try:
                os.makedirs(metamanifestdir, 0o755)
            except OSError as e:
                # error 17 is already exists, we do it this way for concurrency
                if e.errno != 17:
                    print("failed to make directories " + metamanifestdir + " due to :" + e.strerror)
                    sys.exit(1)
            if os.path.exists(metamanifest):
                fh = open(metamanifest, "r+")
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
            os.chmod(metamanifest, 0o644)

    def __htaccess(self, ip, folder, file):
        entry = "RewriteRule ^" + file + "$  ../" + folder + "/%{REMOTE_ADDR}/" + file + " [L,NC,QSA]"
        htaccessFolder = "/var/www/html/latest"
        htaccessFile = htaccessFolder + "/.htaccess"

        CsHelper.mkdir(htaccessFolder, 0o755, True)

        if os.path.exists(htaccessFile):
            fh = open(htaccessFile, "r+")
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
            os.makedirs(htaccessFolder, 0o755)
        except OSError as e:
            # error 17 is already exists, we do it this way for sake of concurrency
            if e.errno != 17:
                print("failed to make directories " + htaccessFolder + " due to :" + e.strerror)
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

            fh = open(htaccessFile, "r+")
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
            print("failed to lock file" + file.name + " due to : " + e.strerror)
            sys.exit(1)  # FIXME
        return True

    def __unflock(self, file):
        try:
            flock(file, LOCK_UN)
        except IOError as e:
            print("failed to unlock file" + file.name + " due to : " + e.strerror)
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

        for vpn in self.dbag:
            if vpn == "id":
                continue

            local_ip = self.dbag[vpn]['local_public_ip']
            dev = CsHelper.get_device(local_ip)

            if dev == "":
                logging.error("Request for ipsec to %s not possible because ip is not configured", local_ip)
                continue

            CsHelper.start_if_stopped("ipsec")
            self.configure_iptables(dev, self.dbag[vpn])
            self.configure_ipsec(self.dbag[vpn])

        # Delete vpns that are no longer in the configuration
        for ip in self.confips:
            self.deletevpn(ip)

    def deletevpn(self, ip):
        logging.info("Removing VPN configuration for %s", ip)
        CsHelper.execute("ipsec down vpn-%s" % ip)
        CsHelper.execute("ipsec down vpn-%s" % ip)
        vpnconffile = "%s/ipsec.vpn-%s.conf" % (self.VPNCONFDIR, ip)
        vpnsecretsfile = "%s/ipsec.vpn-%s.secrets" % (self.VPNCONFDIR, ip)
        os.remove(vpnconffile)
        os.remove(vpnsecretsfile)
        CsHelper.execute("ipsec reload")

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
        peerlist = obj['peer_guest_cidr_list'].replace(' ', '')
        vpnconffile = "%s/ipsec.vpn-%s.conf" % (self.VPNCONFDIR, rightpeer)
        vpnsecretsfile = "%s/ipsec.vpn-%s.secrets" % (self.VPNCONFDIR, rightpeer)
        ikepolicy = obj['ike_policy'].replace(';', '-')
        esppolicy = obj['esp_policy'].replace(';', '-')

        if rightpeer in self.confips:
            self.confips.remove(rightpeer)
        file = CsFile(vpnconffile)
        file.add("#conn for vpn-%s" % rightpeer, 0)
        file.search("conn ", "conn vpn-%s" % rightpeer)
        file.addeq(" left=%s" % leftpeer)
        file.addeq(" leftsubnet=%s" % obj['local_guest_cidr'])
        file.addeq(" right=%s" % rightpeer)
        file.addeq(" rightsubnet=%s" % peerlist)
        file.addeq(" type=tunnel")
        file.addeq(" authby=secret")
        file.addeq(" keyexchange=ike")
        file.addeq(" ike=%s" % ikepolicy)
        file.addeq(" ikelifetime=%s" % self.convert_sec_to_h(obj['ike_lifetime']))
        file.addeq(" esp=%s" % esppolicy)
        file.addeq(" lifetime=%s" % self.convert_sec_to_h(obj['esp_lifetime']))
        file.addeq(" keyingtries=2")
        file.addeq(" auto=route")
        if 'encap' not in obj:
            obj['encap'] = False
        file.addeq(" forceencaps=%s" % CsHelper.bool_to_yn(obj['encap']))
        if obj['dpd']:
            file.addeq(" dpddelay=30")
            file.addeq(" dpdtimeout=120")
            file.addeq(" dpdaction=restart")
        secret = CsFile(vpnsecretsfile)
        secret.search("%s " % leftpeer, "%s %s : PSK \"%s\"" % (leftpeer, rightpeer, obj['ipsec_psk']))
        if secret.is_changed() or file.is_changed():
            secret.commit()
            file.commit()
            logging.info("Configured vpn %s %s", leftpeer, rightpeer)
            CsHelper.execute("ipsec rereadsecrets")

        # This will load the new config
        CsHelper.execute("ipsec reload")
        os.chmod(vpnsecretsfile, 0o400)

        for i in range(3):
            result = CsHelper.execute('ipsec status vpn-%s | grep "%s"' % (rightpeer, peerlist.split(",", 1)[0]))
            if len(result) > 0:
                break
            time.sleep(1)

        # With 'auto=route', connections are established on an attempt to
        # communicate over the S2S VPN. This uses ping to initialize the connection.
        CsHelper.execute("timeout 5 ping -c 3 %s" % (peerlist.split("/", 1)[0].replace(".0", ".1")))

    def convert_sec_to_h(self, val):
        hrs = int(val) / 3600
        return "%sh" % hrs


class CsVpnUser(CsDataBag):
    PPP_CHAP = '/etc/ppp/chap-secrets'

    def process(self):
        for user in self.dbag:
            if user == 'id':
                continue

            userconfig = self.dbag[user]
            if userconfig['add']:
                self.add_l2tp_ipsec_user(user, userconfig)
            else:
                self.del_l2tp_ipsec_user(user, userconfig)

    def add_l2tp_ipsec_user(self, user, obj):
        userfound = False
        password = obj['password']

        userAddEntry = "%s * %s *" % (user, password)
        logging.debug("Adding vpn user '%s'" % user)

        file = CsFile(self.PPP_CHAP)
        userfound = file.searchString(userAddEntry, '#')
        if not userfound:
            logging.debug("User is not there already, so adding user")
            self.del_l2tp_ipsec_user(user, obj)
            file.add(userAddEntry)
        file.commit()

    def del_l2tp_ipsec_user(self, user, obj):
        userfound = False
        password = obj['password']
        userentry = "%s * %s *" % (user, password)

        logging.debug("Deleting the user '%s'" % user)
        file = CsFile(self.PPP_CHAP)
        file.deleteLine(userentry)
        file.commit()

        if not os.path.exists('/var/run/pppd2.tdb'):
            return

        logging.debug("killing the PPPD process for the user '%s'" % user)

        fileContents = CsHelper.execute("tdbdump /var/run/pppd2.tdb")
        for line in fileContents:
            if user in line:
                contentlist = line.split(';')
                for str in contentlist:
                    pppd = str.split('=')[0]
                    if pppd == 'PPPD_PID':
                        pid = str.split('=')[1]
                        if pid:
                            logging.debug("killing process %s" % pid)
                            CsHelper.execute('kill -9 %s' % pid)


class CsRemoteAccessVpn(CsDataBag):
    VPNCONFDIR = "/etc/ipsec.d"

    def process(self):
        self.confips = []

        logging.debug(self.dbag)

        for public_ip in self.dbag:
            if public_ip == "id":
                continue
            vpnconfig = self.dbag[public_ip]

            # Enable remote access vpn
            if vpnconfig['create']:
                logging.debug("Enabling remote access vpn on " + public_ip)

                CsHelper.start_if_stopped("ipsec")
                self.configure_l2tpIpsec(public_ip, self.dbag[public_ip])
                logging.debug("Remote accessvpn  data bag %s",  self.dbag)
                self.remoteaccessvpn_iptables(public_ip, self.dbag[public_ip])

                CsHelper.execute("ipsec update")
                CsHelper.execute("systemctl start xl2tpd")
                CsHelper.execute("ipsec rereadsecrets")
            else:
                logging.debug("Disabling remote access vpn .....")
                CsHelper.execute("ipsec down L2TP-PSK")
                CsHelper.execute("systemctl stop xl2tpd")

    def configure_l2tpIpsec(self, left, obj):
        l2tpconffile = "%s/l2tp.conf" % (self.VPNCONFDIR)
        vpnsecretfilte = "%s/ipsec.any.secrets" % (self.VPNCONFDIR)
        xl2tpdconffile = "/etc/xl2tpd/xl2tpd.conf"
        xl2tpoptionsfile = "/etc/ppp/options.xl2tpd"

        localip = obj['local_ip']
        localcidr = obj['local_cidr']
        publicIface = obj['public_interface']
        iprange = obj['ip_range']
        psk = obj['preshared_key']

        # Left
        l2tpfile = CsFile(l2tpconffile)
        l2tpfile.addeq(" left=%s" % left)
        l2tpfile.commit()

        secret = CsFile(vpnsecretfilte)
        secret.empty()
        secret.addeq("%s %%any : PSK \"%s\"" % (left, psk))
        secret.commit()

        xl2tpdconf = CsFile(xl2tpdconffile)
        xl2tpdconf.addeq("ip range = %s" % iprange)
        xl2tpdconf.addeq("local ip = %s" % localip)
        xl2tpdconf.commit()

        xl2tpoptions = CsFile(xl2tpoptionsfile)
        xl2tpoptions.search("ms-dns ", "ms-dns %s" % localip)
        xl2tpoptions.commit()

    def remoteaccessvpn_iptables(self, publicip, obj):
        publicdev = obj['public_interface']
        localcidr = obj['local_cidr']
        local_ip = obj['local_ip']

        self.fw.append(["", "", "-A INPUT -i %s --dst %s -p udp -m udp --dport 500 -j ACCEPT" % (publicdev, publicip)])
        self.fw.append(["", "", "-A INPUT -i %s --dst %s -p udp -m udp --dport 4500 -j ACCEPT" % (publicdev, publicip)])
        self.fw.append(["", "", "-A INPUT -i %s --dst %s -p udp -m udp --dport 1701 -j ACCEPT" % (publicdev, publicip)])
        self.fw.append(["", "", "-A INPUT -i %s -p ah -j ACCEPT" % publicdev])
        self.fw.append(["", "", "-A INPUT -i %s -p esp -j ACCEPT" % publicdev])
        self.fw.append(["", "", "-A OUTPUT -p ah -j ACCEPT"])
        self.fw.append(["", "", "-A OUTPUT -p esp -j ACCEPT"])

        if self.config.is_vpc():
            self.fw.append(["", "", " -N VPN_FORWARD"])
            self.fw.append(["", "", "-I FORWARD -i ppp+ -j VPN_FORWARD"])
            self.fw.append(["", "", "-I FORWARD -o ppp+ -j VPN_FORWARD"])
            self.fw.append(["", "", "-I FORWARD -o ppp+ -j VPN_FORWARD"])
            self.fw.append(["", "", "-A VPN_FORWARD -s  %s -j RETURN" % localcidr])
            self.fw.append(["", "", "-A VPN_FORWARD -i ppp+ -d %s -j RETURN" % localcidr])
            self.fw.append(["", "", "-A VPN_FORWARD -i ppp+  -o ppp+ -j RETURN"])
        else:
            self.fw.append(["", "", "-A FORWARD -i ppp+ -o  ppp+ -j ACCEPT"])
            self.fw.append(["", "", "-A FORWARD -s %s -o  ppp+ -j ACCEPT" % localcidr])
            self.fw.append(["", "", "-A FORWARD -i ppp+ -d %s  -j ACCEPT" % localcidr])

        self.fw.append(["", "", "-A INPUT -i ppp+ -m udp -p udp --dport 53 -j ACCEPT"])
        self.fw.append(["", "", "-A INPUT -i ppp+ -m tcp -p tcp --dport 53 -j ACCEPT"])
        self.fw.append(["nat", "", "-I PREROUTING -i ppp+ -p tcp -m tcp --dport 53 -j DNAT --to-destination %s" % local_ip])

        if self.config.is_vpc():
            return

        self.fw.append(["mangle", "", "-N  VPN_%s " % publicip])
        self.fw.append(["mangle", "", "-A VPN_%s -j RETURN " % publicip])
        self.fw.append(["mangle", "", "-I VPN_%s -p ah  -j ACCEPT " % publicip])
        self.fw.append(["mangle", "", "-I VPN_%s -p esp  -j ACCEPT " % publicip])
        self.fw.append(["mangle", "", "-I PREROUTING  -d %s -j VPN_%s " % (publicip, publicip)])


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

    # Return the VR guest interface ip
    def getGuestIp(self):
        interfaces = []
        ipAddr = None
        for interface in self.config.address().get_interfaces():
            if interface.is_guest():
                interfaces.append(interface)
            if len(interfaces) > 0:
                ipAddr = sorted(interfaces)[-1]
            if ipAddr:
                return ipAddr.get_ip()

        return None

    def getGuestIpByIp(self, ipa):
        for interface in self.config.address().get_interfaces():
            if interface.ip_in_subnet(ipa):
                return interface.get_ip()
        return None

    def getDeviceByIp(self, ipa):
        for interface in self.config.address().get_interfaces():
            if interface.ip_in_subnet(ipa):
                return interface.get_device()
        return None

    def getNetworkByIp(self, ipa):
        for interface in self.config.address().get_interfaces():
            if interface.ip_in_subnet(ipa):
                return interface.get_network()
        return None

    def getGatewayByIp(self, ipa):
        for interface in self.config.address().get_interfaces():
            if interface.ip_in_subnet(ipa):
                return interface.get_gateway()
        return None

    def getPrivateGatewayNetworks(self):
        interfaces = []
        for interface in self.config.address().get_interfaces():
            if interface.is_private_gateway():
                interfaces.append(interface)
        return interfaces

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
        # Prefetch iptables variables
        public_fwinterface = self.getDeviceByIp(rule['public_ip'])
        internal_fwinterface = self.getDeviceByIp(rule['internal_ip'])
        public_fwports = self.portsToString(rule['public_ports'], ':')
        internal_fwports = self.portsToString(rule['internal_ports'], '-')
        fw1 = "-A PREROUTING -d %s/32 -i %s -p %s -m %s --dport %s -j DNAT --to-destination %s:%s" % \
              (
                rule['public_ip'],
                public_fwinterface,
                rule['protocol'],
                rule['protocol'],
                public_fwports,
                rule['internal_ip'],
                internal_fwports
              )
        fw2 = "-A PREROUTING -d %s/32 -i %s -p %s -m %s --dport %s -j DNAT --to-destination %s:%s" % \
              (
                rule['public_ip'],
                internal_fwinterface,
                rule['protocol'],
                rule['protocol'],
                public_fwports,
                rule['internal_ip'],
                internal_fwports
              )
        fw3 = "-A OUTPUT -d %s/32 -p %s -m %s --dport %s -j DNAT --to-destination %s:%s" % \
              (
                rule['public_ip'],
                rule['protocol'],
                rule['protocol'],
                public_fwports,
                rule['internal_ip'],
                internal_fwports
              )
        fw4 = "-j SNAT --to-source %s -A POSTROUTING -s %s -d %s/32 -o %s -p %s -m %s --dport %s" % \
              (
                self.getGuestIp(),
                self.getNetworkByIp(rule['internal_ip']),
                rule['internal_ip'],
                internal_fwinterface,
                rule['protocol'],
                rule['protocol'],
                self.portsToString(rule['internal_ports'], ':')
              )
        fw5 = "-A PREROUTING -d %s/32 -i %s -p %s -m %s --dport %s -j MARK --set-xmark %s/0xffffffff" % \
              (
                rule['public_ip'],
                public_fwinterface,
                rule['protocol'],
                rule['protocol'],
                public_fwports,
                hex(100 + int(public_fwinterface[3:]))
              )
        fw6 = "-A PREROUTING -d %s/32 -i %s -p %s -m %s --dport %s -m state --state NEW -j CONNMARK --save-mark --nfmask 0xffffffff --ctmask 0xffffffff" % \
              (
                rule['public_ip'],
                public_fwinterface,
                rule['protocol'],
                rule['protocol'],
                public_fwports,
              )
        fw7 = "-A FORWARD -i %s -o %s -p %s -m %s --dport %s -m state --state NEW,ESTABLISHED -j ACCEPT" % \
              (
                public_fwinterface,
                internal_fwinterface,
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
        self.fw.append(["filter", "", fw7])

    def forward_vpc(self, rule):
        fw_prerout_rule = "-A PREROUTING -d %s/32 " % (rule["public_ip"])
        if not rule["protocol"] == "any":
            fw_prerout_rule += " -m %s -p %s" % (rule["protocol"], rule["protocol"])
        if not rule["public_ports"] == "any":
            fw_prerout_rule += " --dport %s" % self.portsToString(rule["public_ports"], ":")
        fw_prerout_rule += " -j DNAT --to-destination %s" % rule["internal_ip"]
        if not rule["internal_ports"] == "any":
            fw_prerout_rule += ":" + self.portsToString(rule["internal_ports"], "-")

        fw_output_rule = "-A OUTPUT -d %s/32" % rule["public_ip"]
        if not rule["protocol"] == "any":
            fw_output_rule += " -m %s -p %s" % (rule["protocol"], rule["protocol"])
        if not rule["public_ports"] == "any":
            fw_output_rule += " --dport %s" % self.portsToString(rule["public_ports"], ":")
        fw_output_rule += " -j DNAT --to-destination %s" % rule["internal_ip"]
        if not rule["internal_ports"] == "any":
            fw_output_rule += ":" + self.portsToString(rule["internal_ports"], "-")

        fw_postrout_rule2 = "-j SNAT --to-source %s -A POSTROUTING -s %s -d %s/32 -o %s -p %s -m %s --dport %s" % \
            (
                self.getGuestIpByIp(rule['internal_ip']),
                self.getNetworkByIp(rule['internal_ip']),
                rule['internal_ip'],
                self.getDeviceByIp(rule['internal_ip']),
                rule['protocol'],
                rule['protocol'],
                self.portsToString(rule['internal_ports'], ':')
            )

        self.fw.append(["nat", "", fw_prerout_rule])
        self.fw.append(["nat", "", fw_postrout_rule2])
        self.fw.append(["nat", "", fw_output_rule])

    def processStaticNatRule(self, rule):
        # FIXME this needs ordering with the VPN no nat rule
        device = self.getDeviceByIp(rule["public_ip"])
        if device is None:
            raise Exception("Ip address %s has no device in the ips databag" % rule["public_ip"])

        chain_name = "PREROUTING-%s-def" % device
        self.fw.append(["mangle", "front",
                        "-A PREROUTING -s %s/32 -m state --state NEW -j %s" %
                        (rule["internal_ip"], chain_name)])
        self.fw.append(["mangle", "",
                        "-A %s -j MARK --set-xmark %s/0xffffffff" %
                        (chain_name, hex(100 + int(device[len("eth"):])))])
        self.fw.append(["mangle", "",
                        "-A %s -j CONNMARK --save-mark --nfmask 0xffffffff --ctmask 0xffffffff" %
                        chain_name])
        private_gateways = self.getPrivateGatewayNetworks()
        for private_gw in private_gateways:
            self.fw.append(["mangle", "front", "-A %s -d %s -j RETURN" %
                            (chain_name, private_gw.get_network())])

        self.fw.append(["nat", "front",
                        "-A PREROUTING -d %s/32 -j DNAT --to-destination %s" % (rule["public_ip"], rule["internal_ip"])])
        self.fw.append(["nat", "front",
                        "-A POSTROUTING -o %s -s %s/32 -j SNAT --to-source %s" % (device, rule["internal_ip"], rule["public_ip"])])
        self.fw.append(["nat", "front",
                        "-A OUTPUT -d %s/32 -j DNAT --to-destination %s" % (rule["public_ip"], rule["internal_ip"])])
        self.fw.append(["filter", "",
                        "-A FORWARD -i %s -o eth0  -d %s  -m state  --state NEW -j ACCEPT " % (device, rule["internal_ip"])])

        # Configure the hairpin snat
        self.fw.append(["nat", "front", "-A POSTROUTING -s %s -d %s -j SNAT -o %s --to-source %s" %
                        (self.getNetworkByIp(rule['internal_ip']), rule["internal_ip"], self.getDeviceByIp(rule["internal_ip"]), self.getGuestIp())])


class IpTablesExecutor:

    config = None

    def __init__(self, config):
        self.config = config

    def process(self):
        acls = CsAcl('networkacl', self.config)
        acls.process()

        acls = CsAcl('firewallrules', self.config)
        acls.flushAllowAllEgressRules()
        acls.process()

        fwd = CsForwardingRules("forwardingrules", self.config)
        fwd.process()

        vpns = CsSite2SiteVpn("site2sitevpn", self.config)
        vpns.process()

        rvpn = CsRemoteAccessVpn("remoteaccessvpn", self.config)
        rvpn.process()

        lb = CsLoadBalancer("loadbalancer", self.config)
        lb.process()

        logging.debug("Configuring iptables rules")
        nf = CsNetfilters()
        nf.compare(self.config.get_fw())

        logging.debug("Configuring iptables rules done ...saving rules")

        # Save iptables configuration - will be loaded on reboot by the iptables-restore that is configured on /etc/rc.local
        CsHelper.save_iptables("iptables-save", "/etc/iptables/rules.v4")
        CsHelper.save_iptables("ip6tables-save", "/etc/iptables/rules.v6")


def main(argv):
    # The file we are currently processing, if it is "cmd_line.json" everything will be processed.
    process_file = argv[1]

    if process_file is None:
        logging.debug("No file was received, do not go on processing the other actions. Just leave for now.")
        return

    json_type = os.path.basename(process_file).split('.json')[0]

    # The "GLOBAL" Configuration object
    config = CsConfig()

    # Load stored ip addresses from disk to CsConfig()
    config.set_address()

    logging.debug("Configuring ip addresses")
    config.address().compare()
    config.address().process()

    databag_map = OrderedDict([("guest_network",     {"process_iptables": True,  "executor": []}),
                               ("vm_password",       {"process_iptables": False, "executor": [CsPassword("vmpassword", config)]}),
                               ("vm_metadata",       {"process_iptables": False, "executor": [CsVmMetadata('vmdata', config)]}),
                               ("network_acl",       {"process_iptables": True,  "executor": []}),
                               ("firewall_rules",    {"process_iptables": True,  "executor": []}),
                               ("forwarding_rules",  {"process_iptables": True,  "executor": []}),
                               ("staticnat_rules",   {"process_iptables": True,  "executor": []}),
                               ("site_2_site_vpn",   {"process_iptables": True,  "executor": []}),
                               ("remote_access_vpn", {"process_iptables": True,  "executor": []}),
                               ("vpn_user_list",     {"process_iptables": False, "executor": [CsVpnUser("vpnuserlist", config)]}),
                               ("vm_dhcp_entry",     {"process_iptables": False, "executor": [CsDhcp("dhcpentry", config)]}),
                               ("dhcp",              {"process_iptables": False, "executor": [CsDhcp("dhcpentry", config)]}),
                               ("load_balancer",     {"process_iptables": True,  "executor": []}),
                               ("monitor_service",   {"process_iptables": False, "executor": [CsMonitor("monitorservice", config)]}),
                               ("static_routes",     {"process_iptables": False, "executor": [CsStaticRoutes("staticroutes", config)]})
                               ])

    def execDatabag(key, db):
        if key not in list(db.keys()) or 'executor' not in db[key]:
            logging.warn("Unable to find config or executor(s) for the databag type %s" % key)
            return
        for executor in db[key]['executor']:
            logging.debug("Processing for databag type: %s" % key)
            executor.process()

    def execIptables(config):
        logging.debug("Processing iptables rules")
        iptables_executor = IpTablesExecutor(config)
        iptables_executor.process()

    if json_type == "cmd_line":
        logging.debug("cmd_line.json changed. All other files will be processed as well.")
        for key in list(databag_map.keys()):
            execDatabag(key, databag_map)
        execIptables(config)
    elif json_type in list(databag_map.keys()):
        execDatabag(json_type, databag_map)
        if databag_map[json_type]['process_iptables']:
            execIptables(config)
    else:
        logging.warn("Unable to find and process databag for file: %s, for json type=%s" % (process_file, json_type))

    red = CsRedundant(config)
    red.set()
    return 0


if __name__ == "__main__":
    main(sys.argv)
