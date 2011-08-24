package com.cloud.baremetal;

public class PxeServerProfile {
	Long zoneId;
	Long podId;
	String url;
	String username;
	String password;
	String type;
	String pingStorageServerIp;
	String pingDir;
	String tftpDir;
	String pingCifsUserName;
	String pingCifspassword;
	
	public PxeServerProfile (Long zoneId, Long podId, String url, String username, String password, String type,
			String pingStorageServerIp, String pingDir, String tftpDir, String pingCifsUserName, String pingCifsPassword) {
		this.zoneId = zoneId;
		this.podId = podId;
		this.url = url;
		this.username = username;
		this.password = password;
		this.type = type;
		this.pingStorageServerIp = pingStorageServerIp;
		this.pingDir = pingDir;
		this.tftpDir = tftpDir;
		this.pingCifsUserName = pingCifsUserName;
		this.pingCifspassword = pingCifsPassword;
	}
	
	public Long getZoneId() {
		return zoneId;
	}
	
	public Long getPodId() {
		return podId;
	}
	
	public String getUrl() {
		return url;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getType() {
		return type;
	}
	
	public String getPingStorageServerIp() {
		return pingStorageServerIp;
	}
	
	public String getPingDir() {
		return pingDir;
	}
	
	public String getTftpDir() {
		return tftpDir;
	}
	
	public String getPingCifsUserName() {
		return pingCifsUserName;
	}
	
	public String getPingCifspassword() {
		return pingCifspassword;
	}
}
