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

# A simple script for enabling and disabling per-vif rules for explicitly
# allowing broadcast/multicast traffic on the port where the VIF is attached

import os
import sys

import cloudstack_pluginlib as pluginlib


def clear_flows(bridge, this_vif_ofport, vif_ofports):
    # Remove flow entries originating from given ofport
    pluginlib.del_flows(bridge, in_port=this_vif_ofport)
	# The following will remove the port being delete from actions
    pluginlib.add_flow(bridge, priority=1100,
                       dl_dst='ff:ff:ff:ff:ff:ff', actions=action)
    pluginlib.add_flow(bridge, priority=1100,
                       nw_dst='224.0.0.0/24', actions=action)


def apply_flows(bridge, this_vif_ofport, vif_ofports):
    action = "".join("output:%s," %ofport
                for ofport in vif_ofports)[:-1]
	# Ensure {b|m}casts sent from VIF ports are always allowed
    pluginlib.add_flow(bridge, priority=1200,
					   in_port=this_vif_ofport,
					   dl_dst='ff:ff:ff:ff:ff:ff',
					   actions='NORMAL')
    pluginlib.add_flow(bridge, priority=1200,
					   in_port=this_vif_ofport,
					   nw_dst='224.0.0.0/24',
					   actions='NORMAL')
	# Ensure {b|m}casts are always propagated to VIF ports
    pluginlib.add_flow(bridge, priority=1100,
                       dl_dst='ff:ff:ff:ff:ff:ff', actions=action)
    pluginlib.add_flow(bridge, priority=1100,
                       nw_dst='224.0.0.0/24', actions=action)

def clear_rules(vif):
    try:
        delcmd = "/sbin/ebtables -t nat -L PREROUTING | grep " + vif
        delcmds = pluginlib.do_cmd(['/bin/bash', '-c', delcmd]).split('\n')
        for cmd in delcmds:
            try:
                cmd = '/sbin/ebtables -t nat -D PREROUTING ' + cmd
                pluginlib.do_cmd(['/bin/bash', '-c', cmd])
            except:
                pass
    except:
        pass


def main(command, vif_raw):
    if command not in ('online', 'offline'):
        return

    vif_name, dom_id, vif_index = vif_raw.split('-')
    # validate vif and dom-id
    this_vif = "%s%s.%s" % (vif_name, dom_id, vif_index)
    # Make sure the networking stack is not linux bridge!
    net_stack = pluginlib.do_cmd(['cat', '/etc/xensource/network.conf'])
    if net_stack.lower() == "bridge":
        if command == 'offline':
            clear_rules(this_vif)
        # Nothing to do here!
        return

    bridge = pluginlib.do_cmd([pluginlib.VSCTL_PATH, 'iface-to-br', this_vif])
    
	# find xs network for this bridge, verify is used for ovs tunnel network
    xs_nw_uuid = pluginlib.do_cmd([pluginlib.XE_PATH, "network-list",
								   "bridge=%s" % bridge, "--minimal"])
    result = pluginlib.do_cmd([pluginlib.XE_PATH,"network-param-get",
						       "uuid=%s" % xs_nw_uuid,
						       "param-name=other-config",
						       "param-key=is-ovs-tun-network", "--minimal"])

    if result != 'True':
		return
    
    vlan = pluginlib.do_cmd([pluginlib.VSCTL_PATH, 'br-to-vlan', bridge])
    if vlan != '0':
            # We need the REAL bridge name
            bridge = pluginlib.do_cmd([pluginlib.VSCTL_PATH,
                                       'br-to-parent', bridge])

    vsctl_output = pluginlib.do_cmd([pluginlib.VSCTL_PATH,
                                     'list-ports', bridge])
    vifs = vsctl_output.split('\n')
    vif_ofports = []
    for vif in vifs:
    	vif_ofport = pluginlib.do_cmd([pluginlib.VSCTL_PATH, 'get',
                                       'Interface', vif, 'ofport'])
    	if this_vif == vif:
    		this_vif_ofport = vif_ofport
        if vif.startswith('vif'): 
            vif_ofports.append(vif_ofport)

    if command == 'offline':
        clear_flows(bridge,  this_vif_ofport, vif_ofports)

    if command == 'online':
        apply_flows(bridge,  this_vif_ofport, vif_ofports)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print "usage: %s [online|offline] vif-domid-idx" % \
               os.path.basename(sys.argv[0])
        sys.exit(1)
    else:
        command, vif_raw = sys.argv[1:3]
        main(command, vif_raw)
