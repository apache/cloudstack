package com.cloud.ovm.object;

import org.apache.xmlrpc.XmlRpcException;

public class OvmVlan extends OvmObject {
	public static class Details {
		public String name;
		public int vid;
		public String pif;
		
		public String toJson() {
			return Coder.toJson(this);
		}
	}
	
	public static String create(Connection c, Details d) throws XmlRpcException {
		Object[] params = {d.toJson()};
		String res = (String)c.call("OvmNetwork.createVlan", params);
		Details ret = Coder.fromJson(res, Details.class);
		return ret.name;
	}
	
	public static void delete(Connection c, String name) throws XmlRpcException {
		Object[] params = {name};
		c.call("OvmNetwork.deleteVlan", params);
	}
}
