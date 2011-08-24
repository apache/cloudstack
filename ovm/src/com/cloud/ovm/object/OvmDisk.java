package com.cloud.ovm.object;


public class OvmDisk extends OvmObject {
	public static final String WRITE = "w";
	public static final String READ = "r";
	public static final String SHAREDWRITE = "w!";
	public static final String SHAREDREAD = "r!";
	
	public static class Details {
		public String type;
		public String path;
		public Boolean isIso;
	}
}
