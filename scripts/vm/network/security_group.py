#!/usr/bin/python
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

import cloud_utils
from cloud_utils import Command
from cloudutils.configFileOps import configFileOps
import logging
import sys
import os
import xml.dom.minidom
from optparse import OptionParser, OptionGroup, OptParseError, BadOptionError, OptionError, OptionConflictError, OptionValueError
import re
import libvirt

logpath = "/var/run/cloud/"        # FIXME: Logs should reside in /var/log/cloud
iptables = Command("iptables")
bash = Command("/bin/bash")
ebtables = Command("ebtables")
driver = "qemu:///system"
cfo = configFileOps("/etc/cloudstack/agent/agent.properties")
hyper = cfo.getEntry("hypervisor.type")
if hyper == "lxc":
    driver = "lxc:///"
def execute(cmd):
    logging.debug(cmd)
    return bash("-c", cmd).stdout

def can_bridge_firewall(privnic):
    try:
        execute("which iptables")
    except:
        print "no iptables on your host machine"
        sys.exit(1)

    try:
        execute("which ebtables")
    except:
        print "no ebtables on your host machine"
        sys.exit(2)


    if not os.path.exists(logpath):
        os.makedirs(logpath)

    cleanup_rules_for_dead_vms()
    cleanup_rules()

    return True
'''
def ipset(ipsetname, proto, start, end, ips):
    try:
        check_call(['ipset', '-N', ipsetname, 'iptreemap'])
    except:
        logging.debug("ipset chain already exists" + ipsetname)

    result = True
    ipsettmp = ''.join(''.join(ipsetname.split('-')).split('_')) + str(int(time.time()) % 1000)

    try:
        check_call(['ipset', '-N', ipsettmp, 'iptreemap'])
        for ip in ips:
            try:
                check_call(['ipset', '-A', ipsettmp, ip])
            except CommandException, cex:
                if cex.reason.rfind('already in set') == -1:
                   raise
        check_call(['ipset', '-W', ipsettmp, ipsetname])
        check_call(['ipset', '-X', ipsettmp])
    except:
        logging.debug("Failed to program ipset " + ipsetname)
        result = False

    return result
'''
def virshlist(*states):

    libvirt_states={ 'running'  : libvirt.VIR_DOMAIN_RUNNING,
                     'shutoff'  : libvirt.VIR_DOMAIN_SHUTOFF,
                     'shutdown' : libvirt.VIR_DOMAIN_SHUTDOWN,
                     'paused'   : libvirt.VIR_DOMAIN_PAUSED,
                     'nostate'  : libvirt.VIR_DOMAIN_NOSTATE,
                     'blocked'  : libvirt.VIR_DOMAIN_BLOCKED,
                     'crashed'  : libvirt.VIR_DOMAIN_CRASHED,
    }

    searchstates = list(libvirt_states[state] for state in states)

    conn = libvirt.openReadOnly(driver)
    if conn == None:
       print 'Failed to open connection to the hypervisor'
       sys.exit(3)

    alldomains = map(conn.lookupByID, conn.listDomainsID())
    alldomains += map(conn.lookupByName, conn.listDefinedDomains())

    domains = []
    for domain in alldomains:
        if domain.info()[0] in searchstates:
            domains.append(domain.name())

    conn.close()

    return domains

def virshdomstate(domain):

    libvirt_states={ libvirt.VIR_DOMAIN_RUNNING  : 'running',
                     libvirt.VIR_DOMAIN_SHUTOFF  : 'shut off',
                     libvirt.VIR_DOMAIN_SHUTDOWN : 'shut down',
                     libvirt.VIR_DOMAIN_PAUSED   : 'paused',
                     libvirt.VIR_DOMAIN_NOSTATE  : 'no state',
                     libvirt.VIR_DOMAIN_BLOCKED  : 'blocked',
                     libvirt.VIR_DOMAIN_CRASHED  : 'crashed',
    }

    conn = libvirt.openReadOnly(driver)
    if conn == None:
       print 'Failed to open connection to the hypervisor'
       sys.exit(3)

    try:
        dom = (conn.lookupByName (domain))
    except libvirt.libvirtError:
        return None

    state = libvirt_states[dom.info()[0]]
    conn.close()

    return state

def virshdumpxml(domain):

    conn = libvirt.openReadOnly(driver)
    if conn == None:
       print 'Failed to open connection to the hypervisor'
       sys.exit(3)

    try:
        dom = (conn.lookupByName (domain))
    except libvirt.libvirtError:
        return None

    xml = dom.XMLDesc(0)
    conn.close()

    return xml

def destroy_network_rules_for_vm(vm_name, vif=None):
    vmchain = vm_name
    vmchain_egress = egress_chain_name(vm_name)
    vmchain_default = None

    delete_rules_for_vm_in_bridge_firewall_chain(vm_name)
    if vm_name.startswith('i-'):
        vmchain_default = '-'.join(vm_name.split('-')[:-1]) + "-def"

    destroy_ebtables_rules(vmchain, vif)

    chains = [vmchain_default, vmchain, vmchain_egress]
    for chain in filter(None, chains):
        try:
            execute("iptables -F " + chain)
        except:
            logging.debug("Ignoring failure to flush chain: " + chain)

    for chain in filter(None, chains):
        try:
            execute("iptables -X " + chain)
        except:
            logging.debug("Ignoring failure to delete chain: " + chain)

    try:
        execute("ipset -F " + vm_name)
        execute("ipset -X " + vm_name)
    except:
        logging.debug("Ignoring failure to delete ipset " + vmchain)

    if vif is not None:
        try:
            dnats = execute("""iptables -t nat -S | awk '/%s/ { sub(/-A/, "-D", $1) ; print }'""" % vif ).split("\n")
            for dnat in filter(None, dnats):
                try:
                    execute("iptables -t nat " + dnat)
                except:
                    logging.debug("Ignoring failure to delete dnat: " + dnat)
        except:
            pass
    remove_rule_log_for_vm(vm_name)
    remove_secip_log_for_vm(vm_name)

    if 1 in [ vm_name.startswith(c) for c in ['r-', 's-', 'v-'] ]:
        return 'true'

    return 'true'

def destroy_ebtables_rules(vm_name, vif):
    delcmd = "ebtables -t nat -L PREROUTING | grep " + vm_name
    delcmds = []
    try:
        delcmds = filter(None, execute(delcmd).split('\n'))
        delcmds = ["-D PREROUTING " + x for x in delcmds ]
    except:
        pass
    postcmds = []
    try:
        postcmd = "ebtables -t nat -L POSTROUTING | grep " + vm_name
        postcmds = filter(None, execute(postcmd).split('\n'))
        postcmds = ["-D POSTROUTING " + x for x in postcmds]
    except:
        pass

    delcmds += postcmds

    for cmd in delcmds:
        try:
            execute("ebtables -t nat " + cmd)
        except:
            logging.debug("Ignoring failure to delete ebtables rules for vm " + vm_name)
    chains = [vm_name+"-in", vm_name+"-out", vm_name+"-in-ips", vm_name+"-out-ips"]
    for chain in chains:
        try:
            execute("ebtables -t nat -F " + chain)
            execute("ebtables -t nat -X " + chain)
        except:
            logging.debug("Ignoring failure to delete ebtables chain for vm " + vm_name)

def default_ebtables_rules(vm_name, vm_ip, vm_mac, vif):
    vmchain_in = vm_name + "-in"
    vmchain_out = vm_name + "-out"
    vmchain_in_ips = vm_name + "-in-ips"
    vmchain_out_ips = vm_name + "-out-ips"

    for chain in [vmchain_in, vmchain_out, vmchain_in_ips, vmchain_out_ips]:
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
    except:
        logging.debug("Failed to program default rules")
        return 'false'

    try:
        execute("ebtables -t nat -A " + vmchain_in + " -s ! " + vm_mac + " -j DROP")
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP -s ! " + vm_mac + " -j DROP")
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP --arp-mac-src ! " + vm_mac + " -j DROP")
        if vm_ip is not None:
            execute("ebtables -t nat -A " + vmchain_in + " -p ARP -j " + vmchain_in_ips)
            execute("ebtables -t nat -I " + vmchain_in_ips + " -p ARP --arp-ip-src " + vm_ip + " -j RETURN")
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP --arp-op Request -j ACCEPT")
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP --arp-op Reply -j ACCEPT")
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP -j DROP")
    except:
        logging.exception("Failed to program default ebtables IN rules")
        return 'false'

    try:
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-op Reply --arp-mac-dst ! " + vm_mac + " -j DROP")
        if vm_ip is not None:
            execute("ebtables -t nat -A " + vmchain_out + " -p ARP -j " + vmchain_out_ips )
            execute("ebtables -t nat -I " + vmchain_out_ips + " -p ARP --arp-ip-dst " + vm_ip + " -j RETURN")
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-op Request -j ACCEPT")
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-op Reply -j ACCEPT")
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP -j DROP")
    except:
        logging.debug("Failed to program default ebtables OUT rules")
        return 'false'


def default_network_rules_systemvm(vm_name, localbrname):
    bridges = getBridges(vm_name)
    domid = getvmId(vm_name)
    vmchain = vm_name

    delete_rules_for_vm_in_bridge_firewall_chain(vm_name)

    try:
        execute("iptables -N " + vmchain)
    except:
        execute("iptables -F " + vmchain)

    for bridge in bridges:
        if bridge != localbrname:
            if not addFWFramework(bridge):
                return False 
            brfw = getBrfw(bridge)
            vifs = getVifsForBridge(vm_name, bridge)
            for vif in vifs:
                try:
                    execute("iptables -A " + brfw + "-OUT" + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -j " + vmchain)
                    execute("iptables -A " + brfw + "-IN" + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -j " + vmchain)
                    execute("iptables -A " + vmchain + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -j RETURN")
                except:
                    logging.debug("Failed to program default rules")
                    return 'false'

    execute("iptables -A " + vmchain + " -j ACCEPT")

    if write_rule_log_for_vm(vm_name, '-1', '_ignore_', domid, '_initial_', '-1') == False:
        logging.debug("Failed to log default network rules for systemvm, ignoring")
    return 'true'

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

def create_ipset_forvm (ipsetname):
    result = True
    try:
        logging.debug("Creating ipset chain .... " + ipsetname)
        execute("ipset -F " + ipsetname)
        execute("ipset -X " + ipsetname)
        execute("ipset -N " + ipsetname + " iphash")
    except:
        logging.debug("ipset chain not exists creating.... " + ipsetname)
        execute("ipset -N " + ipsetname + " iphash")

    return result

def add_to_ipset(ipsetname, ips, action):
    result = True
    for ip in ips:
        try:
            logging.debug("vm ip " + ip)
            execute("ipset " + action + " " + ipsetname + " " + ip)
        except:
            logging.debug("vm ip already in ip set " + ip)
            continue

    return result

def network_rules_vmSecondaryIp(vm_name, ip_secondary, action):
    logging.debug("vmName = "+ vm_name)
    logging.debug("action = "+ action)

    domid = getvmId(vm_name)

    vmchain = vm_name
    add_to_ipset(vmchain, [ip_secondary], action)

    #add ebtables rules for the secondary ip
    ebtables_rules_vmip(vm_name, [ip_secondary], action)

    return 'true'

def ebtables_rules_vmip (vmname, ips, action):
    vmchain_inips = vmname + "-in-ips"
    vmchain_outips = vmname + "-out-ips"

    if action and action.strip() == "-A":
        action = "-I"

    for ip in filter(None, ips):
        logging.debug("ip = " + ip)
        if ip == 0 or ip == "0":
            continue
        try:
            execute("ebtables -t nat " + action + " " + vmchain_inips + " -p ARP --arp-ip-src " + ip + " -j RETURN")
            execute("ebtables -t nat " + action + " " + vmchain_outips + " -p ARP --arp-ip-dst " + ip + " -j RETURN")
        except:
            logging.debug("Failed to program ebtables rules for secondary ip %s for vm %s with action %s" % (ip, vmname, action))

def default_network_rules(vm_name, vm_id, vm_ip, vm_mac, vif, brname, sec_ips):
    if not addFWFramework(brname):
        return False

    vmName = vm_name
    brfw = getBrfw(brname)
    domID = getvmId(vm_name)
    delete_rules_for_vm_in_bridge_firewall_chain(vmName)
    vmchain = vm_name
    vmchain_egress = egress_chain_name(vm_name)
    vmchain_default = '-'.join(vmchain.split('-')[:-1]) + "-def"

    destroy_ebtables_rules(vmName, vif)

    try:
        execute("iptables -N " + vmchain)
    except:
        execute("iptables -F " + vmchain)

    try:
        execute("iptables -N " + vmchain_egress)
    except:
        execute("iptables -F " + vmchain_egress)

    try:
        execute("iptables -N " + vmchain_default)
    except:
        execute("iptables -F " + vmchain_default)

    action = "-A"
    vmipsetName = vm_name
    #create ipset and add vm ips to that ip set
    if create_ipset_forvm(vmipsetName) == False:
       logging.debug(" failed to create ipset for rule " + str(tokens))
       return 'false'

    #add primary nic ip to ipset
    if add_to_ipset(vmipsetName, [vm_ip], action ) == False:
       logging.debug(" failed to add vm " + vm_ip + " ip to set ")
       return 'false'

    #add secodnary nic ips to ipset
    secIpSet = "1"
    ips = sec_ips.split(':')
    ips.pop()
    if ips[0] == "0":
        secIpSet = "0";

    if secIpSet == "1":
        logging.debug("Adding ipset for secondary ips")
        add_to_ipset(vmipsetName, ips, action)
        if write_secip_log_for_vm(vm_name, sec_ips, vm_id) == False:
            logging.debug("Failed to log default network rules, ignoring")

    try:
        execute("iptables -A " + brfw + "-OUT" + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -j " + vmchain_default)
        execute("iptables -A " + brfw + "-IN" + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -j " + vmchain_default)
        execute("iptables -A " + vmchain_default + " -m state --state RELATED,ESTABLISHED -j ACCEPT")
        #allow dhcp
        execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -p udp --dport 67 --sport 68 -j ACCEPT")
        execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -p udp --dport 68 --sport 67  -j ACCEPT")

        #don't let vm spoof its ip address
        if vm_ip is not None:
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -m set ! --set " + vmipsetName + " src -j DROP")
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -m set --set " + vmipsetName + " src -p udp --dport 53  -j RETURN ")
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -m set --set " + vmipsetName + " src -j " + vmchain_egress)
        execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -j " + vmchain)
        execute("iptables -A " + vmchain + " -j DROP")
    except:
        logging.debug("Failed to program default rules for vm " + vm_name)
        return 'false'

    default_ebtables_rules(vmchain, vm_ip, vm_mac, vif)
    #default ebtables rules for vm secondary ips
    ebtables_rules_vmip(vm_name, ips, "-I")

    if vm_ip is not None:
        if write_rule_log_for_vm(vmName, vm_id, vm_ip, domID, '_initial_', '-1') == False:
            logging.debug("Failed to log default network rules, ignoring")

    logging.debug("Programmed default rules for vm " + vm_name)
    return 'true'

def post_default_network_rules(vm_name, vm_id, vm_ip, vm_mac, vif, brname, dhcpSvr, hostIp, hostMacAddr):
    vmchain_default = '-'.join(vm_name.split('-')[:-1]) + "-def"
    vmchain_in = vm_name + "-in"
    vmchain_out = vm_name + "-out"
    domID = getvmId(vm_name)
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
    if write_rule_log_for_vm(vm_name, vm_id, vm_ip, domID, '_initial_', '-1') == False:
            logging.debug("Failed to log default network rules, ignoring")
def delete_rules_for_vm_in_bridge_firewall_chain(vmName):
    vm_name = vmName
    if vm_name.startswith('i-'):
        vm_name = '-'.join(vm_name.split('-')[:-1]) + "-def"

    vmchain = vm_name

    delcmd = """iptables-save | awk '/BF(.*)physdev-is-bridged(.*)%s/ { sub(/-A/, "-D", $1) ; print }'""" % vmchain
    delcmds = filter(None, execute(delcmd).split('\n'))
    for cmd in delcmds:
        try:
            execute("iptables " + cmd)
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
    vm_name = vmName;
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
    curr_domid = getvmId(vmName)
    if (curr_domid is None) or (not curr_domid.isdigit()):
        curr_domid = '-1'

    vm_name = vmName;
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
    if brName is None or brName is "":
        brName = "cloudbr0"
    else:
        brName = execute("iptables-save |grep physdev-is-bridged |grep FORWARD |grep BF |grep '\-o' |awk '{print $4}' | head -1").strip()

    if 1 in [ vm_name.startswith(c) for c in ['r-', 's-', 'v-'] ]:
        default_network_rules_systemvm(vm_name, brName)
        return True

    vmchain = vm_name
    vmchain_default = '-'.join(vmchain.split('-')[:-1]) + "-def"

    vifs = getVifs(vmName)
    logging.debug(vifs, brName)
    for v in vifs:
        execute("iptables -A " + getBrfw(brName) + "-IN " + " -m physdev --physdev-is-bridged --physdev-in " + v + " -j "+ vmchain_default)
        execute("iptables -A " + getBrfw(brName) + "-OUT " + " -m physdev --physdev-is-bridged --physdev-out " + v + " -j "+ vmchain_default)

    #change antispoof rule in vmchain
    try:
        delcmd = """iptables-save | awk '/-A %s(.*)physdev/ { sub(/-A/, "-D", $1) ; print }'""" % vmchain_default
        inscmd = """iptables-save | awk '/-A %s(.*)physdev/ { gsub(/vnet[0-9]+/, "%s") ; sub(/-A/, "-D", $1) ; print }'""" % ( vmchain_default,vifs[0] )
        ipts = []
        for cmd in [delcmd, inscmd]:
            logging.debug(cmd)
            cmds = filter(None, execute(cmd).split('\n'))
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
    vms = virshlist('running')

    result = []
    try:
        for name in vms:
            name = name.rstrip()
            if 1 not in [ name.startswith(c) for c in ['r-', 's-', 'v-', 'i-'] ]:
                continue
            network_rules_for_rebooted_vm(name)
            if name.startswith('i-'):
                log = get_rule_log_for_vm(name)
                result.append(log)
    except:
        logging.exception("Failed to get rule logs, better luck next time!")

    print ";".join(result)

def cleanup_rules_for_dead_vms():
    return True

def cleanup_bridge(bridge):
    bridge_name = getBrfw(bridge)
    logging.debug("Cleaning old bridge chains: " + bridge_name)
    if not bridge_name:
        return True

    # Delete iptables/bridge rules
    rules = execute("""iptables-save | grep %s | grep '^-A' | sed 's/-A/-D/' """ % bridge_name).split("\n")
    for rule in filter(None, rules):
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
        chainscmd = """iptables-save | grep -P '^:(?!.*-(def|eg))' | awk '{sub(/^:/, "", $1) ; print $1}' | sort | uniq"""
        chains = execute(chainscmd).split('\n')
        cleanup = []
        for chain in chains:
            if 1 in [ chain.startswith(c) for c in ['r-', 'i-', 's-', 'v-'] ]:
                vm_name = chain
                result = virshdomstate(vm_name)
                if result == None or len(result) == 0:
                    logging.debug("chain " + chain + " does not correspond to a vm, cleaning up iptable rules")
                    cleanup.append(vm_name)
                    continue
                if not (result == "running" or result == "paused"):
                    logging.debug("vm " + vm_name + " is not running or paused, cleaning up iptable rules")
                    cleanup.append(vm_name)

        bridge_tables = execute("""grep -E '^ebtable_' /proc/modules | cut -f1 -d' ' | sed s/ebtable_//""").split('\n')
        for table in filter(None, bridge_tables):
            chainscmd = """ebtables -t %s -L | awk '/chain:/ { gsub(/(^.*chain: |-(in|out|ips).*)/, ""); print $1}' | sort | uniq""" % table
            chains = execute(chainscmd).split('\n')
            for chain in filter(None, chains):
                if 1 in [ chain.startswith(c) for c in ['r-', 'i-', 's-', 'v-'] ]:
                    vm_name = chain
                    result = virshdomstate(vm_name)
                    if result == None or len(result) == 0:
                        logging.debug("chain " + chain + " does not correspond to a vm, cleaning up ebtable rules")
                        cleanup.append(vm_name)
                        continue
                    if not (result == "running" or result == "paused"):
                        logging.debug("vm " + vm_name + " is not running or paused, cleaning up ebtable rules")
                        cleanup.append(vm_name)

        cleanup = list(set(cleanup))  # remove duplicates
        for vmname in cleanup:
            destroy_network_rules_for_vm(vmname)

        logging.debug("Cleaned up rules for " + str(len(cleanup)) + " chains")
    except:
        logging.debug("Failed to cleanup rules !")

def check_rule_log_for_vm(vmName, vmId, vmIP, domID, signature, seqno):
    vm_name = vmName;
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

def egress_chain_name(vm_name):
    return vm_name + "-eg"

def add_network_rules(vm_name, vm_id, vm_ip, signature, seqno, vmMac, rules, vif, brname, sec_ips):
  try:
    vmName = vm_name
    domId = getvmId(vmName)

    changes = []
    changes = check_rule_log_for_vm(vmName, vm_id, vm_ip, domId, signature, seqno)

    if not 1 in changes:
        logging.debug("Rules already programmed for vm " + vm_name)
        return 'true'

    if changes[0] or changes[1] or changes[2] or changes[3]:
        default_network_rules(vmName, vm_id, vm_ip, vmMac, vif, brname, sec_ips)

    if rules == "" or rules == None:
        lines = []
    else:
        lines = rules.split(';')[:-1]

    logging.debug("    programming network rules for IP: " + vm_ip + " vmname=" + vm_name)
    try:
      vmchain = vm_name
      execute("iptables -F " + vmchain)
      egress_vmchain = egress_chain_name(vm_name)
      execute("iptables -F " + egress_vmchain)
    except:
      logging.debug("Error flushing iptables rules for " + vmchain + ". Presuming firewall rules deleted, re-initializing." )
      default_network_rules(vm_name, vm_id, vm_ip, vmMac, vif, brname)
    egressrule = 0
    for line in lines:
        tokens = line.split(':')
        if len(tokens) != 5:
          continue
        ruletype = tokens[0]
        protocol = tokens[1]
        start = tokens[2]
        end = tokens[3]
        cidrs = tokens.pop();
        ips = cidrs.split(",")
        ips.pop()
        allow_any = False
        if ruletype == 'E':
            vmchain = egress_chain_name(vm_name)
            direction = "-d"
            action = "RETURN"
            egressrule = egressrule + 1
        else:
            vmchain = vm_name
            action = "ACCEPT"
            direction = "-s"
        if '0.0.0.0/0' in ips:
            i = ips.index('0.0.0.0/0')
            del ips[i]
            allow_any = True
        range = start + ":" + end
        if ips:
            if protocol == 'all':
                for ip in ips:
                    execute("iptables -I " + vmchain + " -m state --state NEW " + direction + " " + ip + " -j "+action)
            elif protocol != 'icmp':
                for ip in ips:
                    execute("iptables -I " + vmchain + " -p " + protocol + " -m " + protocol + " --dport " + range + " -m state --state NEW " + direction + " " + ip + " -j "+ action)
            else:
                range = start + "/" + end
                if start == "-1":
                    range = "any"
                for ip in ips:
                    execute("iptables -I " + vmchain + " -p icmp --icmp-type " + range + " " + direction + " " + ip + " -j "+ action)

        if allow_any:
            if protocol == 'all':
                execute("iptables -I " + vmchain + " -m state --state NEW " + direction + " 0.0.0.0/0 -j "+action)
            elif protocol != 'icmp':
                execute("iptables -I " + vmchain + " -p " + protocol + " -m " + protocol + " --dport " + range + " -m state --state NEW -j "+ action)
            else:
                range = start + "/" + end
                if start == "-1":
                    range = "any"
                execute("iptables -I " + vmchain + " -p icmp --icmp-type " + range + " -j "+action)

    egress_vmchain = egress_chain_name(vm_name)
    if egressrule == 0 :
        iptables = "iptables -A " + egress_vmchain + " -j RETURN"
        execute(iptables)
    else:
        iptables = "iptables -A " + egress_vmchain + " -j DROP"
        execute(iptables)

    vmchain = vm_name
    iptables = "iptables -A " + vmchain + " -j DROP"
    execute(iptables)

    if write_rule_log_for_vm(vmName, vm_id, vm_ip, domId, signature, seqno) == False:
        return 'false'

    return 'true'
  except:
    logging.exception("Failed to network rule !")

def getVifs(vmName):
    vifs = []
    xmlfile = virshdumpxml(vmName)
    if xmlfile == None:
        return vifs

    dom = xml.dom.minidom.parseString(xmlfile)
    for network in dom.getElementsByTagName("interface"):
        target = network.getElementsByTagName('target')[0]
        nicdev = target.getAttribute("dev").strip()
        vifs.append(nicdev)
    return vifs

def getVifsForBridge(vmName, brname):
    vifs = []
    xmlfile = virshdumpxml(vmName)
    if xmlfile == None:
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

def getBridges(vmName):
    bridges = []
    xmlfile = virshdumpxml(vmName)
    if xmlfile == None:
        return bridges

    dom = xml.dom.minidom.parseString(xmlfile)
    for network in dom.getElementsByTagName("interface"):
        for source in network.getElementsByTagName('source'):
            bridge = source.getAttribute("bridge").strip()
            bridges.append(bridge)
    return list(set(bridges))

def getvmId(vmName):

    conn = libvirt.openReadOnly(driver)
    if conn == None:
       print 'Failed to open connection to the hypervisor'
       sys.exit(3)

    try:
        dom = (conn.lookupByName (vmName))
    except libvirt.libvirtError:
        return None

    conn.close()

    res = dom.ID()
    if isinstance(res, int):
        res = str(res)
    return res

def getBrfw(brname):
    cmd = "iptables-save |grep physdev-is-bridged |grep FORWARD |grep BF |grep '\-o' | grep -w " + brname  + "|awk '{print $9}' | head -1"
    brfwname = bash("-c", cmd).stdout.strip()
    if brfwname == "":
        brfwname = "BF-" + brname
    return brfwname

def addFWFramework(brname):
    try:
        cfo = configFileOps("/etc/sysctl.conf")
        cfo.addEntry("net.bridge.bridge-nf-call-arptables", "1")
        cfo.addEntry("net.bridge.bridge-nf-call-iptables", "1")
        cfo.addEntry("net.bridge.bridge-nf-call-ip6tables", "1")
        cfo.save()

        execute("sysctl -p /etc/sysctl.conf")
    except:
        logging.debug("failed to turn on bridge netfilter")
        return False

    brfw = getBrfw(brname)
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
        refs = execute("""iptables -n -L %s | awk '/%s(.*)references/ {gsub(/\(/, "") ;print $3}'""" % (brfw,brfw)).strip()
        if refs == "0":
            execute("iptables -I FORWARD -i " + brname + " -j DROP")
            execute("iptables -I FORWARD -o " + brname + " -j DROP")
            execute("iptables -I FORWARD -i " + brname + " -m physdev --physdev-is-bridged -j " + brfw)
            execute("iptables -I FORWARD -o " + brname + " -m physdev --physdev-is-bridged -j " + brfw)
            phydev = execute("brctl show | awk '/^%s[ \t]/ {print $4}'" % brname ).strip()
            execute("iptables -A " + brfw + " -m state --state RELATED,ESTABLISHED -j ACCEPT")
            execute("iptables -A " + brfw + " -m physdev --physdev-is-bridged --physdev-is-in -j " + brfwin)
            execute("iptables -A " + brfw + " -m physdev --physdev-is-bridged --physdev-is-out -j " + brfwout)
            execute("iptables -A " + brfw + " -m physdev --physdev-is-bridged --physdev-out " + phydev + " -j ACCEPT")
        return True
    except:
        try:
            execute("iptables -F " + brfw)
        except:
            return False
        return False

if __name__ == '__main__':
    logging.basicConfig(filename="/var/log/cloudstack/agent/security_group.log", format="%(asctime)s - %(message)s", level=logging.DEBUG)
    parser = OptionParser()
    parser.add_option("--vmname", dest="vmName")
    parser.add_option("--vmip", dest="vmIP")
    parser.add_option("--vmid", dest="vmID")
    parser.add_option("--vmmac", dest="vmMAC")
    parser.add_option("--vif", dest="vif")
    parser.add_option("--sig", dest="sig")
    parser.add_option("--seq", dest="seq")
    parser.add_option("--rules", dest="rules")
    parser.add_option("--brname", dest="brname")
    parser.add_option("--localbrname", dest="localbrname")
    parser.add_option("--dhcpSvr", dest="dhcpSvr")
    parser.add_option("--hostIp", dest="hostIp")
    parser.add_option("--hostMacAddr", dest="hostMacAddr")
    parser.add_option("--nicsecips", dest="nicSecIps")
    parser.add_option("--action", dest="action")
    (option, args) = parser.parse_args()
    if len(args) == 0:
        logging.debug("No command to execute")
        sys.exit(1)
    cmd = args[0]
    logging.debug("Executing command: " + str(cmd))
    if cmd == "can_bridge_firewall":
        can_bridge_firewall(args[1])
    elif cmd == "default_network_rules":
        default_network_rules(option.vmName, option.vmID, option.vmIP, option.vmMAC, option.vif, option.brname, option.nicSecIps)
    elif cmd == "destroy_network_rules_for_vm":
        destroy_network_rules_for_vm(option.vmName, option.vif)
    elif cmd == "default_network_rules_systemvm":
        default_network_rules_systemvm(option.vmName, option.localbrname)
    elif cmd == "get_rule_logs_for_vms":
        get_rule_logs_for_vms()
    elif cmd == "add_network_rules":
        add_network_rules(option.vmName, option.vmID, option.vmIP, option.sig, option.seq, option.vmMAC, option.rules, option.vif, option.brname, option.nicSecIps)
    elif cmd == "network_rules_vmSecondaryIp":
        network_rules_vmSecondaryIp(option.vmName, option.nicSecIps, option.action)
    elif cmd == "cleanup_rules":
        cleanup_rules()
    elif cmd == "post_default_network_rules":
        post_default_network_rules(option.vmName, option.vmID, option.vmIP, option.vmMAC, option.vif, option.brname, option.dhcpSvr, option.hostIp, option.hostMacAddr)
    else:
        logging.debug("Unknown command: " + cmd)
        sys.exit(1)
