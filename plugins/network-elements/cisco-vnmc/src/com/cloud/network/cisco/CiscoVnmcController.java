package com.cloud.network.cisco;

import org.apache.cloudstack.api.Identity;
import org.apache.cloudstack.api.InternalIdentity;

import com.cloud.org.Grouping;

public interface CiscoVnmcController extends Grouping, InternalIdentity, Identity {

	 long getId();

	 String getUuid();

	 void setUuid(String uuid);

	 long getPhysicalNetworkId();

	 long getHostId();

	 String getProviderName();

	 String getDeviceName();

}