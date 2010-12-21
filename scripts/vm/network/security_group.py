#!/usr/bin/python
import cloud_utils
from cloud_utils import Command
import logging
import sys
import os
import xml.dom.minidom
from optparse import OptionParser, OptionGroup, OptParseError, BadOptionError, OptionError, OptionConflictError, OptionValueError
iptables = Command("iptables")
bash = Command("/bin/bash")
virsh = Command("virsh")
ebtablessave = Command("ebtables-save")
ebtables = Command("ebtables")
augtool = Command("augtool")
def execute(cmd):
    logging.debug(cmd)
    return bash("-c", cmd).stdout
def can_bridge_firewall(privnic):
    try:
        execute("which iptables")
    except:
        print "no iptables on your host machine"
        exit(1)

    try:
        execute("which ebtables")
    except:
        print "no ebtables on your host machine"
        exit(2)

    try:
        alreadySetup = augtool.match("/files/etc/sysctl.conf/net.bridge.bridge-nf-call-arptables", "1").stdout.strip()
        if len(alreadySetup) == 0:
            script = """
                        set /files/etc/sysctl.conf/net.bridge.bridge-nf-call-arptables 1
                        save"""
            augtool < script

        alreadySetup = augtool.match("/files/etc/sysctl.conf/net.bridge.bridge-nf-call-iptables", "1").stdout.strip()
        if len(alreadySetup) == 0:
            script = """
                        set /files/etc/sysctl.conf/net.bridge.bridge-nf-call-iptables 1
                        save"""
            augtool < script

        alreadySetup = augtool.match("/files/etc/sysctl.conf/net.bridge.bridge-nf-call-ip6tables", "1").stdout.strip()
        if len(alreadySetup) == 0:
            script = """
                        set /files/etc/sysctl.conf/net.bridge.bridge-nf-call-ip6tables 1
                        save"""
            augtool < script
        execute("sysctl -p /etc/sysctl.conf")
    except:
        print "failed to turn on bridge netfilter"
        exit(3)

    try:
        execute("iptables -N BRIDGE-FIREWALL")
        execute("iptables -N BRIDGE-FIREWALL")
        execute("iptables -I BRIDGE-FIREWALL -m state --state RELATED,ESTABLISHED -j ACCEPT")
        execute("iptables -D FORWARD -j RH-Firewall-1-INPUT")
    except:
        logging.exception('Chain BRIDGE-FIREWALL already exists: ')

    result = 0
    try:
        execute("iptables -n -L FORWARD | grep BRIDGE-FIREWALL")
    except:
        try:
            execute("iptables -I FORWARD -m physdev --physdev-is-bridged -j BRIDGE-FIREWALL")
            execute("iptables -A FORWARD -m physdev --physdev-is-bridged --physdev-out " +  privnic + " -j ACCEPT")
            execute("iptables -A FORWARD -j DROP")
        except:
            result = 1

    
    if result == 1:
        print "br firewall is not supported"
    if not os.path.exists('/var/run/cloud'):
        os.makedirs('/var/run/cloud')
 
    cleanup_rules_for_dead_vms()
    cleanup_rules()
    
    return result
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

def destroy_network_rules_for_vm(vm_name):
    vmchain = vm_name
    
    delete_rules_for_vm_in_bridge_firewall_chain(vm_name)
    if vm_name.startswith('i-') or vm_name.startswith('r-'):
        vmchain =  '-'.join(vm_name.split('-')[:-1])
        vmchain_default =  '-'.join(vm_name.split('-')[:-2]) + "-def"

    destroy_ebtables_rules(vmchain)
    
    try:
        execute("iptables -F " + vmchain_default)
        execute("iptables -X " + vmchain_default)
    except:
        logging.exception("Ignoring failure to delete  chain " + vmchain_default)
    
    try:
        execute("iptables -F " + vmchain)
        execute("iptables -X " + vmchain)
    except:
        logging.debug("Ignoring failure to delete  chain " + vmchain)
    
    remove_rule_log_for_vm(vm_name)
    
    if 1 in [ vm_name.startswith(c) for c in ['r-', 's-', 'v-'] ]:
        return 'true'
    
    return 'true'

def destroy_ebtables_rules(vm_name):
    delcmd = "ebtables-save | grep ROUTING | grep " +  vm_name + " | sed 's/-A/-D/'"
    delcmds = execute(delcmd).split('\n')
    delcmds.pop()
    for cmd in delcmds:
        try:
            execute("ebtables -t nat " +  cmd)
        except:
            logging.debug("Ignoring failure to delete ebtables rules for vm " + vm_name)
    chains = [vm_name+"-in", vm_name+"-out"]
    for chain in chains:
        try:
            execute("ebtables -t nat -F " +  chain)
            execute("ebtables -t nat -X " +  chain)
        except:
            logging.debug("Ignoring failure to delete ebtables chain for vm " + vm_name)   

def default_ebtables_rules(vm_name, vif, vm_ip, vm_mac):
    vmchain_in = vm_name + "-in"
    vmchain_out = vm_name + "-out"
    
    for chain in [vmchain_in, vmchain_out]:
        try:
            execute("ebtables -t nat -N " + chain)
        except:
            execute("ebtables -t nat -F " + chain) 

    try:
        # -s ! 52:54:0:56:44:32 -j DROP 
        execute("ebtables -t nat -A PREROUTING -i " + vif + " -j " +  vmchain_in)
        execute("ebtables -t nat -A POSTROUTING -o " + vif + " -j " + vmchain_out)
    except:
        logging.debug("Failed to program default rules")
        return 'false'
    
    try:
        execute("ebtables -t nat -A " +  vmchain_in + " -i " + vif + " -s ! " +  vm_mac + " -j DROP")
        execute("ebtables -t nat -A " +  vmchain_in + " -p ARP -s ! " + vm_mac + " -j DROP")
        execute("ebtables -t nat -A " +  vmchain_in + " -p ARP --arp-mac-src ! " + vm_mac + " -j DROP")
        execute("ebtables -t nat -A " + vmchain_in +  " -p ARP --arp-ip-src ! " + vm_ip + " -j DROP") 
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP --arp-op Request -j ACCEPT")   
        execute("ebtables -t nat -A " + vmchain_in  + " -p ARP --arp-op Reply -j ACCEPT")    
        execute("ebtables -t nat -A " + vmchain_in + " -p ARP  -j DROP")    
    except:
        logging.exception("Failed to program default ebtables IN rules")
        return 'false'
   
    try:
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-op Reply --arp-mac-dst ! " +  vm_mac + " -j DROP")
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-ip-dst ! " + vm_ip + " -j DROP") 
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-op Request -j ACCEPT")   
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP --arp-op Reply -j ACCEPT")    
        execute("ebtables -t nat -A " + vmchain_out + " -p ARP -j DROP")    
    except:
        logging.debug("Failed to program default ebtables OUT rules")
        return 'false' 
    
            
def default_network_rules_systemvm(vm_name):
    vifs = getVifs(vm_name)
    domid = getvmId(vm_name)
    vmchain = vm_name
    if vm_name.startswith('r-'):
        vmchain = '-'.join(vm_name.split('-')[:-1])
 
    delete_rules_for_vm_in_bridge_firewall_chain(vm_name)
  
    try:
        execute("iptables -N " + vmchain)
    except:
        execute("iptables -F " + vmchain)

  
    for vif in vifs:
        try:
            execute("iptables -A BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-out " + vif +  " -j " + vmchain)
            execute("iptables -A BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-in " + vif + " -j " +  vmchain)
        except:
            logging.debug("Failed to program default rules")
            return 'false'

    execute("iptables -A " + vmchain + " -j ACCEPT")
    
    if write_rule_log_for_vm(vm_name, '-1', '_ignore_', domid, '_initial_', '-1') == False:
        logging.debug("Failed to log default network rules for systemvm, ignoring")
    return 'true'


def default_network_rules(vm_name, vm_ip, vm_id, vm_mac):
    print vm_name 
    print vm_ip 
    print vm_mac
    vmName = vm_name 
    domID = getvmId(vm_name)
    delete_rules_for_vm_in_bridge_firewall_chain(vmName)
    vm_name =  '-'.join(vm_name.split('-')[:-1])
    vmchain = vm_name
    vmchain_default = '-'.join(vmchain.split('-')[:-1]) + "-def"
    
    destroy_ebtables_rules(vmName)
    
    vifs = getVifs(vmName)

    try:
        execute("iptables -N " + vmchain)
    except:
        execute("iptables -F " + vmchain)
        
    try:
        execute("iptables -N " + vmchain_default)
    except:
        execute("iptables -F " + vmchain_default)

    try:
        for v in vifs:
            execute("iptables -A BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-out " + v + " -j " +  vmchain_default)
            execute("iptables -A BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-in " +  v + " -j " + vmchain_default)
        execute("iptables -A  " + vmchain_default + " -m state --state RELATED,ESTABLISHED -j ACCEPT")
        #allow dhcp
        for v in vifs:
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + v + " -p udp --dport 67 --sport 68 -j ACCEPT")
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-out " + v + " -p udp --dport 68 --sport 67  -j ACCEPT")

        #don't let vm spoof its ip address
        for v in vifs:
            execute("iptables -A " + vmchain_default + " -m physdev --physdev-is-bridged --physdev-in " + v  + " --source " +  vm_ip +  " -j RETURN")
        execute("iptables -A " + vmchain_default + " -j " +  vmchain)
    except:
        logging.debug("Failed to program default rules for vm " + vm_name)
        return 'false'
    
    for v in vifs:
    	default_ebtables_rules(vmchain, v, vm_ip, vm_mac)
    
    if write_rule_log_for_vm(vmName, vm_id, vm_ip, domID, '_initial_', '-1') == False:
        logging.debug("Failed to log default network rules, ignoring")
        
    logging.debug("Programmed default rules for vm " + vm_name)
    return 'true'
    
def delete_rules_for_vm_in_bridge_firewall_chain(vmName):
    vm_name = vmName
    if vm_name.startswith('i-') or vm_name.startswith('r-'):
        vm_name =  '-'.join(vm_name.split('-')[:-2])
    
    vmchain = vm_name
    
    delcmd = "iptables -S BRIDGE-FIREWALL | grep " +  vmchain + " | sed 's/-A/-D/'"
    delcmds = execute(delcmd).split('\n')
    delcmds.pop()
    for cmd in delcmds:
        try:
            execute("iptables " + cmd)
        except:
              logging.exception("Ignoring failure to delete rules for vm " + vmName)

'''  
def network_rules_for_rebooted_vm(vmName):
    vm_name = vmName
    vifs = getVifs(vmName) 
    logging.debug("Found a rebooted VM -- reprogramming rules for  " + vmName)
    
    delete_rules_for_vm_in_bridge_firewall_chain(vmName)
    if 1 in [ vm_name.startswith(c) for c in ['r-', 's-', 'v-'] ]:
        default_network_rules_systemvm(session, {"vmName":vmName})
        return True
    
    vmchain = '-'.join(vm_name.split('-')[:-1])
    vmchain_default = '-'.join(vm_name.split('-')[:-2]) + "-def"

    for v in vifs:
        iptables('-A', 'BRIDGE-FIREWALL', '-m', 'physdev', '--physdev-is-bridged', '--physdev-out', v, '-j', vmchain_default)
        iptables('-A', 'BRIDGE-FIREWALL', '-m', 'physdev', '--physdev-is-bridged', '--physdev-in', v, '-j', vmchain_default)

    #change antispoof rule in vmchain
    try:
        delcmd = "iptables -S " +  vmchain_default + " | grep  physdev-in | sed 's/-A/-D/'"
        inscmd = "iptables -S " +  vmchain_default + " | grep  physdev-in | grep vif | sed -r 's/vif[0-9]+.0/" + vif + "/' | sed 's/-A/-I/'"
        inscmd2 = "iptables -S " +  vmchain_default + " | grep  physdev-in | grep tap | sed -r 's/tap[0-9]+.0/" + tap + "/' | sed 's/-A/-I/'"
        
        ipts = []
        for cmd in [delcmd, inscmd, inscmd2]:
            cmds = bash('-c', cmd.split(' ')).split('\n')
            cmds.pop()
            for c in cmds:
                    ipt = c.split(' ')
                    ipt.pop()
                    ipts.append(ipt)
        
        for ipt in ipts:
            try:
                iptables(ipt)
            except:
                logging.debug("Failed to rewrite antispoofing rules for vm " + vmName)
    except:
        logging.debug("No rules found for vm " + vmchain)


    rewrite_rule_log_for_vm(vmName, curr_domid)
    return True
'''  

def rewrite_rule_log_for_vm(vm_name, new_domid):
    logfilename = "/var/run/cloud/" + vm_name +".log"
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
    logfilename = "/var/run/cloud/" + vm_name +".log"
    if not os.path.exists(logfilename):
        return ''
    
    lines = (line.rstrip() for line in open(logfilename))
    
    [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = ['_', '-1', '_', '-1', '_', '-1']
    for line in lines:
        [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = line.split(',')
        break
    
    return ','.join([_vmName, _vmID, _vmIP, _domID, _signature, _seqno])

def get_rule_logs_for_vms():
    cmd = "virsh list|grep running |awk '{print $2}'"
    vms = bash("-c", cmd).stdout.split("\n")
    
    result = []
    try:
        for name in vms:
            name = name.rstrip()
            if 1 not in [ name.startswith(c) for c in ['r-', 's-', 'v-', 'i-'] ]:
                continue
            #network_rules_for_rebooted_vm(session, name)
            if name.startswith('i-'):
                log = get_rule_log_for_vm(name)
                result.append(log)
    except:
        logging.debug("Failed to get rule logs, better luck next time!")
        
    print ";".join(result)

def cleanup_rules_for_dead_vms():
    return True 


def cleanup_rules():
  try:

    chainscmd = "iptables-save | grep '^:' | grep '.*-def' | awk '{print $1}' | cut -d':' -f2"
    chains = execute(chainscmd).split('\n')
    cleaned = 0
    cleanup = []
    for chain in chains:
        if 1 in [ chain.startswith(c) for c in ['r-', 'i-', 's-', 'v-'] ]:
            if chain.startswith('i-') or chain.startswith('r-'):
                vm_name = chain + '-untagged'
            else:
                vm_name = chain
                
            cmd = "virsh list |grep " + vm_name 
            try:
                result = execute(cmd)
            except:
                result = None

            if result == None or len(result) == 0:
                logging.debug("chain " + chain + " does not correspond to a vm, cleaning up")
                cleanup.append(vm_name)
                continue
            if result.find("running") == -1:
                logging.debug("vm " + vm_name + " is not running, cleaning up")
                cleanup.append(vm_name)
                
    for vmname in cleanup:
        destroy_network_rules_for_vm({'vmName':vmname})
                    
    logging.debug("Cleaned up rules for " + str(len(cleanup)) + " chains")                
  except:
    logging.debug("Failed to cleanup rules !")

def check_rule_log_for_vm(vmName, vmId, vmIP, domID, signature, seqno):
    vm_name = vmName;
    logfilename = "/var/run/cloud/" + vm_name +".log"
    if not os.path.exists(logfilename):
        return [True, True, True, True, True, True]
        
    try:
        lines = (line.rstrip() for line in open(logfilename))
    except:
        logging.debug("failed to open " + logfilename) 
        return False

    [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = ['_', '-1', '_', '-1', '_', '-1']
    try:
        for line in lines:
            [_vmName,_vmID,_vmIP,_domID,_signature,_seqno] = line.split(',')
            break
    except:
        logging.debug("Failed to parse log file for vm " + vm_name)
        remove_rule_log_for_vm(vm_name)
        return False
    
    return [(vm_name != _vmName), (vmId != _vmID), (vmIP != _vmIP), (domID != _domID), (signature != _signature),(seqno != _seqno)]

def write_rule_log_for_vm(vmName, vmID, vmIP, domID, signature, seqno):
    vm_name = vmName
    logfilename = "/var/run/cloud/" + vm_name +".log"
    logging.debug("Writing log to " + logfilename)
    logf = open(logfilename, 'w')
    output = ','.join([vmName, vmID, vmIP, domID, signature, seqno])
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
    logfilename = "/var/run/cloud/" + vm_name +".log"

    result = True
    try:
        os.remove(logfilename)
    except:
        logging.debug("Failed to delete rule log file " + logfilename)
        result = False
    
    return result

def add_network_rules(vm_name, vm_id, vm_ip, signature, seqno, vmMac, rules):
  try:
    vmName = vm_name
    domId = getvmId(vmName)
    vm_name =  '-'.join(vm_name.split('-')[:-1])
    vmchain = vm_name
    
    changes = check_rule_log_for_vm(vmName, vm_id, vm_ip, domId, signature, seqno)
    
    if not 1 in changes:
        logging.debug("Rules already programmed for vm " + vm_name)
        return 'true'
    
    if changes[1] or changes[2] or changes[3]:
        logging.debug("Change detected in vmId or vmIp or domId, resetting default rules")
        default_network_rules(vmName, vm_ip, vm_id, vmMac)
        
    lines = rules.split(';')

    print lines
    logging.debug("    programming network rules for  IP: " + vm_ip + " vmname=" + vm_name)
    #iptables('-F', vmchain)
    print lines
    
    for line in lines:
        tokens = line.split(':')
        if len(tokens) != 4:
          continue
        protocol = tokens[0]
        start = tokens[1]
        end = tokens[2]
        cidrs = tokens.pop();
        ips = cidrs.split(",")
        ips.pop()
        allow_any = False
        if  '0.0.0.0/0' in ips:
            i = ips.index('0.0.0.0/0')
            del ips[i]
            allow_any = True
        range = start + ":" + end
        if ips:    
            if protocol == 'all':
                for ip in ips:
                    iptables = "iptables -I " + vmchain + " -m state --state NEW -m iprange --src-range " + ip + " -j ACCEPT"
            elif protocol != 'icmp':
                for ip in ips:
                    iptables = "iptables -I " + vmchain + " -p " + protocol + " -m " + protocol + " --dport " + range + " -m state --state NEW -m iprange --src-range " + ip + " -j ACCEPT"
            else:
                range = start + "/" + end
                if start == "-1":
                    range = "any"
                    for ip in ips:
                        iptables = "iptables -I " + vmchain + " -p icmp --icmp-type " + range + " -m iprange --src-range " + ip + " -j ACCEPT"
            execute(iptables)
        
        if allow_any and protocol != 'all':
            if protocol != 'icmp':
                iptables = "iptables -I " + vmchain + " -p " + protocol + " -m " +  protocol + " --dport " + range + " -m state --state NEW -j ACCEPT"
            else:
                range = start + "/" + end
                if start == "-1":
                    range = "any"
                    iptables = "iptables -I " + vmchain + " -p icmp --icmp-type " + range + " -j ACCEPT"
            execute(iptables)

    iptables =  "iptables -A " + vmchain + " -j DROP"       
    execute(iptables)

    if write_rule_log_for_vm(vmName, vm_id, vm_ip, domId, signature, seqno) == False:
        return 'false'
    
    return 'true'
  except:
    logging.debug("Failed to network rule !: " + sys.exc_type)

def getVifs(vmName):
    vifs = []
    try:
        xmlfile = virsh("dumpxml", vmName).stdout 
    except:
        return vifs    

    dom = xml.dom.minidom.parseString(xmlfile)
    vifs = []
    for network in dom.getElementsByTagName("interface"):
        target = network.getElementsByTagName('target')[0]
        nicdev = target.getAttribute("dev").strip()
        vifs.append(nicdev) 
    return vifs
def getvmId(vmName):
    cmd = "virsh list |grep " + vmName + " | awk '{print $1}'"
    return bash("-c", cmd).stdout.strip()
    
if __name__ == '__main__':
    logging.basicConfig(filename="/var/log/cloud/security_group.log", format="%(asctime)s - %(message)s", level=logging.DEBUG)
    parser = OptionParser()
    parser.add_option("--vmname", dest="vmName")
    parser.add_option("--vmip", dest="vmIP")
    parser.add_option("--vmid", dest="vmID")
    parser.add_option("--vmmac", dest="vmMAC")
    parser.add_option("--vif", dest="vif")
    parser.add_option("--sig", dest="sig")
    parser.add_option("--seq", dest="seq")
    parser.add_option("--rules", dest="rules")
    (option, args) = parser.parse_args()
    cmd = args[0]
    if cmd == "can_bridge_firewall":
        can_bridge_firewall(args[1])
    elif cmd == "default_network_rules":
        default_network_rules(option.vmName, option.vmIP, option.vmID, option.vmMAC)
    elif cmd == "destroy_network_rules_for_vm":
        destroy_network_rules_for_vm(option.vmName) 
    elif cmd == "default_network_rules_systemvm":
        default_network_rules_systemvm(option.vmName)
    elif cmd == "get_rule_logs_for_vms":
        get_rule_logs_for_vms()
    elif cmd == "add_network_rules":
        add_network_rules(option.vmName, option.vmID, option.vmIP, option.sig, option.seq, option.vmMAC, option.rules)
