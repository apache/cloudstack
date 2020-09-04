#!/usr/bin/python3
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

import argparse
from subprocess import check_output, CalledProcessError
import logging
import sys
import os
import xml.dom.minidom
import re
import libvirt
import fcntl
import time
import ipaddress

logpath = "/var/run/cloud/"        # FIXME: Logs should reside in /var/log/cloud
lock_file = "/var/lock/cloudstack_security_group.lock"
driver = "qemu:///system"
lock_handle = None


def obtain_file_lock(path):
    global lock_handle

    try:
        lock_handle = open(path, 'w')
        fcntl.flock(lock_handle, fcntl.LOCK_EX | fcntl.LOCK_NB)
        return True
    except IOError:
        pass

    return False


def execute(cmd):
    logging.debug(cmd)
    try:
        return check_output(cmd, shell=True).decode()
    except CalledProcessError as e:
        logging.exception('Command exited non-zero: %s', cmd)
        raise


def can_bridge_firewall(privnic):
    try:
        execute("which iptables")
    except:
        print("no iptables on your host machine")
        sys.exit(1)

    try:
        execute("which ebtables")
    except:
        print("no ebtables on your host machine")
        sys.exit(2)


    if not os.path.exists(logpath):
        os.makedirs(logpath)

    cleanup_rules_for_dead_vms()
    cleanup_rules()

    return True


def get_libvirt_connection():
    conn = libvirt.openReadOnly(driver)
    if not conn:
        raise Exception('Failed to open connection to the hypervisor')

    return conn


def virshlist(states):
    libvirt_states={ 'running': libvirt.VIR_DOMAIN_RUNNING,
                     'shutoff': libvirt.VIR_DOMAIN_SHUTOFF,
                     'shutdown': libvirt.VIR_DOMAIN_SHUTDOWN,
                     'paused': libvirt.VIR_DOMAIN_PAUSED,
                     'nostate': libvirt.VIR_DOMAIN_NOSTATE,
                     'blocked': libvirt.VIR_DOMAIN_BLOCKED,
                     'crashed': libvirt.VIR_DOMAIN_CRASHED,
    }

    searchstates = list(libvirt_states[state] for state in states)

    conn = get_libvirt_connection()

    alldomains = [domain for domain in map(conn.lookupByID, conn.listDomainsID())]
    alldomains += [domain for domain in map(conn.lookupByName, conn.listDefinedDomains())]

    domains = []
    for domain in alldomains:
        if domain.info()[0] in searchstates:
            domains.append(domain.name())

    conn.close()

    return domains


def virshdumpxml(domain):
    try:
        conn = get_libvirt_connection()
        dom = conn.lookupByName(domain)
        conn.close()
        return dom.XMLDesc(0)
    except libvirt.libvirtError:
        return None


def ipv6_link_local_addr(mac=None):
    eui64 = re.sub(r'[.:-]', '', mac).lower()
    eui64 = eui64[0:6] + 'fffe' + eui64[6:]
    eui64 = hex(int(eui64[0:2], 16) ^ 2)[2:].zfill(2) + eui64[2:]
    return ipaddress.ip_address('fe80::' + ':'.join(re.findall(r'.{4}', eui64)))


def split_ips_by_family(ips):
    if type(ips) is str:
        ips = [ip for ip in ips.split(';') if ip != '']

    ip4s = []
    ip6s = []
    for ip in ips:
        network = ipaddress.ip_network(ip)
        if network.version == 4:
            ip4s.append(ip)
        elif network.version == 6:
            ip6s.append(ip)
    return ip4s, ip6s

def destroy_network_rules_for_nic(vm_name, vm_ip, vm_mac, vif, sec_ips):
    try:
        rules = execute("""iptables-save -t filter | awk '/ %s / { sub(/-A/, "-D", $1) ; print }'""" % vif ).split("\n")
        for rule in [_f for _f in rules if _f]:
            try:
                execute("iptables " + rule)
            except:
                logging.debug("Ignoring failure to delete rule: " + rule)
    except:
        pass

    try:
        dnats = execute("""iptables-save -t nat | awk '/ %s / { sub(/-A/, "-D", $1) ; print }'""" % vif ).split("\n")
        for dnat in [_f for _f in dnats if _f]:
            try:
                execute("iptables -t nat " + dnat)
            except:
                logging.debug("Ignoring failure to delete dnat: " + dnat)
    except:
        pass

    ips = sec_ips.split(';')
    ips.pop()
    ips.append(vm_ip)
    add_to_ipset(vm_name, ips, "-D")
    ebtables_rules_vmip(vm_name, vm_mac, ips, "-D")

    vmchain_in = vm_name + "-in"
    vmchain_out = vm_name + "-out"
    vmchain_in_src = vm_name + "-in-src"
    vmchain_out_dst = vm_name + "-out-dst"
    try:
        execute("ebtables -t nat -D " + vmchain_in_src + " -s " + vm_mac + " -j RETURN")
        execute("ebtables -t nat -D " + vmchain_out_dst + " -p ARP --arp-op Reply --arp-mac-dst " + vm_mac + " -j RETURN")
        execute("ebtables -t nat -D PREROUTING -i " + vif + " -j " + vmchain_in)
        execute("ebtables -t nat -D POSTROUTING -o " + vif + " -j " + vmchain_out)
    except:
        logging.debug("Ignoring failure to delete ebtable rules for vm: " + vm_name)

def get_bridge_physdev(brname):
    physdev = execute("bridge -o link show | awk '/master %s / && !/^[0-9]+: vnet/ {print $2}' | head -1 | cut -d ':' -f1" % brname)
    return physdev.strip()


def destroy_network_rules_for_vm(vm_name, vif=None):
    vmchain = iptables_chain_name(vm_name)
    vmchain_egress = egress_chain_name(vm_name)
    vmchain_default = None
    vm_ipsetname=ipset_chain_name(vm_name)

    delete_rules_for_vm_in_bridge_firewall_chain(vm_name)
    if 1 in [vm_name.startswith(c) for c in ['r-', 's-', 'v-']]:
        return True

    if vm_name.startswith('i-'):
        vmchain_default = '-'.join(vm_name.split('-')[:-1]) + "-def"

    destroy_ebtables_rules(vm_name, vif)

    chains = [vmchain_default, vmchain, vmchain_egress]
    for chain in [_f for _f in chains if _f]:
        try:
            execute("iptables -F " + chain)
            execute('ip6tables -F ' + chain)
        except:
            logging.debug("Ignoring failure to flush chain: " + chain)

    for chain in [_f for _f in chains if _f]:
        try:
            execute("iptables -X " + chain)
            execute('ip6tables -X ' + chain)
        except:
            logging.debug("Ignoring failure to delete chain: " + chain)

    try:
        for ipset in [vm_ipsetname, vm_ipsetname + '-6']:
            execute('ipset -F ' + ipset)
            execute('ipset -X ' + ipset)
    except:
        logging.debug("Ignoring failure to delete ipset " + vmchain)

    if vif:
        try:
            dnats = execute("""iptables -t nat -S | awk '/%s/ { sub(/-A/, "-D", $1) ; print }'""" % vif ).split("\n")
            for dnat in [_f for _f in dnats if _f]:
                try:
                    execute("iptables -t nat " + dnat)
                except:
                    logging.debug("Ignoring failure to delete dnat: " + dnat)
        except:
            pass
    remove_rule_log_for_vm(vm_name)
    remove_secip_log_for_vm(vm_name)

    return True


def destroy_ebtables_rules(vm_name, vif):
    eb_vm_chain = ebtables_chain_name(vm_name)
    delcmd = "ebtables -t nat -L PREROUTING | grep " + eb_vm_chain
    delcmds = []
    try:
        delcmds = [_f for _f in execute(delcmd).split('\n') if _f]
        delcmds = ["-D PREROUTING " + x for x in delcmds ]
    except:
        pass
    postcmds = []
    try:
        postcmd = "ebtables -t nat -L POSTROUTING | grep " + eb_vm_chain
        postcmds = [_f for _f in execute(postcmd).split('\n') if _f]
        postcmds = ["-D POSTROUTING " + x for x in postcmds]
    except:
        pass

    delcmds += postcmds

    for cmd in delcmds:
        try:
            execute("ebtables -t nat " + cmd)
        except:
            logging.debug("Ignoring failure to delete ebtables rules for vm " + vm_name)
    chains = [eb_vm_chain+"-in", eb_vm_chain+"-out", eb_vm_chain+"-in-ips", eb_vm_chain+"-out-ips", eb_vm_chain+"-in-src", eb_vm_chain+"-out-dst"]
    for chain in chains:
        try:
            execute("ebtables -t nat -F " + chain)
            execute("ebtables -t nat -X " + chain)
        except:
            logging.debug("Ignoring failure to delete ebtables chain for vm " + vm_name)


def default_ebtables_rules(vm_name, vm_ip, vm_mac, vif, is_first_nic=False):
    eb_vm_chain=ebtables_chain_name(vm_name)
    vmchain_in = eb_vm_chain + "-in"
    vmchain_out = eb_vm_chain + "-out"
    vmchain_in_ips = eb_vm_chain + "-in-ips"
    vmchain_out_ips = eb_vm_chain + "-out-ips"
    vmchain_in_src = eb_vm_chain + "-in-src"
    vmchain_out_dst = eb_vm_chain + "-out-dst"

    if not is_first_nic:
        try:
            execute("ebtables -t nat -A PREROUTING -i " + vif + " -j " + vmchain_in)
            execute("ebtables -t nat -A POSTROUTING -o " + vif + " -j " + vmchain_out)
            execute("ebtables -t nat -I " + vmchain_in_src + " -s " + vm_mac + " -j RETURN")
            if vm_ip:
                execute("ebtables -t nat -I " + vmchain_in_ips + " -p ARP -s " + vm_mac + " --arp-mac-src " + vm_mac + " --arp-ip-src " + vm_ip + " -j RETURN")
            execute("ebtables -t nat -I " + vmchain_out_dst + " -p ARP --arp-op Reply --arp-mac-dst " + vm_mac + " -j RETURN")
            if vm_ip:
                execute("ebtables -t nat -I " + vmchain_out_ips + " -p ARP --arp-ip-dst " + vm_ip + " -j RETURN")
        except:
            logging.debug("Failed to program rules for additional nic " + vif)
            return False
        return

    destroy_ebtables_rules(vm_name, vif)

    for chain in [vmchain_in, vmchain_out, vmchain_in_ips, vmchain_out_ips, vmchain_in_src, vmchain_out_dst]:
        try:
            execute("ebtables -t nat -N " + chain)
        except:
            execute("ebtables -t nat -F " + chain)

    try:
        # -s ! 52:54:0:56:44:32 -j DROP
        execute("ebtables -t nat -A PREROUTING -i " + vif + " -j " + vmchain_in)
        execute("ebtables -t nat -A POSTROUTING -o " + vif + " -j " + vmchain_out)
        execute("ebtables -t nat -A " + vmchain_in_ips + " -j DROP")
        execute("ebtables -t nat -A " + vmchain_out_ips + " -j DROP")
        execute("ebtables -t nat -A " + vmchain_in_src + " -j DROP")
        execute("ebtables -t nat -A " + vmchain_out_dst + " -p ARP --arp-op Reply -j DROP")

    except:
        logging.debug("Failed to program default rules")
        return False

    try:
        execute("ebtables -t nat -A " + vmchain_in + " -j " + vmchain_in_src)
        execute("ebtables -t nat -I " + vmchain_in_src + " -s " + vm_mac + " -j RETURN")
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP -j " + vmchain_in_ips)
        if vm_ip:
            execute("ebtables -t nat -I " + vmchain_in_ips + " -p ARP -s " + vm_mac + " --arp-mac-src " + vm_mac + " --arp-ip-src " + vm_ip + " -j RETURN")
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP --arp-op Request -j ACCEPT")
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP --arp-op Reply -j ACCEPT")
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP -j DROP")
    except:
        logging.exception("Failed to program default ebtables IN rules")
        return False

    try:
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-op Reply -j " + vmchain_out_dst)
        execute("ebtables -t nat -I " + vmchain_out_dst + " -p ARP --arp-op Reply --arp-mac-dst " + vm_mac + " -j RETURN")
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP -j " + vmchain_out_ips )
        if vm_ip:
            execute("ebtables -t nat -I " + vmchain_out_ips + " -p ARP --arp-ip-dst " + vm_ip + " -j RETURN")
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-op Request -j ACCEPT")
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-op Reply -j ACCEPT")
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP -j DROP")
    except:
        logging.debug("Failed to program default ebtables OUT rules")
        return False

def refactor_ebtable_rules(vm_name):
    vmchain_in = vm_name + "-in"
    vmchain_in_ips = vm_name + "-in-ips"
    vmchain_in_src = vm_name + "-in-src"

    try:
        execute("ebtables -t nat -L " + vmchain_in_src)
        logging.debug("Chain '" + vmchain_in_src + "' exists, ebtables rules have newer version, skip refactoring")
        return True
    except:
        logging.debug("Chain '" + vmchain_in_src + "' does not exist, ebtables rules have old version, start refactoring")

    vif = execute("ebtables -t nat -L PREROUTING | grep " + vmchain_in + " | awk '{print $2}'").strip()
    vm_mac = execute("ebtables -t nat -L " + vmchain_in + " | grep arp-mac-src | awk '{print $5}'").strip()
    vm_ips = execute("ebtables -t nat -L " + vmchain_in_ips + " | grep arp-ip-src | awk '{print $4}'").split('\n')

    destroy_ebtables_rules(vm_name, vif)
    default_ebtables_rules(vm_name, None, vm_mac, vif, True)
    ebtables_rules_vmip(vm_name, vm_mac, vm_ips, "-A")

    logging.debug("Refactoring ebtables rules for vm " + vm_name + " is done")
    return True

def default_network_rules_systemvm(vm_name, localbrname):
    bridges = get_bridges(vm_name)
    domid = get_vm_id(vm_name)
    vmchain = iptables_chain_name(vm_name)

    delete_rules_for_vm_in_bridge_firewall_chain(vm_name)

    try:
        execute("iptables -N " + vmchain)
    except:
        execute("iptables -F " + vmchain)

    for bridge in bridges:
        if bridge != localbrname:
            if not add_fw_framework(bridge):
                return False
            brfw = get_br_fw(bridge)
            vifs = get_vifs_for_bridge(vm_name, bridge)
            for vif in vifs:
                try:
                    execute("iptables -A " + brfw + "-OUT" + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -j " + vmchain)
                    execute("iptables -A " + brfw + "-IN" + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -j " + vmchain)
                    execute("iptables -A " + vmchain + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -j RETURN")
                except:
                    logging.debug("Failed to program default rules")
                    return False

    execute("iptables -A " + vmchain + " -j ACCEPT")

    if not write_rule_log_for_vm(vm_name, '-1', '_ignore_', domid, '_initial_', '-1'):
        logging.debug("Failed to log default network rules for systemvm, ignoring")
    return True


def remove_secip_log_for_vm(vmName):
    vm_name = vmName
    logfilename = logpath + vm_name + ".ip"

    result = True
    try:
        os.remove(logfilename)
    except:
        logging.debug("Failed to delete rule log file " + logfilename)
        result = False

    return result


def write_secip_log_for_vm (vmName, secIps, vmId):
    vm_name = vmName
    logfilename = logpath + vm_name + ".ip"
    logging.debug("Writing log to " + logfilename)
    logf = open(logfilename, 'w')
    output = ','.join([vmName, secIps, vmId])
    result = True

    try:
        logf.write(output)
        logf.write('\n')
    except:
        logging.debug("Failed to write to rule log file " + logfilename)
        result = False

    logf.close()

    return result


def create_ipset_forvm(ipsetname, type='iphash', family='inet'):
    result = True
    try:
        logging.debug("Creating ipset chain .... " + ipsetname)
        execute("ipset -F " + ipsetname)
        execute("ipset -X " + ipsetname)
    except:
        logging.debug("ipset chain not exists creating.... " + ipsetname)
    finally:
        execute('ipset -N ' + ipsetname + ' ' + type + ' family ' + family)

    return result


def add_to_ipset(ipsetname, ips, action):
    result = True
    for ip in ips:
        try:
            logging.debug("vm ip " + str(ip))
            execute("ipset " + action + " " + ipsetname + " " + str(ip))
        except:
            logging.debug("vm ip already in ip set " + str(ip))
            continue

    return result


def network_rules_vmSecondaryIp(vm_name, vm_mac, ip_secondary, action):
    logging.debug("vmName = "+ vm_name)
    logging.debug("vmMac = " + vm_mac)
    logging.debug("action = "+ action)

    vmchain = vm_name
    vmchain6 = vmchain + '-6'

    ip4s, ip6s = split_ips_by_family(ip_secondary)

    add_to_ipset(vmchain, ip4s, action)

    #add ipv6 addresses to ipv6 ipset
    add_to_ipset(vmchain6, ip6s, action)

    #add ebtables rules for the secondary ip
    refactor_ebtable_rules(vm_name)
    ebtables_rules_vmip(vm_name, vm_mac, [ip_secondary], action)

    return True

def ebtables_rules_vmip (vmname, vmmac, ips, action):
    eb_vm_chain=ebtables_chain_name(vmname)
    vmchain_inips = eb_vm_chain + "-in-ips"
    vmchain_outips = eb_vm_chain + "-out-ips"

    if action and action.strip() == "-A":
        action = "-I"

    for ip in [_f for _f in ips if _f]:
        logging.debug("ip = " + ip)
        if ip == 0 or ip == "0":
            continue
        try:
            execute("ebtables -t nat " + action + " " + vmchain_inips + " -p ARP -s " + vmmac + " --arp-mac-src " + vmmac + " --arp-ip-src " + ip + " -j RETURN")
            execute("ebtables -t nat " + action + " " + vmchain_outips + " -p ARP --arp-ip-dst " + ip + " -j RETURN")
        except:
            logging.debug("Failed to program ebtables rules for secondary ip %s for vm %s with action %s" % (ip, vmname, action))

def check_default_network_rules(vm_name, vm_id, vm_ip, vm_ip6, vm_mac, vif, brname, sec_ips, is_first_nic=False):
    brfw = get_br_fw(brname)
    vmchain_default = '-'.join(vm_name.split('-')[:-1]) + "-def"
    try:
        rules = execute("iptables-save |grep -w %s |grep -w %s |grep -w %s" % (brfw, vif, vmchain_default))
    except:
        rules = None
    if not rules:
        logging.debug("iptables rules do not exist, programming default rules for %s %s" % (vm_name,vif))
        default_network_rules(vm_name, vm_id, vm_ip, vm_ip6, vm_mac, vif, brname, sec_ips, is_first_nic)
    else:
        vmchain_in = vm_name + "-in"
        try:
            rules = execute("ebtables -t nat -L PREROUTING | grep %s |grep -w %s" % (vmchain_in, vif))
        except:
            rules = None
        if not rules:
            logging.debug("ebtables rules do not exist, programming default ebtables rules for %s %s" % (vm_name,vif))
            default_ebtables_rules(vm_name, vm_ip, vm_mac, vif, is_first_nic)
            ips = sec_ips.split(';')
            ips.pop()
            ebtables_rules_vmip(vm_name, vm_mac, ips, "-I")
    return True

def default_network_rules(vm_name, vm_id, vm_ip, vm_ip6, vm_mac, vif, brname, sec_ips, is_first_nic=False):
    if not add_fw_framework(brname):
        return False

    vmName = vm_name
    brfw = get_br_fw(brname)
    domID = get_vm_id(vm_name)

    vmchain = iptables_chain_name(vm_name)
    vmchain_egress = egress_chain_name(vm_name)
    vmchain_default = '-'.join(vmchain.split('-')[:-1]) + "-def"
    ipv6_link_local = ipv6_link_local_addr(vm_mac)

    action = "-A"
    vmipsetName = ipset_chain_name(vm_name)
    vmipsetName6 = vmipsetName + '-6'

    if is_first_nic:
        delete_rules_for_vm_in_bridge_firewall_chain(vmName)
        destroy_ebtables_rules(vmName, vif)

        for chain in [vmchain, vmchain_egress, vmchain_default]:
            try:
                execute('iptables -N ' + chain)
            except:
                execute('iptables -F ' + chain)

            try:
                execute('ip6tables -N ' + chain)
            except:
                execute('ip6tables -F ' + chain)

        #create ipset and add vm ips to that ip set
        if not create_ipset_forvm(vmipsetName):
            logging.debug("failed to create ipset for rule %s", vmipsetName)
            return False

        if not create_ipset_forvm(vmipsetName6, family='inet6', type='hash:net'):
           logging.debug("failed to create ivp6 ipset for rule %s", vmipsetName6)
           return False

    #add primary nic ip to ipset
    if not add_to_ipset(vmipsetName, [vm_ip], action ):
        logging.debug("failed to add vm " + vm_ip + " ip to set ")
        return False

    #add secodnary nic ips to ipset
    secIpSet = "1"
    ips = sec_ips.split(';')
    ips.pop()

    if len(ips) == 0 or ips[0] == "0":
        secIpSet = "0"
        ip4s = []
        ip6s = []

    if secIpSet == "1":
        logging.debug("Adding ipset for secondary ipv4 addresses")
        ip4s, ip6s = split_ips_by_family(ips)

        add_to_ipset(vmipsetName, ip4s, action)

        if not write_secip_log_for_vm(vm_name, sec_ips, vm_id):
            logging.debug("Failed to log default network rules, ignoring")

    try:
        execute("iptables -A " + brfw + "-OUT" + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -j " + vmchain_default)
        execute("iptables -A " + brfw + "-IN" + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -j " + vmchain_default)
        execute("iptables -A " + vmchain_default + " -m state --state RELATED,ESTABLISHED -j ACCEPT")
        #allow dhcp
        execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -p udp --dport 67 --sport 68 -j ACCEPT")
        execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -p udp --dport 68 --sport 67  -j ACCEPT")
        execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -p udp --sport 67 -j DROP")

        #don't let vm spoof its ip address
        if vm_ip:
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -m set ! --match-set " + vmipsetName + " src -j DROP")
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -m set ! --match-set " + vmipsetName + " dst -j DROP")
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -m set --match-set " + vmipsetName + " src -p udp --dport 53  -j RETURN ")
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -m set --match-set " + vmipsetName + " src -p tcp --dport 53  -j RETURN ")
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -m set --match-set " + vmipsetName + " src -j " + vmchain_egress)

        execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -j " + vmchain)
        execute("iptables -A " + vmchain + " -j DROP")
    except:
        logging.debug("Failed to program default rules for vm " + vm_name)
        return False

    default_ebtables_rules(vmchain, vm_ip, vm_mac, vif, is_first_nic)
    #default ebtables rules for vm secondary ips
    ebtables_rules_vmip(vm_name, vm_mac, ip4s, "-I")

    if vm_ip and is_first_nic:
        if not write_rule_log_for_vm(vmName, vm_id, vm_ip, domID, '_initial_', '-1'):
            logging.debug("Failed to log default network rules, ignoring")

    vm_ip6_addr = [ipv6_link_local]
    try:
        ip6 = ipaddress.ip_address(vm_ip6)
        if ip6.version == 6:
            vm_ip6_addr.append(ip6)
    except (ipaddress.AddressValueError, ValueError):
        pass

    add_to_ipset(vmipsetName6, vm_ip6_addr, action)
    if secIpSet == "1":
        logging.debug("Adding ipset for secondary ipv6 addresses")
        add_to_ipset(vmipsetName6, ip6s, action)

    try:
        execute('ip6tables -A ' + brfw + '-OUT' + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' -j ' + vmchain_default)
        execute('ip6tables -A ' + brfw + '-IN' + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -j ' + vmchain_default)
        execute('ip6tables -A ' + vmchain_default + ' -m state --state RELATED,ESTABLISHED -j ACCEPT')

        # Allow Instances to receive Router Advertisements, send out solicitations, but block any outgoing Advertisement from a Instance
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' --src fe80::/64 --dst ff02::1 -p icmpv6 --icmpv6-type router-advertisement -m hl --hl-eq 255 -j ACCEPT')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' --dst ff02::2 -p icmpv6 --icmpv6-type router-solicitation -m hl --hl-eq 255 -j RETURN')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p icmpv6 --icmpv6-type router-advertisement -j DROP')

        # Allow neighbor solicitations and advertisements
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p icmpv6 --icmpv6-type neighbor-solicitation -m hl --hl-eq 255 -j RETURN')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' -p icmpv6 --icmpv6-type neighbor-solicitation -m hl --hl-eq 255 -j ACCEPT')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p icmpv6 --icmpv6-type neighbor-advertisement -m set --match-set ' + vmipsetName6 + ' src -m hl --hl-eq 255 -j RETURN')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' -p icmpv6 --icmpv6-type neighbor-advertisement -m hl --hl-eq 255 -j ACCEPT')

        # Packets to allow as per RFC4890
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p icmpv6 --icmpv6-type packet-too-big -m set --match-set ' + vmipsetName6 + ' src -j RETURN')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' -p icmpv6 --icmpv6-type packet-too-big -j ACCEPT')

        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p icmpv6 --icmpv6-type destination-unreachable -m set --match-set ' + vmipsetName6 + ' src -j RETURN')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' -p icmpv6 --icmpv6-type destination-unreachable -j ACCEPT')

        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p icmpv6 --icmpv6-type time-exceeded -m set --match-set ' + vmipsetName6 + ' src -j RETURN')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' -p icmpv6 --icmpv6-type time-exceeded -j ACCEPT')

        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p icmpv6 --icmpv6-type parameter-problem -m set --match-set ' + vmipsetName6 + ' src -j RETURN')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' -p icmpv6 --icmpv6-type parameter-problem -j ACCEPT')

        # MLDv2 discovery packets
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p icmpv6 --dst ff02::16 -j RETURN')

        # Allow Instances to send out DHCPv6 client messages, but block server messages
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p udp --sport 546 --dst ff02::1:2 --src ' + str(ipv6_link_local) + ' -j RETURN')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' -p udp --src fe80::/64 --dport 546 --dst ' + str(ipv6_link_local) + ' -j ACCEPT')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p udp --sport 547 ! --dst fe80::/64 -j DROP')

        # Always allow outbound DNS over UDP and TCP
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p udp --dport 53 -m set --match-set ' + vmipsetName6 + ' src -j RETURN')
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -p tcp --dport 53 -m set --match-set ' + vmipsetName6 + ' src -j RETURN')

        # Prevent source address spoofing
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -m set ! --match-set ' + vmipsetName6 + ' src -j DROP')

        # Send proper traffic to the egress chain of the Instance
        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-in ' + vif + ' -m set --match-set ' + vmipsetName6 + ' src -j ' + vmchain_egress)

        execute('ip6tables -A ' + vmchain_default + ' -m physdev --physdev-is-bridged --physdev-out ' + vif + ' -j ' + vmchain)

        # Drop all other traffic into the Instance
        execute('ip6tables -A ' + vmchain + ' -j DROP')
    except:
        logging.debug('Failed to program default rules for vm ' + vm_name)
        return False

    logging.debug("Programmed default rules for vm " + vm_name)
    return True


def post_default_network_rules(vm_name, vm_id, vm_ip, vm_mac, vif, brname, dhcpSvr, hostIp, hostMacAddr):
    vmchain_default = '-'.join(vm_name.split('-')[:-1]) + "-def"
    iptables_vmchain=iptables_chain_name(vm_name)
    vmchain_in = iptables_vmchain + "-in"
    vmchain_out = iptables_vmchain + "-out"
    domID = get_vm_id(vm_name)
    try:
        execute("iptables -I " + vmchain_default + " 4 -m physdev --physdev-is-bridged --physdev-in " + vif + " --source " + vm_ip + " -j ACCEPT")
    except:
        pass
    try:
        execute("iptables -t nat -A PREROUTING -p tcp -m physdev --physdev-in " + vif + " -m tcp --dport 80 -d " + dhcpSvr + " -j DNAT --to-destination " + hostIp + ":80")
    except:
        pass

    try:
        execute("ebtables -t nat -I " + vmchain_in + " -p IPv4 --ip-protocol tcp --ip-destination-port 80 --ip-dst " + dhcpSvr + " -j dnat --to-destination " + hostMacAddr)
    except:
        pass

    try:
        execute("ebtables -t nat -I " + vmchain_in + " 4 -p ARP --arp-ip-src ! " + vm_ip + " -j DROP")
    except:
        pass
    try:
        execute("ebtables -t nat -I " + vmchain_out + " 2 -p ARP --arp-ip-dst ! " + vm_ip + " -j DROP")
    except:
        pass
    if not write_rule_log_for_vm(vm_name, vm_id, vm_ip, domID, '_initial_', '-1'):
            logging.debug("Failed to log default network rules, ignoring")


def delete_rules_for_vm_in_bridge_firewall_chain(vmName):
    vm_name = vmName
    if vm_name.startswith('i-'):
        vm_name=iptables_chain_name(vm_name)
        vm_name = '-'.join(vm_name.split('-')[:-1]) + "-def"

    vmchain = iptables_chain_name(vm_name)

    delcmd = """iptables-save | awk '/BF(.*)physdev-is-bridged(.*)%s/ { sub(/-A/, "-D", $1) ; print }'""" % vmchain
    delcmds = [_f for _f in execute(delcmd).split('\n') if _f]
    for cmd in delcmds:
        try:
            execute("iptables " + cmd)
        except:
              logging.exception("Ignoring failure to delete rules for vm " + vmName)

    delcmd = """ip6tables-save | awk '/BF(.*)physdev-is-bridged(.*)%s/ { sub(/-A/, "-D", $1) ; print }'""" % vmchain
    delcmds = [_f for _f in execute(delcmd).split('\n') if _f]
    for cmd in delcmds:
        try:
            execute('ip6tables ' + cmd)
        except:
              logging.exception("Ignoring failure to delete rules for vm " + vmName)


def rewrite_rule_log_for_vm(vm_name, new_domid):
    logfilename = logpath + vm_name + ".log"
    if not os.path.exists(logfilename):
        return
    lines = (line.rstrip() for line in open(logfilename))

    [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = ['_', '-1', '_', '-1', '_', '-1']
    for line in lines:
        [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = line.split(',')
        break

    write_rule_log_for_vm(_vmName, _vmID, '0.0.0.0', new_domid, _signature, '-1')


def get_rule_log_for_vm(vmName):
    vm_name = vmName
    logfilename = logpath + vm_name + ".log"
    if not os.path.exists(logfilename):
        return ''

    lines = (line.rstrip() for line in open(logfilename))

    [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = ['_', '-1', '_', '-1', '_', '-1']
    for line in lines:
        [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = line.split(',')
        break

    return ','.join([_vmName, _vmID, _vmIP, _domID, _signature, _seqno])


def check_domid_changed(vmName):
    curr_domid = get_vm_id(vmName)
    if (curr_domid is None) or (not curr_domid.isdigit()):
        curr_domid = '-1'

    vm_name = vmName
    logfilename = logpath + vm_name + ".log"
    if not os.path.exists(logfilename):
        return ['-1', curr_domid]

    lines = (line.rstrip() for line in open(logfilename))

    [_vmName,_vmID,_vmIP,old_domid,_signature,_seqno] = ['_', '-1', '_', '-1', '_', '-1']
    for line in lines:
        [_vmName,_vmID,_vmIP,old_domid,_signature,_seqno] = line.split(',')
        break

    return [curr_domid, old_domid]


def network_rules_for_rebooted_vm(vmName):
    vm_name = vmName
    [curr_domid, old_domid] = check_domid_changed(vm_name)

    if curr_domid == old_domid:
        return True

    if old_domid == '-1':
        return True

    if curr_domid == '-1':
        return True

    logging.debug("Found a rebooted VM -- reprogramming rules for " + vm_name)

    delete_rules_for_vm_in_bridge_firewall_chain(vm_name)

    brName = execute("iptables-save | awk -F '-j ' '/FORWARD -o(.*)physdev-is-bridged(.*)BF/ {print $2}'").strip()
    if not brName:
        brName = "cloudbr0"
    else:
        brName = execute("iptables-save |grep physdev-is-bridged |grep FORWARD |grep BF |grep '\-o' |awk '{print $4}' | head -1").strip()

    if 1 in [ vm_name.startswith(c) for c in ['r-', 's-', 'v-'] ]:
        default_network_rules_systemvm(vm_name, brName)
        return True

    vmchain = iptables_chain_name(vm_name)
    vmchain_default = '-'.join(vmchain.split('-')[:-1]) + "-def"

    vifs = get_vifs(vmName)
    logging.debug(vifs, brName)
    for v in vifs:
        execute("iptables -A " + get_br_fw(brName) + "-IN " + " -m physdev --physdev-is-bridged --physdev-in " + v + " -j " + vmchain_default)
        execute("iptables -A " + get_br_fw(brName) + "-OUT " + " -m physdev --physdev-is-bridged --physdev-out " + v + " -j " + vmchain_default)
        execute("ip6tables -A " + get_br_fw(brName) + "-IN " + " -m physdev --physdev-is-bridged --physdev-in " + v + " -j " + vmchain_default)
        execute("ip6tables -A " + get_br_fw(brName) + "-OUT " + " -m physdev --physdev-is-bridged --physdev-out " + v + " -j " + vmchain_default)

    #change antispoof rule in vmchain
    try:
        delcmd = """iptables-save | awk '/-A %s(.*)physdev/ { sub(/-A/, "-D", $1) ; print }'""" % vmchain_default
        inscmd = """iptables-save | awk '/-A %s(.*)physdev/ { gsub(/vnet[0-9]+/, "%s") ; sub(/-A/, "-D", $1) ; print }'""" % ( vmchain_default,vifs[0] )
        ipts = []
        for cmd in [delcmd, inscmd]:
            logging.debug(cmd)
            cmds = [_f for _f in execute(cmd).split('\n') if _f]
            for c in cmds:
                    ipt = "iptables " + c
                    ipts.append(ipt)

        for ipt in ipts:
            try:
                execute(ipt)
            except:
                logging.debug("Failed to rewrite antispoofing rules for vm " + vm_name)
    except:
        logging.debug("No rules found for vm " + vm_name)

    rewrite_rule_log_for_vm(vm_name, curr_domid)
    return True


def get_rule_logs_for_vms():
    state = ['running']
    vms = virshlist(state)

    result = []
    try:
        for name in vms:
            name = name.rstrip()
            if 1 not in [name.startswith(c) for c in ['r-', 's-', 'v-', 'i-'] ]:
                continue
            # Move actions on rebooted vm to java code
            # network_rules_for_rebooted_vm(name)
            if name.startswith('i-'):
                log = get_rule_log_for_vm(name)
                result.append(log)
    except:
        logging.exception("Failed to get rule logs, better luck next time!")

    print(";".join(result))


def cleanup_rules_for_dead_vms():
    return True


def cleanup_bridge(bridge):
    bridge_name = get_br_fw(bridge)
    logging.debug("Cleaning old bridge chains: " + bridge_name)
    if not bridge_name:
        return True

    # Delete iptables/bridge rules
    rules = execute("""iptables-save | grep %s | grep '^-A' | sed 's/-A/-D/' """ % bridge_name).split("\n")
    for rule in [_f for _f in rules if _f]:
        try:
            command = "iptables " + rule
            execute(command)
        except: pass

    chains = [bridge_name, bridge_name+'-IN', bridge_name+'-OUT']
    # Flush old bridge chain
    for chain in chains:
        try:
            execute("iptables -F " + chain)
        except: pass
    # Remove brige chains
    for chain in chains:
        try:
            execute("iptables -X " + chain)
        except: pass
    return True


def cleanup_rules():
    try:
        states = ['running', 'paused']
        vmsInHost = virshlist(states)

        logging.debug(" Vms on the host : %s ", vmsInHost)

        cleanup = []
        chainscmd = """iptables-save | grep -P '^:(?!.*-(def|eg))' | awk '{sub(/^:/, "", $1) ; print $1}' | sort | uniq"""
        chains = execute(chainscmd).split('\n')

        logging.debug(" iptables chains in the host :%s ", chains)

        for chain in chains:
            if 1 in [ chain.startswith(c) for c in ['r-', 'i-', 's-', 'v-'] ]:
                vm_name = chain
                vmpresent = False

                for vm in vmsInHost:
                    if vm_name  in vm:
                        vmpresent = True
                        break

                if vmpresent is False:
                    logging.debug("vm " + vm_name + " is not running or paused, cleaning up iptables rules")
                    cleanup.append(vm_name)

        bridge_tables = execute("""grep -E '^ebtable_' /proc/modules | cut -f1 -d' ' | sed s/ebtable_//""").split('\n')
        for table in [_f for _f in bridge_tables if _f]:
            chainscmd = """ebtables -t %s -L | awk '/chain:/ { gsub(/(^.*chain: |-(in|out|ips).*)/, ""); print $1}' | sort | uniq""" % table
            chains = execute(chainscmd).split('\n')

        logging.debug(" ebtables chains in the host: %s ", chains)

        for chain in [_f for _f in chains if _f]:
            if 1 in [chain.startswith(c) for c in ['r-', 'i-', 's-', 'v-']]:
                vm_name = chain
                vmpresent = False
                for vm in vmsInHost:
                    if vm_name  in vm:
                        vmpresent = True
                        break

                if vmpresent is False:
                    logging.debug("vm " + vm_name + " is not running or paused, cleaning up ebtables rules")
                    cleanup.append(vm_name)

        cleanup = list(set(cleanup))  # remove duplicates
        for vmname in cleanup:
            destroy_network_rules_for_vm(vmname)

        logging.debug("Cleaned up rules for " + str(len(cleanup)) + " chains")
    except:
        logging.debug("Failed to cleanup rules !")


def check_rule_log_for_vm(vmName, vmId, vmIP, domID, signature, seqno):
    vm_name = vmName
    logfilename = logpath + vm_name + ".log"
    if not os.path.exists(logfilename):
        return [True, True, True, True, True, True]

    try:
        lines = (line.rstrip() for line in open(logfilename))
    except:
        logging.debug("failed to open " + logfilename)
        return [True, True, True, True, True, True]

    [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = ['_', '-1', '_', '-1', '_', '-1']
    try:
        for line in lines:
            [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = line.split(',')
            break
    except:
        logging.debug("Failed to parse log file for vm " + vm_name)
        remove_rule_log_for_vm(vm_name)
        return [True, True, True, True, True, True]

    return [(vm_name != _vmName), (vmId != _vmID), (vmIP != _vmIP), (domID != _domID), (signature != _signature),(seqno != _seqno)]


def write_rule_log_for_vm(vmName, vmID, vmIP, domID, signature, seqno):
    vm_name = vmName
    logfilename = logpath + vm_name + ".log"
    logging.debug("Writing log to " + logfilename)
    logf = open(logfilename, 'w')
    output = ','.join([vmName, vmID, vmIP, str(domID), signature, seqno])
    result = True
    try:
        logf.write(output)
        logf.write('\n')
    except:
        logging.debug("Failed to write to rule log file " + logfilename)
        result = False

    logf.close()

    return result


def remove_rule_log_for_vm(vmName):
    vm_name = vmName
    logfilename = logpath + vm_name + ".log"

    result = True
    try:
        os.remove(logfilename)
    except:
        logging.debug("Failed to delete rule log file " + logfilename)
        result = False

    return result


#ebtables chain max len 31 char
def ebtables_chain_name(vm_name):
    # 23 because there are appends to the chains
    if len(vm_name) > 22:
        vm_name = vm_name[0:22]
    return vm_name


#ipset chain max len 31 char
def ipset_chain_name(vm_name):
    if len(vm_name) > 30:
        vm_name = vm_name[0:30]
    return vm_name


#iptables chain max len 29 char and it is appended with other names like -eg,-def
def iptables_chain_name(vm_name):
    if len(vm_name) > 25:
        vm_name = vm_name[0:24]
    return vm_name


def egress_chain_name(vm_name):
    chain_name = iptables_chain_name(vm_name)
    return chain_name + "-eg"


def parse_network_rules(rules):
    ret = []

    if rules is None or len(rules) == 0:
        return ret

    lines = rules.split('NEXT;')[:-1]
    for line in lines:
        tokens = line.split(';', 3)
        if len(tokens) != 4:
            continue

        ruletype, protocol = tokens[0].split(':')
        start = int(tokens[1])
        end = int(tokens[2])
        cidrs = tokens.pop()

        ipv4 = []
        ipv6 = []
        for ip in cidrs.split(","):
            try:
                network = ipaddress.ip_network(ip, False)
                if network.version == 4:
                    ipv4.append(ip)
                else:
                    ipv6.append(ip)
            except:
                pass

        ret.append({'ipv4': ipv4, 'ipv6': ipv6, 'ruletype': ruletype,
                    'start': start, 'end': end, 'protocol': protocol})

    return ret


def add_network_rules(vm_name, vm_id, vm_ip, vm_ip6, signature, seqno, vmMac, rules, vif, brname, sec_ips):
    try:
        vmName = vm_name
        domId = get_vm_id(vmName)

        changes = check_rule_log_for_vm(vmName, vm_id, vm_ip, domId, signature, seqno)

        if not 1 in changes:
            logging.debug("Rules already programmed for vm " + vm_name)
            return True

        if rules == "" or rules == None:
            lines = []
        else:
            lines = rules.split(';')[:-1]

        logging.debug("programming network rules for IP: " + vm_ip + " vmname=%s", vm_name)

        egress_chain_name(vm_name)
        vmchain = iptables_chain_name(vm_name)
        egress_vmchain = egress_chain_name(vm_name)

        try:
            for chain in [vmchain, egress_vmchain]:
                execute('iptables -F ' + chain)
                execute('ip6tables -F ' + chain)
        except:
            logging.debug("Error flushing iptables rules for " + vm_name + ". Presuming firewall rules deleted, re-initializing." )
            default_network_rules(vm_name, vm_id, vm_ip, vm_ip6, vmMac, vif, brname, sec_ips, True)

        egressrule_v4 = 0
        egressrule_v6 = 0

        for rule in parse_network_rules(rules):
            start = rule['start']
            end = rule['end']
            protocol = rule['protocol']

            if rule['ruletype'] == 'E':
                vmchain = egress_vmchain
                direction = "-d"
                action = "RETURN"
                if rule['ipv4']:
                    egressrule_v4 =+ 1

                if rule['ipv6']:
                    egressrule_v6 +=1

            else:
                vmchain = vm_name
                action = "ACCEPT"
                direction = "-s"

            if start == 0 and end == 0:
                dport = ""
            else:
                dport = " --dport " + str(start) + ":" + str(end)

            if protocol != 'all' and protocol != 'icmp' and protocol != 'tcp' and protocol != 'udp':
                protocol_all = " -p " + protocol
                protocol_state = " "
            else:
                protocol_all = " -p " + protocol + " -m " + protocol
                protocol_state = " -m state --state NEW "

            if 'icmp' == protocol:
                range = str(start) + '/' + str(end)
                if start == -1:
                    range = 'any'

            for ip in rule['ipv4']:
                if protocol == 'all':
                    execute('iptables -I ' + vmchain + ' -m state --state NEW ' + direction + ' ' + ip + ' -j ' + action)
                elif protocol == 'icmp':
                    execute("iptables -I " + vmchain + " -p icmp --icmp-type " + range + " " + direction + " " + ip + " -j " + action)
                else:
                    execute("iptables -I " + vmchain + protocol_all + dport + protocol_state + direction + " " + ip + " -j "+ action)

            for ip in rule['ipv6']:
                if protocol == 'all':
                    execute('ip6tables -I ' + vmchain + ' -m state --state NEW ' + direction + ' ' + ip + ' -j ' + action)
                elif 'icmp' != protocol:
                    execute("ip6tables -I " + vmchain + protocol_all + dport + protocol_state + direction + " " + ip + " -j "+ action)
                else:
                    # ip6tables does not allow '--icmpv6-type any', allowing all ICMPv6 is done by not allowing a specific type
                    if range == 'any':
                        execute('ip6tables -I ' + vmchain + ' -p icmpv6 ' + direction + ' ' + ip + ' -j ' + action)
                    else:
                        execute('ip6tables -I ' + vmchain + ' -p icmpv6 --icmpv6-type ' + range + ' ' + direction + ' ' + ip + ' -j ' + action)

        egress_vmchain = egress_chain_name(vm_name)
        if egressrule_v4 == 0 :
            execute('iptables -A ' + egress_vmchain + ' -j RETURN')
        else:
            execute('iptables -A ' + egress_vmchain + ' -j DROP')

        if egressrule_v6 == 0 :
            execute('ip6tables -A ' + egress_vmchain + ' -j RETURN')
        else:
            execute('ip6tables -A ' + egress_vmchain + ' -j DROP')

        vmchain = iptables_chain_name(vm_name)

        execute('iptables -A ' + vmchain + ' -j DROP')
        execute('ip6tables -A ' + vmchain + ' -j DROP')

        if not write_rule_log_for_vm(vmName, vm_id, vm_ip, domId, signature, seqno):
            return False

        return True
    except:
        logging.exception("Failed to network rule !")


def get_vifs(vm_name):
    vifs = []
    xmlfile = virshdumpxml(vm_name)
    if not xmlfile:
        return vifs

    dom = xml.dom.minidom.parseString(xmlfile)
    for network in dom.getElementsByTagName("interface"):
        target = network.getElementsByTagName('target')[0]
        nicdev = target.getAttribute("dev").strip()
        vifs.append(nicdev)
    return vifs


def get_vifs_for_bridge(vm_name, brname):
    vifs = []
    xmlfile = virshdumpxml(vm_name)
    if not xmlfile:
        return vifs

    dom = xml.dom.minidom.parseString(xmlfile)
    for network in dom.getElementsByTagName("interface"):
        source = network.getElementsByTagName('source')[0]
        bridge = source.getAttribute("bridge").strip()
        if bridge == brname:
            target = network.getElementsByTagName('target')[0]
            nicdev = target.getAttribute("dev").strip()
            vifs.append(nicdev)
    return list(set(vifs))


def get_bridges(vm_name):
    bridges = []
    xmlfile = virshdumpxml(vm_name)
    if not xmlfile:
        return bridges

    dom = xml.dom.minidom.parseString(xmlfile)
    for network in dom.getElementsByTagName("interface"):
        for source in network.getElementsByTagName('source'):
            bridge = source.getAttribute("bridge").strip()
            bridges.append(bridge)
    return list(set(bridges))


def get_vm_id(vm_name):
    conn = get_libvirt_connection()
    try:
        dom = (conn.lookupByName (vm_name))
    except libvirt.libvirtError:
        return None

    conn.close()

    res = dom.ID()
    if isinstance(res, int):
        res = str(res)
    return res


def get_br_fw(brname):
    cmd = "iptables-save |grep physdev-is-bridged |grep FORWARD |grep BF |grep '\-o' | grep -w " + brname  + "|awk '{print $9}' | head -1"
    brfwname = execute(cmd).strip()
    if not brfwname:
        brfwname = "BF-" + brname
    return brfwname


def add_fw_framework(brname):
    try:
        execute("modprobe br_netfilter")
    except:
        logging.debug("failed to load kernel module br_netfilter")

    try:
        execute("sysctl -w net.bridge.bridge-nf-call-arptables=1")
        execute("sysctl -w net.bridge.bridge-nf-call-iptables=1")
        execute("sysctl -w net.bridge.bridge-nf-call-ip6tables=1")
    except:
        logging.warn("failed to turn on bridge netfilter")

    brfw = get_br_fw(brname)
    try:
        execute("iptables -L " + brfw)
    except:
        execute("iptables -N " + brfw)

    brfwout = brfw + "-OUT"
    try:
        execute("iptables -L " + brfwout)
    except:
        execute("iptables -N " + brfwout)

    brfwin = brfw + "-IN"
    try:
        execute("iptables -L " + brfwin)
    except:
        execute("iptables -N " + brfwin)

    try:
        execute('ip6tables -L ' + brfw)
    except:
        execute('ip6tables -N ' + brfw)

    brfwout = brfw + "-OUT"
    try:
        execute('ip6tables -L ' + brfwout)
    except:
        execute('ip6tables -N ' + brfwout)

    brfwin = brfw + "-IN"
    try:
        execute('ip6tables -L ' + brfwin)
    except:
        execute('ip6tables -N ' + brfwin)

    physdev = get_bridge_physdev(brname)

    try:
        refs = int(execute("""iptables -n -L %s | awk '/%s(.*)references/ {gsub(/\(/, "") ;print $3}'""" % (brfw,brfw)).strip())
        refs6 = int(execute("""ip6tables -n -L %s | awk '/%s(.*)references/ {gsub(/\(/, "") ;print $3}'""" % (brfw,brfw)).strip())

        if refs == 0:
            execute("iptables -I FORWARD -i " + brname + " -j DROP")
            execute("iptables -I FORWARD -o " + brname + " -j DROP")
            execute("iptables -I FORWARD -i " + brname + " -m physdev --physdev-is-bridged -j " + brfw)
            execute("iptables -I FORWARD -o " + brname + " -m physdev --physdev-is-bridged -j " + brfw)
            execute("iptables -A " + brfw + " -m state --state RELATED,ESTABLISHED -j ACCEPT")
            execute("iptables -A " + brfw + " -m physdev --physdev-is-bridged --physdev-is-in -j " + brfwin)
            execute("iptables -A " + brfw + " -m physdev --physdev-is-bridged --physdev-is-out -j " + brfwout)
            execute("iptables -A " + brfw + " -m physdev --physdev-is-bridged --physdev-out " + physdev + " -j ACCEPT")

        if refs6 == 0:
            execute('ip6tables -I FORWARD -i ' + brname + ' -j DROP')
            execute('ip6tables -I FORWARD -o ' + brname + ' -j DROP')
            execute('ip6tables -I FORWARD -i ' + brname + ' -m physdev --physdev-is-bridged -j ' + brfw)
            execute('ip6tables -I FORWARD -o ' + brname + ' -m physdev --physdev-is-bridged -j ' + brfw)
            execute('ip6tables -A ' + brfw + ' -m state --state RELATED,ESTABLISHED -j ACCEPT')
            execute('ip6tables -A ' + brfw + ' -m physdev --physdev-is-bridged --physdev-is-in -j ' + brfwin)
            execute('ip6tables -A ' + brfw + ' -m physdev --physdev-is-bridged --physdev-is-out -j ' + brfwout)
            execute('ip6tables -A ' + brfw + ' -m physdev --physdev-is-bridged --physdev-out ' + physdev + ' -j ACCEPT')

        return True
    except:
        try:
            execute("iptables -F " + brfw)
            execute('ip6tables -F ' + brfw)
        except:
            return False
        return False

def verify_network_rules(vm_name, vm_id, vm_ip, vm_ip6, vm_mac, vif, brname, sec_ips):
    if vm_name is None or vm_ip is None or vm_mac is None:
        print("vm_name, vm_ip and vm_mac must be specifed")
        sys.exit(1)

    if vm_id is None:
        vm_id = vm_name.split("-")[-2]

    if brname is None:
        brname = execute("virsh domiflist %s |grep -w '%s' |tr -s ' '|cut -d ' ' -f3" % (vm_name, vm_mac)).strip()
    if not brname:
        print("Cannot find bridge")
        sys.exit(1)

    if vif is None:
        vif = execute("virsh domiflist %s |grep -w '%s' |tr -s ' '|cut -d ' ' -f1" % (vm_name, vm_mac)).strip()
    if not vif:
        print("Cannot find vif")
        sys.exit(1)

    #vm_name = "i-2-55-VM"
    #vm_id = 55
    #vm_ip = "10.11.118.128"
    #vm_ip6 = "fe80::1c00:b4ff:fe00:5"
    #vm_mac = "1e:00:b4:00:00:05"
    #vif = "vnet11"
    #brname = "cloudbr0"
    #sec_ips = "10.11.118.133;10.11.118.135;10.11.118.138;" # end with ";" and seperated by ";"

    vm_ips = []
    if sec_ips is not None:
        vm_ips = sec_ips.split(';')
        vm_ips.pop()
        vm_ips.reverse()
    vm_ips.append(vm_ip)

    if not verify_ipset_for_vm(vm_name, vm_id, vm_ips, vm_ip6):
        sys.exit(2)
    if not verify_iptables_rules_for_bridge(brname):
        sys.exit(3)
    if not verify_default_iptables_rules_for_vm(vm_name, vm_id, vm_ips, vm_ip6, vm_mac, vif, brname):
        sys.exit(4)
    if not verify_ebtables_rules_for_vm(vm_name, vm_id, vm_ips, vm_ip6, vm_mac, vif, brname):
        sys.exit(5)

    sys.exit(0)

def verify_ipset_for_vm(vm_name, vm_id, vm_ips, vm_ip6):
    vmipsetName = ipset_chain_name(vm_name)
    vmipsetName6 = vmipsetName + '-6'

    rules = []
    for rule in execute("ipset list %s" % vmipsetName).split('\n'):
        rules.append(rule)

    # Check if all vm ips and ip6 exist
    for vm_ip in vm_ips:
        found = False
        for rule in rules:
            if rule == vm_ip:
                found = True
                break
        if not found:
            print("vm ip %s is not found" % vm_ip)
            return False

    rules = []
    for rule in execute("ipset list %s" % vmipsetName6).split('\n'):
        rules.append(rule)

    if vm_ip6 is not None:
        found = False
        for rule in rules:
            if rule == vm_ip6:
                found = True
                break
        if not found:
            print("vm ipv6 %s is not found" % vm_ip6)
            return False

    return True

def verify_iptables_rules_for_bridge(brname):
    brfw = get_br_fw(brname)
    brfwin = brfw + "-IN"
    brfwout = brfw + "-OUT"

    expected_rules = []
    expected_rules.append("-A FORWARD -o %s -m physdev --physdev-is-bridged -j %s" % (brname, brfw))
    expected_rules.append("-A FORWARD -i %s -m physdev --physdev-is-bridged -j %s" % (brname, brfw))
    expected_rules.append("-A %s -m state --state RELATED,ESTABLISHED -j ACCEPT" % (brfw))
    expected_rules.append("-A %s -m physdev --physdev-is-in --physdev-is-bridged -j %s" % (brfw, brfwin))
    expected_rules.append("-A %s -m physdev --physdev-is-out --physdev-is-bridged -j %s" % (brfw, brfwout))
    phydev = execute("ip link show type bridge | awk '/^%s[ \t]/ {print $4}'" % brname ).strip()
    expected_rules.append("-A %s -m physdev --physdev-out %s --physdev-is-bridged -j ACCEPT" % (brfw, phydev))

    rules = execute("iptables-save |grep -w %s |grep -v \"^:\"" % brfw).split('\n')

    return verify_expected_rules_exist(expected_rules, rules)

def verify_default_iptables_rules_for_vm(vm_name, vm_id, vm_ips, vm_ip6, vm_mac, vif, brname):
    brfw = get_br_fw(brname)
    brfwin = brfw + "-IN"
    brfwout = brfw + "-OUT"
    vmchain = iptables_chain_name(vm_name)
    vmchain_egress = egress_chain_name(vm_name)
    vm_def = '-'.join(vm_name.split('-')[:-1]) + "-def"

    expected_rules = []
    expected_rules.append("-A %s -m physdev --physdev-in %s --physdev-is-bridged -j %s" % (brfwin, vif, vm_def))
    expected_rules.append("-A %s -m physdev --physdev-out %s --physdev-is-bridged -j %s" % (brfwout, vif, vm_def))
    expected_rules.append("-A %s -m state --state RELATED,ESTABLISHED -j ACCEPT" % (vm_def))
    expected_rules.append("-A %s -p udp -m physdev --physdev-in %s --physdev-is-bridged -m udp --sport 68 --dport 67 -j ACCEPT" % (vm_def, vif))
    expected_rules.append("-A %s -p udp -m physdev --physdev-out %s --physdev-is-bridged -m udp --sport 67 --dport 68 -j ACCEPT" % (vm_def, vif))
    expected_rules.append("-A %s -p udp -m physdev --physdev-in %s --physdev-is-bridged -m udp --sport 67 -j DROP" % (vm_def, vif))
    expected_rules.append("-A %s -m physdev --physdev-in %s --physdev-is-bridged -m set ! --match-set %s src -j DROP" % (vm_def, vif, vm_name))
    expected_rules.append("-A %s -m physdev --physdev-out %s --physdev-is-bridged -m set ! --match-set %s dst -j DROP" % (vm_def, vif, vm_name))
    expected_rules.append("-A %s -p udp -m physdev --physdev-in %s --physdev-is-bridged -m set --match-set %s src -m udp --dport 53 -j RETURN" % (vm_def, vif, vm_name))
    expected_rules.append("-A %s -p tcp -m physdev --physdev-in %s --physdev-is-bridged -m set --match-set %s src -m tcp --dport 53 -j RETURN" % (vm_def, vif, vm_name))
    expected_rules.append("-A %s -m physdev --physdev-in %s --physdev-is-bridged -m set --match-set %s src -j %s" % (vm_def, vif, vm_name, vmchain_egress))
    expected_rules.append("-A %s -m physdev --physdev-out %s --physdev-is-bridged -j %s" % (vm_def, vif, vmchain))

    rules = execute("iptables-save |grep -E \"%s|%s\" |grep -v \"^:\"" % (vm_name, vm_def)).split('\n')

    return verify_expected_rules_in_order(expected_rules, rules)

def verify_ebtables_rules_for_vm(vm_name, vm_id, vm_ips, vm_ip6, vm_mac, vif, brname):
    vmchain_in = vm_name + "-in"
    vmchain_out = vm_name + "-out"
    vmchain_in_ips = vm_name + "-in-ips"
    vmchain_out_ips = vm_name + "-out-ips"
    vmchain_in_src = vm_name + "-in-src"
    vmchain_out_dst = vm_name + "-out-dst"

    new_mac = trim_mac_address(vm_mac)

    # PREROUTING/POSTROUTING
    expected_rules = []
    expected_rules.append("-A PREROUTING -i %s -j %s" % (vif, vmchain_in))
    expected_rules.append("-A POSTROUTING -o %s -j %s" % (vif, vmchain_out))
    rules = execute("ebtables-save |grep -E \"PREROUTING|POSTROUTING\" | grep %s" % vm_name).split('\n')
    if not verify_expected_rules_exist(expected_rules, rules):
        return False

    rules = execute("ebtables-save | grep %s" % vm_name).split('\n')

    # vmchain_in
    expected_rules = []
    expected_rules.append("-A %s -j %s" % (vmchain_in, vmchain_in_src))
    expected_rules.append("-A %s -p ARP -j %s" % (vmchain_in, vmchain_in_ips))
    expected_rules.append("-A %s -p ARP --arp-op Request -j ACCEPT" % (vmchain_in))
    expected_rules.append("-A %s -p ARP --arp-op Reply -j ACCEPT" % (vmchain_in))
    expected_rules.append("-A %s -p ARP -j DROP" % (vmchain_in))
    if not verify_expected_rules_in_order(expected_rules, rules):
        return False

    # vmchain_in_src
    expected_rules = []
    expected_rules.append("-A %s -s %s -j RETURN" % (vmchain_in_src, new_mac))
    expected_rules.append("-A %s -j DROP" % (vmchain_in_src))
    if not verify_expected_rules_in_order(expected_rules, rules):
        return False

    # vmchain_in_ips
    expected_rules = []
    for vm_ip in vm_ips:
        expected_rules.append("-A %s -p ARP -s %s --arp-ip-src %s --arp-mac-src %s -j RETURN" % (vmchain_in_ips, new_mac, vm_ip, new_mac))
    expected_rules.append("-A %s -j DROP" % (vmchain_in_ips))
    if not verify_expected_rules_in_order(expected_rules, rules):
        return False

    # vmchain_out
    expected_rules = []
    expected_rules.append("-A %s -p ARP --arp-op Reply -j %s" % (vmchain_out, vmchain_out_dst))
    expected_rules.append("-A %s -p ARP -j %s" % (vmchain_out, vmchain_out_ips))
    expected_rules.append("-A %s -p ARP --arp-op Request -j ACCEPT" % (vmchain_out))
    expected_rules.append("-A %s -p ARP --arp-op Reply -j ACCEPT" % (vmchain_out))
    expected_rules.append("-A %s -p ARP -j DROP" % (vmchain_out))
    if not verify_expected_rules_in_order(expected_rules, rules):
        return False

    # vmchain_out_dst
    expected_rules = []
    expected_rules.append("-A %s -p ARP --arp-op Reply --arp-mac-dst %s -j RETURN" % (vmchain_out_dst, new_mac))
    expected_rules.append("-A %s -p ARP --arp-op Reply -j DROP" % (vmchain_out_dst))
    if not verify_expected_rules_in_order(expected_rules, rules):
        return False

    # vmchain_out_ips
    expected_rules = []
    for vm_ip in vm_ips:
        expected_rules.append("-A %s -p ARP --arp-ip-dst %s -j RETURN" % (vmchain_out_ips, vm_ip))
    expected_rules.append("-A %s -j DROP" % (vmchain_out_ips))
    if not verify_expected_rules_in_order(expected_rules, rules):
        return False

    return True

def trim_mac_address(vm_mac):
    new_mac = ""
    for mac in vm_mac.split(":"):
        if mac.startswith("0"):
            new_mac += ":" + mac[1:]
        else:
            new_mac += ":" + mac
    return new_mac[1:]

def verify_expected_rules_exist(expected_rules, rules):
    # Check if expected rules exist
    for expected_rule in expected_rules:
        found = False
        for rule in rules:
            if rule == expected_rule:
                found = True
                break
        if not found:
            print("Rule '%s' is not found" % expected_rule)
            return False
    return True

def verify_expected_rules_in_order(expected_rules, rules):
    # Check if expected rules exist in order (!!!)
    i = 0
    for rule in rules:
        if i < len(expected_rules) and rule == expected_rules[i]:
            i += 1
    if i != len(expected_rules):
        print("Cannot find rule '%s'" % expected_rules[i])
        return False
    return True

if __name__ == '__main__':
    logging.basicConfig(filename="/var/log/cloudstack/agent/security_group.log", format="%(asctime)s - %(message)s", level=logging.DEBUG)
    parser = argparse.ArgumentParser(description='Apache CloudStack Security Groups')
    parser.add_argument("command")
    parser.add_argument("--vmname", dest="vmName")
    parser.add_argument("--vmip", dest="vmIP")
    parser.add_argument("--vmip6", dest="vmIP6")
    parser.add_argument("--vmid", dest="vmID")
    parser.add_argument("--vmmac", dest="vmMAC")
    parser.add_argument("--vif", dest="vif")
    parser.add_argument("--sig", dest="sig")
    parser.add_argument("--seq", dest="seq")
    parser.add_argument("--rules", dest="rules")
    parser.add_argument("--brname", dest="brname")
    parser.add_argument("--localbrname", dest="localbrname")
    parser.add_argument("--dhcpSvr", dest="dhcpSvr")
    parser.add_argument("--hostIp", dest="hostIp")
    parser.add_argument("--hostMacAddr", dest="hostMacAddr")
    parser.add_argument("--nicsecips", dest="nicSecIps")
    parser.add_argument("--action", dest="action")
    parser.add_argument("--privnic", dest="privnic")
    parser.add_argument("--isFirstNic", action="store_true", dest="isFirstNic")
    parser.add_argument("--check", action="store_true", dest="check")
    args = parser.parse_args()
    cmd = args.command
    logging.debug("Executing command: %s", cmd)

    for i in range(0, 30):
        if obtain_file_lock(lock_file) is False:
            logging.warning("Lock on %s is being held by other process. Waiting for release." % lock_file)
            time.sleep(0.5)
        else:
            break

    if cmd == "can_bridge_firewall":
        can_bridge_firewall(args.privnic)
    elif cmd == "default_network_rules" and args.check:
        check_default_network_rules(args.vmName, args.vmID, args.vmIP, args.vmIP6, args.vmMAC, args.vif, args.brname, args.nicSecIps, args.isFirstNic)
    elif cmd == "default_network_rules" and not args.check:
        default_network_rules(args.vmName, args.vmID, args.vmIP, args.vmIP6, args.vmMAC, args.vif, args.brname, args.nicSecIps, args.isFirstNic)
    elif cmd == "destroy_network_rules_for_vm":
        if args.vmIP is None:
            destroy_network_rules_for_vm(args.vmName, args.vif)
        else:
            destroy_network_rules_for_nic(args.vmName, args.vmIP, args.vmMAC, args.vif, args.nicSecIps)
    elif cmd == "default_network_rules_systemvm":
        default_network_rules_systemvm(args.vmName, args.localbrname)
    elif cmd == "get_rule_logs_for_vms":
        get_rule_logs_for_vms()
    elif cmd == "add_network_rules":
        add_network_rules(args.vmName, args.vmID, args.vmIP, args.vmIP6, args.sig, args.seq, args.vmMAC, args.rules, args.vif, args.brname, args.nicSecIps)
    elif cmd == "network_rules_vmSecondaryIp":
        network_rules_vmSecondaryIp(args.vmName, args.vmMAC, args.nicSecIps, args.action)
    elif cmd == "cleanup_rules":
        cleanup_rules()
    elif cmd == "post_default_network_rules":
        post_default_network_rules(args.vmName, args.vmID, args.vmIP, args.vmMAC, args.vif, args.brname, args.dhcpSvr, args.hostIp, args.hostMacAddr)
    elif cmd == "verify_network_rules":
        verify_network_rules(args.vmName, args.vmID, args.vmIP, args.vmIP6, args.vmMAC, args.vif, args.brname, args.nicSecIps)
    else:
        logging.debug("Unknown command: " + cmd)
        sys.exit(1)
