package com.cloud.ovm.object;

import org.apache.xmlrpc.XmlRpcException;

public class OvmSecurityGroup extends OvmObject {
	
	// canBridgeFirewall
	// cleanupRules
	// defaultNetworkRulesUserVm
	// addNetworkRules
	// deleteAllNetworkRulesForVm
	
	public static boolean canBridgeFirewall(Connection c) throws XmlRpcException {
		Object[] params = {};
		return (Boolean) c.call("OvmSecurityGroup.can_bridge_firewall", params);
	}
	
	public static boolean cleanupNetworkRules(Connection c) throws XmlRpcException {
		Object[] params = {};
		return (Boolean) c.call("OvmSecurityGroup.cleanup_rules", params);
	}
	
	public static boolean defaultNetworkRulesForUserVm(Connection c, String vmName, String vmId, String ipAddress, String macAddress, String vifName, String bridgeName) throws XmlRpcException {
		Object[] params = {vmName, vmId, ipAddress, macAddress, vifName, bridgeName};
		return (Boolean) c.call("OvmSecurityGroup.default_network_rules_user_vm", params);
	}
	
	public static boolean addNetworkRules(Connection c, String vmName, String vmId, String guestIp, String signature, String seqno, String vifMacAddress, String newRules, String vifDeviceName, String bridgeName) throws XmlRpcException {
		Object[] params = {vmName, vmId, guestIp, signature, seqno, vifMacAddress, newRules, vifDeviceName, bridgeName};
		return (Boolean) c.call("OvmSecurityGroup.add_network_rules", params);
	}
	
	public static boolean deleteAllNetworkRulesForVm(Connection c, String vmName, String vif) throws XmlRpcException {
		Object[] params = {vmName, vif};
		return (Boolean) c.call("OvmSecurityGroup.delete_all_network_rules_for_vm", params);
	}
	
}
