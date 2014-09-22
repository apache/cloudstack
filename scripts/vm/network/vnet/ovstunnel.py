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


# Creates a tunnel mesh across xenserver hosts
# Enforces broadcast drop rules on ingress GRE tunnels

import cloudstack_pluginlib as lib
import logging
import commands
import os
import sys
import subprocess
import time
import json
from optparse import OptionParser, OptionGroup, OptParseError, BadOptionError, OptionError, OptionConflictError, OptionValueError

from time import localtime as _localtime, asctime as _asctime

def setup_ovs_bridge(bridge, key, cs_host_id):

    res = lib.check_switch()
    if res != "SUCCESS":
        #return "FAILURE:%s" % res
	return 'false'

    logging.debug("About to manually create the bridge:%s" % bridge)
    #set gre_key to bridge
    res = lib.do_cmd([lib.VSCTL_PATH, "set", "bridge", bridge,
                                     "other_config:gre_key=%s" % key])

    # enable stp
    lib.do_cmd([lib.VSCTL_PATH, "set", "Bridge", bridge, "stp_enable=true"])

    logging.debug("Bridge has been manually created:%s" % res)
    if res:
#        result = "FAILURE:%s" % res
	result = 'false'
    else:
        # Verify the bridge actually exists, with the gre_key properly set
        res = lib.do_cmd([lib.VSCTL_PATH, "get", "bridge",
                                          bridge, "other_config:gre_key"])
        if key in res:
#            result = "SUCCESS:%s" % bridge
            result = 'true'
        else:
#            result = "FAILURE:%s" % res
            result = 'false'

	lib.do_cmd([lib.VSCTL_PATH, "set", "bridge", bridge, "other_config:is-ovs-tun-network=True"])
	#get list of hosts using this bridge
        conf_hosts = lib.do_cmd([lib.VSCTL_PATH, "get","bridge", bridge,"other_config:ovs-host-setup"])
	#add cs_host_id to list of hosts using this bridge
        conf_hosts = cs_host_id + (conf_hosts and ',%s' % conf_hosts or '')
        lib.do_cmd([lib.VSCTL_PATH, "set", "bridge", bridge,
                   "other_config:ovs-host-setup=%s" % conf_hosts])

    logging.debug("Setup_ovs_bridge completed with result:%s" % result)
    return result

def setup_ovs_bridge_for_distributed_routing(bridge, cs_host_id):

    res = lib.check_switch()
    if res != "SUCCESS":
        return "FAILURE:%s" % res

    logging.debug("About to manually create the bridge:%s" % bridge)
    res = lib.do_cmd([lib.VSCTL_PATH, "--", "--may-exist", "add-br", bridge])
    logging.debug("Bridge has been manually created:%s" % res)

    # Non empty result means something went wrong
    if res:
        result = "FAILURE:%s" % res
    else:
        # Verify the bridge actually exists
        res = lib.do_cmd([lib.VSCTL_PATH, "list", "bridge", bridge])

        res = lib.do_cmd([lib.VSCTL_PATH, "set", "bridge", bridge, "other_config:is-ovs_vpc_distributed_vr_network=True"])
        conf_hosts = lib.do_cmd([lib.VSCTL_PATH, "get","bridge", bridge,"other:ovs-host-setup"])
        conf_hosts = cs_host_id + (conf_hosts and ',%s' % conf_hosts or '')
        lib.do_cmd([lib.VSCTL_PATH, "set", "bridge", bridge,
                   "other_config:ovs-host-setup=%s" % conf_hosts])

        # add a default flow rule to send broadcast and multi-cast packets to L2 flooding table
        lib.add_flow(bridge, priority=1000, dl_dst='ff:ff:ff:ff:ff:ff', table=0, actions='resubmit(,2)')
        lib.add_flow(bridge, priority=1000, nw_dst='224.0.0.0/24', table=0, actions='resubmit(,2)')

        # add a default flow rule to send uni-cast traffic to L2 lookup table
        lib.add_flow(bridge, priority=0, table=0, actions='resubmit(,1)')

        # add a default rule to send unknown mac address to L2 flooding table
        lib.add_flow(bridge, priority=0, table=1, actions='resubmit(,2)')

        # add a default rule in L2 flood table to drop packet
        lib.add_flow(bridge, priority=0, table=2, actions='drop')

        # add a default rule in egress table to forward packet to L3 lookup table
        lib.add_flow(bridge, priority=0, table=3, actions='resubmit(,4)')

        # add a default rule in L3 lookup table to forward packet to L2 lookup table
        lib.add_flow(bridge, priority=0, table=4, actions='resubmit(,1)')

        # add a default rule in ingress table to drop in bound packets
        lib.add_flow(bridge, priority=0, table=5, actions='drop')

        result = "SUCCESS: successfully setup bridge with flow rules"

        logging.debug("Setup_ovs_bridge completed with result:%s" % result)

    return result

def destroy_ovs_bridge(bridge):

    res = lib.check_switch()
    if res != "SUCCESS":
#        return res
        return 'false'

    res = lib.do_cmd([lib.VSCTL_PATH, "del-br", bridge])
    logging.debug("Bridge has been manually removed:%s" % res)
    if res:
#        result = "FAILURE:%s" % res
        result = 'false'
    else:
#        result = "SUCCESS:%s" % bridge
        result = 'true'

    logging.debug("Destroy_ovs_bridge completed with result:%s" % result)
    return result

def create_tunnel(bridge, remote_ip, key, src_host, dst_host):

    logging.debug("Entering create_tunnel")

    res = lib.check_switch()
    if res != "SUCCESS":
        logging.debug("Openvswitch running: NO")
#        return "FAILURE:%s" % res
        return 'false'

    # We need to keep the name below 14 characters
    # src and target are enough - consider a fixed length hash
    name = "t%s-%s-%s" % (key, src_host, dst_host)

    # Verify the bridge to be created
    # NOTE: Timeout should not be necessary anymore
    wait = [lib.VSCTL_PATH, "--timeout=30", "wait-until", "bridge",
                    bridge, "--", "get", "bridge", bridge, "name"]
    res = lib.do_cmd(wait)
    if bridge not in res:
        logging.debug("WARNING:Can't find bridge %s for creating " +
                                  "tunnel!" % bridge)
#        return "FAILURE:NO_BRIDGE"
        return 'false'

    logging.debug("bridge %s for creating tunnel - VERIFIED" % bridge)
    tunnel_setup = False
    drop_flow_setup = False
    try:
        # Create a port and configure the tunnel interface for it
        add_tunnel = [lib.VSCTL_PATH, "add-port", bridge,
                                  name, "--", "set", "interface",
                                  name, "type=gre", "options:key=%s" % key,
                                  "options:remote_ip=%s" % remote_ip]
        lib.do_cmd(add_tunnel)
        tunnel_setup = True
        # verify port
        verify_port = [lib.VSCTL_PATH, "get", "port", name, "interfaces"]
        res = lib.do_cmd(verify_port)
        # Expecting python-style list as output
        iface_list = []
        if len(res) > 2:
            iface_list = res.strip()[1:-1].split(',')
        if len(iface_list) != 1:
            logging.debug("WARNING: Unexpected output while verifying " +
                                      "port %s on bridge %s" % (name, bridge))
#            return "FAILURE:VERIFY_PORT_FAILED"
            return 'false'

        # verify interface
        iface_uuid = iface_list[0]
        verify_interface_key = [lib.VSCTL_PATH, "get", "interface",
                                iface_uuid, "options:key"]
        verify_interface_ip = [lib.VSCTL_PATH, "get", "interface",
                               iface_uuid, "options:remote_ip"]

        key_validation = lib.do_cmd(verify_interface_key)
        ip_validation = lib.do_cmd(verify_interface_ip)

        if not key in key_validation or not remote_ip in ip_validation:
            logging.debug("WARNING: Unexpected output while verifying " +
                          "interface %s on bridge %s" % (name, bridge))
#            return "FAILURE:VERIFY_INTERFACE_FAILED"
            return 'false'

        logging.debug("Tunnel interface validated:%s" % verify_interface_ip)
        cmd_tun_ofport = [lib.VSCTL_PATH, "get", "interface",
                                          iface_uuid, "ofport"]
        tun_ofport = lib.do_cmd(cmd_tun_ofport)
        # Ensure no trailing LF
        if tun_ofport.endswith('\n'):
            tun_ofport = tun_ofport[:-1]

        ovs_tunnel_network = lib.do_cmd([lib.VSCTL_PATH, "get", "bridge", bridge, "other_config:is-ovs-tun-network"])
        ovs_vpc_distributed_vr_network = lib.do_cmd([lib.VSCTL_PATH, "get", "bridge", bridge,
                                                     "other_config:is-ovs_vpc_distributed_vr_network"])

        if ovs_tunnel_network == 'True':
            # add flow entryies for dropping broadcast coming in from gre tunnel
            lib.add_flow(bridge, priority=1000, in_port=tun_ofport,
                         dl_dst='ff:ff:ff:ff:ff:ff', actions='drop')
            lib.add_flow(bridge, priority=1000, in_port=tun_ofport,
                     nw_dst='224.0.0.0/24', actions='drop')
            drop_flow_setup = True

        if ovs_vpc_distributed_vr_network == 'True':
            # add flow rules for dropping broadcast coming in from tunnel ports
            lib.add_flow(bridge, priority=1000, in_port=tun_ofport, table=0,
                         dl_dst='ff:ff:ff:ff:ff:ff', actions='drop')
            lib.add_flow(bridge, priority=1000, in_port=tun_ofport, table=0,
                     nw_dst='224.0.0.0/24', actions='drop')

            # add flow rule to send the traffic from tunnel ports to L2 switching table only
            lib.add_flow(bridge, priority=1000, in_port=tun_ofport, table=0, actions='resubmit(,1)')
            lib.do_cmd([lib.VSCTL_PATH, "set", "interface", name, "options:cloudstack-network-id=%s" % network_uuid])

        logging.debug("Broadcast drop rules added")
#        return "SUCCESS:%s" % name
        return 'true'
    except:
        logging.debug("An unexpected error occured. Rolling back")
        if tunnel_setup:
            logging.debug("Deleting GRE interface")
            # Destroy GRE port and interface
            lib.del_port(bridge, name)
        if drop_flow_setup:
            # Delete flows
            logging.debug("Deleting flow entries from GRE interface")
            lib.del_flows(bridge, in_port=tun_ofport)
        # This will not cancel the original exception
        raise

def destroy_tunnel(bridge, iface_name):

    logging.debug("Destroying tunnel at port %s for bridge %s"
                            % (iface_name, bridge))
    ofport = get_field_of_interface(iface_name, "ofport")
    lib.del_flows(bridge, in_port=ofport)
    lib.del_port(bridge, iface_name)
#    return "SUCCESS"
    return 'true'

def get_field_of_interface(iface_name, field):
    get_iface_cmd = [lib.VSCTL_PATH, "get", "interface", iface_name, field]
    res = lib.do_cmd(get_iface_cmd)
    return res

if __name__ == '__main__':
    logging.basicConfig(filename="/var/log/cloudstack/agent/ovstunnel.log", format="%(asctime)s - %(message)s", level=logging.DEBUG)
    parser = OptionParser()
    parser.add_option("--key", dest="key")
    parser.add_option("--cs_host_id", dest="cs_host_id")
    parser.add_option("--bridge", dest="bridge")
    parser.add_option("--remote_ip", dest="remote_ip")
    parser.add_option("--src_host", dest="src_host")
    parser.add_option("--dst_host", dest="dst_host")
    parser.add_option("--iface_name", dest="iface_name")
    parser.add_option("--config", dest="config")
    (option, args) = parser.parse_args()
    if len(args) == 0:
        logging.debug("No command to execute")
        sys.exit(1)
    cmd = args[0]
    if cmd == "setup_ovs_bridge":
        setup_ovs_bridge(option.bridge, option.key, option.cs_host_id)
    elif cmd == "destroy_ovs_bridge":
        destroy_ovs_bridge(option.bridge)
    elif cmd == "create_tunnel":
        create_tunnel(option.bridge, option.remote_ip, option.key, option.src_host, option.dst_host)
    elif cmd == "destroy_tunnel":
        destroy_tunnel(option.bridge, option.iface_name)
    elif cmd == "setup_ovs_bridge_for_distributed_routing":
        setup_ovs_bridge_for_distributed_routing(bridge, cs_host_id)
    elif cmd == "configure_ovs_bridge_for_network_topology":
        configure_bridge_for_network_topology(brdige, cs_host_id, config)
    elif cmd == "configure_ovs_bridge_for_routing_policies":
        configure_ovs_bridge_for_routing_policies(bridge, config)
    else:
        logging.debug("Unknown command: " + cmd)
        sys.exit(1)

