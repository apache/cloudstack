package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2DescribeSecurityGroupsResponse {

	private List<EC2SecurityGroup> groupSet = new ArrayList<EC2SecurityGroup>();    

	public EC2DescribeSecurityGroupsResponse() {
	}
	
	public void addGroup( EC2SecurityGroup param ) {
		groupSet.add( param );
	}
	
	public EC2SecurityGroup[] getGroupSet() {
		return groupSet.toArray(new EC2SecurityGroup[0]);
	}
}
