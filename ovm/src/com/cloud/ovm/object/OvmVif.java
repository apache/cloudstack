package com.cloud.ovm.object;

public class OvmVif extends OvmObject {
	public static final String NETFRONT = "netfront";
	public static final String IOEMU = "ioemu";
	
	public static class Details {
		public String name;
		public String mac;
		public String bridge;
		public String type;
	}
	
}
