package com.cloud.network.resource;

import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.junit.Before;
import org.junit.Test;

import com.cloud.network.nicira.NiciraNvpApi;

public class NiciraNvpResourceTest {
	NiciraNvpApi _nvpApi = mock(NiciraNvpApi.class);
	NiciraNvpResource _resource;
	
	@Before
	public void setUp() {
		_resource = new NiciraNvpResource() {
			protected NiciraNvpApi createNiciraNvpApi() {
				return _nvpApi;
			}
		};
	}
	
	@Test (expected=ConfigurationException.class)
	public void resourceConfigureFailure() throws ConfigurationException {
		_resource.configure("NiciraNvpResource", Collections.<String,Object>emptyMap());
	}
	
	@Test 
	public void resourceConfigure() throws ConfigurationException {
		Map<String,Object> parameters = new HashMap<String,Object>();
		parameters.put("name","nvptestdevice");
		parameters.put("ip","127.0.0.1");
		parameters.put("adminuser","adminuser");
		parameters.put("guid", "aaaaa-bbbbb-ccccc");
		parameters.put("zoneId", "blublub");
		parameters.put("adminpass","adminpass");
		_resource.configure("NiciraNvpResource", parameters);
		
		verify(_nvpApi).setAdminCredentials("adminuser", "adminpass");
		verify(_nvpApi).setControllerAddress("127.0.0.1");
	}
	
}
