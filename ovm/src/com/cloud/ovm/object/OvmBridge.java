package com.cloud.ovm.object;

import java.util.List;

import org.apache.xmlrpc.XmlRpcException;

public class OvmBridge extends OvmObject {
	public static class Details {
		public String name;
		public String attach;
		public List<String> interfaces;
		
		public String toJson() {
			return Coder.toJson(this);
		}
	}
	
	public static void create(Connection c, Details d)  throws XmlRpcException {
		Object[] params = {d.toJson()};
		c.call("OvmNetwork.createBridge", params);
	}
	
	public static void delete(Connection c, String name) throws XmlRpcException {
		Object[] params = {name};
		c.call("OvmNetwork.deleteBridge", params);
	}
	
	public static List<String> getAllBridges(Connection c) throws XmlRpcException {
		String res = (String) c.call("OvmNetwork.getAllBridges", Coder.EMPTY_PARAMS);
		return Coder.listFromJson(res);
	}
	
	public static String getBridgeByIp(Connection c, String ip) throws XmlRpcException {
		Object[] params = {ip};
		String res = (String) c.call("OvmNetwork.getBridgeByIp", params);
		return Coder.mapFromJson(res).get("bridge");
	}
	
	public static void createVlanBridge(Connection c, OvmBridge.Details bDetails, OvmVlan.Details vDetails) throws XmlRpcException {
		Object[] params = {bDetails.toJson(), vDetails.toJson()};
		c.call("OvmNetwork.createVlanBridge", params);
	}
	
	public static void deleteVlanBridge(Connection c, String name) throws XmlRpcException {
		Object[] params = {name};
		c.call("OvmNetwork.deleteVlanBridge", params);
	}
	
	public static Details getDetails(Connection c, String name) throws XmlRpcException {
		Object[] params = {name};
		String res = (String) c.call("OvmNetwork.getBridgeDetails", params);
		return Coder.fromJson(res, Details.class);
	}
}
