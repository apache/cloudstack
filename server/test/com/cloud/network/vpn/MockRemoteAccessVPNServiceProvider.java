package com.cloud.network.vpn;

import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.RemoteAccessVpn;
import com.cloud.network.VpnUser;
import com.cloud.network.element.RemoteAccessVPNServiceProvider;

@Local (value = RemoteAccessVPNServiceProvider.class)
public class MockRemoteAccessVPNServiceProvider implements
		RemoteAccessVPNServiceProvider {

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		return true;
	}

	@Override
	public String getName() {
		return "MockRemoteAccessVPNServiceProvider";
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String[] applyVpnUsers(RemoteAccessVpn vpn,
			List<? extends VpnUser> users) throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean startVpn(Network network, RemoteAccessVpn vpn)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stopVpn(Network network, RemoteAccessVpn vpn)
			throws ResourceUnavailableException {
		// TODO Auto-generated method stub
		return false;
	}

}
