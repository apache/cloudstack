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

# cloudstack_pluginlib for openvswitch on KVM hypervisor

import configparser
import logging
import os
import subprocess

from time import localtime, asctime

DEFAULT_LOG_FORMAT = "%(asctime)s %(levelname)8s [%(name)s] %(message)s"
DEFAULT_LOG_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"
DEFAULT_LOG_FILE = "/var/log/cloudstack_plugins.log"

PLUGIN_CONFIG_PATH = "/usr/share/cloudstack-common/scripts/vm/hypervisor/xenserver/cloudstack_plugins.conf"
OVSDB_PID_PATH = "/var/run/openvswitch/ovsdb-server.pid"
OVSDB_DAEMON_PATH = "ovsdb-server"
OVS_PID_PATH = "/var/run/openvswitch/ovs-vswitchd.pid"
OVS_DAEMON_PATH = "ovs-vswitchd"
VSCTL_PATH = "/usr/bin/ovs-vsctl"
OFCTL_PATH = "/usr/bin/ovs-ofctl"

class PluginError(Exception):
    """Base Exception class for all plugin errors."""
    def __init__(self, *args):
        Exception.__init__(self, *args)


def setup_logging(log_file=None):
    debug = False
    verbose = False
    log_format = DEFAULT_LOG_FORMAT
    log_date_format = DEFAULT_LOG_DATE_FORMAT
    # try to read plugin configuration file
    if os.path.exists(PLUGIN_CONFIG_PATH):
        config = configparser.ConfigParser()
        config.read(PLUGIN_CONFIG_PATH)
        try:
            options = config.options('LOGGING')
            if 'debug' in options:
                debug = config.getboolean('LOGGING', 'debug')
            if 'verbose' in options:
                verbose = config.getboolean('LOGGING', 'verbose')
            if 'format' in options:
                log_format = config.get('LOGGING', 'format')
            if 'date_format' in options:
                log_date_format = config.get('LOGGING', 'date_format')
            if 'file' in options:
                log_file_2 = config.get('LOGGING', 'file')
        except ValueError:
            # configuration file contained invalid attributes
            # ignore them
            pass
        except configparser.NoSectionError:
            # Missing 'Logging' section in configuration file
            pass

    root_logger = logging.root
    if debug:
        root_logger.setLevel(logging.DEBUG)
    elif verbose:
        root_logger.setLevel(logging.INFO)
    else:
        root_logger.setLevel(logging.WARNING)
    formatter = logging.Formatter(log_format, log_date_format)

    log_filename = log_file or log_file_2 or DEFAULT_LOG_FILE

    logfile_handler = logging.FileHandler(log_filename)
    logfile_handler.setFormatter(formatter)
    root_logger.addHandler(logfile_handler)


def do_cmd(cmd):
    """Abstracts out the basics of issuing system commands. If the command
    returns anything in stderr, a PluginError is raised with that information.
    Otherwise, the output from stdout is returned.
    """

    pipe = subprocess.PIPE
    logging.debug("Executing:%s", cmd)
    proc = subprocess.Popen(cmd, shell=False, stdin=pipe, stdout=pipe,
                            stderr=pipe, close_fds=True)
    ret_code = proc.wait()
    err = proc.stderr.read()
    if ret_code:
        logging.debug("The command exited with the error code: " +
                      "%s (stderr output:%s)" % (ret_code, err))
        raise PluginError(err)
    output = proc.stdout.read()
    if output.endswith('\n'):
        output = output[:-1]
    return output


def _is_process_run(pidFile, name):
    try:
        fpid = open(pidFile, "r")
        pid = fpid.readline()
        fpid.close()
    except IOError as e:
        return -1

    pid = pid[:-1]
    ps = os.popen("ps -ae")
    for l in ps:
        if pid in l and name in l:
            ps.close()
            return 0

    ps.close()
    return -2


def _is_tool_exist(name):
    if os.path.exists(name):
        return 0
    return -1


def check_switch():
    global result

    ret = _is_process_run(OVSDB_PID_PATH, OVSDB_DAEMON_PATH)
    if ret < 0:
        if ret == -1:
            return "NO_DB_PID_FILE"
        if ret == -2:
            return "DB_NOT_RUN"

    ret = _is_process_run(OVS_PID_PATH, OVS_DAEMON_PATH)
    if ret < 0:
        if ret == -1:
            return "NO_SWITCH_PID_FILE"
        if ret == -2:
            return "SWITCH_NOT_RUN"

    if _is_tool_exist(VSCTL_PATH) < 0:
        return "NO_VSCTL"

    if _is_tool_exist(OFCTL_PATH) < 0:
        return "NO_OFCTL"

    return "SUCCESS"


def _build_flow_expr(**kwargs):
    is_delete_expr = kwargs.get('delete', False)
    flow = ""
    if not is_delete_expr:
        flow = "hard_timeout=%s,idle_timeout=%s,priority=%s"\
                % (kwargs.get('hard_timeout', '0'),
                   kwargs.get('idle_timeout', '0'),
                   kwargs.get('priority', '1'))
    in_port = 'in_port' in kwargs and ",in_port=%s" % kwargs['in_port'] or ''
    dl_type = 'dl_type' in kwargs and ",dl_type=%s" % kwargs['dl_type'] or ''
    dl_src = 'dl_src' in kwargs and ",dl_src=%s" % kwargs['dl_src'] or ''
    dl_dst = 'dl_dst' in kwargs and ",dl_dst=%s" % kwargs['dl_dst'] or ''
    nw_src = 'nw_src' in kwargs and ",nw_src=%s" % kwargs['nw_src'] or ''
    nw_dst = 'nw_dst' in kwargs and ",nw_dst=%s" % kwargs['nw_dst'] or ''
    table = 'table' in kwargs and ",table=%s" % kwargs['table'] or ''
    proto = 'proto' in kwargs and ",%s" % kwargs['proto'] or ''
    ip = ('nw_src' in kwargs or 'nw_dst' in kwargs) and ',ip' or ''
    flow = (flow + in_port + dl_type + dl_src + dl_dst +
            (ip or proto) + nw_src + nw_dst)
    return flow


def add_flow(bridge, **kwargs):
    """
    Builds a flow expression for **kwargs and adds the flow entry
    to an Open vSwitch instance
    """
    flow = _build_flow_expr(**kwargs)
    actions = 'actions' in kwargs and ",actions=%s" % kwargs['actions'] or ''
    flow = flow + actions
    addflow = [OFCTL_PATH, "add-flow", bridge, flow]
    do_cmd(addflow)


def del_flows(bridge, **kwargs):
    """
    Removes flows according to criteria passed as keyword.
    """
    flow = _build_flow_expr(delete=True, **kwargs)
    # out_port condition does not exist for all flow commands
    out_port = ("out_port" in kwargs and
                ",out_port=%s" % kwargs['out_port'] or '')
    flow = flow + out_port
    delFlow = [OFCTL_PATH, 'del-flows', bridge, flow]
    do_cmd(delFlow)


def del_all_flows(bridge):
    delFlow = [OFCTL_PATH, "del-flows", bridge]
    do_cmd(delFlow)

    normalFlow = "priority=0 idle_timeout=0 hard_timeout=0 actions=normal"
    add_flow(bridge, normalFlow)


def del_port(bridge, port):
    delPort = [VSCTL_PATH, "del-port", bridge, port]
    do_cmd(delPort)


def get_network_id_for_vif(vif_name):
    domain_id, device_id = vif_name[3:len(vif_name)].split(".")
    dom_uuid = do_cmd([XE_PATH, "vm-list", "dom-id=%s" % domain_id, "--minimal"])
    vif_uuid = do_cmd([XE_PATH, "vif-list", "vm-uuid=%s" % dom_uuid, "device=%s" % device_id, "--minimal"])
    vnet = do_cmd([XE_PATH, "vif-param-get", "uuid=%s" % vif_uuid,  "param-name=other-config",
                             "param-key=cloudstack-network-id"])
    return vnet

def get_network_id_for_tunnel_port(tunnelif_name):
    vnet = do_cmd([VSCTL_PATH, "get", "interface", tunnelif_name, "options:cloudstack-network-id"])
    return vnet

def clear_flooding_rules_for_port(bridge, ofport):
        del_flows(bridge, in_port=ofport, table=2)

def add_flooding_rules_for_port(bridge, in_ofport, out_ofports):
        action = "".join("output:%s," %ofport for ofport in out_ofports)[:-1]
        add_flow(bridge, priority=1100, in_port=in_ofport, table=1, actions=action)

def get_ofport_for_vif(vif_name):
    return do_cmd([VSCTL_PATH, "get", "interface", vif_name, "ofport"])

def get_macaddress_of_vif(vif_name):
    domain_id, device_id = vif_name[3:len(vif_name)].split(".")
    dom_uuid = do_cmd([XE_PATH, "vm-list", "dom-id=%s" % domain_id, "--minimal"])
    vif_uuid = do_cmd([XE_PATH, "vif-list", "vm-uuid=%s" % dom_uuid, "device=%s" % device_id, "--minimal"])
    mac = do_cmd([XE_PATH, "vif-param-get", "uuid=%s" % vif_uuid,  "param-name=MAC"])
    return mac

def get_vif_name_from_macaddress(macaddress):
    vif_uuid = do_cmd([XE_PATH, "vif-list", "MAC=%s" % macaddress, "--minimal"])
    vif_device_id = do_cmd([XE_PATH, "vif-param-get", "uuid=%s" % vif_uuid,  "param-name=device"])
    vm_uuid = do_cmd([XE_PATH, "vif-param-get", "uuid=%s" % vif_uuid,  "param-name=vm-uuid"])
    vm_domain_id = do_cmd([XE_PATH, "vm-param-get", "uuid=%s" % vm_uuid,  "param-name=dom-id"])
    return "vif"+vm_domain_id+"."+vif_device_id

def add_mac_lookup_table_entry(bridge, mac_address, out_of_port):
    add_flow(bridge, priority=1100, dl_dst=mac_address, table=1, actions="output:%s" % out_of_port)

def delete_mac_lookup_table_entry(bridge, mac_address):
    del_flows(bridge, dl_dst=mac_address, table=1)

def add_ip_lookup_table_entry(bridge, ip, dst_tier_gateway_mac, dst_vm_mac):
    action_str = "mod_dl_sr:%s" % dst_tier_gateway_mac + ",mod_dl_dst:%s" % dst_vm_mac +",resubmit(,5)"
    addflow = [OFCTL_PATH, "add-flow", bridge, "table=4", "nw_dst=%s" % ip, "actions=%s" %action_str]
    do_cmd(addflow)

def get_vms_on_host(vpc, host_id):
    all_vms = vpc.vms
    vms_on_host = []
    for vm in all_vms:
      if vm.hostid == host_id:
        vms_on_host.append(vm)
    return vms_on_host

def get_network_details(vpc, network_uuid):
    tiers = vpc.tiers
    for tier in tiers:
      if tier.networkuuid == network_uuid:
        return tier
    return None

class jsonLoader(object):
  def __init__(self, obj):
        for k in obj:
            v = obj[k]
            if isinstance(v, dict):
                setattr(self, k, jsonLoader(v))
            elif isinstance(v, (list, tuple)):
                if len(v) > 0 and isinstance(v[0], dict):
                    setattr(self, k, [jsonLoader(elem) for elem in v])
                else:
                    setattr(self, k, v)
            else:
                setattr(self, k, v)

  def __getattr__(self, val):
        if val in self.__dict__:
            return self.__dict__[val]
        else:
            return None

  def __repr__(self):
        return '{%s}' % str(', '.join('%s : %s' % (k, repr(v)) for (k, v)
                                      in self.__dict__.items()))

  def __str__(self):
        return '{%s}' % str(', '.join('%s : %s' % (k, repr(v)) for (k, v)
                                      in self.__dict__.items()))

def configure_bridge_for_network_topology(bridge, this_host_id, json_config):
    vpconfig = jsonLoader(json.loads(json_config)).vpc

    if vpconfig is None:
        logging.debug("WARNING:Can't find VPC info in json config file")
        return "FAILURE:IMPROPER_JSON_CONFG_FILE"

    # get the list of Vm's in the VPC from the JSON config
    this_host_vms = get_vms_on_host(vpconfig, this_host_id)

    for vm in this_host_vms:
        for nic in vm.nics:
            mac_addr = nic.macaddress
            ip = nic.ipaddress
            vif_name = get_vif_name_from_macaddress(mac_addr)
            of_port = get_ofport_for_vif(vif_name)
            network = get_network_details(vpconfig, nic.networkuuid)

            # Add flow rule in L2 look up table, if the destination mac = MAC of the nic send packet on the found OFPORT
            add_mac_lookup_table_entry(bridge, mac_addr, of_port)

            # Add flow rule in L3 look up table: if the destination IP = VM's IP then modify the packet
            # to set DST MAC = VM's MAC, SRC MAC=tier gateway MAC and send to egress table
            add_ip_lookup_table_entry(bridge, ip, network.gatewaymac, mac_addr)

            # Add flow entry to send with intra tier traffic from the NIC to L2 lookup path)
            addflow = [OFCTL_PATH, "add-flow", bridge, "table=0", "in_port=%s" % of_port,
                       "nw_dst=%s" %network.cidr, "actions=resubmit(,1)"]
            do_cmd(addflow)

            #add flow entry to send inter-tier traffic from the NIC to egress ACL table(to L3 lookup path)
            addflow = [OFCTL_PATH, "add-flow", bridge, "table=0", "in_port=%s" % of_port,
                       "dl_dst=%s" %network.gatewaymac, "nw_dst=%s" %vpconfig.cidr, "actions=resubmit(,3)"]
            do_cmd(addflow)

    # get the list of hosts on which VPC spans from the JSON config
    vpc_spanning_hosts = vpconfig.hosts

    for host in vpc_spanning_hosts:
        if this_host_id == host.hostid:
            continue
        other_host_vms = get_vms_on_host(vpconfig, host.hostid)
        for vm in other_host_vms:
            for nic in vm.nics:
                mac_addr = nic.macaddress
                ip = nic.ipaddress
                network = get_network_details(vpconfig, nic.networkuuid)
                gre_key = network.grekey

                # generate tunnel name from tunnel naming convention
                tunnel_name = "t%s-%s-%s" % (gre_key, this_host_id, host.hostid)
                of_port = get_ofport_for_vif(tunnel_name)

                # Add flow rule in L2 look up table, if the destination mac = MAC of the nic send packet tunnel port
                add_mac_lookup_table_entry(bridge, mac_addr, of_port)

                # Add flow tule in L3 look up table: if the destination IP = VM's IP then modify the packet
                # set DST MAC = VM's MAC, SRC MAC=tier gateway MAC and send to egress table
                add_ip_lookup_table_entry(bridge, ip, network.gatewaymac, mac_addr)

    return "SUCCESS: successfully configured bridge as per the VPC topology"

def get_acl(vpcconfig, required_acl_id):
    acls = vpcconfig.acls
    for acl in acls:
        if acl.id == required_acl_id:
            return acl
    return None

def configure_ovs_bridge_for_routing_policies(bridge, json_config):
    vpconfig = jsonLoader(json.loads(json_config)).vpc

    if vpconfig is None:
        logging.debug("WARNING:Can't find VPC info in json config file")
        return "FAILURE:IMPROPER_JSON_CONFG_FILE"

    # First flush current egress ACL's before re-applying the ACL's
    del_flows(bridge, table=3)

    egress_rules_added = False
    ingress_rules_added = False

    tiers = vpconfig.tiers
    for tier in tiers:
        tier_cidr = tier.cidr
        acl = get_acl(vpconfig, tier.aclid)
        acl_items = acl.aclitems

        for acl_item in acl_items:
            number = acl_item.number
            action = acl_item.action
            direction = acl_item.direction
            source_port_start = acl_item.sourceportstart
            source_port_end = acl_item.sourceportend
            protocol = acl_item.protocol
            source_cidrs = acl_item.sourcecidrs
            acl_priority = 1000 + number
            for source_cidr in source_cidrs:
                if direction is "ingress":
                    ingress_rules_added = True
                    # add flow rule to do action (allow/deny) for flows where source IP of the packet is in
                    # source_cidr and destination ip is in tier_cidr
                    port = source_port_start
                    while (port < source_port_end):
                        if action is "deny":
                            add_flow(bridge, priority= acl_priority, table=5, nw_src=source_cidr, nw_dst=tier_cidr, tp_dst=port,
                                     nw_proto=protocol, actions='drop')
                        if action is "allow":
                            add_flow(bridge, priority= acl_priority,table=5, nw_src=source_cidr, nw_dst=tier_cidr, tp_dst=port,
                                     nw_proto=protocol, actions='resubmit(,1)')
                        port = port + 1

                elif direction in "egress":
                    egress_rules_added = True
                    # add flow rule to do action (allow/deny) for flows where destination IP of the packet is in
                    # source_cidr and source ip is in tier_cidr
                    port = source_port_start
                    while (port < source_port_end):
                        if action is "deny":
                            add_flow(bridge, priority= acl_priority, table=5, nw_src=tier_cidr, nw_dst=source_cidr, tp_dst=port,
                                     nw_proto=protocol, actions='drop')
                        if action is "allow":
                            add_flow(bridge, priority= acl_priority, table=5, nw_src=tier_cidr, nw_dst=source_cidr, tp_dst=port,
                                     nw_proto=protocol, actions='resubmit(,1)')
                        port = port + 1

    if egress_rules_added is False:
        # add a default rule in egress table to forward packet to L3 lookup table
        add_flow(bridge, priority=0, table=3, actions='resubmit(,4)')

    if ingress_rules_added is False:
        # add a default rule in egress table drop packets
        add_flow(bridge, priority=0, table=5, actions='drop')
