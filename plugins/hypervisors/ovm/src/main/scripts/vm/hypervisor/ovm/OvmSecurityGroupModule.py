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
from OvmCommonModule import *
from ConfigFileOps import *
import os
import logging

class OvmSecurityGroup(OvmObject):

    @staticmethod
    def can_bridge_firewall():
        try:
            execute("which iptables")
        except:
            print "iptables was not found on the host"
            return False

        try:
            execute("which ebtables")
        except:
            print "ebtables was not found on the host"
            return False
        
        if not os.path.exists('/var/run/cloud'):
            os.makedirs('/var/run/cloud')
     
        return OvmSecurityGroup.cleanup_rules()        

    @staticmethod
    def cleanup_rules():
        try:
            chainscmd = "iptables-save | grep '^:' | grep -v '.*-def' | awk '{print $1}' | cut -d':' -f2"
            chains = execute(chainscmd).split('\n')
            cleaned = 0
            cleanup = []
            for chain in chains:
                if 1 in [ chain.startswith(c) for c in ['r-', 'i-', 's-', 'v-'] ]:
                    vm_name = chain
                else:
                    continue
                        
                cmd = "xm list | grep " + vm_name 
                try:
                    result = execute(cmd)
                except:
                    result = None

                if result == None or len(result) == 0:
                    logging.debug("chain " + chain + " does not correspond to a vm, cleaning up")
                    cleanup.append(vm_name)
                    
            for vm_name in cleanup:
                OvmSecurityGroup.delete_all_network_rules_for_vm(vm_name)
                        
            logging.debug("Cleaned up rules for " + str(len(cleanup)) + " chains")
            return True
        except:
            logging.debug("Failed to cleanup rules !")
            return False

    @staticmethod
    def add_fw_framework(bridge_name):
        try:
            execute("sysctl -w net.bridge.bridge-nf-call-arptables=1")
            execute("sysctl -w net.bridge.bridge-nf-call-iptables=1")
            execute("sysctl -w net.bridge.bridge-nf-call-ip6tables=1")
        except:
            logging.debug("failed to turn on bridge netfilter")
            return False

        brfw = "BF-" + bridge_name
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
            refs = execute("iptables -n -L  " + brfw + " |grep " + brfw + " | cut -d \( -f2 | awk '{print $1}'").strip()
            if refs == "0":
                execute("iptables -I FORWARD -i " + bridge_name + " -j DROP")
                execute("iptables -I FORWARD -o " + bridge_name + " -j DROP")
                execute("iptables -I FORWARD -i " + bridge_name + " -m physdev --physdev-is-bridged -j " + brfw)
                execute("iptables -I FORWARD -o " + bridge_name + " -m physdev --physdev-is-bridged -j " + brfw)
                phydev = execute("brctl show |grep " + bridge_name + " | awk '{print $4}'").strip()
                execute("iptables -A " + brfw + " -m physdev --physdev-is-bridged --physdev-out " + phydev + " -j ACCEPT")
                execute("iptables -A " + brfw + " -m state --state RELATED,ESTABLISHED -j ACCEPT")
                execute("iptables -A " + brfw + " -m physdev --physdev-is-bridged --physdev-is-out -j " + brfwout)
                execute("iptables -A " + brfw + " -m physdev --physdev-is-bridged --physdev-is-in -j " + brfwin)              
        
            return True
        except:
            try:
                execute("iptables -F " + brfw)
            except:
                return False
            
            return False

    @staticmethod
    def default_network_rules_user_vm(vm_name, vm_id, vm_ip, vm_mac, vif, bridge_name):
        if not OvmSecurityGroup.add_fw_framework(bridge_name):
            return False 

        OvmSecurityGroup.delete_iptables_rules_for_vm(vm_name)
        OvmSecurityGroup.delete_ebtables_rules_for_vm(vm_name)
        
        bridge_firewall_chain = "BF-" + bridge_name    
        vm_chain = vm_name
        default_vm_chain = '-'.join(vm_chain.split('-')[:-1]) + "-def"
        dom_id = getDomId(vm_name)

        try:
            execute("iptables -N " + vm_chain)
        except:
            execute("iptables -F " + vm_chain)
            
        try:
            execute("iptables -N " + default_vm_chain)
        except:
            execute("iptables -F " + default_vm_chain)

        try:
            execute("iptables -A " + bridge_firewall_chain + "-OUT" + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -j " +  default_vm_chain)
            execute("iptables -A " + bridge_firewall_chain + "-IN" + " -m physdev --physdev-is-bridged --physdev-in " +  vif + " -j " + default_vm_chain)
            execute("iptables -A  " + default_vm_chain + " -m state --state RELATED,ESTABLISHED -j ACCEPT")

            # Allow DHCP
            execute("iptables -A " + default_vm_chain + " -m physdev --physdev-is-bridged --physdev-in " + vif + " -p udp --dport 67 --sport 68 -j ACCEPT")
            execute("iptables -A " + default_vm_chain + " -m physdev --physdev-is-bridged --physdev-out " + vif + " -p udp --dport 68 --sport 67  -j ACCEPT")

            # Don't let a VM spoof its ip address
            if vm_ip is not None:
                execute("iptables -A " + default_vm_chain + " -m physdev --physdev-is-bridged --physdev-in " + vif  + " --source " +  vm_ip +  " -j ACCEPT")

            execute("iptables -A " + default_vm_chain + " -j " +  vm_chain)
            execute("iptables -A " + vm_chain + " -j DROP")
        except:
            logging.debug("Failed to program default rules for vm " + vm_name)
            return False
        
        OvmSecurityGroup.default_ebtables_rules(vm_chain, vm_ip, vm_mac, vif)
        
        if vm_ip is not None:
            if (OvmSecurityGroup.write_rule_log_for_vm(vm_name, vm_id, vm_ip, dom_id, '_initial_', '-1') == False):
                logging.debug("Failed to log default network rules, ignoring")
            
        logging.debug("Programmed default rules for vm " + vm_name)
        return True

    @staticmethod
    def default_ebtables_rules(vm_name, vm_ip, vm_mac, vif):
        vm_chain_in = vm_name + "-in"
        vm_chain_out = vm_name + "-out"
        
        for chain in [vm_chain_in, vm_chain_out]:
            try:
                execute("ebtables -t nat -N " + chain)
            except:
                execute("ebtables -t nat -F " + chain) 

        try:
            execute("ebtables -t nat -A PREROUTING -i " + vif + " -j " +  vm_chain_in)
            execute("ebtables -t nat -A POSTROUTING -o " + vif + " -j " + vm_chain_out)
        except:
            logging.debug("Failed to program default rules")
            return False
        
        try:
            execute("ebtables -t nat -A " +  vm_chain_in + " -s ! " +  vm_mac + " -j DROP")
            execute("ebtables -t nat -A " +  vm_chain_in  + " -p ARP -s ! " + vm_mac + " -j DROP")
            execute("ebtables -t nat -A " +  vm_chain_in  + " -p ARP --arp-mac-src ! " + vm_mac + " -j DROP")
            if vm_ip is not None:
                execute("ebtables -t nat -A " + vm_chain_in  +  " -p ARP --arp-ip-src ! " + vm_ip + " -j DROP")
            execute("ebtables -t nat -A " + vm_chain_in  + " -p ARP --arp-op Request -j ACCEPT")   
            execute("ebtables -t nat -A " + vm_chain_in  + " -p ARP --arp-op Reply -j ACCEPT")    
            execute("ebtables -t nat -A " + vm_chain_in  + " -p ARP  -j DROP")    
        except:
            logging.exception("Failed to program default ebtables IN rules")
            return False
       
        try:
            execute("ebtables -t nat -A " + vm_chain_out + " -p ARP --arp-op Reply --arp-mac-dst ! " +  vm_mac + " -j DROP")
            if vm_ip is not None:
                execute("ebtables -t nat -A " + vm_chain_out + " -p ARP --arp-ip-dst ! " + vm_ip + " -j DROP") 
            execute("ebtables -t nat -A " + vm_chain_out + " -p ARP --arp-op Request -j ACCEPT")   
            execute("ebtables -t nat -A " + vm_chain_out + " -p ARP --arp-op Reply -j ACCEPT")    
            execute("ebtables -t nat -A " + vm_chain_out + " -p ARP -j DROP")    
        except:
            logging.debug("Failed to program default ebtables OUT rules")
            return False

        return True

    @staticmethod
    def add_network_rules(vm_name, vm_id, vm_ip, signature, seqno, vm_mac, rules, vif, bridge_name):
        try:
            vm_chain = vm_name
            dom_id = getDomId(vm_name)
            
            changes = []
            changes = OvmSecurityGroup.check_rule_log_for_vm(vm_name, vm_id, vm_ip, dom_id, signature, seqno)
        
            if not 1 in changes:
                logging.debug("Rules already programmed for vm " + vm_name)
                return True
        
            if changes[0] or changes[1] or changes[2] or changes[3]:
                if not OvmSecurityGroup.default_network_rules(vm_name, vm_id, vm_ip, vm_mac, vif, bridge_name):
                    return False

            if rules == "" or rules == None:
                lines = []
            else:
                lines = rules.split(';')[:-1]

            logging.debug("Programming network rules for  IP: " + vm_ip + " vmname=" + vm_name)
            execute("iptables -F " + vm_chain)
        
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
                    
                port_range = start + ":" + end
                if ips:    
                    if protocol == 'all':
                        for ip in ips:
                            execute("iptables -I " + vm_chain + " -m state --state NEW -s " + ip + " -j ACCEPT")
                    elif protocol != 'icmp':
                        for ip in ips:
                            execute("iptables -I " + vm_chain + " -p " + protocol + " -m " + protocol + " --dport " + port_range + " -m state --state NEW -s " + ip + " -j ACCEPT")
                    else:
                        port_range = start + "/" + end
                        if start == "-1":
                            port_range = "any"
                            for ip in ips:
                                execute("iptables -I " + vm_chain + " -p icmp --icmp-type " + port_range + " -s " + ip + " -j ACCEPT")
            
                if allow_any and protocol != 'all':
                    if protocol != 'icmp':
                        execute("iptables -I " + vm_chain + " -p " + protocol + " -m " +  protocol + " --dport " + port_range + " -m state --state NEW -j ACCEPT")
                    else:
                        port_range = start + "/" + end
                        if start == "-1":
                            port_range = "any"
                            execute("iptables -I " + vm_chain + " -p icmp --icmp-type " + port_range + " -j ACCEPT")
        
            iptables =  "iptables -A " + vm_chain + " -j DROP"       
            execute(iptables)
            
            return OvmSecurityGroup.write_rule_log_for_vm(vm_name, vm_id, vm_ip, dom_id, signature, seqno)        
        except:
            logging.debug("Failed to network rule !: " + sys.exc_type)
            return False

    @staticmethod
    def delete_all_network_rules_for_vm(vm_name, vif = None):            
        OvmSecurityGroup.delete_iptables_rules_for_vm(vm_name)
        OvmSecurityGroup.delete_ebtables_rules_for_vm(vm_name)

        vm_chain = vm_name
        default_vm_chain = None
        if vm_name.startswith('i-') or vm_name.startswith('r-'):
            default_vm_chain =  '-'.join(vm_name.split('-')[:-1]) + "-def"
        
        try:
            if default_vm_chain != None: 
                execute("iptables -F " + default_vm_chain)
        except:
            logging.debug("Ignoring failure to delete chain " + default_vm_chain)
        
        try:
            if default_vm_chain != None: 
                execute("iptables -X " + vmchain_default)
        except:
            logging.debug("Ignoring failure to delete chain " + default_vm_chain)

        try:
            execute("iptables -F " + vm_chain)
        except:
            logging.debug("Ignoring failure to delete  chain " + vm_chain)
        
        try:
            execute("iptables -X " + vm_chain)
        except:
            logging.debug("Ignoring failure to delete  chain " + vm_chain)
        
        if vif is not None:
            try:
                dnats = execute("iptables-save -t nat | grep " + vif + " | sed 's/-A/-D/'").split("\n")
                for dnat in dnats:
                    try:
                        execute("iptables -t nat " + dnat)
                    except:
                        logging.debug("Igoring failure to delete dnat: " + dnat) 
            except:
                pass
            
        OvmSecurityGroup.remove_rule_log_for_vm(vm_name)
        
        if 1 in [ vm_name.startswith(c) for c in ['r-', 's-', 'v-'] ]:
            return True
        
        return True

    @staticmethod
    def delete_iptables_rules_for_vm(vm_name):
        vm_name = OvmSecurityGroup.truncate_vm_name(vm_name)
        vm_chain = vm_name
        query = "iptables-save | grep " +  vm_chain + " | grep physdev-is-bridged | sed 's/-A/-D/'"
        delete_cmds = execute(query).split('\n')
        delete_cmds.pop()
        
        for cmd in delete_cmds:
            try:
                execute("iptables " + cmd)
            except:
                logging.exception("Ignoring failure to delete rules for vm " + vm_name)

    @staticmethod
    def delete_ebtables_rules_for_vm(vm_name):
        vm_name = OvmSecurityGroup.truncate_vm_name(vm_name)        
        query = "ebtables -t nat -L --Lx | grep ROUTING | grep " +  vm_name + " | sed 's/-A/-D/'"
        delete_cmds = execute(query).split('\n')
        delete_cmds.pop()

        for cmd in delete_cmds:
            try:
                execute(cmd)
            except:
                logging.debug("Ignoring failure to delete ebtables rules for vm " + vm_name)
                
        chains = [vm_name + "-in", vm_name + "-out"]
        
        for chain in chains:
            try:
                execute("ebtables -t nat -F " +  chain)
                execute("ebtables -t nat -X " +  chain)
            except:
                logging.debug("Ignoring failure to delete ebtables chain for vm " + vm_name)

    @staticmethod
    def truncate_vm_name(vm_name):
        if vm_name.startswith('i-') or vm_name.startswith('r-'):
            truncated_vm_name = '-'.join(vm_name.split('-')[:-1])
        else:
            truncated_vm_name = vm_name
        return truncated_vm_name        

    @staticmethod
    def write_rule_log_for_vm(vm_name, vm_id, vm_ip, dom_id, signature, seqno):
        log_file_name = "/var/run/cloud/" + vm_name + ".log"
        logging.debug("Writing log to " + log_file_name)
        logf = open(log_file_name, 'w')
        output = ','.join([vm_name, vm_id, vm_ip, dom_id, signature, seqno])

        result = True        
        try:
            logf.write(output)
            logf.write('\n')
        except:
            logging.debug("Failed to write to rule log file " + log_file_name)
            result = False
            
        logf.close()        
        return result

    @staticmethod
    def remove_rule_log_for_vm(vm_name):
        log_file_name = "/var/run/cloud/" + vm_name +".log"

        result = True
        try:
            os.remove(log_file_name)
        except:
            logging.debug("Failed to delete rule log file " + log_file_name)
            result = False
        
        return result

    @staticmethod
    def check_rule_log_for_vm(vm_name, vm_id, vm_ip, dom_id, signature, seqno):
        log_file_name = "/var/run/cloud/" + vm_name + ".log"
        if not os.path.exists(log_file_name):
            return [True, True, True, True, True, True]
            
        try:
            lines = (line.rstrip() for line in open(log_file_name))
        except:
            logging.debug("failed to open " + log_file_name) 
            return [True, True, True, True, True, True]

        [_vm_name, _vm_id, _vm_ip, _dom_id, _signature, _seqno] = ['_', '-1', '_', '-1', '_', '-1']
        try:
            for line in lines:
                [_vm_name, _vm_id, _vm_ip, _dom_id, _signature, _seqno] = line.split(',')
                break
        except:
            logging.debug("Failed to parse log file for vm " + vm_name)
            remove_rule_log_for_vm(vm_name)
            return [True, True, True, True, True, True]
        
        return [(vm_name != _vm_name), (vm_id != _vm_id), (vm_ip != _vm_ip), (dom_id != _dom_id), (signature != _signature), (seqno != _seqno)]

    



































            
