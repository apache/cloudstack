# A simple script for enabling and disabling per-vif rules for explicitly
# allowing broadcast/multicast traffic on the port where the VIF is attached
# 
# Copyright (C) 2012 Citrix Systems

import os
import sys

import cloudstack_pluginlib as pluginlib


def clear_flows(bridge, vif_ofport):
    # Remove flow entries originating from given ofport
    pluginlib.del_flows(bridge, in_port=vif_ofport)
    # Leverage out_port option for removing flows based on actions
    pluginlib.del_flows(bridge, out_port=vif_ofport)


def apply_flows(bridge, vif_ofport):
    action = "output:%s," % vif_ofport
    # Ensure {b|m}casts sent from VIF ports are always allowed
    pluginlib.add_flow(bridge, priority=1200,
					   in_port=vif_ofport,
					   dl_dst='ff:ff:ff:ff:ff:ff',
					   actions='NORMAL')
    pluginlib.add_flow(bridge, priority=1200,
					   in_port=vif_ofport,
					   nw_dst='224.0.0.0/24',
					   actions='NORMAL')
    # Ensure {b|m}casts are always propagated to VIF ports
    pluginlib.add_flow(bridge, priority=1100,
                       dl_dst='ff:ff:ff:ff:ff:ff', actions=action)
    pluginlib.add_flow(bridge, priority=1100,
                       nw_dst='224.0.0.0/24', actions=action)


def main(command, vif_raw):
    if command not in ('online', 'offline'):
        return
    # TODO (very important)
    # Quit immediately if networking is NOT being managed by the OVS tunnel manager
    vif_name, dom_id, vif_index = vif_raw.split('-')
    # validate vif and dom-id
    vif = "%s%s.%s" % (vif_name, dom_id, vif_index)

    bridge = pluginlib.do_cmd([pluginlib.VSCTL_PATH, 'iface-to-br', vif])
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
            bridge = pluginlib.do_cmd([pluginlib.VSCTL_PATH, 'br-to-parent', bridge])
    vif_ofport = pluginlib.do_cmd([pluginlib.VSCTL_PATH, 'get', 'Interface',
                                   vif, 'ofport'])

    if command == 'offline':
        clear_flows(bridge, vif_ofport)

    if command == 'online':
        apply_flows(bridge,  vif_ofport)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print "usage: %s [online|offline] vif-domid-idx" % \
               os.path.basename(sys.argv[0])
        sys.exit(1)
    else:
        command, vif_raw = sys.argv[1:3]
        main(command, vif_raw)