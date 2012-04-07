package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2AuthorizeRevokeSecurityGroup {

	private String name;
	private List<EC2IpPermission> permissionSet = new ArrayList<EC2IpPermission>();    // a list of permissions to be removed from the group
	
	public EC2AuthorizeRevokeSecurityGroup() {
		name = null;
	}
	
	public void setName( String name ) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

	public void addIpPermission( EC2IpPermission param ) {
		permissionSet.add( param );
	}
	
	public EC2IpPermission[] getIpPermissionSet() {
		return permissionSet.toArray(new EC2IpPermission[0]);
	}
}
