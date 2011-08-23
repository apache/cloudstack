package com.cloud.hypervisor.vmware.mo;

public interface VersioningContants {
	
	// portgroups created on vSwitch will carry a version number now, so that when CloudStack has a major naming convention upgrade
	// we know how to cleanup at vCenter side
	public final static int PORTGROUP_NAMING_VERSION = 1;
}
