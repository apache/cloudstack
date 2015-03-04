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

# Common function for Cloudstack's XenAPI plugins

import ConfigParser
import logging
import os
import subprocess
import simplejson as json
import copy

from time import localtime, asctime

DEFAULT_LOG_FORMAT = "%(asctime)s %(levelname)8s [%(name)s] %(message)s"
DEFAULT_LOG_DATE_FORMAT = "%Y-%m-%d %H:%M:%S"
DEFAULT_LOG_FILE = "/var/log/cloudstack_plugins.log"

PLUGIN_CONFIG_PATH = "/etc/xensource/cloudstack_plugins.conf"
OVSDB_PID_PATH = "/var/run/openvswitch/ovsdb-server.pid"
OVSDB_DAEMON_PATH = "ovsdb-server"
OVS_PID_PATH = "/var/run/openvswitch/ovs-vswitchd.pid"
OVS_DAEMON_PATH = "ovs-vswitchd"
VSCTL_PATH = "/usr/bin/ovs-vsctl"
OFCTL_PATH = "/usr/bin/ovs-ofctl"
XE_PATH = "/opt/xensource/bin/xe"

# OpenFlow tables set in a pipeline processing fashion for the bridge created for a VPC's that are enabled for
# distributed routing.
# L2 path (intra-tier traffic)  CLASSIFIER-> L2 lookup -> L2 flooding tables
# L3 path (inter-tier traffic)  CLASSIFIER-> EGRESS ACL -> L3 lookup -> INGRESS ACL-> L2 lookup -> L2 flooding tables

# Classifier table has the rules to separate broadcast/multi-cast traffic, inter-tier traffic, intra-tier traffic
CLASSIFIER_TABLE=0
# Lookup table to determine the output port (vif/tunnel port) based on the MAC address
L2_LOOKUP_TABLE=1
# flooding table has the rules to flood on ports (both VIF, tunnel ports) except on the port on which packet arrived
L2_FLOOD_TABLE=2
# table has flow rules derived from egress ACL's
EGRESS_ACL_TABLE=3
# Lookup table to determine the output port (vif/tunnel port) based on the IP address
L3_LOOKUP_TABLE=4
# table has flow rules derived from egress ACL's
INGRESS_ACL_TABLE=5

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
        config = ConfigParser.ConfigParser()
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
        except ConfigParser.NoSectionError:
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
    except IOError, e:
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
    cookie = 'cookie' in kwargs and ",cookie=%s" % kwargs['cookie'] or ''
    proto = 'proto' in kwargs and ",%s" % kwargs['proto'] or ''
    ip = ('nw_src' in kwargs or 'nw_dst' in kwargs) and ',ip' or ''
    flow = (flow + cookie+ in_port + dl_type + dl_src + dl_dst +
            (ip or proto) + nw_src + nw_dst + table)
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
    hostname = do_cmd(["/bin/bash", "-c", "hostname"])
    this_host_uuid = do_cmd([XE_PATH, "host-list", "hostname=%s" % hostname, "--minimal"])
    dom_uuid = do_cmd([XE_PATH, "vm-list", "dom-id=%s" % domain_id, "resident-on=%s" %this_host_uuid, "--minimal"])
    vif_uuid = do_cmd([XE_PATH, "vif-list", "vm-uuid=%s" % dom_uuid, "device=%s" % device_id, "--minimal"])
    vnet = do_cmd([XE_PATH, "vif-param-get", "uuid=%s" % vif_uuid,  "param-name=other-config",
                             "param-key=cloudstack-network-id"])
    return vnet

def get_network_id_for_tunnel_port(tunnelif_name):
    vnet = do_cmd([VSCTL_PATH, "get", "interface", tunnelif_name, "options:cloudstack-network-id"])
    return vnet

def clear_flooding_rules_for_port(bridge, ofport):
        del_flows(bridge, in_port=ofport, table=L2_FLOOD_TABLE)

def clear_flooding_rules_for_all_ports(bridge):
        del_flows(bridge, cookie=111, table=L2_FLOOD_TABLE)

def add_flooding_rules_for_port(bridge, in_ofport, out_ofports):
        action = "".join("output:%s," %ofport for ofport in out_ofports)[:-1]
        add_flow(bridge, cookie=111, priority=1100, in_port=in_ofport, table=L2_FLOOD_TABLE, actions=action)

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
    action = "output=%s" %out_of_port
    add_flow(bridge, priority=1100, dl_dst=mac_address, table=L2_LOOKUP_TABLE, actions=action)

def delete_mac_lookup_table_entry(bridge, mac_address):
    del_flows(bridge, dl_dst=mac_address, table=L2_LOOKUP_TABLE)

def add_ip_lookup_table_entry(bridge, ip, dst_tier_gateway_mac, dst_vm_mac):
    action_str = "mod_dl_src:%s" % dst_tier_gateway_mac + ",mod_dl_dst:%s" % dst_vm_mac + ",resubmit(,%s)"%INGRESS_ACL_TABLE
    action_str = "table=%s"%L3_LOOKUP_TABLE + ", ip, nw_dst=%s" % ip + ",  actions=%s" %action_str
    addflow = [OFCTL_PATH, "add-flow", bridge, action_str]
    do_cmd(addflow)

def get_vpc_vms_on_host(vpc, host_id):
    all_vms = vpc.vms
    vms_on_host = []
    for vm in all_vms:
      if str(vm.hostid) == (host_id):
        vms_on_host.append(vm)
    return vms_on_host

def get_network_details(vpc, network_uuid):
    tiers = vpc.tiers
    for tier in tiers:
      if str(tier.networkuuid) == (network_uuid):
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
                                      in self.__dict__.iteritems()))

  def __str__(self):
        return '{%s}' % str(', '.join('%s : %s' % (k, repr(v)) for (k, v)
                                      in self.__dict__.iteritems()))
def get_acl(vpcconfig, required_acl_id):
    acls = vpcconfig.acls
    for acl in acls:
        if acl.id == required_acl_id:
            return acl
    return None

def check_tunnel_exists(bridge, tunnel_name):
    try:
        res = do_cmd([VSCTL_PATH, "port-to-br", tunnel_name])
        return res == bridge
    except:
        return False

def create_tunnel(bridge, remote_ip, gre_key, src_host, dst_host, network_uuid):

    logging.debug("Creating tunnel from host %s" %src_host + " to host %s" %dst_host + " with GRE key %s" %gre_key)

    res = check_switch()
    if res != "SUCCESS":
        logging.debug("Openvswitch running: NO")
        return "FAILURE:%s" % res

    # We need to keep the name below 14 characters
    # src and target are enough - consider a fixed length hash
    name = "t%s-%s-%s" % (gre_key, src_host, dst_host)

    # Verify the xapi bridge to be created
    # NOTE: Timeout should not be necessary anymore
    wait = [VSCTL_PATH, "--timeout=30", "wait-until", "bridge",
                    bridge, "--", "get", "bridge", bridge, "name"]
    res = do_cmd(wait)
    if bridge not in res:
        logging.debug("WARNING:Can't find bridge %s for creating " +
                                  "tunnel!" % bridge)
        return "FAILURE:NO_BRIDGE"
    logging.debug("bridge %s for creating tunnel - VERIFIED" % bridge)
    tunnel_setup = False
    drop_flow_setup = False
    try:
        # Create a port and configure the tunnel interface for it
        add_tunnel = [VSCTL_PATH, "add-port", bridge,
                                  name, "--", "set", "interface",
                                  name, "type=gre", "options:key=%s" % gre_key,
                                  "options:remote_ip=%s" % remote_ip]
        do_cmd(add_tunnel)
        tunnel_setup = True
        # verify port
        verify_port = [VSCTL_PATH, "get", "port", name, "interfaces"]
        res = do_cmd(verify_port)
        # Expecting python-style list as output
        iface_list = []
        if len(res) > 2:
            iface_list = res.strip()[1:-1].split(',')
        if len(iface_list) != 1:
            logging.debug("WARNING: Unexpected output while verifying " +
                                      "port %s on bridge %s" % (name, bridge))
            return "FAILURE:VERIFY_PORT_FAILED"

        # verify interface
        iface_uuid = iface_list[0]
        verify_interface_key = [VSCTL_PATH, "get", "interface",
                                iface_uuid, "options:key"]
        verify_interface_ip = [VSCTL_PATH, "get", "interface",
                               iface_uuid, "options:remote_ip"]

        key_validation = do_cmd(verify_interface_key)
        ip_validation = do_cmd(verify_interface_ip)

        if not gre_key in key_validation or not remote_ip in ip_validation:
            logging.debug("WARNING: Unexpected output while verifying " +
                          "interface %s on bridge %s" % (name, bridge))
            return "FAILURE:VERIFY_INTERFACE_FAILED"
        logging.debug("Tunnel interface validated:%s" % verify_interface_ip)
        cmd_tun_ofport = [VSCTL_PATH, "get", "interface",
                                          iface_uuid, "ofport"]
        tun_ofport = do_cmd(cmd_tun_ofport)
        # Ensure no trailing LF
        if tun_ofport.endswith('\n'):
            tun_ofport = tun_ofport[:-1]
        # find xs network for this bridge, verify is used for ovs tunnel network
        xs_nw_uuid = do_cmd([XE_PATH, "network-list",
								   "bridge=%s" % bridge, "--minimal"])

        ovs_tunnel_network = is_regular_tunnel_network(xs_nw_uuid)
        ovs_vpc_distributed_vr_network = is_vpc_network_with_distributed_routing(xs_nw_uuid)

        if ovs_tunnel_network == 'True':
            # add flow entryies for dropping broadcast coming in from gre tunnel
            add_flow(bridge, priority=1000, in_port=tun_ofport,
                         dl_dst='ff:ff:ff:ff:ff:ff', actions='drop')
            add_flow(bridge, priority=1000, in_port=tun_ofport,
                     nw_dst='224.0.0.0/24', actions='drop')
            drop_flow_setup = True
            logging.debug("Broadcast drop rules added")

        if ovs_vpc_distributed_vr_network == 'True':
            # add flow rules for dropping broadcast coming in from tunnel ports
            add_flow(bridge, priority=1000, in_port=tun_ofport, table=0,
                         dl_dst='ff:ff:ff:ff:ff:ff', actions='drop')
            add_flow(bridge, priority=1000, in_port=tun_ofport, table=0,
                     nw_dst='224.0.0.0/24', actions='drop')

            # add flow rule to send the traffic from tunnel ports to L2 switching table only
            add_flow(bridge, priority=1100, in_port=tun_ofport, table=0, actions='resubmit(,1)')

            # mark tunnel interface with network id for which this tunnel was created
            do_cmd([VSCTL_PATH, "set", "interface", name, "options:cloudstack-network-id=%s" % network_uuid])
            update_flooding_rules_on_port_plug_unplug(bridge, name, 'online', network_uuid)

        logging.debug("Successfully created tunnel from host %s" %src_host + " to host %s" %dst_host +
                      " with GRE key %s" %gre_key)
        return "SUCCESS:%s" % name
    except:
        logging.debug("An unexpected error occured. Rolling back")
        if tunnel_setup:
            logging.debug("Deleting GRE interface")
            # Destroy GRE port and interface
            del_port(bridge, name)
        if drop_flow_setup:
            # Delete flows
            logging.debug("Deleting flow entries from GRE interface")
            del_flows(bridge, in_port=tun_ofport)
        # This will not cancel the original exception
        raise

# Configures the bridge created for a VPC that is enabled for distributed routing. Management server sends VPC
# physical topology details (which VM from which tier running on which host etc). Based on the VPC physical topology L2
# lookup table and L3 lookup tables are updated by this function.
def configure_vpc_bridge_for_network_topology(bridge, this_host_id, json_config, sequence_no):

    vpconfig = jsonLoader(json.loads(json_config)).vpc
    if vpconfig is None:
        logging.debug("WARNING:Can't find VPC topology information in the json configuration file")
        return "FAILURE:IMPROPER_JSON_CONFG_FILE"

    try:
        if not os.path.exists('/var/run/cloud'):
            os.makedirs('/var/run/cloud')

        # create a temporary file to store OpenFlow rules corresponding to L2 and L3 lookup table updates
        ofspec_filename = "/var/run/cloud/" + bridge + sequence_no + ".ofspec"
        ofspec = open(ofspec_filename, 'w+')

        # get the list of VM's in all the tiers of VPC running in this host from the JSON config
        this_host_vms = get_vpc_vms_on_host(vpconfig, this_host_id)

        for vm in this_host_vms:
            for nic in vm.nics:
                mac_addr = nic.macaddress
                ip = nic.ipaddress
                vif_name = get_vif_name_from_macaddress(mac_addr)
                of_port  = get_ofport_for_vif(vif_name)
                network  = get_network_details(vpconfig, nic.networkuuid)

                # Add OF rule in L2 look up table, if packet's destination mac matches MAC of the VM's nic
                # then send packet on the found OFPORT
                ofspec.write("table=%s" %L2_LOOKUP_TABLE + " priority=1100 dl_dst=%s " %mac_addr +
                             " actions=output:%s" %of_port + "\n")

                # Add OF rule in L3 look up table: if packet's destination IP matches VM's IP then modify the packet
                # to set DST MAC = VM's MAC, SRC MAC= destination tier gateway MAC and send to egress table. This step
                # emulates steps VPC virtual router would have done on the current host itself.
                action_str = " mod_dl_src:%s"%network.gatewaymac + ",mod_dl_dst:%s" % mac_addr \
                             + ",resubmit(,%s)"%INGRESS_ACL_TABLE
                action_str = "table=%s"%L3_LOOKUP_TABLE + " ip nw_dst=%s"%ip + " actions=%s" %action_str
                ofspec.write(action_str + "\n")

                # Add OF rule to send intra-tier traffic from this nic of the VM to L2 lookup path (L2 switching)
                action_str = "table=%s" %CLASSIFIER_TABLE + " priority=1200 in_port=%s " %of_port + \
                             " ip nw_dst=%s " %network.cidr + " actions=resubmit(,%s)" %L2_LOOKUP_TABLE
                ofspec.write(action_str + "\n")

                # Add OF rule to send inter-tier traffic from this nic of the VM to egress ACL table(L3 lookup path)
                action_str = "table=%s "%CLASSIFIER_TABLE + " priority=1100 in_port=%s " %of_port +\
                             " ip dl_dst=%s " %network.gatewaymac + " nw_dst=%s " %vpconfig.cidr + \
                             " actions=resubmit(,%s)" %EGRESS_ACL_TABLE
                ofspec.write(action_str + "\n")

        # get the list of hosts on which VPC spans from the JSON config
        vpc_spanning_hosts = vpconfig.hosts

        for host in vpc_spanning_hosts:
            if str(this_host_id) == str(host.hostid):
                continue

            other_host_vms = get_vpc_vms_on_host(vpconfig, str(host.hostid))

            for vm in other_host_vms:
                for nic in vm.nics:
                    mac_addr = nic.macaddress
                    ip = nic.ipaddress
                    network = get_network_details(vpconfig, nic.networkuuid)
                    gre_key = network.grekey

                    # generate tunnel name as per the tunnel naming convention
                    tunnel_name = "t%s-%s-%s" % (gre_key, this_host_id, host.hostid)

                    # check if tunnel exists already, if not create a tunnel from this host to remote host
                    if not check_tunnel_exists(bridge, tunnel_name):
                        create_tunnel(bridge, str(host.ipaddress), str(gre_key), this_host_id,
                                      host.hostid, network.networkuuid)

                    of_port = get_ofport_for_vif(tunnel_name)

                    # Add flow rule in L2 look up table, if packet's destination mac matches MAC of the VM's nic
                    # on the remote host then send packet on the found OFPORT corresponding to the tunnel
                    ofspec.write("table=%s" %L2_LOOKUP_TABLE + " priority=1100 dl_dst=%s " %mac_addr +
                                 " actions=output:%s" %of_port + "\n")

                    # Add flow rule in L3 look up table. if packet's destination IP matches VM's IP then modify the
                    # packet to set DST MAC = VM's MAC, SRC MAC=tier gateway MAC and send to ingress table. This step
                    # emulates steps VPC virtual router would have done on the current host itself.
                    action_str = "mod_dl_src:%s"%network.gatewaymac + ",mod_dl_dst:%s" % mac_addr + \
                                 ",resubmit(,%s)"%INGRESS_ACL_TABLE
                    action_str = "table=%s"%L3_LOOKUP_TABLE + " ip nw_dst=%s"%ip + " actions=%s" %action_str
                    ofspec.write(action_str + "\n")

        # add a default rule in L2_LOOKUP_TABLE to send unknown mac address to L2 flooding table
        ofspec.write("table=%s "%L2_LOOKUP_TABLE + " priority=0 " + " actions=resubmit(,%s)"%L2_FLOOD_TABLE + "\n")

        # add a default rule in L3 lookup table to forward (unknown destination IP) packets to L2 lookup table. This
        # is fallback option to send the packet to VPC VR, when routing can not be performed at the host
        ofspec.write("table=%s "%L3_LOOKUP_TABLE + " priority=0 " + " actions=resubmit(,%s)"%L2_LOOKUP_TABLE + "\n")

        # First flush current L2_LOOKUP_TABLE & L3_LOOKUP_TABLE before re-applying L2 & L3 lookup entries
        del_flows(bridge, table=L2_LOOKUP_TABLE)
        del_flows(bridge, table=L3_LOOKUP_TABLE)

        ofspec.seek(0)
        logging.debug("Adding below flows rules in L2 & L3 lookup tables:\n" + ofspec.read())
        ofspec.close()

        # update bridge with the flow-rules for L2 lookup and L3 lookup in the file in one attempt
        do_cmd([OFCTL_PATH, 'add-flows', bridge, ofspec_filename])

        # now that we updated the bridge with flow rules close and delete the file.
        os.remove(ofspec_filename)

        return "SUCCESS: successfully configured bridge as per the VPC topology update with sequence no: %s"%sequence_no

    except Exception,e:
        error_message = "An unexpected error occurred while configuring bridge " + bridge + \
                        " as per latest VPC topology update with sequence no: %s" %sequence_no
        logging.debug(error_message + " due to " + str(e))
        if os.path.isfile(ofspec_filename):
            os.remove(ofspec_filename)
        raise error_message

# Configures the bridge created for a VPC that is enabled for distributed firewall. Management server sends VPC routing
# policy (network ACL applied on the tiers etc) details. Based on the VPC routing policies ingress ACL table and
# egress ACL tables are updated by this function.
def configure_vpc_bridge_for_routing_policies(bridge, json_config, sequence_no):

    vpconfig = jsonLoader(json.loads(json_config)).vpc
    if vpconfig is None:
        logging.debug("WARNING: Can't find VPC routing policies info in json config file")
        return "FAILURE:IMPROPER_JSON_CONFG_FILE"

    try:

        if not os.path.exists('/var/run/cloud'):
            os.makedirs('/var/run/cloud')

        # create a temporary file to store OpenFlow rules corresponding to ingress and egress ACL table updates
        ofspec_filename = "/var/run/cloud/" + bridge + sequence_no + ".ofspec"
        ofspec = open(ofspec_filename, 'w+')

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
                if protocol == "all":
                    protocol = "*"
                elif protocol == "tcp":
                    protocol = "6"
                elif protocol == "udp":
                    protocol == "17"
                elif protocol == "icmp":
                    protocol == "1"
                source_cidrs = acl_item.sourcecidrs
                acl_priority = 1000 + number
                if direction == "ingress":
                    matching_table = INGRESS_ACL_TABLE
                    resubmit_table = L2_LOOKUP_TABLE
                elif direction == "egress":
                    matching_table = EGRESS_ACL_TABLE
                    resubmit_table = L3_LOOKUP_TABLE

                for source_cidr in source_cidrs:
                    if source_port_start is None and source_port_end is None:
                        if source_cidr.startswith('0.0.0.0'):
                            if action == "deny":
                                if direction == "ingress":
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " nw_dst=%s " %tier_cidr + " nw_proto=%s " %protocol +
                                                 " actions=drop" + "\n")
                                else:
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " nw_src=%s " %tier_cidr + " nw_proto=%s " %protocol +
                                                 " actions=drop" + "\n")
                            if action == "allow":
                                if direction == "ingress":
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " nw_dst=%s " %tier_cidr + " nw_proto=%s " %protocol +
                                                 " actions=resubmit(,%s)"%resubmit_table + "\n")
                                else:
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " nw_src=%s " %tier_cidr + " nw_proto=%s " %protocol +
                                                 " actions=resubmit(,%s)"%resubmit_table + "\n")
                        else:
                            if action == "deny":
                                if direction == "ingress":
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " nw_src=%s " %source_cidr + " nw_dst=%s " %tier_cidr +
                                                 " nw_proto=%s " %protocol + " actions=drop" + "\n")
                                else:
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " nw_src=%s " %tier_cidr + " nw_dst=%s " %source_cidr +
                                                 " nw_proto=%s " %protocol + " actions=drop" + "\n")
                            if action == "allow":
                                if direction == "ingress":
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " nw_src=%s "%source_cidr + " nw_dst=%s " %tier_cidr +
                                                 " nw_proto=%s " %protocol +
                                                 " actions=resubmit(,%s)"%resubmit_table  + "\n")
                                else:
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " nw_src=%s "%tier_cidr + " nw_dst=%s " %source_cidr +
                                                 " nw_proto=%s " %protocol +
                                                 " actions=resubmit(,%s)"%resubmit_table  + "\n")
                        continue

                    # add flow rule to do action (allow/deny) for flows where source IP of the packet is in
                    # source_cidr and destination ip is in tier_cidr
                    port = int(source_port_start)
                    while (port <= int(source_port_end)):
                        if source_cidr.startswith('0.0.0.0'):
                            if action == "deny":
                                if direction == "ingress":
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " tp_dst=%s " %port + " nw_dst=%s " %tier_cidr +
                                                 " nw_proto=%s " %protocol + " actions=drop"  + "\n")
                                else:
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " tp_dst=%s " %port + " nw_src=%s " %tier_cidr +
                                                 " nw_proto=%s " %protocol + " actions=drop" + "\n")
                            if action == "allow":
                                if direction == "ingress":
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " tp_dst=%s " %port + " nw_dst=%s " %tier_cidr +
                                                 " nw_proto=%s " %protocol +
                                                 " actions=resubmit(,%s)"%resubmit_table + "\n")
                                else:
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " tp_dst=%s " %port + " nw_src=%s " %tier_cidr +
                                                 " nw_proto=%s " %protocol +
                                                 " actions=resubmit(,%s)"%resubmit_table + "\n")
                        else:
                            if action == "deny":
                                if direction == "ingress":
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " tp_dst=%s " %port + " nw_src=%s " %source_cidr +
                                                 " nw_dst=%s " %tier_cidr +
                                                 " nw_proto=%s " %protocol + " actions=drop" + "\n")
                                else:
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " tp_dst=%s " %port + " nw_src=%s " %tier_cidr +
                                                 " nw_dst=%s " %source_cidr +
                                                 " nw_proto=%s " %protocol + " actions=drop" + "\n")
                            if action == "allow":
                                if direction == "ingress":
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " tp_dst=%s " %port + " nw_src=%s "%source_cidr +
                                                 " nw_dst=%s " %tier_cidr +
                                                 " nw_proto=%s " %protocol +
                                                 " actions=resubmit(,%s)"%resubmit_table  + "\n")
                                else:
                                    ofspec.write("table=%s "%matching_table + " priority=%s " %acl_priority + " ip " +
                                                 " tp_dst=%s " %port + " nw_src=%s "%tier_cidr +
                                                 " nw_dst=%s " %source_cidr +
                                                 " nw_proto=%s " %protocol +
                                                 " actions=resubmit(,%s)"%resubmit_table  + "\n")
                        port = port + 1

        # add a default rule in egress table to allow packets (so forward packet to L3 lookup table)
        ofspec.write("table=%s " %EGRESS_ACL_TABLE + " priority=0 actions=resubmit(,%s)" %L3_LOOKUP_TABLE + "\n")

        # add a default rule in ingress table to drop packets
        ofspec.write("table=%s " %INGRESS_ACL_TABLE + " priority=0 actions=drop" + "\n")

        # First flush current ingress and egress ACL's before re-applying the ACL's
        del_flows(bridge, table=EGRESS_ACL_TABLE)
        del_flows(bridge, table=INGRESS_ACL_TABLE)

        ofspec.seek(0)
        logging.debug("Adding below flows rules Ingress & Egress ACL tables:\n" + ofspec.read())
        ofspec.close()

        # update bridge with the flow-rules for ingress and egress ACL's added in the file in one attempt
        do_cmd([OFCTL_PATH, 'add-flows', bridge, ofspec_filename])

        # now that we updated the bridge with flow rules delete the file.
        os.remove(ofspec_filename)

        return "SUCCESS: successfully configured bridge as per the latest routing policies update with " \
               "sequence no: %s"%sequence_no

    except Exception,e:
        error_message = "An unexpected error occurred while configuring bridge " + bridge + \
                        " as per latest VPC's routing policy update with sequence number %s." %sequence_no
        logging.debug(error_message + " due to " + str(e))
        if os.path.isfile(ofspec_filename):
            os.remove(ofspec_filename)
        raise error_message

# configures bridge L2 flooding rules stored in table=2. Single bridge is used for all the tiers of VPC. So controlled
# flooding is required to restrict the broadcast to only to the ports (vifs and tunnel interfaces) in the tier. Also
# packets arrived from the tunnel ports should not be flooded on the other tunnel ports.
def update_flooding_rules_on_port_plug_unplug(bridge, interface, command, if_network_id):

    class tier_ports:
        tier_vif_ofports = []
        tier_tunnelif_ofports = []
        tier_all_ofports = []

    logging.debug("Updating the flooding rules on bridge " + bridge + " as interface  %s" %interface +
                  " is %s"%command + " now.")
    try:

        if not os.path.exists('/var/run/cloud'):
            os.makedirs('/var/run/cloud')

        # create a temporary file to store OpenFlow rules corresponding L2 flooding table
        ofspec_filename = "/var/run/cloud/" + bridge + "-" +interface + "-" + command + ".ofspec"
        ofspec = open(ofspec_filename, 'w+')

        all_tiers = dict()

        vsctl_output = do_cmd([VSCTL_PATH, 'list-ports', bridge])
        ports = vsctl_output.split('\n')

        for port in ports:

            if_ofport = do_cmd([VSCTL_PATH, 'get', 'Interface', port, 'ofport'])

            if port.startswith('vif'):
                network_id = get_network_id_for_vif(port)
                if network_id not in all_tiers.keys():
                    all_tiers[network_id] = tier_ports()
                tier_ports_info = all_tiers[network_id]
                tier_ports_info.tier_vif_ofports.append(if_ofport)
                tier_ports_info.tier_all_ofports.append(if_ofport)
                all_tiers[network_id] = tier_ports_info

            if port.startswith('t'):
                network_id = get_network_id_for_tunnel_port(port)[1:-1]
                if network_id not in all_tiers.keys():
                    all_tiers[network_id] = tier_ports()
                tier_ports_info = all_tiers[network_id]
                tier_ports_info.tier_tunnelif_ofports.append(if_ofport)
                tier_ports_info.tier_all_ofports.append(if_ofport)
                all_tiers[network_id] = tier_ports_info

        for network_id, tier_ports_info in all_tiers.items():
            if len(tier_ports_info.tier_all_ofports) == 1 :
                continue

            # for a packet arrived from tunnel port, flood only on to VIF ports connected to bridge for this tier
            for port in tier_ports_info.tier_tunnelif_ofports:
                action = "".join("output:%s," %ofport for ofport in tier_ports_info.tier_vif_ofports)[:-1]
                ofspec.write("table=%s " %L2_FLOOD_TABLE + " priority=1100 in_port=%s " %port +
                             "actions=%s " %action + "\n")

            # for a packet arrived from VIF port send on all VIF and tunnel ports corresponding to the tier excluding
            # the port on which packet arrived
            for port in tier_ports_info.tier_vif_ofports:
                tier_all_ofports_copy = copy.copy(tier_ports_info.tier_all_ofports)
                tier_all_ofports_copy.remove(port)
                action = "".join("output:%s," %ofport for ofport in tier_all_ofports_copy)[:-1]
                ofspec.write("table=%s " %L2_FLOOD_TABLE + " priority=1100 in_port=%s " %port +
                             "actions=%s " %action + "\n")

        # add a default rule in L2 flood table to drop packet
        ofspec.write("table=%s " %L2_FLOOD_TABLE + " priority=0 actions=drop")

        # First flush current L2 flooding table before re-populating the tables
        del_flows(bridge, table=L2_FLOOD_TABLE)

        ofspec.seek(0)
        logging.debug("Adding below flows rules L2 flooding table: \n" + ofspec.read())
        ofspec.close()

        # update bridge with the flow-rules for broadcast rules added in the file in one attempt
        do_cmd([OFCTL_PATH, 'add-flows', bridge, ofspec_filename])

        # now that we updated the bridge with flow rules delete the file.
        os.remove(ofspec_filename)

        logging.debug("successfully configured bridge %s as per the latest flooding rules " %bridge)

    except Exception,e:
        if os.path.isfile(ofspec_filename):
            os.remove(ofspec_filename)
        error_message = "An unexpected error occurred while updating the flooding rules for the bridge " + \
                        bridge + " when interface " + " %s" %interface + " is %s" %command
        logging.debug(error_message + " due to " + str(e))
        raise error_message


def is_regular_tunnel_network(xs_nw_uuid):
    cmd = [XE_PATH,"network-param-get", "uuid=%s" % xs_nw_uuid, "param-name=other-config",
                            "param-key=is-ovs-tun-network", "--minimal"]
    logging.debug("Executing:%s", cmd)
    pipe = subprocess.PIPE
    proc = subprocess.Popen(cmd, shell=False, stdin=pipe, stdout=pipe,
                            stderr=pipe, close_fds=True)
    ret_code = proc.wait()
    if ret_code:
        return False

    output = proc.stdout.read()
    if output.endswith('\n'):
        output = output[:-1]
    return output


def is_vpc_network_with_distributed_routing(xs_nw_uuid):
    cmd = [XE_PATH,"network-param-get", "uuid=%s" % xs_nw_uuid, "param-name=other-config",
                            "param-key=is-ovs-vpc-distributed-vr-network", "--minimal"]
    logging.debug("Executing:%s", cmd)
    pipe = subprocess.PIPE
    proc = subprocess.Popen(cmd, shell=False, stdin=pipe, stdout=pipe,
                            stderr=pipe, close_fds=True)
    ret_code = proc.wait()
    if ret_code:
        return False

    output = proc.stdout.read()
    if output.endswith('\n'):
        output = output[:-1]
    return output