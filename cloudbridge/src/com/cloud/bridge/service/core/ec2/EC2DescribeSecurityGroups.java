package com.cloud.bridge.service.core.ec2;

import java.util.ArrayList;
import java.util.List;

public class EC2DescribeSecurityGroups {
	
	private List<String> groupSet = new ArrayList<String>();    // a list of strings identifying each group
	private EC2GroupFilterSet gfs = null;

	public EC2DescribeSecurityGroups() {
	}

	public void addGroupName( String param ) {
		groupSet.add( param );
	}
	
	public String[] getGroupSet() {
		return groupSet.toArray(new String[0]);
	}
	
	public EC2GroupFilterSet getFilterSet() {
		return gfs;
	}
	
	public void setFilterSet( EC2GroupFilterSet param ) {
		gfs = param;
	}
}
